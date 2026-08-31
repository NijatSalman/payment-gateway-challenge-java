package com.checkout.payment.gateway.idempotency;

import com.checkout.payment.gateway.domain.Payment;
import com.checkout.payment.gateway.domain.ProcessedPayment;
import com.checkout.payment.gateway.exception.IdempotencyKeyConflictException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Remembers the payment created for each Idempotency-Key so a repeated request returns the same
 * payment instead of charging the card again.
 *
 * <p>How it works: every request first tries to claim its key with {@code putIfAbsent}, which is
 * atomic — exactly one request wins, even under concurrency. The winner processes the payment and
 * publishes the result into a {@link CompletableFuture} (a one-slot mailbox); every other request
 * with the same key waits on that mailbox and returns the same payment, so the bank is called at
 * most once per key. A failed attempt releases the key so the merchant can retry.
 */
@Component
public class IdempotencyStore {

  private record Entry(String fingerprint, CompletableFuture<Payment> result) {
  }

  private final Map<String, Entry> entries = new ConcurrentHashMap<>();

  public ProcessedPayment process(String key, String fingerprint, Supplier<Payment> processor) {
    Entry entry = new Entry(fingerprint, new CompletableFuture<>());
    Entry existing = entries.putIfAbsent(key, entry);
    if (existing == null) {
      return new ProcessedPayment(processFirstRequest(key, entry, processor), false);
    }
    return new ProcessedPayment(replayExistingRequest(key, fingerprint, existing), true);
  }

  /** This request claimed the key: process the payment and publish the result to any waiters. */
  private Payment processFirstRequest(String key, Entry entry, Supplier<Payment> processor) {
    try {
      Payment payment = processor.get();
      entry.result().complete(payment);
      return payment;
    } catch (RuntimeException ex) {
      entries.remove(key);
      entry.result().completeExceptionally(ex);
      throw ex;
    }
  }

  /** The key is already claimed: reject a different request, otherwise return its payment. */
  private static Payment replayExistingRequest(String key, String fingerprint, Entry existing) {
    if (!existing.fingerprint().equals(fingerprint)) {
      throw new IdempotencyKeyConflictException(key);
    }
    return await(existing.result());
  }

  /** Waits for the first request's payment; rethrows its original exception, not a wrapped one. */
  private static Payment await(CompletableFuture<Payment> result) {
    try {
      return result.join();
    } catch (CompletionException ex) {
      throw ex.getCause() instanceof RuntimeException cause ? cause : ex;
    }
  }
}
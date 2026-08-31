package com.checkout.payment.gateway.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.checkout.payment.gateway.domain.Payment;
import com.checkout.payment.gateway.domain.ProcessedPayment;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.IdempotencyKeyConflictException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class IdempotencyStoreTest {

  private final IdempotencyStore store = new IdempotencyStore();

  @Test
  void whenSameKeyIsProcessedConcurrentlyThenPaymentIsCreatedOnlyOnce() throws Exception {
    Payment payment = payment();
    AtomicInteger processedCount = new AtomicInteger();
    Callable<ProcessedPayment> request = () -> store.process("order-1", "fp", () -> {
      processedCount.incrementAndGet();
      sleep(); // keep the "bank call" busy so the requests overlap
      return payment;
    });

    ExecutorService executor = Executors.newFixedThreadPool(10);
    List<Future<ProcessedPayment>> results = executor.invokeAll(Collections.nCopies(10, request));
    executor.shutdown();

    for (Future<ProcessedPayment> result : results) {
      assertThat(result.get().payment()).isEqualTo(payment); // everyone got the same payment
    }
    assertThat(processedCount).hasValue(1); // ...but it was created only once
  }

  @Test
  void whenKeyIsReusedWithDifferentFingerprintThenConflictIsThrown() {
    store.process("order-1", "fp", IdempotencyStoreTest::payment);

    assertThatThrownBy(() -> store.process("order-1", "other-fp", IdempotencyStoreTest::payment))
        .isInstanceOf(IdempotencyKeyConflictException.class);
  }

  @Test
  void whenProcessingFailsThenKeyCanBeUsedAgain() {
    RuntimeException failure = new RuntimeException("bank down");
    assertThatThrownBy(() -> store.process("order-1", "fp", () -> {
      throw failure;
    })).isSameAs(failure);

    ProcessedPayment retried = store.process("order-1", "fp", IdempotencyStoreTest::payment);

    assertThat(retried.replayed()).isFalse();
  }

  private static Payment payment() {
    return new Payment(UUID.randomUUID(), PaymentStatus.AUTHORIZED, "8877", 4, 2030, "GBP", 100,
        "auth-code", Instant.now());
  }

  private static void sleep() {
    try {
      Thread.sleep(50);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
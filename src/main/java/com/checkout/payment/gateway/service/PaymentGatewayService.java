package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.client.BankPaymentRequest;
import com.checkout.payment.gateway.client.BankPaymentResponse;
import com.checkout.payment.gateway.domain.Payment;
import com.checkout.payment.gateway.domain.ProcessedPayment;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.idempotency.IdempotencyStore;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.validation.PaymentRequestValidator;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);
  private static final int LAST_FOUR_DIGITS = 4;

  private final PaymentsRepository paymentsRepository;
  private final PaymentRequestValidator paymentRequestValidator;
  private final AcquiringBankClient acquiringBankClient;
  private final IdempotencyStore idempotencyStore;
  private final Clock clock;

  public PaymentGatewayService(PaymentsRepository paymentsRepository,
      PaymentRequestValidator paymentRequestValidator, AcquiringBankClient acquiringBankClient,
      IdempotencyStore idempotencyStore, Clock clock) {
    this.paymentsRepository = paymentsRepository;
    this.paymentRequestValidator = paymentRequestValidator;
    this.acquiringBankClient = acquiringBankClient;
    this.idempotencyStore = idempotencyStore;
    this.clock = clock;
  }

  public Payment getPaymentById(UUID id) {
    LOG.debug("Payment retrieval requested: paymentId={}", id);
    return paymentsRepository.findById(id)
        .orElseThrow(() -> new PaymentNotFoundException(id));
  }

  /**
   * Processes a payment. When an Idempotency-Key is supplied, a repeated request with the same key
   * returns the payment created by the first request instead of charging the card again.
   */
  public ProcessedPayment processPayment(PaymentRequest request, String idempotencyKey) {
    paymentRequestValidator.validate(request);
    if (!StringUtils.hasText(idempotencyKey)) {
      return new ProcessedPayment(process(request), false);
    }
    paymentRequestValidator.validateIdempotencyKey(idempotencyKey);
    ProcessedPayment result =
        idempotencyStore.process(idempotencyKey, fingerprint(request), () -> process(request));
    if (result.replayed()) {
      LOG.info("Payment replayed: paymentId={}, idempotencyKey={}", result.payment().id(),
          idempotencyKey);
    }
    return result;
  }

  private Payment process(PaymentRequest request) {
    BankPaymentResponse bankResponse =
        acquiringBankClient.authorize(BankPaymentRequest.from(request));

    Payment payment = new Payment(
        UUID.randomUUID(),
        bankResponse.authorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED,
        lastFourDigits(request.cardNumber()),
        request.expiryMonth(),
        request.expiryYear(),
        request.currency(),
        request.amount(),
        bankResponse.authorized() ? bankResponse.authorizationCode() : null,
        Instant.now(clock));
    paymentsRepository.save(payment);
    LOG.info("Payment processed: paymentId={}, status={}", payment.id(), payment.status());
    return payment;
  }

  /** Identifies the request without holding the full card number or CVV. */
  private static String fingerprint(PaymentRequest request) {
    return String.join("|", lastFourDigits(request.cardNumber()),
        String.valueOf(request.expiryMonth()), String.valueOf(request.expiryYear()),
        request.currency(), String.valueOf(request.amount()));
  }

  private static String lastFourDigits(String cardNumber) {
    return cardNumber.substring(cardNumber.length() - LAST_FOUR_DIGITS);
  }
}
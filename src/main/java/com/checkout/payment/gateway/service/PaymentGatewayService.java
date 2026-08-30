package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.client.BankPaymentRequest;
import com.checkout.payment.gateway.client.BankPaymentResponse;
import com.checkout.payment.gateway.domain.Payment;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.validation.PaymentRequestValidator;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);
  private static final int LAST_FOUR_DIGITS = 4;

  private final PaymentsRepository paymentsRepository;
  private final PaymentRequestValidator paymentRequestValidator;
  private final AcquiringBankClient acquiringBankClient;
  private final Clock clock;

  public PaymentGatewayService(PaymentsRepository paymentsRepository,
      PaymentRequestValidator paymentRequestValidator, AcquiringBankClient acquiringBankClient,
      Clock clock) {
    this.paymentsRepository = paymentsRepository;
    this.paymentRequestValidator = paymentRequestValidator;
    this.acquiringBankClient = acquiringBankClient;
    this.clock = clock;
  }

  public Payment getPaymentById(UUID id) {
    LOG.debug("Payment retrieval requested: paymentId={}", id);
    return paymentsRepository.findById(id)
        .orElseThrow(() -> new PaymentNotFoundException(id));
  }

  public Payment processPayment(PaymentRequest request) {
    paymentRequestValidator.validate(request);
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

  private static String lastFourDigits(String cardNumber) {
    return cardNumber.substring(cardNumber.length() - LAST_FOUR_DIGITS);
  }
}
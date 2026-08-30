package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.domain.Payment;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.validation.PaymentRequestValidator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final PaymentRequestValidator paymentRequestValidator;

  public PaymentGatewayService(PaymentsRepository paymentsRepository,
      PaymentRequestValidator paymentRequestValidator) {
    this.paymentsRepository = paymentsRepository;
    this.paymentRequestValidator = paymentRequestValidator;
  }

  public Payment getPaymentById(UUID id) {
    LOG.debug("Payment retrieval requested: paymentId={}", id);
    return paymentsRepository.findById(id)
        .orElseThrow(() -> new PaymentNotFoundException(id));
  }

  public Payment processPayment(PaymentRequest paymentRequest) {
    paymentRequestValidator.validate(paymentRequest);
    throw new UnsupportedOperationException("Payment processing is not implemented yet");
  }
}
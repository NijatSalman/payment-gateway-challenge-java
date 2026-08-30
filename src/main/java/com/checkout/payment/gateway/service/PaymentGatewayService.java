package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.domain.Payment;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;

  public PaymentGatewayService(PaymentsRepository paymentsRepository) {
    this.paymentsRepository = paymentsRepository;
  }

  public Payment getPaymentById(UUID id) {
    LOG.debug("Retrieving payment with id {}", id);
    return paymentsRepository.findById(id)
        .orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  public Payment processPayment(PostPaymentRequest paymentRequest) {
    throw new UnsupportedOperationException("Payment processing is not implemented yet");
  }
}
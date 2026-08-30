package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.domain.Payment;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    PaymentStatus status,
    @JsonProperty("card_number_last_four") String cardNumberLastFour,
    @JsonProperty("expiry_month") int expiryMonth,
    @JsonProperty("expiry_year") int expiryYear,
    String currency,
    long amount) {

  public static PaymentResponse from(Payment payment) {
    return new PaymentResponse(
        payment.id(),
        payment.status(),
        payment.cardLastFour(),
        payment.expiryMonth(),
        payment.expiryYear(),
        payment.currency(),
        payment.amount());
  }
}
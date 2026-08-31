package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.domain.Payment;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record PaymentResponse(
    @Schema(description = "Payment identifier, used to retrieve the payment later.",
        example = "6f1c0a3e-8b2d-4c8e-9f1a-2b3c4d5e6f70")
    UUID id,

    @Schema(description = "Outcome of the payment as decided by the acquiring bank.",
        example = "Authorized")
    PaymentStatus status,

    @JsonProperty("card_number_last_four")
    @Schema(description = "Last four digits of the card. The full number is never returned.",
        example = "8871")
    String cardNumberLastFour,

    @JsonProperty("expiry_month")
    @Schema(example = "4")
    int expiryMonth,

    @JsonProperty("expiry_year")
    @Schema(example = "2030")
    int expiryYear,

    @Schema(example = "GBP")
    String currency,

    @Schema(description = "Amount in the minor currency unit.", example = "1050")
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
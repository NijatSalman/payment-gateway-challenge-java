package com.checkout.payment.gateway.domain;

import com.checkout.payment.gateway.enums.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record Payment(
    UUID id,
    PaymentStatus status,
    String cardLastFour,
    int expiryMonth,
    int expiryYear,
    String currency,
    long amount,
    String authorizationCode,
    Instant createdAt) {
}
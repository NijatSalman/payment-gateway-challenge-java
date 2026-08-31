package com.checkout.payment.gateway.domain;

/** Outcome of a payment request: the payment and whether it was replayed from an earlier request. */
public record ProcessedPayment(Payment payment, boolean replayed) {
}
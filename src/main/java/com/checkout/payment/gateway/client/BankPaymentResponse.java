package com.checkout.payment.gateway.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Response body returned by the acquiring bank. */
public record BankPaymentResponse(
    boolean authorized,
    @JsonProperty("authorization_code") String authorizationCode) {
}
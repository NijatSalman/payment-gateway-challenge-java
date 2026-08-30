package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.model.PaymentRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Request body expected by the acquiring bank ({@code POST /payments}). */
public record BankPaymentRequest(
    @JsonProperty("card_number") String cardNumber,
    @JsonProperty("expiry_date") String expiryDate,
    String currency,
    long amount,
    String cvv) {

  private static final String EXPIRY_DATE_FORMAT = "%02d/%d";
  private static final String MASK = "****";

  public static BankPaymentRequest from(PaymentRequest request) {
    return new BankPaymentRequest(
        request.cardNumber(),
        String.format(EXPIRY_DATE_FORMAT, request.expiryMonth(), request.expiryYear()),
        request.currency(),
        request.amount(),
        request.cvv());
  }

  @Override
  public String toString() {
    return "BankPaymentRequest{"
        + "cardNumber=" + MASK + cardNumber.substring(cardNumber.length() - 4)
        + ", expiryDate='" + expiryDate + '\''
        + ", currency='" + currency + '\''
        + ", amount=" + amount
        + ", cvv=***"
        + '}';
  }
}
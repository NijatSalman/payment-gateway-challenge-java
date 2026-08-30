package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PostPaymentRequest(
    @JsonProperty("card_number") String cardNumber,
    @JsonProperty("expiry_month") Integer expiryMonth,
    @JsonProperty("expiry_year") Integer expiryYear,
    String currency,
    Long amount,
    String cvv) {

  private static final String MASK = "****";

  @Override
  public String toString() {
    return "PostPaymentRequest{"
        + "cardNumber=" + maskCardNumber()
        + ", expiryMonth=" + expiryMonth
        + ", expiryYear=" + expiryYear
        + ", currency='" + currency + '\''
        + ", amount=" + amount
        + ", cvv=" + (cvv == null ? null : "***")
        + '}';
  }

  private String maskCardNumber() {
    if (cardNumber == null) {
      return null;
    }
    if (cardNumber.length() < 4) {
      return MASK;
    }
    return MASK + cardNumber.substring(cardNumber.length() - 4);
  }
}
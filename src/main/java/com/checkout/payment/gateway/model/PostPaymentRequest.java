package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PostPaymentRequest(
    @JsonProperty("card_number")
    @NotBlank(message = "is required")
    @Pattern(regexp = "^\\d{14,19}$", message = "must be 14-19 digits")
    String cardNumber,

    @JsonProperty("expiry_month")
    @NotNull(message = "is required")
    @Min(value = 1, message = "must be between 1 and 12")
    @Max(value = 12, message = "must be between 1 and 12")
    Integer expiryMonth,

    @JsonProperty("expiry_year")
    @NotNull(message = "is required")
    @Min(value = 1, message = "must be between 1 and 9999")
    @Max(value = 9999, message = "must be between 1 and 9999")
    Integer expiryYear,

    @NotBlank(message = "is required")
    @Size(min = 3, max = 3, message = "must be 3 characters")
    String currency,

    @NotNull(message = "is required")
    @Positive(message = "must be greater than 0")
    Long amount,

    @NotBlank(message = "is required")
    @Pattern(regexp = "^\\d{3,4}$", message = "must be 3-4 digits")
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
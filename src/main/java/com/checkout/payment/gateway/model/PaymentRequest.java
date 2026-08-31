package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PaymentRequest(
    @JsonProperty("card_number")
    @Schema(description = "Card number, 14-19 digits. The test bank authorizes cards ending in an"
        + " odd digit, declines even ones and is unavailable for cards ending in 0.",
        example = "2222405343248871")
    @NotNull(message = "is required")
    @Pattern(regexp = "^\\d{14,19}$", message = "must be 14-19 digits")
    String cardNumber,

    @JsonProperty("expiry_month")
    @Schema(description = "Card expiry month (1-12).", example = "4")
    @NotNull(message = "is required")
    @Min(value = 1, message = "must be between 1 and 12")
    @Max(value = 12, message = "must be between 1 and 12")
    Integer expiryMonth,

    @JsonProperty("expiry_year")
    @Schema(description = "Card expiry year (4 digits). Month + year must not be in the past.",
        example = "2030")
    @NotNull(message = "is required")
    @Min(value = 1, message = "must be between 1 and 9999")
    @Max(value = 9999, message = "must be between 1 and 9999")
    Integer expiryYear,

    @Schema(description = "ISO 4217 currency code, uppercase. Supported: EUR, GBP, USD.",
        example = "GBP")
    @NotNull(message = "is required")
    @Size(min = 3, max = 3, message = "must be 3 characters")
    String currency,

    @Schema(description = "Amount in the minor currency unit, e.g. 1050 = £10.50.",
        example = "1050")
    @NotNull(message = "is required")
    @Positive(message = "must be greater than 0")
    Long amount,

    @Schema(description = "Card verification value, 3-4 digits. Never stored or returned.",
        example = "123")
    @NotNull(message = "is required")
    @Pattern(regexp = "^\\d{3,4}$", message = "must be 3-4 digits")
    String cvv) {

  private static final String MASK = "****";

  @Override
  public String toString() {
    return "PaymentRequest{"
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
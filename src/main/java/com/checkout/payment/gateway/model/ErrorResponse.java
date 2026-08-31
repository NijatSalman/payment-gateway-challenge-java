package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * API error body. {@code status} and {@code errors} are only present for rejected payment
 * requests.
 */
@JsonInclude(Include.NON_NULL)
public record ErrorResponse(
    @Schema(description = "Only present for rejected payment requests.", example = "Rejected")
    PaymentStatus status,

    @Schema(example = "Payment request rejected")
    String message,

    @Schema(description = "One entry per invalid request field; only present for rejected"
        + " payment requests.")
    List<FieldError> errors) {

  private static final String REJECTED_MESSAGE = "Payment request rejected";

  public static ErrorResponse of(String message) {
    return new ErrorResponse(null, message, null);
  }

  public static ErrorResponse rejected(List<FieldError> errors) {
    return new ErrorResponse(PaymentStatus.REJECTED, REJECTED_MESSAGE, errors);
  }
}
package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;

@JsonInclude(Include.NON_NULL)
public record ErrorResponse(PaymentStatus status, String message, List<FieldError> errors) {

  private static final String REJECTED_MESSAGE = "Payment request rejected";

  public static ErrorResponse of(String message) {
    return new ErrorResponse(null, message, null);
  }

  public static ErrorResponse rejected(List<FieldError> errors) {
    return new ErrorResponse(PaymentStatus.REJECTED, REJECTED_MESSAGE, errors);
  }
}
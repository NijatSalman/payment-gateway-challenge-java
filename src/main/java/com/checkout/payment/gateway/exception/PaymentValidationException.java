package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.FieldError;
import java.util.List;

public class PaymentValidationException extends RuntimeException {

  private final List<FieldError> errors;

  public PaymentValidationException(List<FieldError> errors) {
    super("Payment request rejected");
    this.errors = List.copyOf(errors);
  }

  public List<FieldError> getErrors() {
    return errors;
  }
}
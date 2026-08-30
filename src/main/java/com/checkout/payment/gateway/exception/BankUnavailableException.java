package com.checkout.payment.gateway.exception;

/** The acquiring bank could not be reached or reported itself unavailable. */
public class BankUnavailableException extends RuntimeException {

  public BankUnavailableException(String message) {
    super(message);
  }

  public BankUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
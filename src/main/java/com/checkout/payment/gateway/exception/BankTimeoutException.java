package com.checkout.payment.gateway.exception;

/**
 * The acquiring bank did not answer in time. The request may already have been processed, so this
 * failure must never be retried automatically.
 */
public class BankTimeoutException extends BankUnavailableException {

  public BankTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }
}
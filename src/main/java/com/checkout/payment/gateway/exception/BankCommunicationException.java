package com.checkout.payment.gateway.exception;

/** The acquiring bank answered with an unexpected status. */
public class BankCommunicationException extends RuntimeException {

  public BankCommunicationException(String message) {
    super(message);
  }
}
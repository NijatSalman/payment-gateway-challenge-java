package com.checkout.payment.gateway.exception;

public class IdempotencyKeyConflictException extends RuntimeException {

  private final String idempotencyKey;

  public IdempotencyKeyConflictException(String idempotencyKey) {
    super("Idempotency-Key already used with a different request");
    this.idempotencyKey = idempotencyKey;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }
}
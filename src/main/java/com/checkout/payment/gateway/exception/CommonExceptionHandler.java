package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.FieldError;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);
  private static final String RETRY_AFTER_SECONDS = "30";

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleInvalidRequest(MethodArgumentNotValidException ex) {
    List<FieldError> errors = ex.getFieldErrors().stream()
        .map(error -> new FieldError(toSnakeCase(error.getField()), error.getDefaultMessage()))
        .toList();
    return rejected(errors);
  }

  @ExceptionHandler(PaymentValidationException.class)
  public ResponseEntity<ErrorResponse> handleInvalidPayment(PaymentValidationException ex) {
    return rejected(ex.getErrors());
  }

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex) {
    LOG.debug("Payment not found: paymentId={}", ex.getPaymentId());
    return error(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    LOG.info("Invalid request parameter: parameter={}", ex.getName());
    return error(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'");
  }

  @ExceptionHandler(BankUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleBankUnavailable(BankUnavailableException ex) {
    LOG.warn("Acquiring bank unavailable: reason={}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
        .body(ErrorResponse.of("Acquiring bank unavailable, please retry later"));
  }

  @ExceptionHandler(BankCommunicationException.class)
  public ResponseEntity<ErrorResponse> handleBankCommunication(BankCommunicationException ex) {
    LOG.error("Unexpected acquiring bank response: reason={}", ex.getMessage());
    return error(HttpStatus.BAD_GATEWAY, "Unexpected response from acquiring bank");
  }

  private static ResponseEntity<ErrorResponse> rejected(List<FieldError> errors) {
    LOG.info("Payment request rejected: fields={}", errors.stream().map(FieldError::field).toList());
    return ResponseEntity.badRequest().body(ErrorResponse.rejected(errors));
  }

  private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(ErrorResponse.of(message));
  }

  private static String toSnakeCase(String fieldName) {
    return fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
  }
}
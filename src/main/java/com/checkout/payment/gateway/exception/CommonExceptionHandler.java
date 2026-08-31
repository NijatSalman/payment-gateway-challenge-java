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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableBody() {
    LOG.info("Malformed request body");
    return error(HttpStatus.BAD_REQUEST, "Malformed request body");
  }

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex) {
    LOG.debug("Payment not found: paymentId={}", ex.getPaymentId());
    return error(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleUnknownResource(NoResourceFoundException ex) {
    LOG.debug("Resource not found: path={}", ex.getResourcePath());
    return error(HttpStatus.NOT_FOUND, "Resource not found");
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    LOG.info("Invalid request parameter: parameter={}", ex.getName());
    return error(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex) {
    LOG.info("Method not allowed: method={}", ex.getMethod());
    return error(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex) {
    LOG.info("Unsupported media type: contentType={}", ex.getContentType());
    return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type");
  }

  @ExceptionHandler(IdempotencyKeyConflictException.class)
  public ResponseEntity<ErrorResponse> handleIdempotencyKeyConflict(
      IdempotencyKeyConflictException ex) {
    LOG.info("Idempotency key conflict: idempotencyKey={}", ex.getIdempotencyKey());
    return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
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

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    LOG.error("Unexpected error", ex);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
  }

  private static ResponseEntity<ErrorResponse> rejected(List<FieldError> errors) {
    List<String> fields = errors.stream().map(FieldError::field).toList();
    LOG.info("Payment request rejected: fields={}", fields);
    return ResponseEntity.badRequest().body(ErrorResponse.rejected(errors));
  }

  private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(ErrorResponse.of(message));
  }

  private static String toSnakeCase(String fieldName) {
    return fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
  }
}
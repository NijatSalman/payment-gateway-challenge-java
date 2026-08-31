package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.domain.ProcessedPayment;
import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Process card payments and retrieve them by id")
public class PaymentGatewayController {

  static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
  static final String IDEMPOTENT_REPLAYED_HEADER = "Idempotent-Replayed";

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @Operation(summary = "Process a card payment",
      description = "Validates the request and forwards it to the acquiring bank. The outcome is"
          + " Authorized or Declined as decided by the bank; invalid requests are rejected"
          + " without calling the bank. Test bank behaviour depends on the card number's last"
          + " digit: odd = authorized, even = declined, 0 = bank unavailable.")
  @ApiResponse(responseCode = "201", description = "Payment processed (Authorized or Declined)")
  @ApiResponse(responseCode = "400", description = "Invalid request, rejected without a bank call",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "422", description = "Idempotency-Key reused for a different request",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "502", description = "Unexpected response from the acquiring bank",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "503", description = "Acquiring bank unavailable, retry later"
      + " (Retry-After header set)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping
  public ResponseEntity<PaymentResponse> processPayment(
      @Valid @RequestBody PaymentRequest request,
      @Parameter(description = "Optional key making the request safe to retry: the same key"
          + " returns the payment created by the first request instead of charging again."
          + " Letters, digits, '-' and '_', max 255 characters.")
      @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey) {
    ProcessedPayment processed = paymentGatewayService.processPayment(request, idempotencyKey);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(processed.payment().id())
        .toUri();
    return ResponseEntity.created(location)
        .header(IDEMPOTENT_REPLAYED_HEADER, String.valueOf(processed.replayed()))
        .body(PaymentResponse.from(processed.payment()));
  }

  @Operation(summary = "Retrieve a previously made payment by its id")
  @ApiResponse(responseCode = "200", description = "Payment found")
  @ApiResponse(responseCode = "404", description = "No payment with this id",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/{id}")
  public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID id) {
    return ResponseEntity.ok(PaymentResponse.from(paymentGatewayService.getPaymentById(id)));
  }
}
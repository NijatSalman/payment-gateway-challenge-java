package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.domain.ProcessedPayment;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
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
public class PaymentGatewayController {

  static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
  static final String IDEMPOTENT_REPLAYED_HEADER = "Idempotent-Replayed";

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @PostMapping
  public ResponseEntity<PaymentResponse> processPayment(
      @Valid @RequestBody PaymentRequest request,
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

  @GetMapping("/{id}")
  public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID id) {
    return ResponseEntity.ok(PaymentResponse.from(paymentGatewayService.getPaymentById(id)));
  }
}
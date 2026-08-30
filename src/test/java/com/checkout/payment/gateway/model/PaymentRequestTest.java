package com.checkout.payment.gateway.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentRequestTest {

  @Test
  void whenPrintedThenCardNumberAndCvvAreMasked() {
    PaymentRequest request =
        new PaymentRequest("2222405343248877", 4, 2030, "GBP", 100L, "123");

    String text = request.toString();

    assertThat(text)
        .contains("****8877")
        .contains("cvv=***")
        .doesNotContain("2222405343248877")
        .doesNotContain("cvv=123");
  }

  @Test
  void whenCardDataIsMissingThenPrintingDoesNotFail() {
    PaymentRequest request = new PaymentRequest(null, 4, 2030, "GBP", 100L, null);

    assertThat(request.toString()).contains("cardNumber=null").contains("cvv=null");
  }
}
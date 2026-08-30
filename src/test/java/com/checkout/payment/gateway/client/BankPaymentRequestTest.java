package com.checkout.payment.gateway.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.checkout.payment.gateway.model.PaymentRequest;
import org.junit.jupiter.api.Test;

class BankPaymentRequestTest {

  @Test
  void mapsPaymentRequestAndZeroPadsExpiryMonth() {
    BankPaymentRequest request =
        BankPaymentRequest.from(new PaymentRequest("2222405343248877", 4, 2030, "GBP", 100L, "123"));

    assertThat(request)
        .isEqualTo(new BankPaymentRequest("2222405343248877", "04/2030", "GBP", 100, "123"));
  }

  @Test
  void keepsTwoDigitExpiryMonth() {
    BankPaymentRequest request =
        BankPaymentRequest.from(new PaymentRequest("2222405343248877", 12, 2030, "GBP", 100L, "123"));

    assertThat(request.expiryDate()).isEqualTo("12/2030");
  }

  @Test
  void toStringMasksCardNumberAndCvv() {
    String text = new BankPaymentRequest("2222405343248877", "04/2030", "GBP", 100, "123").toString();

    assertThat(text)
        .contains("****8877")
        .contains("cvv=***")
        .doesNotContain("2222405343248877")
        .doesNotContain("cvv=123");
  }
}
package com.checkout.payment.gateway.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.checkout.payment.gateway.configuration.PaymentProperties;
import com.checkout.payment.gateway.exception.PaymentValidationException;
import com.checkout.payment.gateway.model.FieldError;
import com.checkout.payment.gateway.model.PaymentRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PaymentRequestValidatorTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-08-15T10:00:00Z"), ZoneOffset.UTC);

  private final PaymentRequestValidator validator =
      new PaymentRequestValidator(FIXED_CLOCK, new PaymentProperties(Set.of("USD", "EUR", "GBP")));

  @Test
  void cardExpiringInCurrentMonthIsValid() {
    assertThatCode(() -> validator.validate(request(8, 2026, "GBP"))).doesNotThrowAnyException();
  }

  @Test
  void cardExpiringInFutureIsValid() {
    assertThatCode(() -> validator.validate(request(9, 2026, "GBP"))).doesNotThrowAnyException();
    assertThatCode(() -> validator.validate(request(1, 2027, "GBP"))).doesNotThrowAnyException();
  }

  @Test
  void cardExpiredLastMonthIsRejected() {
    assertThatThrownBy(() -> validator.validate(request(7, 2026, "GBP")))
        .isInstanceOf(PaymentValidationException.class)
        .extracting(ex -> ((PaymentValidationException) ex).getErrors())
        .isEqualTo(java.util.List.of(new FieldError("expiry_year", "card has expired")));
  }

  @Test
  void unsupportedCurrencyIsRejected() {
    assertThatThrownBy(() -> validator.validate(request(12, 2030, "XYZ")))
        .isInstanceOf(PaymentValidationException.class)
        .extracting(ex -> ((PaymentValidationException) ex).getErrors())
        .isEqualTo(java.util.List.of(new FieldError("currency", "is not supported")));
  }

  @Test
  void lowercaseCurrencyIsRejected() {
    assertThatThrownBy(() -> validator.validate(request(12, 2030, "gbp")))
        .isInstanceOf(PaymentValidationException.class);
  }

  @Test
  void allBusinessRuleViolationsAreReportedTogether() {
    assertThatThrownBy(() -> validator.validate(request(7, 2026, "XYZ")))
        .isInstanceOf(PaymentValidationException.class)
        .extracting(ex -> ((PaymentValidationException) ex).getErrors())
        .isEqualTo(java.util.List.of(
            new FieldError("currency", "is not supported"),
            new FieldError("expiry_year", "card has expired")));
  }

  private static PaymentRequest request(int expiryMonth, int expiryYear, String currency) {
    return new PaymentRequest("2222405343248877", expiryMonth, expiryYear, currency, 100L,
        "123");
  }
}
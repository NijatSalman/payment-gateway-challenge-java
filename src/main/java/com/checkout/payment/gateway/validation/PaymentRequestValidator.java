package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.configuration.PaymentProperties;
import com.checkout.payment.gateway.exception.PaymentValidationException;
import com.checkout.payment.gateway.model.FieldError;
import com.checkout.payment.gateway.model.PaymentRequest;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Business rules that go beyond the per-field format constraints declared on
 * {@link PaymentRequest}. A card is considered valid until the last day of its expiry month.
 */
@Component
public class PaymentRequestValidator {

  private final Clock clock;
  private final PaymentProperties paymentProperties;

  public PaymentRequestValidator(Clock clock, PaymentProperties paymentProperties) {
    this.clock = clock;
    this.paymentProperties = paymentProperties;
  }

  public void validate(PaymentRequest request) {
    List<FieldError> errors = new ArrayList<>();
    if (!paymentProperties.supportedCurrencies().contains(request.currency())) {
      errors.add(new FieldError("currency", "is not supported"));
    }
    if (isExpired(request.expiryMonth(), request.expiryYear())) {
      errors.add(new FieldError("expiry_year", "card has expired"));
    }
    if (!errors.isEmpty()) {
      throw new PaymentValidationException(errors);
    }
  }

  private boolean isExpired(int expiryMonth, int expiryYear) {
    return YearMonth.of(expiryYear, expiryMonth).isBefore(YearMonth.now(clock));
  }
}
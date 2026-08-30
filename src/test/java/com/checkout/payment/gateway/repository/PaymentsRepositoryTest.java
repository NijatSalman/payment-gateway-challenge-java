package com.checkout.payment.gateway.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.checkout.payment.gateway.domain.Payment;
import com.checkout.payment.gateway.enums.PaymentStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentsRepositoryTest {

  private final PaymentsRepository repository = new PaymentsRepository();

  @Test
  void whenPaymentIsSavedThenItCanBeFoundById() {
    Payment payment = payment(UUID.randomUUID(), PaymentStatus.AUTHORIZED);

    repository.save(payment);

    assertThat(repository.findById(payment.id())).contains(payment);
  }

  @Test
  void whenIdIsUnknownThenEmptyIsReturned() {
    assertThat(repository.findById(UUID.randomUUID())).isEmpty();
  }

  @Test
  void whenSameIdIsSavedAgainThenLatestPaymentIsKept() {
    UUID id = UUID.randomUUID();
    repository.save(payment(id, PaymentStatus.AUTHORIZED));
    Payment latest = payment(id, PaymentStatus.DECLINED);

    repository.save(latest);

    assertThat(repository.findById(id)).contains(latest);
  }

  private static Payment payment(UUID id, PaymentStatus status) {
    return new Payment(id, status, "4321", 12, 2030, "USD", 10, null, Instant.now());
  }
}
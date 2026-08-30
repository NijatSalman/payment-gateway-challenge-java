package com.checkout.payment.gateway.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.domain.Payment;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  private PaymentsRepository paymentsRepository;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    Payment payment = new Payment(UUID.randomUUID(), PaymentStatus.AUTHORIZED, "4321", 12, 2030,
        "USD", 10, "auth-code", Instant.now());
    paymentsRepository.save(payment);

    mvc.perform(get("/api/v1/payments/" + payment.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(payment.id().toString()))
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.card_number_last_four").value("4321"))
        .andExpect(jsonPath("$.expiry_month").value(12))
        .andExpect(jsonPath("$.expiry_year").value(2030))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(10));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(get("/api/v1/payments/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Page not found"));
  }
}
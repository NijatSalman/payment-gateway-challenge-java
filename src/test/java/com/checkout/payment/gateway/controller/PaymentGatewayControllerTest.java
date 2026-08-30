package com.checkout.payment.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.domain.Payment;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.exception.PaymentValidationException;
import com.checkout.payment.gateway.model.FieldError;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentGatewayController.class)
class PaymentGatewayControllerTest {

  private static final String PAYMENTS_URL = "/api/v1/payments";
  private static final String CARD_NUMBER = "2222405343248877";
  private static final String CVV = "123";

  @Autowired
  private MockMvc mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private PaymentGatewayService paymentGatewayService;

  @Nested
  class GetPayment {

    @Test
    void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
      Payment payment = payment(PaymentStatus.AUTHORIZED);
      when(paymentGatewayService.getPaymentById(payment.id())).thenReturn(payment);

      mvc.perform(get(PAYMENTS_URL + "/" + payment.id()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(payment.id().toString()))
          .andExpect(jsonPath("$.status").value("Authorized"))
          .andExpect(jsonPath("$.card_number_last_four").value("8877"))
          .andExpect(jsonPath("$.expiry_month").value(4))
          .andExpect(jsonPath("$.expiry_year").value(2030))
          .andExpect(jsonPath("$.currency").value("GBP"))
          .andExpect(jsonPath("$.amount").value(100));
    }

    @Test
    void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
      UUID unknownId = UUID.randomUUID();
      when(paymentGatewayService.getPaymentById(unknownId))
          .thenThrow(new PaymentNotFoundException(unknownId));

      mvc.perform(get(PAYMENTS_URL + "/" + unknownId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").value("Payment " + unknownId + " not found"));
    }

    @Test
    void whenPaymentIdIsNotAUuidThen400IsReturned() throws Exception {
      mvc.perform(get(PAYMENTS_URL + "/not-a-uuid"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }
  }

  @Nested
  class ProcessPayment {

    @Test
    void whenRequestIsValidThenPaymentIsCreated() throws Exception {
      Payment payment = payment(PaymentStatus.DECLINED);
      when(paymentGatewayService.processPayment(any())).thenReturn(payment);

      mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON).content(validRequest()))
          .andExpect(status().isCreated())
          .andExpect(header().string("Location",
              "http://localhost" + PAYMENTS_URL + "/" + payment.id()))
          .andExpect(jsonPath("$.id").value(payment.id().toString()))
          .andExpect(jsonPath("$.status").value("Declined"))
          .andExpect(jsonPath("$.card_number_last_four").value("8877"))
          .andExpect(jsonPath("$.expiry_month").value(4))
          .andExpect(jsonPath("$.expiry_year").value(2030))
          .andExpect(jsonPath("$.currency").value("GBP"))
          .andExpect(jsonPath("$.amount").value(100));
    }

    @ParameterizedTest(name = "{0}={1} -> {2}")
    @MethodSource("invalidFields")
    void whenFieldIsInvalidThenRequestIsRejected(String field, Object value, String message)
        throws Exception {
      mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON)
              .content(requestWith(field, value)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value("Rejected"))
          .andExpect(jsonPath("$.message").value("Payment request rejected"))
          .andExpect(jsonPath("$.errors", hasSize(1)))
          .andExpect(jsonPath("$.errors[0].field").value(field))
          .andExpect(jsonPath("$.errors[0].message").value(message));

      verifyNoInteractions(paymentGatewayService);
    }

    static Stream<Arguments> invalidFields() {
      return Stream.of(
          Arguments.of("card_number", null, "is required"),
          Arguments.of("card_number", "", "must be 14-19 digits"),
          Arguments.of("card_number", "1234567890123", "must be 14-19 digits"),
          Arguments.of("card_number", "12345678901234567890", "must be 14-19 digits"),
          Arguments.of("card_number", "2222 4053 4324 8877", "must be 14-19 digits"),
          Arguments.of("card_number", "22224053432488ab", "must be 14-19 digits"),
          Arguments.of("expiry_month", null, "is required"),
          Arguments.of("expiry_month", 0, "must be between 1 and 12"),
          Arguments.of("expiry_month", 13, "must be between 1 and 12"),
          Arguments.of("expiry_year", null, "is required"),
          Arguments.of("expiry_year", 0, "must be between 1 and 9999"),
          Arguments.of("expiry_year", 10000, "must be between 1 and 9999"),
          Arguments.of("currency", null, "is required"),
          Arguments.of("currency", "", "must be 3 characters"),
          Arguments.of("currency", "US", "must be 3 characters"),
          Arguments.of("currency", "USDD", "must be 3 characters"),
          Arguments.of("amount", null, "is required"),
          Arguments.of("amount", 0, "must be greater than 0"),
          Arguments.of("amount", -1, "must be greater than 0"),
          Arguments.of("cvv", null, "is required"),
          Arguments.of("cvv", "", "must be 3-4 digits"),
          Arguments.of("cvv", "12", "must be 3-4 digits"),
          Arguments.of("cvv", "12345", "must be 3-4 digits"),
          Arguments.of("cvv", "12a", "must be 3-4 digits"));
    }

    @ParameterizedTest(name = "{0}={1} is accepted")
    @MethodSource("validBoundaryFields")
    void whenFieldIsOnValidBoundaryThenRequestIsAccepted(String field, Object value)
        throws Exception {
      when(paymentGatewayService.processPayment(any())).thenReturn(payment(PaymentStatus.AUTHORIZED));

      mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON)
              .content(requestWith(field, value)))
          .andExpect(status().isCreated());
    }

    static Stream<Arguments> validBoundaryFields() {
      return Stream.of(
          Arguments.of("card_number", "12345678901234"),
          Arguments.of("card_number", "1234567890123456789"),
          Arguments.of("expiry_month", 1),
          Arguments.of("expiry_month", 12),
          Arguments.of("amount", 1),
          Arguments.of("cvv", "1234"));
    }

    @Test
    void whenSeveralFieldsAreInvalidThenAllErrorsAreReported() throws Exception {
      Map<String, Object> request = validRequestFields();
      request.put("card_number", "123");
      request.put("cvv", "1");
      request.remove("amount");

      mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON).content(toJson(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value("Rejected"))
          .andExpect(jsonPath("$.errors", hasSize(3)))
          .andExpect(jsonPath("$.errors[?(@.field == 'card_number')]").exists())
          .andExpect(jsonPath("$.errors[?(@.field == 'cvv')]").exists())
          .andExpect(jsonPath("$.errors[?(@.field == 'amount')]").exists());
    }

    @Test
    void whenBusinessRulesFailThenRequestIsRejected() throws Exception {
      when(paymentGatewayService.processPayment(any())).thenThrow(new PaymentValidationException(
          List.of(new FieldError("currency", "is not supported"),
              new FieldError("expiry_year", "card has expired"))));

      mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON).content(validRequest()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value("Rejected"))
          .andExpect(jsonPath("$.errors", hasSize(2)))
          .andExpect(jsonPath("$.errors[0].field").value("currency"))
          .andExpect(jsonPath("$.errors[0].message").value("is not supported"))
          .andExpect(jsonPath("$.errors[1].field").value("expiry_year"))
          .andExpect(jsonPath("$.errors[1].message").value("card has expired"));
    }

    @Test
    void whenBodyIsMalformedThen400IsReturned() throws Exception {
      mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON).content("{\"card_number\":"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(paymentGatewayService);
    }

    @Test
    void whenRequestIsRejectedThenCardDataIsNotEchoed() throws Exception {
      String invalidCard = "4111111111111";
      String invalidCvv = "12345";
      Map<String, Object> request = validRequestFields();
      request.put("card_number", invalidCard);
      request.put("cvv", invalidCvv);

      String body = mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON)
              .content(toJson(request)))
          .andExpect(status().isBadRequest())
          .andReturn().getResponse().getContentAsString();

      assertThat(body).doesNotContain(invalidCard).doesNotContain(invalidCvv);
    }
  }

  private static Payment payment(PaymentStatus status) {
    return new Payment(UUID.randomUUID(), status, "8877", 4, 2030, "GBP", 100, null,
        Instant.now());
  }

  private String validRequest() throws Exception {
    return toJson(validRequestFields());
  }

  private String requestWith(String field, Object value) throws Exception {
    Map<String, Object> request = validRequestFields();
    request.put(field, value);
    return toJson(request);
  }

  private static Map<String, Object> validRequestFields() {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("card_number", CARD_NUMBER);
    fields.put("expiry_month", 4);
    fields.put("expiry_year", 2030);
    fields.put("currency", "GBP");
    fields.put("amount", 100);
    fields.put("cvv", CVV);
    return fields;
  }

  private String toJson(Map<String, Object> fields) throws Exception {
    return objectMapper.writeValueAsString(fields);
  }
}
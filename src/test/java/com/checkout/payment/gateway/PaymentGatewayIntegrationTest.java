package com.checkout.payment.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Runs the real application end to end; only the acquiring bank's HTTP endpoint is replaced by a
 * scripted server.
 */
@SpringBootTest(properties = "resilience4j.retry.instances.acquiringBank.wait-duration=0ms")
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
class PaymentGatewayIntegrationTest {

  private static final String PAYMENTS_URL = "/api/v1/payments";
  private static final String BANK_URL = "http://localhost:8080/payments";
  private static final String CARD_NUMBER = "2222405343248877";
  private static final String CVV = "123";
  private static final String VALID_REQUEST = """
      {"card_number":"%s","expiry_month":4,"expiry_year":2030,"currency":"GBP","amount":100,
       "cvv":"%s"}""".formatted(CARD_NUMBER, CVV);
  private static final String BANK_AUTHORIZED =
      "{\"authorized\":true,\"authorization_code\":\"0bb07405-6d44-4b50-a14f-7ae0beff13ad\"}";
  private static final String BANK_DECLINED = "{\"authorized\":false,\"authorization_code\":\"\"}";

  @Autowired
  private MockMvc mvc;
  @Autowired
  private MockRestServiceServer bank;

  @AfterEach
  void resetBank() {
    bank.reset();
  }

  @Test
  void whenBankAuthorizesThenPaymentIsCreatedAndRetrievable() throws Exception {
    bank.expect(once(), requestTo(BANK_URL))
        .andRespond(withSuccess(BANK_AUTHORIZED, APPLICATION_JSON));

    String created = mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.card_number_last_four").value("8877"))
        .andReturn().getResponse().getContentAsString();
    String id = JsonPath.read(created, "$.id");

    mvc.perform(get(PAYMENTS_URL + "/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.card_number_last_four").value("8877"))
        .andExpect(jsonPath("$.expiry_month").value(4))
        .andExpect(jsonPath("$.expiry_year").value(2030))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100));
    bank.verify();
  }

  @Test
  void whenBankDeclinesThenPaymentIsCreatedAsDeclined() throws Exception {
    bank.expect(once(), requestTo(BANK_URL))
        .andRespond(withSuccess(BANK_DECLINED, APPLICATION_JSON));

    String created = mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Declined"))
        .andReturn().getResponse().getContentAsString();
    String id = JsonPath.read(created, "$.id");

    mvc.perform(get(PAYMENTS_URL + "/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Declined"));
  }

  @Test
  void whenRequestIsInvalidThenBankIsNotCalled() throws Exception {
    String invalidRequest = VALID_REQUEST.replace("\"currency\":\"GBP\"", "\"currency\":\"XYZ\"");

    mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON).content(invalidRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.errors[0].field").value("currency"));
    bank.verify();
  }

  @Test
  void whenBankStaysUnavailableThenRetriesAreExhaustedAndServiceUnavailableIsReturned()
      throws Exception {
    bank.expect(times(3), requestTo(BANK_URL)).andRespond(withServiceUnavailable());

    mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON).content(VALID_REQUEST))
        .andExpect(status().isServiceUnavailable())
        .andExpect(header().string("Retry-After", "30"))
        .andExpect(jsonPath("$.message").value("Acquiring bank unavailable, please retry later"));
    bank.verify();
  }

  @Test
  void whenBankRecoversWithinRetriesThenPaymentIsAuthorized() throws Exception {
    bank.expect(times(2), requestTo(BANK_URL)).andRespond(withServiceUnavailable());
    bank.expect(once(), requestTo(BANK_URL))
        .andRespond(withSuccess(BANK_AUTHORIZED, APPLICATION_JSON));

    mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON).content(VALID_REQUEST))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Authorized"));
    bank.verify();
  }

  @Test
  void whenBankTimesOutThenBankIsNotRetried() throws Exception {
    bank.expect(once(), requestTo(BANK_URL)).andRespond(request -> {
      throw new HttpTimeoutException("read timed out");
    });

    mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON).content(VALID_REQUEST))
        .andExpect(status().isServiceUnavailable())
        .andExpect(header().string("Retry-After", "30"));
    bank.verify();
  }

  @Test
  void whenBankRespondsUnexpectedlyThenBadGatewayIsReturnedWithoutRetry() throws Exception {
    bank.expect(once(), requestTo(BANK_URL)).andRespond(withBadRequest());

    mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON).content(VALID_REQUEST))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Unexpected response from acquiring bank"));
    bank.verify();
  }

  @Test
  void whenSameIdempotencyKeyIsRepeatedThenPaymentIsReplayed() throws Exception {
    bank.expect(once(), requestTo(BANK_URL))
        .andRespond(withSuccess(BANK_AUTHORIZED, APPLICATION_JSON));

    String first = mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON)
            .header("Idempotency-Key", "order-1").content(VALID_REQUEST))
        .andExpect(status().isCreated())
        .andExpect(header().string("Idempotent-Replayed", "false"))
        .andReturn().getResponse().getContentAsString();

    String second = mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON)
            .header("Idempotency-Key", "order-1").content(VALID_REQUEST))
        .andExpect(status().isCreated())
        .andExpect(header().string("Idempotent-Replayed", "true"))
        .andReturn().getResponse().getContentAsString();

    assertThat(second).isEqualTo(first);
    bank.verify();
  }

  @Test
  void whenIdempotencyKeyIsReusedWithDifferentRequestThen422IsReturned() throws Exception {
    bank.expect(once(), requestTo(BANK_URL))
        .andRespond(withSuccess(BANK_AUTHORIZED, APPLICATION_JSON));
    mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON)
            .header("Idempotency-Key", "order-2").content(VALID_REQUEST))
        .andExpect(status().isCreated());

    String differentAmount = VALID_REQUEST.replace("\"amount\":100", "\"amount\":500");
    mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON)
            .header("Idempotency-Key", "order-2").content(differentAmount))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message")
            .value("Idempotency-Key already used with a different request"));
    bank.verify();
  }

  @Test
  void whenIdempotencyKeyIsInvalidThenRequestIsRejectedWithoutBankCall() throws Exception {
    mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON)
            .header("Idempotency-Key", "bad key!").content(VALID_REQUEST))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.errors[0].field").value("Idempotency-Key"));
    bank.verify();
  }

  @Test
  void whenBankFailedThenSameIdempotencyKeyCanBeRetried() throws Exception {
    bank.expect(times(3), requestTo(BANK_URL)).andRespond(withServiceUnavailable());
    bank.expect(once(), requestTo(BANK_URL))
        .andRespond(withSuccess(BANK_AUTHORIZED, APPLICATION_JSON));

    mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON)
            .header("Idempotency-Key", "order-3").content(VALID_REQUEST))
        .andExpect(status().isServiceUnavailable());

    mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON)
            .header("Idempotency-Key", "order-3").content(VALID_REQUEST))
        .andExpect(status().isCreated())
        .andExpect(header().string("Idempotent-Replayed", "false"));
    bank.verify();
  }

  @Test
  void whenNoIdempotencyKeyThenEachRequestCreatesANewPayment() throws Exception {
    bank.expect(times(2), requestTo(BANK_URL))
        .andRespond(withSuccess(BANK_AUTHORIZED, APPLICATION_JSON));

    String first = mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andReturn().getResponse().getContentAsString();
    String second = mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andReturn().getResponse().getContentAsString();

    assertThat(JsonPath.<String>read(first, "$.id")).isNotEqualTo(JsonPath.read(second, "$.id"));
    bank.verify();
  }

  @Test
  void whenHealthIsRequestedThenApplicationIsUp() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void whenPaymentIsProcessedThenCardDataIsNeverExposed() throws Exception {
    bank.expect(once(), requestTo(BANK_URL))
        .andRespond(withSuccess(BANK_AUTHORIZED, APPLICATION_JSON));

    String created = mvc.perform(post(PAYMENTS_URL).contentType(APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andReturn().getResponse().getContentAsString();
    String id = JsonPath.read(created, "$.id");
    String retrieved = mvc.perform(get(PAYMENTS_URL + "/" + id))
        .andReturn().getResponse().getContentAsString();

    assertThat(created).doesNotContain(CARD_NUMBER).doesNotContain("\"cvv\"");
    assertThat(retrieved).doesNotContain(CARD_NUMBER).doesNotContain("\"cvv\"");
  }
}
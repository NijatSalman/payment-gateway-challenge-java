package com.checkout.payment.gateway.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.checkout.payment.gateway.configuration.ApplicationConfiguration;
import com.checkout.payment.gateway.exception.BankCommunicationException;
import com.checkout.payment.gateway.exception.BankUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@RestClientTest(AcquiringBankClient.class)
@Import(ApplicationConfiguration.class)
class AcquiringBankClientTest {

  private static final String BANK_URL = "http://localhost:8080/payments";
  private static final BankPaymentRequest REQUEST =
      new BankPaymentRequest("2222405343248877", "04/2030", "GBP", 100, "123");
  private static final String EXPECTED_REQUEST_JSON = """
      {"card_number":"2222405343248877","expiry_date":"04/2030","currency":"GBP",
       "amount":100,"cvv":"123"}""";

  @Autowired
  private AcquiringBankClient client;
  @Autowired
  private MockRestServiceServer server;

  @Test
  void whenBankAuthorizesThenAuthorizedResponseIsReturned() {
    server.expect(requestTo(BANK_URL))
        .andExpect(method(POST))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(EXPECTED_REQUEST_JSON, JsonCompareMode.STRICT))
        .andRespond(withSuccess(
            "{\"authorized\":true,\"authorization_code\":\"0bb07405-6d44-4b50-a14f-7ae0beff13ad\"}",
            MediaType.APPLICATION_JSON));

    BankPaymentResponse response = client.authorize(REQUEST);

    assertThat(response.authorized()).isTrue();
    assertThat(response.authorizationCode()).isEqualTo("0bb07405-6d44-4b50-a14f-7ae0beff13ad");
    server.verify();
  }

  @Test
  void whenBankDeclinesThenDeclinedResponseIsReturned() {
    server.expect(requestTo(BANK_URL))
        .andRespond(withSuccess("{\"authorized\":false,\"authorization_code\":\"\"}",
            MediaType.APPLICATION_JSON));

    BankPaymentResponse response = client.authorize(REQUEST);

    assertThat(response.authorized()).isFalse();
    assertThat(response.authorizationCode()).isEmpty();
  }

  @Test
  void whenBankRespondsWith503ThenBankUnavailableIsThrown() {
    server.expect(requestTo(BANK_URL)).andRespond(withServiceUnavailable());

    assertThatThrownBy(() -> client.authorize(REQUEST))
        .isInstanceOf(BankUnavailableException.class)
        .hasMessageContaining("503");
  }

  @Test
  void whenBankRespondsWith400ThenCommunicationFailureIsThrown() {
    server.expect(requestTo(BANK_URL)).andRespond(withBadRequest());

    assertThatThrownBy(() -> client.authorize(REQUEST))
        .isInstanceOf(BankCommunicationException.class)
        .hasMessageContaining("400");
  }

  @Test
  void whenBankRespondsWith500ThenCommunicationFailureIsThrown() {
    server.expect(requestTo(BANK_URL)).andRespond(withServerError());

    assertThatThrownBy(() -> client.authorize(REQUEST))
        .isInstanceOf(BankCommunicationException.class)
        .hasMessageContaining("500");
  }

  @Test
  void whenBankIsUnreachableThenBankUnavailableIsThrown() {
    AcquiringBankClient unreachableClient =
        new AcquiringBankClient(RestClient.builder().baseUrl("http://localhost:1").build());

    assertThatThrownBy(() -> unreachableClient.authorize(REQUEST))
        .isInstanceOf(BankUnavailableException.class)
        .hasMessageContaining("unreachable");
  }
}
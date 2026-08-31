package com.checkout.payment.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * Runs the application against the REAL bank simulator (the repo's Mountebank configuration),
 * proving the wire contract. Requires Docker; excluded from './gradlew test', run with
 * './gradlew e2eTest'.
 */
@Tag("e2e")
@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BankSimulatorE2ETest {

  private static final int BANK_PORT = 8080;

  @Container
  private static final GenericContainer<?> BANK_SIMULATOR =
      new GenericContainer<>("bbyars/mountebank:2.8.1")
          .withCopyFileToContainer(MountableFile.forHostPath("imposters/bank_simulator.ejs"),
              "/imposters/bank_simulator.ejs")
          .withCommand("--configfile", "/imposters/bank_simulator.ejs", "--allowInjection")
          .withExposedPorts(BANK_PORT);

  @DynamicPropertySource
  static void bankUrl(DynamicPropertyRegistry registry) {
    registry.add("acquiring-bank.url", () -> "http://" + BANK_SIMULATOR.getHost() + ":"
        + BANK_SIMULATOR.getMappedPort(BANK_PORT) + "/payments");
  }

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void whenCardEndsWithOddDigitThenPaymentIsAuthorizedAndRetrievable() {
    ResponseEntity<Map<String, Object>> created = processPayment("2222405343248871");

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).containsEntry("status", "Authorized");
    assertThat(created.getBody()).containsEntry("card_number_last_four", "8871");

    ResponseEntity<Map<String, Object>> retrieved = getPayment(created.getHeaders().getLocation());

    assertThat(retrieved.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(retrieved.getBody()).isEqualTo(created.getBody());
  }

  @Test
  void whenCardEndsWithEvenDigitThenPaymentIsDeclined() {
    ResponseEntity<Map<String, Object>> response = processPayment("2222405343248872");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).containsEntry("status", "Declined");
  }

  @Test
  void whenCardEndsWithZeroThenServiceUnavailableIsReturned() {
    ResponseEntity<Map<String, Object>> response = processPayment("2222405343248870");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("30");
  }

  private ResponseEntity<Map<String, Object>> getPayment(java.net.URI location) {
    return restTemplate.exchange(location, org.springframework.http.HttpMethod.GET, null,
        new org.springframework.core.ParameterizedTypeReference<>() {
        });
  }

  private ResponseEntity<Map<String, Object>> processPayment(String cardNumber) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String request = """
        {"card_number":"%s","expiry_month":4,"expiry_year":2030,"currency":"GBP",
         "amount":100,"cvv":"123"}""".formatted(cardNumber);
    return restTemplate.exchange("/api/v1/payments", org.springframework.http.HttpMethod.POST,
        new HttpEntity<>(request, headers),
        new org.springframework.core.ParameterizedTypeReference<>() {
        });
  }
}
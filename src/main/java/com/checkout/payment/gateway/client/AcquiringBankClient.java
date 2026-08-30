package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.BankCommunicationException;
import com.checkout.payment.gateway.exception.BankUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class AcquiringBankClient {

  private final RestClient restClient;

  public AcquiringBankClient(RestClient acquiringBankRestClient) {
    this.restClient = acquiringBankRestClient;
  }

  public BankPaymentResponse authorize(BankPaymentRequest request) {
    try {
      return restClient.post()
          .contentType(MediaType.APPLICATION_JSON)
          .body(request)
          .retrieve()
          .onStatus(status -> status == HttpStatus.SERVICE_UNAVAILABLE, (req, res) -> {
            throw new BankUnavailableException("Acquiring bank responded with 503");
          })
          .onStatus(HttpStatusCode::isError, (req, res) -> {
            throw new BankCommunicationException(
                "Acquiring bank responded with " + res.getStatusCode().value());
          })
          .body(BankPaymentResponse.class);
    } catch (ResourceAccessException ex) {
      throw new BankUnavailableException("Acquiring bank unreachable", ex);
    }
  }
}
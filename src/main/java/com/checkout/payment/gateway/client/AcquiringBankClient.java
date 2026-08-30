package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.BankCommunicationException;
import com.checkout.payment.gateway.exception.BankTimeoutException;
import com.checkout.payment.gateway.exception.BankUnavailableException;
import io.github.resilience4j.retry.annotation.Retry;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
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

  /** Retried while the bank is unavailable (see {@code resilience4j.retry} configuration). */
  @Retry(name = "acquiringBank")
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
      if (isReadTimeout(ex)) {
        throw new BankTimeoutException("Acquiring bank did not respond in time", ex);
      }
      throw new BankUnavailableException("Acquiring bank unreachable", ex);
    }
  }

  /** A timeout after the request was sent; a connect timeout means it never left. */
  private static boolean isReadTimeout(ResourceAccessException ex) {
    Throwable cause = ex.getCause();
    return cause instanceof HttpTimeoutException && !(cause instanceof HttpConnectTimeoutException);
  }
}
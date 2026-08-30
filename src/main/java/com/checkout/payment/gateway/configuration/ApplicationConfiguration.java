package com.checkout.payment.gateway.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({PaymentProperties.class, AcquiringBankProperties.class})
public class ApplicationConfiguration {

  @Bean
  public RestClient acquiringBankRestClient(RestClient.Builder builder,
      AcquiringBankProperties acquiringBankProperties) {
    return builder.baseUrl(acquiringBankProperties.url()).build();
  }
}
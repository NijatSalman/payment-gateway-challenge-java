package com.checkout.payment.gateway.configuration;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(Set<String> supportedCurrencies) {
}
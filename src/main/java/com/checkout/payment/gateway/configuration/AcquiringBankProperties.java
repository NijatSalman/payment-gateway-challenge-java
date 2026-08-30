package com.checkout.payment.gateway.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "acquiring-bank")
public record AcquiringBankProperties(String url) {
}
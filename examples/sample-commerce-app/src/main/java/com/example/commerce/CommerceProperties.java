package com.example.commerce;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "commerce")
public record CommerceProperties(String paymentBaseUrl, int minimumOrderCents) { }

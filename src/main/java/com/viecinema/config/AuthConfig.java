package com.viecinema.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "security.jwt")
@Data
public class AuthConfig {
    // Secret keys
    @Value("${security.jwt.access-token-secret}")
    private String accessTokenSecret;

    @Value("${security.jwt.refresh-token-secret}")
    private String refreshTokenSecret;

    // Expiration times
    @Value("${security.jwt.access-token-expiration}")
    private Duration accessTokenExpire;

    @Value("${security.jwt.refresh-token-expiration}")
    private Duration refreshTokenExpire;
}


package com.viecinema.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "vnpay")
public class VnpayConfig {
    private String tmnCode;
    private String hashSecret;
    private String apiUrl;
    private String returnUrl;
    private String ipnUrl;
    private Integer timeout;
    private String version;
    private String command;
    private String currencyCode;
    private String locale;
}

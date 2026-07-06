package com.apigateway.tenant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtConfig {

    private String secret;
    private long accessTokenExpiryMs;
    private long refreshTokenExpiryMs;
}

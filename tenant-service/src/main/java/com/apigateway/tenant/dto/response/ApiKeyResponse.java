package com.apigateway.tenant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {

    private UUID id;
    private String name;
    private String keyPrefix;
    private Integer rateLimit;
    private Integer rateWindowSeconds;
    private boolean active;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    // Only returned ONCE at creation — never again
    // null in all other responses
    private String fullKey;
}
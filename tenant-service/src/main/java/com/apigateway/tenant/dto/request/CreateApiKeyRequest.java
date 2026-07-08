package com.apigateway.tenant.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateApiKeyRequest {

    @NotBlank(message = "API key name is required")
    @Size(min = 2, max = 50,
            message = "Name must be between 2 and 50 characters")
    private String name;

    @Min(value = 1, message = "Rate limit must be at least 1")
    @Max(value = 100000, message = "Rate limit cannot exceed 100000")
    private Integer rateLimit;

    @Min(value = 1, message = "Rate window must be at least 1 second")
    @Max(value = 3600, message = "Rate window cannot exceed 3600 seconds")
    private Integer rateWindowSeconds;
}
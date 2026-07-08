package com.apigateway.tenant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTenantRequest {

    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 100,
            message = "Organization name must be between 2 and 100 characters")
    private String name;
}
package com.apigateway.tenant.controller;

import com.apigateway.tenant.document.AuditLog;
import com.apigateway.tenant.dto.response.PagedResponse;
import com.apigateway.tenant.service.AuditService;
import com.apigateway.tenant.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final SecurityUtils securityUtils;

    @GetMapping("/logs")
    public ResponseEntity<PagedResponse<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action) {

        String tenantId = securityUtils.getCurrentUser()
                .getTenant().getId().toString();

        Page<AuditLog> auditPage = auditService
                .getAuditLogs(tenantId, action, page, size);

        return ResponseEntity.ok(
                PagedResponse.<AuditLog>builder()
                        .content(auditPage.getContent())
                        .page(auditPage.getNumber())
                        .size(auditPage.getSize())
                        .totalElements(auditPage.getTotalElements())
                        .totalPages(auditPage.getTotalPages())
                        .first(auditPage.isFirst())
                        .last(auditPage.isLast())
                        .build()
        );
    }
}
package com.apigateway.tenant.controller;

import com.apigateway.tenant.document.AuditLog;
import com.apigateway.tenant.service.AuditService;
import com.apigateway.tenant.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final SecurityUtils securityUtils;

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        String tenantId = securityUtils.getCurrentUser()
                .getTenant().getId().toString();
        return ResponseEntity.ok(
                auditService.getAuditLogs(tenantId));
    }
}
package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ==================== Analytics ====================

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnalyticsStatsResponse> getAnalyticsStats() {
        return ResponseEntity.ok(adminService.getAnalyticsStats());
    }

    // ==================== Audit Logs ====================

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs() {
        return ResponseEntity.ok(adminService.getAuditLogs());
    }

    // ==================== Workload ====================

    @GetMapping("/workload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WorkloadResponse>> getWorkload() {
        return ResponseEntity.ok(adminService.getWorkload());
    }

}

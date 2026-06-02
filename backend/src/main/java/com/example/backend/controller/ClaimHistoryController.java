package com.example.backend.controller;

import com.example.backend.dto.ClaimHistoryResponse;
import com.example.backend.service.ClaimHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims/{claimId}/history")
@RequiredArgsConstructor
public class ClaimHistoryController {

    private final ClaimHistoryService claimHistoryService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClaimHistoryResponse>> getClaimHistory(@PathVariable String claimId) {
        return ResponseEntity.ok(claimHistoryService.getClaimHistory(claimId));
    }
}

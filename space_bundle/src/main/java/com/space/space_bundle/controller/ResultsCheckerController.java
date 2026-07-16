package com.space.space_bundle.controller;

import com.space.space_bundle.dto.checkerport.CheckerPortArcRequest;
import com.space.space_bundle.dto.checkerport.CheckerPortCorrectionRequest;
import com.space.space_bundle.dto.checkerport.CheckerPortVoucherRequest;
import com.space.space_bundle.entity.ResultCheckerPricing;
import com.space.space_bundle.entity.ResultsTransaction;
import com.space.space_bundle.service.ResultsCheckerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.space.space_bundle.security.CustomUserDetailsService.CustomUserDetails;

@RestController
@RequestMapping("/results-checker")
@RequiredArgsConstructor
public class ResultsCheckerController {

    private final ResultsCheckerService resultsCheckerService;

    // Ideally extract userId from SecurityContext, for now we can pass null or parse it
    @PostMapping("/vouchers")
    public ResponseEntity<ResultsTransaction> buyVoucher(
            @RequestBody CheckerPortVoucherRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails != null ? userDetails.getUserId() : null;
        return ResponseEntity.ok(resultsCheckerService.buyVoucher(request, userId));
    }

    @PostMapping("/checks")
    public ResponseEntity<ResultsTransaction> checkResult(
            @RequestBody CheckerPortArcRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails != null ? userDetails.getUserId() : null;
        return ResponseEntity.ok(resultsCheckerService.checkResult(request, userId));
    }

    @PostMapping("/checks/{referenceId}/correction")
    public ResponseEntity<ResultsTransaction> submitCorrection(
            @PathVariable String referenceId,
            @RequestBody CheckerPortCorrectionRequest request) {
        return ResponseEntity.ok(resultsCheckerService.submitCorrection(referenceId, request));
    }

    @GetMapping("/transactions/{referenceId}")
    public ResponseEntity<ResultsTransaction> getTransaction(@PathVariable String referenceId) {
        return resultsCheckerService.getTransaction(referenceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/transactions")
    public ResponseEntity<Iterable<ResultsTransaction>> getUserTransactions(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.space.space_bundle.security.CustomUserDetailsService.CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUserId() == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(resultsCheckerService.getTransactionsByUserId(userDetails.getUserId()));
    }

    @GetMapping("/pricing")
    public ResponseEntity<Iterable<ResultCheckerPricing>> getPricing() {
        return ResponseEntity.ok(resultsCheckerService.getAllPricing());
    }

    @GetMapping("/my-ip")
    public ResponseEntity<java.util.Map<String, String>> getServerIp() {
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String ip = restTemplate.getForObject("https://api.ipify.org", String.class);
            return ResponseEntity.ok(java.util.Map.of("ip", ip, "message", "Provide this IP to Moolre for whitelisting."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Failed to fetch IP: " + e.getMessage()));
        }
    }
}

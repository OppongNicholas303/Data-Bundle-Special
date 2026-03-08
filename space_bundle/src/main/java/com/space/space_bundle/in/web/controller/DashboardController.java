package com.space.space_bundle.in.web.controller;

import com.space.space_bundle.core.services.DashboardService;
import com.space.space_bundle.in.web.dto.ApiResponse;
import com.space.space_bundle.in.web.dto.DashboardStats;
import com.space.space_bundle.out.security.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardStats>> getDashboardStats(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        
        String userId = userDetails.getUserId();
        log.info("Dashboard stats request for user: {}", userId);
        
        DashboardStats stats = dashboardService.getStats(userId);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

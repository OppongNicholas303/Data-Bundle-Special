package com.space.space_bundle.controller;

import com.space.space_bundle.dto.AdminMashupPackageResponse;
import com.space.space_bundle.dto.ApiResponse;
import com.space.space_bundle.dto.SetBundlePriceRequest;
import com.space.space_bundle.entity.MashupBundle;
import com.space.space_bundle.service.MashupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/mashup")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMashupController {

    private final MashupService mashupService;

    @GetMapping("/packages")
    public ResponseEntity<ApiResponse<List<AdminMashupPackageResponse>>> getPackages() {
        return ResponseEntity.ok(ApiResponse.success(mashupService.getAdminPackages()));
    }

    @PostMapping("/packages/sync")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncPackages() {
        return ResponseEntity.ok(ApiResponse.success("Mashup packages synced", mashupService.syncFromExternal()));
    }

    @PutMapping("/packages/{id}/price")
    public ResponseEntity<ApiResponse<AdminMashupPackageResponse>> setPrice(
            @PathVariable String id,
            @RequestBody SetBundlePriceRequest request) {
        MashupBundle bundle = mashupService.setSellingPrice(id, request.getSellingPrice());
        return ResponseEntity.ok(ApiResponse.success("Mashup selling price updated",
                AdminMashupPackageResponse.from(bundle)));
    }

    @PutMapping("/packages/{id}/status")
    public ResponseEntity<ApiResponse<AdminMashupPackageResponse>> setStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        MashupBundle bundle = mashupService.setStatus(id, body.get("status"));
        return ResponseEntity.ok(ApiResponse.success("Mashup status updated",
                AdminMashupPackageResponse.from(bundle)));
    }
}

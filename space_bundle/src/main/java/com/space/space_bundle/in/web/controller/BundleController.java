package com.space.space_bundle.in.web.controller;

import com.space.space_bundle.core.entities.Bundle;
import com.space.space_bundle.core.services.BundleService;
import com.space.space_bundle.in.web.dto.ApiResponse;
import com.space.space_bundle.in.web.dto.CreateBundleRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bundle REST Controller
 * Handles bundle catalog operations
 */
@Slf4j
@RestController
@RequestMapping("/bundles")
@RequiredArgsConstructor
public class BundleController {

    private final BundleService bundleService;

    /**
     * GET /api/bundles/network/{network} - Get active bundles for network
     */
    @GetMapping("/network/{network}")
    public ResponseEntity<ApiResponse<List<Bundle>>> getNetworkBundles(@PathVariable String network) {
        
        log.info("Bundle catalog request for network: {}", network);
        
        List<Bundle> bundles = bundleService.getActiveNetworkBundles(network);
        
        log.info("Bundle catalog retrieved: network={}, count={}", network, bundles.size());
        
        return ResponseEntity.ok(ApiResponse.success(bundles));
    }

    /**
     * GET /api/bundles/{code} - Get bundle by code
     */
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<Bundle>> getBundleByCode(@PathVariable String code) {
        
        log.info("Bundle lookup request: code={}", code);
        
        Bundle bundle = bundleService.getBundleByCode(code);
        
        log.info("Bundle found: code={}, price={}", code, bundle.getSellingPrice());
        
        return ResponseEntity.ok(ApiResponse.success(bundle));
    }

    /**
     * POST /api/bundles - Create new bundle (Admin only)
     */
    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Bundle>> createBundle(@RequestBody CreateBundleRequest request) {
        
        log.info("Bundle creation request: code={}, network={}", request.getCode(), request.getNetwork());
        
        Bundle bundle = bundleService.createBundle(
                request.getCode(),
                request.getName(),
                request.getDataSize(),
                request.getNetwork(),
                request.getCostPrice(),
                request.getSellingPrice(),
                request.getDescription()
        );
        
        log.info("Bundle created successfully: id={}, code={}", bundle.getId(), bundle.getCode());
        
        return ResponseEntity.ok(ApiResponse.success(bundle));
    }
}
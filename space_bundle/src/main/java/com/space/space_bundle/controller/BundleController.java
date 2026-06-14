package com.space.space_bundle.controller;

import com.space.space_bundle.dto.MashupBundleRequest;
import com.space.space_bundle.entity.Bundle;
import com.space.space_bundle.dto.ApiResponse;
import com.space.space_bundle.dto.CreateBundleRequest;
import com.space.space_bundle.entity.MashupBundle;
import com.space.space_bundle.service.BundleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bundles")
@RequiredArgsConstructor
public class BundleController {

    private final BundleService bundleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Bundle>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(bundleService.getAll()));
    }

    @GetMapping("/network/{network}")
    public ResponseEntity<ApiResponse<List<Bundle>>> getByNetwork(@PathVariable String network) {
        return ResponseEntity.ok(ApiResponse.success(bundleService.getActiveByNetwork(network)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Bundle>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(bundleService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Bundle>> create(@RequestBody CreateBundleRequest request) {
        Bundle bundle = bundleService.create(request.getCode(), request.getName(),
                request.getDataSize(), request.getNetwork(),
                request.getCostPrice(), request.getSellingPrice(), request.getDescription());
        return ResponseEntity.ok(ApiResponse.success("Bundle created", bundle));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Bundle>> update(@PathVariable String id,
                                                      @RequestBody CreateBundleRequest request) {
        Bundle updated = bundleService.update(id,
                request.getName(), request.getDataSize(),
                request.getCostPrice(), request.getSellingPrice(), request.getDescription());
        return ResponseEntity.ok(ApiResponse.success("Bundle updated", updated));
    }

    @PostMapping("/create-mashup")

//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MashupBundle>> create(@RequestBody @Valid MashupBundleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Mashup bundle created", bundleService.createMashupBundle(request)));
    }
}

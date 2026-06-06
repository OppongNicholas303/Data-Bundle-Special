package com.space.space_bundle.controller;

import com.space.space_bundle.feature.FeatureFlag;
import com.space.space_bundle.feature.FeatureFlagService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/feature-flags")
@RequiredArgsConstructor
public class FeatureFlagController {

    private final FeatureFlagService service;

    @GetMapping
    public ResponseEntity<List<FeatureFlag>> list() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/{key}")
    public ResponseEntity<Boolean> get(@PathVariable String key) {
        boolean enabled = service.isEnabled(key, true);
        return ResponseEntity.ok(enabled);
    }

    @PutMapping("/{key}")
    public ResponseEntity<FeatureFlag> set(@PathVariable String key, @RequestBody ToggleRequest req) {
        FeatureFlag updated = service.setFlag(key, req.isEnabled());
        return ResponseEntity.ok(updated);
    }

    @Data
    static class ToggleRequest {
        private boolean enabled;
    }
}


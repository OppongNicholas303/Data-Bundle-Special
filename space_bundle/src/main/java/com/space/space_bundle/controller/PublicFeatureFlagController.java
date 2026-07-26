package com.space.space_bundle.controller;

import com.space.space_bundle.feature.FeatureFlag;
import com.space.space_bundle.feature.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public/feature-flags")
@RequiredArgsConstructor
public class PublicFeatureFlagController {

    private final FeatureFlagService service;

    @GetMapping
    public ResponseEntity<Map<String, Boolean>> getPublicFlags() {
        List<FeatureFlag> flags = service.listAll();
        Map<String, Boolean> flagMap = flags.stream()
                .collect(Collectors.toMap(FeatureFlag::getKey, FeatureFlag::isEnabled));
        
        // Provide defaults if not set
        flagMap.putIfAbsent("payment.moolre.enabled", true);
        flagMap.putIfAbsent("payment.paystack.enabled", false);
        
        return ResponseEntity.ok(flagMap);
    }
}

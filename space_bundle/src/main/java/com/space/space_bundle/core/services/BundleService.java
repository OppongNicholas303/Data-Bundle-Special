package com.space.space_bundle.core.services;

import com.space.space_bundle.core.entities.Bundle;
import com.space.space_bundle.core.port.out.BundleRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class BundleService {

    private final BundleRepositoryPort bundleRepository;

    public Bundle createBundle(String code, String name, String dataSize, String network, 
                              BigDecimal costPrice, BigDecimal sellingPrice, String description) {
        Bundle bundle = Bundle.builder()
                .id(UUID.randomUUID().toString())
                .code(code)
                .name(name)
                .dataSize(dataSize)
                .network(network)
                .costPrice(costPrice)
                .sellingPrice(sellingPrice)
                .status(Bundle.BundleStatus.ACTIVE)
                .description(description)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return bundleRepository.save(bundle);
    }

    public Bundle getBundleByCode(String code) {
        return bundleRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Bundle not found: " + code));
    }

    public List<Bundle> getActiveNetworkBundles(String network) {
        return bundleRepository.findActiveByNetwork(network);
    }

    public BigDecimal getBundlePrice(String bundleCode) {
        Bundle bundle = getBundleByCode(bundleCode);
        if (bundle.getStatus() != Bundle.BundleStatus.ACTIVE) {
            throw new IllegalStateException("Bundle is not active: " + bundleCode);
        }
        return bundle.getSellingPrice();
    }
}
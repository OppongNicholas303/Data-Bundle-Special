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
        List<Bundle> bundles = bundleRepository.findByCode(code);
        
        if (bundles.isEmpty()) {
            throw new IllegalArgumentException("Bundle not found: " + code);
        }
        
        if (bundles.size() > 1) {
            // Return the first active bundle if multiple exist
            return bundles.stream()
                    .filter(b -> b.getStatus() == Bundle.BundleStatus.ACTIVE)
                    .findFirst()
                    .orElse(bundles.get(0));
        }
        
        return bundles.get(0);
    }

    public Bundle getBundleByCodeAndNetwork(String code, String network) {
        return bundleRepository.findByCodeAndNetwork(code, network)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Bundle not found: " + code + " for network: " + network));
    }

    public List<Bundle> getActiveNetworkBundles(String network) {
        return bundleRepository.findActiveByNetwork(network);
    }

    public List<Bundle> getAllBundles() {
        return bundleRepository.findAll();
    }

    public BigDecimal getBundlePrice(String bundleCode, String network) {
        Bundle bundle = getBundleByCodeAndNetwork(bundleCode, network);
        if (bundle.getStatus() != Bundle.BundleStatus.ACTIVE) {
            throw new IllegalStateException("Bundle is not active: " + bundleCode);
        }
        return bundle.getSellingPrice();
    }
}
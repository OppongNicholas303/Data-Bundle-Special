package com.space.space_bundle.core.services;

import com.space.space_bundle.core.entities.Bundle;
import com.space.space_bundle.core.entities.BundlePrice;
import com.space.space_bundle.core.port.out.AutomationPort;
import com.space.space_bundle.core.port.out.BundlePricePort;
import com.space.space_bundle.core.port.out.BundleRepositoryPort;
import com.space.space_bundle.core.port.out.dto.PackageDto;
import com.space.space_bundle.in.web.dto.BundleWithPriceDto;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class BundleService {

    private final BundleRepositoryPort bundleRepository;
    private final AutomationPort automationPort;
    private final BundlePricePort bundlePriceRepository;

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
//
//    public List<PackageDto> getAllBundles() {
//        return automationPort.getBundlePackage();
////        return bundleRepository.findAll();
//    }

    public List<BundleWithPriceDto> getAllBundles() {
        List<PackageDto> packages = automationPort.getBundlePackage();
        List<BundlePrice> allPrices = bundlePriceRepository.findAll();
        
        // Create a map for O(1) lookup
        var priceMap = allPrices.stream()
                .collect(java.util.stream.Collectors.toMap(
                    bp -> bp.getName().toLowerCase(),
                    bp -> bp,
                    (existing, replacement) -> existing
                ));

        return packages.stream()
                .map(pkg -> {
                    BundlePrice bundlePrice = priceMap.get(pkg.name().toLowerCase());
                    return new BundleWithPriceDto(
                            pkg.id(),
                            pkg.name(),
                            pkg.size(),
                            pkg.network(),
                            pkg.validityDays(),
                            pkg.costPrice(),
                            bundlePrice != null ? bundlePrice.getSellingPrice() : null
                    );
                })
                .toList();
    }

    public BigDecimal getBundlePrice(String bundleCode) {
        log.info("Looking up bundle price for bundleCode: {}", bundleCode);
        BundlePrice bundlePrice = bundlePriceRepository.findByName(bundleCode)
                .orElseThrow(() -> {
                    log.error("Bundle price not found for bundleCode: {}", bundleCode);
                    return new IllegalArgumentException("Bundle price not found for bundleCode: " + bundleCode + ". Please create bundle price first.");
                });
        log.info("Found bundle price: {} for bundleCode: {}", bundlePrice.getSellingPrice(), bundleCode);
        return bundlePrice.getSellingPrice();
    }

    public BundlePrice createBundlePrice(Long packageId, java.math.BigDecimal sellingPrice, String name) {
        com.space.space_bundle.core.entities.BundlePrice bp = com.space.space_bundle.core.entities.BundlePrice.builder()
                .packageId(packageId)
                .sellingPrice(sellingPrice)
                .name(name)
                .build();

        return bundlePriceRepository.save(bp);
    }
}
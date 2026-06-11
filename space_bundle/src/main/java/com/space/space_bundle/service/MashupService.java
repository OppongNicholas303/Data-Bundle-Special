package com.space.space_bundle.service;

import com.space.space_bundle.dto.AdminMashupPackageResponse;
import com.space.space_bundle.dto.ExternalMashupPackagesResponse;
import com.space.space_bundle.dto.MashupPackageResponse;
import com.space.space_bundle.entity.MashupBundle;
import com.space.space_bundle.repository.MashupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MashupService {

    private final MashupRepository mashupRepository;
    private final WebClient webClient;

    @Value("${mashup.offers.url:https://myspaceserver.com/api/external/special-offers/mashup/packages}")
    private String mashupPackagesUrl;

    @Value("${mashup.offers.timeout-seconds:8}")
    private long timeoutSeconds;

    public List<MashupPackageResponse> getPublicPackages() {
        refreshFromExternalIfPossible();
        return mashupRepository
                .findByStatusAndAvailableTrueOrderBySellingPriceAsc(MashupBundle.MashupStatus.ACTIVE.name())
                .stream()
                .filter(MashupBundle::isPurchasable)
                .map(MashupPackageResponse::from)
                .toList();
    }

    public List<AdminMashupPackageResponse> getAdminPackages() {
        refreshFromExternalIfPossible();
        return mashupRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        MashupBundle::getSellingPrice,
                        Comparator.nullsLast(BigDecimal::compareTo)))
                .map(AdminMashupPackageResponse::from)
                .toList();
    }

    public Map<String, Object> syncFromExternal() {
        ExternalMashupPackagesResponse response = webClient.get()
                .uri(mashupPackagesUrl)
                .retrieve()
                .bodyToMono(ExternalMashupPackagesResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

        if (response == null || !response.isSuccess() || response.getPackages() == null) {
            throw new IllegalStateException("Invalid mashup packages response");
        }

        int created = 0;
        int updated = 0;
        Set<Integer> seenPackageIds = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        for (ExternalMashupPackagesResponse.ExternalMashupPackage external : response.getPackages()) {
            Integer packageId = external.getSpecialOfferPackageId() != null
                    ? external.getSpecialOfferPackageId()
                    : external.getId();

            if (packageId == null) {
                log.warn("[MASHUP] Skipping external package with no id: {}", external);
                continue;
            }

            seenPackageIds.add(packageId);
            MashupBundle bundle = mashupRepository.findBySpecialOfferPackageId(packageId)
                    .orElseGet(() -> MashupBundle.builder()
                            .id(UUID.randomUUID().toString())
                            .specialOfferPackageId(packageId)
                            .status(MashupBundle.MashupStatus.INACTIVE.name())
                            .createdAt(now)
                            .build());

            boolean isNew = bundle.getUpdatedAt() == null;
            bundle.setExternalId(external.getId());
            bundle.setSpecialOfferPackageId(packageId);
            bundle.setSlug(external.getSlug());
            bundle.setName(external.getName());
            bundle.setDescription(external.getDescription());
            bundle.setDataAmountMb(external.getDataAmountMb());
            bundle.setDataSize(external.getName());
            bundle.setNetwork(normalizeNetwork(external.getNetwork()));
            bundle.setCostPrice(external.getCostPrice());
            bundle.setAvailable(true);
            bundle.setLastSeenAt(now);
            bundle.setUpdatedAt(now);
            if (bundle.getStatus() == null) {
                bundle.setStatus(MashupBundle.MashupStatus.INACTIVE.name());
            }

            mashupRepository.save(bundle);
            if (isNew) created++;
            else updated++;
        }

        int unavailable = 0;
        for (MashupBundle existing : mashupRepository.findAll()) {
            if (existing.getSpecialOfferPackageId() != null
                    && !seenPackageIds.contains(existing.getSpecialOfferPackageId())
                    && existing.isAvailable()) {
                existing.setAvailable(false);
                existing.setUpdatedAt(now);
                mashupRepository.save(existing);
                unavailable++;
            }
        }

        return Map.of(
                "created", created,
                "updated", updated,
                "unavailable", unavailable,
                "seen", seenPackageIds.size()
        );
    }

    public MashupBundle setSellingPrice(String id, BigDecimal sellingPrice) {
        if (sellingPrice == null || sellingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Selling price must be greater than zero");
        }

        MashupBundle bundle = getByIdOrPackageId(id);
        bundle.setSellingPrice(sellingPrice);
        if (bundle.isAvailable()) {
            bundle.setStatus(MashupBundle.MashupStatus.ACTIVE.name());
        }
        bundle.setUpdatedAt(LocalDateTime.now());
        return mashupRepository.save(bundle);
    }

    public MashupBundle setStatus(String id, String status) {
        String normalizedStatus = normalizeStatus(status);
        MashupBundle bundle = getByIdOrPackageId(id);
        bundle.setStatus(normalizedStatus);
        bundle.setUpdatedAt(LocalDateTime.now());
        return mashupRepository.save(bundle);
    }

    public MashupBundle getPurchasablePackage(String bundleCode, String packageId) {
        try {
            syncFromExternal();
        } catch (Exception ex) {
            log.warn("[MASHUP] External purchase refresh failed; using cached package state: {}", ex.getMessage());
        }

        MashupBundle bundle = resolvePackage(bundleCode, packageId);
        if (!bundle.isPurchasable()) {
            throw new IllegalStateException("Mashup package is not available for purchase");
        }
        return bundle;
    }

    private void refreshFromExternalIfPossible() {
        try {
            syncFromExternal();
        } catch (Exception ex) {
            log.warn("[MASHUP] External refresh failed; using cached packages: {}", ex.getMessage());
        }
    }

    private MashupBundle resolvePackage(String bundleCode, String packageId) {
        Integer parsedPackageId = parseInteger(packageId);
        if (parsedPackageId != null) {
            return mashupRepository.findBySpecialOfferPackageId(parsedPackageId)
                    .orElseThrow(() -> new IllegalArgumentException("Mashup package not found: " + packageId));
        }

        if (bundleCode != null && !bundleCode.isBlank()) {
            return mashupRepository.findBySlug(bundleCode)
                    .orElseThrow(() -> new IllegalArgumentException("Mashup package not found: " + bundleCode));
        }

        if (packageId != null && !packageId.isBlank()) {
            return mashupRepository.findById(packageId)
                    .orElseThrow(() -> new IllegalArgumentException("Mashup package not found: " + packageId));
        }

        throw new IllegalArgumentException("Mashup package is required");
    }

    private MashupBundle getByIdOrPackageId(String id) {
        return mashupRepository.findById(id)
                .or(() -> {
                    Integer packageId = parseInteger(id);
                    return packageId == null
                            ? java.util.Optional.empty()
                            : mashupRepository.findBySpecialOfferPackageId(packageId);
                })
                .orElseThrow(() -> new IllegalArgumentException("Mashup package not found: " + id));
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeNetwork(String network) {
        if (network == null || network.isBlank()) return "MTN";
        return network.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        try {
            return MashupBundle.MashupStatus.valueOf(status.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid mashup status: " + status);
        }
    }
}

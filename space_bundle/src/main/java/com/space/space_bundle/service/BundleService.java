package com.space.space_bundle.service;

import com.space.space_bundle.entity.Bundle;
import com.space.space_bundle.repository.BundleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BundleService {

    private final BundleRepository bundleRepository;

    public Bundle create(String code, String name, String dataSize, String network,
                         BigDecimal costPrice, BigDecimal sellingPrice, String description) {
        return bundleRepository.save(Bundle.builder()
                .id(UUID.randomUUID().toString())
                .code(code).name(name).dataSize(dataSize).network(network)
                .costPrice(costPrice).sellingPrice(sellingPrice)
                .status(Bundle.BundleStatus.ACTIVE.name())
                .description(description)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
    }

    public Bundle getByCodeAndNetwork(String code, String network) {
        // Normalize to lowercase — bundles are stored as "mtn", "telecel", "airteltigo"
//        String normalized = network == null ? null : network.toLowerCase();

        if(network.equals("TELECEL")){
            network = network.toLowerCase();
        }

        String finalNetwork = network;
        return bundleRepository.findByCodeAndNetwork(code, network)
                .orElseThrow(() -> new IllegalArgumentException("Bundle not found: " + code + "/" + finalNetwork));
    }

    public BigDecimal getPrice(String code, String network) {
        Bundle bundle = getByCodeAndNetwork(code, network);
        if (!Bundle.BundleStatus.ACTIVE.name().equals(bundle.getStatus()))
            throw new IllegalStateException("Bundle is not active: " + code);
        return bundle.getSellingPrice();
    }

    public BigDecimal getCostPrice(String code, String network) {
        return getByCodeAndNetwork(code, network).getCostPrice();
    }

    public List<Bundle> getAll() {
        return bundleRepository.findAll(Sort.by(Sort.Direction.ASC, "sellingPrice"));
    }

    public List<Bundle> getActiveByNetwork(String network) {
        String normalized = network == null ? null : network.toLowerCase();
        return bundleRepository.findByNetworkAndStatus(normalized, Bundle.BundleStatus.ACTIVE.name());
    }

    public Bundle getById(String id) {
        return bundleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bundle not found: " + id));
    }

    public Bundle update(String id, String name, String dataSize,
                         BigDecimal costPrice, BigDecimal sellingPrice, String description) {
        Bundle bundle = getById(id);
        if (name != null) bundle.setName(name);
        if (dataSize != null) bundle.setDataSize(dataSize);
        if (costPrice != null) bundle.setCostPrice(costPrice);
        if (sellingPrice != null) bundle.setSellingPrice(sellingPrice);
        if (description != null) bundle.setDescription(description);
        bundle.setUpdatedAt(LocalDateTime.now());
        return bundleRepository.save(bundle);
    }

    public Bundle setStatus(String id, String status) {
        try { Bundle.BundleStatus.valueOf(status); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid status: " + status); }
        Bundle bundle = getById(id);
        bundle.setStatus(status);
        bundle.setUpdatedAt(LocalDateTime.now());
        return bundleRepository.save(bundle);
    }

    public void delete(String id) {
        bundleRepository.delete(getById(id));
    }
}

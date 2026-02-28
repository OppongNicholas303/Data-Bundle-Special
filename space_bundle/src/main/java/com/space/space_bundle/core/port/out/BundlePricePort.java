package com.space.space_bundle.core.port.out;

import com.space.space_bundle.core.entities.BundlePrice;

import java.util.List;
import java.util.Optional;

public interface BundlePricePort {
    BundlePrice save(BundlePrice bundle);
    Optional<BundlePrice> findById(Long id);
    Optional<BundlePrice> findByName(String name);
    Optional<BundlePrice> findByNameIgnoreCase(String name);
    List<BundlePrice> findAll();
}





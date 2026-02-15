package com.space.space_bundle.core.port.out;

import com.space.space_bundle.core.entities.Bundle;
import java.util.List;
import java.util.Optional;

public interface BundleRepositoryPort {
    Bundle save(Bundle bundle);
    Optional<Bundle> findById(String id);
    List<Bundle> findByCode(String code);
    Optional<Bundle> findByCodeAndNetwork(String code, String network);
    List<Bundle> findByNetwork(String network);
    List<Bundle> findActiveByNetwork(String network);
    List<Bundle> findAll();
}
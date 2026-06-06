package com.space.space_bundle.feature;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {
    Optional<FeatureFlag> findByKey(String key);
}


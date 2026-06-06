package com.space.space_bundle.feature;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FeatureFlagRepository repository;

    @Transactional(readOnly = true)
    public boolean isEnabled(String key, boolean defaultValue) {
        return repository.findByKey(key).map(FeatureFlag::isEnabled).orElse(defaultValue);
    }

    @Transactional
    public FeatureFlag setFlag(String key, boolean enabled) {
        FeatureFlag flag = repository.findByKey(key)
                .orElse(new FeatureFlag(null, key, enabled, LocalDateTime.now()));
        flag.setEnabled(enabled);
        flag.setUpdatedAt(LocalDateTime.now());
        return repository.save(flag);
    }

    @Transactional(readOnly = true)
    public List<FeatureFlag> listAll() {
        return repository.findAll();
    }
}


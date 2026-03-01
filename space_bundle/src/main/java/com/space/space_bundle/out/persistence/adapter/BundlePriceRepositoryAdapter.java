package com.space.space_bundle.out.persistence.adapter;

import com.space.space_bundle.core.entities.BundlePrice;
import com.space.space_bundle.core.port.out.BundlePricePort;
import com.space.space_bundle.out.persistence.entity.BundlePriceDocument;
import com.space.space_bundle.out.persistence.mapper.BundlePriceMapper;
import com.space.space_bundle.out.persistence.repository.BundlePriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BundlePriceRepositoryAdapter implements BundlePricePort {
    private final BundlePriceRepository bundlePriceRepository;

    @Override
    public BundlePrice save(BundlePrice bundle) {
        BundlePriceDocument doc = BundlePriceMapper.toDocument(bundle);
        BundlePriceDocument saved = bundlePriceRepository.save(doc);
        return BundlePriceMapper.toDomain(saved);
    }

    @Override
    public Optional<BundlePrice> findById(Long id) {
        return bundlePriceRepository.findById(id).map(BundlePriceMapper::toDomain);
    }

    @Override
    public Optional<BundlePrice> findByName(String name) {
        return bundlePriceRepository.findByName(name).map(BundlePriceMapper::toDomain);
    }

    @Override
    public Optional<BundlePrice> findByNameIgnoreCase(String name) {
        return bundlePriceRepository.findByNameIgnoreCase(name).map(BundlePriceMapper::toDomain);
    }

    @Override
    public List<BundlePrice> findAll() {
        return bundlePriceRepository.findAll()
                .stream()
                .map(BundlePriceMapper::toDomain)
                .collect(Collectors.toList());
    }
}
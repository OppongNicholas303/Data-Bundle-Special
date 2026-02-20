package com.space.space_bundle.out.persistence.adapter;

import com.space.space_bundle.core.entities.Bundle;
import com.space.space_bundle.core.port.out.BundleRepositoryPort;
import com.space.space_bundle.out.persistence.entity.BundleDocument;
import com.space.space_bundle.out.persistence.mapper.BundleMapper;
import com.space.space_bundle.out.persistence.repository.BundleMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BundleRepositoryAdapter implements BundleRepositoryPort {

    private final BundleMongoRepository mongoRepository;

    @Override
    public Bundle save(Bundle bundle) {
        BundleDocument document = BundleMapper.toDocument(bundle);
        BundleDocument saved = mongoRepository.save(document);
        return BundleMapper.toDomain(saved);
    }

    @Override
    public Optional<Bundle> findById(String id) {
        return mongoRepository.findById(id)
                .map(BundleMapper::toDomain);
    }

    @Override
    public List<Bundle> findByCode(String code) {
        return mongoRepository.findByCode(code)
                .stream()
                .map(BundleMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Bundle> findByCodeAndNetwork(String code, String network) {
        return mongoRepository.findByCodeAndNetwork(code, network)
                .map(BundleMapper::toDomain);
    }

    @Override
    public List<Bundle> findByNetwork(String network) {
        return mongoRepository.findByNetwork(network)
                .stream()
                .map(BundleMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Bundle> findActiveByNetwork(String network) {
        return mongoRepository.findByNetworkAndStatus(network, Bundle.BundleStatus.ACTIVE.name())
                .stream()
                .map(BundleMapper::toDomain)
                .collect(Collectors.toList());
    }

//    @Override
//    public List<Bundle> findAll() {
//        return mongoRepository.findAll()
//                .stream()
//                .map(BundleMapper::toDomain)
//                .collect(Collectors.toList());
//    }

    @Override
    public List<Bundle> findAll() {
        return mongoRepository
                .findAll(Sort.by(Sort.Direction.ASC, "sellingPrice"))
                .stream()
                .map(BundleMapper::toDomain)
                .collect(Collectors.toList());
    }
}
package com.space.space_bundle.out.persistence.adapter;

import com.space.space_bundle.core.entities.Commission;
import com.space.space_bundle.core.port.out.CommissionRepositoryPort;
import com.space.space_bundle.out.persistence.mapper.CommissionMapper;
import com.space.space_bundle.out.persistence.repository.CommissionMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommissionRepositoryAdapter implements CommissionRepositoryPort {

    private final CommissionMongoRepository mongoRepository;

    @Override
    public Commission save(Commission commission) {
        return CommissionMapper.toDomain(
                mongoRepository.save(CommissionMapper.toDocument(commission)));
    }

    @Override
    public Optional<Commission> findByOrderId(String orderId) {
        return mongoRepository.findByOrderId(orderId).map(CommissionMapper::toDomain);
    }

    @Override
    public List<Commission> findByAgentId(String agentId) {
        return mongoRepository.findByAgentId(agentId).stream()
                .map(CommissionMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Commission> findById(String id) {
        return mongoRepository.findById(id).map(CommissionMapper::toDomain);
    }
}

package com.space.space_bundle.core.port.out;

import com.space.space_bundle.core.entities.Commission;

import java.util.List;
import java.util.Optional;

public interface CommissionRepositoryPort {
    Commission save(Commission commission);
    Optional<Commission> findByOrderId(String orderId);
    List<Commission> findByAgentId(String agentId);
    Optional<Commission> findById(String id);
}

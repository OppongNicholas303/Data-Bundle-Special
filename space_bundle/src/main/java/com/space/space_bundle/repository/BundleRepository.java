package com.space.space_bundle.repository;

import com.space.space_bundle.entity.Bundle;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BundleRepository extends MongoRepository<Bundle, String> {
    List<Bundle> findByCode(String code);
    Optional<Bundle> findFirstByCodeAndNetwork(String code, String network);
    List<Bundle> findByNetwork(String network);
    List<Bundle> findByNetworkAndStatus(String network, String status);
    List<Bundle> findByStatus(String status, Sort sort);
}

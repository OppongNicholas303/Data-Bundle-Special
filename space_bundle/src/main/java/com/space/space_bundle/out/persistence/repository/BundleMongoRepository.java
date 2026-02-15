package com.space.space_bundle.out.persistence.repository;

import com.space.space_bundle.out.persistence.entity.BundleDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BundleMongoRepository extends MongoRepository<BundleDocument, String> {
    List<BundleDocument> findByCode(String code);
    Optional<BundleDocument> findByCodeAndNetwork(String code, String network);
    List<BundleDocument> findByNetwork(String network);
    List<BundleDocument> findByNetworkAndStatus(String network, String status);
}
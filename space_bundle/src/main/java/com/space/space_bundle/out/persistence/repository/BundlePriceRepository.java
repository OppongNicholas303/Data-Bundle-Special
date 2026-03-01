package com.space.space_bundle.out.persistence.repository;

import com.space.space_bundle.out.persistence.entity.BundlePriceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface BundlePriceRepository extends MongoRepository<BundlePriceDocument, Long> {
    Optional<BundlePriceDocument> findByName(String name);

    // Case-insensitive search for bundle name
    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    Optional<BundlePriceDocument> findByNameIgnoreCase(String name);
}





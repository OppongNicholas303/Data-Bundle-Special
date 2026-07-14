package com.space.space_bundle.repository;

import com.space.space_bundle.entity.ResultCheckerPricing;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultCheckerPricingRepository extends MongoRepository<ResultCheckerPricing, String> {
}

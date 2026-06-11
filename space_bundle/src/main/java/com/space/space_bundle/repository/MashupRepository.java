package com.space.space_bundle.repository;

import com.space.space_bundle.entity.MashupBundle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MashupRepository extends MongoRepository<MashupBundle, String> {
    Optional<MashupBundle> findBySpecialOfferPackageId(Integer specialOfferPackageId);
    Optional<MashupBundle> findBySlug(String slug);
    List<MashupBundle> findByStatusAndAvailableTrueOrderBySellingPriceAsc(String status);
}

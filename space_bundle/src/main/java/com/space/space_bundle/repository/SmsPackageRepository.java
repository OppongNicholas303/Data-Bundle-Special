package com.space.space_bundle.repository;

import com.space.space_bundle.entity.SmsPackage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SmsPackageRepository extends MongoRepository<SmsPackage, String> {
    List<SmsPackage> findByActiveTrueOrderByPriceAsc();
    List<SmsPackage> findAllByOrderByPriceAsc();
}

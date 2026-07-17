package com.space.space_bundle.repository;

import com.space.space_bundle.entity.SmsCampaign;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmsCampaignRepository extends MongoRepository<SmsCampaign, String> {
    List<SmsCampaign> findByUserIdOrderByCreatedAtDesc(String userId);
}

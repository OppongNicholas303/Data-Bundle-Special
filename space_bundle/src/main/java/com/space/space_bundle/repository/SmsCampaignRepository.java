package com.space.space_bundle.repository;

import com.space.space_bundle.entity.SmsCampaign;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Pageable;

@Repository
public interface SmsCampaignRepository extends MongoRepository<SmsCampaign, String> {
    List<SmsCampaign> findByUserIdOrderByCreatedAtDesc(String userId);
    List<SmsCampaign> findAllByOrderByCreatedAtDesc();
    List<SmsCampaign> findByStatusIn(List<String> statuses, Pageable pageable);
}

package com.space.space_bundle.repository;

import com.space.space_bundle.entity.SmsSenderIdRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface SmsSenderIdRequestRepository extends MongoRepository<SmsSenderIdRequest, String> {
    List<SmsSenderIdRequest> findByUserIdOrderByCreatedAtDesc(String userId);
    List<SmsSenderIdRequest> findAllByOrderByCreatedAtDesc();
    Optional<SmsSenderIdRequest> findByUserIdAndSenderIdAndStatus(
            String userId, String senderId, String status);
    boolean existsByUserIdAndSenderId(String userId, String senderId);
}

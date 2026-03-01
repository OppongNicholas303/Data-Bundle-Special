package com.space.space_bundle.out.persistence.repository;

import com.space.space_bundle.out.persistence.entity.OrderDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringOrderRepository extends MongoRepository<OrderDocument, String> {
    List<OrderDocument> findByUserId(String userId);
    List<OrderDocument> findByUserIdAndPhoneNumber(String userId, String phoneNumber);
    List<OrderDocument> findByUserIdAndStatus(String userId, String status);
    List<OrderDocument> findByUserIdAndPhoneNumberAndStatus(String userId, String phoneNumber, String status);
    List<OrderDocument> findByPhoneNumber(String phoneNumber);
}
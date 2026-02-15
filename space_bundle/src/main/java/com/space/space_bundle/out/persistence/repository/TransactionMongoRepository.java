package com.space.space_bundle.out.persistence.repository;

import com.space.space_bundle.out.persistence.entity.TransactionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionMongoRepository extends MongoRepository<TransactionDocument, String> {
    List<TransactionDocument> findByUserId(String userId);
    List<TransactionDocument> findByOrderId(String orderId);
}
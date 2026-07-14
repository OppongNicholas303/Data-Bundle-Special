package com.space.space_bundle.repository;

import com.space.space_bundle.entity.ResultsTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResultsTransactionRepository extends MongoRepository<ResultsTransaction, String> {
    Optional<ResultsTransaction> findByReferenceId(String referenceId);
    List<ResultsTransaction> findByUserId(String userId);
    List<ResultsTransaction> findByStatusInAndCreatedAtBefore(List<com.space.space_bundle.entity.ServiceStatus> statuses, LocalDateTime time);
}

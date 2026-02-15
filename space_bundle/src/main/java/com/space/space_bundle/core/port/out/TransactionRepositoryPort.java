package com.space.space_bundle.core.port.out;

import com.space.space_bundle.core.entities.Transaction;
import java.util.List;
import java.util.Optional;

public interface TransactionRepositoryPort {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(String id);
    List<Transaction> findByUserId(String userId);
    List<Transaction> findByOrderId(String orderId);
}
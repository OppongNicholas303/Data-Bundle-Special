package com.space.space_bundle.out.persistence.adapter;

import com.space.space_bundle.core.entities.Transaction;
import com.space.space_bundle.core.port.out.TransactionRepositoryPort;
import com.space.space_bundle.out.persistence.entity.TransactionDocument;
import com.space.space_bundle.out.persistence.mapper.TransactionMapper;
import com.space.space_bundle.out.persistence.repository.TransactionMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepositoryPort {

    private final TransactionMongoRepository mongoRepository;

    @Override
    public Transaction save(Transaction transaction) {
        TransactionDocument document = TransactionMapper.toDocument(transaction);
        TransactionDocument saved = mongoRepository.save(document);
        return TransactionMapper.toDomain(saved);
    }

    @Override
    public Optional<Transaction> findById(String id) {
        return mongoRepository.findById(id)
                .map(TransactionMapper::toDomain);
    }

    @Override
    public List<Transaction> findByUserId(String userId) {
        return mongoRepository.findByUserId(userId)
                .stream()
                .map(TransactionMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByOrderId(String orderId) {
        return mongoRepository.findByOrderId(orderId)
                .stream()
                .map(TransactionMapper::toDomain)
                .collect(Collectors.toList());
    }
}
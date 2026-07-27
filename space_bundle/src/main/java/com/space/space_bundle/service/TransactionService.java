package com.space.space_bundle.service;

import com.space.space_bundle.entity.Transaction;
import com.space.space_bundle.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    public Transaction createTransaction(String userId, String orderId, String type,
                                         BigDecimal amount, BigDecimal before, BigDecimal after,
                                         String description, String status) {
        return transactionRepository.save(Transaction.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .orderId(orderId)
                .type(type)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .currency("GHS")
                .status(status)
                .description(description)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    public Transaction createDebit(String userId, String orderId, BigDecimal amount,
                                   BigDecimal before, BigDecimal after, String description) {
        return createTransaction(userId, orderId, Transaction.Type.DEBIT.name(),
                amount, before, after, description, Transaction.Status.PENDING.name());
    }

    public Transaction createCompletedDebit(String userId, String orderId, BigDecimal amount,
                                   BigDecimal before, BigDecimal after, String description) {
        return createTransaction(userId, orderId, Transaction.Type.DEBIT.name(),
                amount, before, after, description, Transaction.Status.COMPLETED.name());
    }

    public Transaction createCredit(String userId, String orderId, BigDecimal amount,
                                    BigDecimal before, BigDecimal after, String description) {
        return createTransaction(userId, orderId, Transaction.Type.CREDIT.name(),
                amount, before, after, description, Transaction.Status.PENDING.name());
    }

    public Transaction createCompletedCredit(String userId, String orderId, BigDecimal amount,
                                    BigDecimal before, BigDecimal after, String description) {
        return createTransaction(userId, orderId, Transaction.Type.CREDIT.name(),
                amount, before, after, description, Transaction.Status.COMPLETED.name());
    }

    public Transaction createRefund(String userId, String orderId, BigDecimal amount,
                                    BigDecimal before, BigDecimal after, String description) {
        return createTransaction(userId, orderId, Transaction.Type.REFUND.name(),
                amount, before, after, description, Transaction.Status.PENDING.name());
    }

    public Transaction createCompletedRefund(String userId, String orderId, BigDecimal amount,
                                    BigDecimal before, BigDecimal after, String description) {
        return createTransaction(userId, orderId, Transaction.Type.REFUND.name(),
                amount, before, after, description, Transaction.Status.COMPLETED.name());
    }

    public Transaction createPayment(String userId, String orderId, BigDecimal amount,
                                     BigDecimal before, BigDecimal after, String description) {
        return createTransaction(userId, orderId, Transaction.Type.DEBIT.name(),
                amount, before, after, description, Transaction.Status.COMPLETED.name());
    }

    public Transaction createCommission(String userId, String orderId, BigDecimal amount,
                                        BigDecimal before, BigDecimal after, String description) {
        return createTransaction(userId, orderId, Transaction.Type.COMMISSION.name(),
                amount, before, after, description, Transaction.Status.COMPLETED.name());
    }

    public Transaction createWithdrawal(String userId, String reference, BigDecimal amount,
                                        BigDecimal before, BigDecimal after, String description) {
        return createTransaction(userId, reference, Transaction.Type.WITHDRAWAL.name(),
                amount, before, after, description, Transaction.Status.PENDING.name());
    }

    public void complete(String transactionId) {
        transactionRepository.findById(transactionId).ifPresent(tx -> {
            tx.setStatus(Transaction.Status.COMPLETED.name());
            tx.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(tx);
        });
    }

    public List<Transaction> getTransactionsByOrderId(String orderId) {
        return transactionRepository.findByOrderId(orderId);
    }

    public void complete(String transactionId, BigDecimal before, BigDecimal after) {
        transactionRepository.findById(transactionId).ifPresent(tx -> {
            tx.setBalanceBefore(before);
            tx.setBalanceAfter(after);
            tx.setStatus(Transaction.Status.COMPLETED.name());
            tx.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(tx);
        });
    }

    public boolean atomicComplete(String transactionId, BigDecimal before, BigDecimal after) {
        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query(
            org.springframework.data.mongodb.core.query.Criteria.where("id").is(transactionId).and("status").is(Transaction.Status.PENDING.name())
        );
        org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update()
            .set("balanceBefore", before)
            .set("balanceAfter", after)
            .set("status", Transaction.Status.COMPLETED.name())
            .set("updatedAt", LocalDateTime.now());
        com.mongodb.client.result.UpdateResult result = mongoTemplate.updateFirst(query, update, Transaction.class);
        return result.getModifiedCount() > 0;
    }

    public void fail(String transactionId) {
        transactionRepository.findById(transactionId).ifPresent(tx -> {
            tx.setStatus(Transaction.Status.FAILED.name());
            tx.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(tx);
        });
    }

    public List<Transaction> getByUserId(String userId) {
        return transactionRepository.findByUserId(userId);
    }

    public List<Transaction> getByOrderId(String orderId) {
        return transactionRepository.findByOrderId(orderId);
    }

    public Transaction getFirstByOrderId(String orderId) {
        List<Transaction> txs = transactionRepository.findByOrderId(orderId);
        if (txs.isEmpty()) throw new IllegalArgumentException("Transaction not found for orderId: " + orderId);
        return txs.get(0);
    }

    public List<com.space.space_bundle.dto.AdminTransactionView> getAllTransactions(String status, String type, String search, String fromDate, String toDate) {
        org.springframework.data.mongodb.core.aggregation.AggregationOperation lookupUser = org.springframework.data.mongodb.core.aggregation.Aggregation.lookup("users", "userId", "_id", "user");
        org.springframework.data.mongodb.core.aggregation.AggregationOperation unwindUser = org.springframework.data.mongodb.core.aggregation.Aggregation.unwind("user", true);

        java.util.List<org.springframework.data.mongodb.core.query.Criteria> criteriaList = new java.util.ArrayList<>();

        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            criteriaList.add(org.springframework.data.mongodb.core.query.Criteria.where("status").is(status.toUpperCase()));
        }

        if (type != null && !type.isEmpty() && !"ALL".equalsIgnoreCase(type)) {
            criteriaList.add(org.springframework.data.mongodb.core.query.Criteria.where("type").is(type.toUpperCase()));
        }

        if (search != null && !search.isEmpty()) {
            String searchPattern = ".*" + java.util.regex.Pattern.quote(search) + ".*";
            criteriaList.add(new org.springframework.data.mongodb.core.query.Criteria().orOperator(
                    org.springframework.data.mongodb.core.query.Criteria.where("user.email").regex(searchPattern, "i"),
                    org.springframework.data.mongodb.core.query.Criteria.where("user.phoneNumber").regex(searchPattern, "i"),
                    org.springframework.data.mongodb.core.query.Criteria.where("id").regex(searchPattern, "i"),
                    org.springframework.data.mongodb.core.query.Criteria.where("reference").regex(searchPattern, "i"),
                    org.springframework.data.mongodb.core.query.Criteria.where("description").regex(searchPattern, "i")
            ));
        }

        if (fromDate != null && !fromDate.isEmpty()) {
            try {
                LocalDateTime from = java.time.LocalDate.parse(fromDate).atStartOfDay();
                criteriaList.add(org.springframework.data.mongodb.core.query.Criteria.where("createdAt").gte(from));
            } catch (Exception ignored) {}
        }
        
        if (toDate != null && !toDate.isEmpty()) {
            try {
                LocalDateTime to = java.time.LocalDate.parse(toDate).plusDays(1).atStartOfDay();
                criteriaList.add(org.springframework.data.mongodb.core.query.Criteria.where("createdAt").lt(to));
            } catch (Exception ignored) {}
        }

        org.springframework.data.mongodb.core.aggregation.AggregationOperation match = null;
        if (!criteriaList.isEmpty()) {
            match = org.springframework.data.mongodb.core.aggregation.Aggregation.match(
                    new org.springframework.data.mongodb.core.query.Criteria().andOperator(criteriaList.toArray(new org.springframework.data.mongodb.core.query.Criteria[0]))
            );
        }

        org.springframework.data.mongodb.core.aggregation.AggregationOperation sort = org.springframework.data.mongodb.core.aggregation.Aggregation.sort(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
        
        org.springframework.data.mongodb.core.aggregation.AggregationOperation project = org.springframework.data.mongodb.core.aggregation.Aggregation.project()
                .and("_id").as("id")
                .and("userId").as("userId")
                .and("orderId").as("orderId")
                .and("type").as("type")
                .and("amount").as("amount")
                .and("balanceBefore").as("balanceBefore")
                .and("balanceAfter").as("balanceAfter")
                .and("currency").as("currency")
                .and("status").as("status")
                .and("reference").as("reference")
                .and("description").as("description")
                .and("createdAt").as("createdAt")
                .and("updatedAt").as("updatedAt")
                .and("user.email").as("userEmail")
                .and("user.phoneNumber").as("userPhone")
                .and("user.username").as("username");

        java.util.List<org.springframework.data.mongodb.core.aggregation.AggregationOperation> operations = new java.util.ArrayList<>();
        operations.add(lookupUser);
        operations.add(unwindUser);
        if (match != null) operations.add(match);
        operations.add(sort);
        operations.add(org.springframework.data.mongodb.core.aggregation.Aggregation.limit(1000));
        operations.add(project);

        org.springframework.data.mongodb.core.aggregation.Aggregation aggregation = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(operations);

        return mongoTemplate.aggregate(aggregation, "transactions", com.space.space_bundle.dto.AdminTransactionView.class).getMappedResults();
    }
}

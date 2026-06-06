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

    public Transaction createCredit(String userId, String orderId, BigDecimal amount,
                                    BigDecimal before, BigDecimal after, String description) {
        return createTransaction(userId, orderId, Transaction.Type.CREDIT.name(),
                amount, before, after, description, Transaction.Status.PENDING.name());
    }

    public Transaction createRefund(String userId, String orderId, BigDecimal amount,
                                    BigDecimal before, BigDecimal after, String description) {
        return createTransaction(userId, orderId, Transaction.Type.REFUND.name(),
                amount, before, after, description, Transaction.Status.PENDING.name());
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

    public void complete(String transactionId, BigDecimal before, BigDecimal after) {
        transactionRepository.findById(transactionId).ifPresent(tx -> {
            tx.setBalanceBefore(before);
            tx.setBalanceAfter(after);
            tx.setStatus(Transaction.Status.COMPLETED.name());
            tx.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(tx);
        });
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
}

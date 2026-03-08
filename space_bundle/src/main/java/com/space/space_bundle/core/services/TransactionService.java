package com.space.space_bundle.core.services;

import com.space.space_bundle.core.entities.Transaction;
import com.space.space_bundle.core.port.out.TransactionRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepositoryPort transactionRepository;

    public Transaction createDebitTransaction(String userId, String orderId, BigDecimal amount, BigDecimal before, BigDecimal after, String description) {
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .orderId(orderId)
                .type(Transaction.TransactionType.DEBIT)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .currency("GHS")
                .status(Transaction.TransactionStatus.PENDING)
                .description(description)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(transaction);
    }

    public Transaction createCreditTransaction(String userId, String orderId, BigDecimal amount, BigDecimal before, BigDecimal after, String description) {
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .orderId(orderId)
                .type(Transaction.TransactionType.CREDIT)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .currency("GHS")
                .status(Transaction.TransactionStatus.PENDING)
                .description(description)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(transaction);
    }

    public Transaction createRefundTransaction(String userId, String orderId, BigDecimal amount, BigDecimal before, BigDecimal after, String description) {
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .orderId(orderId)
                .type(Transaction.TransactionType.REFUND)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .currency("GHS")
                .status(Transaction.TransactionStatus.PENDING)
                .description(description)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(transaction);
    }

    public Transaction createPaymentTransaction(String userId, String orderId, BigDecimal amount, BigDecimal before, BigDecimal after, String description) {
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .orderId(orderId)
                .type(Transaction.TransactionType.DEBIT)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .currency("GHS")
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(description)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(transaction);
    }

    public void completeTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        transaction.markCompleted();
        transactionRepository.save(transaction);
    }

    public void completeTransaction(String transactionId, BigDecimal before, BigDecimal after) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        transaction.setBalanceBefore(before);
        transaction.setBalanceAfter(after);
        transaction.markCompleted();
        transactionRepository.save(transaction);
    }

    public void failTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        transaction.markFailed();
        transactionRepository.save(transaction);
    }

    public List<Transaction> getUserTransactions(String userId) {
        return transactionRepository.findByUserId(userId);
    }

    public List<Transaction> getOrderTransactions(String orderId) {
        return transactionRepository.findByOrderId(orderId);
    }

    public Transaction getTransactionByOrderId(String orderId) {
        List<Transaction> transactions = transactionRepository.findByOrderId(orderId);
        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("Transaction not found for orderId: " + orderId);
        }
        return transactions.get(0);
    }
}
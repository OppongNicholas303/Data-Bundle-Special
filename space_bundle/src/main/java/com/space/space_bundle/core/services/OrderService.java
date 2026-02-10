package com.space.space_bundle.core.services;

import com.space.space_bundle.core.entities.Order;
import com.space.space_bundle.core.entities.Transaction;
import com.space.space_bundle.core.entities.Wallet;
import com.space.space_bundle.core.port.out.AutomationPort;
import com.space.space_bundle.core.port.out.OrderRepositoryPort;
import com.space.space_bundle.core.port.out.WalletRepositoryPort;
import com.space.space_bundle.core.exceptions.InsufficientBalanceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepositoryPort orderRepository;
    private final WalletRepositoryPort walletRepository;
    private final AutomationPort automationPort;
    private final TransactionService transactionService;
    private final BundleService bundleService;

    @Transactional
    public Order createGuestOrder(
            String network,
            String phoneNumber,
            String bundleCode
    ) {
        BigDecimal amount = bundleService.getBundlePrice(bundleCode, network);
        
        Order order = Order.builder()
                .userId(null)
                .network(network)
                .phoneNumber(phoneNumber)
                .bundleCode(bundleCode)
                .amount(amount)
                .status(com.space.space_bundle.core.enums.OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        order = orderRepository.save(order);

        try {
            order.markPaid();
            order = orderRepository.save(order);

            order.markProcessing();
            order = orderRepository.save(order);

            String providerReference = automationPort.buyDataBundle(order);

            order.markCompleted(providerReference);
            order = orderRepository.save(order);

        } catch (Exception ex) {
            log.error("Order processing failed: orderId={}, error={}", order.getId(), ex.getMessage(), ex);
            
            order.markFailed(ex.getMessage());
            order = orderRepository.save(order);
            
            throw new RuntimeException("Order failed: " + ex.getMessage());
        }

        return order;
    }

    @Transactional
    public Order createOrder(
            String userId,
            String network,
            String phoneNumber,
            String bundleCode
    ) {
        // Get bundle and price by code AND network
        BigDecimal amount = bundleService.getBundlePrice(bundleCode, network);
        
        // Load and validate wallet
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));


        // Check wallet balance
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(wallet.getBalance(), amount);
        }

        // Create order
        Order order = Order.builder()
                .userId(userId)
                .network(network)
                .phoneNumber(phoneNumber)
                .bundleCode(bundleCode)
                .amount(amount)
                .status(com.space.space_bundle.core.enums.OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        order = orderRepository.save(order);

        // Create debit transaction
        Transaction debitTx = transactionService.createDebitTransaction(
                userId, order.getId(), amount, "Order payment for " + bundleCode);

        try {
            // Debit wallet
            wallet.debit(amount);
            walletRepository.save(wallet);
            transactionService.completeTransaction(debitTx.getId());

            order.markPaid();
            order = orderRepository.save(order);

            // Send to bot for processing
            order.markProcessing();
            order = orderRepository.save(order);



            String providerReference = automationPort.buyDataBundle(order);

            // Mark as completed
            order.markCompleted(providerReference);
            order = orderRepository.save(order);

        } catch (Exception ex) {
            // Handle failure and refund
            log.error("Order processing failed: orderId={}, error={}", order.getId(), ex.getMessage(), ex);
            
            transactionService.failTransaction(debitTx.getId());
            
            order.markFailed(ex.getMessage());
            order = orderRepository.save(order);

            // Create refund transaction
            Transaction refundTx = transactionService.createRefundTransaction(
                    userId, order.getId(), amount, "Refund for failed order " + order.getId());
            
            wallet.credit(amount);
            walletRepository.save(wallet);
            transactionService.completeTransaction(refundTx.getId());

            order.markRefunded();
            order = orderRepository.save(order);
            
            log.info("Order refunded: orderId={}, reason={}", order.getId(), ex.getMessage());
            
            throw new RuntimeException("Order failed: " + ex.getMessage());
        }

        return order;
    }

    public List<Order> getOrders(String userId, String orderId, String phoneNumber, String status) {
        return orderRepository.findByFilters(userId, orderId, phoneNumber, status);
    }

    public Order getOrderById(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }
        
        return order;
    }
}

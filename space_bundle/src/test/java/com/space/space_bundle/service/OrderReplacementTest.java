package com.space.space_bundle.service;

import com.space.space_bundle.entity.Order;
import com.space.space_bundle.entity.Transaction;
import com.space.space_bundle.repository.OrderRepository;
import com.space.space_bundle.repository.WalletRepository;
import com.space.space_bundle.security.MoolreAdapter;
import com.space.space_bundle.security.PaystackAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderReplacementTest {
    private OrderRepository orderRepository;
    private TransactionService transactionService;
    private AutomationService automationService;
    private MongoTemplate mongoTemplate;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        WalletRepository walletRepository = mock(WalletRepository.class);
        BundleService bundleService = mock(BundleService.class);
        MashupService mashupService = mock(MashupService.class);
        AgentService agentService = mock(AgentService.class);
        CommissionService commissionService = mock(CommissionService.class);
        transactionService = mock(TransactionService.class);
        MoolreAdapter moolreAdapter = mock(MoolreAdapter.class);
        PaystackAdapter paystackAdapter = mock(PaystackAdapter.class);
        automationService = mock(AutomationService.class);
        com.space.space_bundle.feature.FeatureFlagService featureFlagService =
                mock(com.space.space_bundle.feature.FeatureFlagService.class);
        mongoTemplate = mock(MongoTemplate.class);
        WalletService walletService = mock(WalletService.class);

        orderService = new OrderService(orderRepository, walletRepository, bundleService, mashupService,
                agentService, commissionService, transactionService, moolreAdapter, paystackAdapter,
                automationService, featureFlagService, mongoTemplate, walletService);
    }

    @Test
    void replacementBlocksWalletOrderWithoutCompletedDebit() {
        Order order = failedWalletOrder();
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(transactionService.getTransactionsByOrderId("order-1")).thenReturn(List.of());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> orderService.reprocessFailedOrder("order-1", "admin-1"));

        assertTrue(error.getMessage().contains("Completed wallet debit"));
        verifyNoInteractions(automationService);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void replacementBlocksOrderWithRefundEvenWhenWalletDebitWasCompleted() {
        Order order = failedWalletOrder();
        Transaction debit = transaction(Transaction.Type.DEBIT, Transaction.Status.COMPLETED, "25.00");
        Transaction refund = transaction(Transaction.Type.REFUND, Transaction.Status.COMPLETED, "25.00");
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(transactionService.getTransactionsByOrderId("order-1")).thenReturn(List.of(debit, refund));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> orderService.reprocessFailedOrder("order-1", "admin-1"));

        assertTrue(error.getMessage().contains("full order amount remains paid"));
        verifyNoInteractions(automationService);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void replacementRejectsNonRetryableOrderState() {
        Order order = failedWalletOrder();
        order.setStatus(Order.OrderStatus.COMPLETED.name());
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class,
                () -> orderService.reprocessFailedOrder("order-1", "admin-1"));
        verifyNoInteractions(transactionService, automationService);
    }

    private Order failedWalletOrder() {
        return Order.builder().id("order-1").userId("user-1").amount(new BigDecimal("25.00"))
                .status(Order.OrderStatus.FAILED.name()).build();
    }

    private Transaction transaction(Transaction.Type type, Transaction.Status status, String amount) {
        return Transaction.builder().type(type.name()).status(status.name()).amount(new BigDecimal(amount)).build();
    }
}

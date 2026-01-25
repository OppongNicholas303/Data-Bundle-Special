package com.space.space_bundle.core.services;

import com.space.space_bundle.core.entities.Order;
import com.space.space_bundle.core.entities.OrderStatus;
import com.space.space_bundle.core.entities.Wallet;
import com.space.space_bundle.core.port.out.ProviderOrderPort;
import com.space.space_bundle.out.persistence.repository.WalletRepositoryPort;
import com.space.space_bundle.core.port.out.authenticationPort.AutomationPort;
import com.space.space_bundle.out.persistence.repository.OrderRepositoryPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderService {

    private final OrderRepositoryPort orderRepository;
    private final WalletRepositoryPort walletRepository;
    private final AutomationPort automationPort;

    public OrderService(
            OrderRepositoryPort orderRepository,
            WalletRepositoryPort walletRepository,
            AutomationPort automationPort
    ) {
        this.orderRepository = orderRepository;
        this.walletRepository = walletRepository;
        this.automationPort = automationPort;
    }

    /**
     * Create and process a data bundle order
     */
    public Order createOrder(
            String userId,
            String network,
            String phoneNumber,
            String bundleCode,
            BigDecimal amount
    ) {
        // 1️⃣ Load wallet
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));

        // 2️⃣ Debit wallet
        wallet.debit(amount);
        walletRepository.save(wallet);

        // 3️⃣ Create order
        Order order = Order.builder()
                .userId(userId)
                .network(network)
                .phoneNumber(phoneNumber)
                .bundleCode(bundleCode)
                .amount(amount)
                .status(com.space.space_bundle.core.enums.OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        order.markPaid();
        orderRepository.save(order);

        try {
            // 4️⃣ Send to provider (BOT)
            order.markProcessing();
            orderRepository.save(order);

            String providerReference = automationPort.buyDataBundle(order);

            // 5️⃣ Complete
            order.markCompleted(providerReference);
            orderRepository.save(order);

        } catch (Exception ex) {
            // 6️⃣ Failure → refund
            order.markFailed(ex.getMessage());
            orderRepository.save(order);

            wallet.credit(amount);
            walletRepository.save(wallet);

            order.markRefunded();
            orderRepository.save(order);
        }

        return order;
    }
}

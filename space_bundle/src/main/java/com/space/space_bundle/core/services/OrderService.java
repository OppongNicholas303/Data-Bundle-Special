package com.space.space_bundle.core.services;

import com.space.space_bundle.core.entities.Order;
import com.space.space_bundle.core.entities.Transaction;
import com.space.space_bundle.core.entities.Wallet;
import com.space.space_bundle.core.port.out.AutomationPort;
import com.space.space_bundle.core.port.out.OrderRepositoryPort;
import com.space.space_bundle.core.port.out.WalletRepositoryPort;
import com.space.space_bundle.out.payment.PaystackAdapter;
import com.space.space_bundle.out.payment.dto.PaystackInitializeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepositoryPort orderRepository;
    private final WalletRepositoryPort walletRepository;
    private final AutomationPort automationPort;
    private final TransactionService transactionService;
    private final BundleService bundleService;
    private final PaystackAdapter paystackAdapter;
    private final UserService userService;

    @Value("${paystack.callback-url:http://localhost:3000/payment/callback}")
    private String paystackCallbackUrl;

    @Transactional
    public Order createGuestOrder(
            String network,
            String phoneNumber,
            String bundleCode,
            String email,
            String userID,
            String packageId) {
        BigDecimal amount = bundleService.getBundlePrice(bundleCode, network);
        
        // Add 2% Paystack transaction fee
        BigDecimal paystackFee = amount.multiply(BigDecimal.valueOf(0.02));
        BigDecimal totalAmount = amount.add(paystackFee);
        
        Order order = Order.builder()
                .userId(userID)
                .network(network)
                .phoneNumber(phoneNumber)
                .bundleCode(bundleCode)
                .amount(totalAmount)
                .byFrom(packageId)
                .providerStatus("processing")
                .status(com.space.space_bundle.core.enums.OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();
        
        order = orderRepository.save(order);
        log.info("Order created with Paystack fee: baseAmount={}, fee={}, total={}", amount, paystackFee, totalAmount);
        
        if (userID != null) {
            Optional<Wallet> wallet = walletRepository.findByUserId(userID);
            if (wallet.isPresent() && wallet.get().getBalance().compareTo(totalAmount) >= 0) {
                return processOrderWithWallet(order, wallet.get(), totalAmount, userID, email);
            }else {
                return initializePaystackPaymentForGuest(order, email);
            }
        }
        
        return initializePaystackPaymentForGuest(order, email);
    }


    private Order initializePaystackPaymentForGuest(Order order, String email) {
        try {
            Integer amountInKobo = order.getAmount().multiply(BigDecimal.valueOf(100)).intValue();

            String reference = "ORDER_" + order.getId();

            PaystackInitializeResponse response = paystackAdapter.initializeTransaction(
                    email, amountInKobo, reference, paystackCallbackUrl);

            if (response.isStatus() && response.getData() != null) {
                order.setPendingPayment(
                    reference, 
                    response.getData().getAuthorization_url(),
                    response.getData().getAccess_code()
                );
                order = orderRepository.save(order);
                log.info("Paystack payment initialized for guest: orderId={}, reference={}", order.getId(), reference);
            } else {
                throw new RuntimeException("Failed to initialize payment: " + response.getMessage());
            }

        } catch (Exception ex) {
            log.error("Payment initialization failed: orderId={}, error={}", order.getId(), ex.getMessage(), ex);
            order.markFailed("Payment initialization failed: " + ex.getMessage());
            order = orderRepository.save(order);
            throw new RuntimeException("Payment initialization failed: " + ex.getMessage());
        }

        return order;
    }

//    @Transactional
//    public Order createOrder(
//            String userId,
//            String network,
//            String phoneNumber,
//            String bundleCode
//    ) {
//        BigDecimal amount = bundleService.getBundlePrice(bundleCode, network);
//
//        Wallet wallet = walletRepository.findByUserId(userId)
//                .orElseThrow(() -> new IllegalStateException("Wallet not found"));
//
//        Order order = Order.builder()
//                .userId(userId)
//                .network(network)
//                .phoneNumber(phoneNumber)
//                .bundleCode(bundleCode)
//                .amount(amount)
//                .status(com.space.space_bundle.core.enums.OrderStatus.CREATED)
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        order = orderRepository.save(order);
//
//        // Check if user has sufficient balance
//        // TODO: Remove this condition to always use Paystack for testing
//        if (false && wallet.getBalance().compareTo(amount) >= 0) {
//            return processOrderWithWallet(order, wallet, amount, userId, "email@gmail.com");
//        } else {
//            return initializePaystackPayment(order, userId);
//        }
//    }

    private Order processOrderWithWallet(Order order, Wallet wallet, BigDecimal amount, String userId, String email) {
        Transaction debitTx = transactionService.createDebitTransaction(
                userId, order.getId(), amount, "Order payment for " + order.getBundleCode());

        if (wallet.getBalance().compareTo(amount)<=0 ){
            return  initializePaystackPaymentForGuest(order, email);
        }

        try {
            wallet.debit(amount);
            walletRepository.save(wallet);
            transactionService.completeTransaction(debitTx.getId());

            order.markPaid();
            order = orderRepository.save(order);

            order.markProcessing();
            order = orderRepository.save(order);

            String providerReference = automationPort.buyDataBundle(order);

            order.markCompleted(providerReference);
            order = orderRepository.save(order);

        } catch (Exception ex) {

            log.error("Order processing failed: orderId={}, error={}", order.getId(), ex.getMessage(), ex);

            transactionService.failTransaction(debitTx.getId());

            order.markFailed(ex.getMessage());
            order = orderRepository.save(order);

            Transaction refundTx = transactionService.createRefundTransaction(
                    userId, order.getId(), amount, "Refund for failed order " + order.getId());

            wallet.credit(amount);
            walletRepository.save(wallet);
            transactionService.completeTransaction(refundTx.getId());

            order.markRefunded();
            order = orderRepository.save(order);

            log.info("Order refunded: orderId={}, reason={}", order.getId(), ex.getMessage());


            return  initializePaystackPaymentForGuest(order, email);

//            throw new RuntimeException("Order failed: " + ex.getMessage());
        }

        return order;
    }

    private Order initializePaystackPayment(Order order, String userId) {
        try {
            String userEmail = userService.getUserById(userId).getEmail();
            Integer amountInKobo = order.getAmount().multiply(BigDecimal.valueOf(100)).intValue();
            String reference = "ORDER_" + order.getId();

            PaystackInitializeResponse response = paystackAdapter.initializeTransaction(
                    userEmail, amountInKobo, reference, paystackCallbackUrl);

            if (response.isStatus() && response.getData() != null) {
                order.setPendingPayment(
                    reference, 
                    response.getData().getAuthorization_url(),
                    response.getData().getAccess_code()
                );
                order = orderRepository.save(order);
                log.info("Paystack payment initialized: orderId={}, reference={}", order.getId(), reference);
            } else {
                throw new RuntimeException("Failed to initialize payment: " + response.getMessage());
            }

        } catch (Exception ex) {
            log.error("Payment initialization failed: orderId={}, error={}", order.getId(), ex.getMessage(), ex);
            order.markFailed("Payment initialization failed: " + ex.getMessage());
            order = orderRepository.save(order);
            throw new RuntimeException("Payment initialization failed: " + ex.getMessage());
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

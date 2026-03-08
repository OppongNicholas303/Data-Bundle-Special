package com.space.space_bundle.out.config;

import com.space.space_bundle.core.port.out.*;
import com.space.space_bundle.core.port.out.AutomationPort;
import com.space.space_bundle.core.port.out.authenticationPort.*;
import com.space.space_bundle.core.services.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ServiceConfig {

    @Bean
    public BundleService bundleService(
            BundleRepositoryPort bundleRepository
    ) {
        return new BundleService(bundleRepository);
    }

    @Bean
    public TransactionService transactionService(
            TransactionRepositoryPort transactionRepository
    ) {
        return new TransactionService(transactionRepository);
    }

    // -----------------------------
    // ORDER SERVICE
    // -----------------------------
    @Bean
    public OrderService orderService(
            OrderRepositoryPort orderRepositoryPort,
            WalletRepositoryPort walletRepository,
            AutomationPort automationPort,
            TransactionService transactionService,
            BundleService bundleService,
            com.space.space_bundle.out.payment.PaystackAdapter paystackAdapter,
            UserService userService
    ) {
        return new OrderService(orderRepositoryPort, walletRepository, automationPort, transactionService, bundleService, paystackAdapter, userService);
    }

    // -----------------------------
    // WALLET SERVICE
    // -----------------------------
    @Bean
    public WalletService walletService(
            WalletRepositoryPort walletRepository,
            TransactionService transactionService,
            com.space.space_bundle.out.payment.PaystackAdapter paystackAdapter,
            UserService userService
    ) {
        return new WalletService(walletRepository, transactionService, paystackAdapter, userService);
    }

    // -----------------------------
    // USER SERVICE
    // -----------------------------
    @Bean
    public UserService userService(
            UserRepositoryPort userRepository
    ) {
        return new UserService(userRepository);
    }

    // -----------------------------
    // PROVIDER ACCOUNT SERVICE
    // -----------------------------
//    @Bean
//    public ProviderAccountService providerAccountService(
//            ProviderAccountRepository providerAccountRepository
//    ) {
//        return new ProviderAccountService(providerAccountRepository);
//    }

    @Bean
    public AuthenticationService authenticationService(
            UserRepositoryPort userRepository,
            RefreshTokenRepositoryPort refreshTokenRepository,
            PasswordEncoderPort passwordEncoder,
            JwtPort jwtPort,
            SecurityAuditPort securityAudit,
            WalletService walletService
    ) {
        return new AuthenticationService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtPort,
                securityAudit,
                walletService
        );
    }

    @Bean
    public DashboardService dashboardService(
            TransactionService transactionService,
            OrderService orderService,
            BundleService bundleService
    ) {
        return new DashboardService(transactionService, orderService, bundleService);
    }
}


package com.space.space_bundle.out.config;

import com.space.space_bundle.core.port.out.*;
import com.space.space_bundle.core.port.out.AutomationPort;
import com.space.space_bundle.core.port.out.authenticationPort.*;
import com.space.space_bundle.core.services.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;


@Configuration
public class ServiceConfig {

    @Bean
    public BundleService bundleService(BundleRepositoryPort bundleRepository) {
        return new BundleService(bundleRepository);
    }

    @Bean
    public UserService userService(UserRepositoryPort userRepository) {
        return new UserService(userRepository);
    }

    @Bean
    public WalletService walletService(
            WalletRepositoryPort walletRepository,
            TransactionService transactionService,
            com.space.space_bundle.out.payment.PaystackAdapter paystackAdapter,
            UserService userService
    ) {
        return new WalletService(walletRepository, transactionService, paystackAdapter, userService);
    }

    @Bean
    public CommissionService commissionService(
            CommissionRepositoryPort commissionRepository,
            WalletRepositoryPort walletRepository,
            TransactionService transactionService,
            AgentProfileRepositoryPort agentProfileRepository
    ) {
        return new CommissionService(commissionRepository, walletRepository, transactionService, agentProfileRepository);
    }

    @Bean
    public AgentService agentService(
            AgentProfileRepositoryPort agentProfileRepository,
            AgentBundlePricingRepositoryPort agentBundlePricingRepository,
            UserRepositoryPort userRepository,
            BundleRepositoryPort bundleRepository,
            WalletRepositoryPort walletRepository,
            TransactionService transactionService,
            com.space.space_bundle.out.payment.PaystackAdapter paystackAdapter
    ) {
        return new AgentService(
                agentProfileRepository,
                agentBundlePricingRepository,
                userRepository,
                bundleRepository,
                walletRepository,
                transactionService,
                paystackAdapter);
    }

    @Bean
    public OrderService orderService(
            OrderRepositoryPort orderRepositoryPort,
            WalletRepositoryPort walletRepository,
            AutomationPort automationPort,
            TransactionService transactionService,
            BundleService bundleService,
            com.space.space_bundle.out.payment.PaystackAdapter paystackAdapter,
            UserService userService,
            @Lazy AgentService agentService,
            @Lazy CommissionService commissionService
    ) {
        return new OrderService(orderRepositoryPort, walletRepository, automationPort,
                transactionService, bundleService, paystackAdapter, userService,
                agentService, commissionService);
    }

    @Bean
    public AuthenticationService authenticationService(
            UserRepositoryPort userRepository,
            RefreshTokenRepositoryPort refreshTokenRepository,
            PasswordEncoderPort passwordEncoder,
            JwtPort jwtPort,
            SecurityAuditPort securityAudit,
            WalletService walletService,
            EmailPort emailPort
    ) {
        return new AuthenticationService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtPort,
                securityAudit,
                walletService,
                emailPort);
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

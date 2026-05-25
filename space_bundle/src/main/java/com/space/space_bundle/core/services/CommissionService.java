package com.space.space_bundle.core.services;

import com.space.space_bundle.core.entities.Commission;
import com.space.space_bundle.core.entities.Transaction;
import com.space.space_bundle.core.entities.Wallet;
import com.space.space_bundle.core.port.out.AgentProfileRepositoryPort;
import com.space.space_bundle.core.port.out.CommissionRepositoryPort;
import com.space.space_bundle.core.port.out.WalletRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionRepositoryPort commissionRepository;
    private final WalletRepositoryPort walletRepository;
    private final TransactionService transactionService;
    private final AgentProfileRepositoryPort agentProfileRepository;

    /**
     * Atomically:
     * 1. Create commission record
     * 2. Credit agent wallet
     * 3. Create transaction ledger entry
     * 4. Update agent profile totals
     */
    @Transactional
    public Commission settleAgentCommission(String agentId, String orderId,
                                            BigDecimal baseAmount, BigDecimal sellingAmount) {
        BigDecimal profit = sellingAmount.subtract(baseAmount);

        if (profit.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("[COMMISSION] No profit to settle for orderId={}", orderId);
            return null;
        }

        // Idempotency guard — never double-credit
        if (commissionRepository.findByOrderId(orderId).isPresent()) {
            log.warn("[COMMISSION] Commission already settled for orderId={}", orderId);
            return commissionRepository.findByOrderId(orderId).get();
        }

        Commission commission = Commission.builder()
                .id(UUID.randomUUID().toString())
                .agentId(agentId)
                .orderId(orderId)
                .baseAmount(baseAmount)
                .sellingAmount(sellingAmount)
                .profit(profit)
                .status(Commission.CommissionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        commission = commissionRepository.save(commission);

        // Credit agent wallet
        Wallet agentWallet = walletRepository.findByUserId(agentId)
                .orElseThrow(() -> new IllegalStateException("Agent wallet not found for agentId=" + agentId));

        BigDecimal before = agentWallet.getBalance();
        agentWallet.credit(profit);
        walletRepository.save(agentWallet);
        BigDecimal after = agentWallet.getBalance();

        // Immutable transaction ledger entry
        transactionService.createCommissionTransaction(agentId, orderId, profit, before, after,
                "Commission earned from order " + orderId);

        // Mark commission settled
        commission.settle();
        commission = commissionRepository.save(commission);

        // Update agent profile totals
        agentProfileRepository.findByUserId(agentId).ifPresent(profile -> {
            profile.recordSale(sellingAmount, profit);
            agentProfileRepository.save(profile);
        });

        log.info("[COMMISSION] Settled: agentId={}, orderId={}, profit={}", agentId, orderId, profit);
        return commission;
    }

    public List<Commission> getAgentCommissions(String agentId) {
        return commissionRepository.findByAgentId(agentId);
    }
}

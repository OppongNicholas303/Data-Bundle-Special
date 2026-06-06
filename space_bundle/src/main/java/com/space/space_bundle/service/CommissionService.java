package com.space.space_bundle.service;

import com.space.space_bundle.entity.Commission;
import com.space.space_bundle.entity.Wallet;
import com.space.space_bundle.repository.AgentProfileRepository;
import com.space.space_bundle.repository.CommissionRepository;
import com.space.space_bundle.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionRepository commissionRepository;
    private final WalletRepository walletRepository;
    private final TransactionService transactionService;
    private final AgentProfileRepository agentProfileRepository;

    @Transactional
    public Commission settle(String agentId, String orderId,
                             BigDecimal baseAmount, BigDecimal sellingAmount) {
        BigDecimal profit = sellingAmount.subtract(baseAmount);
        if (profit.compareTo(BigDecimal.ZERO) <= 0) return null;

        // Idempotency guard
        List<Commission> existing = commissionRepository.findByOrderId(orderId);
        if (!existing.isEmpty()) {
            log.warn("[COMMISSION] Already settled for orderId={}", orderId);
            return existing.get(0);
        }

        Commission commission = commissionRepository.save(Commission.builder()
                .id(UUID.randomUUID().toString())
                .agentId(agentId).orderId(orderId)
                .baseAmount(baseAmount).sellingAmount(sellingAmount).profit(profit)
                .status(Commission.Status.PENDING.name())
                .createdAt(LocalDateTime.now())
                .build());

        // Resolve agent profile -> userId, then credit agent wallet
        String agentUserId = agentProfileRepository.findById(agentId)
                .map(profile -> profile.getUserId())
                .orElseThrow(() -> new IllegalStateException("Agent profile not found: " + agentId));

        Wallet wallet = walletRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new IllegalStateException("Agent wallet not found for user: " + agentUserId));
        BigDecimal before = wallet.getBalance();
        wallet.credit(profit);
        walletRepository.save(wallet);

        transactionService.createCommission(agentId, orderId, profit, before, wallet.getBalance(),
                "Commission from order " + orderId);

        // Mark settled
        commission.setStatus(Commission.Status.SETTLED.name());
        commission = commissionRepository.save(commission);

        // Update agent profile totals (agentId is agentProfile id)
        agentProfileRepository.findById(agentId).ifPresent(profile -> {
            profile.setTotalSales(profile.getTotalSales().add(sellingAmount));
            profile.setTotalProfit(profile.getTotalProfit().add(profit));
            profile.setUpdatedAt(LocalDateTime.now());
            agentProfileRepository.save(profile);
        });

        log.info("[COMMISSION] Settled: agentId={}, orderId={}, profit={}", agentId, orderId, profit);
        return commission;
    }

    public List<Commission> getByAgentId(String agentId) {
        return commissionRepository.findByAgentId(agentId);
    }
}

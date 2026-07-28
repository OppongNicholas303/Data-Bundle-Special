package com.space.space_bundle.controller;

import com.space.space_bundle.entity.AgentProfile;
import com.space.space_bundle.entity.Commission;
import com.space.space_bundle.entity.Wallet;
import com.space.space_bundle.repository.AgentProfileRepository;
import com.space.space_bundle.repository.CommissionRepository;
import com.space.space_bundle.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/migration")
@RequiredArgsConstructor
public class MigrationController {

    private final CommissionRepository commissionRepository;
    private final WalletRepository walletRepository;
    private final AgentProfileRepository agentProfileRepository;

    @PostMapping("/run")
    public String runMigration() {
        List<Commission> settledCommissions = commissionRepository.findAll().stream()
                .filter(c -> "SETTLED".equalsIgnoreCase(c.getStatus()))
                .collect(Collectors.toList());

        Map<String, BigDecimal> agentTotals = settledCommissions.stream()
                .collect(Collectors.groupingBy(Commission::getAgentId,
                        Collectors.mapping(Commission::getProfit, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        for (Map.Entry<String, BigDecimal> entry : agentTotals.entrySet()) {
            String agentId = entry.getKey();
            BigDecimal totalCommission = entry.getValue();

            AgentProfile profile = agentProfileRepository.findById(agentId).orElse(null);
            if (profile == null) continue;

            Wallet wallet = walletRepository.findFirstByUserId(profile.getUserId()).orElse(null);
            if (wallet == null) continue;

            BigDecimal availableToMove = totalCommission.min(wallet.getBalance());

            if (availableToMove.compareTo(BigDecimal.ZERO) > 0) {
                wallet.setCommissionBalance(wallet.getCommissionBalance().add(availableToMove));
                wallet.setBalance(wallet.getBalance().subtract(availableToMove));
                walletRepository.save(wallet);
                log.info("Migrated GHS {} for user {}. New Balance: {}, New Commission Balance: {}",
                        availableToMove, profile.getUserId(), wallet.getBalance(), wallet.getCommissionBalance());
            }
        }

        return "Migration completed for " + agentTotals.size() + " agents.";
    }
}

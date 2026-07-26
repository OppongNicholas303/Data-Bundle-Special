package com.space.space_bundle.service;

import com.space.space_bundle.entity.WithdrawalRequest;
import com.space.space_bundle.entity.Wallet;
import com.space.space_bundle.repository.WithdrawalRepository;
import com.space.space_bundle.repository.WalletRepository;
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
public class AdminWithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final WalletRepository walletRepository;
    private final TransactionService transactionService;

    public List<WithdrawalRequest> getAll() {
        return withdrawalRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<WithdrawalRequest> getByStatus(String status) {
        return withdrawalRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase());
    }

    public List<WithdrawalRequest> getByAgentProfileId(String agentProfileId) {
        return withdrawalRepository.findByAgentProfileIdOrderByCreatedAtDesc(agentProfileId);
    }

    @Transactional
    public WithdrawalRequest approve(String withdrawalId, String adminNote) {
        WithdrawalRequest req = getOrThrow(withdrawalId);

        if (!WithdrawalRequest.Status.PENDING.name().equals(req.getStatus()))
            throw new IllegalStateException("Withdrawal is not PENDING — current status: " + req.getStatus());

        req.setStatus(WithdrawalRequest.Status.APPROVED.name());
        req.setAdminNote(adminNote);
        req.setUpdatedAt(LocalDateTime.now());
        withdrawalRepository.save(req);

        // Mark the withdrawal transaction as completed (admin will manually transfer via MoMo)
        transactionService.getByUserId(req.getAgentUserId()).stream()
                .filter(t -> req.getReference().equals(t.getOrderId()))
                .findFirst()
                .ifPresent(t -> transactionService.complete(t.getId()));

        log.info("[ADMIN] Withdrawal approved: id={}, ref={}, amount={}",
                withdrawalId, req.getReference(), req.getAmount());
        return req;
    }

    @Transactional
    public WithdrawalRequest reject(String withdrawalId, String adminNote) {
        WithdrawalRequest req = getOrThrow(withdrawalId);

        if (!WithdrawalRequest.Status.PENDING.name().equals(req.getStatus()))
            throw new IllegalStateException("Withdrawal is not PENDING — current status: " + req.getStatus());

        // Refund the agent's wallet
        Wallet wallet = walletRepository.findFirstByUserId(req.getAgentUserId())
                .orElseThrow(() -> new IllegalStateException("Agent wallet not found"));

        BigDecimal before = wallet.getBalance();
        wallet.credit(req.getAmount());
        walletRepository.save(wallet);

        transactionService.createCompletedRefund(req.getAgentUserId(), req.getReference(),
                req.getAmount(), before, wallet.getBalance(),
                "Refund — withdrawal rejected by admin: " + (adminNote != null ? adminNote : ""));

        // Mark failed on the original withdrawal transaction
        transactionService.getByUserId(req.getAgentUserId()).stream()
                .filter(t -> req.getReference().equals(t.getOrderId()))
                .findFirst()
                .ifPresent(t -> transactionService.fail(t.getId()));

        req.setStatus(WithdrawalRequest.Status.REJECTED.name());
        req.setAdminNote(adminNote);
        req.setUpdatedAt(LocalDateTime.now());
        withdrawalRepository.save(req);

        log.info("[ADMIN] Withdrawal rejected: id={}, ref={}, amount={} refunded",
                withdrawalId, req.getReference(), req.getAmount());
        return req;
    }

    private WithdrawalRequest getOrThrow(String id) {
        return withdrawalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal request not found: " + id));
    }
}

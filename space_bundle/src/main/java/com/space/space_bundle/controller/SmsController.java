package com.space.space_bundle.controller;

import com.space.space_bundle.dto.ApiResponse;
import com.space.space_bundle.dto.SmsSendRequest;
import com.space.space_bundle.entity.SmsCampaign;
import com.space.space_bundle.entity.SmsPackage;
import com.space.space_bundle.entity.SmsSenderIdRequest;
import com.space.space_bundle.repository.SmsCampaignRepository;
import com.space.space_bundle.repository.SmsPackageRepository;
import com.space.space_bundle.repository.SmsSenderIdRequestRepository;
import com.space.space_bundle.security.CustomUserDetailsService;
import com.space.space_bundle.service.MoolreSmsService;
import com.space.space_bundle.service.SmsCampaignService;
import com.space.space_bundle.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class SmsController {
    private final WalletService walletService;
    private final SmsCampaignRepository campaignRepository;
    private final SmsPackageRepository smsPackageRepository;
    private final SmsSenderIdRequestRepository senderIdRepository;
    private final SmsCampaignService campaignService;
    private final MoolreSmsService moolreSmsService;


    @GetMapping("/packages")
    public ResponseEntity<ApiResponse<List<SmsPackage>>> packages() {
        return ResponseEntity.ok(ApiResponse.success(smsPackageRepository.findByActiveTrueOrderByPriceAsc()));
    }

    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> balance(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(Map.of("smsBalance", walletService.getSmsBalance(user.getUserId()))));
    }

    @PostMapping("/buy-package/{packageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> buyPackage(@PathVariable String packageId,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails user) {
        SmsPackage smsPackage = smsPackageRepository.findById(packageId).filter(SmsPackage::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive SMS package"));
        if (smsPackage.getMessagesCount() <= 0 || smsPackage.getPrice() == null || smsPackage.getPrice().signum() <= 0)
            throw new IllegalStateException("SMS package is not configured correctly");
        String userId = user.getUserId();
        walletService.debit(userId, smsPackage.getPrice(), "SMSPKG_" + UUID.randomUUID(),
                "Purchased SMS Package: " + smsPackage.getName());
        int newBalance;
        try {
            newBalance = walletService.atomicSmsCredit(userId, smsPackage.getMessagesCount());
            if (newBalance <= 0) throw new IllegalStateException("SMS credit grant did not complete");
        } catch (RuntimeException failure) {
            walletService.credit(userId, smsPackage.getPrice(), "Compensation for incomplete SMS package purchase");
            throw failure;
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "SMS credits added",
                "smsBalance", newBalance, "packageName", smsPackage.getName(), "credits", smsPackage.getMessagesCount())));
    }

    @PostMapping("/sender-ids")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SmsSenderIdRequest>> requestSenderId(@RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails user) {
        String senderId = request.getOrDefault("senderId", "").trim();
        if (senderId.isBlank() || senderId.length() > 11 || !senderId.matches("(?=.*[A-Za-z])[A-Za-z0-9]+"))
            throw new IllegalArgumentException("Sender ID must be 1-11 letters and numbers, contain at least one letter, and contain no special characters");
        if (senderIdRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId()).stream()
                .anyMatch(existing -> existing.getSenderId().equalsIgnoreCase(senderId)))
            throw new IllegalStateException("You have already submitted this sender ID");
        SmsSenderIdRequest entity = SmsSenderIdRequest.builder().id(UUID.randomUUID().toString())
                .userId(user.getUserId()).senderId(senderId).status("PENDING").providerStatus("UNKNOWN")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        return ResponseEntity.ok(ApiResponse.success(senderIdRepository.save(entity)));
    }

    @GetMapping("/sender-ids")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SmsSenderIdRequest>>> senderIds(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(senderIdRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId())));
    }

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SmsCampaign>> send(@RequestBody SmsSendRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(campaignService.send(user.getUserId(), request)));
    }

    @GetMapping("/campaigns")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SmsCampaign>>> campaigns(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(campaignRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId())));
    }
}

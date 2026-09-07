package com.space.space_bundle.controller;

import com.space.space_bundle.dto.ApiResponse;
import com.space.space_bundle.dto.SmsSendRequest;
import com.space.space_bundle.entity.SmsCampaign;
import com.space.space_bundle.entity.SmsPackage;
import com.space.space_bundle.repository.SmsCampaignRepository;
import com.space.space_bundle.repository.SmsPackageRepository;
import com.space.space_bundle.security.CustomUserDetailsService;
import com.space.space_bundle.service.MoolreSmsService;
import com.space.space_bundle.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class SmsController {

    private final MoolreSmsService moolreSmsService;
    private final WalletService walletService;
    private final SmsCampaignRepository smsCampaignRepository;
    private final SmsPackageRepository smsPackageRepository;

    @Value("${moolre.sms.price-per-page:0.04}")
    private double pricePerPage;

    @Value("${moolre.sms.sender-ids:Moolre}")
    private List<String> allowedSenderIds;

    @GetMapping("/packages")
    public ResponseEntity<ApiResponse<List<SmsPackage>>> getActivePackages() {
        List<SmsPackage> packages = smsPackageRepository.findByActiveTrueOrderByPriceAsc();
        return ResponseEntity.ok(ApiResponse.success(packages));
    }

    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSmsBalance(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        int smsBalance = walletService.getSmsBalance(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("smsBalance", smsBalance)));
    }

    @PostMapping("/buy-package/{packageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> buyPackage(
            @PathVariable String packageId,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        String userId = userDetails.getUserId();
        SmsPackage smsPkg = smsPackageRepository.findById(packageId)
                .orElse(null);

        if (smsPkg == null || !smsPkg.isActive()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or inactive SMS package"));
        }

        BigDecimal price = smsPkg.getPrice();
        BigDecimal balance = walletService.getBalance(userId);

        if (balance.compareTo(price) < 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Insufficient wallet balance to purchase this package. Required: GHS " + price));
        }

        // Debit wallet and credit SMS credits
        String txId = "SMSPKG_" + System.currentTimeMillis();
        walletService.debit(userId, price, txId, "Purchased SMS Package: " + smsPkg.getName() + " (" + smsPkg.getMessagesCount() + " messages)");
        int newBalance = walletService.atomicSmsCredit(userId, smsPkg.getMessagesCount());

        log.info("User {} purchased SMS package {} ({} messages) for GHS {}. New SMS balance: {}",
                userId, smsPkg.getName(), smsPkg.getMessagesCount(), price, newBalance);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "message", "Successfully purchased " + smsPkg.getMessagesCount() + " SMS credits",
                "smsBalance", newBalance,
                "packageName", smsPkg.getName()
        )));
    }

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SmsCampaign>> sendSms(
            @Valid @RequestBody SmsSendRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        // Validate sender ID
        if (!allowedSenderIds.contains(request.getSenderId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Sender ID not approved"));
        }

        // Calculate pages and total credits required
        int pages = MoolreSmsService.calculatePages(request.getMessage());
        int recipientsCount = request.getRecipients().size();
        int totalRequiredCredits = pages * recipientsCount;

        int userSmsBalance = walletService.getSmsBalance(userId);
        boolean usingSmsCredits = userSmsBalance >= totalRequiredCredits;
        double totalCost = pages * recipientsCount * pricePerPage;
        BigDecimal costBd = BigDecimal.valueOf(totalCost);

        if (usingSmsCredits) {
            Integer updatedBalance = walletService.atomicSmsDebit(userId, totalRequiredCredits);
            if (updatedBalance == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Insufficient SMS credits"));
            }
        } else {
            // Check wallet cash balance
            BigDecimal currentBalance = walletService.getBalance(userId);
            if (currentBalance.compareTo(costBd) < 0) {
                return ResponseEntity.badRequest().body(ApiResponse.error(
                        "Insufficient SMS credits (" + userSmsBalance + " credits available) and insufficient wallet cash balance (GHS " + currentBalance + "). Required: " + totalRequiredCredits + " SMS credits or GHS " + totalCost
                ));
            }

            // Deduct cash from wallet
            String campaignId = "SMS_" + System.currentTimeMillis();
            walletService.debit(userId, costBd, campaignId, "Bulk SMS: " + recipientsCount + " recipients");
        }

        // Create campaign record
        SmsCampaign campaign = new SmsCampaign();
        campaign.setUserId(userId);
        campaign.setSenderId(request.getSenderId());
        campaign.setMessage(request.getMessage());
        campaign.setRecipients(request.getRecipients());
        campaign.setPages(pages);
        campaign.setTotalCost(totalCost);
        campaign.setStatus("PROCESSING");

        SmsCampaign savedCampaign = smsCampaignRepository.save(campaign);

        try {
            // Dispatch via Moolre
            List<String> refs = moolreSmsService.sendBulkSms(request.getSenderId(), request.getMessage(), request.getRecipients());

            savedCampaign.setMoolreRefs(refs);
            savedCampaign.setStatus("DISPATCHED");
            smsCampaignRepository.save(savedCampaign);

            return ResponseEntity.ok(ApiResponse.success(savedCampaign));
        } catch (Exception e) {
            log.error("Failed to dispatch SMS", e);
            // Refund the user if Moolre fails immediately
            if (usingSmsCredits) {
                walletService.atomicSmsCredit(userId, totalRequiredCredits);
            } else {
                walletService.credit(userId, costBd, "Refund for failed SMS campaign");
            }

            savedCampaign.setStatus("FAILED");
            smsCampaignRepository.save(savedCampaign);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to send SMS: " + e.getMessage()));
        }
    }

    @GetMapping("/campaigns")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SmsCampaign>>> getCampaigns(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        List<SmsCampaign> campaigns = smsCampaignRepository.findByUserIdOrderByCreatedAtDesc(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(campaigns));
    }

    @GetMapping("/sender-ids")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<String>>> getSenderIds() {
        return ResponseEntity.ok(ApiResponse.success(allowedSenderIds));
    }
}

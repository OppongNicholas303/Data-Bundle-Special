package com.space.space_bundle.controller;

import com.space.space_bundle.dto.ApiResponse;
import com.space.space_bundle.dto.SmsSendRequest;
import com.space.space_bundle.entity.SmsCampaign;
import com.space.space_bundle.repository.SmsCampaignRepository;
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

@Slf4j
@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class SmsController {

    private final MoolreSmsService moolreSmsService;
    private final WalletService walletService;
    private final SmsCampaignRepository smsCampaignRepository;

    @Value("${moolre.sms.price-per-page:0.04}")
    private double pricePerPage;

    @Value("${moolre.sms.sender-ids:Moolre}")
    private List<String> allowedSenderIds;

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

        // Calculate pages and cost
        int pages = MoolreSmsService.calculatePages(request.getMessage());
        int recipientsCount = request.getRecipients().size();
        double totalCost = pages * recipientsCount * pricePerPage;
        BigDecimal costBd = BigDecimal.valueOf(totalCost);

        // Check wallet balance
        BigDecimal currentBalance = walletService.getBalance(userId);
        if (currentBalance.compareTo(costBd) < 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Insufficient wallet balance. Cost is GHS " + totalCost));
        }

        // Deduct from wallet
        String campaignId = "SMS_" + System.currentTimeMillis();
        walletService.debit(userId, costBd, campaignId, "Bulk SMS: " + recipientsCount + " recipients");

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
            walletService.credit(userId, costBd, "Refund for failed SMS campaign");
            
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

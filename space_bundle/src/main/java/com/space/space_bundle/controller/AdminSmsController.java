package com.space.space_bundle.controller;

import com.space.space_bundle.dto.ApiResponse;
import com.space.space_bundle.entity.SmsCampaign;
import com.space.space_bundle.entity.SmsSenderIdRequest;
import com.space.space_bundle.entity.SmsOperationsSettings;
import com.space.space_bundle.repository.SmsCampaignRepository;
import com.space.space_bundle.repository.SmsSenderIdRequestRepository;
import com.space.space_bundle.repository.SmsOperationsSettingsRepository;
import com.space.space_bundle.service.SmsCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/sms")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSmsController {
    private final SmsSenderIdRequestRepository senderIdRepository;
    private final SmsCampaignRepository campaignRepository;
    private final SmsCampaignService campaignService;
    private final SmsOperationsSettingsRepository settingsRepository;

    @GetMapping("/sender-ids")
    public ResponseEntity<ApiResponse<List<SmsSenderIdRequest>>> senderIds() {
        return ResponseEntity.ok(ApiResponse.success(senderIdRepository.findAllByOrderByCreatedAtDesc()));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<SmsOperationsSettings>> settings() {
        return ResponseEntity.ok(ApiResponse.success(settingsRepository.findById("sms")
                .orElseGet(() -> SmsOperationsSettings.builder().id("sms").sendingEnabled(false).providerBundleReady(false).build())));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<SmsOperationsSettings>> updateSettings(@RequestBody Map<String, Object> body, Authentication authentication) {
        SmsOperationsSettings settings = settingsRepository.findById("sms")
                .orElseGet(() -> SmsOperationsSettings.builder().id("sms").sendingEnabled(false).providerBundleReady(false).build());
        if (body.get("providerBundleReady") instanceof Boolean ready) settings.setProviderBundleReady(ready);
        if (body.get("sendingEnabled") instanceof Boolean enabled) settings.setSendingEnabled(enabled);
        if (body.containsKey("providerBundleBalanceNote")) settings.setProviderBundleBalanceNote(String.valueOf(body.get("providerBundleBalanceNote")));
        if (!settings.isProviderBundleReady()) settings.setSendingEnabled(false);
        settings.setUpdatedBy(authentication.getName());
        settings.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(ApiResponse.success(settingsRepository.save(settings)));
    }

    @PutMapping("/sender-ids/{id}/review")
    public ResponseEntity<ApiResponse<SmsSenderIdRequest>> reviewSenderId(@PathVariable String id,
            @RequestBody Map<String, String> body, Authentication authentication) {
        SmsSenderIdRequest request = senderIdRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Sender ID request not found"));
        String decision = body.getOrDefault("decision", "").toUpperCase();
        if (!List.of("ADMIN_APPROVED", "APPROVED", "REJECTED").contains(decision)) throw new IllegalArgumentException("Decision must be ADMIN_APPROVED, APPROVED or REJECTED");
        if ("APPROVED".equals(decision) && !"ADMIN_APPROVED".equals(request.getStatus()))
            throw new IllegalStateException("Review the sender ID request before activating it");
        request.setStatus(decision);
        request.setReviewNote(body.get("note"));
        request.setReviewedBy(authentication.getName());
        request.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(ApiResponse.success(senderIdRepository.save(request)));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<ApiResponse<List<SmsCampaign>>> campaigns() {
        return ResponseEntity.ok(ApiResponse.success(campaignRepository.findAllByOrderByCreatedAtDesc()));
    }

    @PostMapping("/campaigns/{id}/retry")
    public ResponseEntity<ApiResponse<SmsCampaign>> retry(@PathVariable String id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(campaignService.retry(id, authentication.getName())));
    }

    @PostMapping("/campaigns/{id}/recover-credit-reservation")
    public ResponseEntity<ApiResponse<SmsCampaign>> recoverReservation(@PathVariable String id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(campaignService.resumeCreditReservation(id, authentication.getName())));
    }

    @PostMapping("/campaigns/{id}/reconcile")
    public ResponseEntity<ApiResponse<SmsCampaign>> reconcile(@PathVariable String id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(campaignService.reconcile(id, authentication.getName())));
    }

    @PutMapping("/campaigns/{id}/recipients/{reference}/resolve")
    public ResponseEntity<ApiResponse<SmsCampaign>> resolveRecipient(@PathVariable String id,
            @PathVariable String reference, @RequestBody Map<String, String> body, Authentication authentication) {
        String outcome = body.getOrDefault("outcome", "").toUpperCase();
        return ResponseEntity.ok(ApiResponse.success(campaignService.resolveRecipient(id, reference, outcome, authentication.getName())));
    }

    @PostMapping("/campaigns/{id}/cancel")
    public ResponseEntity<ApiResponse<SmsCampaign>> cancel(@PathVariable String id, @RequestBody Map<String, String> body,
            Authentication authentication) {
        String reason = body.getOrDefault("reason", "").trim();
        if (reason.isBlank()) throw new IllegalArgumentException("A cancellation reason is required");
        return ResponseEntity.ok(ApiResponse.success(campaignService.cancelAndRelease(id, authentication.getName(), reason)));
    }
}

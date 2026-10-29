package com.space.space_bundle.service;

import com.space.space_bundle.dto.SmsSendRequest;
import com.space.space_bundle.entity.SmsCampaign;
import com.space.space_bundle.entity.SmsSenderIdRequest;
import com.space.space_bundle.entity.SmsOperationsSettings;
import com.space.space_bundle.repository.SmsCampaignRepository;
import com.space.space_bundle.repository.SmsSenderIdRequestRepository;
import com.space.space_bundle.repository.SmsOperationsSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmsCampaignService {
    private final SmsCampaignRepository campaignRepository;
    private final SmsSenderIdRequestRepository senderIdRepository;
    private final WalletService walletService;
    private final MoolreSmsService moolre;
    private final MongoTemplate mongoTemplate;
    private final SmsOperationsSettingsRepository settingsRepository;

    private static final Pattern PHONE = Pattern.compile("^\\+?[1-9][0-9]{7,14}$");
    private static final int MAX_RECIPIENTS = 500;
    private static final int MAX_MESSAGE_SEGMENTS = 10;

    public SmsCampaign send(String userId, SmsSendRequest request) {
        requireProviderReady();
        validateRequest(request);
        senderIdRepository.findByUserIdAndSenderIdAndStatus(userId, request.getSenderId(), "APPROVED")
                .orElseThrow(() -> new IllegalArgumentException("Sender ID must be approved by an admin"));

        int segments = MoolreSmsService.calculatePages(request.getMessage());
        if (segments > MAX_MESSAGE_SEGMENTS)
            throw new IllegalArgumentException("Message is too long (maximum " + MAX_MESSAGE_SEGMENTS + " SMS segments)");
        int credits = Math.multiplyExact(segments, request.getRecipients().size());
        String campaignId = UUID.randomUUID().toString();
        List<String> normalizedRecipients = request.getRecipients().stream().map(String::trim).toList();
        List<SmsCampaign.RecipientDelivery> delivery = normalizedRecipients.stream()
                .map(recipient -> new SmsCampaign.RecipientDelivery(recipient, UUID.randomUUID().toString(), "QUEUED", segments))
                .collect(Collectors.toCollection(ArrayList::new));
        SmsCampaign campaign = new SmsCampaign();
        campaign.setId(campaignId);
        campaign.setUserId(userId);
        campaign.setSenderId(request.getSenderId());
        campaign.setMessage(request.getMessage());
        campaign.setRecipients(new ArrayList<>(normalizedRecipients));
        campaign.setPages(segments);
        campaign.setCreditsReserved(credits);
        campaign.setCreditReservationStatus("PENDING");
        campaign.setStatus("PENDING_CREDIT_RESERVATION");
        campaign.setDeliveryDetails(delivery);
        campaign = campaignRepository.save(campaign);
        if (walletService.atomicSmsDebitOnce(userId, credits, "sms-campaign-reserve:" + campaignId) == null) {
            campaignRepository.delete(campaign);
            throw new IllegalArgumentException("Insufficient SMS credits. Buy an SMS package to continue.");
        }
        campaign.setCreditReservationStatus("RESERVED");
        campaign.setStatus("SUBMITTING");
        campaign = campaignRepository.save(campaign);
        return dispatch(campaign, null);
    }

    public SmsCampaign resumeCreditReservation(String campaignId, String adminId) {
        SmsCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("SMS campaign not found"));
        if ("PENDING_CREDIT_RESERVATION".equals(campaign.getStatus())) {
            Query claim = new Query(Criteria.where("id").is(campaignId).and("status").is("PENDING_CREDIT_RESERVATION"));
            if (mongoTemplate.updateFirst(claim, new Update().set("status", "RESERVING_CREDITS")
                    .set("lastAdminActionBy", adminId).set("lastAdminActionAt", LocalDateTime.now()), SmsCampaign.class).getModifiedCount() == 0)
                throw new IllegalStateException("Another admin is already recovering this campaign");
        } else if (!"RESERVING_CREDITS".equals(campaign.getStatus()) || campaign.getLastAdminActionAt() == null
                || campaign.getLastAdminActionAt().isAfter(LocalDateTime.now().minusMinutes(5))) {
            throw new IllegalStateException("Campaign is not eligible for credit reservation recovery");
        } else {
            Query claim = new Query(Criteria.where("id").is(campaignId).and("status").is("RESERVING_CREDITS")
                    .and("lastAdminActionAt").lte(LocalDateTime.now().minusMinutes(5)));
            if (mongoTemplate.updateFirst(claim, new Update().set("lastAdminActionBy", adminId)
                    .set("lastAdminActionAt", LocalDateTime.now()), SmsCampaign.class).getModifiedCount() == 0)
                throw new IllegalStateException("Another admin is already recovering this campaign");
        }
        campaign = campaignRepository.findById(campaignId).orElseThrow();
        if (walletService.atomicSmsDebitOnce(campaign.getUserId(), campaign.getCreditsReserved(),
                "sms-campaign-reserve:" + campaignId) == null) {
            campaign.setStatus("INSUFFICIENT_CREDITS");
            return campaignRepository.save(campaign);
        }
        campaign.setCreditReservationStatus("RESERVED");
        campaign.setStatus("SUBMITTING");
        campaign.setLastAdminActionBy(adminId);
        campaign.setLastAdminActionAt(LocalDateTime.now());
        return dispatch(campaignRepository.save(campaign), adminId);
    }

    private void validateRequest(SmsSendRequest request) {
        if (request.getSenderId() == null || request.getSenderId().isBlank() || request.getSenderId().length() > 11)
            throw new IllegalArgumentException("Choose a sender ID of at most 11 characters");
        if (request.getMessage() == null || request.getMessage().isBlank())
            throw new IllegalArgumentException("Message cannot be empty");
        if (request.getRecipients() == null || request.getRecipients().isEmpty() || request.getRecipients().size() > MAX_RECIPIENTS)
            throw new IllegalArgumentException("A campaign must contain between 1 and " + MAX_RECIPIENTS + " recipients");
        if (request.getRecipients().stream().anyMatch(number -> number == null || !PHONE.matcher(number.trim()).matches()))
            throw new IllegalArgumentException("Every recipient must be a valid international phone number");
        if (request.getRecipients().stream().map(String::trim).map(String::toLowerCase).distinct().count() != request.getRecipients().size())
            throw new IllegalArgumentException("Duplicate recipient phone numbers are not allowed");
    }

    public SmsCampaign retry(String campaignId, String adminId) {
        requireProviderReady();
        SmsCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("SMS campaign not found"));
        if (!List.of("FAILED_NEEDS_ADMIN_ACTION", "PARTIALLY_DELIVERED").contains(campaign.getStatus()))
            throw new IllegalStateException("Only definitively failed recipients can be retried safely");
        if (campaign.getDeliveryDetails().stream().noneMatch(r -> "FAILED".equals(r.getStatus()) && !r.isCreditsReleased()))
            throw new IllegalStateException("There are no failed, credit-reserved recipients to retry");
        Query claim = new Query(Criteria.where("id").is(campaignId).and("status").is(campaign.getStatus())
                .and("creditsReleased").ne(true));
        Update update = new Update().set("status", "SUBMITTING").set("lastAdminActionBy", adminId)
                .set("lastAdminActionAt", LocalDateTime.now());
        if (mongoTemplate.updateFirst(claim, update, SmsCampaign.class).getModifiedCount() == 0)
            throw new IllegalStateException("Campaign is already being processed or is no longer retryable");
        campaign = campaignRepository.findById(campaignId).orElseThrow();
        return dispatch(campaign, adminId);
    }

    private void requireProviderReady() {
        SmsOperationsSettings settings = settingsRepository.findById("sms").orElse(null);
        if (settings == null || !settings.isSendingEnabled() || !settings.isProviderBundleReady())
            throw new IllegalStateException("SMS sending is paused by admin until the Moolre bundle is confirmed ready");
    }

    private SmsCampaign dispatch(SmsCampaign campaign, String adminId) {
        List<SmsCampaign.RecipientDelivery> pending = campaign.getDeliveryDetails().stream()
                .filter(item -> "QUEUED".equals(item.getStatus()) || ("FAILED".equals(item.getStatus()) && !item.isCreditsReleased()))
                .toList();
        List<String> recipients = pending.stream().map(SmsCampaign.RecipientDelivery::getRecipient).toList();
        List<String> refs = pending.stream().map(SmsCampaign.RecipientDelivery::getReference).toList();
        if (pending.isEmpty()) throw new IllegalStateException("No eligible recipients remain in this campaign");
        try {
            moolre.sendBulkSms(campaign.getSenderId(), campaign.getMessage(), recipients, refs);
            campaign.setMoolreRefs(new ArrayList<>(refs));
            campaign.setStatus(campaign.getDeliveryDetails().stream().anyMatch(item -> "DELIVERED".equals(item.getStatus()))
                    ? "PARTIALLY_DELIVERED" : "ACCEPTED");
            campaign.setFailureCode(null);
            campaign.setFailureReason(null);
            campaign.setProviderResponseMessage("Moolre accepted the batch; delivery is not yet confirmed.");
            pending.forEach(item -> item.setStatus("ACCEPTED"));
        } catch (MoolreSmsService.MoolreSmsException e) {
            boolean hasDelivered = campaign.getDeliveryDetails().stream().anyMatch(item -> "DELIVERED".equals(item.getStatus()));
            campaign.setStatus(e.isDefinitiveRejection()
                    ? (hasDelivered ? "PARTIALLY_DELIVERED" : "FAILED_NEEDS_ADMIN_ACTION") : "UNKNOWN");
            campaign.setFailureCode(e.getProviderCode());
            campaign.setFailureReason(e.getMessage());
            campaign.setProviderResponseMessage(e.isDefinitiveRejection()
                    ? "Moolre definitively rejected the batch; reserved user credits remain held for admin retry or cancellation."
                    : "The provider outcome is unknown. Reconcile before retrying; do not resend blindly.");
            pending.forEach(item -> item.setStatus(e.isDefinitiveRejection() ? "FAILED" : "UNKNOWN"));
            pauseSending(e.getProviderCode(), e.getMessage());
        }
        campaign.setLastAdminActionBy(adminId == null ? campaign.getLastAdminActionBy() : adminId);
        campaign.setLastAdminActionAt(adminId == null ? campaign.getLastAdminActionAt() : LocalDateTime.now());
        return campaignRepository.save(campaign);
    }

    private void pauseSending(String code, String reason) {
        SmsOperationsSettings settings = settingsRepository.findById("sms")
                .orElseGet(() -> SmsOperationsSettings.builder().id("sms").build());
        settings.setSendingEnabled(false);
        settings.setProviderBundleReady(false);
        settings.setProviderBundleBalanceNote("Paused after Moolre response " + (code == null ? "UNKNOWN" : code) + ": " + reason);
        settings.setUpdatedAt(LocalDateTime.now());
        settingsRepository.save(settings);
    }

    public SmsCampaign cancelAndRelease(String campaignId, String adminId, String reason) {
        SmsCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("SMS campaign not found"));
        if ("CANCELLED".equals(campaign.getStatus()) && campaign.isCreditsReleased()) return campaign;
        if (!List.of("FAILED_NEEDS_ADMIN_ACTION", "PARTIALLY_DELIVERED", "CANCELLING").contains(campaign.getStatus()) || campaign.isCreditsReleased())
            throw new IllegalStateException("Only campaigns with confirmed failed recipients can be canceled");
        int releasable = campaign.getDeliveryDetails().stream().filter(item -> "FAILED".equals(item.getStatus()) && !item.isCreditsReleased())
                .mapToInt(SmsCampaign.RecipientDelivery::getCredits).sum();
        if (releasable <= 0) throw new IllegalStateException("No confirmed failed recipient credits can be released");
        if (!"CANCELLING".equals(campaign.getStatus())) {
            Query claim = new Query(Criteria.where("id").is(campaignId).and("status").is(campaign.getStatus())
                    .and("creditsReleased").ne(true));
            Update update = new Update().set("status", "CANCELLING").set("lastAdminActionBy", adminId)
                    .set("lastAdminActionAt", LocalDateTime.now()).set("providerResponseMessage", "Cancellation in progress: " + reason);
            if (mongoTemplate.updateFirst(claim, update, SmsCampaign.class).getModifiedCount() == 0)
                throw new IllegalStateException("Campaign was already handled");
        }
        campaign = campaignRepository.findById(campaignId).orElseThrow();
        if (walletService.atomicSmsCreditOnce(campaign.getUserId(), releasable, "sms-campaign-release:" + campaignId) == null)
            throw new IllegalStateException("Campaign credits could not be released; cancellation can be safely retried");
        campaign.setStatus("CANCELLED");
        campaign.setCreditsReleased(true);
        campaign.setProviderResponseMessage("Cancelled by admin: " + reason);
        campaign.setLastAdminActionBy(adminId);
        campaign.setLastAdminActionAt(LocalDateTime.now());
        campaign.getDeliveryDetails().stream().filter(item -> "FAILED".equals(item.getStatus())).forEach(item -> item.setCreditsReleased(true));
        return campaignRepository.save(campaign);
    }

    public SmsCampaign resolveRecipient(String campaignId, String reference, String outcome, String adminId) {
        if (!List.of("DELIVERED", "FAILED").contains(outcome))
            throw new IllegalArgumentException("Resolution must be DELIVERED or FAILED");
        SmsCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("SMS campaign not found"));
        String previousStatus = campaign.getStatus();
        if (!List.of("UNKNOWN", "ACCEPTED", "PARTIALLY_DELIVERED").contains(previousStatus))
            throw new IllegalStateException("Campaign is not awaiting recipient delivery resolution");
        Query claim = new Query(Criteria.where("id").is(campaignId).and("status").is(previousStatus));
        if (mongoTemplate.updateFirst(claim, new Update().set("status", "RECONCILING"), SmsCampaign.class).getModifiedCount() == 0)
            throw new IllegalStateException("Another admin is already processing this campaign");
        campaign = campaignRepository.findById(campaignId).orElseThrow();
        SmsCampaign.RecipientDelivery recipient = campaign.getDeliveryDetails().stream()
                .filter(item -> item.getReference().equals(reference)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Recipient reference not found"));
        if (recipient.isCreditsReleased()) throw new IllegalStateException("Credits for this recipient were already released");
        if ("DELIVERED".equals(recipient.getStatus())) throw new IllegalStateException("Delivered recipient cannot be changed");
        recipient.setStatus(outcome);
        boolean hasUnresolved = campaign.getDeliveryDetails().stream().anyMatch(item ->
                List.of("UNKNOWN", "ACCEPTED", "QUEUED").contains(item.getStatus()) || item.getStatus().startsWith("PROVIDER_STATUS_"));
        boolean anyDelivered = campaign.getDeliveryDetails().stream().anyMatch(item -> "DELIVERED".equals(item.getStatus()));
        boolean anyFailed = campaign.getDeliveryDetails().stream().anyMatch(item -> "FAILED".equals(item.getStatus()) && !item.isCreditsReleased());
        campaign.setStatus(hasUnresolved ? "UNKNOWN" : anyDelivered && anyFailed ? "PARTIALLY_DELIVERED"
                : anyFailed ? "FAILED_NEEDS_ADMIN_ACTION" : "COMPLETED");
        campaign.setLastAdminActionBy(adminId);
        campaign.setLastAdminActionAt(LocalDateTime.now());
        return campaignRepository.save(campaign);
    }

    public SmsCampaign reconcile(String campaignId, String adminId) {
        SmsCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("SMS campaign not found"));
        if (!List.of("UNKNOWN", "ACCEPTED", "PARTIALLY_DELIVERED", "SUBMITTING", "RECONCILING").contains(campaign.getStatus()))
            throw new IllegalStateException("Campaign does not need provider reconciliation");
        List<String> refs = campaign.getDeliveryDetails().stream().map(SmsCampaign.RecipientDelivery::getReference).toList();
        Map<String, Integer> providerStatuses = moolre.checkMessageStatuses(refs);
        for (SmsCampaign.RecipientDelivery item : campaign.getDeliveryDetails()) {
            Integer raw = providerStatuses.get(item.getReference());
            if (raw != null) item.setStatus("PROVIDER_STATUS_" + raw);
        }
        campaign.setProviderResponseMessage("Raw Moolre delivery statuses refreshed. Admin must interpret unresolved provider states before retry or credit release.");
        if (List.of("RECONCILING", "SUBMITTING").contains(campaign.getStatus())) campaign.setStatus("UNKNOWN");
        campaign.setLastAdminActionBy(adminId);
        campaign.setLastAdminActionAt(LocalDateTime.now());
        return campaignRepository.save(campaign);
    }
}

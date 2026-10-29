package com.space.space_bundle.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "sms_campaigns")
public class SmsCampaign {
    @Id
    private String id;
    private String userId;
    private String senderId;
    private String message;
    private List<String> recipients;
    private int pages;
    private double totalCost;
    private String status;
    private LocalDateTime createdAt;
    private List<String> moolreRefs;
    private int creditsReserved;
    private String creditReservationStatus; // PENDING, RESERVED, RELEASED
    private boolean creditsReleased;
    private String failureCode;
    private String failureReason;
    private String lastAdminActionBy;
    private LocalDateTime lastAdminActionAt;
    private List<RecipientDelivery> deliveryDetails;
    private String providerResponseMessage;

    public static class RecipientDelivery {
        private String recipient;
        private String reference;
        private String status; // QUEUED, ACCEPTED, DELIVERED, FAILED, UNKNOWN
        private int credits;
        private boolean creditsReleased;

        public RecipientDelivery() {}
        public RecipientDelivery(String recipient, String reference, String status, int credits) {
            this.recipient = recipient; this.reference = reference; this.status = status; this.credits = credits;
        }
        public String getRecipient() { return recipient; }
        public void setRecipient(String recipient) { this.recipient = recipient; }
        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getCredits() { return credits; }
        public void setCredits(int credits) { this.credits = credits; }
        public boolean isCreditsReleased() { return creditsReleased; }
        public void setCreditsReleased(boolean creditsReleased) { this.creditsReleased = creditsReleased; }
    }

    public SmsCampaign() {
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getRecipients() { return recipients; }
    public void setRecipients(List<String> recipients) { this.recipients = recipients; }

    public int getPages() { return pages; }
    public void setPages(int pages) { this.pages = pages; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<String> getMoolreRefs() { return moolreRefs; }
    public void setMoolreRefs(List<String> moolreRefs) { this.moolreRefs = moolreRefs; }
    public int getCreditsReserved() { return creditsReserved; }
    public void setCreditsReserved(int creditsReserved) { this.creditsReserved = creditsReserved; }
    public String getCreditReservationStatus() { return creditReservationStatus; }
    public void setCreditReservationStatus(String creditReservationStatus) { this.creditReservationStatus = creditReservationStatus; }
    public boolean isCreditsReleased() { return creditsReleased; }
    public void setCreditsReleased(boolean creditsReleased) { this.creditsReleased = creditsReleased; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getLastAdminActionBy() { return lastAdminActionBy; }
    public void setLastAdminActionBy(String lastAdminActionBy) { this.lastAdminActionBy = lastAdminActionBy; }
    public LocalDateTime getLastAdminActionAt() { return lastAdminActionAt; }
    public void setLastAdminActionAt(LocalDateTime lastAdminActionAt) { this.lastAdminActionAt = lastAdminActionAt; }
    public List<RecipientDelivery> getDeliveryDetails() { return deliveryDetails; }
    public void setDeliveryDetails(List<RecipientDelivery> deliveryDetails) { this.deliveryDetails = deliveryDetails; }
    public String getProviderResponseMessage() { return providerResponseMessage; }
    public void setProviderResponseMessage(String providerResponseMessage) { this.providerResponseMessage = providerResponseMessage; }
}

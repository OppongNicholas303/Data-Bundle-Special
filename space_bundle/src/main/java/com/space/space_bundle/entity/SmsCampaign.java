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
}

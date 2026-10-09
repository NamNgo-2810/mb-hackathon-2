package com.mb.callassistant.lead;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Hoạt động trong lịch sử của lead (cuộc gọi, hẹn gặp, SMS, khác). Hoạt động
 * loại {@link ActivityType#CUOC_GOI} mang bản sao tóm tắt/gợi ý AI (do U4 ghi
 * qua {@code LeadService.recordCallActivity}) để lịch sử hiển thị kể cả khi AI
 * lỗi (US3.1, BR2.3). U1 sở hữu thực thể này.
 */
@Entity
@Table(name = "activity")
public class Activity {

    @Id
    @Column(name = "activity_id")
    private String activityId;

    @Column(name = "lead_id", nullable = false)
    private String leadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ActivityType type;

    /** Thời điểm xảy ra, ISO-8601 +07:00 (BR6.2). */
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "product")
    private String product;

    @Column(name = "result")
    private String result;

    @Lob
    @Column(name = "note")
    private String note;

    /** Chỉ có với type=CUOC_GOI. */
    @Column(name = "call_id")
    private String callId;

    /** null = "Không có tóm tắt AI" (AC3.1.3, BR2.3). */
    @Lob
    @Column(name = "ai_summary")
    private String aiSummary;

    @ElementCollection
    @CollectionTable(name = "activity_ai_suggestion", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "suggestion")
    private List<String> aiSuggestions = new ArrayList<>();

    protected Activity() {
        // JPA
    }

    public Activity(String activityId, String leadId, ActivityType type, OffsetDateTime occurredAt) {
        this.activityId = activityId;
        this.leadId = leadId;
        this.type = type;
        this.occurredAt = occurredAt;
    }

    public String getActivityId() {
        return activityId;
    }

    public String getLeadId() {
        return leadId;
    }

    public void setLeadId(String leadId) {
        this.leadId = leadId;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public List<String> getAiSuggestions() {
        return aiSuggestions;
    }

    public void setAiSuggestions(List<String> aiSuggestions) {
        this.aiSuggestions = aiSuggestions == null ? new ArrayList<>() : aiSuggestions;
    }
}

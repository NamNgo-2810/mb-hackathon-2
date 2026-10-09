package com.mb.callassistant.analysis;

import com.mb.callassistant.lead.LeadStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Hồ sơ keyin đã lưu của một cuộc gọi (U4, nguồn chân lý {@code entities.md}).
 * Lưu idempotent theo {@code callId} ({@code callId} unique, upsert tại chỗ,
 * AC2.3.6/BR5.2) trong một giao dịch cùng updateStage + recordCallActivity +
 * createFromCall (BR5.3). Hai trường bắt buộc: {@code leadStage},
 * {@code callResult} (BR5.1).
 *
 * <p>Quy ước dữ liệu (BR8.3): {@code demandAmount} là integer VND;
 * {@code agreedRate} là decimal %/năm; {@code savedAt} là ISO-8601 +07:00.
 * {@code fieldSources} (tên-trường → {@link FieldSource}) lưu dưới dạng JSON.
 */
@Entity
@Table(name = "call_keyin")
public class CallKeyin {

    @Id
    @Column(name = "keyin_id")
    private String keyinId;

    @Column(name = "call_id", nullable = false, unique = true)
    private String callId;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_stage", nullable = false)
    private LeadStage leadStage;

    @Column(name = "call_result", nullable = false)
    private String callResult;

    /** VND, số nguyên (BR8.3). */
    @Column(name = "demand_amount")
    private Long demandAmount;

    @Column(name = "term")
    private String term;

    /** Lãi suất %/năm, 2 chữ số thập phân (BR8.3). */
    @Column(name = "agreed_rate")
    private BigDecimal agreedRate;

    @Column(name = "product")
    private String product;

    @Column(name = "source_of_funds")
    private String sourceOfFunds;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "channel")
    private String channel;

    @Lob
    @Column(name = "objections")
    private String objections;

    @Lob
    @Column(name = "rm_note")
    private String rmNote;

    /** JSON: tên-trường → {@link FieldSource} (BR4.3). */
    @Lob
    @Column(name = "field_sources_json", nullable = false)
    private String fieldSourcesJson;

    /** ISO-8601 +07:00 (BR8.3). */
    @Column(name = "saved_at", nullable = false)
    private OffsetDateTime savedAt;

    protected CallKeyin() {
        // JPA
    }

    public CallKeyin(String keyinId, String callId) {
        this.keyinId = keyinId;
        this.callId = callId;
    }

    public String getKeyinId() {
        return keyinId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public LeadStage getLeadStage() {
        return leadStage;
    }

    public void setLeadStage(LeadStage leadStage) {
        this.leadStage = leadStage;
    }

    public String getCallResult() {
        return callResult;
    }

    public void setCallResult(String callResult) {
        this.callResult = callResult;
    }

    public Long getDemandAmount() {
        return demandAmount;
    }

    public void setDemandAmount(Long demandAmount) {
        this.demandAmount = demandAmount;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public BigDecimal getAgreedRate() {
        return agreedRate;
    }

    public void setAgreedRate(BigDecimal agreedRate) {
        this.agreedRate = agreedRate;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getSourceOfFunds() {
        return sourceOfFunds;
    }

    public void setSourceOfFunds(String sourceOfFunds) {
        this.sourceOfFunds = sourceOfFunds;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getObjections() {
        return objections;
    }

    public void setObjections(String objections) {
        this.objections = objections;
    }

    public String getRmNote() {
        return rmNote;
    }

    public void setRmNote(String rmNote) {
        this.rmNote = rmNote;
    }

    public String getFieldSourcesJson() {
        return fieldSourcesJson;
    }

    public void setFieldSourcesJson(String fieldSourcesJson) {
        this.fieldSourcesJson = fieldSourcesJson;
    }

    public OffsetDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(OffsetDateTime savedAt) {
        this.savedAt = savedAt;
    }
}

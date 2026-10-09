package com.mb.callassistant.lead;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Tiềm năng bán (lead) do đúng một RM phụ trách ({@code ownerRmId}) — chỗ duy
 * nhất đặt bất biến quyền sở hữu (FR1.4, BR1.1). Mang máy trạng thái
 * {@link LeadStage} (SM1), mức ưu tiên, điểm xếp hạng, hạn SLA, nội dung tư vấn
 * và kịch bản bán.
 */
@Entity
@Table(name = "\"lead\"")
public class Lead {

    @Id
    @Column(name = "lead_id")
    private String leadId;

    @Column(name = "lead_code", nullable = false, unique = true)
    private String leadCode;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "owner_rm_id", nullable = false)
    private String ownerRmId;

    @Column(name = "product_interest")
    private String productInterest;

    @Column(name = "source")
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private LeadPriority priority = LeadPriority.THUONG;

    /** Điểm xếp hạng /10 (0..10). */
    @Column(name = "rank_score")
    private Integer rankScore;

    /** Hạn SLA, ISO-8601 có offset +07:00 (BR6.2). */
    @Column(name = "sla_due_at")
    private OffsetDateTime slaDueAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    private LeadStage stage = LeadStage.CHUA_TIEP_CAN;

    /** Khối Nội dung (thông tin TNB, sản phẩm, dư nợ) — JSON/văn bản tự do. */
    @Lob
    @Column(name = "content")
    private String content;

    @Lob
    @Column(name = "sales_script")
    private String salesScript;

    protected Lead() {
        // JPA
    }

    public Lead(String leadId, String leadCode, String customerId, String ownerRmId) {
        this.leadId = leadId;
        this.leadCode = leadCode;
        this.customerId = customerId;
        this.ownerRmId = ownerRmId;
    }

    public String getLeadId() {
        return leadId;
    }

    public String getLeadCode() {
        return leadCode;
    }

    public void setLeadCode(String leadCode) {
        this.leadCode = leadCode;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getOwnerRmId() {
        return ownerRmId;
    }

    public void setOwnerRmId(String ownerRmId) {
        this.ownerRmId = ownerRmId;
    }

    public String getProductInterest() {
        return productInterest;
    }

    public void setProductInterest(String productInterest) {
        this.productInterest = productInterest;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LeadPriority getPriority() {
        return priority;
    }

    public void setPriority(LeadPriority priority) {
        this.priority = priority;
    }

    public Integer getRankScore() {
        return rankScore;
    }

    public void setRankScore(Integer rankScore) {
        this.rankScore = rankScore;
    }

    public OffsetDateTime getSlaDueAt() {
        return slaDueAt;
    }

    public void setSlaDueAt(OffsetDateTime slaDueAt) {
        this.slaDueAt = slaDueAt;
    }

    public LeadStage getStage() {
        return stage;
    }

    public void setStage(LeadStage stage) {
        this.stage = stage;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSalesScript() {
        return salesScript;
    }

    public void setSalesScript(String salesScript) {
        this.salesScript = salesScript;
    }
}

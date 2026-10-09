package com.mb.callassistant.analysis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Kết quả phân tích AI của một cuộc gọi (U4, nguồn chân lý {@code entities.md}).
 * Mỗi cuộc gọi có tối đa một bản hiện hành ({@code callId} unique); chạy lại AI
 * thay thế bản cũ (upsert theo callId, BR5.2). Điểm tổng và hạng do server tự
 * tính từ {@code criteriaScores}, bỏ qua con số LLM trả (BR3.1/BR3.2, ADR-006).
 *
 * <p>Các value object dạng danh sách/bản đồ ({@code suggestions},
 * {@code criteriaScores}, {@code suggestedKeyin}, {@code suggestedTasks}) được
 * lưu dưới dạng JSON text (cột {@code *_json}); lớp DTO và service parse lại
 * bằng {@code ObjectMapper} dùng chung. Giữ JPA đơn giản, không nhúng
 * {@code ObjectMapper} vào thực thể.
 */
@Entity
@Table(name = "call_analysis")
public class CallAnalysis {

    @Id
    @Column(name = "analysis_id")
    private String analysisId;

    /** Tối đa một CallAnalysis hiện hành / cuộc gọi (BR5.2). FK-by-value tới CallRecord.id. */
    @Column(name = "call_id", nullable = false, unique = true)
    private String callId;

    @Lob
    @Column(name = "summary", nullable = false)
    private String summary;

    /** JSON: danh sách chuỗi gợi ý tiếp theo. */
    @Lob
    @Column(name = "suggestions_json")
    private String suggestionsJson;

    /** JSON: đúng 5 {@link CriterionScore}. */
    @Lob
    @Column(name = "criteria_scores_json", nullable = false)
    private String criteriaScoresJson;

    /** Server tính = round(tổng 5 tiêu chí × 2), ∈ [0,100] (BR3.1). */
    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false)
    private AnalysisGrade grade;

    @Enumerated(EnumType.STRING)
    @Column(name = "tone", nullable = false)
    private CallTone tone;

    @Lob
    @Column(name = "insight_strength")
    private String insightStrength;

    @Lob
    @Column(name = "insight_improvement")
    private String insightImprovement;

    /** JSON: bản đồ tên-trường keyin → {@link SuggestedValue} (trường vắng bị bỏ, BR4.1). */
    @Lob
    @Column(name = "suggested_keyin_json")
    private String suggestedKeyinJson;

    /** JSON: danh sách {@link SuggestedTask}. */
    @Lob
    @Column(name = "suggested_tasks_json")
    private String suggestedTasksJson;

    /** Nhãn model đã dùng (không chứa khóa API, BR8.2). */
    @Column(name = "model")
    private String model;

    /** ISO-8601 +07:00 (BR8.3). */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected CallAnalysis() {
        // JPA
    }

    public CallAnalysis(String analysisId, String callId, OffsetDateTime createdAt) {
        this.analysisId = analysisId;
        this.callId = callId;
        this.createdAt = createdAt;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSuggestionsJson() {
        return suggestionsJson;
    }

    public void setSuggestionsJson(String suggestionsJson) {
        this.suggestionsJson = suggestionsJson;
    }

    public String getCriteriaScoresJson() {
        return criteriaScoresJson;
    }

    public void setCriteriaScoresJson(String criteriaScoresJson) {
        this.criteriaScoresJson = criteriaScoresJson;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public AnalysisGrade getGrade() {
        return grade;
    }

    public void setGrade(AnalysisGrade grade) {
        this.grade = grade;
    }

    public CallTone getTone() {
        return tone;
    }

    public void setTone(CallTone tone) {
        this.tone = tone;
    }

    public String getInsightStrength() {
        return insightStrength;
    }

    public void setInsightStrength(String insightStrength) {
        this.insightStrength = insightStrength;
    }

    public String getInsightImprovement() {
        return insightImprovement;
    }

    public void setInsightImprovement(String insightImprovement) {
        this.insightImprovement = insightImprovement;
    }

    public String getSuggestedKeyinJson() {
        return suggestedKeyinJson;
    }

    public void setSuggestedKeyinJson(String suggestedKeyinJson) {
        this.suggestedKeyinJson = suggestedKeyinJson;
    }

    public String getSuggestedTasksJson() {
        return suggestedTasksJson;
    }

    public void setSuggestedTasksJson(String suggestedTasksJson) {
        this.suggestedTasksJson = suggestedTasksJson;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

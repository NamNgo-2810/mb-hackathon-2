package com.mb.callassistant.analysis;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Đánh giá chất lượng AI do RM nhập khi lưu hồ sơ cuộc gọi (U4, entities.md):
 * điểm 1–10, các thẻ nhận xét nhanh và góp ý tự do (≤1000 ký tự). Mỗi cuộc gọi
 * tối đa một đánh giá ({@code callId} unique); lưu cùng giao dịch keyin
 * (BR5.3), lưu lặp cập nhật tại chỗ.
 */
@Entity
@Table(name = "ai_feedback")
public class AiFeedback {

    @Id
    @Column(name = "feedback_id")
    private String feedbackId;

    @Column(name = "call_id", nullable = false, unique = true)
    private String callId;

    /** Thang 1–10 (BR8.4). */
    @Column(name = "rating", nullable = false)
    private int rating;

    @ElementCollection
    @CollectionTable(name = "ai_feedback_tag", joinColumns = @JoinColumn(name = "feedback_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Lob
    @Column(name = "comment")
    private String comment;

    protected AiFeedback() {
        // JPA
    }

    public AiFeedback(String feedbackId, String callId, int rating) {
        this.feedbackId = feedbackId;
        this.callId = callId;
        this.rating = rating;
    }

    public String getFeedbackId() {
        return feedbackId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

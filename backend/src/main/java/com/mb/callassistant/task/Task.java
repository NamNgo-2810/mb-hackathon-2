package com.mb.callassistant.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Công việc / tác vụ CRM của RM hiển thị trên lịch làm việc (S5) và thao tác qua
 * hộp thoại tạo/đổi giờ (S6). Do đúng một RM phụ trách ({@code ownerRmId}, BR1.1),
 * có thể gắn một lead ({@code leadId}, BR1.3), có thể sinh tự động từ một cuộc
 * gọi ({@code origin = AI_FROM_CALL}, {@code sourceCallId}, BR2.1/BR2.3), tạo
 * thủ công ({@code MANUAL}) hoặc nạp sẵn ({@code SEED}).
 *
 * <p>Bất biến (xem {@code entities.md} entity_constraints):
 * <ul>
 *   <li>{@code completedAt != null} ⇔ {@code status == DONE} (BR5.1).</li>
 *   <li>{@code type == KHAC} chỉ khi {@code leadId == null} (BR3.2).</li>
 * </ul>
 * Mọi thời gian mang offset {@code +07:00}, ranh giới ngày theo
 * {@code Asia/Ho_Chi_Minh} (BR6.1).
 */
@Entity
@Table(name = "task")
public class Task {

    @Id
    @Column(name = "task_id")
    private String taskId;

    @Column(name = "owner_rm_id", nullable = false)
    private String ownerRmId;

    @Column(name = "lead_id")
    private String leadId;

    @Column(name = "source_call_id")
    private String sourceCallId;

    /** Khoá chống trùng do bên tạo cung cấp; bắt buộc với createFromCall (BR2.1/BR2.2). */
    @Column(name = "client_key")
    private String clientKey;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TaskType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private TaskPriority priority = TaskPriority.THUONG;

    /** Ngày giờ đến hạn, ISO-8601 có offset +07:00 (BR6.1). */
    @Column(name = "due_at", nullable = false)
    private OffsetDateTime dueAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false)
    private TaskOrigin origin;

    /** Bản sao tóm tắt AI của cuộc gọi gốc để thẻ panel trái S5 hiển thị độc lập (BR2.3). */
    @Lob
    @Column(name = "ai_summary")
    private String aiSummary;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected Task() {
        // JPA
    }

    public Task(String taskId, String ownerRmId, String title, TaskType type,
                OffsetDateTime dueAt, TaskOrigin origin, OffsetDateTime createdAt) {
        this.taskId = taskId;
        this.ownerRmId = ownerRmId;
        this.title = title;
        this.type = type;
        this.dueAt = dueAt;
        this.origin = origin;
        this.createdAt = createdAt;
    }

    /**
     * Đánh dấu hoàn thành (BR5.1): TODO → DONE, đặt {@code completedAt}. Idempotent
     * — gọi lại khi đã DONE giữ nguyên {@code completedAt}.
     *
     * @param now thời điểm hiện tại (có offset +07:00)
     */
    public void markDone(OffsetDateTime now) {
        if (this.status == TaskStatus.DONE) {
            return; // no-op idempotent
        }
        this.status = TaskStatus.DONE;
        this.completedAt = now;
    }

    /**
     * Đổi giờ đến hạn (BR5.2). Giữ nguyên {@code status} (và {@code completedAt}),
     * vì đổi giờ không phải là chuyển trạng thái.
     */
    public void reschedule(OffsetDateTime newDueAt) {
        this.dueAt = newDueAt;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getOwnerRmId() {
        return ownerRmId;
    }

    public void setOwnerRmId(String ownerRmId) {
        this.ownerRmId = ownerRmId;
    }

    public String getLeadId() {
        return leadId;
    }

    public void setLeadId(String leadId) {
        this.leadId = leadId;
    }

    public String getSourceCallId() {
        return sourceCallId;
    }

    public void setSourceCallId(String sourceCallId) {
        this.sourceCallId = sourceCallId;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public OffsetDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(OffsetDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(TaskOrigin origin) {
        this.origin = origin;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}

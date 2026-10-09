package com.mb.callassistant.web.dto;

import com.mb.callassistant.task.Task;

import java.time.OffsetDateTime;

/**
 * DTO công việc (C4 Task). Gồm các trường read-only denormalised
 * ({@code leadCode}, {@code customerName}, {@code aiSummary}) để thẻ ở panel
 * trái S5 và lưới lịch hiển thị độc lập, không gọi chéo CallAnalysis (BR2.3).
 * {@code leadCode}/{@code customerName} do controller điền qua
 * {@code LeadService} khi {@code leadId != null}; null khi không gắn lead.
 */
public record TaskDto(
        String taskId,
        String ownerRmId,
        String leadId,
        String sourceCallId,
        String title,
        String type,
        String priority,
        OffsetDateTime dueAt,
        Integer durationMinutes,
        String status,
        String origin,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt,
        String leadCode,
        String customerName,
        String aiSummary
) {

    public static TaskDto from(Task t, String leadCode, String customerName) {
        return new TaskDto(
                t.getTaskId(),
                t.getOwnerRmId(),
                t.getLeadId(),
                t.getSourceCallId(),
                t.getTitle(),
                t.getType() == null ? null : t.getType().name(),
                t.getPriority() == null ? null : t.getPriority().name(),
                t.getDueAt(),
                t.getDurationMinutes(),
                t.getStatus() == null ? null : t.getStatus().name(),
                t.getOrigin() == null ? null : t.getOrigin().name(),
                t.getNote(),
                t.getCreatedAt(),
                t.getCompletedAt(),
                leadCode,
                customerName,
                t.getAiSummary()
        );
    }

    /** Không có thông tin lead denormalised (công việc không gắn lead). */
    public static TaskDto from(Task t) {
        return from(t, null, null);
    }
}

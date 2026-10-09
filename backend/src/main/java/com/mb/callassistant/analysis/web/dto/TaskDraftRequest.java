package com.mb.callassistant.analysis.web.dto;

import com.mb.callassistant.task.TaskDraft;
import com.mb.callassistant.task.TaskPriority;
import com.mb.callassistant.task.TaskType;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

/**
 * Một "công việc tiếp theo" trong body PUT /keyin. {@code clientKey} bắt buộc
 * (I4/BR5.2); {@code dueAt} ISO-8601 +07:00 (BR8.3). Ánh xạ sang
 * {@link TaskDraft}.
 */
public record TaskDraftRequest(
        @NotBlank(message = "clientKey là bắt buộc") String clientKey,
        @NotBlank(message = "title là bắt buộc") String title,
        OffsetDateTime dueAt,
        TaskType type,
        TaskPriority priority,
        Integer durationMinutes,
        boolean aiSuggested
) {

    public TaskDraft toDraft() {
        return new TaskDraft(clientKey, title, dueAt, type, priority, durationMinutes, aiSuggested);
    }
}

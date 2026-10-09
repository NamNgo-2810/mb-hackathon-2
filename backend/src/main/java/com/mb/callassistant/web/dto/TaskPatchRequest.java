package com.mb.callassistant.web.dto;

import com.mb.callassistant.task.TaskStatus;

import java.time.OffsetDateTime;

/**
 * Body cho {@code PATCH /api/tasks/{taskId}} (C4): đánh dấu xong và/hoặc đổi
 * giờ. {@code status} chỉ nhận {@code DONE} (một chiều, BR5.1); {@code dueAt} ở
 * quá khứ cần {@code confirmPast = true} (BR5.2, AC3.3.3). Mọi trường không bắt
 * buộc để một PATCH có thể chỉ đổi giờ hoặc chỉ đánh dấu xong.
 */
public record TaskPatchRequest(
        TaskStatus status,
        OffsetDateTime dueAt,
        Boolean confirmPast
) {

    public boolean confirmPastFlag() {
        return Boolean.TRUE.equals(confirmPast);
    }
}

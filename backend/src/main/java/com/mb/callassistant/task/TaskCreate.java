package com.mb.callassistant.task;

import java.time.OffsetDateTime;

/**
 * Giá trị tạo công việc thủ công (C4 {@code POST /api/tasks}, S6). Value-object
 * đầu vào cho {@link TaskService#createManual}. {@code leadId} không bắt buộc;
 * khi khác null, lead phải thuộc RM hiện tại (BR1.3) và {@code type} không được
 * là {@code KHAC} (BR3.2).
 */
public record TaskCreate(
        String title,
        TaskType type,
        OffsetDateTime dueAt,
        Integer durationMinutes,
        TaskPriority priority,
        String leadId,
        String note
) {
}

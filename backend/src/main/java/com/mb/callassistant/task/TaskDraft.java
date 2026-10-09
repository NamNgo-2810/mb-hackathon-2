package com.mb.callassistant.task;

import java.time.OffsetDateTime;

/**
 * Bản nháp một "công việc tiếp theo" do U4 gửi qua
 * {@link TaskService#createFromCall} (I4). Value-object đầu vào, không lưu trữ
 * riêng — mỗi draft vật chất hoá thành tối đa một {@link Task}.
 *
 * <p>{@code clientKey} bắt buộc (quyết định của stage này, BR2.2): thiếu thì cả
 * lời gọi bị từ chối, nhờ đó idempotent theo {@code (sourceCallId, clientKey)}
 * luôn xác định (BR2.1). {@code type} chỉ nhận
 * {@code LICH_HEN | NHAC_GOI | TASK_ZALO} (BR3.2/BR6.2).
 */
public record TaskDraft(
        String clientKey,
        String title,
        OffsetDateTime dueAt,
        TaskType type,
        TaskPriority priority,
        Integer durationMinutes,
        boolean aiSuggested
) {
}

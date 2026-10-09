package com.mb.callassistant.task;

/**
 * Loại công việc (U5). {@code KHAC} chỉ dùng cho công việc seed không gắn lead
 * và chỉ hiện ở bộ lọc "Tất cả" (BR3.2, BR4.3). Tạo thủ công hoặc từ cuộc gọi
 * dùng {@code LICH_HEN | NHAC_GOI | TASK_ZALO} (BR6.2).
 */
public enum TaskType {
    LICH_HEN,
    NHAC_GOI,
    TASK_ZALO,
    KHAC
}

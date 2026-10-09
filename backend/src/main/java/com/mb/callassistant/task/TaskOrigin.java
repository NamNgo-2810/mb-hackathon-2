package com.mb.callassistant.task;

/**
 * Nguồn gốc công việc (U5). {@code AI_FROM_CALL} sinh từ keyin cuộc gọi (I4);
 * {@code MANUAL} do RM tạo tay (S6); {@code SEED} là dữ liệu mẫu (thường không
 * gắn lead).
 */
public enum TaskOrigin {
    AI_FROM_CALL,
    MANUAL,
    SEED
}

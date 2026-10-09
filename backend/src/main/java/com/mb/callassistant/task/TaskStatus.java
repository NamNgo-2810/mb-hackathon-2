package com.mb.callassistant.task;

/**
 * Trạng thái vòng đời công việc (U5, SM ở functional-spec.md). Một chiều cho
 * demo: {@code TODO -> DONE} (BR5.1). Đổi giờ không đổi trạng thái.
 */
public enum TaskStatus {
    TODO,
    DONE
}

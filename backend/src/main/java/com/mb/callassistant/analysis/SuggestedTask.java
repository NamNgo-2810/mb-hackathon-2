package com.mb.callassistant.analysis;

/**
 * Công việc tiếp theo do AI đề xuất (value object trong {@link CallAnalysis}).
 * Khác với {@link com.mb.callassistant.task.TaskDraft} (đầu vào I4, bắt buộc
 * {@code dueAt}/{@code type}): đây là gợi ý hiển thị để RM chỉnh ở S3, các
 * trường có thể còn trống. Khi RM lưu, giao diện gửi draft hoàn chỉnh (kèm
 * {@code clientKey}) qua PUT /keyin (BR5.2).
 */
public record SuggestedTask(
        String title,
        String type,
        String priority,
        boolean aiSuggested
) {
}

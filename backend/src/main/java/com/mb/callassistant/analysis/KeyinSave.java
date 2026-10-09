package com.mb.callassistant.analysis;

import com.mb.callassistant.lead.LeadStage;
import com.mb.callassistant.task.TaskDraft;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Đầu vào lưu hồ sơ cuộc gọi (lớp nghiệp vụ, U4). DTO web ({@code KeyinSaveDto})
 * ánh xạ sang record này. Hai trường bắt buộc: {@code leadStage},
 * {@code callResult} (BR5.1). {@code nextTasks} là các {@link TaskDraft} hoàn
 * chỉnh (mỗi draft bắt buộc {@code clientKey}) cho {@code createFromCall} (I4).
 * {@code feedbackRating} null nghĩa là RM không đánh giá AI.
 */
public record KeyinSave(
        LeadStage leadStage,
        String callResult,
        Long demandAmount,
        String term,
        BigDecimal agreedRate,
        String product,
        String sourceOfFunds,
        LocalDate expectedDate,
        String channel,
        String objections,
        String rmNote,
        Map<String, FieldSource> fieldSources,
        List<TaskDraft> nextTasks,
        Integer feedbackRating,
        List<String> feedbackTags,
        String feedbackComment
) {
}

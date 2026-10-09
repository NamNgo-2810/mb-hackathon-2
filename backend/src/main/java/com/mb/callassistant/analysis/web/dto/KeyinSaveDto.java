package com.mb.callassistant.analysis.web.dto;

import com.mb.callassistant.analysis.FieldSource;
import com.mb.callassistant.analysis.KeyinSave;
import com.mb.callassistant.lead.LeadStage;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Body PUT /keyin (C3). Hai trường bắt buộc {@code leadStage}+{@code callResult}
 * (BR5.1); {@code fieldSources} ghi nguồn từng trường (BR4.3); {@code nextTasks}
 * là công việc tiếp theo (I4). Thiếu bắt buộc → 400 {@code VALIDATION_FAILED}
 * (details.field) qua {@code ApiExceptionHandler}.
 */
public record KeyinSaveDto(
        @NotNull(message = "leadStage là bắt buộc") LeadStage leadStage,
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
        List<TaskDraftRequest> nextTasks,
        Integer feedbackRating,
        List<String> feedbackTags,
        String feedbackComment
) {

    public KeyinSave toSave() {
        Map<String, FieldSource> sources = fieldSources == null ? new LinkedHashMap<>() : fieldSources;
        List<com.mb.callassistant.task.TaskDraft> drafts = new ArrayList<>();
        if (nextTasks != null) {
            for (TaskDraftRequest t : nextTasks) {
                drafts.add(t.toDraft());
            }
        }
        return new KeyinSave(leadStage, callResult, demandAmount, term, agreedRate, product,
                sourceOfFunds, expectedDate, channel, objections, rmNote, sources, drafts,
                feedbackRating, feedbackTags, feedbackComment);
    }
}

package com.mb.callassistant.analysis.web.dto;

/** Công việc đề xuất trong DTO C3 (serialize từ {@code SuggestedTask}). */
public record TaskDraftDto(
        String title,
        String type,
        String priority,
        boolean aiSuggested
) {
}

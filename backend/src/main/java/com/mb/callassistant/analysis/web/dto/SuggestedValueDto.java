package com.mb.callassistant.analysis.web.dto;

/** Giá trị keyin đề xuất trong DTO C3 (serialize từ {@code SuggestedValue}). */
public record SuggestedValueDto(
        String value,
        Integer sourceMs
) {
}

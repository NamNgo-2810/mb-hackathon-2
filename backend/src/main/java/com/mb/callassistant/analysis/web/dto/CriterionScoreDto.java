package com.mb.callassistant.analysis.web.dto;

import java.math.BigDecimal;

/** Một tiêu chí chấm điểm trong DTO C3 (serialize từ {@code CriterionScore}). */
public record CriterionScoreDto(
        String code,
        String name,
        BigDecimal score,
        String evidence
) {
}

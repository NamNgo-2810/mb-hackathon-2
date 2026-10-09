package com.mb.callassistant.analysis;

import java.math.BigDecimal;

/**
 * Điểm một tiêu chí chấm cuộc gọi, kèm bằng chứng do LLM đưa (value object,
 * entities.md). Điểm là decimal bước 0,5 trong [0,10] (review R-04). Value
 * object thuần — không phải thực thể JPA; được serialize vào trường JSON của
 * {@link CallAnalysis}.
 */
public record CriterionScore(
        CriterionCode code,
        String name,
        BigDecimal score,
        String evidence
) {
}

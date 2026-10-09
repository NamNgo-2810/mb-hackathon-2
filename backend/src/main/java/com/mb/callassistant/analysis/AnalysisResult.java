package com.mb.callassistant.analysis;

import java.util.List;
import java.util.Map;

/**
 * Kết quả phân tích AI đã parse và kiểm hợp lệ, trước khi lưu thành
 * {@link CallAnalysis} (value object nội bộ của U4). {@code totalScore} và
 * {@code grade} do server tính từ {@code criteriaScores}, KHÔNG lấy từ LLM
 * (BR3.1/BR3.2). {@code suggestedKeyin} là bản đồ tên-trường → giá trị đề xuất;
 * trường không có trong transcript bị bỏ (BR4.1).
 */
public record AnalysisResult(
        String summary,
        List<String> suggestions,
        List<CriterionScore> criteriaScores,
        int totalScore,
        AnalysisGrade grade,
        CallTone tone,
        String insightStrength,
        String insightImprovement,
        Map<String, SuggestedValue> suggestedKeyin,
        List<SuggestedTask> suggestedTasks,
        String model
) {
}

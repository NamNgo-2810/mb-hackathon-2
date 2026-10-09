package com.mb.callassistant.analysis.web.dto;

import com.mb.callassistant.analysis.AnalysisJson;
import com.mb.callassistant.analysis.CallAnalysis;
import com.mb.callassistant.analysis.CriterionScore;
import com.mb.callassistant.analysis.SuggestedTask;
import com.mb.callassistant.analysis.SuggestedValue;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO kết quả phân tích (C3, {@code GET /analysis}). Lưu ý ánh xạ tên (review
 * R-01): thực thể giữ {@code criteriaScores}, hợp đồng C3 serialize thành
 * {@code criteria}. Điểm tổng/hạng do server tính (BR3.1/BR3.3).
 */
public record CallAnalysisDto(
        String callId,
        String summary,
        List<String> suggestions,
        List<CriterionScoreDto> criteria,
        int totalScore,
        String grade,
        String tone,
        String insightStrength,
        String insightImprovement,
        Map<String, SuggestedValueDto> suggestedKeyin,
        List<TaskDraftDto> suggestedTasks,
        String model,
        OffsetDateTime createdAt
) {

    public static CallAnalysisDto from(CallAnalysis a, AnalysisJson json) {
        List<CriterionScoreDto> criteria = new ArrayList<>();
        for (CriterionScore cs : json.readCriteria(a.getCriteriaScoresJson())) {
            criteria.add(new CriterionScoreDto(
                    cs.code() == null ? null : cs.code().name(),
                    cs.name(), cs.score(), cs.evidence()));
        }

        Map<String, SuggestedValueDto> keyin = new LinkedHashMap<>();
        for (Map.Entry<String, SuggestedValue> e : json.readSuggestedKeyin(a.getSuggestedKeyinJson()).entrySet()) {
            SuggestedValue v = e.getValue();
            keyin.put(e.getKey(), new SuggestedValueDto(v.value(), v.sourceMs()));
        }

        List<TaskDraftDto> tasks = new ArrayList<>();
        for (SuggestedTask t : json.readSuggestedTasks(a.getSuggestedTasksJson())) {
            tasks.add(new TaskDraftDto(t.title(), t.type(), t.priority(), t.aiSuggested()));
        }

        return new CallAnalysisDto(
                a.getCallId(),
                a.getSummary(),
                json.readStringList(a.getSuggestionsJson()),
                criteria,
                a.getTotalScore(),
                a.getGrade() == null ? null : a.getGrade().name(),
                a.getTone() == null ? null : a.getTone().name(),
                a.getInsightStrength(),
                a.getInsightImprovement(),
                keyin,
                tasks,
                a.getModel(),
                a.getCreatedAt()
        );
    }
}

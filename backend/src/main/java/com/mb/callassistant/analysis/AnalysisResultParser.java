package com.mb.callassistant.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parse JSON kết quả AI (expectJson đã đảm bảo là object, có thể bọc hàng rào
 * ```json) thành {@link AnalysisResult}, rồi dùng {@link CallScoringService} để
 * tự tính {@code totalScore}/{@code grade} (BR3.1/BR3.2). Trường vắng trong
 * {@code suggestedKeyin} bị bỏ (không bịa, BR4.1). JSON sai cấu trúc / thiếu
 * trường bắt buộc → {@link AnalysisInvalidException} (BR2.1).
 */
@Component
public class AnalysisResultParser {

    private final ObjectMapper objectMapper;
    private final CallScoringService scoringService;

    public AnalysisResultParser(ObjectMapper objectMapper, CallScoringService scoringService) {
        this.objectMapper = objectMapper;
        this.scoringService = scoringService;
    }

    /**
     * @param rawText nội dung text do LlmClient trả (đã qua expectJson)
     * @param model   nhãn model đã dùng
     * @return kết quả đã kiểm hợp lệ và chấm điểm
     * @throws AnalysisInvalidException khi JSON sai cấu trúc / điểm sai (BR2.1/BR2.2)
     */
    public AnalysisResult parse(String rawText, String model) {
        JsonNode root = readJson(rawText);
        if (root == null || !root.isObject()) {
            throw new AnalysisInvalidException("Kết quả AI không phải JSON object");
        }

        String summary = textOrNull(root, "summary");
        if (summary == null || summary.isBlank()) {
            throw new AnalysisInvalidException("Thiếu trường summary trong kết quả AI");
        }

        List<CriterionScore> criteria = parseCriteria(root.path("criteriaScores"));
        // Kiểm bất biến ngay — sai thì coi là AI thất bại (BR2.2), không bịa.
        scoringService.validateCriteria(criteria);
        int totalScore = scoringService.totalScore(criteria);
        AnalysisGrade grade = scoringService.grade(totalScore);

        List<String> suggestions = parseStringArray(root.path("suggestions"));
        CallTone tone = parseTone(root.path("tone"));
        String insightStrength = textOrNull(root, "insightStrength");
        String insightImprovement = textOrNull(root, "insightImprovement");
        Map<String, SuggestedValue> suggestedKeyin = parseSuggestedKeyin(root.path("suggestedKeyin"));
        List<SuggestedTask> suggestedTasks = parseSuggestedTasks(root.path("suggestedTasks"));

        return new AnalysisResult(summary, suggestions, criteria, totalScore, grade, tone,
                insightStrength, insightImprovement, suggestedKeyin, suggestedTasks, model);
    }

    private List<CriterionScore> parseCriteria(JsonNode node) {
        List<CriterionScore> out = new ArrayList<>();
        if (node == null || !node.isArray()) {
            // Để scoringService.validateCriteria ném lỗi "đúng 5 phần tử".
            return out;
        }
        for (JsonNode item : node) {
            CriterionCode code = parseCriterionCode(item.path("code"));
            String name = textNode(item.path("name"));
            BigDecimal score = numberOrNull(item.path("score"));
            String evidence = textNode(item.path("evidence"));
            out.add(new CriterionScore(code, name, score, evidence));
        }
        return out;
    }

    private CriterionCode parseCriterionCode(JsonNode node) {
        if (node == null || !node.isTextual()) {
            return null;
        }
        try {
            return CriterionCode.valueOf(node.asText().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private CallTone parseTone(JsonNode node) {
        if (node == null || !node.isTextual()) {
            throw new AnalysisInvalidException("Thiếu trường tone trong kết quả AI");
        }
        try {
            return CallTone.valueOf(node.asText().trim());
        } catch (IllegalArgumentException e) {
            throw new AnalysisInvalidException("Giá trị tone không hợp lệ");
        }
    }

    private Map<String, SuggestedValue> parseSuggestedKeyin(JsonNode node) {
        Map<String, SuggestedValue> out = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return out;
        }
        node.fields().forEachRemaining(entry -> {
            JsonNode v = entry.getValue();
            if (v == null || v.isNull()) {
                return; // trường vắng → bỏ (BR4.1)
            }
            String value;
            Integer sourceMs = null;
            if (v.isObject()) {
                JsonNode valueNode = v.path("value");
                if (valueNode.isMissingNode() || valueNode.isNull()) {
                    return; // không có value → bỏ, không bịa
                }
                value = valueNode.asText();
                if (v.path("sourceMs").isIntegralNumber()) {
                    sourceMs = v.path("sourceMs").asInt();
                }
            } else {
                // Dạng rút gọn: giá trị trực tiếp, không có sourceMs.
                value = v.asText();
            }
            if (value == null || value.isBlank()) {
                return;
            }
            out.put(entry.getKey(), new SuggestedValue(value, sourceMs));
        });
        return out;
    }

    private List<SuggestedTask> parseSuggestedTasks(JsonNode node) {
        List<SuggestedTask> out = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return out;
        }
        for (JsonNode item : node) {
            String title = textNode(item.path("title"));
            if (title == null || title.isBlank()) {
                continue;
            }
            String type = textNode(item.path("type"));
            String priority = textNode(item.path("priority"));
            boolean aiSuggested = item.path("aiSuggested").asBoolean(true);
            out.add(new SuggestedTask(title, type, priority, aiSuggested));
        }
        return out;
    }

    private List<String> parseStringArray(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return out;
        }
        for (JsonNode item : node) {
            if (item.isTextual()) {
                out.add(item.asText());
            }
        }
        return out;
    }

    private String textOrNull(JsonNode root, String field) {
        return textNode(root.path(field));
    }

    private String textNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isValueNode()) {
            return null;
        }
        return node.asText();
    }

    private BigDecimal numberOrNull(JsonNode node) {
        if (node == null || !node.isNumber()) {
            // Chuỗi số cũng chấp nhận để tránh lỗi parse giả.
            if (node != null && node.isTextual()) {
                try {
                    return new BigDecimal(node.asText().trim().replace(",", "."));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }
        return node.decimalValue();
    }

    private JsonNode readJson(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String candidate = stripFence(text.strip());
        try {
            return objectMapper.readTree(candidate);
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            return null;
        }
    }

    /** Gỡ hàng rào ```json ... ``` quanh JSON nếu có (giống JsonResponses). */
    private String stripFence(String text) {
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNewline = text.indexOf('\n');
        if (firstNewline < 0) {
            return text;
        }
        String firstLine = text.substring(3, firstNewline).trim();
        if (!(firstLine.isEmpty() || firstLine.equalsIgnoreCase("json"))) {
            return text;
        }
        String rest = text.substring(firstNewline + 1);
        int closing = rest.lastIndexOf("```");
        if (closing >= 0) {
            rest = rest.substring(0, closing);
        }
        return rest.strip();
    }
}

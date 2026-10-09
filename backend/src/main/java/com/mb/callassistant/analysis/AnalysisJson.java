package com.mb.callassistant.analysis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * (De)serialize các value object dạng danh sách/bản đồ của {@link CallAnalysis}
 * vào/ra các cột JSON text, dùng {@code ObjectMapper} dùng chung. Giữ thực thể
 * JPA đơn giản (không nhúng mapper vào entity). Mọi lỗi parse trả về giá trị
 * rỗng an toàn (không ném) để đọc một bản ghi cũ không làm hỏng API.
 */
@Component
public class AnalysisJson {

    private final ObjectMapper objectMapper;

    public AnalysisJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Không serialize được dữ liệu phân tích", e);
        }
    }

    public List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<String> out = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            return out == null ? new ArrayList<>() : out;
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public List<CriterionScore> readCriteria(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<CriterionScore> out = objectMapper.readValue(json,
                    new TypeReference<List<CriterionScore>>() {
                    });
            return out == null ? new ArrayList<>() : out;
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public Map<String, SuggestedValue> readSuggestedKeyin(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, SuggestedValue> out = objectMapper.readValue(json,
                    new TypeReference<LinkedHashMap<String, SuggestedValue>>() {
                    });
            return out == null ? new LinkedHashMap<>() : out;
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }

    public List<SuggestedTask> readSuggestedTasks(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<SuggestedTask> out = objectMapper.readValue(json,
                    new TypeReference<List<SuggestedTask>>() {
                    });
            return out == null ? new ArrayList<>() : out;
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public Map<String, FieldSource> readFieldSources(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, FieldSource> out = objectMapper.readValue(json,
                    new TypeReference<LinkedHashMap<String, FieldSource>>() {
                    });
            return out == null ? new LinkedHashMap<>() : out;
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }
}

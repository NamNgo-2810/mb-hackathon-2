package com.mb.callassistant.analysis;

/**
 * Một giá trị keyin do AI đề xuất, kèm mốc trích xuất trong ghi âm (value
 * object, entities.md). {@code sourceMs} là nguồn của huy hiệu "AI · mm:ss"
 * (BR4.1); có thể null khi LLM không nêu được mốc. Trường không có trong
 * transcript bị bỏ hẳn khỏi {@code suggestedKeyin} (không bịa, BR4.1/AC2.3.2).
 */
public record SuggestedValue(
        String value,
        Integer sourceMs
) {
}

package com.mb.callassistant.copilot;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chấm điểm và chọn đoạn cho một câu hỏi (BR2.2). Điểm = số từ khoá của câu hỏi
 * xuất hiện trong {@code normalizedText} của đoạn (có trọng số theo tần suất).
 * Chọn các đoạn {@code score > 0}, sắp giảm dần, lấy tối đa {@code topK}.
 */
@Component
public class ChunkSearcher {

    /**
     * @param chunks        toàn bộ đoạn trong chỉ mục
     * @param questionTokens từ khoá đã chuẩn hoá của câu hỏi (BR2.1)
     * @param topK          số đoạn tối đa trả về
     * @return danh sách {@link RetrievedChunk} điểm > 0, giảm dần, ≤ topK
     */
    public List<RetrievedChunk> search(List<DocumentChunk> chunks, List<String> questionTokens, int topK) {
        if (chunks == null || chunks.isEmpty() || questionTokens == null || questionTokens.isEmpty()) {
            return List.of();
        }
        List<RetrievedChunk> scored = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            double score = score(chunk.normalizedText(), questionTokens);
            if (score > 0) {
                scored.add(RetrievedChunk.from(chunk, score));
            }
        }
        scored.sort(Comparator.comparingDouble(RetrievedChunk::score).reversed()
                .thenComparing(RetrievedChunk::documentId)
                .thenComparingInt(RetrievedChunk::chunkIndex));
        int limit = Math.max(0, topK);
        if (scored.size() > limit) {
            return new ArrayList<>(scored.subList(0, limit));
        }
        return scored;
    }

    /** Điểm = tổng số lần xuất hiện của mỗi từ khoá riêng biệt trong đoạn. */
    private double score(String normalizedText, List<String> questionTokens) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return 0;
        }
        double total = 0;
        for (String token : questionTokens) {
            total += countOccurrences(normalizedText, token);
        }
        return total;
    }

    private int countOccurrences(String haystack, String needle) {
        if (needle.isEmpty()) {
            return 0;
        }
        int count = 0;
        int from = 0;
        while (true) {
            int idx = haystack.indexOf(needle, from);
            if (idx < 0) {
                break;
            }
            count++;
            from = idx + needle.length();
        }
        return count;
    }
}

package com.mb.callassistant.copilot;

/**
 * Một {@link DocumentChunk} được chọn cho một câu hỏi cụ thể, kèm điểm khớp từ
 * khoá (entities.md {@code RetrievedChunk}, BR2.2). Là đầu vào để dựng khối DATA
 * trong prompt và để kiểm chứng trích dẫn của LLM (BR4.2).
 *
 * <p>Ràng buộc: {@code score > 0} — đoạn có điểm 0 không được chọn (BR2.2).
 */
public record RetrievedChunk(
        String documentId,
        int chunkIndex,
        double score,
        Integer page,
        String section) {

    public RetrievedChunk {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId là bắt buộc");
        }
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("chunkIndex không âm");
        }
        if (score <= 0) {
            throw new IllegalArgumentException("score phải > 0 (BR2.2)");
        }
    }

    /** Dựng từ một {@link DocumentChunk} và điểm đã tính. */
    public static RetrievedChunk from(DocumentChunk chunk, double score) {
        return new RetrievedChunk(chunk.documentId(), chunk.chunkIndex(), score,
                chunk.page(), chunk.section());
    }
}

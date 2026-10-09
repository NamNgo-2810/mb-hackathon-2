package com.mb.callassistant.copilot;

/**
 * Một đoạn văn bản của tài liệu, cắt theo trang hoặc mục (entities.md
 * {@code DocumentChunk}, BR1.3). Là đơn vị để tìm từ khoá và trích dẫn trang/mục;
 * chỉ tồn tại trong chỉ mục trong bộ nhớ.
 *
 * <p>Ràng buộc (BR1.3): mỗi đoạn phải có ít nhất một trong {@code page} hoặc
 * {@code section} để trích dẫn được nguồn. {@code normalizedText} là bản đã bỏ
 * dấu/hạ thường để chấm điểm từ khoá (BR2.1).
 */
public record DocumentChunk(
        String documentId,
        int chunkIndex,
        Integer page,
        String section,
        String text,
        String normalizedText) {

    public DocumentChunk {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId là bắt buộc");
        }
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("chunkIndex không âm");
        }
        boolean hasPage = page != null;
        boolean hasSection = section != null && !section.isBlank();
        if (!hasPage && !hasSection) {
            throw new IllegalArgumentException(
                    "DocumentChunk phải có ít nhất một trong page hoặc section (BR1.3)");
        }
        if (text == null) {
            throw new IllegalArgumentException("text là bắt buộc");
        }
        if (normalizedText == null) {
            throw new IllegalArgumentException("normalizedText là bắt buộc");
        }
    }
}

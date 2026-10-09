package com.mb.callassistant.copilot;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Một tài liệu chính sách đã trích ra file text/md và nạp vào chỉ mục khi khởi
 * động (entities.md {@code PolicyDocument}, BR1.1/BR1.3). Là nguồn kiến thức duy
 * nhất Copilot được phép trả lời.
 *
 * <p>Lưu trong bộ nhớ (không phải thực thể JPA): tài liệu nạp một lần lúc khởi
 * động, chỉ mục giữ trong {@link PolicyDocumentRepository}. {@code title} và
 * {@code effectiveDate} bắt buộc — tài liệu thiếu chúng bị bỏ qua khi nạp (BR1.1).
 */
public class PolicyDocument {

    private final String documentId;
    private final String title;
    private final LocalDate effectiveDate;
    private final String sourcePdfPath;
    private final String extractedTextPath;
    private final Integer pageCount;
    private final OffsetDateTime loadedAt;

    public PolicyDocument(String documentId, String title, LocalDate effectiveDate,
                          String sourcePdfPath, String extractedTextPath,
                          Integer pageCount, OffsetDateTime loadedAt) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId là bắt buộc");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title là bắt buộc (BR1.1)");
        }
        if (effectiveDate == null) {
            throw new IllegalArgumentException("effectiveDate là bắt buộc (BR1.1)");
        }
        this.documentId = documentId;
        this.title = title;
        this.effectiveDate = effectiveDate;
        this.sourcePdfPath = sourcePdfPath;
        this.extractedTextPath = extractedTextPath;
        this.pageCount = pageCount;
        this.loadedAt = loadedAt;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public String getSourcePdfPath() {
        return sourcePdfPath;
    }

    public String getExtractedTextPath() {
        return extractedTextPath;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public OffsetDateTime getLoadedAt() {
        return loadedAt;
    }
}

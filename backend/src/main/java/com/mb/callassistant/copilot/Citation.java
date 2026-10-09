package com.mb.callassistant.copilot;

import java.time.LocalDate;

/**
 * Trích dẫn nguồn kèm ngày hiệu lực, hiển thị cạnh câu trả lời (entities.md
 * {@code Citation}, BR4.1). {@code documentId} có thể vắng, nhưng
 * {@code documentTitle} và {@code effectiveDate} luôn có, và phải có ít nhất một
 * trong {@code page} hoặc {@code section} (FR3.2).
 */
public record Citation(
        String documentId,
        String documentTitle,
        Integer page,
        String section,
        LocalDate effectiveDate) {

    public Citation {
        if (documentTitle == null || documentTitle.isBlank()) {
            throw new IllegalArgumentException("documentTitle là bắt buộc (BR4.1)");
        }
        if (effectiveDate == null) {
            throw new IllegalArgumentException("effectiveDate là bắt buộc (BR4.1)");
        }
        boolean hasPage = page != null;
        boolean hasSection = section != null && !section.isBlank();
        if (!hasPage && !hasSection) {
            throw new IllegalArgumentException(
                    "Citation phải có ít nhất một trong page hoặc section (BR4.1)");
        }
    }
}

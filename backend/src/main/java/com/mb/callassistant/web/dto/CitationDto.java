package com.mb.callassistant.web.dto;

import com.mb.callassistant.copilot.Citation;

import java.time.LocalDate;

/**
 * DTO trích dẫn (hợp đồng C2). {@code effectiveDate} dạng ISO date
 * ({@code YYYY-MM-DD}); {@code documentTitle} + {@code effectiveDate} luôn có.
 */
public record CitationDto(
        String documentId,
        String documentTitle,
        Integer page,
        String section,
        LocalDate effectiveDate) {

    public static CitationDto from(Citation c) {
        return new CitationDto(c.documentId(), c.documentTitle(), c.page(), c.section(), c.effectiveDate());
    }
}

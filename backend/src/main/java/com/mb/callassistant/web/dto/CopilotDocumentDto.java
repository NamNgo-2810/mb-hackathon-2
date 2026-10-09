package com.mb.callassistant.web.dto;

import com.mb.callassistant.copilot.PolicyDocument;

import java.time.LocalDate;

/**
 * DTO tài liệu đã nạp (hợp đồng C2 {@code GET /api/copilot/documents}): chân cột
 * lịch sử tra cứu.
 */
public record CopilotDocumentDto(
        String documentId,
        String title,
        LocalDate effectiveDate) {

    public static CopilotDocumentDto from(PolicyDocument d) {
        return new CopilotDocumentDto(d.getDocumentId(), d.getTitle(), d.getEffectiveDate());
    }
}

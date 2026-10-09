package com.mb.callassistant.web.dto;

import com.mb.callassistant.lead.Customer;
import com.mb.callassistant.lead.Lead;

import java.time.OffsetDateTime;

/** DTO tóm tắt lead (C1 LeadSummary). */
public record LeadSummary(
        String leadId,
        String leadCode,
        String customerName,
        String productInterest,
        String source,
        String priority,
        Integer rankScore,
        OffsetDateTime slaDueAt,
        String stage
) {

    public static LeadSummary from(Lead lead, Customer customer) {
        return new LeadSummary(
                lead.getLeadId(),
                lead.getLeadCode(),
                customer == null ? null : customer.getFullName(),
                lead.getProductInterest(),
                lead.getSource(),
                lead.getPriority().name(),
                lead.getRankScore(),
                lead.getSlaDueAt(),
                lead.getStage().name()
        );
    }
}

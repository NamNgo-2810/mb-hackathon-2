package com.mb.callassistant.web.dto;

import com.mb.callassistant.lead.Customer;
import com.mb.callassistant.lead.Lead;

import java.time.OffsetDateTime;

/**
 * DTO chi tiết lead (C1 LeadDetail): các trường của {@link LeadSummary} cộng
 * customer, content, salesScript và phoneValid (BR2.1). {@code content} là văn
 * bản/JSON thô đã lưu (khối Nội dung); giao diện tự diễn giải.
 */
public record LeadDetail(
        String leadId,
        String leadCode,
        String customerName,
        String productInterest,
        String source,
        String priority,
        Integer rankScore,
        OffsetDateTime slaDueAt,
        String stage,
        CustomerDto customer,
        String content,
        String salesScript,
        boolean phoneValid
) {

    public static LeadDetail from(Lead lead, Customer customer) {
        return new LeadDetail(
                lead.getLeadId(),
                lead.getLeadCode(),
                customer == null ? null : customer.getFullName(),
                lead.getProductInterest(),
                lead.getSource(),
                lead.getPriority().name(),
                lead.getRankScore(),
                lead.getSlaDueAt(),
                lead.getStage().name(),
                CustomerDto.from(customer),
                lead.getContent(),
                lead.getSalesScript(),
                customer != null && customer.isPhoneValid()
        );
    }
}

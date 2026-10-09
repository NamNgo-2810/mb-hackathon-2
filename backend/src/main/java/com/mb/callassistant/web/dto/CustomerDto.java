package com.mb.callassistant.web.dto;

import com.mb.callassistant.lead.Customer;

/** DTO khách hàng trong LeadDetail (C1). estimatedIncome là số nguyên VND (BR6.2). */
public record CustomerDto(
        String customerId,
        String fullName,
        String segment,
        String phoneNumber,
        Integer age,
        String occupation,
        Long estimatedIncome,
        String address
) {

    public static CustomerDto from(Customer c) {
        if (c == null) {
            return null;
        }
        return new CustomerDto(
                c.getCustomerId(),
                c.getFullName(),
                c.getSegment(),
                c.getPhoneNumber(),
                c.getAge(),
                c.getOccupation(),
                c.getEstimatedIncome(),
                c.getAddress()
        );
    }
}

package com.mb.callassistant.web.dto;

import com.mb.callassistant.domain.CallRecord;

import java.time.Instant;

/**
 * DTO cuộc gọi (C1 Call). {@code callId}/{@code leadId} là chuỗi; giữ
 * {@code sttStatus} để tương thích giao diện cũ (contract C1).
 */
public record CallDto(
        String callId,
        String leadId,
        String rmId,
        String phoneNumber,
        Instant startedAt,
        Instant endedAt,
        Integer durationSeconds,
        String pipelineStatus,
        String pipelineStep,
        String errorDetail,
        String sttStatus
) {

    public static CallDto from(CallRecord cr) {
        return new CallDto(
                cr.getId() == null ? null : String.valueOf(cr.getId()),
                cr.getLeadId(),
                cr.getRmId(),
                cr.getPhoneNumber(),
                cr.getStartedAt(),
                cr.getEndedAt(),
                cr.getDurationSeconds(),
                cr.getPipelineStatus() == null ? null : cr.getPipelineStatus().name(),
                cr.getPipelineStep() == null ? null : cr.getPipelineStep().name(),
                cr.getErrorDetail(),
                cr.getSttStatus() == null ? null : cr.getSttStatus().name()
        );
    }
}

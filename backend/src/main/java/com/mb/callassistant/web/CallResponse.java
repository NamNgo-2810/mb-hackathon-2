package com.mb.callassistant.web;

import com.mb.callassistant.domain.CallRecord;

import java.time.Instant;

/**
 * Response view of a {@link CallRecord}, including the pipeline status so the
 * frontend can poll until the transcript is ready (FR1.3).
 */
public record CallResponse(
        Long id,
        String tnbCode,
        String phoneNumber,
        String rmName,
        Instant startedAt,
        Integer durationSeconds,
        String recordingName,
        String sttStatus,
        String errorDetail,
        Instant createdAt
) {

    public static CallResponse from(CallRecord cr) {
        return new CallResponse(
                cr.getId(),
                cr.getTnbCode(),
                cr.getPhoneNumber(),
                cr.getRmName(),
                cr.getStartedAt(),
                cr.getDurationSeconds(),
                cr.getRecordingName(),
                cr.getSttStatus() == null ? null : cr.getSttStatus().name(),
                cr.getErrorDetail(),
                cr.getCreatedAt()
        );
    }
}

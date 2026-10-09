package com.mb.callassistant.web.dto;

import com.mb.callassistant.lead.Activity;

import java.time.OffsetDateTime;
import java.util.List;

/** DTO hoạt động (C1 Activity). aiSummary null = "Không có tóm tắt AI" (BR2.3). */
public record ActivityDto(
        String activityId,
        String type,
        OffsetDateTime occurredAt,
        String product,
        String result,
        String note,
        String callId,
        String aiSummary,
        List<String> aiSuggestions
) {

    public static ActivityDto from(Activity a) {
        return new ActivityDto(
                a.getActivityId(),
                a.getType().name(),
                a.getOccurredAt(),
                a.getProduct(),
                a.getResult(),
                a.getNote(),
                a.getCallId(),
                a.getAiSummary(),
                a.getAiSuggestions()
        );
    }
}

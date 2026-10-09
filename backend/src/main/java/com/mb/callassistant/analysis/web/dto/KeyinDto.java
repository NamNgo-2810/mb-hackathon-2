package com.mb.callassistant.analysis.web.dto;

import com.mb.callassistant.analysis.AiFeedback;
import com.mb.callassistant.analysis.AnalysisJson;
import com.mb.callassistant.analysis.CallKeyin;
import com.mb.callassistant.analysis.FieldSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** DTO keyin đã lưu (C3, {@code GET /keyin}). Gồm đánh giá AI nếu có. */
public record KeyinDto(
        String callId,
        String leadStage,
        String callResult,
        Long demandAmount,
        String term,
        BigDecimal agreedRate,
        String product,
        String sourceOfFunds,
        LocalDate expectedDate,
        String channel,
        String objections,
        String rmNote,
        Map<String, FieldSource> fieldSources,
        OffsetDateTime savedAt,
        Integer feedbackRating,
        List<String> feedbackTags,
        String feedbackComment
) {

    public static KeyinDto from(CallKeyin k, AiFeedback feedback, AnalysisJson json) {
        return new KeyinDto(
                k.getCallId(),
                k.getLeadStage() == null ? null : k.getLeadStage().name(),
                k.getCallResult(),
                k.getDemandAmount(),
                k.getTerm(),
                k.getAgreedRate(),
                k.getProduct(),
                k.getSourceOfFunds(),
                k.getExpectedDate(),
                k.getChannel(),
                k.getObjections(),
                k.getRmNote(),
                json.readFieldSources(k.getFieldSourcesJson()),
                k.getSavedAt(),
                feedback == null ? null : feedback.getRating(),
                feedback == null ? null : feedback.getTags(),
                feedback == null ? null : feedback.getComment()
        );
    }
}

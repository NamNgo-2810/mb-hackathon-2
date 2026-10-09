package com.mb.callassistant.web;

import com.mb.callassistant.domain.TranscriptTurn;

/**
 * Response view of a {@link TranscriptTurn}: the RM/KH label, time window, and
 * Vietnamese text (FR4).
 */
public record TranscriptTurnResponse(
        Long id,
        String speaker,
        long startMs,
        long endMs,
        String textVi
) {

    public static TranscriptTurnResponse from(TranscriptTurn t) {
        return new TranscriptTurnResponse(
                t.getId(),
                t.getSpeaker() == null ? null : t.getSpeaker().name(),
                t.getStartMs(),
                t.getEndMs(),
                t.getTextVi()
        );
    }
}

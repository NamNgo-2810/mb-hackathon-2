package com.mb.callassistant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * One utterance in a call transcript, labeled RM (agent) or KH (customer),
 * with its time window and the Vietnamese text.
 */
@Entity
@Table(name = "transcript_turn", indexes = {
        @Index(name = "idx_turn_call", columnList = "call_id,start_ms")
})
public class TranscriptTurn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK-by-value to {@link CallRecord#getId()} (kept simple for the core slice). */
    @Column(name = "call_id", nullable = false)
    private Long callId;

    @Enumerated(EnumType.STRING)
    @Column(name = "speaker", nullable = false)
    private Speaker speaker;

    @Column(name = "start_ms", nullable = false)
    private long startMs;

    @Column(name = "end_ms", nullable = false)
    private long endMs;

    /** Transcribed text in Vietnamese. */
    @Column(name = "text_vi", length = 4000, nullable = false)
    private String textVi;

    protected TranscriptTurn() {
        // JPA
    }

    public TranscriptTurn(Long callId, Speaker speaker, long startMs, long endMs, String textVi) {
        this.callId = callId;
        this.speaker = speaker;
        this.startMs = startMs;
        this.endMs = endMs;
        this.textVi = textVi;
    }

    public Long getId() {
        return id;
    }

    public Long getCallId() {
        return callId;
    }

    public Speaker getSpeaker() {
        return speaker;
    }

    public long getStartMs() {
        return startMs;
    }

    public long getEndMs() {
        return endMs;
    }

    public String getTextVi() {
        return textVi;
    }
}

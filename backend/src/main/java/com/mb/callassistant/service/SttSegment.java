package com.mb.callassistant.service;

/**
 * One transcribed segment returned by an {@link SttClient}: the time window and
 * the Vietnamese text. Speaker is NOT part of this record — for per-leg audio
 * the caller already knows the speaker (RM or KH), and for a single mixed file
 * the provider's diarization label is carried in {@link #diarizationLabel}.
 *
 * @param startMs           segment start, milliseconds from the leg start
 * @param endMs             segment end, milliseconds from the leg start
 * @param textVi            transcribed Vietnamese text
 * @param diarizationLabel  provider diarization label for single-file fallback
 *                          (e.g. "speaker_0"), or {@code null} for per-leg audio
 */
public record SttSegment(long startMs, long endMs, String textVi, String diarizationLabel) {

    public SttSegment(long startMs, long endMs, String textVi) {
        this(startMs, endMs, textVi, null);
    }
}

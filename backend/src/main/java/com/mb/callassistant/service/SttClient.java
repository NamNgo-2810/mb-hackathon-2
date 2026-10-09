package com.mb.callassistant.service;

import java.util.List;

/**
 * Abstraction over the Vietnamese speech-to-text provider (FR4). The core slice
 * ships {@link WhisperHttpSttClient}, which targets a Whisper-compatible HTTP
 * endpoint; the concrete provider is deferred and pluggable via config
 * ({@code STT_PROVIDER}, {@code STT_BASE_URL}, {@code STT_API_KEY}).
 */
public interface SttClient {

    /**
     * Transcribe a single .wav leg.
     *
     * @param wavBytes       the raw .wav audio
     * @param languageHint   BCP-47-ish language hint, e.g. {@code "vi"}
     * @param diarize        request speaker diarization (used for single mixed files)
     * @return ordered transcript segments
     * @throws SttException when the provider is unreachable or returns an error
     */
    List<SttSegment> transcribe(byte[] wavBytes, String languageHint, boolean diarize);
}

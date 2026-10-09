package com.mb.callassistant.domain;

/** Lifecycle of the recording-download → speech-to-text pipeline for a call. */
public enum SttStatus {
    /** Call record created; call may still be in progress, UUID may be unresolved. */
    PENDING,
    /** Call marked ended; pipeline queued/running. */
    PROCESSING,
    /**
     * Recording was not found yet after bounded retries, but may still appear
     * (FreeSWITCH flushes the .wav a moment after hangup). Non-terminal: the
     * frontend keeps polling and the pipeline can be retried (FR1.3, FR3.2).
     */
    RECORDING_NOT_READY,
    /** Transcript produced and persisted. */
    DONE,
    /** Permanently failed: STT error, or recording never appeared. See error detail. */
    FAILED
}

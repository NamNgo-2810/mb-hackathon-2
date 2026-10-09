package com.mb.callassistant.service;

/**
 * Thrown when the speech-to-text provider is unreachable, misconfigured, or
 * returns an error (FR4). Mapped to a FAILED pipeline status; surfaced, never
 * swallowed (phase guardrails).
 */
public class SttException extends RuntimeException {

    public SttException(String message) {
        super(message);
    }

    public SttException(String message, Throwable cause) {
        super(message, cause);
    }
}

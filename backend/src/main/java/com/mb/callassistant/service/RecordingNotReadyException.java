package com.mb.callassistant.service;

/**
 * Thrown when a recording cannot be fetched because it does not exist yet or is
 * not ready on the recording server (FR3.2). Mapped to HTTP 404/409 at the
 * controller boundary.
 */
public class RecordingNotReadyException extends RuntimeException {

    public RecordingNotReadyException(String message) {
        super(message);
    }

    public RecordingNotReadyException(String message, Throwable cause) {
        super(message, cause);
    }
}

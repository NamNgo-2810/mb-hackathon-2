package com.mb.callassistant.llm;

/**
 * A classified LLM failure thrown back to the caller (I3 contract:
 * {@code LlmException(kind: ...)}). The caller (U3/U4) uses {@link #kind()} and
 * {@link #isRetryable()} to pick a fallback (BR3.2, BR4.1, WF3).
 *
 * <p><b>Security (BR1.2 / NFR2.4 / NFR2.5):</b> the message must never contain
 * the API key or any prompt/response content. Callers constructing one must
 * pass only a short, non-sensitive technical description.
 */
public class LlmException extends RuntimeException {

    /** The five fixed failure categories (BR4.1, I3). */
    public enum Kind {
        /** No API key configured; thrown immediately, no network call (BR2.1). */
        NOT_CONFIGURED,
        /** The call exceeded its timeout (BR3.1). */
        TIMEOUT,
        /** Connection failure, 5xx, or 529 overloaded (BR4.1). */
        UNAVAILABLE,
        /** Provider rate limit, HTTP 429 (BR4.1). */
        RATE_LIMITED,
        /** Other 4xx, empty body, or an unreadable/invalid response (BR4.1). */
        BAD_RESPONSE
    }

    private final Kind kind;
    private final boolean retryable;
    /** Provider HTTP status when known, otherwise {@code null}. */
    private final Integer providerStatus;

    public LlmException(Kind kind, boolean retryable, Integer providerStatus, String message) {
        super(message);
        this.kind = kind;
        this.retryable = retryable;
        this.providerStatus = providerStatus;
    }

    public LlmException(Kind kind, boolean retryable, Integer providerStatus, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.retryable = retryable;
        this.providerStatus = providerStatus;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Integer providerStatus() {
        return providerStatus;
    }
}

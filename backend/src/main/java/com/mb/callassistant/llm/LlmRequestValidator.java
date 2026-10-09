package com.mb.callassistant.llm;

/**
 * Boundary input validation for an {@link LlmRequest} (BR2.2, NFR2.8). A failed
 * check is a <b>programming error</b> on the caller's side, so it throws
 * {@link IllegalArgumentException} — <i>not</i> an {@link LlmException} — and
 * always <i>before</i> any network call is made.
 *
 * <p>Messages describe which field is wrong but never echo the actual
 * {@code systemPrompt}/{@code userContent}/{@code cachedContext} content, so no
 * prompt data leaks into an exception (BR1.2).
 */
final class LlmRequestValidator {

    /** Hard ceiling on any per-call timeout, in milliseconds (BR2.2/BR3.1). */
    static final int MAX_TIMEOUT_MS = 120000;

    private LlmRequestValidator() {
    }

    /**
     * @param request            the request to validate (must be non-null)
     * @param maxOutputTokensCap configured cap for {@code maxOutputTokens}
     * @throws IllegalArgumentException when any field is invalid
     */
    static void validate(LlmRequest request, int maxOutputTokensCap) {
        if (request == null) {
            throw new IllegalArgumentException("LlmRequest must not be null");
        }
        if (isBlank(request.systemPrompt())) {
            throw new IllegalArgumentException("LlmRequest.systemPrompt must not be empty");
        }
        if (isBlank(request.userContent())) {
            throw new IllegalArgumentException("LlmRequest.userContent must not be empty");
        }
        if (request.maxOutputTokens() < 1 || request.maxOutputTokens() > maxOutputTokensCap) {
            throw new IllegalArgumentException(
                    "LlmRequest.maxOutputTokens must be within [1, " + maxOutputTokensCap + "]");
        }
        Integer timeout = request.timeout();
        if (timeout != null) {
            if (timeout < 1) {
                throw new IllegalArgumentException("LlmRequest.timeout must be a positive number of milliseconds");
            }
            if (timeout > MAX_TIMEOUT_MS) {
                throw new IllegalArgumentException("LlmRequest.timeout must not exceed " + MAX_TIMEOUT_MS + " ms");
            }
        }
        if (request.purpose() == null) {
            throw new IllegalArgumentException("LlmRequest.purpose must not be null");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

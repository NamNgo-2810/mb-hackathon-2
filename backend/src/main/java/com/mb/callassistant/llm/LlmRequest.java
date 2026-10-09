package com.mb.callassistant.llm;

/**
 * A single request to the shared LLM gateway (I3 contract {@code LlmClient}).
 * Built by a caller (U3 Copilot or U4 CallAnalysis) and passed to
 * {@link LlmClient#complete(LlmRequest)}.
 *
 * <p>Field names follow the I3 contract verbatim. Note the contract/FD naming
 * reconciliation (tech-stack-decisions.md): FD's {@code timeoutMs} is this
 * record's {@link #timeout()} (milliseconds), the name the contract uses.
 *
 * <ul>
 *   <li>{@code systemPrompt} — the system instruction (BR5.1, trusted).</li>
 *   <li>{@code cachedContext} — optional fixed context (scoring rubric, field
 *       descriptions) that may use provider prompt caching (BR5.2); treated as
 *       untrusted DATA, never as instructions.</li>
 *   <li>{@code userContent} — untrusted data (transcript, document excerpt,
 *       question); always wrapped in a DATA delimiter block (BR5.1).</li>
 *   <li>{@code maxOutputTokens} — must be within {@code [1, maxOutputTokensCap]}
 *       (BR2.2).</li>
 *   <li>{@code timeout} — optional per-call timeout in milliseconds; absent
 *       (null) means the configured default (30s); must be {@code <= 120000}
 *       (BR2.2/BR3.1).</li>
 *   <li>{@code expectJson} — when {@code true}, the gateway verifies the result
 *       parses as a JSON object (BR4.2).</li>
 *   <li>{@code purpose} — logging/metrics tag only (does not change behaviour).</li>
 * </ul>
 */
public record LlmRequest(
        String systemPrompt,
        String cachedContext,
        String userContent,
        int maxOutputTokens,
        Integer timeout,
        boolean expectJson,
        LlmPurpose purpose) {

    /**
     * Convenience factory for the common case: no cached context, default
     * timeout, with an explicit {@code expectJson} flag.
     */
    public static LlmRequest of(
            String systemPrompt, String userContent, int maxOutputTokens,
            boolean expectJson, LlmPurpose purpose) {
        return new LlmRequest(systemPrompt, null, userContent, maxOutputTokens, null, expectJson, purpose);
    }
}

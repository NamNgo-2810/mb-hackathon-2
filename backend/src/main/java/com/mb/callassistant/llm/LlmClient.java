package com.mb.callassistant.llm;

/**
 * Shared LLM gateway (I3 contract, ADR-004). The single in-process Java
 * interface U3 (Copilot) and U4 (CallAnalysis) depend on; they never talk to a
 * provider SDK directly (NFR3.4).
 *
 * <p>Implementations are selected by configuration {@code app.llm.provider}:
 * {@link AnthropicLlmClient} (real) or {@link MockLlmClient} (canned, offline).
 */
public interface LlmClient {

    /**
     * Run one completion.
     *
     * @param request the request (validated before any network call, BR2.2)
     * @return the successful result
     * @throws LlmException            on a classified provider/config failure
     *                                 (BR2.1, BR3.1, BR4.1); never retried
     *                                 internally (BR3.2)
     * @throws IllegalArgumentException when the request is invalid (BR2.2,
     *                                  a programming error, not an LlmException)
     */
    LlmResponse complete(LlmRequest request);
}

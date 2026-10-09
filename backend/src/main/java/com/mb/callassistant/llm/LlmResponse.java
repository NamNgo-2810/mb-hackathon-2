package com.mb.callassistant.llm;

/**
 * A successful result from the LLM gateway (I3 contract: {@code LlmResponse{
 * text, model, usage }}). The {@code usage} fields of the contract are expanded
 * here into the individual token counts plus latency and the truncation flag
 * (entities.md {@code LlmResponse}).
 *
 * <ul>
 *   <li>{@code text} — the model's answer text.</li>
 *   <li>{@code model} — the resolved model id that produced it.</li>
 *   <li>{@code inputTokens} / {@code outputTokens} / {@code cachedInputTokens}
 *       — provider usage counts (0 when the provider did not report them).</li>
 *   <li>{@code latencyMs} — wall-clock latency of the call.</li>
 *   <li>{@code truncated} — {@code true} when the response stopped because it
 *       hit {@code maxOutputTokens} (Anthropic {@code stop_reason=max_tokens}).</li>
 * </ul>
 */
public record LlmResponse(
        String text,
        String model,
        int inputTokens,
        int outputTokens,
        int cachedInputTokens,
        long latencyMs,
        boolean truncated) {
}

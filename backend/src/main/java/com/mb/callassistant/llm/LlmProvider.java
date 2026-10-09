package com.mb.callassistant.llm;

/**
 * Which LLM backend the shared gateway uses (ADR-004, BR6.1).
 *
 * <ul>
 *   <li>{@link #ANTHROPIC} — calls the real Anthropic Claude Messages API.</li>
 *   <li>{@link #MOCK} — returns canned results without any network call, for
 *       tests and for a demo without an API key.</li>
 * </ul>
 */
public enum LlmProvider {
    ANTHROPIC,
    MOCK
}

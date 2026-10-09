package com.mb.callassistant.llm;

/**
 * Why a call is being made. Used for logging and metrics only — it never
 * changes gateway behaviour (entities.md, {@code LlmRequest.purpose}).
 *
 * <ul>
 *   <li>{@link #CALL_ANALYSIS} — U4 CallAnalysis scoring/summarising a call.</li>
 *   <li>{@link #COPILOT} — U3 Copilot answering a policy question.</li>
 * </ul>
 */
public enum LlmPurpose {
    CALL_ANALYSIS,
    COPILOT
}

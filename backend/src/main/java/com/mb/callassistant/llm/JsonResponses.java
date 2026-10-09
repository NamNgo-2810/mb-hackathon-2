package com.mb.callassistant.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helper for the {@code expectJson} check (BR4.2 / NFR2.7): decide whether a
 * model's text output parses as a JSON <b>object</b>, tolerating a surrounding
 * Markdown code fence (```` ```json ... ``` ````). Structural/business
 * validation of the JSON is the caller's job, not the gateway's.
 */
final class JsonResponses {

    private JsonResponses() {
    }

    /**
     * @return {@code true} when {@code text} (after stripping an optional code
     *         fence) parses as a JSON object; {@code false} otherwise
     */
    static boolean isJsonObject(ObjectMapper mapper, String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String candidate = stripFence(text.strip());
        try {
            JsonNode node = mapper.readTree(candidate);
            return node != null && node.isObject();
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            return false;
        }
    }

    /** Remove a leading ```` ```json ```` / ```` ``` ```` fence and its closing ```` ``` ````. */
    private static String stripFence(String text) {
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNewline = text.indexOf('\n');
        if (firstNewline < 0) {
            return text;
        }
        String firstLine = text.substring(3, firstNewline).trim();
        // Only treat as a fence when the info string is empty or a json-ish tag.
        if (!(firstLine.isEmpty() || firstLine.equalsIgnoreCase("json"))) {
            return text;
        }
        String rest = text.substring(firstNewline + 1);
        int closing = rest.lastIndexOf("```");
        if (closing >= 0) {
            rest = rest.substring(0, closing);
        }
        return rest.strip();
    }
}

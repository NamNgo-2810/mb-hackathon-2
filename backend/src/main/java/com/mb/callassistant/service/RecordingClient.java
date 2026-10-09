package com.mb.callassistant.service;

import com.mb.callassistant.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Thin client for the <b>two</b> HTTP endpoints the real FreeSWITCH/EC2 recording
 * server exposes (FR2, FR3). There is no UUID resolution and no dialplan key —
 * the "most recent recording filename" is the call↔recording link (demo-simple):
 *
 * <ul>
 *   <li>{@code GET <base>/api/recordings/latest} — the filename of the most
 *       recent recording. The response may be JSON ({@code {"name":"..."}} or
 *       {@code {"filename":"..."}}) or a bare string; both are handled.</li>
 *   <li>{@code GET <base>/api/recordings/download/:name} — the {@code .wav} bytes
 *       for that filename.</li>
 * </ul>
 *
 * <p>Base URL and optional API key come from the environment via
 * {@link AppProperties} (NFR3) — no hardcoded secrets.
 */
@Component
public class RecordingClient {

    private static final Logger log = LoggerFactory.getLogger(RecordingClient.class);
    private static final Duration LATEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;
    private final AppProperties props;

    public RecordingClient(WebClient webClient, AppProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    /**
     * Fetch the filename of the most recent recording from
     * {@code GET <base>/api/recordings/latest}. Handles both a JSON object (keys
     * {@code name}/{@code filename}/{@code file}/{@code recording}) and a bare
     * string body defensively.
     *
     * @return the filename, or empty if the server is unconfigured/unreachable or
     *         reports no recording yet (the caller then retries / stays not-ready)
     */
    @SuppressWarnings("unchecked")
    public Optional<String> latestFilename() {
        String root = baseRootOrNull();
        if (root == null) {
            log.debug("RECORDING_BASE_URL not configured; cannot fetch latest recording filename.");
            return Optional.empty();
        }
        String url = root + "/api/recordings/latest";

        try {
            // Read the raw body as a String first so a plain-string response works too.
            String body = webClient.get()
                    .uri(url)
                    .headers(this::applyAuth)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(LATEST_TIMEOUT);

            String name = parseFilename(body);
            if (name == null || name.isBlank()) {
                log.info("Recordings 'latest' endpoint returned no filename yet ({}).", url);
                return Optional.empty();
            }
            log.info("Latest recording filename resolved: {}", name);
            return Optional.of(name);
        } catch (WebClientResponseException.NotFound nf) {
            // No recording available yet.
            return Optional.empty();
        } catch (WebClientResponseException e) {
            log.warn("Recordings 'latest' error {} from {}: {}", e.getRawStatusCode(), url, e.getStatusText());
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("Recordings 'latest' unreachable at {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Download the {@code .wav} bytes for {@code name} from
     * {@code GET <base>/api/recordings/download/:name}.
     *
     * @return the raw bytes, or {@code null} when the file is not there yet
     *         (404 / empty), so the caller can retry with backoff
     */
    public byte[] download(String name) {
        String root = baseRootOrNull();
        if (root == null || name == null || name.isBlank()) {
            return null;
        }
        String encoded = UriUtils.encodePathSegment(name, StandardCharsets.UTF_8);
        String url = root + "/api/recordings/download/" + encoded;
        try {
            byte[] bytes = webClient.get()
                    .uri(url)
                    .headers(this::applyAuth)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(DOWNLOAD_TIMEOUT);
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            log.info("Downloaded recording {} ({} bytes).", name, bytes.length);
            return bytes;
        } catch (WebClientResponseException.NotFound nf) {
            return null; // not flushed yet — caller decides
        } catch (RuntimeException e) {
            log.warn("Recording download failed for {}: {}", url, e.getMessage());
            return null;
        }
    }

    /** The configured base URL with trailing slashes trimmed, or {@code null} when unset. */
    private String baseRootOrNull() {
        String base = props.getRecording().getBaseUrl();
        if (base == null || base.isBlank()) {
            return null;
        }
        return base.replaceAll("/+$", "");
    }

    /**
     * Parse the filename out of the 'latest' response. Accepts a JSON object with
     * any of a few common keys, a JSON string, or a bare string.
     */
    private String parseFilename(String body) {
        if (body == null) {
            return null;
        }
        String trimmed = body.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // JSON object?
        if (trimmed.startsWith("{")) {
            Map<String, Object> map = tryParseJsonObject(trimmed);
            if (map != null) {
                for (String key : new String[] {"name", "filename", "file", "recording", "latest"}) {
                    Object v = map.get(key);
                    if (v != null && !String.valueOf(v).isBlank()) {
                        return String.valueOf(v).trim();
                    }
                }
                return null;
            }
        }
        // JSON string ("foo.wav") — strip the surrounding quotes.
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        // Bare string.
        return trimmed;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> tryParseJsonObject(String json) {
        try {
            // Reuse Jackson via Spring's shared ObjectMapper indirectly through a tiny read.
            com.fasterxml.jackson.databind.ObjectMapper mapper = JacksonHolder.MAPPER;
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.debug("Could not parse 'latest' body as JSON object: {}", e.getMessage());
            return null;
        }
    }

    private void applyAuth(HttpHeaders headers) {
        String key = props.getRecording().getApiKey();
        if (key != null && !key.isBlank()) {
            headers.set("X-API-Key", key);
        }
    }

    /** Lazily-held shared Jackson mapper (ObjectMapper is thread-safe for reads). */
    private static final class JacksonHolder {
        private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
                new com.fasterxml.jackson.databind.ObjectMapper();
    }
}

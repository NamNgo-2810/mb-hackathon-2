package com.mb.callassistant.service;

import com.mb.callassistant.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link SttClient} backed by the <b>Google Cloud Speech-to-Text</b> REST API
 * ({@code POST https://speech.googleapis.com/v1/speech:recognize?key=<API_KEY>}).
 * Selected when {@code app.stt.provider=google}.
 *
 * <p>Audio is sent inline, base64-encoded, with {@code languageCode=vi-VN} for
 * Vietnamese. We request word time offsets so we can derive per-utterance
 * timings for the transcript turns. Because {@link SttService} already splits
 * the stereo recording into two mono channels, each request is single-channel.
 *
 * <p>The API key comes from {@code app.stt.api-key} ({@code STT_API_KEY}) — the
 * key for a project with the Cloud Speech-to-Text API enabled (NFR3: from env,
 * never hardcoded). The optional {@code app.stt.base-url} overrides the Google
 * endpoint host for testing; it defaults to the public Google endpoint.
 */
@Component
@ConditionalOnProperty(name = "app.stt.provider", havingValue = "google", matchIfMissing = true)
public class GoogleSttClient implements SttClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleSttClient.class);
    private static final String DEFAULT_HOST = "https://speech.googleapis.com";
    private static final java.time.Duration TIMEOUT = java.time.Duration.ofSeconds(120);

    private final WebClient webClient;
    private final AppProperties props;

    public GoogleSttClient(WebClient webClient, AppProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SttSegment> transcribe(byte[] wavBytes, String languageHint, boolean diarize) {
        String apiKey = props.getStt().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new SttException(
                    "STT_API_KEY is not configured; cannot call Google Cloud Speech-to-Text. "
                            + "Set it to an API key for a project with the Cloud Speech-to-Text API enabled.");
        }
        String host = props.getStt().getBaseUrl();
        if (host == null || host.isBlank()) {
            host = DEFAULT_HOST;
        }
        String url = host.replaceAll("/+$", "") + "/v1/speech:recognize?key=" + apiKey;

        // Google wants the raw audio base64-encoded, inline in the request.
        String audioB64 = Base64.getEncoder().encodeToString(wavBytes);

        Map<String, Object> config = new LinkedHashMap<>();
        // WAV from FreeSWITCH is LINEAR16 PCM. Let Google read the sample rate from
        // the WAV header by omitting sampleRateHertz (supported for LINEAR16 WAV).
        config.put("encoding", "LINEAR16");
        config.put("languageCode", languageHint == null || languageHint.isBlank() ? "vi-VN" : toBcp47(languageHint));
        config.put("enableWordTimeOffsets", true);
        config.put("audioChannelCount", 1); // SttService already split stereo into mono channels
        config.put("model", "default");

        Map<String, Object> audio = new LinkedHashMap<>();
        audio.put("content", audioB64);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("config", config);
        payload.put("audio", audio);

        try {
            Map<String, Object> body = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(TIMEOUT);

            if (body == null) {
                throw new SttException("Google STT returned an empty response.");
            }
            return mapResults(body);
        } catch (WebClientResponseException e) {
            throw new SttException(
                    "Google STT error " + e.getRawStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (SttException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new SttException("Google STT request failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<SttSegment> mapResults(Map<String, Object> body) {
        List<SttSegment> out = new ArrayList<>();
        Object resultsObj = body.get("results");
        if (!(resultsObj instanceof List<?> results) || results.isEmpty()) {
            log.warn("Google STT returned no results; empty transcript for this channel.");
            return out;
        }
        for (Object r : results) {
            if (!(r instanceof Map)) {
                continue;
            }
            Map<String, Object> result = (Map<String, Object>) r;
            Object altsObj = result.get("alternatives");
            if (!(altsObj instanceof List<?> alts) || alts.isEmpty()) {
                continue;
            }
            Map<String, Object> best = (Map<String, Object>) alts.get(0);
            String text = best.get("transcript") == null ? "" : String.valueOf(best.get("transcript")).trim();
            if (text.isEmpty()) {
                continue;
            }
            long startMs = 0L;
            long endMs = 0L;
            Object wordsObj = best.get("words");
            if (wordsObj instanceof List<?> words && !words.isEmpty()) {
                startMs = parseDurationMs(((Map<String, Object>) words.get(0)).get("startTime"));
                endMs = parseDurationMs(((Map<String, Object>) words.get(words.size() - 1)).get("endTime"));
            }
            out.add(new SttSegment(startMs, endMs, text, null));
        }
        return out;
    }

    /**
     * Google durations are strings like {@code "1.700s"} (or sometimes a
     * {@code {seconds, nanos}} object). Convert to milliseconds.
     */
    @SuppressWarnings("unchecked")
    private long parseDurationMs(Object duration) {
        if (duration == null) {
            return 0L;
        }
        if (duration instanceof String s) {
            String t = s.endsWith("s") ? s.substring(0, s.length() - 1) : s;
            try {
                return Math.round(Double.parseDouble(t) * 1000.0);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        if (duration instanceof Map<?, ?> m) {
            double seconds = toDouble(((Map<String, Object>) m).get("seconds"));
            double nanos = toDouble(((Map<String, Object>) m).get("nanos"));
            return Math.round(seconds * 1000.0 + nanos / 1_000_000.0);
        }
        return 0L;
    }

    private double toDouble(Object n) {
        return n instanceof Number num ? num.doubleValue() : 0.0;
    }

    /** Map a short hint like {@code "vi"} to Google's {@code "vi-VN"}. */
    private String toBcp47(String hint) {
        String h = hint.trim();
        if (h.equalsIgnoreCase("vi")) {
            return "vi-VN";
        }
        return h;
    }
}

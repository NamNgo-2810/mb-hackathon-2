package com.mb.callassistant.service;

import com.mb.callassistant.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Default {@link SttClient}: posts the .wav to a Whisper-compatible HTTP endpoint
 * and maps the returned segments. Targets the OpenAI/Whisper-style
 * {@code POST <STT_BASE_URL>/v1/audio/transcriptions} multipart contract with
 * {@code response_format=verbose_json} (segment timestamps).
 *
 * <p>The exact provider is deferred: swap this bean for another {@link SttClient}
 * without touching {@link SttService}. All endpoint/secret values come from the
 * environment via {@link AppProperties} (NFR3).
 */
@Component
@ConditionalOnProperty(name = "app.stt.provider", havingValue = "whisper")
public class WhisperHttpSttClient implements SttClient {

    private static final Logger log = LoggerFactory.getLogger(WhisperHttpSttClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(120);

    private final WebClient webClient;
    private final AppProperties props;

    public WhisperHttpSttClient(WebClient webClient, AppProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SttSegment> transcribe(byte[] wavBytes, String languageHint, boolean diarize) {
        String base = props.getStt().getBaseUrl();
        if (base == null || base.isBlank()) {
            throw new SttException(
                    "STT_BASE_URL is not configured; cannot transcribe. Set it to a Whisper-compatible endpoint.");
        }
        String url = base.replaceAll("/+$", "") + "/v1/audio/transcriptions";

        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("file", new ByteArrayResource(wavBytes) {
            @Override
            public String getFilename() {
                return "audio.wav";
            }
        }).contentType(MediaType.parseMediaType("audio/wav"));
        mb.part("model", "whisper-1");
        mb.part("language", languageHint == null ? "vi" : languageHint);
        mb.part("response_format", "verbose_json");
        if (diarize) {
            // Non-standard hint honored by several Whisper-compatible servers; ignored otherwise.
            mb.part("diarize", "true");
        }

        try {
            Map<String, Object> body = webClient.post()
                    .uri(url)
                    .headers(this::applyAuth)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(mb.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(TIMEOUT);

            if (body == null) {
                throw new SttException("STT provider returned an empty response for " + url);
            }
            return mapSegments(body);
        } catch (WebClientResponseException e) {
            throw new SttException(
                    "STT provider error " + e.getRawStatusCode() + " from " + url + ": " + e.getStatusText(), e);
        } catch (SttException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new SttException("STT request to " + url + " failed: " + e.getMessage(), e);
        }
    }

    private void applyAuth(HttpHeaders headers) {
        String key = props.getStt().getApiKey();
        if (key != null && !key.isBlank()) {
            headers.setBearerAuth(key);
        }
    }

    @SuppressWarnings("unchecked")
    private List<SttSegment> mapSegments(Map<String, Object> body) {
        List<SttSegment> out = new ArrayList<>();
        Object segments = body.get("segments");
        if (segments instanceof List<?> list && !list.isEmpty()) {
            for (Object o : list) {
                if (!(o instanceof Map)) {
                    continue;
                }
                Map<String, Object> seg = (Map<String, Object>) o;
                long startMs = toMillis(seg.get("start"));
                long endMs = toMillis(seg.get("end"));
                String text = seg.get("text") == null ? "" : String.valueOf(seg.get("text")).trim();
                String label = seg.get("speaker") == null ? null : String.valueOf(seg.get("speaker"));
                if (!text.isEmpty()) {
                    out.add(new SttSegment(startMs, endMs, text, label));
                }
            }
            return out;
        }

        // Fallback: a single "text" field with no segment timings.
        Object text = body.get("text");
        if (text != null && !String.valueOf(text).isBlank()) {
            out.add(new SttSegment(0, 0, String.valueOf(text).trim(), null));
        }
        if (out.isEmpty()) {
            log.warn("STT response had no segments and no text; returning empty transcript.");
        }
        return out;
    }

    /** Whisper reports seconds (floating point); we store milliseconds. */
    private long toMillis(Object seconds) {
        if (seconds instanceof Number n) {
            return Math.round(n.doubleValue() * 1000.0);
        }
        return 0L;
    }
}

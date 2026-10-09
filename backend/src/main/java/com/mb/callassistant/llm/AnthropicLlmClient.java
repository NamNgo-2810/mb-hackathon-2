package com.mb.callassistant.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mb.callassistant.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * {@link LlmClient} backed by the <b>Anthropic Claude Messages API</b>
 * ({@code POST <baseUrl>/v1/messages}). Selected when
 * {@code app.llm.provider=anthropic} (the default).
 *
 * <p>Follows the {@code GoogleSttClient} shape: the shared {@link WebClient}
 * and {@link AppProperties} are injected, the call is made synchronously with
 * {@code .block(Duration)}, and provider errors are mapped to a classified
 * {@link LlmException} (BR4.1). The library never retries (BR3.2).
 *
 * <p><b>Secrets:</b> the API key is read directly from the
 * {@code ANTHROPIC_API_KEY} environment variable (BR1.1 / NFR2.1); when it is
 * absent the gateway is {@code NOT_CONFIGURED} and {@link #complete} throws
 * immediately without touching the network (BR2.1 / NFR2.2). The key, prompts,
 * cached context, and response text are never logged (BR1.2 / NFR2.4 / NFR2.5).
 */
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "anthropic", matchIfMissing = true)
public class AnthropicLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmClient.class);
    /** Environment variable holding the API key — the only source (BR1.1). */
    static final String API_KEY_ENV = "ANTHROPIC_API_KEY";
    /** Anthropic "overloaded" status, treated as UNAVAILABLE (BR4.1). */
    private static final int STATUS_OVERLOADED = 529;

    private final WebClient webClient;
    private final AppProperties props;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public AnthropicLlmClient(WebClient webClient, AppProperties props, ObjectMapper objectMapper) {
        this(webClient, props, objectMapper, System.getenv(API_KEY_ENV));
    }

    /** Test seam: inject the key directly instead of reading the environment. */
    AnthropicLlmClient(WebClient webClient, AppProperties props, ObjectMapper objectMapper, String apiKey) {
        this.webClient = webClient;
        this.props = props;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        if (isBlank(apiKey)) {
            // WF1 step 2: warn without the value; stay NOT_CONFIGURED, keep running.
            log.warn("LLM is not configured: {} is not set; LlmClient will return NOT_CONFIGURED.", API_KEY_ENV);
        }
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        AppProperties.Llm cfg = props.getLlm();

        // BR2.2 / NFR2.8 — reject invalid input before anything else.
        LlmRequestValidator.validate(request, cfg.getMaxOutputTokensCap());

        // BR2.1 / NFR2.2 — not configured: fail immediately, no network.
        if (isBlank(apiKey)) {
            throw new LlmException(LlmException.Kind.NOT_CONFIGURED, false, null,
                    "LLM not configured: " + API_KEY_ENV + " is not set");
        }

        String model = cfg.getModel();
        if (isBlank(model)) {
            // model is operator configuration, not a per-call runtime failure (NFR2.3).
            throw new IllegalArgumentException(
                    "app.llm.model is not configured; set ANTHROPIC_MODEL for the anthropic provider");
        }

        String baseUrl = normalizeBase(cfg.getBaseUrl());
        if (!baseUrl.startsWith("https://")) {
            // NFR2.9 — the real provider must be reached over TLS.
            throw new IllegalArgumentException("app.llm.base-url must use https for the anthropic provider");
        }
        String url = baseUrl + "/v1/messages";

        int timeoutMs = request.timeout() != null ? request.timeout() : cfg.getDefaultTimeoutMs();
        Duration timeout = Duration.ofMillis(timeoutMs);

        Map<String, Object> payload = MessageBuilder.build(request, model, true);

        long startNanos = System.nanoTime();
        Map<String, Object> body;
        try {
            body = webClient.post()
                    .uri(url)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", cfg.getAnthropicVersion())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(mapType())
                    .block(timeout);
        } catch (WebClientResponseException e) {
            throw mapStatus(request, e.getStatusCode().value(), e);
        } catch (WebClientRequestException e) {
            // connection refused / DNS / reset — treat as UNAVAILABLE (retryable).
            throw unavailable(request, "provider connection failed", e);
        } catch (RuntimeException e) {
            if (isTimeout(e)) {
                throw timeout(request, e);
            }
            throw badResponse(request, "provider request failed", e);
        }
        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;

        if (body == null || body.isEmpty()) {
            throw badResponse(request, "empty provider response", null);
        }

        LlmResponse response = mapResponse(request, body, model, latencyMs);

        // BR4.2 / NFR2.7 — when JSON is expected, verify it parses (allow fences).
        if (request.expectJson() && !JsonResponses.isJsonObject(objectMapper, response.text())) {
            throw badResponse(request, "expected a JSON object but response was not JSON", null);
        }

        logSuccess(request, response);
        return response;
    }

    @SuppressWarnings("unchecked")
    private LlmResponse mapResponse(LlmRequest request, Map<String, Object> body, String model, long latencyMs) {
        try {
            StringBuilder text = new StringBuilder();
            Object contentObj = body.get("content");
            if (contentObj instanceof List<?> blocks) {
                for (Object b : blocks) {
                    if (b instanceof Map<?, ?> block && "text".equals(block.get("type"))) {
                        Object t = block.get("text");
                        if (t != null) {
                            text.append(t);
                        }
                    }
                }
            }
            if (text.length() == 0) {
                throw badResponse(request, "provider response had no text content", null);
            }

            String respModel = body.get("model") instanceof String m ? m : model;
            boolean truncated = "max_tokens".equals(body.get("stop_reason"));

            int inputTokens = 0;
            int outputTokens = 0;
            int cachedInputTokens = 0;
            if (body.get("usage") instanceof Map<?, ?> usage) {
                inputTokens = asInt(usage.get("input_tokens"));
                outputTokens = asInt(usage.get("output_tokens"));
                // cache read tokens when the provider reports them (BR5.2 best-effort).
                cachedInputTokens = asInt(usage.get("cache_read_input_tokens"));
            }
            return new LlmResponse(text.toString(), respModel, inputTokens, outputTokens,
                    cachedInputTokens, latencyMs, truncated);
        } catch (LlmException e) {
            throw e;
        } catch (RuntimeException e) {
            throw badResponse(request, "could not read provider response", e);
        }
    }

    // --- error mapping (BR4.1 / NFR3.1) ---

    private LlmException mapStatus(LlmRequest request, int status, Throwable cause) {
        if (status == 429) {
            LlmException ex = new LlmException(LlmException.Kind.RATE_LIMITED, true, status,
                    "provider rate limited", cause);
            logFailure(request, ex);
            return ex;
        }
        if (status == STATUS_OVERLOADED || status >= 500) {
            LlmException ex = new LlmException(LlmException.Kind.UNAVAILABLE, true, status,
                    "provider unavailable", cause);
            logFailure(request, ex);
            return ex;
        }
        // other 4xx -> bad response, not retryable.
        LlmException ex = new LlmException(LlmException.Kind.BAD_RESPONSE, false, status,
                "provider rejected the request", cause);
        logFailure(request, ex);
        return ex;
    }

    private LlmException timeout(LlmRequest request, Throwable cause) {
        LlmException ex = new LlmException(LlmException.Kind.TIMEOUT, true, null, "provider call timed out", cause);
        logFailure(request, ex);
        return ex;
    }

    private LlmException unavailable(LlmRequest request, String message, Throwable cause) {
        LlmException ex = new LlmException(LlmException.Kind.UNAVAILABLE, true, null, message, cause);
        logFailure(request, ex);
        return ex;
    }

    private LlmException badResponse(LlmRequest request, String message, Throwable cause) {
        LlmException ex = new LlmException(LlmException.Kind.BAD_RESPONSE, false, null, message, cause);
        logFailure(request, ex);
        return ex;
    }

    private static boolean isTimeout(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof TimeoutException
                    || t instanceof java.util.concurrent.CancellationException
                    || t.getClass().getName().contains("Timeout")) {
                return true;
            }
            String msg = t.getMessage();
            if (msg != null && msg.contains("Timeout on blocking read")) {
                // Reactor Mono.block(Duration) throws IllegalStateException on timeout.
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    // --- logging: technical fields only, never key/prompt/response (BR1.2) ---

    private void logSuccess(LlmRequest request, LlmResponse response) {
        log.info("LLM call ok purpose={} model={} latencyMs={} inputTokens={} outputTokens={} cachedInputTokens={} truncated={}",
                request.purpose(), response.model(), response.latencyMs(), response.inputTokens(),
                response.outputTokens(), response.cachedInputTokens(), response.truncated());
    }

    private void logFailure(LlmRequest request, LlmException ex) {
        log.warn("LLM call failed purpose={} kind={} retryable={} providerStatus={}",
                request.purpose(), ex.kind(), ex.isRetryable(), ex.providerStatus());
    }

    // --- helpers ---

    private static org.springframework.core.ParameterizedTypeReference<Map<String, Object>> mapType() {
        return new org.springframework.core.ParameterizedTypeReference<>() {
        };
    }

    private static int asInt(Object n) {
        return n instanceof Number num ? num.intValue() : 0;
    }

    private static String normalizeBase(String base) {
        if (base == null || base.isBlank()) {
            return "https://api.anthropic.com";
        }
        return base.replaceAll("/+$", "");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

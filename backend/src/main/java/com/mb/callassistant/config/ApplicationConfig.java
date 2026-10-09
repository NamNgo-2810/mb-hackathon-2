package com.mb.callassistant.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wires config properties, a shared WebClient for upstream HTTP, and CORS for
 * the local frontend.
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ApplicationConfig {

    private final AppProperties props;

    public ApplicationConfig(AppProperties props) {
        this.props = props;
    }

    /**
     * Shared non-blocking HTTP client used by RecordingService and the STT client.
     * The default codec in-memory limit is 256 KB, which is far smaller than a
     * call recording {@code .wav}; raise it so the whole file can be buffered
     * (configurable via {@code app.recording.max-download-bytes}).
     */
    @Bean
    public WebClient webClient() {
        int maxBytes = props.getRecording().getMaxDownloadBytes();
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxBytes))
                .build();
    }

    /** CORS so the browser frontend (served by the Node demo on :3000) can call this API. */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] origins = props.getCors().getAllowedOrigins().split("\\s*,\\s*");
                // C1/ADR-002: open PUT/PATCH and the Authorization header so the Node
                // frontend origin can reach the lead/call API in both demo and login mode.
                registry.addMapping("/api/**")
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}

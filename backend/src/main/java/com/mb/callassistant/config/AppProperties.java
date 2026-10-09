package com.mb.callassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for the {@code app.*} properties in application.yml.
 * Every value originates from an environment variable (NFR3) — see .env.example.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Recording recording = new Recording();
    private final Stt stt = new Stt();
    private final Cors cors = new Cors();
    private final Auth auth = new Auth();
    private final Llm llm = new Llm();
    private final Copilot copilot = new Copilot();
    private final Analysis analysis = new Analysis();

    public Recording getRecording() {
        return recording;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public Stt getStt() {
        return stt;
    }

    public Cors getCors() {
        return cors;
    }

    public Auth getAuth() {
        return auth;
    }

    public Llm getLlm() {
        return llm;
    }

    public Copilot getCopilot() {
        return copilot;
    }

    public static class Recording {
        /**
         * Base URL of the FreeSWITCH recordings server exposing
         * {@code /api/recordings/latest} and {@code /api/recordings/download/:name}.
         */
        private String baseUrl = "";
        /** Optional API key sent as {@code X-API-Key} to the recordings server. */
        private String apiKey = "";
        /** Bounded retries for a recording that may be a moment late after hangup (FR3.2). */
        private int retryMaxAttempts = 6;
        /** Initial delay between recording-download attempts; grows with simple backoff. */
        private long retryDelayMs = 1500;
        /**
         * Max bytes to buffer in memory when downloading a recording. WebClient's
         * default is 256 KB, far below a call {@code .wav}; default here is 64 MB.
         */
        private int maxDownloadBytes = 64 * 1024 * 1024;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getRetryMaxAttempts() {
            return retryMaxAttempts;
        }

        public void setRetryMaxAttempts(int retryMaxAttempts) {
            this.retryMaxAttempts = retryMaxAttempts;
        }

        public long getRetryDelayMs() {
            return retryDelayMs;
        }

        public void setRetryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
        }

        public int getMaxDownloadBytes() {
            return maxDownloadBytes;
        }

        public void setMaxDownloadBytes(int maxDownloadBytes) {
            this.maxDownloadBytes = maxDownloadBytes;
        }
    }

    public static class Stt {
        private String provider = "whisper";
        private String baseUrl = "";
        private String apiKey = "";
        /**
         * Which stereo channel carries the RM (agent): {@code left} or
         * {@code right}. The other channel is the KH (customer). Default
         * {@code left}=RM, {@code right}=KH; flip without a code change (FR4.2).
         */
        private String rmChannel = "left";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getRmChannel() {
            return rmChannel;
        }

        public void setRmChannel(String rmChannel) {
            this.rmChannel = rmChannel;
        }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    /**
     * Auth mode (ADR-007, BR1.3). {@code demo} (default) resolves a fixed sample
     * RM without any token; {@code login} resolves the RM from a Bearer token
     * (implemented by U6). U1 ships the demo provider only.
     */
    public static class Auth {
        private String mode = "demo";

        /**
         * Thời hạn phiên đăng nhập, tính bằng giờ (BR2.1). Mặc định 8 giờ, cấu
         * hình được qua {@code app.auth.session-ttl-hours} / env
         * {@code AUTH_SESSION_TTL_HOURS}. Chỉ dùng ở chế độ {@code login} (U6).
         */
        private int sessionTtlHours = 8;

        /**
         * (Tùy chọn, BR5.2) Độ trễ cố định (ms) cho phản hồi đăng nhập thất bại
         * để giảm nhẹ dò mật khẩu. Mặc định 0 (tắt). Cấu hình qua
         * {@code app.auth.fail-delay-ms} / env {@code AUTH_FAIL_DELAY_MS}.
         */
        private long failDelayMs = 0;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public int getSessionTtlHours() {
            return sessionTtlHours;
        }

        public void setSessionTtlHours(int sessionTtlHours) {
            this.sessionTtlHours = sessionTtlHours;
        }

        public long getFailDelayMs() {
            return failDelayMs;
        }

        public void setFailDelayMs(long failDelayMs) {
            this.failDelayMs = failDelayMs;
        }
    }

    /**
     * Shared LLM gateway configuration (U2, ADR-004, I3). Bound from the
     * {@code app.llm.*} properties. The API key is deliberately <b>not</b> a
     * field here: it is read directly from the {@code ANTHROPIC_API_KEY}
     * environment variable inside the adapter so it is never bound into a
     * commitable property (BR1.1 / NFR2.1).
     */
    public static class Llm {
        /** {@code anthropic} (default) or {@code mock} (BR6.1, NFR3.4). */
        private String provider = "anthropic";
        /** Claude model id; chosen by the operator when a key is set (NFR2.3). */
        private String model = "";
        /** Provider base URL; must be https for the real provider (NFR2.9). */
        private String baseUrl = "https://api.anthropic.com";
        /** Anthropic Messages API version header value. */
        private String anthropicVersion = "2023-06-01";
        /** Default per-call timeout in ms when the request omits one (BR3.1/NFR1.1). */
        private int defaultTimeoutMs = 30000;
        /** Upper bound for {@code LlmRequest.maxOutputTokens} (BR2.2). */
        private int maxOutputTokensCap = 4096;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getAnthropicVersion() {
            return anthropicVersion;
        }

        public void setAnthropicVersion(String anthropicVersion) {
            this.anthropicVersion = anthropicVersion;
        }

        public int getDefaultTimeoutMs() {
            return defaultTimeoutMs;
        }

        public void setDefaultTimeoutMs(int defaultTimeoutMs) {
            this.defaultTimeoutMs = defaultTimeoutMs;
        }

        public int getMaxOutputTokensCap() {
            return maxOutputTokensCap;
        }

        public void setMaxOutputTokensCap(int maxOutputTokensCap) {
            this.maxOutputTokensCap = maxOutputTokensCap;
        }
    }

    /**
     * Copilot (U3, hợp đồng C2) configuration, bound from {@code app.copilot.*}.
     * Documents are loaded once at startup from {@link #docsDir}; the search and
     * prompt parameters tune retrieval quality (ADR-003, FD §6).
     */
    public static class Copilot {
        /** Thư mục chứa file chính sách text/md đã trích (BR1.1). */
        private String docsDir = "backend/policy-docs";
        /** Số đoạn tối đa nhúng vào prompt (BR2.2). */
        private int topK = 5;
        /** Số lượt lịch sử tối đa nhận từ giao diện mỗi yêu cầu (BR6.2). */
        private int maxHistoryTurns = 10;
        /** Độ dài tối đa của câu hỏi (BR7.2). */
        private int maxQuestionLength = 1000;
        /** Timeout cho mỗi lần gọi LLM, ms (BR5.1). */
        private int llmTimeoutMs = 30000;
        /** maxOutputTokens truyền cho LlmClient (BR5.1). */
        private int maxOutputTokens = 1024;

        public String getDocsDir() {
            return docsDir;
        }

        public void setDocsDir(String docsDir) {
            this.docsDir = docsDir;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public int getMaxHistoryTurns() {
            return maxHistoryTurns;
        }

        public void setMaxHistoryTurns(int maxHistoryTurns) {
            this.maxHistoryTurns = maxHistoryTurns;
        }

        public int getMaxQuestionLength() {
            return maxQuestionLength;
        }

        public void setMaxQuestionLength(int maxQuestionLength) {
            this.maxQuestionLength = maxQuestionLength;
        }

        public int getLlmTimeoutMs() {
            return llmTimeoutMs;
        }

        public void setLlmTimeoutMs(int llmTimeoutMs) {
            this.llmTimeoutMs = llmTimeoutMs;
        }

        public int getMaxOutputTokens() {
            return maxOutputTokens;
        }

        public void setMaxOutputTokens(int maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }
    }

    /**
     * Phân tích & kết quả cuộc gọi (U4, bước ANALYZE của pipeline, I2/I3).
     * Bound từ {@code app.analysis.*}. Ngưỡng xếp hạng cấu hình được (BR3.3);
     * timeout gọi LLM (BR8.2/NFR1); maxOutputTokens cho phản hồi phân tích; và
     * tham số chờ ghi âm dự phòng khi chưa có transcript (AC2.4.1). Không có
     * trường khóa API ở đây — khóa đọc trực tiếp từ môi trường trong adapter
     * (NFR2.1).
     */
    public static class Analysis {
        /** Ngưỡng điểm tối thiểu cho hạng A (BR3.3). */
        private int gradeThresholdA = 85;
        /** Ngưỡng điểm tối thiểu cho hạng B (BR3.3). */
        private int gradeThresholdB = 70;
        /** Ngưỡng điểm tối thiểu cho hạng C (BR3.3); dưới ngưỡng này là D. */
        private int gradeThresholdC = 50;
        /** Timeout mỗi lần gọi LLM phân tích, ms (BR8.2/NFR1). */
        private int llmTimeoutMs = 30000;
        /** maxOutputTokens truyền cho LlmClient khi sinh kết quả phân tích. */
        private int maxOutputTokens = 2048;
        /** Số lần chờ ghi âm/transcript tối đa khi dự phòng NO_RECORDING (AC2.4.1). */
        private int recordingWaitMaxAttempts = 6;
        /** Độ trễ giữa các lần chờ ghi âm/transcript, ms (AC2.4.1). */
        private long recordingWaitMs = 1500;

        public int getGradeThresholdA() {
            return gradeThresholdA;
        }

        public void setGradeThresholdA(int gradeThresholdA) {
            this.gradeThresholdA = gradeThresholdA;
        }

        public int getGradeThresholdB() {
            return gradeThresholdB;
        }

        public void setGradeThresholdB(int gradeThresholdB) {
            this.gradeThresholdB = gradeThresholdB;
        }

        public int getGradeThresholdC() {
            return gradeThresholdC;
        }

        public void setGradeThresholdC(int gradeThresholdC) {
            this.gradeThresholdC = gradeThresholdC;
        }

        public int getLlmTimeoutMs() {
            return llmTimeoutMs;
        }

        public void setLlmTimeoutMs(int llmTimeoutMs) {
            this.llmTimeoutMs = llmTimeoutMs;
        }

        public int getMaxOutputTokens() {
            return maxOutputTokens;
        }

        public void setMaxOutputTokens(int maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }

        public int getRecordingWaitMaxAttempts() {
            return recordingWaitMaxAttempts;
        }

        public void setRecordingWaitMaxAttempts(int recordingWaitMaxAttempts) {
            this.recordingWaitMaxAttempts = recordingWaitMaxAttempts;
        }

        public long getRecordingWaitMs() {
            return recordingWaitMs;
        }

        public void setRecordingWaitMs(long recordingWaitMs) {
            this.recordingWaitMs = recordingWaitMs;
        }
    }
}

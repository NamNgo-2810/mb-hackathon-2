package com.mb.callassistant.copilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mb.callassistant.config.AppProperties;
import com.mb.callassistant.lead.CurrentRmProvider;
import com.mb.callassistant.llm.LlmClient;
import com.mb.callassistant.llm.LlmException;
import com.mb.callassistant.llm.LlmPurpose;
import com.mb.callassistant.llm.LlmRequest;
import com.mb.callassistant.llm.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Điều phối luồng hỏi–đáp Copilot (WF2). Validate biên (BR7.2), xác định RM
 * (I1), chuẩn hoá câu hỏi (BR2.1), tìm đoạn (BR2.2); nhánh rỗng ⇒ câu cố định,
 * KHÔNG gọi LLM (BR3.1); nhánh có ngữ cảnh ⇒ gọi {@link LlmClient} expectJson,
 * timeout 30 giây (BR5.1), bắt {@link LlmException} ⇒ {@link AiException} (BR5.2),
 * JSON sai cấu trúc ⇒ BAD_RESPONSE (BR5.3); kiểm chứng trích dẫn (BR4.2); ghi
 * đúng một {@link CopilotQueryLog} cho mọi nhánh kể cả lỗi (BR7.1); log không
 * chứa khoá/nội dung (BR7.3).
 */
@Service
public class CopilotService {

    private static final Logger log = LoggerFactory.getLogger(CopilotService.class);
    private static final ZoneOffset VN_OFFSET = ZoneOffset.ofHours(7);

    private final AppProperties props;
    private final CurrentRmProvider currentRmProvider;
    private final PolicyDocumentRepository documents;
    private final CopilotQueryLogRepository queryLogs;
    private final ChunkSearcher chunkSearcher;
    private final CopilotPromptBuilder promptBuilder;
    private final CitationVerifier citationVerifier;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public CopilotService(AppProperties props, CurrentRmProvider currentRmProvider,
                          PolicyDocumentRepository documents, CopilotQueryLogRepository queryLogs,
                          ChunkSearcher chunkSearcher, CopilotPromptBuilder promptBuilder,
                          CitationVerifier citationVerifier, LlmClient llmClient,
                          ObjectMapper objectMapper) {
        this.props = props;
        this.currentRmProvider = currentRmProvider;
        this.documents = documents;
        this.queryLogs = queryLogs;
        this.chunkSearcher = chunkSearcher;
        this.promptBuilder = promptBuilder;
        this.citationVerifier = citationVerifier;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Trả lời một câu hỏi (WF2).
     *
     * @param question câu hỏi (đã qua validate biên của controller, nhưng dịch vụ
     *                 vẫn tự bảo vệ)
     * @param callId   cuộc gọi đang diễn ra nếu có (tuỳ chọn)
     * @param history  lịch sử phiên (≤ maxHistoryTurns, giao diện giữ)
     * @return câu trả lời đã kiểm chứng
     * @throws AiException khi LLM lỗi/quá giờ/JSON sai (BR5.2, BR5.3 → 502)
     */
    public CopilotAnswer ask(String question, String callId, List<CopilotPromptBuilder.HistoryTurn> history) {
        AppProperties.Copilot cfg = props.getCopilot();
        validate(question, history, cfg);

        String rmId = currentRmProvider.currentRmId();
        List<String> tokens = TextNormalizer.tokens(question);
        List<RetrievedChunk> retrieved =
                chunkSearcher.search(documents.allChunks(), tokens, cfg.getTopK());

        // Nhánh rỗng: không gọi LLM (BR3.1).
        if (retrieved.isEmpty()) {
            CopilotAnswer answer = CopilotAnswer.notFound();
            writeLog(rmId, callId, question, answer);
            log.info("Copilot: khong tim thay doan khop — tra loi khong-tim-thay, khong goi LLM (rmId={}, doan=0)", rmId);
            return answer;
        }

        // Nhánh có ngữ cảnh: gọi LLM (BR5.1).
        String systemPrompt = promptBuilder.systemPrompt();
        String userContent = promptBuilder.userContent(question, retrieved, documents, history);
        LlmRequest request = new LlmRequest(
                systemPrompt, null, userContent, cfg.getMaxOutputTokens(),
                cfg.getLlmTimeoutMs(), true, LlmPurpose.COPILOT);

        LlmResponse response;
        try {
            response = llmClient.complete(request);
        } catch (LlmException e) {
            log.warn("Copilot: LLM loi kind={} — tra 502 AI_ERROR (rmId={}, doan={})",
                    e.kind(), rmId, retrieved.size());
            writeLog(rmId, callId, question, CopilotAnswer.notFound());
            throw toAiException(e);
        }

        CopilotAnswer answer = parseAndVerify(response, retrieved, rmId, callId, question);
        return answer;
    }

    private CopilotAnswer parseAndVerify(LlmResponse response, List<RetrievedChunk> retrieved,
                                         String rmId, String callId, String question) {
        JsonNode root = readJson(response.text());
        // JSON sai cấu trúc: thiếu answerFound/answer ⇒ BAD_RESPONSE (BR5.3).
        if (root == null || !root.isObject() || !root.hasNonNull("answerFound") || !root.has("answer")) {
            log.warn("Copilot: JSON LLM sai cau truc — xu ly nhu BAD_RESPONSE (rmId={})", rmId);
            writeLog(rmId, callId, question, CopilotAnswer.notFound());
            throw new AiException(LlmException.Kind.BAD_RESPONSE,
                    "Trợ lý AI trả về dữ liệu không đúng định dạng. Vui lòng thử lại.");
        }

        boolean answerFound = root.get("answerFound").asBoolean(false);
        String answerText = root.path("answer").asText("");
        List<CitationVerifier.RawCitation> rawCitations = parseCitations(root.path("citations"));
        List<String> rawSuggestions = parseStringArray(root.path("suggestions"));

        CopilotAnswer answer = citationVerifier.verify(
                answerFound, answerText, rawCitations, rawSuggestions, retrieved, documents);
        writeLog(rmId, callId, question, answer);
        log.info("Copilot: tra loi xong (rmId={}, answerFound={}, so_trich_dan={})",
                rmId, answer.answerFound(), answer.citations().size());
        return answer;
    }

    private void validate(String question, List<CopilotPromptBuilder.HistoryTurn> history,
                          AppProperties.Copilot cfg) {
        if (question == null || question.isBlank()) {
            throw new CopilotValidationException("question", "Câu hỏi không được để trống.");
        }
        if (question.length() > cfg.getMaxQuestionLength()) {
            throw new CopilotValidationException("question",
                    "Câu hỏi quá dài (tối đa " + cfg.getMaxQuestionLength() + " ký tự).");
        }
        if (history != null && history.size() > cfg.getMaxHistoryTurns()) {
            throw new CopilotValidationException("history",
                    "Lịch sử hội thoại vượt quá " + cfg.getMaxHistoryTurns() + " lượt.");
        }
    }

    private void writeLog(String rmId, String callId, String question, CopilotAnswer answer) {
        List<String> citedIds = new ArrayList<>();
        if (answer.answerFound()) {
            for (Citation c : answer.citations()) {
                if (c.documentId() != null && !citedIds.contains(c.documentId())) {
                    citedIds.add(c.documentId());
                }
            }
        }
        CopilotQueryLog entry = new CopilotQueryLog(
                UUID.randomUUID().toString(), rmId, callId, question,
                answer.answerFound(), citedIds, OffsetDateTime.now(VN_OFFSET));
        queryLogs.save(entry);
    }

    private AiException toAiException(LlmException e) {
        String message = switch (e.kind()) {
            case NOT_CONFIGURED -> "Trợ lý AI chưa được cấu hình. Vui lòng liên hệ quản trị.";
            case TIMEOUT -> "Trợ lý AI phản hồi quá chậm. Vui lòng thử lại.";
            case RATE_LIMITED -> "Trợ lý AI đang quá tải. Vui lòng thử lại sau giây lát.";
            case UNAVAILABLE -> "Không kết nối được trợ lý AI. Vui lòng thử lại.";
            case BAD_RESPONSE -> "Trợ lý AI trả về dữ liệu không hợp lệ. Vui lòng thử lại.";
        };
        return new AiException(e.kind(), message, e);
    }

    private JsonNode readJson(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String candidate = stripFence(text.strip());
        try {
            return objectMapper.readTree(candidate);
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            return null;
        }
    }

    /** Gỡ hàng rào ```` ```json ... ``` ```` quanh JSON nếu có. */
    private String stripFence(String text) {
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNewline = text.indexOf('\n');
        if (firstNewline < 0) {
            return text;
        }
        String firstLine = text.substring(3, firstNewline).trim();
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

    private List<CitationVerifier.RawCitation> parseCitations(JsonNode node) {
        List<CitationVerifier.RawCitation> out = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return out;
        }
        for (JsonNode item : node) {
            String documentId = item.path("documentId").isMissingNode() || item.path("documentId").isNull()
                    ? null : item.path("documentId").asText(null);
            String documentTitle = item.path("documentTitle").asText(null);
            Integer page = item.path("page").isIntegralNumber() ? item.path("page").asInt() : null;
            String section = item.path("section").isMissingNode() || item.path("section").isNull()
                    ? null : item.path("section").asText(null);
            LocalDate effective = parseDate(item.path("effectiveDate").asText(null));
            out.add(new CitationVerifier.RawCitation(documentId, documentTitle, page, section, effective));
        }
        return out;
    }

    private List<String> parseStringArray(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return out;
        }
        for (JsonNode item : node) {
            if (item.isTextual()) {
                out.add(item.asText());
            }
        }
        return out;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Số lượt hỏi đã ghi (OKR, dùng khi cần hiển thị/thống kê). */
    public long queryCount() {
        return queryLogs.count();
    }
}

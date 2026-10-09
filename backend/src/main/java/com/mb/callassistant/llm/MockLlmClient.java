package com.mb.callassistant.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mb.callassistant.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * {@link LlmClient} that returns canned results without any network call
 * (BR6.1 / NFR3.4). Selected when {@code app.llm.provider=mock}, so a demo or a
 * test can run with no {@code ANTHROPIC_API_KEY} and no connectivity.
 *
 * <p>It still runs the same boundary validation as the real adapter (BR2.2) and
 * the same {@code expectJson} check (BR4.2), so callers behave identically. A
 * canned {@link LlmException} can be installed to exercise a caller's fallback
 * path.
 */
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "mock")
public class MockLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(MockLlmClient.class);

    private final AppProperties props;
    private final ObjectMapper objectMapper;
    private final Map<LlmPurpose, String> cannedByPurpose = new EnumMap<>(LlmPurpose.class);
    private volatile LlmException cannedError;
    private volatile String model = "mock-claude";

    public MockLlmClient(AppProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        // Canned JSON HỢP LỆ theo schema của từng bên tiêu thụ, để demo offline
        // (profile demo, provider=mock) chạy end-to-end (BR6.1 / NFR3.4).
        cannedByPurpose.put(LlmPurpose.COPILOT, CANNED_COPILOT_JSON);
        cannedByPurpose.put(LlmPurpose.CALL_ANALYSIS, CANNED_CALL_ANALYSIS_JSON);
    }

    /**
     * COPILOT: khớp các trường {@code CopilotService} parse — {@code answerFound},
     * {@code answer}, {@code citations[{documentId, documentTitle, page, section,
     * effectiveDate}]}, {@code suggestions[]}. Trích dẫn trỏ tới tài liệu seed
     * {@code backend/policy-docs/bieu-lai-suat-mau.md} (documentId suy từ tên file)
     * trang 1, ngày hiệu lực khớp metadata, để {@code CitationVerifier} giữ lại.
     */
    private static final String CANNED_COPILOT_JSON = """
            {
              "answerFound": true,
              "answer": "Khách hàng ưu tiên (VIP) gửi tiết kiệm kỳ hạn 12 tháng được hưởng lãi suất 5,8%/năm, điều kiện số tiền gửi tối thiểu 500 triệu đồng (dữ liệu mẫu demo).",
              "citations": [
                {
                  "documentId": "bieu-lai-suat-mau",
                  "documentTitle": "Biểu lãi suất tiền gửi KHCN (MẪU DEMO)",
                  "page": 1,
                  "section": null,
                  "effectiveDate": "2026-09-01"
                }
              ],
              "suggestions": [
                "Điều kiện áp dụng lãi suất VIP là gì?",
                "Lãi suất kỳ hạn 24 tháng cho khách VIP?"
              ]
            }
            """;

    /**
     * CALL_ANALYSIS: khớp schema U4 — {@code summary}, {@code suggestions[]},
     * {@code criteriaScores} đúng 5 mã, {@code tone}, insight, {@code suggestedKeyin}
     * (vài trường kèm {@code sourceMs}), {@code suggestedTasks[]} (mỗi task có
     * {@code clientKey}). KHÔNG kèm trường tổng (server tự tính, BR3.2).
     */
    private static final String CANNED_CALL_ANALYSIS_JSON = """
            {
              "summary": "RM Yến tư vấn khách VIP gửi tiết kiệm 500 triệu, kỳ hạn 12 tháng, lãi suất 5,8%/năm; khách đồng ý mở trên App vào thứ Hai.",
              "suggestions": [
                "Gửi hướng dẫn mở sổ qua Zalo/SMS",
                "Nhắc nạp tiền sáng thứ Hai"
              ],
              "criteriaScores": [
                { "code": "CHAO_HOI", "name": "Chào hỏi & Nhận diện KH", "score": 10, "evidence": "Chào đúng tên khách" },
                { "code": "KHAI_THAC_NHU_CAU", "name": "Khai thác nhu cầu & Số tiền", "score": 10, "evidence": "Xác định 500 triệu, 12 tháng" },
                { "code": "CHINH_SACH_LAI_SUAT", "name": "Nắm vững chính sách & Lãi suất", "score": 9.5, "evidence": "Nêu đúng 5,8%/năm VIP" },
                { "code": "XU_LY_THAC_MAC", "name": "Xử lý thắc mắc & Thuyết phục", "score": 9, "evidence": "Giải đáp điều kiện VIP" },
                { "code": "CHOT_CAM_KET", "name": "Chốt cam kết & Next Step", "score": 9.5, "evidence": "Hẹn mở App thứ Hai" }
              ],
              "tone": "TICH_CUC",
              "insightStrength": "RM nắm rõ chính sách VIP và hướng dẫn mở App thành công.",
              "insightImprovement": "Có thể cross-sell thêm thẻ tín dụng hoàn tiền khi khách duy trì số dư.",
              "suggestedKeyin": {
                "demandAmount": { "value": "500000000", "sourceMs": 45000 },
                "term": { "value": "12 tháng", "sourceMs": 45000 },
                "agreedRate": { "value": "5.80", "sourceMs": 80000 },
                "product": { "value": "Tiết kiệm dài hạn VIP - 12T", "sourceMs": 80000 },
                "channel": { "value": "App MBBank", "sourceMs": 170000 }
              },
              "suggestedTasks": [
                { "clientKey": "mock-task-1", "title": "Gửi SMS/Zalo hướng dẫn mở sổ trên App", "type": "TASK_ZALO", "priority": "CAO", "aiSuggested": true },
                { "clientKey": "mock-task-2", "title": "Gọi nhắc nạp tiền sáng thứ Hai", "type": "NHAC_GOI", "priority": "THUONG", "aiSuggested": true }
              ]
            }
            """;

    /** Install the canned text for a purpose (tests/demo). */
    public void setCanned(LlmPurpose purpose, String text) {
        cannedByPurpose.put(purpose, text);
    }

    /** Install a canned error so the next call throws it (tests). */
    public void setCannedError(LlmException error) {
        this.cannedError = error;
    }

    /** Clear any installed canned error. */
    public void clearCannedError() {
        this.cannedError = null;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        // Same boundary validation as the real adapter (BR2.2).
        LlmRequestValidator.validate(request, props.getLlm().getMaxOutputTokensCap());

        LlmException error = this.cannedError;
        if (error != null) {
            log.warn("LLM MOCK returning canned error purpose={} kind={}", request.purpose(), error.kind());
            throw error;
        }

        String text = cannedByPurpose.getOrDefault(request.purpose(), "Phản hồi giả lập (MOCK).");

        if (request.expectJson() && !JsonResponses.isJsonObject(objectMapper, text)) {
            throw new LlmException(LlmException.Kind.BAD_RESPONSE, false, null,
                    "mock canned text for purpose " + request.purpose() + " is not a JSON object");
        }

        log.info("LLM MOCK call ok purpose={} model={}", request.purpose(), model);
        return new LlmResponse(text, model, 0, 0, 0, 0L, false);
    }
}

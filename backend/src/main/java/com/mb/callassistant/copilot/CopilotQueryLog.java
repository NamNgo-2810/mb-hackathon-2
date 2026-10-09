package com.mb.callassistant.copilot;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Nhật ký mỗi lượt hỏi Copilot, phục vụ đếm lượt dùng cho OKR (entities.md
 * {@code CopilotQueryLog}, BR7.1). Không lưu toàn văn câu trả lời; chỉ lưu đủ để
 * đếm và truy vết (BR7.3).
 *
 * <p>Lưu trong bộ nhớ (không phải thực thể JPA) — demo chỉ cần đếm lượt.
 */
public class CopilotQueryLog {

    private final String queryId;
    private final String rmId;
    private final String callId;
    private final String question;
    private final boolean answerFound;
    private final List<String> citedDocumentIds;
    private final OffsetDateTime askedAt;

    public CopilotQueryLog(String queryId, String rmId, String callId, String question,
                           boolean answerFound, List<String> citedDocumentIds,
                           OffsetDateTime askedAt) {
        this.queryId = queryId;
        this.rmId = rmId;
        this.callId = callId;
        this.question = question;
        this.answerFound = answerFound;
        this.citedDocumentIds = citedDocumentIds == null ? List.of() : List.copyOf(citedDocumentIds);
        this.askedAt = askedAt;
    }

    public String getQueryId() {
        return queryId;
    }

    public String getRmId() {
        return rmId;
    }

    public String getCallId() {
        return callId;
    }

    public String getQuestion() {
        return question;
    }

    public boolean isAnswerFound() {
        return answerFound;
    }

    public List<String> getCitedDocumentIds() {
        return citedDocumentIds;
    }

    public OffsetDateTime getAskedAt() {
        return askedAt;
    }
}

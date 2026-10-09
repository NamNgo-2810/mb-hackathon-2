package com.mb.callassistant.copilot;

import java.util.List;

/**
 * Kết quả trả về cho một câu hỏi (entities.md {@code CopilotAnswer}, khớp schema
 * C2): trạng thái tìm thấy, nội dung trả lời, trích dẫn đã kiểm chứng, và 2–4
 * câu hỏi gợi ý.
 *
 * <p>Ràng buộc (BR3.1/BR3.3): {@code answerFound=false} ⇒ {@code answer} là câu
 * không-tìm-thấy cố định AND {@code citations} rỗng.
 */
public record CopilotAnswer(
        boolean answerFound,
        String answer,
        List<Citation> citations,
        List<String> suggestions) {

    public CopilotAnswer {
        citations = citations == null ? List.of() : List.copyOf(citations);
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
        if (answer == null) {
            throw new IllegalArgumentException("answer là bắt buộc");
        }
        if (!answerFound && !citations.isEmpty()) {
            throw new IllegalArgumentException(
                    "answerFound=false thì citations phải rỗng (BR3.1)");
        }
    }

    /** Câu trả lời "không tìm thấy" cố định (BR3.1, BR3.3). */
    public static CopilotAnswer notFound() {
        return new CopilotAnswer(false, CopilotMessages.NOT_FOUND, List.of(), List.of());
    }

    /** Câu trả lời có nội dung đã kiểm chứng (BR4.1, BR6.1). */
    public static CopilotAnswer found(String answer, List<Citation> citations, List<String> suggestions) {
        return new CopilotAnswer(true, answer, citations, suggestions);
    }
}

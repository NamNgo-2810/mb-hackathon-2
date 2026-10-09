package com.mb.callassistant.copilot;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Kiểm chứng trích dẫn LLM trả về với các đoạn đã gửi (BR4.2) và hoàn thiện câu
 * trả lời (BR3.2, BR4.1, BR6.1). Giữ một citation chỉ khi nó map được tới một
 * {@link RetrievedChunk} đã nằm trong ngữ cảnh (cùng documentId và trang/mục);
 * loại cái không khớp; sau lọc rỗng ⇒ ép {@code answerFound=false}.
 */
@Component
public class CitationVerifier {

    /** Một citation thô do LLM đề xuất (trước kiểm chứng). */
    public record RawCitation(String documentId, String documentTitle, Integer page,
                              String section, LocalDate effectiveDate) {
    }

    /**
     * Hoàn thiện câu trả lời đã kiểm chứng.
     *
     * @param answerFound  cờ LLM trả về
     * @param answerText   nội dung LLM trả về
     * @param rawCitations trích dẫn thô LLM đề xuất
     * @param rawSuggestions gợi ý thô LLM đề xuất
     * @param retrieved    các đoạn đã gửi (ngữ cảnh)
     * @param documents    chỉ mục để điền title/effectiveDate chuẩn
     * @return {@link CopilotAnswer} đã kiểm chứng (không-tìm-thấy khi không còn trích dẫn hợp lệ)
     */
    public CopilotAnswer verify(boolean answerFound, String answerText,
                                List<RawCitation> rawCitations, List<String> rawSuggestions,
                                List<RetrievedChunk> retrieved, PolicyDocumentRepository documents) {
        if (!answerFound) {
            return CopilotAnswer.notFound();
        }

        List<Citation> verified = new ArrayList<>();
        if (rawCitations != null) {
            for (RawCitation raw : rawCitations) {
                RetrievedChunk match = matchRetrieved(raw, retrieved);
                if (match == null) {
                    continue; // trích dẫn bịa — loại (BR4.2)
                }
                PolicyDocument doc = documents.findById(match.documentId()).orElse(null);
                String title = doc != null ? doc.getTitle()
                        : (raw.documentTitle() != null ? raw.documentTitle() : match.documentId());
                LocalDate effective = doc != null ? doc.getEffectiveDate() : raw.effectiveDate();
                Integer page = match.page();
                String section = match.section();
                if (effective == null || (page == null && (section == null || section.isBlank()))) {
                    continue; // thiếu dữ liệu bắt buộc cho Citation (BR4.1)
                }
                verified.add(new Citation(match.documentId(), title, page, section, effective));
            }
        }

        if (verified.isEmpty()) {
            // Không còn trích dẫn hợp lệ ⇒ ép không-tìm-thấy (BR3.2, BR4.2).
            return CopilotAnswer.notFound();
        }

        List<String> suggestions = trimSuggestions(rawSuggestions);
        String answer = answerText == null || answerText.isBlank()
                ? "(Không có nội dung trả lời)" : answerText;
        return CopilotAnswer.found(answer, dedupe(verified), suggestions);
    }

    /** Khớp theo documentId và trang/mục với một đoạn đã gửi. */
    private RetrievedChunk matchRetrieved(RawCitation raw, List<RetrievedChunk> retrieved) {
        if (raw == null || retrieved == null) {
            return null;
        }
        for (RetrievedChunk chunk : retrieved) {
            if (!matchesDocument(raw, chunk)) {
                continue;
            }
            boolean pageOk = raw.page() != null && raw.page().equals(chunk.page());
            boolean sectionOk = raw.section() != null && chunk.section() != null
                    && raw.section().trim().equalsIgnoreCase(chunk.section().trim());
            // Nếu LLM không nêu page/section cụ thể, chấp nhận khi documentId khớp.
            boolean noLocator = raw.page() == null && (raw.section() == null || raw.section().isBlank());
            if (pageOk || sectionOk || noLocator) {
                return chunk;
            }
        }
        return null;
    }

    private boolean matchesDocument(RawCitation raw, RetrievedChunk chunk) {
        return raw.documentId() != null && raw.documentId().equals(chunk.documentId());
    }

    /** Cắt gợi ý còn 2–4; nếu < 2 giữ nguyên (câu trả lời vẫn hợp lệ nhưng thiếu gợi ý). */
    private List<String> trimSuggestions(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String s : raw) {
            if (s != null && !s.isBlank()) {
                cleaned.add(s.trim());
            }
        }
        if (cleaned.size() > 4) {
            return new ArrayList<>(cleaned.subList(0, 4));
        }
        return cleaned;
    }

    private List<Citation> dedupe(List<Citation> citations) {
        List<Citation> out = new ArrayList<>();
        for (Citation c : citations) {
            boolean dup = out.stream().anyMatch(e ->
                    java.util.Objects.equals(e.documentId(), c.documentId())
                            && java.util.Objects.equals(e.page(), c.page())
                            && java.util.Objects.equals(e.section(), c.section()));
            if (!dup) {
                out.add(c);
            }
        }
        return out;
    }
}

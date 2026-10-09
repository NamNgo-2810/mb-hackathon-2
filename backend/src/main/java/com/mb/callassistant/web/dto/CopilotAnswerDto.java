package com.mb.callassistant.web.dto;

import com.mb.callassistant.copilot.CopilotAnswer;

import java.util.List;

/**
 * DTO câu trả lời Copilot (hợp đồng C2). Khi {@code answerFound=false},
 * {@code answer} là câu không-tìm-thấy cố định và {@code citations} rỗng.
 */
public record CopilotAnswerDto(
        boolean answerFound,
        String answer,
        List<CitationDto> citations,
        List<String> suggestions) {

    public static CopilotAnswerDto from(CopilotAnswer a) {
        return new CopilotAnswerDto(
                a.answerFound(),
                a.answer(),
                a.citations().stream().map(CitationDto::from).toList(),
                a.suggestions());
    }
}

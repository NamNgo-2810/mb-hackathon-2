package com.mb.callassistant.web;

import com.mb.callassistant.copilot.CopilotAnswer;
import com.mb.callassistant.copilot.CopilotPromptBuilder;
import com.mb.callassistant.copilot.CopilotService;
import com.mb.callassistant.copilot.PolicyDocumentRepository;
import com.mb.callassistant.web.dto.CopilotAnswerDto;
import com.mb.callassistant.web.dto.CopilotAskRequest;
import com.mb.callassistant.web.dto.CopilotDocumentDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Copilot API (hợp đồng C2): hỏi chính sách và liệt kê tài liệu đã nạp.
 *
 * <ul>
 *   <li>{@code POST /api/copilot/ask} — 200 {@link CopilotAnswerDto}; 400
 *       VALIDATION_FAILED khi biên sai; 502 AI_ERROR khi LLM lỗi (qua
 *       {@link ApiExceptionHandler}).</li>
 *   <li>{@code GET /api/copilot/documents} — danh sách tài liệu đã nạp.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/copilot")
public class CopilotController {

    private final CopilotService copilotService;
    private final PolicyDocumentRepository documents;

    public CopilotController(CopilotService copilotService, PolicyDocumentRepository documents) {
        this.copilotService = copilotService;
        this.documents = documents;
    }

    /** Hỏi chính sách — chỉ trả lời từ tài liệu (FR3.2, FR3.3). */
    @PostMapping("/ask")
    public CopilotAnswerDto ask(@Valid @RequestBody CopilotAskRequest request) {
        List<CopilotPromptBuilder.HistoryTurn> history = new ArrayList<>();
        if (request.history() != null) {
            for (CopilotAskRequest.HistoryTurnDto turn : request.history()) {
                history.add(new CopilotPromptBuilder.HistoryTurn(turn.role(), turn.text()));
            }
        }
        CopilotAnswer answer = copilotService.ask(request.question(), request.callId(), history);
        return CopilotAnswerDto.from(answer);
    }

    /** Danh sách tài liệu đã nạp (chân cột lịch sử tra cứu). */
    @GetMapping("/documents")
    public List<CopilotDocumentDto> documents() {
        return documents.findAll().stream().map(CopilotDocumentDto::from).toList();
    }
}

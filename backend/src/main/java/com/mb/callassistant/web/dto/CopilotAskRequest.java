package com.mb.callassistant.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request của {@code POST /api/copilot/ask} (hợp đồng C2). {@code question} bắt
 * buộc, 1–1000 ký tự; {@code callId} tuỳ chọn; {@code history} ≤ 10 lượt (ngữ
 * cảnh phiên do giao diện giữ; rỗng = cuộc trò chuyện mới, BR6.2).
 */
public record CopilotAskRequest(
        @NotBlank(message = "Câu hỏi không được để trống.")
        @Size(max = 1000, message = "Câu hỏi quá dài (tối đa 1000 ký tự).")
        String question,
        String callId,
        @Size(max = 10, message = "Lịch sử hội thoại vượt quá 10 lượt.")
        List<HistoryTurnDto> history) {

    /** Một lượt lịch sử: {@code role} (user|assistant) và {@code text}. */
    public record HistoryTurnDto(String role, String text) {
    }
}

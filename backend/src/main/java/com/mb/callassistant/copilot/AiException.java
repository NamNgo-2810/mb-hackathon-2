package com.mb.callassistant.copilot;

import com.mb.callassistant.llm.LlmException;

/**
 * Lỗi AI của Copilot, ánh xạ sang {@code 502 AI_ERROR} ở tầng web (BR5.2). Bọc
 * {@link LlmException.Kind} để tầng web/kiểm thử phân biệt {@code NOT_CONFIGURED}
 * với các loại lỗi khác. Thông điệp luôn tiếng Việt, không lộ chi tiết nội bộ
 * và không chứa khoá/nội dung tài liệu (BR7.3).
 */
public class AiException extends RuntimeException {

    private final LlmException.Kind kind;

    public AiException(LlmException.Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public AiException(LlmException.Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public LlmException.Kind kind() {
        return kind;
    }

    /** {@code true} khi nguyên nhân là chưa cấu hình khoá LLM (NOT_CONFIGURED). */
    public boolean isNotConfigured() {
        return kind == LlmException.Kind.NOT_CONFIGURED;
    }
}

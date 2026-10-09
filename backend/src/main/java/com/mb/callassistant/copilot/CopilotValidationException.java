package com.mb.callassistant.copilot;

/**
 * 400 VALIDATION_FAILED cho đầu vào Copilot ở biên (BR7.2). {@code field} là tên
 * trường lỗi (ví dụ {@code question}, {@code history}).
 */
public class CopilotValidationException extends RuntimeException {

    private final String field;

    public CopilotValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}

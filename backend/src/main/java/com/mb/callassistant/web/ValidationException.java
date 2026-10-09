package com.mb.callassistant.web;

/**
 * 400 VALIDATION_FAILED (BR3.1, BR6.3): dữ liệu nhập ở biên không hợp lệ.
 * {@code field} là tên trường lỗi (ví dụ {@code phoneNumber}, {@code leadStage}).
 */
public class ValidationException extends RuntimeException {

    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}

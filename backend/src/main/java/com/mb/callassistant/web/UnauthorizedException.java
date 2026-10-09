package com.mb.callassistant.web;

/**
 * 401 UNAUTHORIZED — request ở chế độ login thiếu/sai/hết hạn token (BR2.2), hoặc
 * đăng nhập sai thông tin (BR1.2). Thông điệp đã ở tiếng Việt; biên REST ánh xạ
 * sang envelope {@code UNAUTHORIZED} trong {@link ApiExceptionHandler}.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}

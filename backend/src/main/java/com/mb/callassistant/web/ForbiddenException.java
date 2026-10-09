package com.mb.callassistant.web;

/**
 * 403 FORBIDDEN (BR1.1): tài nguyên không thuộc RM hiện tại. Không tiết lộ tài
 * nguyên có tồn tại hay không.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}

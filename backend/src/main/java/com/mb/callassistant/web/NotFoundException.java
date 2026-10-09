package com.mb.callassistant.web;

/** 404 NOT_FOUND (BR1.2): tài nguyên của chính RM nhưng không tồn tại. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}

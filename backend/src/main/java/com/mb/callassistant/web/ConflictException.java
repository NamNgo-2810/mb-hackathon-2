package com.mb.callassistant.web;

/** 409 CONFLICT (BR3.2): đã có một cuộc gọi khác chưa xử lý xong. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

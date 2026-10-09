package com.mb.callassistant.web;

import com.mb.callassistant.copilot.AiException;
import com.mb.callassistant.copilot.CopilotValidationException;
import com.mb.callassistant.service.RecordingNotReadyException;
import com.mb.callassistant.service.SttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Translates exceptions into a consistent JSON error envelope at the API
 * boundary. Errors are surfaced, never swallowed (phase guardrails). The
 * envelope deliberately omits internal stack traces.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> onValidation(MethodArgumentNotValidException e) {
        List<Map<String, String>> details = new ArrayList<>();
        e.getBindingResult().getFieldErrors().forEach(fe ->
                details.add(Map.of("field", fe.getField(),
                        "reason", fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage())));
        return envelope(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "One or more fields failed validation.", details);
    }

    @ExceptionHandler(RecordingNotReadyException.class)
    public ResponseEntity<Map<String, Object>> onRecordingNotReady(RecordingNotReadyException e) {
        // Not-ready/not-found is a 404 so the frontend can keep polling (FR3.2).
        return envelope(HttpStatus.NOT_FOUND, "RECORDING_NOT_READY", e.getMessage(), null);
    }

    /** 403 FORBIDDEN — resource not owned by the current RM (BR1.1). */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> onForbidden(ForbiddenException e) {
        return envelope(HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage(), null);
    }

    /**
     * 401 UNAUTHORIZED — chế độ login: thiếu/sai/hết hạn token hoặc đăng nhập
     * sai thông tin (BR1.2, BR2.2). Thông điệp đã tiếng Việt; không lộ trường nào
     * sai và không log token (BR5.1).
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> onUnauthorized(UnauthorizedException e) {
        return envelope(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage(), null);
    }

    /** 404 NOT_FOUND — the current RM's own resource does not exist (BR1.2). */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> onNotFound(NotFoundException e) {
        return envelope(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), null);
    }

    /** 400 VALIDATION_FAILED — boundary input invalid (BR3.1, BR6.3). */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> onValidationDomain(ValidationException e) {
        List<Map<String, String>> details = null;
        if (e.getField() != null) {
            details = new ArrayList<>();
            details.add(Map.of("field", e.getField(),
                    "reason", e.getMessage() == null ? "invalid" : e.getMessage()));
        }
        return envelope(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                e.getMessage() == null ? "Dữ liệu không hợp lệ." : e.getMessage(), details);
    }

    /** 409 CONFLICT — one in-flight call at a time (BR3.2). */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> onConflict(ConflictException e) {
        return envelope(HttpStatus.CONFLICT, "CONFLICT", e.getMessage(), null);
    }

    @ExceptionHandler(SttException.class)
    public ResponseEntity<Map<String, Object>> onStt(SttException e) {
        log.warn("STT error surfaced to client: {}", e.getMessage());
        return envelope(HttpStatus.BAD_GATEWAY, "STT_ERROR", e.getMessage(), null);
    }

    /** 400 VALIDATION_FAILED — Copilot boundary input invalid (BR7.2). */
    @ExceptionHandler(CopilotValidationException.class)
    public ResponseEntity<Map<String, Object>> onCopilotValidation(CopilotValidationException e) {
        List<Map<String, String>> details = null;
        if (e.getField() != null) {
            details = new ArrayList<>();
            details.add(Map.of("field", e.getField(),
                    "reason", e.getMessage() == null ? "invalid" : e.getMessage()));
        }
        return envelope(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                e.getMessage() == null ? "Dữ liệu không hợp lệ." : e.getMessage(), details);
    }

    /** 502 AI_ERROR — Copilot LLM failure/timeout, shown only in the panel (BR5.2). */
    @ExceptionHandler(AiException.class)
    public ResponseEntity<Map<String, Object>> onAi(AiException e) {
        // kind() ghi lại để quan sát (không lộ chi tiết nội bộ, BR7.3); thông điệp đã tiếng Việt.
        log.warn("Copilot AI error surfaced to client kind={}", e.kind());
        return envelope(HttpStatus.BAD_GATEWAY, "AI_ERROR", e.getMessage(), null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> onStatus(ResponseStatusException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return envelope(status, status.name(), e.getReason() == null ? status.getReasonPhrase() : e.getReason(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> onUnexpected(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return envelope(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Something went wrong processing the request.", null);
    }

    private ResponseEntity<Map<String, Object>> envelope(HttpStatus status, String code, String message,
                                                         List<Map<String, String>> details) {
        Map<String, Object> error = new java.util.LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        if (details != null && !details.isEmpty()) {
            error.put("details", details);
        }
        error.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(Map.of("error", error));
    }
}

package com.mb.callassistant.analysis.web;

import com.mb.callassistant.analysis.AnalysisJson;
import com.mb.callassistant.analysis.CallAnalysis;
import com.mb.callassistant.analysis.CallKeyin;
import com.mb.callassistant.analysis.KeyinSaveResult;
import com.mb.callassistant.analysis.KeyinService;
import com.mb.callassistant.analysis.web.dto.CallAnalysisDto;
import com.mb.callassistant.analysis.web.dto.KeyinDto;
import com.mb.callassistant.analysis.web.dto.KeyinSaveDto;
import com.mb.callassistant.analysis.web.dto.KeyinSavedDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API C3 — Kết quả cuộc gọi & keyin (U4). Lỗi nổi qua
 * {@code ApiExceptionHandler} sẵn có (403/404/400/409).
 * <ul>
 *   <li>{@code GET  /api/calls/{callId}/analysis} → 200 {@link CallAnalysisDto} | 404 (BR5.5).</li>
 *   <li>{@code POST /api/calls/{callId}/analysis/retry} → 202 | 409 (BR1.4).</li>
 *   <li>{@code GET  /api/calls/{callId}/keyin} → 200 {@link KeyinDto} | 404 (BR5.5).</li>
 *   <li>{@code PUT  /api/calls/{callId}/keyin} → 200 {@link KeyinSavedDto} | 400 | 403 (BR5.1/BR5.5).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/calls")
public class AnalysisController {

    private final KeyinService keyinService;
    private final AnalysisJson analysisJson;

    public AnalysisController(KeyinService keyinService, AnalysisJson analysisJson) {
        this.keyinService = keyinService;
        this.analysisJson = analysisJson;
    }

    /** Kết quả phân tích (WF3). 404 khi chưa có (đang xử lý hoặc AI lỗi). */
    @GetMapping("/{callId}/analysis")
    public CallAnalysisDto getAnalysis(@PathVariable Long callId) {
        CallAnalysis analysis = keyinService.getOwnedAnalysis(callId);
        return CallAnalysisDto.from(analysis, analysisJson);
    }

    /** Chạy lại phân tích AI (WF6). 202 Accepted; 409 khi chưa có transcript/đang chạy. */
    @PostMapping("/{callId}/analysis/retry")
    public ResponseEntity<Void> retry(@PathVariable Long callId) {
        keyinService.retryAnalysis(callId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /** Keyin đã lưu (WF4). 404 khi chưa lưu. */
    @GetMapping("/{callId}/keyin")
    public KeyinDto getKeyin(@PathVariable Long callId) {
        CallKeyin keyin = keyinService.getOwnedKeyin(callId);
        return KeyinDto.from(keyin, keyinService.findFeedback(callId), analysisJson);
    }

    /** Lưu hồ sơ cuộc gọi (WF4). 200 kèm mã tương tác + taskId; 400 thiếu bắt buộc; 403 khác RM. */
    @PutMapping("/{callId}/keyin")
    public KeyinSavedDto saveKeyin(@PathVariable Long callId, @Valid @RequestBody KeyinSaveDto body) {
        KeyinSaveResult result = keyinService.saveKeyin(callId, body.toSave());
        return KeyinSavedDto.from(result);
    }
}

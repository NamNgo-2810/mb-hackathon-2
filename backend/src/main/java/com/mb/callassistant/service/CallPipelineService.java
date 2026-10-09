package com.mb.callassistant.service;

import com.mb.callassistant.domain.CallRecord;
import com.mb.callassistant.domain.PipelineStatus;
import com.mb.callassistant.domain.PipelineStep;
import com.mb.callassistant.domain.SttStatus;
import com.mb.callassistant.repository.CallRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the post-call pipeline for one {@link CallRecord}: download the single
 * stereo recording (FR3) → transcribe with RM/KH separation via stereo split
 * (FR4). Executes
 * asynchronously so {@code POST /api/calls/{id}/ended} returns immediately; the
 * frontend polls {@code GET /api/calls/{id}} for status (FR1.3).
 *
 * <p>Status semantics (FR1.3, FR3.2):
 * <ul>
 *   <li>{@link SttStatus#PROCESSING} — pipeline running.</li>
 *   <li>{@link SttStatus#RECORDING_NOT_READY} — the .wav was not found within the
 *       download retry budget but may still appear; <b>non-terminal</b>, so the
 *       frontend keeps polling and a retry can be triggered.</li>
 *   <li>{@link SttStatus#DONE} — transcript persisted.</li>
 *   <li>{@link SttStatus#FAILED} — terminal: STT error or unexpected failure.</li>
 * </ul>
 */
@Service
public class CallPipelineService {

    private static final Logger log = LoggerFactory.getLogger(CallPipelineService.class);

    private final CallRecordRepository callRecords;
    private final RecordingService recordingService;
    private final SttService sttService;
    /** Các bước pipeline đã đăng ký (U4 đóng góp ANALYZE); chạy sau STT (I2). */
    private final List<CallPipelineStep> pipelineSteps;
    /** Lazy để tránh vòng phụ thuộc CallService ↔ CallPipelineService. U1 ghi trạng thái (BR1.2/BR4.2). */
    private final CallService callService;

    public CallPipelineService(CallRecordRepository callRecords,
                               RecordingService recordingService,
                               SttService sttService,
                               List<CallPipelineStep> pipelineSteps,
                               @Lazy CallService callService) {
        this.callRecords = callRecords;
        this.recordingService = recordingService;
        this.sttService = sttService;
        this.pipelineSteps = pipelineSteps;
        this.callService = callService;
    }

    /**
     * Mark the call processing, then run download + STT on a background thread.
     * A not-ready recording is recorded as {@link SttStatus#RECORDING_NOT_READY}
     * (non-terminal — keep polling); STT and unexpected errors are terminal
     * {@link SttStatus#FAILED}. Failures are never swallowed (phase guardrails).
     */
    @Async
    public void runPipeline(Long callId) {
        setStatus(callId, SttStatus.PROCESSING, null);
        try {
            byte[] stereoWav = recordingService.downloadRecording(callId);
            sttService.transcribe(callId, stereoWav);
            setStatus(callId, SttStatus.DONE, null);
            log.info("STT complete for call {}; running pipeline steps.", callId);
            // Sau STT: chạy các bước đã đăng ký (U4 ANALYZE) qua máy trạng thái SM2.
            runRegisteredSteps(callId);
            log.info("Pipeline complete for call {}.", callId);
        } catch (RecordingNotReadyException e) {
            // Not ready yet — the frontend keeps polling (FR1.3) and may re-trigger.
            log.warn("Recording not ready for call {}: {}", callId, e.getMessage());
            setStatus(callId, SttStatus.RECORDING_NOT_READY, e.getMessage());
            updatePipelineQuietly(callId, PipelineStatus.NO_RECORDING, null, e.getMessage());
        } catch (SttException e) {
            log.warn("Pipeline failed for call {} (STT): {}", callId, e.getMessage());
            setStatus(callId, SttStatus.FAILED, e.getMessage());
            updatePipelineQuietly(callId, PipelineStatus.STT_FAILED, null, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Pipeline failed for call {} (unexpected): {}", callId, e.getMessage(), e);
            setStatus(callId, SttStatus.FAILED, "Unexpected error: " + e.getMessage());
            updatePipelineQuietly(callId, PipelineStatus.STT_FAILED, null, "Unexpected error");
        }
    }

    /**
     * Sau STT thành công: đặt {@code PROCESSING/ANALYZE} rồi chạy từng bước đã
     * đăng ký theo thứ tự (U4 cung cấp ANALYZE). Bước lỗi
     * {@link CallPipelineStep.StepResult#failed} với kind AI_FAILED → U1 đặt
     * {@code AI_FAILED} và dừng; tất cả ok → {@code DONE}. Chỉ U1 ghi trạng thái,
     * qua {@link CallService#updatePipeline} (BR1.2/BR4.2). Giữ nguyên nhánh
     * NO_RECORDING/STT_FAILED ở {@link #runPipeline}.
     */
    private void runRegisteredSteps(Long callId) {
        if (pipelineSteps == null || pipelineSteps.isEmpty()) {
            // Không có bước nào đăng ký: coi pipeline đã xong (tương thích slice cũ).
            updatePipelineQuietly(callId, PipelineStatus.DONE, null, null);
            return;
        }
        List<String> transcript = loadTranscriptStrings(callId);
        String leadContext = loadLeadContext(callId);
        updatePipelineQuietly(callId, PipelineStatus.PROCESSING, PipelineStep.ANALYZE, null);
        for (CallPipelineStep step : pipelineSteps) {
            CallPipelineStep.StepResult result = step.run(callId, transcript, leadContext);
            if (result == null || !result.isOk()) {
                // Bước (U4) đã có thể tự yêu cầu U1 đặt AI_FAILED; đặt lại để chắc chắn
                // (updatePipeline cho phép PROCESSING->AI_FAILED và AI_FAILED->AI_FAILED).
                PipelineStatus target = mapFailure(result);
                updatePipelineQuietly(callId, target, null,
                        result == null ? "Bước pipeline lỗi" : result.getDetail());
                return;
            }
        }
        updatePipelineQuietly(callId, PipelineStatus.DONE, null, null);
    }

    private PipelineStatus mapFailure(CallPipelineStep.StepResult result) {
        if (result == null || result.getFailureKind() == null) {
            return PipelineStatus.AI_FAILED;
        }
        return switch (result.getFailureKind()) {
            case NO_RECORDING -> PipelineStatus.NO_RECORDING;
            case STT_FAILED -> PipelineStatus.STT_FAILED;
            case AI_FAILED -> PipelineStatus.AI_FAILED;
        };
    }

    /** Transcript dạng chuỗi thô cho các bước (U4 tự đọc cấu trúc của mình qua repo). */
    private List<String> loadTranscriptStrings(Long callId) {
        // Bước ANALYZE của U4 đọc transcript qua TranscriptTurnRepository; ở đây chỉ
        // truyền danh sách rỗng-an-toàn để giữ chữ ký I2 (U1 không ghép chuỗi riêng).
        return new ArrayList<>();
    }

    /** leadId tối thiểu làm ngữ cảnh cho bước AI (I2). */
    private String loadLeadContext(Long callId) {
        return callRecords.findById(callId).map(CallRecord::getLeadId).orElse(null);
    }

    /**
     * Đặt trạng thái SM2 qua U1, nuốt lỗi chuyển không hợp lệ để một bước lỗi
     * không làm hỏng cả pipeline (lỗi vẫn được ghi log, không nuốt im lặng
     * nghiệp vụ, NFR6).
     */
    private void updatePipelineQuietly(Long callId, PipelineStatus target, PipelineStep step, String detail) {
        try {
            callService.updatePipeline(callId, target, step, detail);
        } catch (RuntimeException e) {
            log.warn("Không cập nhật được pipelineStatus call {} -> {} ({})", callId, target, e.getMessage());
        }
    }

    /**
     * Persist a status transition. Each {@code save} runs in its own repository
     * transaction, which is what we want here: this method is called by self via
     * the {@code @Async} entry point, so a method-level {@code @Transactional}
     * proxy would not apply anyway. (Previously annotated {@code @Transactional};
     * removed as misleading — R-04.)
     */
    void setStatus(Long callId, SttStatus status, String errorDetail) {
        callRecords.findById(callId).ifPresent(cr -> {
            cr.setSttStatus(status);
            cr.setErrorDetail(errorDetail);
            callRecords.save(cr);
        });
    }
}

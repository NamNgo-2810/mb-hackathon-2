package com.mb.callassistant.analysis;

import com.mb.callassistant.service.CallPipelineStep;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Bước ANALYZE của pipeline (I2, U4). U1 gọi sau TRANSCRIBE; ủy quyền cho
 * {@link CallAnalysisService}. Cần transcript mới chạy (BR1.1 — kiểm trong
 * service). Trả {@link StepResult#ok()} khi phân tích thành công, hoặc
 * {@link StepResult#failed} với {@link FailureKind#AI_FAILED} khi thất bại
 * (service đã yêu cầu U1 đặt trạng thái; U4 không ghi pipelineStatus trực tiếp,
 * BR1.2).
 */
@Component
public class AnalyzeCallPipelineStep implements CallPipelineStep {

    private final CallAnalysisService analysisService;

    public AnalyzeCallPipelineStep(CallAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @Override
    public StepResult run(Long callId, List<String> transcriptTurns, String leadContext) {
        try {
            boolean ok = analysisService.analyze(callId, leadContext);
            return ok ? StepResult.ok()
                    : StepResult.failed(FailureKind.AI_FAILED, "Phân tích AI thất bại");
        } catch (RuntimeException e) {
            return StepResult.failed(FailureKind.AI_FAILED, "Lỗi phân tích AI");
        }
    }
}

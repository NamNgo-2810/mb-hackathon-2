package com.mb.callassistant.service;

import java.util.List;

/**
 * I2 — điểm mở rộng pipeline. Sau khi STT xong, U1 gọi các bước đã đăng ký theo
 * thứ tự; U4 đăng ký bước ANALYZE. U1 sở hữu {@code CallRecord} và là bên duy
 * nhất ghi trạng thái (qua {@code CallService.updatePipeline}, BR4.2).
 */
public interface CallPipelineStep {

    /** Loại lỗi phân biệt được cho một bước pipeline (ánh xạ sang trạng thái SM2). */
    enum FailureKind {
        NO_RECORDING,
        STT_FAILED,
        AI_FAILED
    }

    /** Kết quả một bước: thành công hoặc lỗi có phân loại. */
    final class StepResult {
        private final boolean ok;
        private final FailureKind failureKind;
        private final String detail;

        private StepResult(boolean ok, FailureKind failureKind, String detail) {
            this.ok = ok;
            this.failureKind = failureKind;
            this.detail = detail;
        }

        public static StepResult ok() {
            return new StepResult(true, null, null);
        }

        public static StepResult failed(FailureKind kind, String detail) {
            return new StepResult(false, kind, detail);
        }

        public boolean isOk() {
            return ok;
        }

        public FailureKind getFailureKind() {
            return failureKind;
        }

        public String getDetail() {
            return detail;
        }
    }

    /**
     * Chạy bước cho một cuộc gọi.
     *
     * @param callId         id cuộc gọi
     * @param transcriptTurns các lượt transcript (dạng chuỗi thô ở U1; U4 tự
     *                        giải nghĩa cấu trúc của mình)
     * @param leadContext    ngữ cảnh lead tối thiểu (leadId) để bước AI dùng
     */
    StepResult run(Long callId, List<String> transcriptTurns, String leadContext);
}

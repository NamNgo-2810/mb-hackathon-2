package com.mb.callassistant.domain;

/**
 * Bước hiện tại của pipeline khi {@link PipelineStatus#PROCESSING}; {@code null}
 * ở các trạng thái cuối (BR4.3). Thứ tự:
 * {@code FETCH_RECORDING -> TRANSCRIBE -> ANALYZE}. U4 đăng ký bước
 * {@link #ANALYZE} qua điểm mở rộng {@code CallPipelineStep}.
 */
public enum PipelineStep {
    FETCH_RECORDING,
    TRANSCRIBE,
    ANALYZE
}

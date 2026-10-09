package com.mb.callassistant.analysis;

/**
 * Máy trạng thái phân tích AI của cuộc gọi (SMA, functional-spec.md). Ánh xạ
 * vào {@code CallRecord.pipelineStatus} của U1 (U4 không ghi trạng thái đó trực
 * tiếp, BR1.2). {@link #ANALYSIS_FAILED} là trạng thái dừng-tạm ổn định; đường
 * ra duy nhất là RM chạy lại thủ công đưa về {@link #ANALYZING} (BR1.3).
 */
public enum AnalysisState {
    PENDING_TRANSCRIPT,
    ANALYZING,
    ANALYZED,
    ANALYSIS_FAILED,
    NO_ANALYSIS
}

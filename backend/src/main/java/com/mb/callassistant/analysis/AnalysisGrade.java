package com.mb.callassistant.analysis;

/**
 * Hạng cuộc gọi suy ra từ {@code totalScore} theo ngưỡng cấu hình (BR3.3):
 * A ≥ 85, B 70–84, C 50–69, D &lt; 50. Biên: 85 thuộc A, 84 thuộc B (AC2.2.4).
 * Server tự tính, không lấy từ LLM (ADR-006).
 */
public enum AnalysisGrade {
    A,
    B,
    C,
    D
}

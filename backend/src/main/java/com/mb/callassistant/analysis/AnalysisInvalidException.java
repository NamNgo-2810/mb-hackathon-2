package com.mb.callassistant.analysis;

/**
 * "AI thất bại": kết quả LLM sai cấu trúc / điểm ngoài khoảng (BR2.1/BR2.2).
 * Dùng nội bộ U4 để kích hoạt chạy lại một lần rồi chuyển ANALYSIS_FAILED; KHÔNG
 * bao giờ lưu {@link CallAnalysis} từ kết quả như vậy. Không chứa prompt/khóa
 * (BR8.2).
 */
public class AnalysisInvalidException extends RuntimeException {

    public AnalysisInvalidException(String message) {
        super(message);
    }
}

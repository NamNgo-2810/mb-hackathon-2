package com.mb.callassistant.domain;

/**
 * Trạng thái pipeline của một {@link CallRecord} (SM2, BR4.3). U1 là bên duy
 * nhất ghi trạng thái này (BR4.2). Chuyển hợp lệ:
 * {@code IN_CALL -> PROCESSING -> DONE}; {@code PROCESSING -> NO_RECORDING |
 * STT_FAILED | AI_FAILED}; {@code AI_FAILED -> PROCESSING} (U4 chạy lại AI).
 */
public enum PipelineStatus {
    /** Cuộc gọi đang diễn ra; chưa kết thúc. */
    IN_CALL,
    /** Đã kết thúc, pipeline đang chạy. */
    PROCESSING,
    /** Pipeline hoàn tất, có transcript + phân tích. Trạng thái cuối. */
    DONE,
    /** Không tìm thấy ghi âm. Trạng thái cuối (keyin vẫn lưu được ở U4). */
    NO_RECORDING,
    /** Lỗi STT. Trạng thái cuối. */
    STT_FAILED,
    /** Lỗi/timeout bước AI. Có thể chạy lại về {@link #PROCESSING} (U4). */
    AI_FAILED
}

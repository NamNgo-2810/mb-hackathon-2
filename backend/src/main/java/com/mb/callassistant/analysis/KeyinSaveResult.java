package com.mb.callassistant.analysis;

import java.util.List;

/**
 * Kết quả lưu hồ sơ cuộc gọi (BR6.1): mã cuộc gọi, mã tương tác và danh sách
 * taskId đã tạo. Giao diện dùng để mở hộp thoại điều hướng S4.
 */
public record KeyinSaveResult(
        String callId,
        String interactionCode,
        List<String> createdTaskIds
) {
}

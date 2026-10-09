package com.mb.callassistant.copilot;

/**
 * Chuỗi cố định của Copilot (BR3.3). Câu "không tìm thấy" phải đúng từng chữ:
 * giao diện và kiểm thử so khớp nguyên văn, không được thêm số liệu hay diễn giải.
 */
public final class CopilotMessages {

    /** Câu trả lời cố định khi không tìm thấy thông tin (BR3.1, BR3.3, hợp đồng C2). */
    public static final String NOT_FOUND =
            "Không tìm thấy thông tin này trong tài liệu chính sách hiện có";

    private CopilotMessages() {
    }
}

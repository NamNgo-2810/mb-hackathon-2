package com.mb.callassistant.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body cho {@code POST /api/auth/login} (C5). Kiểm tra tại biên: username và
 * password bắt buộc, giới hạn độ dài để chống lạm dụng. KHÔNG log giá trị
 * (BR5.1).
 */
public record LoginRequest(

        @NotBlank(message = "Tên đăng nhập là bắt buộc")
        @Size(max = 128, message = "Tên đăng nhập quá dài")
        String username,

        @NotBlank(message = "Mật khẩu là bắt buộc")
        @Size(max = 256, message = "Mật khẩu quá dài")
        String password
) {
}

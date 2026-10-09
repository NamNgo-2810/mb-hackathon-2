package com.mb.callassistant.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Băm/đối chiếu mật khẩu bằng BCrypt (BR1.1, BR5.1) qua
 * {@link BCryptPasswordEncoder} của {@code spring-security-crypto}. Mật khẩu thô
 * không bao giờ được lưu hay log — chỉ dùng tức thời để {@link #matches} hoặc
 * {@link #hash}. Dùng được ở cả hai chế độ (seed login cần {@link #hash}).
 */
@Component
public class PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** True nếu mật khẩu thô khớp bản băm BCrypt (BR1.1). */
    public boolean matches(String rawPassword, String bcryptHash) {
        if (rawPassword == null || bcryptHash == null || bcryptHash.isBlank()) {
            return false;
        }
        return encoder.matches(rawPassword, bcryptHash);
    }

    /** Băm BCrypt một mật khẩu thô (chỉ dùng khi seed tài khoản RM mẫu). */
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }
}

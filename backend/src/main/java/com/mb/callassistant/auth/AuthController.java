package com.mb.callassistant.auth;

import com.mb.callassistant.config.AppProperties;
import com.mb.callassistant.lead.RmUser;
import com.mb.callassistant.lead.RmUserRepository;
import com.mb.callassistant.web.UnauthorizedException;
import com.mb.callassistant.web.dto.Rm;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Điểm vào đăng nhập/đăng xuất (C5, U6). Chỉ có ý nghĩa ở chế độ {@code login};
 * bean chỉ nạp khi {@code app.auth.mode=login} nên ở chế độ demo không có route
 * này. Thông điệp lỗi tiếng Việt; không log mật khẩu/token (BR5.1).
 *
 * <p>Thông điệp đăng nhập sai dùng DUY NHẤT một câu, không lộ trường nào sai
 * (BR1.2).
 */
@RestController
@RequestMapping("/api/auth")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.auth.mode", havingValue = "login")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /** Thông điệp duy nhất cho mọi lỗi đăng nhập (BR1.2). */
    static final String LOGIN_FAILED_MESSAGE = "Sai tên đăng nhập hoặc mật khẩu";
    private static final String BEARER_PREFIX = "Bearer ";

    private final RmUserRepository rmUsers;
    private final PasswordHasher passwordHasher;
    private final AuthTokenService tokenService;
    private final AppProperties props;

    public AuthController(RmUserRepository rmUsers, PasswordHasher passwordHasher,
                          AuthTokenService tokenService, AppProperties props) {
        this.rmUsers = rmUsers;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
        this.props = props;
    }

    /**
     * Đăng nhập (BR1.1/BR1.2). Đúng → 200 {@link LoginResponse}; sai (username
     * không có HOẶC password sai) → 401 với một thông điệp duy nhất; thiếu
     * trường → 400 VALIDATION_FAILED (do {@code @Valid}).
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        RmUser rm = rmUsers.findByUsername(req.username()).orElse(null);
        boolean ok = rm != null && passwordHasher.matches(req.password(), rm.getPasswordHash());
        if (!ok) {
            applyFailDelay();
            // Không log username/password; không nói rõ trường nào sai (BR1.2/BR5.1).
            throw new UnauthorizedException(LOGIN_FAILED_MESSAGE);
        }
        AuthTokenService.IssuedToken issued = tokenService.issue(rm);
        return new LoginResponse(issued.rawToken(), issued.session().getExpiresAt(), Rm.from(rm));
    }

    /**
     * Đăng xuất (BR2.3): thu hồi phiên của token hiện tại, trả 204. Idempotent —
     * token thiếu/không khớp vẫn trả 204.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearer(authorization);
        tokenService.revoke(token);
        return ResponseEntity.noContent().build();
    }

    private String extractBearer(String authorization) {
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    /** (BR5.2) Độ trễ cố định tùy chọn cho phản hồi thất bại; mặc định 0 (tắt). */
    private void applyFailDelay() {
        long delay = props.getAuth().getFailDelayMs();
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

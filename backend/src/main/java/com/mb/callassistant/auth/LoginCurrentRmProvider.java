package com.mb.callassistant.auth;

import com.mb.callassistant.lead.CurrentRmProvider;
import com.mb.callassistant.lead.RmUser;
import com.mb.callassistant.web.UnauthorizedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Bản hiện thực chế độ {@code login} của {@link CurrentRmProvider} (I1, ADR-007,
 * U6). Chỉ hoạt động khi {@code app.auth.mode=login}; chế độ {@code demo} (mặc
 * định) vẫn dùng {@code DemoCurrentRmProvider} của U1 nên đổi chế độ chỉ qua cấu
 * hình, không sửa unit khác (BR3.1). Hai bean này loại trừ nhau theo
 * {@code app.auth.mode} nên luôn có đúng một {@code CurrentRmProvider}.
 *
 * <p>{@code currentRm()} đọc token Bearer của request hiện tại qua
 * {@link AuthContext}, giải qua {@link AuthTokenService#resolve}; thiếu/sai/hết
 * hạn → ném {@link UnauthorizedException} để biên REST trả 401 (BR2.2, pseudocode
 * FD §6).
 */
@Component
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "login")
public class LoginCurrentRmProvider implements CurrentRmProvider {

    private final AuthTokenService tokenService;

    public LoginCurrentRmProvider(AuthTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public RmUser currentRm() {
        String rawToken = AuthContext.getToken();
        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthorizedException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn");
        }
        return tokenService.resolve(rawToken)
                .orElseThrow(() -> new UnauthorizedException(
                        "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"));
    }
}

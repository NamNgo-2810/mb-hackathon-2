package com.mb.callassistant.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Đọc header {@code Authorization: Bearer <token>} và đặt token thô vào
 * {@link AuthContext} cho {@link LoginCurrentRmProvider} dùng (BR2.2). Filter
 * KHÔNG tự chặn request — biên REST tự ném 401 khi một handler cần
 * {@code currentRm()}. Luôn xóa {@link AuthContext} ở {@code finally} để không
 * rò token sang request khác trên cùng luồng (BR5.1).
 *
 * <p>Chỉ đăng ký khi {@code app.auth.mode=login}; ở chế độ demo filter không tồn
 * tại nên không có chi phí nào.
 */
@Component
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "login")
public class BearerTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith(BEARER_PREFIX)) {
                String token = header.substring(BEARER_PREFIX.length()).trim();
                if (!token.isEmpty()) {
                    AuthContext.setToken(token);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }
}

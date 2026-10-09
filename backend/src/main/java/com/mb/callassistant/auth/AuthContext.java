package com.mb.callassistant.auth;

/**
 * Giữ token Bearer thô của request hiện tại trong một {@link ThreadLocal}, do
 * {@link BearerTokenFilter} đặt ở đầu request và xóa ở cuối. Cho phép
 * {@link LoginCurrentRmProvider} đọc token mà không phụ thuộc trực tiếp vào
 * {@code HttpServletRequest}. Chỉ chứa token thô trong bộ nhớ trong vòng đời
 * một request; không bao giờ log (BR5.1).
 */
public final class AuthContext {

    private static final ThreadLocal<String> CURRENT_TOKEN = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setToken(String rawToken) {
        CURRENT_TOKEN.set(rawToken);
    }

    /** Token Bearer thô của request hiện tại, hoặc null nếu không có. */
    public static String getToken() {
        return CURRENT_TOKEN.get();
    }

    public static void clear() {
        CURRENT_TOKEN.remove();
    }
}

package com.mb.callassistant.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Băm token Bearer bằng SHA-256 (BR2.1). Server chỉ lưu bản băm này, không bao
 * giờ lưu/log token thô (BR5.1). Hàm thuần, không trạng thái.
 */
public final class TokenHasher {

    private TokenHasher() {
    }

    /** SHA-256 của token thô, trả chuỗi hex thường. */
    public static String sha256(String rawToken) {
        if (rawToken == null) {
            throw new IllegalArgumentException("rawToken null");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luôn có trên mọi JVM chuẩn.
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }
}

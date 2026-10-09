package com.mb.callassistant.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Phiên đăng nhập của một RM ở chế độ {@code login} (ADR-007, U6). Mỗi lần đăng
 * nhập đúng sinh một token Bearer ngẫu nhiên; server chỉ lưu BẢN BĂM của token
 * ({@code tokenHash}), không bao giờ lưu token thô (BR2.1, BR5.1, NFR2).
 *
 * <p>Bất biến (FD §3): một phiên chỉ <b>hợp lệ</b> khi {@code revokedAt == null}
 * AND {@code now < expiresAt}. Không có đường quay lại từ REVOKED/EXPIRED về
 * ACTIVE — RM phải đăng nhập lại.
 *
 * <p><b>BR5.1:</b> {@code tokenHash} và token thô không bao giờ xuất hiện trong
 * log hay payload API; {@link #toString()} cố ý loại {@code tokenHash}.
 */
@Entity
@Table(name = "auth_session")
public class AuthSession {

    @Id
    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "rm_id", nullable = false)
    private String rmId;

    /** Băm SHA-256 của token Bearer ≥ 32 byte; KHÔNG lưu token thô (BR2.1/BR5.1). */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** Đặt khi logout; null khi phiên còn hiệu lực (BR2.3). */
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected AuthSession() {
        // JPA
    }

    public AuthSession(String sessionId, String rmId, String tokenHash,
                       OffsetDateTime createdAt, OffsetDateTime expiresAt) {
        this.sessionId = sessionId;
        this.rmId = rmId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /** Phiên hợp lệ khi chưa thu hồi và chưa hết hạn (bất biến FD §3, BR2.2). */
    public boolean isValid(OffsetDateTime now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getRmId() {
        return rmId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    /** Cố ý KHÔNG đưa {@code tokenHash} vào chuỗi/log (BR5.1). */
    @Override
    public String toString() {
        return "AuthSession{sessionId='" + sessionId + "', rmId='" + rmId
                + "', createdAt=" + createdAt + ", expiresAt=" + expiresAt
                + ", revokedAt=" + revokedAt + '}';
    }
}

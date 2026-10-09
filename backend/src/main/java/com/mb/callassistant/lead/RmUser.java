package com.mb.callassistant.lead;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Nhân viên quan hệ khách hàng (RM). Ở U1 chỉ cần hình dạng tối thiểu để chế độ
 * demo trả về một RM mẫu cố định (ADR-007). U6 bổ sung đăng nhập thật mà không
 * đổi hình dạng này.
 *
 * <p><b>BR6.1:</b> {@code passwordHash} không bao giờ được đưa vào payload API
 * hay log; chỉ lưu dạng băm BCrypt (null ở chế độ demo).
 */
@Entity
@Table(name = "rm_user")
public class RmUser {

    @Id
    @Column(name = "rm_id")
    private String rmId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    /** Băm BCrypt; null ở chế độ demo. Không bao giờ trả ra API/log (BR6.1). */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    protected RmUser() {
        // JPA
    }

    public RmUser(String rmId, String username, String displayName) {
        this.rmId = rmId;
        this.username = username;
        this.displayName = displayName;
    }

    public String getRmId() {
        return rmId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}

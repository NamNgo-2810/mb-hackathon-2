package com.mb.callassistant.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Kho phiên đăng nhập (U6). Tra phiên theo bản băm của token Bearer (BR2.2). */
public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {

    /** Phiên gắn với một token (qua bản băm SHA-256); dùng để xác thực request. */
    Optional<AuthSession> findByTokenHash(String tokenHash);
}

package com.mb.callassistant.auth;

import com.mb.callassistant.web.dto.Rm;

import java.time.OffsetDateTime;

/**
 * Phản hồi {@code POST /api/auth/login} (C5). Chứa token Bearer THÔ (chỉ trả một
 * lần — BR2.1), thời điểm hết hạn phiên và RM (qua {@link Rm}, không lộ
 * passwordHash — BR5.1).
 */
public record LoginResponse(String token, OffsetDateTime expiresAt, Rm rm) {
}

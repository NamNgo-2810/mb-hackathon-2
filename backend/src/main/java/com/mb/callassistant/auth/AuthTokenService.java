package com.mb.callassistant.auth;

import com.mb.callassistant.config.AppProperties;
import com.mb.callassistant.lead.RmUser;
import com.mb.callassistant.lead.RmUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Dịch vụ cấp/giải/thu hồi token Bearer của phiên đăng nhập (U6, BR2.1–BR2.3).
 *
 * <p>Token thô ngẫu nhiên ≥ 32 byte được sinh bằng {@link SecureRandom}, trả về
 * giao diện <b>đúng một lần</b> ở phản hồi đăng nhập; server chỉ lưu bản băm
 * SHA-256 ({@link TokenHasher}). Không bao giờ log token thô hay bản băm (BR5.1).
 *
 * <p>{@link Clock} được tiêm để test được nhánh hết hạn một cách tất định.
 */
@Service
public class AuthTokenService {

    /** Số byte ngẫu nhiên của token Bearer; ≥ 32 theo BR2.1 (dùng 32). */
    static final int TOKEN_BYTES = 32;

    private final AuthSessionRepository sessions;
    private final RmUserRepository rmUsers;
    private final AppProperties props;
    private final SecureRandom random;
    private final Clock clock;

    public AuthTokenService(AuthSessionRepository sessions, RmUserRepository rmUsers, AppProperties props) {
        this(sessions, rmUsers, props, new SecureRandom(), Clock.systemDefaultZone());
    }

    /** Hàm dựng dùng cho test: tiêm {@link SecureRandom} và {@link Clock} tất định. */
    AuthTokenService(AuthSessionRepository sessions, RmUserRepository rmUsers, AppProperties props,
                     SecureRandom random, Clock clock) {
        this.sessions = sessions;
        this.rmUsers = rmUsers;
        this.props = props;
        this.random = random;
        this.clock = clock;
    }

    /**
     * Cấp token mới cho một RM đăng nhập đúng (BR2.1). Trả cặp
     * {@link IssuedToken}: token thô (chỉ lúc này) + phiên đã lưu.
     */
    @Transactional
    public IssuedToken issue(RmUser rm) {
        byte[] buf = new byte[TOKEN_BYTES];
        random.nextBytes(buf);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);

        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime expiresAt = now.plusHours(props.getAuth().getSessionTtlHours());
        AuthSession session = new AuthSession(
                UUID.randomUUID().toString(),
                rm.getRmId(),
                TokenHasher.sha256(rawToken),
                now,
                expiresAt);
        sessions.save(session);
        return new IssuedToken(rawToken, session);
    }

    /**
     * Giải token thô thành RM hiện tại nếu phiên còn hiệu lực (BR2.2). Thiếu
     * token, không khớp phiên, đã thu hồi hoặc hết hạn → {@link Optional#empty()}.
     */
    @Transactional(readOnly = true)
    public Optional<RmUser> resolve(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        return sessions.findByTokenHash(TokenHasher.sha256(rawToken))
                .filter(s -> s.isValid(now))
                .flatMap(s -> rmUsers.findById(s.getRmId()));
    }

    /**
     * Thu hồi phiên của token hiện tại (BR2.3). Idempotent: token thiếu/không
     * khớp/đã thu hồi đều trả về êm, không ném lỗi.
     */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        sessions.findByTokenHash(TokenHasher.sha256(rawToken))
                .filter(s -> s.getRevokedAt() == null)
                .ifPresent(s -> {
                    s.setRevokedAt(OffsetDateTime.now(clock));
                    sessions.save(s);
                });
    }

    /** Cặp trả về khi cấp token: token THÔ (một lần) + phiên đã lưu. */
    public record IssuedToken(String rawToken, AuthSession session) {
    }
}

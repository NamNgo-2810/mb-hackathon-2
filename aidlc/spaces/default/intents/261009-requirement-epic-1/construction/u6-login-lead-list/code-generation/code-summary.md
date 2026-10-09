# Tóm tắt sinh mã — U6 `u6-login-lead-list`

Chế độ đăng nhập thật (`app.auth.mode=login`) và danh sách lead của RM hiện tại
(hợp đồng C5, I1). Chế độ `demo` giữ nguyên là mặc định (ADR-007, BR3.1).

## Tệp đã tạo / sửa

### Backend — package mới `com.mb.callassistant.auth`
- `AuthSession.java` — thực thể phiên (sessionId, rmId, tokenHash, createdAt, expiresAt, revokedAt); bất biến `isValid(now)` = chưa thu hồi và chưa hết hạn (BR2.1/BR2.2/BR5.1).
- `AuthSessionRepository.java` — `findByTokenHash` (BR2.2).
- `TokenHasher.java` — băm SHA-256 token (hex), hàm thuần (BR2.1/BR5.1).
- `AuthTokenService.java` — `issue/resolve/revoke`; token ≥ 32 byte `SecureRandom` (Base64 url-safe), chỉ lưu băm, hạn `sessionTtlHours`; `Clock` tiêm được để test hết hạn (BR2.1–BR2.3).
- `AuthContext.java` — ThreadLocal giữ token thô của request (BR2.2/BR5.1).
- `BearerTokenFilter.java` — `OncePerRequestFilter`, chỉ nạp khi `login`; đọc `Authorization: Bearer`, đặt vào `AuthContext`, xóa ở `finally`.
- `LoginCurrentRmProvider.java` — bản `login` của `CurrentRmProvider`, `@ConditionalOnProperty(havingValue="login")`; thiếu/sai token → `UnauthorizedException` → 401 (BR2.2/BR3.1).
- `PasswordHasher.java` — bọc `BCryptPasswordEncoder` (BR1.1/BR5.1).
- `LoginRmSeeder.java` — `CommandLineRunner`, chỉ chạy khi `login`; seed `rm.a`/`rm.b` + vài lead minh hoạ; mật khẩu từ `AUTH_DEMO_PASSWORD` (mặc định demo), idempotent (BR1.1/BR5.1).
- `AuthController.java` — `POST /api/auth/login` + `/logout`, `@ConditionalOnProperty(havingValue="login")`; một thông điệp 401 duy nhất, `failDelayMs` tùy chọn (BR1.1/BR1.2/BR2.1/BR2.3/BR5.2).
- `LoginRequest.java`, `LoginResponse.java` — DTO body/phản hồi (không lộ passwordHash).

### Backend — sửa tại chỗ
- `config/AppProperties.java` — thêm `Auth.sessionTtlHours` (8), `Auth.failDelayMs` (0).
- `web/UnauthorizedException.java` — lỗi 401 mới.
- `web/ApiExceptionHandler.java` — ánh xạ `UnauthorizedException` → envelope `UNAUTHORIZED` 401.
- `web/LeadListController.java` — `GET /api/leads` cho RM hiện tại, sắp ưu tiên rồi SLA (null cuối), rỗng → `[]` (BR4.1–BR4.3).
- `pom.xml` — thêm `spring-security-crypto` (version do Boot BOM quản, **không** thêm starter-security); JaCoCo include thêm `com.mb.callassistant.auth.*` giữ sàn 0.80.
- `src/main/resources/application.properties` — thêm `app.auth.session-ttl-hours`, `app.auth.fail-delay-ms`.
- `backend/.env.example` — thêm `AUTH_MODE`, `AUTH_SESSION_TTL_HOURS`, `AUTH_FAIL_DELAY_MS`, `AUTH_DEMO_PASSWORD`.
- `backend/README.md` — mục U6 (bật login, biến môi trường, tài khoản mẫu, luồng Bearer, lưu ý bảo mật).

### Frontend
- `public/screens/0-dang-nhap.html` — màn S0 (username/password, nút Đăng nhập có vòng xoay + vô hiệu khi gửi, dòng lỗi icon+chữ, `data-testid`).
- `public/screens/leads-list.html` — danh sách lead (leadCode, KH, sản phẩm, nguồn, SLA còn lại, rankScore/10, nhãn ưu tiên, giai đoạn), rỗng → "Chưa có tiềm năng bán"; nút đăng xuất; `data-testid` `lead-list`, `lead-row-{leadId}`.
- `public/js/api.js` — thêm `login`, `logout`, `listLeads`, kho token (`setAuthToken`/`getAuthToken`/`clearAuthAndRedirect`); gắn `Authorization: Bearer` khi có token; bắt `401` → xóa token + về S0 **chỉ khi đang có token** (chế độ login) (AC4.1.3).
- `server/index.js` — route tĩnh cho S0, danh sách lead và shortcut `/login`, `/leads`.

## Quyết định & cách bố trí
- **Hai bean `CurrentRmProvider` loại trừ nhau theo `app.auth.mode`**: `DemoCurrentRmProvider` (`matchIfMissing=true`) ở demo, `LoginCurrentRmProvider` (`havingValue="login"`) ở login → luôn đúng một bean trong mỗi chế độ.
- **Danh sách lead đặt ở `web/LeadListController` (collection path `/api/leads`)**, tách khỏi `LeadController` của U1; path literal `/demo`,`/next` của U1 vẫn ưu tiên hơn pattern `{leadId}` nên không nhập nhằng.
- **Sắp xếp** dùng `LeadPriority.ordinal()` vì thứ tự khai báo enum (KHAI_THAC_NGAY, HOT_LEAD, THUONG) đã trùng ưu tiên nghiệp vụ; `slaDueAt` null xếp cuối cùng mức ưu tiên (comparator trong controller, không phụ thuộc thứ tự null của DB) (BR4.2).
- **Bảo mật**: BCrypt cho mật khẩu, SHA-256 cho token; token ≥ 32 byte `SecureRandom`; một thông điệp 401; không log mật khẩu/token/băm.

## Sai lệch (deviations)
- `LoginRmSeeder` seed thêm vài **lead minh hoạ** (ngoài tài khoản RM) để danh sách lead có nội dung ở chế độ login, vì `DemoDataSeeder` chỉ chạy ở profile `demo`. Dữ liệu seed này là bảng của U1 nhưng U6 chỉ ghi khi chạy `login`, idempotent, không đổi hình dạng thực thể.
- `GET /api/leads` của U6 không chuyển trạng thái/ghi gì — chỉ đọc; kiểm 403 lead đơn lẻ của RM khác vẫn do U1 thực thi (AC4.2.2, N/A trong traceability U6).

## Trạng thái test
**Hoãn theo yêu cầu người dùng** ("gen code cho tất cả các unit một lượt, phần
test để sau"). Lần này **không tạo file test** và **không chạy** build/test. Sàn
độ phủ JaCoCo 80% dòng cho package `com.mb.callassistant.auth` đã thêm vào
`pom.xml` và **không bị hạ** — chỉ kiểm chứng ở đợt test sau theo
`unit-test-instructions.md` (phương pháp test-after theo từng lớp).

## Cấu hình — cách đổi sang chế độ login
- Đặt `AUTH_MODE=login` (hoặc `app.auth.mode=login` trong `application.properties`).
- (Tùy chọn) `AUTH_SESSION_TTL_HOURS`, `AUTH_FAIL_DELAY_MS`, `AUTH_DEMO_PASSWORD`.
- Khởi động lại backend; `LoginRmSeeder` seed `rm.a`/`rm.b`. Mở `/login` trên
  frontend, đăng nhập, xem danh sách lead ở `/leads`.
- Để quay lại demo: bỏ `AUTH_MODE` hoặc đặt `=demo` (mặc định). Profile `demo`
  luôn ghim `app.auth.mode=demo`.

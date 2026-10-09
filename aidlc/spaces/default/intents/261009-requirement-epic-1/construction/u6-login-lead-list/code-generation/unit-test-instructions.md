# Hướng dẫn kiểm thử Unit — U6 `u6-login-lead-list`

> Package của U6: `com.mb.callassistant.auth` (và điểm vào `GET /api/leads` ở
> `com.mb.callassistant.web.LeadListController`). Mọi lệnh dưới đây **chỉ** chạy
> test của U6, không chạy toàn bộ suite.
>
> **Lưu ý quan trọng (theo yêu cầu người dùng):** đợt sinh mã lần này **DỜI** toàn
> bộ việc viết/chạy test sang sau. File này ghi sẵn khung và lệnh chính xác để đợt
> test chạy được ngay; **chưa tạo file test và chưa chạy lệnh nào ở lần này**. Sàn
> độ phủ 80% dòng cho package `auth` **không bị hạ**.

## Khung và cấu hình test

- Framework: **JUnit 5 (Jupiter) + Mockito + AssertJ + Spring Test/MockMvc** —
  đã có qua `spring-boot-starter-test` trong `backend/pom.xml` (version do Spring
  Boot BOM 3.3.4 quản, không pin).
- BCrypt: `spring-security-crypto` (thêm ở Step 11 của plan, version do BOM quản).
- Đo độ phủ: **JaCoCo qua `mvn verify`**; rule BUNDLE sẽ bao gồm
  `com.mb.callassistant.auth.*` với sàn `0.80` LINE (Step 11). KHÔNG hạ sàn.
- Vị trí file test (đợt sau): `backend/src/test/java/com/mb/callassistant/auth/`
  và `backend/src/test/java/com/mb/callassistant/web/LeadListControllerTest.java`.
- Test chế độ login dùng property `app.auth.mode=login` (ví dụ
  `@TestPropertySource(properties = "app.auth.mode=login")` cho test MockMvc, hoặc
  dựng trực tiếp bean cho unit test thuần).

## Cách chạy test của riêng U6

Lệnh chạy test phạm vi U6 (chạy được trước bước test-first đầu tiên của đợt test):

```
mvn -f backend/pom.xml -Dtest="com.mb.callassistant.auth.**.*Test" verify
```

Khi cần gộp cả test controller danh sách lead ở package `web`:

```
mvn -f backend/pom.xml -Dtest="com.mb.callassistant.auth.**.*Test,com.mb.callassistant.web.LeadListControllerTest" verify
```

- `verify` chạy pha JaCoCo (`report` + `check`) nên vừa chạy test vừa đo độ phủ.
- **IntelliJ IDEA**: mở package `com.mb.callassistant.auth` trong cây `src/test/java`,
  chuột phải → **Run 'Tests in auth'**; hoặc chạy từng lớp `*Test`. Để xem độ phủ,
  dùng **More Run/Debug → Run with Coverage** (hoặc chạy goal Maven `verify` trong
  tab Maven để lấy báo cáo JaCoCo tại `backend/target/site/jacoco/index.html`).
- `-Dtest=...` giữ phạm vi đúng U6 — **không** dùng `mvn test` trần (sẽ chạy cả
  suite của các unit khác, trái quy ước per-unit).

## Mục tiêu độ phủ

- **Sàn 80% line coverage** cho package `com.mb.callassistant.auth` (JaCoCo BUNDLE,
  Step 11) — kiểm **thủ công trên máy lập trình viên trước mỗi lần commit** theo
  team.md; không có CI. Sàn là ràng buộc, không được nới để cho build qua.
- Chiến lược **standard**: khoảng **5–8 test / thành phần** cho các lớp nghiệp vụ
  và API chính (`AuthTokenService`, `PasswordHasher`, `LoginCurrentRmProvider`,
  `AuthController`, `BearerTokenFilter`, `LeadListController`), cộng test tích hợp
  MockMvc cho biên REST.

## Hướng dẫn mock/stub

- **`RmUserRepository`, `AuthSessionRepository`, `LeadService`/`LeadRepository`**:
  mock bằng Mockito (`@Mock` + `when(...).thenReturn(...)`); không chạm DB thật
  trong unit test lớp nghiệp vụ.
- **`PasswordHasher` / BCrypt**: ở unit test `AuthTokenService` có thể mock; ở test
  riêng `PasswordHasherTest` dùng `BCryptPasswordEncoder` thật với hash cố định để
  kiểm `matches` đúng/sai.
- **`SecureRandom` / thời gian**: tiêm `Clock` (hoặc `Supplier<OffsetDateTime>`) vào
  `AuthTokenService` để test được `expiresAt` và nhánh hết hạn một cách tất định;
  kiểm token sinh ra có độ dài giải mã ≥ 32 byte.
- **MockMvc (login/logout, `GET /api/leads`)**: dựng context chế độ login, nạp
  bean giả cho provider/token service khi cần cô lập; kiểm **đúng một** thông điệp
  401 `Sai tên đăng nhập hoặc mật khẩu` cho cả hai nhánh sai username / sai password
  (BR1.2) và không có `passwordHash`/token thô/`tokenHash` trong payload (BR5.1).

## Dữ liệu test

- Tài khoản RM mẫu: `username` cố định (ví dụ `rm.a`, `rm.b`) với `passwordHash`
  BCrypt dựng sẵn trong test; **không** dùng mật khẩu thật, chỉ giá trị demo trong test.
- Phiên: tạo `AuthSession` với `expiresAt` tương lai (hợp lệ), quá khứ (hết hạn),
  và `revokedAt != null` (đã thu hồi) để phủ ba nhánh của `isValid`/BR2.2.
- Lead: hai RM (A, B) mỗi RM vài lead với `priority` khác nhau
  (KHAI_THAC_NGAY/HOT_LEAD/THUONG) và `slaDueAt` khác nhau để kiểm BR4.1 (chỉ lead
  của RM hiện tại) và BR4.2 (thứ tự ưu tiên rồi SLA); một RM không có lead để kiểm
  BR4.3 (`[]` → "Chưa có tiềm năng bán").
- Token: một token thô hợp lệ (giải mã ≥ 32 byte) và băm SHA-256 tương ứng; một
  token sai/không khớp phiên để kiểm nhánh 401.

## Phạm vi kiểm (test-after theo lớp, DỜI sang đợt sau)

- Lớp mô hình/phiên: bất biến `AuthSession.isValid` (BR2.2); không lộ `tokenHash` (BR5.1).
- Lớp nghiệp vụ: `issue`/`resolve`/`revoke` (BR2.1–BR2.3), `PasswordHasher.matches` (BR1.1).
- Lớp API: login đúng/sai (BR1.1/BR1.2), logout 204 (BR2.3), filter + 401 khi
  thiếu/sai/hết hạn token (BR2.2), `GET /api/leads` lọc theo RM + sắp xếp + rỗng
  (BR4.1–BR4.3).
- Chế độ demo: `GET /api/leads` vẫn chạy với RM mẫu (không hồi quy ADR-007/BR3.1).

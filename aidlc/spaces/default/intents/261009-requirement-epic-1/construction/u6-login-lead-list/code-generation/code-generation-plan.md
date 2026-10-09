# Code Generation Plan — U6 `u6-login-lead-list`

## Summary

- Builds: Chế độ đăng nhập thật (`app.auth.mode=login`) cho hợp đồng C5/I1 trong package mới `com.mb.callassistant.auth` — `POST /api/auth/login`, `POST /api/auth/logout`, bản `login` của `CurrentRmProvider`, token Bearer (≥ 32 byte, chỉ lưu băm SHA-256, hạn 8 giờ cấu hình được), kiểm mật khẩu BCrypt, phiên `AuthSession`; thêm điểm vào `GET /api/leads` (danh sách lead của RM hiện tại) và màn S0 đăng nhập + danh sách lead ở frontend. Chế độ `demo` giữ nguyên là mặc định.
- Touches: thêm `backend/src/main/java/com/mb/callassistant/auth/**` (provider login, AuthSession + repo, AuthTokenService, AuthController, bearer filter, DTO) và `web/LeadListController.java`; mở rộng `config/AppProperties.java` (khối `Auth`), `application.properties`/`application-demo.properties` (khóa `app.auth.*`), `backend/pom.xml` (thêm `spring-security-crypto` + mở rộng include JaCoCo), `config/DemoDataSeeder` không đổi và seed RM login mới ở `config/LoginRmSeeder`; frontend thêm `public/screens/0-dang-nhap.html`, `public/screens/leads-list` (cột danh sách) và sửa `public/js/api.js` (header Authorization + bắt 401).
- Tests: test-after theo từng lớp (xác thực mật khẩu, token/hash/hết hạn, filter 401, `GET /api/leads` lọc + sắp xếp, provider login) khoảng ~14–20 test JUnit5 + Mockito + MockMvc; sàn JaCoCo ≥ 80% dòng cho package `com.mb.callassistant.auth` — **DỜI** sang đợt test sau theo yêu cầu người dùng (xem mục bên dưới); sàn không bị hạ.

## Testing Contract

```json
{
  "version": 1,
  "methodology": "test-after",
  "source": "team",
  "ordering": "Hiện thực từng lớp có thể kiểm thử trước, rồi viết và chạy test cho lớp đó ngay sau đó, và xác nhận độ phủ cục bộ trước khi commit.",
  "scope": "classic",
  "test_strategy": "standard",
  "project_type": "brownfield",
  "applicable_notes": [
    {
      "layer": "org",
      "text": "We treat tests as a first-class deliverable in every Bolt. The specific\nmethodology (TDD, BDD, ATDD, or classic test-after) is affirmed at\npractices-discovery and recorded in `team.md` under this heading with explicit\n`Methodology` and `Ordering` fields; Code Generation resolves those fields\nindependently from coverage, tooling, and scope notes.\n\nWhen no posture has been affirmed, our default per scope is:\n- **Methodology**: test-after\n- **Ordering**: implement each applicable testable layer, then write and run\n  that layer's tests.\n- `mvp`, `enterprise`, `feature`, `infra`, `classic` add an 80% line-coverage\n  floor and CI execution before merge.\n- `bugfix`, `security-patch` add a targeted regression for the specific\n  bug/vulnerability and require the existing suite to remain green.\n- `express` uses the Minimal strategy: requirement-driven unit tests (one per\n  requirement, with a happy-path floor per component); existing tests remain\n  green.\n- `poc`, `refactor`, `workshop` add no extra new-test floor and require the\n  existing suite to remain green.\n\nThe active `Test Strategy` still applies in every scope and determines test\nvolume/types. Scope floors are additive; they never reduce or replace the\nselected strategy.\n\nBuild and Test verifies defined coverage floors and affirmed quality targets;\nthey may not be weakened to make a step pass.\n\nAffirm a stricter posture in `team.md` if the team commits to one."
    },
    {
      "layer": "team",
      "text": "- **Methodology**: test-after\n- **Methodology evidence**: Kho hiện không có test tự động nào và\n  `backend/README.md` ghi rõ đây là quyết định hackathon; nhóm chốt viết test\n  sau khi hiện thực, khớp hướng này.\n- **Ordering**: Hiện thực từng lớp có thể kiểm thử trước, rồi viết và chạy test\n  cho lớp đó ngay sau đó, và xác nhận độ phủ cục bộ trước khi commit.\n- Giữ **sàn 80% line coverage**, nhưng được kiểm tra **thủ công trên máy của\n  lập trình viên trước mỗi lần commit** — chạy test và đo độ phủ, rồi mới\n  commit.\n- Công cụ: backend Spring Boot dùng **JaCoCo qua `mvn verify`**; phần Node\n  dùng **`npm test` kèm đo coverage**.\n- **Không có CI.** Việc chạy test và kiểm độ phủ hoàn toàn phụ thuộc kỷ luật\n  thủ công trước khi commit."
    }
  ],
  "obligations": {
    "strategy": "standard",
    "strategy_volume": [
      "Five to eight tests per component.",
      "Unit tests plus integration tests for key boundaries.",
      "Add E2E, performance, or security tests when requirements demand them."
    ],
    "scope_floor": [
      "Keep the existing test suite green.",
      "This scope adds no extra new-test floor beyond the selected test strategy."
    ],
    "combination_rule": "Apply every selected-strategy obligation and every scope-floor obligation; neither replaces the other, and a targeted scope regression may add the narrowest necessary test type beyond the strategy default."
  },
  "plan_profile": {
    "methodology": "test-after",
    "runner_step": "Verify the existing test runner/configuration and record the exact unit-scoped command.",
    "runner_ready_before_first_test": true,
    "testable_layers": [
      "Data model / database behavior",
      "Repository / data access",
      "Business logic",
      "API / endpoint",
      "Frontend behavior"
    ],
    "steps": [
      "Project structure and production configuration skeleton.",
      "Verify the existing test runner/configuration and record the exact unit-scoped command.",
      "Data model / database behavior - implement.",
      "Data model / database behavior - write and run its tests after implementation.",
      "Repository / data access - implement.",
      "Repository / data access - write and run its tests after implementation.",
      "Business logic - implement.",
      "Business logic - write and run its tests after implementation.",
      "API / endpoint - implement.",
      "API / endpoint - write and run its tests after implementation.",
      "Frontend behavior - implement.",
      "Frontend behavior - write and run its tests after implementation.",
      "Environment/build configuration.",
      "Documentation and traceability."
    ]
  },
  "input_sha256": "sha256:52668eb3c8bf7e056ba9b04127f40aee266e413d50cb4c7f118099f13105190a",
  "contract_sha256": "sha256:35ccee48aeec23012c9db61dd9bdd4ac6d9bfe02862ef0b0423c8392302d1932"
}
```

## Thay đổi theo yêu cầu người dùng

Theo yêu cầu của người dùng ("gen code cho tất cả các unit một lượt, phần test để
sau"), **mọi bước viết và chạy test trong kế hoạch này được DỜI sang một đợt sau**.
Trong đợt sinh mã lần này:

- KHÔNG tạo bất kỳ file test nào (không tạo `backend/src/test/java/com/mb/callassistant/auth/**`).
- KHÔNG chạy `mvn verify`, `mvn test` hay bất kỳ lệnh build/test nào.
- Các bước có nhãn **(DỜI — test)** dưới đây vẫn giữ nguyên trong kế hoạch để đợt
  test sau thực hiện; chúng mô tả đúng việc test-after theo từng lớp.

Lưu ý ràng buộc không được nới: **sàn độ phủ 80% dòng cho package
`com.mb.callassistant.auth` KHÔNG bị hạ** — Step 11 vẫn thêm package này vào khối
include của JaCoCo với mức 0.80. Sàn chỉ được kiểm chứng ở đợt test sau. Phương
pháp test-after theo `## Testing Contract` ở trên vẫn là nguồn chân lý; mục này chỉ
dời thời điểm thực thi các bước test, không đổi phương pháp và không hạ mục tiêu
chất lượng.

## Steps

- [x] **Step 1 — Dựng package `auth` và mở rộng khối cấu hình `Auth`** *(cấu trúc/cấu hình)*
  - Tạo package `backend/src/main/java/com/mb/callassistant/auth/`.
  - Mở rộng `config/AppProperties.Auth`: giữ `mode` (mặc định `demo`); thêm trường
    `sessionTtlHours` (mặc định 8, cấu hình được, BR2.1) và tùy chọn
    `failDelayMs` (mặc định 0, BR5.2) kèm getter/setter theo đúng kiểu đang có.
    **Không** thêm mật khẩu/token thô vào property.
  - BR: BR2.1, BR3.1. File: `config/AppProperties.java`.

- [x] **Step 2 — Thực thể `AuthSession` + repository** *(lớp mô hình — implement)*
  - `auth/AuthSession.java`: `@Entity @Table(name="auth_session")` với `sessionId` (PK),
    `rmId` (FK logic tới `RmUser.rmId`), `tokenHash` (unique, **chỉ băm SHA-256**,
    không lưu token thô), `createdAt`, `expiresAt`, `revokedAt` (nullable). Phương
    thức `isValid(OffsetDateTime now)` = `revokedAt == null && now < expiresAt`
    (bất biến ở FD §3). Không đưa `tokenHash` vào `toString`/log (BR5.1).
  - `auth/AuthSessionRepository.java` extends `JpaRepository<AuthSession,String>`:
    `Optional<AuthSession> findByTokenHash(String tokenHash)`.
  - BR: BR2.1, BR2.2, BR5.1. Files: `auth/AuthSession.java`, `auth/AuthSessionRepository.java`.

- [x] **Step 3 — Xác minh test runner + ghi lệnh test phạm vi unit** *(test runner readiness)*
  - Brownfield: xác nhận lệnh test phạm vi U6 chạy được trước bước test-first đầu
    tiên. Ghi lệnh chính xác, phạm vi **chỉ** package `auth`, vào
    `unit-test-instructions.md`:
    `mvn -f backend/pom.xml -Dtest="com.mb.callassistant.auth.**.*Test" verify`.
  - KHÔNG chạy lệnh trong đợt này (xem "Thay đổi theo yêu cầu người dùng"); chỉ ghi nhận.
  - BR: — (hạ tầng test). File: `unit-test-instructions.md`.

- [x] **Step 4 — Dịch vụ token & phiên `AuthTokenService`** *(lớp nghiệp vụ — implement)*
  - `auth/AuthTokenService.java` (`@Service`): `issue(RmUser)` sinh token Bearer ngẫu
    nhiên ≥ 32 byte bằng `SecureRandom` (Base64 url-safe không đệm), tính
    `tokenHash = sha256(token)`, tạo `AuthSession` với `expiresAt = now + sessionTtlHours`
    (BR2.1), lưu, trả về cặp `{ rawToken, session }` (token thô chỉ trả **một lần**).
  - `resolve(String rawToken): Optional<RmUser>` — tra `findByTokenHash(sha256(token))`,
    áp bất biến `isValid` (BR2.2), trả RM của `rmId`; ngược lại rỗng.
  - `revoke(String rawToken)` — đặt `revokedAt = now` cho phiên khớp (BR2.3), idempotent.
  - Băm SHA-256 trong một tiện ích `auth/TokenHasher.java`; không log token thô/băm (BR5.1).
  - BR: BR2.1, BR2.2, BR2.3, BR5.1. Files: `auth/AuthTokenService.java`, `auth/TokenHasher.java`.

- [x] **Step 5 — Bản `login` của `CurrentRmProvider`** *(lớp nghiệp vụ — implement)*
  - `auth/LoginCurrentRmProvider.java` (`@Component`,
    `@ConditionalOnProperty(name="app.auth.mode", havingValue="login")`): implement
    `com.mb.callassistant.lead.CurrentRmProvider`. `currentRm()` đọc token Bearer của
    request hiện tại (qua `RequestContextHolder`/`HttpServletRequest` tiêm vào), gọi
    `AuthTokenService.resolve`; thiếu/sai/hết hạn → ném `UnauthorizedException` để biên
    REST trả 401 (pseudocode FD §6, BR2.2). Không đổi `DemoCurrentRmProvider`
    (`matchIfMissing=true`) nên **chế độ demo vẫn là mặc định** (ADR-007, BR3.1).
  - `web/UnauthorizedException.java` (nếu chưa có) + ánh xạ 401 `UNAUTHORIZED` trong
    `web/ApiExceptionHandler.java` theo ErrorEnvelope (thông điệp tiếng Việt).
  - BR: BR2.2, BR3.1. Files: `auth/LoginCurrentRmProvider.java`, `web/UnauthorizedException.java`, `web/ApiExceptionHandler.java`.

- [x] **Step 6 — Kiểm mật khẩu BCrypt + seed tài khoản RM login** *(lớp nghiệp vụ — implement)*
  - `auth/PasswordHasher.java` (`@Component`): bọc `BCryptPasswordEncoder`
    (`spring-security-crypto`, thêm ở Step 11) — `matches(raw, hash)` dùng ở đăng nhập.
  - `auth/LoginRmSeeder.java` (`CommandLineRunner`, chỉ chạy khi `app.auth.mode=login`
    qua `@ConditionalOnProperty`): seed vài tài khoản RM mẫu với `passwordHash` BCrypt
    (dùng `RmUser.setPasswordHash`), idempotent theo `username`. **Mật khẩu demo chỉ
    lấy từ biến môi trường/README**, không hardcode bí mật ngoài giá trị seed rõ-ràng-là-demo.
    `RmUser` đã tồn tại từ U1 (U1 sở hữu bảng) — U6 chỉ bổ sung `passwordHash` nếu còn thiếu.
  - BR: BR1.1, BR5.1. Files: `auth/PasswordHasher.java`, `auth/LoginRmSeeder.java`.

- [ ] **Step 6T — (DỜI — test) Test xác thực mật khẩu + token/phiên**
  - `PasswordHasher.matches` đúng/sai; `AuthTokenService.issue` sinh token ≥ 32 byte,
    chỉ lưu băm, `expiresAt = createdAt + ttl`; `resolve` trả rỗng khi hết hạn/đã thu hồi;
    `revoke` idempotent.
  - BR: BR1.1, BR2.1, BR2.2, BR2.3, BR5.1. Files (đợt sau):
    `backend/src/test/java/com/mb/callassistant/auth/AuthTokenServiceTest.java`,
    `.../auth/PasswordHasherTest.java`.

- [x] **Step 7 — Lọc token Bearer theo request** *(lớp API — implement)*
  - `auth/BearerTokenFilter.java` (`OncePerRequestFilter`,
    `@ConditionalOnProperty(name="app.auth.mode", havingValue="login")`): đọc header
    `Authorization: Bearer <token>`, đặt token vào `AuthContext` (ThreadLocal theo
    request) để `LoginCurrentRmProvider` dùng; không chặn request ở đây (biên REST tự
    ném 401 khi cần currentRm). Đăng ký filter qua `@Bean FilterRegistrationBean`
    hoặc `@Component`. Ở chế độ demo filter không hoạt động.
  - `auth/AuthContext.java`: ThreadLocal giữ token thô của request, xóa ở `finally`.
  - BR: BR2.2. Files: `auth/BearerTokenFilter.java`, `auth/AuthContext.java`.

- [x] **Step 8 — Điểm vào đăng nhập/đăng xuất `AuthController`** *(lớp API — implement)*
  - `auth/AuthController.java` (`@RestController @RequestMapping("/api/auth")`):
    - `POST /login` — body `{ username, password }` (`@Valid`, `maxLength` theo C5);
      tra `RmUserRepository.findByUsername`; `PasswordHasher.matches` (BR1.1). Đúng →
      `AuthTokenService.issue`, trả `200 { token, expiresAt, rm }` (dùng DTO `Rm.from`,
      không lộ passwordHash). Sai (username không có HOẶC password sai) → `401` với
      **một** thông điệp `Sai tên đăng nhập hoặc mật khẩu` (BR1.2); tùy chọn áp
      `failDelayMs` (BR5.2). Thiếu trường → `400 VALIDATION_FAILED`.
    - `POST /logout` — thu hồi phiên của token hiện tại (BR2.3), trả `204`.
  - DTO `auth/LoginRequest.java`, `auth/LoginResponse.java` (`token`, `expiresAt`, `rm`).
  - BR: BR1.1, BR1.2, BR2.1, BR2.3. Files: `auth/AuthController.java`, `auth/LoginRequest.java`, `auth/LoginResponse.java`.

- [x] **Step 9 — Điểm vào danh sách lead `GET /api/leads`** *(lớp API — implement)*
  - `web/LeadListController.java` (`@RestController @RequestMapping("/api")`): phương
    thức `@GetMapping("/leads")` (ĐÚNG collection path; U1 vẫn sở hữu `/demo`, `/next`,
    `/{leadId}` — path literal được ưu tiên nên không nhập nhằng). Lấy `rmId` qua
    `CurrentRmProvider.currentRmId()` (chế độ login → RM của token, BR2.2/BR4.1), gọi
    `leadService.findLeadsByRm(rmId)` rồi map `LeadSummary.from`.
  - **Sắp xếp (BR4.2)**: KHAI_THAC_NGAY > HOT_LEAD > THUONG rồi `slaDueAt` tăng dần.
    Kho hiện có `findByOwnerRmIdOrderByPriorityAscSlaDueAtAsc` sắp theo *thứ tự khai báo
    enum* `LeadPriority`. Bước này **phải xác minh** thứ tự khai báo của
    `LeadPriority` khớp (KHAI_THAC_NGAY, HOT_LEAD, THUONG); nếu khớp thì dùng truy vấn
    sẵn có, nếu không thì sắp lại trong controller/service bằng comparator theo đúng ưu
    tiên nghiệp vụ (không đổi enum do U1 sở hữu). Danh sách rỗng → trả `[]` (BR4.3);
    giao diện hiển thị "Chưa có tiềm năng bán".
  - BR: BR4.1, BR4.2, BR4.3. File: `web/LeadListController.java`.

- [ ] **Step 9T — (DỜI — test) Test filter 401 + API login/logout + GET /api/leads**
  - MockMvc chế độ login: login đúng → 200 có token/expiresAt/rm (không có passwordHash);
    sai username / sai password → 401 cùng một thông điệp; thiếu trường → 400;
    request không token tới `GET /api/leads` → 401; token hết hạn/đã logout → 401;
    `GET /api/leads` chỉ trả lead của RM hiện tại (BR4.1), đúng thứ tự ưu tiên+SLA
    (BR4.2), rỗng → `[]` (BR4.3). Chế độ demo: `GET /api/leads` vẫn chạy với RM mẫu.
  - BR: BR1.1, BR1.2, BR2.2, BR2.3, BR4.1, BR4.2, BR4.3. Files (đợt sau):
    `backend/src/test/java/com/mb/callassistant/auth/AuthControllerTest.java`,
    `.../auth/BearerTokenFilterTest.java`,
    `backend/src/test/java/com/mb/callassistant/web/LeadListControllerTest.java`.

- [x] **Step 10 — Giao diện: màn S0 đăng nhập + cột danh sách lead** *(lớp frontend — implement)*
  - `public/screens/0-dang-nhap.html`: màn S0 theo hệ thiết kế của mockup gốc (phong
    cách `1-chi-tiet-tiem-nang-ban.html`): ô `username`, `password`, nút **Đăng nhập**
    (vô hiệu + vòng xoay khi đang gửi), dòng lỗi kèm icon (không chỉ dựa màu) hiển thị
    `Sai tên đăng nhập hoặc mật khẩu` (BR1.2). Thêm `data-testid` cho các phần tử
    tương tác (`login-username`, `login-password`, `login-submit`, `login-error`).
  - Màn danh sách lead (cột trên S1): mỗi lead hiện `leadCode`, sản phẩm, nguồn, SLA
    còn lại, `rankScore`/10, nhãn ưu tiên, `stage`; rỗng → "Chưa có tiềm năng bán"
    (BR4.3). `data-testid` dạng `lead-list`, `lead-row-{leadId}`.
  - `public/js/api.js`: thêm `login`, `logout`, `listLeads`; khi có token lưu (bộ nhớ
    phiên trình duyệt) thì gắn header `Authorization: Bearer <token>`; bắt lỗi `401`
    → xóa token và quay về S0 **chỉ ở chế độ login** (AC4.1.3); chế độ demo không gắn
    token và không chuyển hướng.
  - BR: BR1.2, BR2.2, BR4.1, BR4.3. Files: `public/screens/0-dang-nhap.html`,
    `public/screens/leads-list.html` (hoặc cột tích hợp theo mockup), `public/js/api.js`.

- [ ] **Step 10T — (DỜI — test) Test hành vi frontend (Node coverage)**
  - Theo team.md: phần Node dùng `npm test` kèm đo coverage. Kiểm: `api.js` gắn
    `Authorization` khi có token; `401` ở chế độ login xóa token + về S0; render danh
    sách rỗng hiện "Chưa có tiềm năng bán". (Nếu chưa có runner Node cho `public/`,
    đợt test sau dựng tối thiểu trước bước test-first đầu tiên.)
  - BR: BR1.2, BR2.2, BR4.3. Files (đợt sau): `public/js/__tests__/api.test.*`.

- [x] **Step 11 — Phụ thuộc, cấu hình môi trường & build** *(env/build config)*
  - `backend/pom.xml`: thêm `org.springframework.security:spring-security-crypto`
    (BCrypt) **chỉ khi chưa có**; **không pin version** (quản bởi Spring Boot BOM 3.3.4).
  - Mở rộng khối JaCoCo `includes` trong `backend/pom.xml`: thêm
    `com.mb.callassistant.auth.*` vào rule BUNDLE giữ sàn `0.80` — **không hạ sàn**
    (Testing Contract + team.md).
  - `application.properties` / `application-demo.properties`: thêm
    `app.auth.session-ttl-hours=${AUTH_SESSION_TTL_HOURS:8}` (BR2.1) và (tùy chọn)
    `app.auth.fail-delay-ms=${AUTH_FAIL_DELAY_MS:0}` (BR5.2); giữ `app.auth.mode`
    mặc định `demo`. Mật khẩu seed login chỉ qua env (ví dụ `AUTH_DEMO_PASSWORD`),
    nêu trong README/`.env.example`, không commit giá trị thật.
  - BR: BR1.1, BR2.1, BR3.1, BR5.1. Files: `backend/pom.xml`, `application.properties`,
    `application-demo.properties`, `backend/.env.example`.

- [x] **Step 12 — Tài liệu & traceability** *(docs & traceability)*
  - JavaDoc trên provider/service/controller theo kiểu file hiện có; cập nhật
    `backend/README.md` một đoạn ngắn: bật `app.auth.mode=login`, biến
    `AUTH_SESSION_TTL_HOURS`, tài khoản RM mẫu + nguồn mật khẩu (env), luồng
    `Authorization: Bearer`.
  - Ở Step 5 của stage (sau sinh mã) tạo `source-manifest.json` và `traceability.json`
    cho mọi AC/BR của U6 (AC4.1.1–AC4.1.3, AC4.2.1, AC4.2.3; AC4.2.2 là N/A — do U1).
  - BR: BR5.1 (ghi chú bảo mật README). Files: `backend/README.md`,
    (ở Step 5 stage) `source-manifest.json`, `traceability.json`.

## Story traceability

| Step | Story / AC | BR |
|---|---|---|
| Step 1 | US4.1 (nền cấu hình phiên) | BR2.1, BR3.1 |
| Step 2 | US4.1 (AC4.1.1, AC4.1.3) — phiên/token | BR2.1, BR2.2, BR5.1 |
| Step 4 | US4.1 (AC4.1.1, AC4.1.3) — cấp/giải token | BR2.1, BR2.2, BR2.3, BR5.1 |
| Step 5 | US4.1 (AC4.1.3), US4.2 (nền RM hiện tại) | BR2.2, BR3.1 |
| Step 6 | US4.1 (AC4.1.1, AC4.1.2) — mật khẩu BCrypt | BR1.1, BR5.1 |
| Step 7 | US4.1 (AC4.1.3) — token theo request | BR2.2 |
| Step 8 | US4.1 (AC4.1.1, AC4.1.2) — login/logout | BR1.1, BR1.2, BR2.1, BR2.3 |
| Step 9 | US4.2 (AC4.2.1, AC4.2.3) — danh sách lead | BR4.1, BR4.2, BR4.3 |
| Step 10 | US4.1 (AC4.1.2, AC4.1.3), US4.2 (AC4.2.1, AC4.2.3) — S0 + danh sách | BR1.2, BR2.2, BR4.1, BR4.3 |
| Step 11 | US4.1, US4.2 (cấu hình chạy + sàn phủ) | BR1.1, BR2.1, BR3.1, BR5.1 |
| Step 12 | tất cả (bàn giao) | BR5.1 |

Ghi chú ranh giới: **AC4.2.2** (403 khi RM khác gọi `GET /api/leads/{leadId}`) do U1
thực thi (BR1.1 của U1); U6 không sở hữu path đó nên không nhân bản — đánh dấu
`N/A` trong traceability (khớp `functional-design/traceability.json`).

## Sources

- `.kiro/aidlc-common/stages/construction/code-generation.md` (Step 2)
- `construction/u6-login-lead-list/functional-design/{functional-spec.md, rules.md, entities.md, traceability.json}` (U6)
- `inception/units-generation/unit-of-work.md` (U6)
- `inception/contract-design/contract-summary.md` (C1, C5 `/api/auth/login` · `/api/auth/logout` · `/api/leads`, I1 `CurrentRmProvider`)
- `inception/domain-design/decisions.md` (ADR-002 CORS, ADR-007 demo/login bằng cấu hình)
- `inception/user-stories/stories.md` (US4.1 AC4.1.1–AC4.1.3, US4.2 AC4.2.1–AC4.2.3)
- `inception/refined-mockups/mockups.md` (S0 đăng nhập, cột danh sách lead S1)
- Mã hiện có: `lead/{RmUser.java, RmUserRepository.java, CurrentRmProvider.java, DemoCurrentRmProvider.java, LeadService.java, LeadRepository.java, LeadPriority.java}`, `web/{LeadController.java, ApiExceptionHandler.java, dto/Rm.java, dto/LeadSummary.java}`, `config/{AppProperties.java, ApplicationConfig.java, DemoDataSeeder.java}`, `backend/pom.xml`, `application*.properties`, `public/js/{api.js, config.js}`, `public/screens/1-chi-tiet-tiem-nang-ban.html`

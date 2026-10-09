# Unit Test Instructions — U2 `u2-llm-client`

> Lưu ý: Theo yêu cầu người dùng, việc viết và chạy test được **dời sang một đợt
> sau**; tài liệu này mô tả đầy đủ cách test-after để đợt đó thực hiện. Sàn độ
> phủ 80% dòng cho package U2 **không bị hạ**.

## Framework setup

- **Bộ test**: JUnit5 (Jupiter) + Mockito + AssertJ — đã có qua
  `spring-boot-starter-test` trong `backend/pom.xml` (phiên bản do parent BOM
  quản lý, không ghim).
- **Giả lập HTTP**: dùng **MockWebServer** (OkHttp) để giả lập Anthropic
  Messages API; **không gọi mạng thật** trong unit test (NFR4.1). MockWebServer
  chưa có trong `pom.xml` — ở đợt test, thêm phụ thuộc test-scope
  `com.squareup.okhttp3:mockwebserver` (ghim phiên bản ổn định tương thích
  Java 17, ví dụ `4.12.0`). Thay thế: dựng `WebClient` trỏ vào base URL của
  MockWebServer và truyền vào adapter trong test (constructor injection như
  `GoogleSttClient`).
- **Cấu hình test**: tạo `LlmClient` với `WebClient` trỏ `mockWebServer.url("/")`
  và `AppProperties.Llm` đặt `baseUrl` = URL đó; đặt/để trống env
  `ANTHROPIC_API_KEY` tùy ca test (dùng cơ chế đặt biến môi trường/`System`
  property trong test, không ghi khóa thật).
- Mục `src/test/java/com/mb/callassistant/llm/` là nơi đặt mọi file test của U2.

## Chạy test của riêng unit U2

Chạy **chỉ** các test của U2 (phạm vi package `com.mb.callassistant.llm`), kèm
báo cáo JaCoCo:

```
mvn -f backend/pom.xml -Dtest="com.mb.callassistant.llm.**.*Test" verify
```

- Đây là lệnh phạm vi unit; **không** dùng lệnh chạy toàn bộ suite.
- Trong IntelliJ: mở **Maven tool window** → `call-assistant` → **Lifecycle** →
  `verify` (hoặc chuột phải một lớp test → *Run*). Có thể dán filter
  `-Dtest=com.mb.callassistant.llm.**.*Test` vào ô *Run Maven Goal*.
- Báo cáo độ phủ sau `verify`: `backend/target/site/jacoco/index.html` — kiểm thủ
  công trước khi commit (team.md: JaCoCo thủ công, không CI).

## Expected coverage targets

- **Sàn ≥ 80% dòng (LINE COVEREDRATIO)** cho package `com.mb.callassistant.llm`,
  do rule JaCoCo BUNDLE trong `backend/pom.xml` kiểm ở pha `verify`
  (BR cũ của team.md / NFR4.1). Không được hạ sàn hay loại lớp khỏi include để
  cho qua.
- Phủ đủ các nhánh nghiệp vụ: validate đầu vào, NOT_CONFIGURED, timeout, 5 nhánh
  ánh xạ lỗi, kiểm JSON (`expectJson`), tách dữ liệu/chỉ dẫn, không-log, MOCK.

## Mocking / stubbing guidance

- **Adapter Anthropic**: MockWebServer trả lần lượt các kịch bản:
  - 200 + body hợp lệ có `content[].text`, `usage`, `stop_reason` ⇒ `LlmResponse`
    đầy đủ token + `truncated` khi `stop_reason=max_tokens`.
  - Trễ phản hồi > timeout cấu hình ⇒ `LlmException(TIMEOUT, retryable)`; khẳng
    định kết nối bị huỷ trong biên thời gian hợp lý.
  - 429 ⇒ `RATE_LIMITED`; 500/503/529 ⇒ `UNAVAILABLE`; 400/404 ⇒ `BAD_RESPONSE`;
    body rỗng/không đọc được ⇒ `BAD_RESPONSE`.
  - `expectJson=true`: body JSON có khung ```` ```json ```` ⇒ bóc được; không
    phải JSON ⇒ `BAD_RESPONSE`.
  - **NOT_CONFIGURED**: không đặt `ANTHROPIC_API_KEY` ⇒ `complete()` ném ngay,
    `mockWebServer.getRequestCount() == 0`.
  - **Không tự thử lại**: mỗi lần gọi lỗi ⇒ đúng 1 request
    (`getRequestCount() == 1`).
  - **Không log bí mật**: cài một appender bắt log (Logback `ListAppender`),
    khẳng định chuỗi "dấu vân tay" trong `userContent`/`text` và chuỗi khóa test
    **không** xuất hiện trong log; log có `purpose`, `model`, `latencyMs`.
  - **Tách dữ liệu**: đọc `RecordedRequest.getBody()`, khẳng định `userContent`
    nằm trong khối phân tách và không ở phần `system`.
- **Adapter MOCK**: không cần mạng; dùng kết quả cài sẵn theo `purpose`; test
  nhánh cài sẵn `LlmException`.
- Dùng Mockito cho các cộng tác viên nội bộ nếu cần; không mock `WebClient` bằng
  tay khi đã có MockWebServer.

## Test data

- Dùng dữ liệu giả, không PII thật (NFR7). "Dấu vân tay" trong prompt/response là
  chuỗi nhận dạng dễ tìm trong log, ví dụ `FINGERPRINT-USERCONTENT-7f3a` và
  `FINGERPRINT-RESPONSE-9c21`.
- Khóa test giả, ví dụ `sk-ant-TEST-doNotLog`, chỉ để kiểm "không log khóa";
  không bao giờ dùng khóa thật.
- Mẫu body Anthropic tối thiểu: `{"content":[{"type":"text","text":"..."}],
  "model":"claude-...","usage":{"input_tokens":10,"output_tokens":20},
  "stop_reason":"end_turn"}`.
- Mẫu JSON cho `expectJson`: một chuỗi JSON object hợp lệ và một biến thể bọc
  trong khung code fence để test bước bóc khung.

## Sources

- `.kiro/aidlc-common/stages/construction/code-generation.md` (Step 2 — unit test instructions)
- `nfr-requirements/{security-requirements.md, tech-stack-decisions.md}` (U2 — tiêu chí Pass/Fail, NFR4.1)
- `team.md` (Testing Posture: test-after, JaCoCo ≥ 80% thủ công, không CI)
- Mã hiện có: `backend/pom.xml` (JaCoCo), `service/GoogleSttClient.java` (khuôn test WebClient)

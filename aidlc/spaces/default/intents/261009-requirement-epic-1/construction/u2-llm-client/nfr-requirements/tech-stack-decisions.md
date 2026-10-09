# Tech Stack Decisions — U2 `u2-llm-client`

Các quyết định công nghệ cho thư viện cổng LLM (`kind: library`). U2 xây tiếp
trên ứng dụng Spring Boot hiện có (brownfield), nên ưu tiên dùng lại thành phần
sẵn có thay vì thêm phụ thuộc mới. Mỗi quyết định mang ID dẫn xuất `NFRx.y`,
kèm lý do, phương án bị loại và tiêu chí Pass/Fail khi áp dụng.

Nguồn công nghệ nền: `technology-stack.md` (Java 17, Spring Boot 3.3.4,
`spring-boot-starter-web`, `spring-boot-starter-webflux` với `WebClient`,
Jackson đi kèm BOM). Ràng buộc test: JUnit5 + Mockito, không gọi mạng thật
trong unit test (NFR4).

## TS1 — Ngôn ngữ & nền tảng

### NFR2.10 — Java 17 / Spring Boot 3.3.4 (dùng lại nền hiện có)
- **Quyết định**: Viết thư viện thành một package Java trong backend Spring Boot
  hiện có (ADR-001), nhắm JDK 17 và BOM `spring-boot-starter-parent:3.3.4`.
- **Lý do**: Khớp `java.version=17` và parent BOM đã khai trong `backend/pom.xml`;
  không phải thêm toolchain hay tiến trình mới.
- **Phương án bị loại**: Tách thành dịch vụ riêng (ADR-001 Alternatives) — thừa
  cho demo.
- **Pass/Fail**: Pass khi unit biên dịch dưới JDK 17 trong module backend hiện
  có mà không nâng/đổi parent BOM; Fail khi phải thêm runtime khác.
- **Nguồn**: `technology-stack.md`, ADR-001.

## TS2 — HTTP client gọi Anthropic

### NFR1.2 — Dùng HTTP client sẵn có của Spring (WebClient/RestClient), không thêm SDK Anthropic
- **Quyết định**: Gọi Anthropic Messages API bằng client HTTP đã có trong ứng
  dụng — `WebClient` (từ `spring-boot-starter-webflux` đã khai trong
  `technology-stack.md`), hoặc `RestClient` của Spring 6 nếu dùng đồng bộ —
  **không** thêm phụ thuộc Anthropic Java SDK.
- **Lý do**:
  - (+) Không tăng số phụ thuộc và bề mặt bảo trì; `webflux`/`WebClient` đã có
    sẵn.
  - (+) `WebClient` hỗ trợ timeout theo yêu cầu (phục vụ NFR1.1) và dễ giả lập
    bằng MockWebServer trong test.
  - (+) Toàn quyền kiểm soát header (`x-api-key`, `anthropic-version`), thân
    request và cách phân loại mã trạng thái (phục vụ NFR3.1).
  - (−) Phải tự dựng và bảo trì thân request/parse phản hồi của Messages API.
    Chấp nhận được vì bề mặt API nhỏ (một endpoint `messages`).
- **Phương án bị loại**:
  - Thêm Anthropic Java SDK: thêm phụ thuộc và ràng buộc phiên bản, trùng với
    phần xử lý khóa/timeout/lỗi mà ADR-004 muốn tập trung trong một adapter;
    cũng khó kiểm soát ánh xạ lỗi về 5 loại cố định.
  - Dùng `java.net.http.HttpClient` thuần: được, nhưng bỏ phí tiện ích timeout/
    codec/test của Spring đã có.
- **Pass/Fail**: Pass khi `backend/pom.xml` không có phụ thuộc SDK Anthropic và
  lời gọi đi qua client Spring; Fail khi thêm SDK bên thứ ba cho việc này.
- **Nguồn**: `technology-stack.md`, ADR-004.

### NFR1.3 — Timeout cấu hình được ở tầng client
- **Quyết định**: Cấu hình timeout của client theo `timeoutMs`/`defaultTimeoutMs`
  (30000 ms) cho mỗi lời gọi; hết hạn ⇒ huỷ và ánh xạ sang `TIMEOUT`.
- **Lý do**: Thực thi BR3.1/NFR1.1 ở đúng nơi phát request.
- **Pass/Fail**: Pass khi test MockWebServer trễ > timeout ⇒ nhận `TIMEOUT`
  trong biên thời gian hợp lý; Fail khi treo quá hạn.
- **Nguồn**: NFR1, FR3.6, BR3.1.

## TS3 — Xử lý JSON

### NFR3.3 — Jackson cho dựng request và kiểm JSON đọc được
- **Quyết định**: Dùng Jackson (đi kèm Spring BOM) để serialize thân request
  Messages API và để kiểm `expectJson` (parse `LlmResponse.text` thành JSON,
  cho phép bỏ khung code fence) theo BR4.2.
- **Lý do**: Jackson đã có sẵn; không cần thư viện JSON thứ hai.
- **Pass/Fail**: Pass khi kiểm JSON dùng Jackson và không thêm lib JSON khác;
  Fail khi nạp thêm phụ thuộc JSON trùng vai trò.
- **Nguồn**: `technology-stack.md`, BR4.2.

## TS4 — Giao diện và adapter

### NFR3.4 — Trừu tượng `LlmClient` + adapter Anthropic + adapter MOCK
- **Quyết định**: Bên gọi (U3/U4) chỉ phụ thuộc giao diện `LlmClient` (hợp đồng
  I3). Có hai hiện thực chọn bằng cấu hình `provider`: `ANTHROPIC` (thật) và
  `MOCK` (trả kết quả cài sẵn theo `purpose`, không gọi mạng) (BR6.1, ADR-004).
- **Lý do**: Đổi model/nhà cung cấp chỉ sửa một chỗ; test và demo chạy không cần
  khóa hay mạng (phục vụ NFR4).
- **Pass/Fail**: Pass khi U3/U4 chỉ tham chiếu `LlmClient` và `provider=MOCK`
  cho ra kết quả định sẵn mà không mở kết nối; Fail khi bên gọi phụ thuộc trực
  tiếp adapter Anthropic.
- **Nguồn**: ADR-004, BR6.1, I3.

### NFR3.5 — Prompt caching đánh dấu phần cố định (best-effort)
- **Quyết định**: Khi có `cachedContext`, adapter Anthropic đánh dấu
  `systemPrompt + cachedContext` là phần có thể cache; adapter MOCK bỏ qua
  (BR5.2).
- **Lý do**: Giảm chi phí/độ trễ cho phần nội dung lặp lại; không bắt buộc nên
  không gây lỗi khi nhà cung cấp không hỗ trợ.
- **Pass/Fail**: Pass khi body gửi Anthropic đánh dấu phần cache khi có
  `cachedContext` (test qua MockWebServer); không áp dụng cho MOCK.
- **Nguồn**: ADR-004, BR5.2.

## TS5 — Kiểm thử

### NFR4.1 — JUnit5 + Mockito, giả lập HTTP, không mạng thật
- **Quyết định**: Unit test dùng JUnit5 + Mockito; mọi lời gọi LLM được giả lập
  bằng MockWebServer (hoặc tương đương) — không request mạng thật trong unit
  test. Nhắm độ phủ dòng ≥ 80%, đo thủ công bằng JaCoCo qua `mvn verify` trước
  khi commit (team practices, NFR4).
- **Lý do**: Khớp thực hành nhóm (test-after, JaCoCo, không CI) và yêu cầu giả
  lập STT/LLM trong test (NFR4).
- **Pass/Fail**: Pass khi suite chạy offline và báo cáo JaCoCo cho package của
  unit đạt ≥ 80% dòng; Fail khi test phụ thuộc endpoint Anthropic thật.
- **Nguồn**: NFR4, `team.md` Testing Posture, `technology-stack.md`.

## TS6 — Lệnh/kịch bản áp dụng
- Build & test cục bộ chạy đúng như nhóm đã chốt: `mvn verify` (kèm JaCoCo).
  Không có CI; việc chạy test và đo độ phủ là thủ công trước mỗi commit.

## Lưu ý chuyển giao cho Code Generation (từ Functional Design — advisory)
- **I3 trả lỗi vs ném lỗi**: `functional-spec.md`/`rules.md` mô tả "ném
  `LlmError`" trong khi I3 ở `contract-summary.md` ghi "errors:
  `LlmException(kind=…)`". Code Generation phải thống nhất theo cách đặt tên của
  I3 (lớp `LlmException` với enum `kind`), hoặc ghi rõ quyết định thống nhất.
- **Đặt tên trường**: `purpose` và `timeoutMs` dùng trong FD; I3 nêu `timeout`.
  Code Generation phải khớp đúng tên theo I3 khi hiện thực `LlmRequest`.
- Đây chỉ là ghi chú; **không** sửa file Functional Design ở stage này.

## Bảng tóm tắt quyết định

| ID | Quyết định | Phương án bị loại | Nguồn |
|---|---|---|---|
| NFR2.10 | Java 17 / Spring Boot 3.3.4, package trong backend | Dịch vụ riêng | ADR-001 |
| NFR1.2 | WebClient/RestClient sẵn có, không thêm SDK | Anthropic SDK; HttpClient thuần | ADR-004 |
| NFR1.3 | Timeout cấu hình ở client, mặc định 30 s | — | BR3.1 |
| NFR3.3 | Jackson cho JSON | Thêm lib JSON khác | BR4.2 |
| NFR3.4 | `LlmClient` + adapter ANTHROPIC/MOCK | Bên gọi tự gọi SDK | ADR-004, BR6.1 |
| NFR3.5 | Đánh dấu prompt caching (best-effort) | — | BR5.2 |
| NFR4.1 | JUnit5 + Mockito + MockWebServer, JaCoCo ≥ 80% | Test chạm mạng thật | NFR4 |

## Sources

- `.kiro/aidlc-common/stages/construction/nfr-requirements.md` (Step 5 format)
- `functional-spec.md`, `rules.md`, `entities.md` (U2)
- `requirements.md` (NFR1, NFR3, NFR4, FR3.6)
- `contract-summary.md` (I3 `LlmClient`, ghi chú đặt tên)
- `decisions.md` (ADR-001, ADR-004)
- `technology-stack.md` (codekb: Java 17, Spring Boot 3.3.4, WebClient, Jackson)
- `team.md` (Testing Posture: test-after, JaCoCo, không CI)

# Code Generation Plan — U3 `u3-copilot`

## Summary

- Builds: Copilot tra cứu chính sách (CopilotComponent, hợp đồng C2) trong package `com.mb.callassistant.copilot` — nạp + lập chỉ mục tài liệu text/md khi khởi động (`PolicyDocument`/`DocumentChunk`), tìm từ khoá không phân biệt dấu, dựng prompt khối DATA và gọi U2 `LlmClient` (expectJson, 30 giây), kiểm chứng trích dẫn, nhánh "không tìm thấy" không gọi LLM, `502 AI_ERROR` phân biệt `NOT_CONFIGURED`, ghi `CopilotQueryLog`, hai endpoint `POST /api/copilot/ask` + `GET /api/copilot/documents`, và panel Copilot trên màn S2.
- Touches: thêm package mới `backend/src/main/java/com/mb/callassistant/copilot/**` và controller `web/CopilotController.java`; mở rộng `config/AppProperties.java` (khối `Copilot`); thêm `app.copilot.*` vào `backend/src/main/resources/application.properties` và `application-demo.properties`; thêm 1–2 file SAMPLE trong `backend/policy-docs/`; mở rộng khối JaCoCo `includes` trong `backend/pom.xml` cho `com.mb.callassistant.copilot.*`; lắp panel Copilot vào slot dành sẵn ở `public/screens/2-thuc-hien-cuoc-goi.html` và thêm `public/js/copilot.js` + hàm API trong `public/js/api.js`.
- Tests: JUnit5 + Mockito (mock `LlmClient`, thư mục tài liệu tạm) cho mọi lớp (nạp tài liệu, tìm từ khoá không dấu, nhánh rỗng, kiểm chứng trích dẫn, lỗi/timeout LLM → 502, JSON sai cấu trúc, log OKR, validation biên) + sàn JaCoCo ≥ 80% dòng cho package U3 — DỜI sang đợt sau theo yêu cầu người dùng (xem mục bên dưới); không tạo file test ở đợt này.

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

Theo yêu cầu của người dùng ("gen code cho tất cả các unit một lượt, phần test để sau"), **mọi bước viết và chạy test trong kế hoạch này được DỜI sang một đợt sau**. Trong đợt sinh mã lần này:

- KHÔNG tạo bất kỳ file test nào (không tạo `backend/src/test/java/com/mb/callassistant/copilot/**`).
- KHÔNG chạy `mvn verify`, `mvn test`, `npm test` hay bất kỳ lệnh build/test nào.
- Các bước có nhãn **(DỜI — test)** dưới đây vẫn giữ nguyên trong kế hoạch để đợt test sau thực hiện; chúng mô tả đúng việc test-after theo từng lớp.

Lưu ý ràng buộc không được nới: **sàn độ phủ 80% dòng cho package `com.mb.callassistant.copilot` KHÔNG bị hạ.** Nó chỉ được kiểm chứng ở đợt test sau. Phương pháp test-after theo `## Testing Contract` ở trên vẫn là nguồn chân lý; mục này chỉ dời thời điểm thực thi các bước test, không đổi phương pháp và không hạ mục tiêu chất lượng.

## Steps

- [x] **Step 1 — Dựng cấu trúc package và khung cấu hình** *(cấu trúc/cấu hình)*
  - Tạo package `backend/src/main/java/com/mb/callassistant/copilot/`.
  - Mở rộng `backend/src/main/java/com/mb/callassistant/config/AppProperties.java`: thêm lớp lồng `Copilot` với các trường `docsDir` (mặc định `backend/policy-docs`), `topK` (mặc định `5`), `maxHistoryTurns` (mặc định `10`), `maxQuestionLength` (mặc định `1000`), `llmTimeoutMs` (mặc định `30000`), `maxOutputTokens` (mặc định `1024`) kèm getter/setter theo đúng khuôn khối `Llm`/`Stt` đang có.
  - BR: BR1.1, BR2.2, BR5.1, BR6.2, BR7.2. File: `config/AppProperties.java`.

- [x] **Step 2 — Xác minh test runner + ghi lệnh test phạm vi unit** *(test runner readiness)*
  - Brownfield: ghi lệnh test phạm vi chỉ U3 vào `unit-test-instructions.md`:
    `mvn -f backend/pom.xml -Dtest="com.mb.callassistant.copilot.**.*Test" verify`.
  - KHÔNG chạy lệnh trong đợt này (xem "Thay đổi theo yêu cầu người dùng"); chỉ ghi nhận.
  - BR: NFR4. File: `unit-test-instructions.md`.

- [x] **Step 3 — Thực thể & value object (lớp mô hình)** *(Data model — implement)*
  - `copilot/PolicyDocument.java`: thực thể lưu bền `documentId`, `title`, `effectiveDate` (LocalDate), `sourcePdfPath?`, `extractedTextPath`, `pageCount?`, `loadedAt` (OffsetDateTime +07:00).
  - `copilot/DocumentChunk.java`: value object `documentId`, `chunkIndex`, `page?`, `section?`, `text`, `normalizedText` (ràng buộc: có ít nhất một trong `page`/`section`).
  - `copilot/RetrievedChunk.java`: value object `documentId`, `chunkIndex`, `score` (> 0), `page?`, `section?`.
  - `copilot/Citation.java`: value object `documentId?`, `documentTitle`, `page?`, `section?`, `effectiveDate` (ít nhất một trong page/section).
  - `copilot/CopilotAnswer.java`: value object `answerFound`, `answer`, `citations[]`, `suggestions[]`.
  - `copilot/CopilotQueryLog.java`: thực thể lưu bền `queryId`, `rmId`, `callId?`, `question`, `answerFound`, `citedDocumentIds[]`, `askedAt`.
  - Hằng số câu "không tìm thấy": `copilot/CopilotMessages.NOT_FOUND = "Không tìm thấy thông tin này trong tài liệu chính sách hiện có"` (BR3.3 — chuỗi cố định đúng từng chữ).
  - BR: BR1.1, BR1.3, BR3.3, BR4.1, BR7.1. AC: AC1.2.1, AC1.2.2.
  - Files: `copilot/PolicyDocument.java`, `copilot/DocumentChunk.java`, `copilot/RetrievedChunk.java`, `copilot/Citation.java`, `copilot/CopilotAnswer.java`, `copilot/CopilotQueryLog.java`, `copilot/CopilotMessages.java`.

- [ ] **Step 3T — (DỜI — test) Test lớp mô hình**
  - Ràng buộc: `DocumentChunk`/`Citation` yêu cầu ít nhất page hoặc section; `RetrievedChunk.score > 0`; `CopilotAnswer` nhánh không-tìm-thấy ⇒ câu cố định + citations rỗng.
  - BR: BR1.3, BR3.3, BR4.1. File (đợt sau): `copilot/CopilotModelTest.java`.

- [x] **Step 4 — Repository tài liệu & nhật ký (lớp truy cập dữ liệu)** *(Repository — implement)*
  - `copilot/PolicyDocumentRepository.java` + `copilot/CopilotQueryLogRepository.java`: bản lưu trong bộ nhớ (in-memory, theo khuôn `CallRecordRepository`), vì tài liệu nạp một lần lúc khởi động và log chỉ để đếm OKR cho demo.
  - `PolicyDocumentRepository` giữ danh sách `PolicyDocument` và chỉ mục `DocumentChunk` đã lập.
  - BR: BR7.1. AC: AC1.3.2 (qua `GET /api/copilot/documents`).
  - Files: `copilot/PolicyDocumentRepository.java`, `copilot/CopilotQueryLogRepository.java`.

- [ ] **Step 4T — (DỜI — test) Test repository**
  - Lưu/đọc `PolicyDocument` và `CopilotQueryLog`; liệt kê tài liệu theo thứ tự nạp.
  - BR: BR7.1. File (đợt sau): `copilot/CopilotRepositoryTest.java`.

- [x] **Step 5 — Nạp & lập chỉ mục tài liệu (lớp nghiệp vụ)** *(Business logic — implement)*
  - `copilot/TextNormalizer.java`: bỏ dấu tiếng Việt (NFKD + loại dấu), hạ thường, tách từ — dùng chung cho `normalizedText` của đoạn và chuẩn hoá câu hỏi (BR2.1).
  - `copilot/PolicyDocumentLoader.java` (`@Component`, chạy khi khởi động qua `ApplicationRunner`/`@PostConstruct`): đọc `app.copilot.docs-dir`; với mỗi `.md`/`.txt`: đọc khối metadata (`title`, `effectiveDate` bắt buộc; `sourcePdf` tuỳ chọn); thiếu/không đọc được/rỗng ⇒ ghi log tên file + bỏ qua (BR1.1, BR1.2); hợp lệ ⇒ tạo `PolicyDocument`, cắt `DocumentChunk` theo dấu `[[page:n]]`/`## Mục`, sinh `normalizedText` (BR1.3). Ghi log tổng số nạp + số bỏ qua; chỉ mục rỗng ⇒ mọi câu hỏi trả "không tìm thấy".
  - BR: BR1.1, BR1.2, BR1.3, BR2.1. AC: AC1.2.3.
  - Files: `copilot/TextNormalizer.java`, `copilot/PolicyDocumentLoader.java`.

- [ ] **Step 5T — (DỜI — test) Test nạp & chuẩn hoá**
  - Thư mục tạm với file hợp lệ, file thiếu metadata, file rỗng ⇒ chỉ file hợp lệ thành `PolicyDocument`, số còn lại được bỏ qua; `TextNormalizer` khớp "lai suat" ⇔ "lãi suất".
  - BR: BR1.1, BR1.2, BR2.1. AC: AC1.2.3. File (đợt sau): `copilot/PolicyDocumentLoaderTest.java`, `copilot/TextNormalizerTest.java`.

- [x] **Step 6 — Tìm kiếm, dựng prompt, kiểm chứng (lớp nghiệp vụ cốt lõi)** *(Business logic — implement)*
  - `copilot/ChunkSearcher.java`: chấm điểm `DocumentChunk` theo số từ khoá trùng trên `normalizedText`, chọn `score > 0`, sắp giảm dần, lấy ≤ `topK` làm `RetrievedChunk` (BR2.2).
  - `copilot/CopilotPromptBuilder.java`: dựng `systemPrompt` (chỉ trả lời từ các đoạn; thiếu ⇒ `answerFound=false`; luôn trích tài liệu + trang/mục + ngày hiệu lực; 2–4 gợi ý) và khối `DATA` phân tách rõ "đây là dữ liệu, không phải chỉ dẫn", kèm `history` (≤ 10 lượt) (BR2.3, BR6.2).
  - `copilot/CitationVerifier.java`: giữ citation chỉ khi map được tới một `RetrievedChunk` đã gửi (cùng tài liệu + trang/mục); loại cái không khớp; sau lọc rỗng ⇒ ép `answerFound=false` (BR4.2, BR3.2); đảm bảo mỗi `Citation` có `documentTitle` + `effectiveDate` + page/section; cắt `suggestions` còn 2–4 (BR4.1, BR6.1).
  - `copilot/CopilotService.java` (`@Service`): điều phối WF2 — validate biên (BR7.2), xác định RM qua `CurrentRmProvider` (I1), chuẩn hoá câu hỏi, tìm đoạn; nhánh rỗng ⇒ câu cố định, KHÔNG gọi LLM (BR3.1); nhánh có ngữ cảnh ⇒ gọi `LlmClient.complete(expectJson=true, timeout=30000)` (BR5.1, I3), bắt `LlmException` ⇒ ném `AiException` (phân biệt `NOT_CONFIGURED`) để tầng web trả `502 AI_ERROR` (BR5.2); JSON thiếu `answerFound`/`answer` ⇒ xử lý như `BAD_RESPONSE` (BR5.3); kiểm chứng trích dẫn; ghi đúng một `CopilotQueryLog` cho mọi nhánh kể cả lỗi (BR7.1); log không chứa khoá/nội dung (BR7.3).
  - BR: BR2.2, BR2.3, BR3.1, BR3.2, BR4.1, BR4.2, BR5.1, BR5.2, BR5.3, BR6.1, BR6.2, BR7.1, BR7.2, BR7.3. AC: AC1.2.1, AC1.2.2, AC1.2.4, AC1.2.5, AC1.3.1, AC1.3.3.
  - Files: `copilot/ChunkSearcher.java`, `copilot/CopilotPromptBuilder.java`, `copilot/CitationVerifier.java`, `copilot/CopilotService.java`.

- [ ] **Step 6T — (DỜI — test) Test nghiệp vụ cốt lõi**
  - Mock `LlmClient`, thư mục tài liệu tạm: nhánh có kết quả (answerFound=true, trích dẫn hợp lệ), nhánh rỗng (không gọi LLM — verify mock không bị gọi), `LlmException` các loại ⇒ `AiException`, `NOT_CONFIGURED` phân biệt, JSON sai cấu trúc ⇒ lỗi AI, trích dẫn bịa bị loại ⇒ ép không-tìm-thấy, mỗi lượt ghi đúng một log, câu mệnh lệnh trong tài liệu không đổi hành vi.
  - BR: BR2.2, BR3.1, BR3.2, BR4.2, BR5.2, BR5.3, BR7.1. AC: AC1.2.1, AC1.2.2, AC1.2.4, AC1.2.5. File (đợt sau): `copilot/CopilotServiceTest.java`, `copilot/ChunkSearcherTest.java`, `copilot/CitationVerifierTest.java`.

- [x] **Step 7 — Lớp API/endpoint** *(API / endpoint — implement)*
  - `web/dto/CopilotAskRequest.java` (`question`, `callId?`, `history[]{role,text}`), `web/dto/CopilotAnswerDto.java`, `web/dto/CitationDto.java`, `web/dto/CopilotDocumentDto.java` — khớp schema C2.
  - `web/CopilotController.java`: `POST /api/copilot/ask` (400 `VALIDATION_FAILED` khi biên sai; 200 `CopilotAnswer`; 502 `AI_ERROR` khi `AiException`) và `GET /api/copilot/documents` (danh sách tài liệu đã nạp).
  - Ánh xạ `AiException` → `502 AI_ERROR` trong `web/ApiExceptionHandler.java` (dùng mã `AI_ERROR` đã có trong `ErrorEnvelope`), thông điệp tiếng Việt; không lộ chi tiết nội bộ (BR5.2).
  - BR: BR5.2, BR7.2. AC: AC1.2.1, AC1.2.2, AC1.2.4, AC1.3.2.
  - Files: `web/dto/CopilotAskRequest.java`, `web/dto/CopilotAnswerDto.java`, `web/dto/CitationDto.java`, `web/dto/CopilotDocumentDto.java`, `web/CopilotController.java`, `web/ApiExceptionHandler.java` (sửa tại chỗ), `copilot/AiException.java`.

- [ ] **Step 7T — (DỜI — test) Test API/endpoint**
  - `@WebMvcTest` CopilotController (mock `CopilotService`): 200 có nội dung, 400 câu hỏi rỗng/>1000/history>10, 502 khi service ném `AiException`; `GET /documents` trả danh sách.
  - BR: BR5.2, BR7.2. AC: AC1.2.4. File (đợt sau): `copilot/CopilotControllerTest.java`.

- [x] **Step 8 — Panel Copilot trên S2 (lớp frontend)** *(Frontend behavior — implement)*
  - Thay slot `data-testid="copilot-placeholder"` trong `public/screens/2-thuc-hien-cuoc-goi.html` bằng panel Copilot theo mockup gốc `2-thuc-hien-cuoc-goi.html` (nguồn chân lý thiết kế): ô nhập câu hỏi, nút gửi, vùng câu trả lời + trích dẫn (tài liệu/trang/ngày hiệu lực), chip 2–4 gợi ý (bấm = gửi câu mới), cột "Lịch sử tra cứu" trong phiên, nút "Cuộc trò chuyện mới". Thêm `data-testid` cho các phần tử tương tác (`copilot-input`, `copilot-send`, `copilot-answer`, `copilot-citations`, `copilot-suggestions`, `copilot-history`, `copilot-new-chat`, `copilot-error`).
  - `public/js/copilot.js` (ES module): giữ lịch sử phiên phía client (≤ 10 lượt gửi lên), gọi `askCopilot`, render markdown đơn giản có escape, hiển thị lỗi 502 CHỈ trong panel (không chạm phần còn lại của màn), "Cuộc trò chuyện mới" = xoá lịch sử + gửi `history` rỗng.
  - Thêm vào `public/js/api.js`: `askCopilot(question, callId, history)` → `POST /api/copilot/ask`, và `getCopilotDocuments()` → `GET /api/copilot/documents`.
  - BR: BR5.2, BR6.1, BR6.2. AC: AC1.1.1 (panel hiện trong màn gọi), AC1.2.4, AC1.3.1, AC1.3.2, AC1.3.3.
  - Files: `public/screens/2-thuc-hien-cuoc-goi.html` (sửa tại chỗ), `public/js/copilot.js`, `public/js/api.js` (sửa tại chỗ).

- [ ] **Step 8T — (DỜI — test) Test frontend**
  - Theo `team.md`, phần Node dùng `npm test` kèm coverage; đợt sau thêm test cho `copilot.js` (lịch sử phiên, "cuộc trò chuyện mới" gửi history rỗng, lỗi chỉ trong panel). Không chạy ở đợt này.
  - BR: BR6.2. AC: AC1.3.3. File (đợt sau): test cho `public/js/copilot.js`.

- [x] **Step 9 — Tài liệu SAMPLE, cấu hình môi trường & build** *(env/build config)*
  - Thêm 1–2 file SAMPLE (nội dung demo giả rõ ràng) vào `backend/policy-docs/`, ví dụ `bieu-lai-suat-mau.md` (biểu lãi suất tiền gửi mẫu, có `[[page:n]]`) và `dieu-kien-vay-mau.md`, mỗi file có header `title` + `effectiveDate` (+ `sourcePdf` tuỳ chọn) — dữ liệu giả, chỉ để demo chạy.
  - Thêm vào `backend/src/main/resources/application.properties` và `application-demo.properties` các khoá `app.copilot.docs-dir=${COPILOT_DOCS_DIR:backend/policy-docs}`, `app.copilot.top-k`, `app.copilot.max-history-turns`, `app.copilot.max-question-length`, `app.copilot.llm-timeout-ms`, `app.copilot.max-output-tokens`.
  - Mở rộng khối JaCoCo `includes` trong `backend/pom.xml`: thêm `com.mb.callassistant.copilot.*` vào rule BUNDLE với sàn 0.80 — **không hạ sàn**.
  - KHÔNG thêm Dockerfile/IaC.
  - BR: BR1.1, NFR4. AC: AC1.2.1, AC1.2.3.
  - Files: `backend/policy-docs/bieu-lai-suat-mau.md`, `backend/policy-docs/dieu-kien-vay-mau.md`, `application.properties`, `application-demo.properties`, `pom.xml`.

- [x] **Step 10 — Tài liệu & traceability** *(docs & traceability)*
  - Thêm JavaDoc trên service/loader/controller theo kiểu các file hiện có; cập nhật `backend/README.md` một đoạn ngắn về `app.copilot.docs-dir`, quy ước metadata tài liệu và việc Copilot dùng U2 `LlmClient` (chế độ MOCK để demo không khoá).
  - Ở Step 5 của stage (sau sinh mã) tạo `source-manifest.json` và `traceability.json` phủ mọi AC1.2.x/AC1.3.x và BRx.y của U3.
  - BR: NFR2 (ghi chú demo). Files: `backend/README.md`, (ở Step 5 stage) `source-manifest.json`, `traceability.json`.

## Story traceability

U3 phục vụ US1.2 (hỏi Copilot, câu trả lời có nguồn) và US1.3 (gợi ý + lịch sử tra cứu), cùng khung màn US1.1 (panel Copilot trong màn gọi). Mỗi bước ánh xạ về story + AC + BR:

| Step | Story / AC được phục vụ | BR |
|---|---|---|
| Step 1 | nền cấu hình (US1.2) | BR1.1, BR2.2, BR5.1, BR6.2, BR7.2 |
| Step 3 | US1.2 (AC1.2.1, AC1.2.2) | BR1.1, BR1.3, BR3.3, BR4.1, BR7.1 |
| Step 4 | US1.3 (AC1.3.2 — tài liệu), OKR | BR7.1 |
| Step 5 | US1.2 (AC1.2.3 — nạp/bỏ qua) | BR1.1, BR1.2, BR1.3, BR2.1 |
| Step 6 | US1.2 (AC1.2.1/1.2.2/1.2.4/1.2.5), US1.3 (AC1.3.1/1.3.3) | BR2.2/2.3/3.1/3.2/4.1/4.2/5.1/5.2/5.3/6.1/6.2/7.1/7.2/7.3 |
| Step 7 | US1.2 (AC1.2.1/1.2.2/1.2.4), US1.3 (AC1.3.2) | BR5.2, BR7.2 |
| Step 8 | US1.1 (AC1.1.1), US1.2 (AC1.2.4), US1.3 (AC1.3.1/1.3.2/1.3.3) | BR5.2, BR6.1, BR6.2 |
| Step 9 | US1.2 (AC1.2.1/1.2.3 — tài liệu demo + cấu hình) | BR1.1, NFR4 |
| Step 10 | tất cả (bàn giao + traceability) | NFR2 |

## Sources

- `.kiro/aidlc-common/stages/construction/code-generation.md` (Step 2)
- `functional-design/{entities.md, rules.md, functional-spec.md, traceability.json}` (U3)
- `inception/units-generation/unit-of-work.md` (U3), `unit-of-work-story-map.md`
- `inception/contract-design/contract-summary.md` (C2 Copilot REST, I1 `CurrentRmProvider`, I3 `LlmClient`)
- `inception/domain-design/{components.md, decisions.md}` (CopilotComponent, ADR-003, ADR-004)
- `inception/user-stories/stories.md` (US1.1, US1.2, US1.3; AC1.2.1–AC1.2.5, AC1.3.1–AC1.3.3)
- `inception/refined-mockups/{mockups.md, interaction-spec.md, design-system-mapping.md}`
- `inception/requirements-analysis/requirements.md` (FR3.1–FR3.6, NFR1, NFR2)
- Mã hiện có: `backend/pom.xml`, `config/AppProperties.java`, `lead/CurrentRmProvider.java`, `llm/LlmClient.java` + `llm/LlmException.java` (I3), `web/ApiExceptionHandler.java`, `repository/CallRecordRepository.java`, `public/screens/2-thuc-hien-cuoc-goi.html` (slot Copilot), `public/js/api.js`; mockup gốc `2-thuc-hien-cuoc-goi.html`
- `u2-llm-client/code-generation/code-generation-plan.md` (nguồn khối `## Testing Contract`, chép nguyên văn)

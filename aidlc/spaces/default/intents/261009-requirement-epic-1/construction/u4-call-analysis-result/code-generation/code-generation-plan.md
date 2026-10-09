# Code Generation Plan — U4 `u4-call-analysis-result`

## Summary

- Builds: Luồng sau cuộc gọi của U4 — bước pipeline `ANALYZE` (cắm vào I2) gọi `LlmClient` (I3) chấm điểm/tóm tắt, server tự tính `totalScore`/`grade`, lưu `CallAnalysis`; API C3 (`GET /analysis`, `POST /analysis/retry`, `GET`+`PUT /keyin`) với lưu một-giao-dịch idempotent (keyin + feedback + updateStage + recordCallActivity + createFromCall); và màn S3/S4 + khối tóm tắt AI ở S1.
- Touches: package mới `backend/src/main/java/com/mb/callassistant/analysis/**`; mở rộng `service/CallPipelineService` để chạy bước `CallPipelineStep` đã đăng ký sau STT; sửa `llm/MockLlmClient` (canned JSON hợp lệ cho CẢ `COPILOT` lẫn `CALL_ANALYSIS`); thêm `app.analysis.*` vào `config/AppProperties` + `application*.properties`; mở JaCoCo include cho `com.mb.callassistant.analysis.*` trong `backend/pom.xml`; màn `public/screens/3-ket-qua-cuoc-goi.html`, `4-...`, khối S1, `public/js/api.js`. KHÔNG sửa mã của U1/U5.
- Tests: JUnit5 + Mockito (bộ ba service: scoring, analyze-step, keyin-transaction) + `@WebMvcTest`/MockMvc cho C3 + test JS cho api.js, sàn JaCoCo ≥ 80% dòng cho package U4 — tất cả DỜI sang đợt sau theo yêu cầu người dùng (xem mục bên dưới); không tạo file test trong đợt này.

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

Theo yêu cầu của người dùng ("gen code cho tất cả các unit một lượt, phần test
để sau"), **mọi bước viết và chạy test trong kế hoạch này được DỜI sang một đợt
sau**. Trong đợt sinh mã lần này:

- KHÔNG tạo bất kỳ file test nào (không tạo `backend/src/test/java/com/mb/callassistant/analysis/**`
  và không tạo file test JS cho `public/js/`).
- KHÔNG chạy `mvn verify`, `mvn test`, `npm test` hay bất kỳ lệnh build/test nào.
- Các bước có nhãn **(DỜI — test)** dưới đây vẫn giữ nguyên trong kế hoạch để đợt
  test sau thực hiện; chúng mô tả đúng việc test-after theo từng lớp.

Lưu ý ràng buộc không được nới: **sàn độ phủ 80% dòng cho package
`com.mb.callassistant.analysis` KHÔNG bị hạ.** Nó chỉ được kiểm chứng ở đợt
test sau. Phương pháp test-after theo `## Testing Contract` ở trên vẫn là nguồn
chân lý; mục này chỉ dời thời điểm thực thi các bước test, không đổi phương pháp
và không hạ mục tiêu chất lượng.

## Steps

- [x] **Step 1 — Dựng cấu trúc package và khung cấu hình** *(cấu trúc/cấu hình)*
  - Tạo package `backend/src/main/java/com/mb/callassistant/analysis/`.
  - Mở rộng `config/AppProperties.java`: thêm lớp lồng `Analysis` với các trường
    `gradeThresholdA` (85), `gradeThresholdB` (70), `gradeThresholdC` (50) — ngưỡng
    xếp hạng cấu hình được (BR3.3); `llmTimeoutMs` (30000, BR8.2/NFR1); `maxOutputTokens`
    (2048); `recordingWaitMaxAttempts`/`recordingWaitMs` cho dự phòng NO_RECORDING
    (AC2.4.1) kèm getter/setter theo đúng kiểu đang có. Không thêm trường khóa API.
  - BR: BR3.3, BR8.2, NFR1. File: `config/AppProperties.java`.

- [x] **Step 2 — Thực thể & value object JPA của U4** *(lớp mô hình — implement)*
  - `analysis/CallAnalysis.java` (`@Entity`): `analysisId` PK, `callId` unique
    (tối đa một bản hiện hành/cuộc gọi — tức 0..1 phía CallAnalysis theo review R-03),
    `summary`, `suggestions` (list), `criteriaScores` (5 `CriterionScore`), `totalScore`
    (integer 0–100), `grade` (enum A/B/C/D), `tone` (enum), `insightStrength`,
    `insightImprovement`, `suggestedKeyin` (object), `suggestedTasks` (list), `model`,
    `createdAt` (+07:00).
  - `analysis/CallKeyin.java` (`@Entity`): `keyinId` PK, `callId` unique, `leadStage`
    (dùng lại `lead.LeadStage`), `callResult`, các trường tuỳ chọn (`demandAmount`
    integer VND, `term`, `agreedRate` decimal, `product`, `sourceOfFunds`, `expectedDate`,
    `channel`, `objections`, `rmNote`), `fieldSources` (object), `savedAt` (+07:00).
  - `analysis/AiFeedback.java` (`@Entity`): `feedbackId` PK, `callId` unique, `rating`
    1–10, `tags` (list), `comment` (≤1000).
  - Value object: `analysis/CriterionScore.java` (code enum CHAO_HOI/.../CHOT_CAM_KET,
    name, score decimal bước 0,5, evidence — review R-04: điểm tiêu chí decimal, tổng
    integer), `analysis/SuggestedValue.java` (value + sourceMs), tái dùng `task.TaskDraft`
    cho công việc đề xuất. Enum: `AnalysisGrade`, `CallTone`, `FieldSource`,
    `AnalysisState` (SMA: PENDING_TRANSCRIPT/ANALYZING/ANALYZED/ANALYSIS_FAILED/NO_ANALYSIS).
  - **Ánh xạ tên (review R-01)**: thực thể giữ `criteriaScores`; DTO C3 serialize thành
    `criteria`. Ghi rõ ánh xạ này ở DTO (Step 7), không bịa tên.
  - AC/BR: AC2.2.1, AC2.3.1–AC2.3.3, BR2.2, BR3.1, BR3.3, BR4.1, BR4.3, BR8.3.
    Files: `analysis/CallAnalysis.java`, `analysis/CallKeyin.java`, `analysis/AiFeedback.java`,
    `analysis/CriterionScore.java`, `analysis/SuggestedValue.java`, `analysis/AnalysisGrade.java`,
    `analysis/CallTone.java`, `analysis/FieldSource.java`, `analysis/AnalysisState.java`.

- [ ] **Step 2T — (DỜI — test) Test thực thể & value object**
  - Ràng buộc bất biến: `criteriaScores` đúng 5 phần tử; `rating` ∈ [1,10]; `savedAt`/`createdAt`
    có offset +07:00; tiền integer.
  - AC/BR: BR2.2, BR8.3. File (đợt sau): `.../analysis/CallAnalysisEntityTest.java`.

- [x] **Step 3 — Repository (lớp truy cập dữ liệu)** *(repository — implement)*
  - `analysis/CallAnalysisRepository.java` (`findByCallId`), `analysis/CallKeyinRepository.java`
    (`findByCallId` — nền idempotent BR5.2), `analysis/AiFeedbackRepository.java` (`findByCallId`),
    theo đúng khuôn `JpaRepository` của U1/U5. Transcript đọc qua `TranscriptTurnRepository`
    (U1, không tạo mới).
  - BR: BR5.2 (upsert tại chỗ theo callId). Files: `analysis/CallAnalysisRepository.java`,
    `analysis/CallKeyinRepository.java`, `analysis/AiFeedbackRepository.java`.

- [x] **Step 4 — Xác minh test runner + ghi lệnh test phạm vi unit** *(test runner readiness)*
  - Brownfield: xác nhận lệnh test phạm vi U4 chạy được trước bước test-first đầu tiên; ghi
    chính xác vào `unit-test-instructions.md`:
    `mvn -f backend/pom.xml -Dtest="com.mb.callassistant.analysis.**.*Test" verify`.
  - KHÔNG chạy lệnh trong đợt này (xem "Thay đổi theo yêu cầu người dùng"); chỉ ghi nhận.
  - BR: NFR6. File: `unit-test-instructions.md`.

- [x] **Step 5 — Chấm điểm & kiểm kết quả AI (lớp nghiệp vụ)** *(nghiệp vụ — implement)*
  - `analysis/CallScoringService.java`: nhận JSON AI đã parse → kiểm `criteriaScores`
    đúng 5 phần tử, mỗi `score` ∈ [0,10] bước 0,5; sai → ném lỗi "AI thất bại", KHÔNG bịa
    (BR2.1, BR2.2). Tính `totalScore = round(tổng 5 × 2)` ∈ [0,100], BỎ QUA mọi tổng do LLM
    đưa (BR3.1, BR3.2). Suy `grade` theo ngưỡng cấu hình A≥85/B70–84/C50–69/D<50, biên 85→A,
    84→B (BR3.3, AC2.2.1, AC2.2.4).
  - `analysis/AnalysisResultParser.java`: parse JSON AI (dùng `JsonResponses`-style strip fence
    nếu cần) sang `CallAnalysis` tạm (summary, suggestions, criteria, tone, insight,
    suggestedKeyin kèm sourceMs — trường vắng để trống không bịa BR4.1, suggestedTasks).
  - AC/BR: AC2.2.1, AC2.2.3, AC2.2.4, AC2.3.1, AC2.3.2, BR2.1, BR2.2, BR3.1, BR3.2, BR3.3, BR4.1.
    Files: `analysis/CallScoringService.java`, `analysis/AnalysisResultParser.java`.

- [ ] **Step 5T — (DỜI — test) Test chấm điểm & parser**
  - 10;10;9,5;9;9,5 → 96/A (AC2.2.1); tổng 84 → B (AC2.2.4); score ngoài [0,10] hoặc sai bước →
    AI thất bại, không lưu (AC2.2.3); bỏ qua tổng LLM (BR3.2); trường vắng → bỏ, không bịa (BR4.1).
  - AC/BR: AC2.2.1, AC2.2.3, AC2.2.4, BR2.2, BR3.1, BR3.2, BR4.1. File (đợt sau):
    `.../analysis/CallScoringServiceTest.java`.

- [x] **Step 6 — Bước ANALYZE của pipeline + điều phối phân tích (lớp nghiệp vụ)** *(nghiệp vụ — implement)*
  - `analysis/CallAnalysisService.java`: điều phối WF1. Dựng prompt — `systemPrompt` là chỉ dẫn;
    transcript + ngữ cảnh lead đặt trong khối DATA phân tách là **dữ liệu** (BR8.1, dùng mẫu U2
    `MessageBuilder`/`LlmRequest.userContent`). Gọi `LlmClient.complete(expectJson=true)` (I3) với
    `purpose = CALL_ANALYSIS`, timeout cấu hình. Kiểm kết quả (BR2.1); sai → chạy lại ĐÚNG MỘT LẦN;
    vẫn sai hoặc `LlmException` → không lưu `CallAnalysis`, yêu cầu U1 đặt `AI_FAILED`
    qua `CallService.updatePipeline` (BR1.2, BR2.3). Hợp lệ → tính điểm (Step 5), lưu `CallAnalysis`
    (idempotent theo callId), yêu cầu U1 đặt `DONE`. Ghi log `{callId, step, latencyMs, outcome}`,
    KHÔNG log khóa/prompt (BR8.2).
  - `analysis/AnalyzeCallPipelineStep.java` (`@Component` implements `service.CallPipelineStep`,
    I2): `run(callId, transcriptTurns, leadContext)` ủy quyền cho `CallAnalysisService`; map kết quả
    sang `StepResult.ok()` / `StepResult.failed(FailureKind.AI_FAILED, detail)`. Cần transcript mới chạy
    (BR1.1).
  - **Mở rộng brownfield `service/CallPipelineService.java`**: hiện sau STT nó đặt `SttStatus.DONE`
    mà CHƯA gọi các `CallPipelineStep` đã đăng ký. Inject `List<CallPipelineStep>` (Spring thu các
    `@Component`) và `CallService`; sau khi STT thành công: đặt `updatePipeline(callId, PROCESSING, ANALYZE)`
    rồi chạy từng step theo thứ tự; `StepResult.failed(AI_FAILED)` → `updatePipeline(..., AI_FAILED, ...)`;
    tất cả ok → `updatePipeline(..., DONE)`. Giữ nhánh NO_RECORDING/STT_FAILED hiện có. KHÔNG đổi ranh
    giới SM2 (chỉ U1 ghi trạng thái, qua `updatePipeline`).
  - AC/BR: AC2.1.1, AC2.1.4, BR1.1, BR1.2, BR2.1, BR2.3, BR8.1, BR8.2. Files:
    `analysis/CallAnalysisService.java`, `analysis/AnalyzeCallPipelineStep.java`,
    `service/CallPipelineService.java` (mở rộng tại chỗ).

- [ ] **Step 6T — (DỜI — test) Test bước ANALYZE & điều phối**
  - Mock `LlmClient`: JSON hợp lệ → ANALYZED + updatePipeline(DONE); JSON sai lần 1, đúng lần 2 →
    chạy lại một lần rồi ANALYZED; sai cả hai / `LlmException` → AI_FAILED, không lưu; không transcript →
    không chạy; log không chứa prompt/khóa.
  - AC/BR: BR1.1, BR1.2, BR2.1, BR2.3, BR8.1, BR8.2. File (đợt sau):
    `.../analysis/CallAnalysisServiceTest.java`.

- [x] **Step 7 — Lưu keyin một-giao-dịch + chạy lại AI (lớp nghiệp vụ)** *(nghiệp vụ — implement)*
  - `analysis/KeyinService.java`:
    - `getOwnedAnalysis(callId)` / `getOwnedKeyin(callId)`: qua `CallService.getOwnedCall(callId)`
      (I2) → 403 nếu khác RM, 404 nếu chưa có (BR5.5).
    - `saveKeyin(callId, KeyinSave)` **trong `@Transactional`** (BR5.3): kiểm `leadStage`+`callResult`
      bắt buộc → thiếu ném `ValidationException(field)` → 400 (BR5.1, AC2.3.5); idempotent theo callId
      (upsert, AC2.3.6, BR5.2); lưu `CallKeyin`+`fieldSources`; lưu `AiFeedback` nếu có; gọi
      `LeadService.updateStage(rmId, leadId, leadStage)` (I1); `LeadService.recordCallActivity(rmId,
      leadId, callId, product, result, note, aiSummary, aiSuggestions)` kèm bản sao từ `CallAnalysis`
      hiện có (hoặc null khi AI lỗi, BR5.4); `TaskService.createFromCall(rmId, leadId, callId,
      callAiSummary, drafts)` — **đúng 5 tham số thật của U5**, mỗi draft BẮT BUỘC `clientKey` (I4,
      BR5.2). Trả `{ callId, interactionCode, createdTaskIds }` (BR6.1). Lỗi bất kỳ → cả giao dịch
      hoàn tác.
    - `retryAnalysis(callId)`: kiểm sở hữu; chưa có transcript hoặc đang ANALYZING → `ConflictException`
      → 409 (BR1.4); hợp lệ → `updatePipeline(callId, PROCESSING, ANALYZE)` và chạy lại
      `CallAnalysisService` (BR1.3), trả 202. Quy tắc "chỉ điền trường trống" là ở giao diện (Step 10).
  - `rmId`/`leadId` lấy qua U1 (`CurrentRmProvider`, `CallService.getOwnedCall`); không tự suy.
  - AC/BR: AC2.3.4, AC2.3.5, AC2.3.6, AC2.4.3(server), BR1.3, BR1.4, BR5.1–BR5.5, BR6.1, BR8.4.
    File: `analysis/KeyinService.java`.

- [ ] **Step 7T — (DỜI — test) Test lưu keyin & chạy lại**
  - Thiếu trường bắt buộc → 400 không lưu; lưu lặp cùng callId → một keyin, không task trùng
    (idempotent); lỗi giữa giao dịch → hoàn tác; khác RM → 403; retry khi chưa có transcript/đang chạy
    → 409. Mock `LeadService`/`TaskService`.
  - AC/BR: AC2.3.5, AC2.3.6, BR1.4, BR5.1–BR5.5. File (đợt sau): `.../analysis/KeyinServiceTest.java`.

- [x] **Step 8 — API C3 (lớp endpoint)** *(API — implement)*
  - `analysis/web/AnalysisController.java` (`@RestController`, `@RequestMapping("/api/calls")`), theo
    đúng kiểu `CallController`/`TaskController`, lỗi nổi qua `ApiExceptionHandler` sẵn có:
    - `GET /api/calls/{callId}/analysis` → 200 `CallAnalysisDto` | 404 khi chưa có (BR5.5, WF3).
    - `POST /api/calls/{callId}/analysis/retry` → 202 | 409 (BR1.4).
    - `GET /api/calls/{callId}/keyin` → 200 `KeyinDto` | 404 (BR5.5).
    - `PUT /api/calls/{callId}/keyin` → 200 `KeyinSavedDto` | 400 (`details.field`) | 403 (BR5.1, BR5.5).
  - DTO (`analysis/web/dto/`): `CallAnalysisDto` (serialize `criteriaScores` → `criteria` theo review
    R-01), `SuggestedValueDto`, `TaskDraftDto`, `KeyinSaveDto` (`@Valid`: `leadStage`, `callResult`,
    `fieldSources`, `nextTasks` bắt buộc), `KeyinDto`, `KeyinSavedDto`. Giữ nguyên phong cách `record`
    + factory `from(...)` như các DTO hiện có.
  - AC/BR: AC2.1.1, AC2.3.4, AC2.3.5, AC2.4.3, BR1.4, BR5.1, BR5.5, BR6.1, BR8.4. Files:
    `analysis/web/AnalysisController.java`, `analysis/web/dto/*.java`.

- [ ] **Step 8T — (DỜI — test) Test API C3**
  - `@WebMvcTest(AnalysisController.class)` + MockMvc: mã trạng thái và hình dạng JSON cho 200/202/404/409,
    400 với `details.field=leadStage|callResult`, 403 khác RM; `criteria` xuất hiện đúng tên.
  - AC/BR: AC2.3.5, BR1.4, BR5.1, BR5.5. File (đợt sau): `.../analysis/web/AnalysisControllerTest.java`.

- [x] **Step 9 — MockLlmClient: canned JSON hợp lệ cho CẢ hai purpose** *(nghiệp vụ/demo — implement)*
  - **Sửa `llm/MockLlmClient.java` tại chỗ** (brownfield, thuộc U2 — liệt kê ở Touches): cập nhật
    `cannedByPurpose` để trả JSON HỢP LỆ, khớp schema của bên tiêu thụ, cho demo offline đầu-cuối:
    - `COPILOT`: JSON có đủ trường `CopilotService` đọc — `answerFound` (boolean), `answer` (string),
      `citations` (mảng object `{documentId, documentTitle, page, section, effectiveDate}` — `documentTitle`
      và `effectiveDate` bắt buộc, có `page` HOẶC `section`), `suggestions` (mảng ≤4 string). Dùng
      `answerFound:true` với một citation khớp tài liệu seed.
    - `CALL_ANALYSIS`: JSON khớp schema U4 — `summary`, `suggestions[]`, mảng điểm 5 tiêu chí (đúng mã
      CHAO_HOI/KHAI_THAC_NHU_CAU/CHINH_SACH_LAI_SUAT/XU_LY_THAC_MAC/CHOT_CAM_KET), `tone`,
      `insightStrength`, `insightImprovement`, `suggestedKeyin` (vài trường kèm `sourceMs`),
      `suggestedTasks[]` (mỗi task có `clientKey`). KHÔNG kèm trường tổng (server tự tính, BR3.2).
  - Giữ nguyên hành vi còn lại của `MockLlmClient` (validate biên, `expectJson`, canned error) — chỉ đổi
    nội dung canned. KHÔNG đổi I3/`LlmClient`.
  - AC/BR: AC1.2.1 (demo), AC2.2.1 (demo), BR6.1(U2), NFR3.4(U2). File: `llm/MockLlmClient.java`.

- [x] **Step 10 — Giao diện S3/S4 + khối tóm tắt AI ở S1** *(frontend — implement)*
  - `public/js/api.js`: thêm `getAnalysis(callId)`, `retryAnalysis(callId)`, `getKeyin(callId)`,
    `saveKeyin(callId, body)` (gắn `backendHeaders`), và `pollCallStatus` dùng `pipelineStatus`
    (BR7.3, poll 3s). KHÔNG đổi hàm cũ.
  - `public/screens/3-ket-qua-cuoc-goi.html` (nguồn UI: root `3-ket-qua-cuoc-goi.html`): 3 cột — cột 1
    điểm/hạng/5 tiêu chí/tone/insight; cột 2 trình phát (`recordingUrl`) + transcript lượt + nút tải
    `.wav`; cột 3 form keyin auto-fill + huy hiệu `AI · mm:ss`/`RM đã sửa` + đánh giá AI + công việc
    tiếp theo; thanh tiến trình 3 bước tự cập nhật; dự phòng theo loại lỗi (NO_RECORDING/STT_FAILED/
    AI_FAILED) + nút "Thử phân tích AI lại". Thêm `data-testid` cho phần tử tương tác.
  - `public/screens/4-...` / hộp thoại S4 (nguồn UI: root `4-lua-chon-dieu-huong.html`): 3 lựa chọn
    (Liên hệ KH tiếp theo → `getNextLead`; Chi tiết lịch làm việc → S5 với `?sourceCallId`; Đóng & về
    trang chủ).
  - Khối "Tóm tắt AI" trong Quản lý hoạt động ở S1 (`getLeadActivities`): tóm tắt/gợi ý AI chỉ đọc,
    thu gọn/mở rộng + liên kết "Xem kết quả cuộc gọi"; thiếu → "Không có tóm tắt AI".
  - Quy tắc "chỉ điền trường trống" khi chạy lại AI thực hiện ở client (AC2.4.3, BR4.2).
  - AC/BR: AC2.1.1, AC2.1.2, AC2.2.2, AC2.3.1–AC2.3.3, AC2.4.1, AC2.4.2, AC2.4.3, AC2.4.4, AC2.4.5,
    AC3.1.1–AC3.1.3, BR4.2, BR6.1, BR6.2, BR7.1–BR7.4. Files: `public/js/api.js`,
    `public/screens/3-ket-qua-cuoc-goi.html`, `public/screens/4-lua-chon-dieu-huong.html` (và khối S1
    trong màn chi tiết lead).

- [ ] **Step 10T — (DỜI — test) Test giao diện api.js**
  - Test JS cho `api.js` (fetch giả lập): đường dẫn/ân header đúng; `retryAnalysis` map 202/409;
    `saveKeyin` map 400/403; "chỉ điền trường trống" không ghi đè giá trị RM.
  - AC/BR: AC2.4.3, BR4.2. File (đợt sau): test JS cạnh `public/js/`.

- [x] **Step 11 — Cấu hình môi trường & build** *(env/build config)*
  - Thêm `app.analysis.*` vào `backend/src/main/resources/application.properties` và
    `application-demo.properties` (demo để `app.llm.provider=mock`): `grade-threshold-a/b/c`,
    `llm-timeout-ms`, `max-output-tokens`, `recording-wait-*`.
  - Mở rộng khối JaCoCo `includes` trong `backend/pom.xml`: thêm `com.mb.callassistant.analysis.*`
    vào rule BUNDLE với sàn 0.80 — **không hạ sàn**.
  - BR: BR3.3, NFR6. Files: `application.properties`, `application-demo.properties`, `pom.xml`.

- [x] **Step 12 — Tài liệu & traceability** *(docs & traceability)*
  - JavaDoc trên service/controller theo kiểu các file hiện có; cập nhật `backend/README.md` một đoạn
    ngắn về `app.analysis.*`, bước ANALYZE của pipeline và chế độ MOCK offline đầu-cuối.
  - Ở Step 5 của stage (sau sinh mã) tạo `source-manifest.json` (gồm cả `service/CallPipelineService.java`
    và `llm/MockLlmClient.java` đã sửa) và `traceability.json` cho mọi AC/BR của U4.
  - BR: NFR6. Files: `backend/README.md`, (ở Step 5 stage) `source-manifest.json`, `traceability.json`.

## Story traceability

| Step | Story | AC chính | BR / NFR |
|---|---|---|---|
| Step 1 | US2.2, US2.4 (nền cấu hình) | — | BR3.3, BR8.2, NFR1 |
| Step 2 | US2.1, US2.2, US2.3 (mô hình) | AC2.2.1, AC2.3.1–2.3.3 | BR2.2, BR3.1, BR4.1, BR4.3, BR8.3 |
| Step 3 | US2.3 (persist idempotent) | AC2.3.6 | BR5.2 |
| Step 5 | US2.2 (chấm điểm) | AC2.2.1, AC2.2.3, AC2.2.4 | BR2.1, BR2.2, BR3.1, BR3.2, BR3.3 |
| Step 6 | US2.1 (pipeline AI) | AC2.1.1, AC2.1.4 | BR1.1, BR1.2, BR2.1, BR2.3, BR8.1, BR8.2 |
| Step 7 | US2.3, US2.4, US3.1 (lưu + retry) | AC2.3.4–2.3.6, AC2.4.3 | BR1.3, BR1.4, BR5.1–BR5.5, BR6.1 |
| Step 8 | US2.1–US2.4 (API C3) | AC2.1.1, AC2.3.4, AC2.3.5 | BR1.4, BR5.1, BR5.5, BR6.1 |
| Step 9 | US1.2, US2.1–US2.3 (demo offline) | AC1.2.1, AC2.2.1 | BR6.1(U2), NFR3.4(U2) |
| Step 10 | US2.1, US2.2, US2.4, US3.1 (S3/S4/S1) | AC2.1.1–2.1.2, AC2.2.2, AC2.4.1–2.4.5, AC3.1.1–3.1.3 | BR4.2, BR6.1, BR6.2, BR7.1–BR7.4 |
| Step 11 | tất cả (cấu hình chạy) | — | BR3.3, NFR6 |
| Step 12 | tất cả (bàn giao) | — | NFR6 |

## Sources

- `.kiro/aidlc-common/stages/construction/code-generation.md` (Step 2)
- `construction/u4-call-analysis-result/functional-design/{functional-spec.md, rules.md, entities.md, traceability.json}` (U4), `functional-design/reviews/review-01.md` (R-01…R-04)
- `inception/units-generation/{unit-of-work.md (U4), unit-of-work-story-map.md}`
- `inception/contract-design/contract-summary.md` (C3, I1, I2, I3, I4)
- `inception/user-stories/stories.md` (US2.1–US2.4, US3.1)
- `inception/refined-mockups/{mockups.md (S3, S4), interaction-spec.md, design-system-mapping.md}`
- `inception/requirements-analysis/requirements.md` (FR4.x, NFR1, NFR3, NFR6)
- `construction/u2-llm-client/code-generation/code-generation-plan.md` (khối `## Testing Contract` sao chép nguyên văn)
- Mã hiện có: `service/{CallService.java, CallPipelineStep.java, CallPipelineService.java, SttService.java}`, `lead/LeadService.java`, `task/{TaskService.java (createFromCall 5 tham số), TaskDraft.java}`, `llm/{LlmClient.java, LlmRequest.java, LlmResponse.java, LlmException.java, LlmPurpose.java, MockLlmClient.java, JsonResponses.java, MessageBuilder.java}`, `copilot/{CopilotService.java, CopilotAnswer.java, Citation.java}`, `domain/{CallRecord.java, TranscriptTurn.java, PipelineStatus.java, PipelineStep.java}`, `web/{ApiExceptionHandler.java, CallController.java, dto/CallDto.java}`, `config/AppProperties.java`, `public/js/api.js`, root `3-ket-qua-cuoc-goi.html`, `4-lua-chon-dieu-huong.html`

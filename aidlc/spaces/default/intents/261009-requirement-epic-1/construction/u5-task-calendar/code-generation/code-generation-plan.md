# Code Generation Plan — U5 `u5-task-calendar`

## Summary

- Builds: Unit Công việc & Lịch làm việc (kind `service`) — thực thể `Task` (JPA), `TaskService` với `createFromCall` (I4, idempotent theo `(sourceCallId, clientKey)`) và CRUD lịch (C4: `GET/POST /api/tasks`, `PATCH /api/tasks/{taskId}`), cộng màn lịch tuần S5 và hộp thoại tạo/đổi giờ S6 trên server Node; quyền sở hữu dựa lại U1 (`CurrentRmProvider`, `LeadService.assertOwned`).
- Touches: thêm package mới `backend/src/main/java/com/mb/callassistant/task/**` và các DTO `backend/src/main/java/com/mb/callassistant/web/dto/{TaskDto,TaskCreateRequest,TaskPatchRequest}.java` + `web/TaskController.java`; mở rộng `config/DemoDataSeeder.java` (seed vài Task demo, có một việc `KHAC` không gắn lead); thêm `public/screens/5-lich-lam-viec.html` + `public/js/calendar.js` và một route trong `server/index.js`; mở rộng khối JaCoCo include trong `backend/pom.xml` cho `com.mb.callassistant.task.*`.
- Tests: JUnit5 + Mockito (+ MockMvc cho lớp API), ~18–24 test bao phủ idempotent `createFromCall`, `clientKey` bắt buộc, ownership 403/404, validate tạo/đổi giờ, `confirmPast`, cap 42 ngày, lọc loại `KHAC`, TODO→DONE idempotent; sàn JaCoCo ≥ 80% dòng cho package U5 — **DỜI** sang đợt sau theo yêu cầu người dùng (xem mục bên dưới).

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

- KHÔNG tạo bất kỳ file test nào (không tạo `backend/src/test/java/com/mb/callassistant/task/**`).
- KHÔNG chạy `mvn verify`, `mvn test`, `npm test` hay bất kỳ lệnh build/test nào.
- Các bước có nhãn **(DỜI — test)** dưới đây vẫn giữ nguyên trong kế hoạch để đợt test sau thực hiện; chúng mô tả đúng việc test-after theo từng lớp.

Lưu ý ràng buộc không được nới: **sàn độ phủ 80% dòng cho package `com.mb.callassistant.task` KHÔNG bị hạ.** Nó chỉ được kiểm chứng ở đợt test sau. Phương pháp test-after theo `## Testing Contract` ở trên vẫn là nguồn chân lý; mục này chỉ dời thời điểm thực thi các bước test, không đổi phương pháp và không hạ mục tiêu chất lượng.

## Steps

- [x] **Step 1 — Dựng cấu trúc package và khung thực thể/cấu hình** *(cấu trúc/cấu hình)*
  - Tạo package `backend/src/main/java/com/mb/callassistant/task/`.
  - Thêm các enum của U5 theo khuôn enum U1 (`LeadStage`/`LeadPriority`): `task/TaskType.java` `{ LICH_HEN, NHAC_GOI, TASK_ZALO, KHAC }`, `task/TaskPriority.java` `{ CAO, THUONG }`, `task/TaskStatus.java` `{ TODO, DONE }`, `task/TaskOrigin.java` `{ AI_FROM_CALL, MANUAL, SEED }`.
  - Không thêm cấu hình property mới; dùng `CurrentRmProvider`/`LeadService` sẵn có của U1 và `@Profile("demo")` sẵn có.
  - BR: BR6.1, BR6.2. Files: `task/TaskType.java`, `task/TaskPriority.java`, `task/TaskStatus.java`, `task/TaskOrigin.java`.

- [x] **Step 2 — Xác minh test runner + ghi lệnh test phạm vi unit** *(test runner readiness)*
  - Brownfield: xác nhận lệnh test phạm vi **chỉ U5** chạy được trước bước test-first đầu tiên. Ghi lệnh chính xác vào `unit-test-instructions.md`:
    `mvn -f backend/pom.xml -Dtest="com.mb.callassistant.task.**.*Test" verify`.
  - KHÔNG chạy lệnh trong đợt này (xem "Thay đổi theo yêu cầu người dùng"); chỉ ghi nhận.
  - BR: NFR2 (nền tảng chất lượng). File: `unit-test-instructions.md`.

- [x] **Step 3 — Thực thể `Task` (lớp mô hình dữ liệu)** *(data model — implement)*
  - `task/Task.java`: `@Entity` JPA theo khuôn `lead/Lead.java` — `@Id String taskId`; `ownerRmId` (bắt buộc), `leadId` (nullable), `sourceCallId` (nullable), `clientKey` (nullable); `title` (≤200), `type` (`@Enumerated(STRING) TaskType`), `priority` (`TaskPriority`, mặc định `THUONG`), `dueAt` (`OffsetDateTime`, offset +07:00 — BR6.1), `durationMinutes` (Integer, mặc định 30), `status` (`TaskStatus`, mặc định `TODO`), `origin` (`TaskOrigin`), `aiSummary` (nullable), `note` (nullable, ≤1000), `createdAt`, `completedAt` (nullable).
  - Bất biến `entities.md`: `completedAt != null` ⇔ `status = DONE`; `type = KHAC` chỉ khi `leadId = null`. Đặt phương thức `markDone()` (set `status=DONE`, `completedAt=now`, idempotent) và `reschedule(dueAt)` giữ nguyên `status` để lớp nghiệp vụ dùng.
  - AC: AC3.2.2; BR: BR5.1, BR6.1. File: `task/Task.java`.

- [ ] **Step 3T — (DỜI — test) Test thực thể `Task`**
  - `markDone` đặt `completedAt` và idempotent; `reschedule` không đổi `status`; bất biến `completedAt`/`status` giữ đúng.
  - BR: BR5.1. File (đợt sau): `backend/src/test/java/com/mb/callassistant/task/TaskTest.java`.

- [x] **Step 4 — Repository (lớp truy cập dữ liệu)** *(repository — implement)*
  - `task/TaskRepository.java` extends `JpaRepository<Task, String>` theo khuôn `lead/LeadRepository.java`, với các truy vấn dẫn xuất:
    - `Optional<Task> findByTaskIdAndOwnerRmId(String taskId, String ownerRmId)` — cơ sở ownership 403/404 (BR1.1/BR1.2).
    - `List<Task> findByOwnerRmIdAndDueAtBetween(String ownerRmId, OffsetDateTime from, OffsetDateTime to)` — truy vấn khoảng (BR4.1/BR4.4).
    - `List<Task> findByOwnerRmIdAndDueAtBetweenAndType(..., TaskType type)` — lọc loại (BR4.3).
    - `List<Task> findByOwnerRmIdAndSourceCallId(String ownerRmId, String sourceCallId)` — khối "AI Key-in từ cuộc gọi" (BR4.6).
    - `Optional<Task> findByOwnerRmIdAndSourceCallIdAndClientKey(..., String clientKey)` — khoá idempotent `createFromCall` (BR2.1).
  - AC: AC3.2.1, AC3.2.3, AC3.2.5; BR: BR1.1, BR2.1, BR4.1, BR4.3, BR4.6. File: `task/TaskRepository.java`.

- [ ] **Step 4T — (DỜI — test) Test repository**
  - `@DataJpaTest`: lọc theo khoảng/loại/sourceCallId trả đúng tập; tra `(sourceCallId, clientKey)` tìm đúng bản ghi.
  - BR: BR2.1, BR4.1, BR4.3, BR4.6. File (đợt sau): `backend/src/test/java/com/mb/callassistant/task/TaskRepositoryTest.java`.

- [x] **Step 5 — `TaskService` (lớp nghiệp vụ, gồm I4)** *(business logic — implement)*
  - `task/TaskService.java` (`@Service`) theo khuôn `lead/LeadService.java`; inject `TaskRepository`, `CurrentRmProvider`, `LeadService` (U1, I1). Dùng `web.ValidationException(field, msg)`, `ForbiddenException`, `NotFoundException` sẵn có. Múi giờ `Asia/Ho_Chi_Minh`/offset +07:00 (BR6.1); hằng số `ZoneId VN`.
  - `createFromCall(rmId, leadId, callId, List<TaskDraft> drafts)` (I4): `assertCurrentRm(rmId)` (BR1.1); nếu `leadId != null` → `leadService.assertOwned(rmId, leadId)` (BR1.3); validate **mọi** draft trước khi tạo gì — thiếu `clientKey` → `ValidationException("clientKey", ...)` (BR2.2), `title`/`dueAt`/`type ∈ {LICH_HEN,NHAC_GOI,TASK_ZALO}` (BR3.1/BR3.2/BR6.2); mỗi draft tra `(callId, clientKey)` → có thì trả `taskId` cũ, chưa có thì tạo `Task` `origin=AI_FROM_CALL`, `sourceCallId=callId`, `aiSummary=` tham số truyền sang (BR2.3); trả `List<String> taskId[]` đúng thứ tự (`@Transactional`).
  - `createManual(TaskCreate)` (POST /api/tasks): validate `title`/`dueAt` (BR3.1), `durationMinutes ∈ {15,30,60}`/`type`/`priority` (BR6.2), `KHAC` chỉ khi `leadId=null` (BR3.2); `leadId != null` → `assertOwned` (BR1.3); tạo `origin=MANUAL`, `status=TODO`.
  - `listTasks(from, to, type?, sourceCallId?)`: cap 42 ngày + `from ≤ to` → `ValidationException` (BR4.1); lọc `ownerRmId=currentRm` (BR1.1); gồm cả DONE và việc không gắn lead (BR4.4); `type` chỉ định dùng truy vấn lọc, `KHAC` không lọt khi lọc loại cụ thể (BR4.3); `sourceCallId` → truy vấn khối panel trái (BR4.6).
  - `markDone(taskId)` / `reschedule(taskId, dueAt, confirmPast)` (PATCH): `findByTaskIdAndOwnerRmId` → không có mà của chính RM → 404 (BR1.2), của RM khác → 403 (BR1.1); `markDone` TODO→DONE idempotent (BR5.1); `reschedule` `dueAt < now` và `confirmPast != true` → `ValidationException("dueAt", reason=PAST_NOT_CONFIRMED)` (BR5.2), ngược lại cập nhật giữ `status`.
  - AC: AC3.2.1–AC3.2.5, AC3.3.1–AC3.3.3, AC3.3.5, AC3.4.1–AC3.4.2; BR: BR1.1–BR1.3, BR2.1–BR2.3, BR3.1–BR3.2, BR4.1–BR4.6, BR5.1–BR5.2, BR6.1–BR6.2. Files: `task/TaskService.java`, `task/TaskDraft.java` (value-object đầu vào I4 — record), `task/TaskCreate.java` (giá trị tạo thủ công — record).
  - Ghi chú ranh giới (BR5.3): U5 **không** tạo cuộc gọi/sửa lead; "Gọi ngay"/"Chi tiết TNB" chỉ là điều hướng của giao diện sang màn U1.

- [ ] **Step 5T — (DỜI — test) Test `TaskService`**
  - `createFromCall` idempotent theo `(callId, clientKey)` (gọi hai lần → một Task, trả lại taskId cũ); thiếu `clientKey` → từ chối cả lời gọi, không tạo gì; `leadId` của RM khác → 403; cap 42 ngày + `from>to` → 400; lọc `KHAC` bị loại khi lọc loại cụ thể; TODO→DONE idempotent; đổi giờ quá khứ chưa `confirmPast` → PAST_NOT_CONFIRMED. Mock `TaskRepository`/`CurrentRmProvider`/`LeadService`.
  - BR: BR1.1–BR1.3, BR2.1–BR2.2, BR4.1, BR4.3, BR5.1–BR5.2. File (đợt sau): `backend/src/test/java/com/mb/callassistant/task/TaskServiceTest.java`.

- [x] **Step 6 — Controller REST C4 (lớp API)** *(API/endpoint — implement)*
  - `web/TaskController.java` theo khuôn `web/LeadController.java`/`web/CallController.java`: `GET /api/tasks` (tham số `from`, `to`, `type?`, `sourceCallId?`), `POST /api/tasks` (201), `PATCH /api/tasks/{taskId}` (status=DONE và/hoặc dueAt + confirmPast). Dùng `@Valid` (bean validation sẵn có) cho request body.
  - DTO record theo khuôn `web/dto/CallDto.java` (có factory `from`): `web/dto/TaskDto.java` (gồm trường read-only denormalised `leadCode`, `customerName`, `aiSummary` lấy từ U1/`Task` — ghi chú advisory review), `web/dto/TaskCreateRequest.java`, `web/dto/TaskPatchRequest.java`. `TaskDto.from(Task, leadCode?, customerName?)` điền denormalised; `leadCode`/`customerName` đọc qua `LeadService` khi `leadId != null`, null khi không gắn lead.
  - Mã lỗi và `ErrorEnvelope` do `web/ApiExceptionHandler.java` sẵn có xử lý (VALIDATION_FAILED/FORBIDDEN/NOT_FOUND); không thêm handler mới trừ khi thiếu mapping.
  - AC: AC3.2.1–AC3.2.3, AC3.3.1–AC3.3.3, AC3.3.5; BR: BR1.1–BR1.2, BR4.1, BR4.3, BR5.2. Files: `web/TaskController.java`, `web/dto/TaskDto.java`, `web/dto/TaskCreateRequest.java`, `web/dto/TaskPatchRequest.java`.

- [ ] **Step 6T — (DỜI — test) Test controller (MockMvc)**
  - `@WebMvcTest(TaskController.class)` mock `TaskService`: 200/201 happy-path; 400 khi khoảng > 42 ngày và khi thiếu tiêu đề; 404 taskId của chính RM không tồn tại; 403 khi service ném ForbiddenException; 400 PAST_NOT_CONFIRMED; `type=KHAC` trả đúng tập KHAC (hành vi đã document).
  - BR: BR1.1–BR1.2, BR3.1, BR4.1, BR4.3, BR5.2. File (đợt sau): `backend/src/test/java/com/mb/callassistant/task/TaskControllerTest.java`.

- [x] **Step 7 — Màn S5 lịch tuần + hộp thoại S6 (lớp giao diện)** *(frontend — implement)*
  - Tạo `public/screens/5-lich-lam-viec.html` dựa trên root mockup `5-lich-lam-viec.html` (nguồn chân lý UI): lịch tuần T2–T7 08:00–17:00 + dòng "Ngoài giờ" (BR4.2); panel trái "AI Key-in từ cuộc gọi" (gọi `GET /api/tasks?sourceCallId=`) và "Lịch trình còn lại trong ngày" (AC3.2.1/AC3.2.5); bộ lọc loại Tất cả/LICH_HEN/NHAC_GOI/TASK_ZALO (BR4.3); chế độ xem Ngày/Tuần/Tháng/Danh sách (US3.4), Danh sách tách done/upcoming; trạng thái rỗng "Không có công việc trong tuần này" (AC3.2.4); thẻ việc có nút "Đã xong"/"Đổi giờ"/"Gọi ngay"/"Chi tiết TNB" (điều hướng S2/S1 của U1 — BR5.3); hộp thoại S6 tạo công việc + cảnh báo vàng "Thời điểm này đã qua. Vẫn lưu?" khi đổi giờ quá khứ (AC3.3.3). Ẩn các nút Outlook/Google và Zalo ZNS (A4).
  - `public/js/calendar.js`: logic gọi API (dùng khuôn `public/js/api.js`), dựng lịch, lọc, chuyển chế độ xem, confirmPast. Thêm `data-testid` cho nút/ô tương tác (`calendar-task-done-btn`, `calendar-task-reschedule-btn`, `calendar-filter-select`, `calendar-view-tabs`, `task-create-form-submit`, `reschedule-past-confirm` …) theo guide tự động hoá.
  - Thêm route phục vụ màn trong `server/index.js` theo khuôn S1/S2: `app.get('/screens/5-lich-lam-viec.html', ...)` và lối tắt `app.get('/calendar', ...)`.
  - AC: AC3.2.1–AC3.2.5, AC3.3.1–AC3.3.5, AC3.4.1–AC3.4.2; BR: BR4.2–BR4.6, BR5.1–BR5.3. Files: `public/screens/5-lich-lam-viec.html`, `public/js/calendar.js`, `server/index.js`.

- [ ] **Step 7T — (DỜI — test) Test hành vi giao diện**
  - Kiểm hành vi JS thuần tuý (gom ô giờ/"Ngoài giờ", lọc loại loại KHAC, tách done/upcoming, bật cờ confirmPast) qua `npm test` phạm vi file calendar; không chạy trong đợt này.
  - BR: BR4.2, BR4.3, BR4.5, BR5.2. File (đợt sau): `tests/` phần Node (phạm vi file `calendar`).

- [x] **Step 8 — Dữ liệu mẫu & cấu hình build** *(env/build config)*
  - Mở rộng `config/DemoDataSeeder.java`: inject `TaskRepository`; seed vài `Task` cho RM demo trong tuần hiện tại — gồm việc gắn lead demo (LICH_HEN/NHAC_GOI, có việc DONE), ít nhất **một việc `KHAC` không gắn lead** (origin `SEED`, `leadId=null`) để minh hoạ bộ lọc "Tất cả" (A2, BR3.2/BR4.3). Idempotent: chỉ seed khi chưa có Task của RM demo.
  - Mở rộng khối JaCoCo `includes` trong `backend/pom.xml`: thêm `com.mb.callassistant.task.*` vào rule BUNDLE với sàn `0.80` — **không hạ sàn**.
  - BR: A2, BR3.2, NFR2. Files: `config/DemoDataSeeder.java`, `backend/pom.xml`.

- [x] **Step 9 — Tài liệu & traceability** *(docs & traceability)*
  - JavaDoc trên `TaskService`/`TaskController` theo kiểu file hiện có; cập nhật `backend/README.md` một đoạn ngắn về endpoint `/api/tasks` và màn `/calendar`.
  - Ở Step 5 của stage (sau sinh mã) sẽ tạo `source-manifest.json` (liệt kê mọi path U5 tạo/sửa) và `traceability.json` cho mọi AC/BR của U5.
  - BR: NFR2. Files: `backend/README.md`, (ở Step 5 stage) `source-manifest.json`, `traceability.json`.

## Story traceability

U5 là phần bổ sung của US-03 (Lịch sử & Lịch làm việc), phục vụ US3.2–US3.4; `createFromCall` (I4) phục vụ US2.3 của U4 (`unit-of-work-story-map.md`). Mỗi bước ánh xạ về story và AC/BR:

| Step | Story | AC | BR |
|---|---|---|---|
| Step 1 | US3.2–US3.4 (nền enum/cấu trúc) | — | BR6.1, BR6.2 |
| Step 3 | US3.2, US3.3 (thực thể Task) | AC3.2.2, AC3.3.1 | BR5.1, BR6.1 |
| Step 4 | US3.2 (truy vấn lịch/panel) | AC3.2.1, AC3.2.3, AC3.2.5 | BR1.1, BR2.1, BR4.1, BR4.3, BR4.6 |
| Step 5 | US2.3 (I4), US3.2–US3.4 | AC3.2.1–AC3.2.5, AC3.3.1–AC3.3.3, AC3.3.5, AC3.4.1–AC3.4.2, AC2.3.6 | BR1.1–BR1.3, BR2.1–BR2.3, BR3.1–BR3.2, BR4.1–BR4.6, BR5.1–BR5.2 |
| Step 6 | US3.2–US3.3 (API C4) | AC3.2.1–AC3.2.3, AC3.3.1–AC3.3.3, AC3.3.5 | BR1.1–BR1.2, BR4.1, BR4.3, BR5.2 |
| Step 7 | US3.2–US3.4 (màn S5/S6) | AC3.2.1–AC3.2.5, AC3.3.1–AC3.3.5, AC3.4.1–AC3.4.2 | BR4.2–BR4.6, BR5.1–BR5.3 |
| Step 8 | US3.2 (dữ liệu mẫu không gắn lead) | AC3.2.2 | A2, BR3.2, BR4.3 |
| Step 9 | tất cả (bàn giao/truy vết) | — | NFR2 |

Các AC US3.1 (tóm tắt AI trong lịch sử hoạt động) thuộc U4, không nằm trong U5. US4.x (đăng nhập/danh sách lead) thuộc U6; U5 chỉ dựa lại `CurrentRmProvider` cho RM hiện tại.

## Sources

- `.kiro/aidlc-common/stages/construction/code-generation.md` (Step 2)
- `construction/u5-task-calendar/functional-design/{entities.md, rules.md, functional-spec.md, traceability.json}` — nguồn chân lý thực thể/quy tắc/luồng của U5
- `inception/units-generation/unit-of-work.md` (U5), `inception/units-generation/unit-of-work-story-map.md`
- `inception/contract-design/contract-summary.md` — C4 (`/api/tasks`, from/to, confirmPast, mã lỗi), I1 (`CurrentRmProvider`, `LeadService.assertOwned`), I4 (`TaskService.createFromCall`), `TaskDraft`
- `inception/domain-design/components.md` — TaskComponent
- `inception/user-stories/stories.md` — US3.2 (AC3.2.1–AC3.2.5), US3.3 (AC3.3.1–AC3.3.5), US3.4 (AC3.4.1–AC3.4.2), AC2.3.6
- `inception/refined-mockups/{mockups.md (S5, S6), interaction-spec.md, design-system-mapping.md}`
- `inception/requirements-analysis/requirements.md` — FR1.4, FR4.7, FR5.2–FR5.5, NFR2, A2, A4
- Mã hiện có: `backend/src/main/java/com/mb/callassistant/lead/{LeadService.java, LeadRepository.java, Lead.java, CurrentRmProvider.java, LeadStage.java}`, `web/{ApiExceptionHandler.java, dto/CallDto.java, ValidationException.java, ForbiddenException.java, NotFoundException.java}`, `config/DemoDataSeeder.java`, `backend/pom.xml`, `server/index.js`, root mockup `5-lich-lam-viec.html`, `4-lua-chon-dieu-huong.html` (chỉ điều hướng vào lịch)

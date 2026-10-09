# Code Generation Plan — U1 `u1-lead-call-foundation`

## Summary

- Builds: nền tảng Lead & cuộc gọi cho CRM — thực thể/REST cho Customer, Lead, Activity, CallRecord và chế độ demo RM mẫu; kiểm quyền sở hữu phía server (FR1.4); tạo/kết thúc cuộc gọi idempotent + điều phối pipeline (I2); giao diện S1 (chi tiết lead) và khung S2 (thực hiện cuộc gọi) theo mockup.
- Touches: `backend/` (các package mới `lead`, `call` trong `com.mb.callassistant`; mở rộng `domain/CallRecord`, `web/CallController`, `service/CallService,CallPipelineService`; `pom.xml`, `application*.properties`, seed), `public/` (S1/S2 + token Tailwind + ES module `api.js`/`ui.js`), `server/` (phục vụ trang S1/S2 — ADR-002).
- Tests: test-after, Standard — JUnit 5 + Mockito + Spring slices, ~5–8 test/lớp cho domain/repository/service/controller (ước lượng ~55–70 test backend), sàn 80% line coverage qua JaCoCo `mvn verify`; không có test frontend bắt buộc ở U1 (chỉ `data-testid` để U4 dùng sau).

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

- Theo yêu cầu "gen code for all units at once, let the test for later": lượt sinh mã này **chỉ hiện thực mã ứng dụng**. Các bước viết/chạy test (Step 4, 6, 8, 10, 12, 14) và phần chạy coverage ở Step 18 được **hoãn lại**, sẽ làm ở một lượt sau (trước/trong Build and Test). Không viết file test trong lượt này.
- Sàn 80% line coverage **không bị hạ**; nó chỉ được kiểm sau khi các test bị hoãn được viết. Step 2 (thêm JaCoCo + dependency test vào `pom.xml`) vẫn làm để lượt test sau chạy được ngay.

## Steps

- [x] **Step 1 — Cấu trúc package & cấu hình production.** Tạo package `com.mb.callassistant.lead` và `com.mb.callassistant.call` (giữ `CallRecord`/`CallService`/`CallPipelineService` hiện có, mở rộng tại chỗ — brownfield, KHÔNG tạo bản sao `*_modified`). Mở rộng CORS trong cấu hình để mở thêm method `PUT`/`PATCH` và header `Authorization` cho origin Node `http://localhost:3000` (ADR-002, contract C1). Thêm cấu hình chế độ Auth `app.auth.mode=demo|login` vào `application.properties` (mặc định `demo`, ADR-007). Secrets chỉ qua biến môi trường, không hard-code (NFR2/BR6.1). Files: `backend/src/main/java/com/mb/callassistant/lead/` (package), `backend/src/main/java/com/mb/callassistant/call/` (package), `backend/src/main/resources/application.properties`, `backend/src/main/resources/application-demo.properties`. IDs: FR1.1, BR1.3, BR6.1.
- [x] **Step 2 — Thêm/kiểm test runner & ghi lệnh unit-scoped.** Thêm JaCoCo plugin (phiên bản ghim) vào `backend/pom.xml` với `prepare-agent` + `report` gắn vào `verify`, cấu hình sàn 80% line (chỉ là ngưỡng kiểm, KHÔNG hạ để qua step — NFR4); `spring-boot-starter-test` do Boot parent quản lý (không ghi version). Ghi lệnh chạy test unit-scoped chính xác vào `unit-test-instructions.md` (xác minh brownfield trước test-first đầu tiên). Files: `backend/pom.xml`. IDs: NFR4.
- [x] **Step 3 — Data model/entity & schema.** Mở rộng `domain/CallRecord` thêm `leadId`, `rmId`, `pipelineStatus` (enum `PipelineStatus`), `pipelineStep` (enum `PipelineStep`), giữ nguyên `tnbCode`/`rmName`/`sttStatus`/`recordingName` để không hỏng trang cũ (entities.md). Tạo entity `RmUser`, `Customer`, `Lead` (enum `LeadPriority`, `LeadStage`), `Activity` (enum `ActivityType`). Thêm trường dẫn xuất `phoneValid` (tính từ `Customer.phoneNumber`, không lưu cột — advisory). Tiền = integer VND; thời gian = `Instant`/offset +07:00 (BR6.2). Files: `backend/src/main/java/com/mb/callassistant/domain/CallRecord.java`, `.../domain/PipelineStatus.java`, `.../domain/PipelineStep.java`, `.../lead/RmUser.java`, `.../lead/Customer.java`, `.../lead/Lead.java`, `.../lead/LeadPriority.java`, `.../lead/LeadStage.java`, `.../lead/Activity.java`, `.../lead/ActivityType.java`. IDs: FR1.3, BR6.2, BR6.3.
- [ ] **Step 4 — Test lớp data model.** Viết test cho enum/invariant thực thể: `CallRecord` kết thúc idempotent (endedAt đặt một lần — AC1.1.3/BR4.1), `phoneValid` dẫn xuất đúng/sai theo định dạng số VN (BR3.1), ánh xạ enum `LeadStage`/`PipelineStatus`. Files: `backend/src/test/java/com/mb/callassistant/domain/CallRecordTest.java`, `.../lead/CustomerTest.java`, `.../lead/LeadTest.java`. IDs: AC1.1.3, BR3.1, BR4.1.
- [x] **Step 5 — Repository/data access.** Tạo `RmUserRepository`, `CustomerRepository`, `LeadRepository` (truy vấn theo `ownerRmId`; truy vấn lead kế tiếp theo `stage ∈ {CHUA_TIEP_CAN, DANG_TIEP_CAN}`), `ActivityRepository` (theo `leadId` sắp `occurredAt` giảm dần — BR2.2). Tái dùng `CallRecordRepository` hiện có. Files: `backend/src/main/java/com/mb/callassistant/lead/RmUserRepository.java`, `.../lead/CustomerRepository.java`, `.../lead/LeadRepository.java`, `.../lead/ActivityRepository.java`. IDs: FR1.3, FR4.9, BR2.2, BR5.1.
- [ ] **Step 6 — Test repository (@DataJpaTest, H2).** Kiểm truy vấn: lead theo RM, lịch sử Activity sắp giảm dần, tập ứng viên "lead kế tiếp" đúng trạng thái. Files: `backend/src/test/java/com/mb/callassistant/lead/LeadRepositoryTest.java`, `.../lead/ActivityRepositoryTest.java`. IDs: FR4.9, BR2.2, BR5.1.
- [x] **Step 7 — Business logic: Auth & Lead (I1).** `CurrentRmProvider` (demo trả RM mẫu cố định — BR1.3/ADR-007). `LeadService` hiện thực I1: `getOwnedLead`, `assertOwned` (403 khi khác RM — BR1.1; 404 khi của chính RM mà không có — BR1.2), `updateStage` (ép tập chuyển hợp lệ SM1), `recordCallActivity` (idempotent theo callId, dành cho U4 qua I1), `findNextLead` (BR5.1). Files: `backend/src/main/java/com/mb/callassistant/lead/CurrentRmProvider.java`, `.../lead/DemoCurrentRmProvider.java`, `.../lead/LeadService.java`. IDs: FR1.3, FR1.4, FR4.9, BR1.1, BR1.2, BR1.3, BR2.1, BR5.1.
- [ ] **Step 8 — Test business logic Auth & Lead.** `LeadService`: 403/404 phân biệt (BR1.1/BR1.2), `findNextLead` sắp đúng ưu tiên và loại `excludeLeadId` (BR5.1), `recordCallActivity` idempotent, `updateStage` từ chối chuyển ngoài tập (SM1). Mock repository. Files: `backend/src/test/java/com/mb/callassistant/lead/LeadServiceTest.java`, `.../lead/DemoCurrentRmProviderTest.java`. IDs: AC4.3.1, AC4.3.2, BR1.1, BR1.2, BR5.1.
- [x] **Step 9 — Business logic: Call & pipeline (I2).** Mở rộng `CallService`/`CallPipelineService`: tạo cuộc gọi gắn `leadId`+`rmId` (kiểm quyền lead qua `LeadService.assertOwned` — BR3.1; 400 khi số không hợp lệ; 409 khi đã có cuộc gọi chưa xử lý xong — BR3.2); kết thúc idempotent đặt `endedAt`/`durationSeconds`, chuyển `IN_CALL → PROCESSING`, chạy pipeline đúng một lần (BR4.1). Hiện thực I2 `CallService.getOwnedCall`/`updatePipeline` (chỉ U1 ghi trạng thái — BR4.2) và điểm mở rộng `CallPipelineStep` (U4 đăng ký ANALYZE sau). Máy trạng thái SM2 có lối thoát `AI_FAILED → PROCESSING` (chạy lại thủ công — advisory, nhất quán U4). Files: `backend/src/main/java/com/mb/callassistant/call/CallService.java` (mở rộng từ `service/CallService.java` theo cấu trúc hiện có), `.../call/CallPipelineStep.java`, `.../call/CallService.java` (giao diện I2), cập nhật `backend/src/main/java/com/mb/callassistant/service/CallPipelineService.java`. IDs: FR1.5, FR2.2, BR3.1, BR3.2, BR4.1, BR4.2, BR4.3.
- [ ] **Step 10 — Test business logic Call & pipeline.** Tạo cuộc gọi: 403 lead khác RM, 400 số không hợp lệ, 409 vượt giới hạn một cuộc gọi (BR3.1/BR3.2); kết thúc idempotent chạy pipeline đúng một lần dù gọi hai lần (AC1.1.3/BR4.1); `updatePipeline` chỉ cho chuyển hợp lệ SM2 (BR4.3). Mock `RecordingClient`/`SttService`/pipeline — không mạng. Files: `backend/src/test/java/com/mb/callassistant/call/CallServiceTest.java`, `.../call/CallPipelineStepTest.java`. IDs: AC1.1.3, AC4.4.2, BR3.1, BR3.2, BR4.1, BR4.3.
- [x] **Step 11 — API/endpoint layer (C1).** Mở rộng `web/CallController` và thêm `web/LeadController` theo schema C1, giữ nguyên API cũ (`/api/calls/initiate`, `sttStatus` trong response): `GET /api/me`, `GET /api/leads/demo`, `GET /api/leads/{leadId}`, `GET /api/leads/{leadId}/activities`, `GET /api/leads/next`, `POST /api/calls`, `POST /api/calls/{callId}/ended`, `GET /api/calls/{callId}`. DTO (`Rm`, `LeadSummary`, `LeadDetail` kèm `phoneValid`, `Activity`, `Call`), validation ở biên (BR6.3), ánh xạ lỗi qua `ApiExceptionHandler` theo `ErrorEnvelope` (403/404/400/409/204). Không log transcript/secret (NFR2/NFR6). Files: `backend/src/main/java/com/mb/callassistant/web/LeadController.java`, `.../web/CallController.java` (mở rộng), `.../web/dto/` (các record DTO), `.../web/ApiExceptionHandler.java` (bổ sung mã lỗi). IDs: FR1.3, FR1.4, FR1.5, FR2.2, FR4.9, BR1.1, BR1.2, BR6.3.
- [ ] **Step 12 — Test API/endpoint (@WebMvcTest).** `LeadController`: 200 chi tiết đủ khối (AC4.3.1), lịch sử rỗng (AC4.3.2), 403 lead khác RM, 204 không còn lead kế tiếp. `CallController`: 201 tạo cuộc gọi (AC4.4.1), 400 số không hợp lệ (AC4.4.2), 200 kết thúc idempotent. Mock service layer. Files: `backend/src/test/java/com/mb/callassistant/web/LeadControllerTest.java`, `.../web/CallControllerTest.java`. IDs: AC4.3.1, AC4.3.2, AC4.4.1, AC4.4.2.
- [x] **Step 13 — Seed dữ liệu demo.** Mở rộng `config/DemoDataSeeder` seed một RM mẫu, Customer + Lead mẫu (khớp nội dung mockup S1), vài Activity, để `GET /api/leads/demo` mở thẳng S1 (ADR-007, A2). Chỉ bật ở profile `demo` (H2). Files: `backend/src/main/java/com/mb/callassistant/config/DemoDataSeeder.java`. IDs: FR1.2, FR1.3, A2.
- [ ] **Step 14 — Test seed/integration (@SpringBootTest, profile demo).** Kiểm `GET /api/leads/demo` trả LeadDetail đã seed đủ khối; `GET /api/leads/{id}/activities` của lead mẫu. Mock STT/recording client. Files: `backend/src/test/java/com/mb/callassistant/DemoSeedIntegrationTest.java`. IDs: FR1.3, AC4.3.1.
- [x] **Step 15 — Frontend S1 (chi tiết lead).** Dựng `public/screens/1-chi-tiet-tiem-nang-ban.html` theo mockup gốc `1-chi-tiet-tiem-nang-ban.html` (bố cục, token màu, Font Awesome theo `design-system-mapping.md`): thông tin KH, thanh tiến trình giai đoạn, khối Nội dung, Kịch bản bán, Quản lý hoạt động (rỗng → "Chưa có hoạt động"; hoạt động cuộc gọi thiếu tóm tắt → "Không có tóm tắt AI"). Nút **Gọi KH**. ES module `public/js/api.js` (gọi C1), `public/js/ui.js`, `public/js/tailwind-config.js`. `data-testid` trên phần tử tương tác (nút Gọi KH, tab). Escape HTML mọi dữ liệu. Files: `public/screens/1-chi-tiet-tiem-nang-ban.html`, `public/js/api.js`, `public/js/ui.js`, `public/js/tailwind-config.js`. IDs: FR1.3, FR1.5, US4.3, US4.4, NFR5, BR2.2, BR2.3.
- [x] **Step 16 — Frontend khung S2 (thực hiện cuộc gọi).** Dựng `public/screens/2-thuc-hien-cuoc-goi.html` theo mockup `2-thuc-hien-cuoc-goi.html`: trạng thái "Đang gọi", đồng hồ thời lượng, tab Thông tin KH/Chi tiết TNB/Lịch sử, chỗ dành panel Copilot (U3 lắp sau), nút **Kết thúc** → `POST /api/calls/{callId}/ended` rồi chuyển màn (AC1.1.1/AC1.1.2). Nghe sự kiện `end` của MicroSIP SSE (C6) để gọi `/ended` (idempotent). `data-testid` trên nút Kết thúc/tab. Files: `public/screens/2-thuc-hien-cuoc-goi.html`, cập nhật `public/js/api.js`. IDs: FR2.1, FR2.2, US1.1, AC1.1.1, AC1.1.2, AC1.1.3.
- [x] **Step 17 — Node phục vụ trang S1/S2.** Thêm route trong `server/index.js` phục vụ `public/screens/1-chi-tiet-tiem-nang-ban.html` và `2-thuc-hien-cuoc-goi.html` (ADR-002). Giữ nguyên MicroSIP/SSE hiện có. Files: `server/index.js`. IDs: ADR-002, FR2.1.
- [ ] **Step 18 — Cấu hình môi trường/build & chạy coverage cục bộ.** Hoàn tất cấu hình JaCoCo + profile `demo`; chạy `mvn verify` (qua IntelliJ Maven tool window — Maven không trên PATH) kiểm sàn 80% cho package U1; sửa test cho đạt sàn mà KHÔNG hạ ngưỡng. (Không Dockerfile/IaC — N/A: chỉ chạy cục bộ, không CI.) Files: `backend/pom.xml`, `backend/src/main/resources/application-demo.properties`. IDs: NFR4.
- [x] **Step 19 — Tài liệu & traceability.** Cập nhật `backend/README.md` (chế độ demo, biến môi trường, chạy test cục bộ); viết `code-summary.md`, `source-manifest.json` và `traceability.json` cho mọi AC/BR của U1. Deployment artifacts (Dockerfile, IaC): **N/A — lý do: dự án chỉ chạy cục bộ, không CI/CD, không triển khai (team.md).** Files: `backend/README.md`, `<code-generation-record>/code-summary.md`, `<code-generation-record>/source-manifest.json`, `<code-generation-record>/traceability.json`. IDs: NFR6, FR1.4.

## Story traceability

| Story / AC | BR liên quan | Steps |
|---|---|---|
| US1.1 / AC1.1.1 | BR1.3, BR2.1 | 16 |
| US1.1 / AC1.1.2 | BR4.1 | 9, 11, 16 |
| US1.1 / AC1.1.3 | BR4.1 | 4, 9, 10, 16 |
| US4.3 / AC4.3.1 | BR1.1, BR2.1 | 7, 11, 12, 14, 15 |
| US4.3 / AC4.3.2 | BR2.2 | 5, 6, 12, 15 |
| US4.4 / AC4.4.1 | BR3.1 | 9, 11, 12, 15 |
| US4.4 / AC4.4.2 | BR3.1, BR6.3 | 9, 10, 11, 12 |
| FR1.4 (quyền sở hữu) | BR1.1, BR1.2 | 7, 8, 11, 12 |
| FR4.9 (lead kế tiếp) | BR5.1 | 5, 6, 7, 8, 11 |
| FR2.2 (kết thúc + pipeline) | BR4.1, BR4.2, BR4.3 | 9, 10, 16 |
| NFR2 (bảo mật) | BR6.1 | 1, 11, 19 |
| NFR4 (test/coverage) | — | 2, 18 |

## Sources

- Stage: `.kiro/aidlc-common/stages/construction/code-generation.md` (Step 2)
- Unit design U1: `functional-design/{entities.md, rules.md, functional-spec.md, traceability.json}`
- Upstream: `inception/units-generation/{unit-of-work.md, unit-of-work-story-map.md}`, `inception/contract-design/contract-summary.md` (C1, C6, I1, I2), `inception/domain-design/{components.md, decisions.md}`, `inception/user-stories/stories.md` (US1.1, US4.3, US4.4), `inception/requirements-analysis/requirements.md` (FR1–FR2, FR4.9, NFR1–NFR7)
- Mockup: `1-chi-tiet-tiem-nang-ban.html`, `2-thuc-hien-cuoc-goi.html`; `inception/refined-mockups/design-system-mapping.md`
- Mã hiện có: `backend/` (`com.mb.callassistant` — CallRecord, CallService, CallPipelineService, CallController, DemoDataSeeder, pom.xml, application*.properties), `server/index.js`
- Thực hành: `aidlc/spaces/default/memory/team.md` (test-after, local-only, no CI), `.../org.md`, `codekb/mb-hackathon-2/`

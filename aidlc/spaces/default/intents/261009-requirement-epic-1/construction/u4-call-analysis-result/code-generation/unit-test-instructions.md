# Hướng dẫn kiểm thử — U4 `u4-call-analysis-result`

> Phương pháp **test-after** theo `## Testing Contract` của kế hoạch (nguồn chân
> lý). **Toàn bộ việc viết và chạy test ở U4 được DỜI sang một đợt sau** theo yêu
> cầu người dùng ("gen code cho tất cả các unit một lượt, phần test để sau"):
> trong đợt sinh mã hiện tại KHÔNG tạo file test và KHÔNG chạy lệnh test. Tài liệu
> này ghi sẵn cách chạy để đợt test sau thực hiện. **Sàn độ phủ 80% dòng KHÔNG bị
> hạ** — chỉ hoãn thời điểm kiểm chứng.

## Khung test & cấu hình

- **Backend**: Spring Boot + JUnit 5 (Jupiter) + Mockito, có sẵn qua
  `spring-boot-starter-test`; đo độ phủ bằng **JaCoCo qua `mvn verify`** (không CI;
  chạy thủ công trên máy lập trình viên trước mỗi lần commit).
- **Phân lớp test U4**:
  - Service nghiệp vụ thuần (`CallScoringService`, `AnalysisResultParser`): JUnit
    thuần, không cần Spring context.
  - Điều phối (`CallAnalysisService`, `KeyinService`): JUnit + **Mockito** mock các
    cộng tác (`LlmClient`, `CallService`, `LeadService`, `TaskService`, repository).
  - API C3 (`AnalysisController`): **`@WebMvcTest(AnalysisController.class)` +
    `MockMvc`**, mock lớp service bằng `@MockBean`.
  - Thực thể/ràng buộc: JUnit thuần (bất biến `criteriaScores` 5 phần tử, `rating`
    1–10, offset +07:00).
- **Frontend (`public/js/api.js`)**: `npm test` kèm đo coverage (test JS giả lập
  `fetch`), theo quy ước Node của nhóm.
- Mọi file test backend đặt dưới
  `backend/src/test/java/com/mb/callassistant/analysis/**` (gương theo package sản
  xuất `com.mb.callassistant.analysis`).

## Cách chạy test CỦA RIÊNG U4

Lệnh **giới hạn đúng phạm vi U4** (không chạy toàn bộ suite), chạy từ gốc dự án,
là một lệnh độc lập:

```
mvn -f backend/pom.xml -Dtest="com.mb.callassistant.analysis.**.*Test" verify
```

- `verify` kích hoạt JaCoCo để báo độ phủ cho package U4.
- Phần giao diện (khi tới đợt test JS): `npm test` ở gốc dự án (hoặc theo
  `package.json`), chỉ các file test cạnh `public/js/`.

**Lưu ý IntelliJ IDEA**: mở lớp test trong package `com.mb.callassistant.analysis`
và chạy bằng nút ▶ ở gutter (class hoặc method), hoặc chuột phải thư mục
`analysis` trong `src/test/java` → **Run Tests in 'analysis'**. Để xem độ phủ,
dùng **Run with Coverage** (biểu tượng khiên) — nhưng con số chính thức trước
commit lấy từ báo cáo JaCoCo của `mvn verify`
(`backend/target/site/jacoco/index.html`), không phải bộ đo của IDE.

## Mục tiêu độ phủ

- **Sàn 80% line coverage** cho package `com.mb.callassistant.analysis` (team
  `## Testing Posture`; scope `classic` cộng sàn 80%). **Không được hạ** để cho
  qua — nếu thiếu phủ thì bổ sung test, không nới ngưỡng trong `pom.xml`.
- Strategy **Standard**: khoảng 5–8 test mỗi component, kèm integration ở biên
  chính (`@WebMvcTest` cho C3; test giao dịch lưu keyin).
- Suite hiện có phải **giữ xanh** (`classic`): bước ANALYZE mới cắm vào
  `CallPipelineService` không được làm hỏng pipeline STT đang chạy của U1.

## Hướng dẫn mock/stub

- **`LlmClient`**: mock Mockito. Kịch bản bắt buộc — JSON hợp lệ (ANALYZED), JSON
  sai lần 1 rồi đúng lần 2 (chạy lại đúng một lần), sai cả hai lần (AI_FAILED),
  ném `LlmException` các `Kind`. KHÔNG gọi provider thật trong test.
- **`MockLlmClient`** (U2, đã sửa ở Step 9): dùng cho test demo/offline đầu-cuối —
  xác nhận canned JSON `CALL_ANALYSIS` khớp schema U4 và `COPILOT` khớp các trường
  `CopilotService` đọc.
- **`CallService` / `LeadService` / `TaskService`**: mock để xác minh U4 gọi đúng
  — `updatePipeline(callId, PROCESSING/ANALYZE | DONE | AI_FAILED)`, `updateStage`,
  `recordCallActivity(... aiSummary, aiSuggestions)`, và
  `createFromCall(rmId, leadId, callId, callAiSummary, drafts)` **đúng 5 tham số**;
  xác minh keyin lưu trong **một giao dịch** và hoàn tác khi một bước lỗi (dùng
  `doThrow` ở một cộng tác).
- **Repository**: mock cho test đơn vị; nếu cần kiểm JPA thật thì
  `@DataJpaTest` với H2 trong bộ nhớ.
- **Quyền sở hữu**: stub `CurrentRmProvider`/`CallService.getOwnedCall` để dựng cả
  nhánh 403 (khác RM) lẫn 404 (chưa có).

## Dữ liệu test

- **Chấm điểm**: bộ `10;10;9,5;9;9,5` → `totalScore=96`, `grade=A` (AC2.2.1); tổng
  tính được `84` → `grade=B`, biên `85→A` (AC2.2.4); một tiêu chí ngoài `[0,10]`
  hoặc không chia hết `0,5` → AI thất bại, không lưu (AC2.2.3). Luôn bỏ qua trường
  tổng do LLM trả (BR3.2).
- **suggestedKeyin**: JSON có `demandAmount`/`term`/`agreedRate` kèm `sourceMs`; một
  trường vắng trong transcript → không xuất hiện (không bịa, BR4.1/AC2.3.2).
- **Lưu keyin**: thiếu `leadStage`/`callResult` → 400 `details.field` (AC2.3.5);
  gọi `PUT` hai lần cùng `callId` + cùng `clientKey` → một `CallKeyin`, không task
  trùng (AC2.3.6, idempotent).
- **Retry**: chưa có transcript hoặc đang `ANALYZING` → 409 (BR1.4).
- **Tiền/thời gian**: `demandAmount` integer VND; `savedAt`/`createdAt` ISO-8601
  offset `+07:00` (BR8.3).
- Dùng builder/factory nhỏ cho `CallAnalysis`/`KeyinSave`/`TaskDraft`; tránh JSON
  literal lớn lặp lại; đặt tên biến theo ý nghĩa (`validScores`, `scoreOutOfRange`,
  `keyinMissingRequired`).

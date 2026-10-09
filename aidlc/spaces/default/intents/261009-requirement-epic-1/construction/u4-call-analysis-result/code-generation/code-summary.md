# Tóm tắt sinh mã — U4 `u4-call-analysis-result`

Luồng sau cuộc gọi: bước pipeline `ANALYZE` chấm điểm/tóm tắt bằng `LlmClient`
(I3), server tự tính `totalScore`/`grade`, lưu `CallAnalysis`; API C3 (xem kết
quả, chạy lại AI, lấy/lưu keyin một-giao-dịch idempotent); màn S3/S4 và khối
tóm tắt AI ở lịch sử hoạt động S1.

## Tệp đã tạo / sửa

Backend (gói mới `com.mb.callassistant.analysis`):
- Enum/VO: `AnalysisGrade`, `CallTone`, `FieldSource`, `CriterionCode`,
  `AnalysisState`, `CriterionScore`, `SuggestedValue`, `SuggestedTask`,
  `AnalysisResult`.
- Thực thể JPA: `CallAnalysis` (callId unique, các value object dạng
  danh sách/bản đồ lưu JSON text), `CallKeyin` (callId unique, tiền integer VND,
  lãi suất decimal, `fieldSources` JSON), `AiFeedback` (rating 1–10, tags, comment).
- Repository: `CallAnalysisRepository`, `CallKeyinRepository`,
  `AiFeedbackRepository` (đều `findByCallId`).
- Nghiệp vụ: `CallScoringService` (kiểm 5 tiêu chí, điểm 0–10 bước 0,5,
  `totalScore=round(tổng×2)`, hạng theo ngưỡng cấu hình), `AnalysisResultParser`
  (parse JSON AI, bỏ trường vắng trong `suggestedKeyin`), `AnalysisInvalidException`,
  `AnalysisJson` ((de)serialize JSON cột), `CallAnalysisService` (điều phối WF1:
  dựng prompt DATA, gọi LLM `CALL_ANALYSIS`, chạy lại 1 lần, lưu idempotent, đặt
  DONE/AI_FAILED qua U1), `AnalyzeCallPipelineStep` (I2),
  `KeyinSave`/`KeyinSaveResult`/`KeyinService` (lưu một-giao-dịch, retry 409).
- API C3: `analysis/web/AnalysisController` + DTO (`CallAnalysisDto` serialize
  `criteriaScores`→`criteria` theo R-01, `CriterionScoreDto`, `SuggestedValueDto`,
  `TaskDraftDto`, `KeyinSaveDto`, `TaskDraftRequest`, `KeyinDto`, `KeyinSavedDto`).
- Sửa tại chỗ: `config/AppProperties` (+`Analysis`), `service/CallPipelineService`
  (chạy các `CallPipelineStep` đã đăng ký sau STT, inject `@Lazy CallService` để
  tránh vòng phụ thuộc), `llm/MockLlmClient` (canned JSON hợp lệ cho COPILOT &
  CALL_ANALYSIS), `application.properties`/`application-demo.properties`,
  `pom.xml` (JaCoCo include `com.mb.callassistant.analysis.*`, giữ sàn 0.80).

Frontend:
- `public/js/api.js`: thêm `getAnalysis`, `retryAnalysis`, `getKeyin`,
  `saveKeyin`, `pollCallStatus` (poll pipelineStatus 3s).
- `public/js/call-result.js` + `public/screens/3-ket-qua-cuoc-goi.html`: S3 ba cột
  (điểm/hạng/tiêu chí/tone/insight; audio + transcript + tải .wav; form keyin
  auto-fill + huy hiệu `AI · mm:ss`/`RM đã sửa` + đánh giá AI + công việc tiếp
  theo), thanh tiến trình 3 bước tự cập nhật, dự phòng theo loại lỗi + nút chạy
  lại AI, hộp thoại S4.
- `public/screens/4-lua-chon-dieu-huong.html`: S4 độc lập 3 lựa chọn.
- `public/screens/2-thuc-hien-cuoc-goi.html`: S2 điều hướng sang S3 khi kết thúc.
- `public/screens/1-chi-tiet-tiem-nang-ban.html`: liên kết "Xem kết quả cuộc gọi".

## Quyết định & sai khác

- `criteriaScores`/`suggestedKeyin`/`suggestedTasks`/`fieldSources` lưu dưới dạng
  JSON text để JPA đơn giản, không nhúng `ObjectMapper` vào thực thể; lớp DTO
  parse lại qua `AnalysisJson`.
- Prompt injection (BR8.1): transcript truyền qua `LlmRequest.userContent`;
  gateway (`MessageBuilder`) đã bọc khối DATA phân tách — không ghép vào
  systemPrompt.
- Ranh giới SM2 (BR1.2): U4 không ghi `pipelineStatus` trực tiếp; mọi cập nhật đi
  qua `CallService.updatePipeline`. `CallPipelineService` mở rộng tại chỗ để chạy
  bước ANALYZE sau STT và map `AI_FAILED`.
- Retry (BR1.4): chỉ cho `AI_FAILED`→`PROCESSING` (DONE là trạng thái cuối), 409
  khi chưa có transcript hoặc đang chạy. Quy tắc "chỉ điền trường trống" (BR4.2)
  thực hiện ở client.
- `createFromCall` gọi đúng 5 tham số thật của U5 `(rmId, leadId, callId,
  callAiSummary, drafts)`, mỗi draft bắt buộc `clientKey`.
- Sai khác với mockup: S3 nối API thật thay cho số liệu cứng trong root mockup;
  giữ bố cục 3 cột, thanh tiến trình, dự phòng lỗi và nút chạy lại.

## Trạng thái test

Hoãn theo yêu cầu người dùng ("gen code cho tất cả các unit một lượt, phần test
để sau"): KHÔNG tạo file test và KHÔNG chạy `mvn verify`/`mvn test`/`npm test`
trong đợt này. Phương pháp vẫn là **test-after** theo `## Testing Contract`; sàn
độ phủ **80% dòng** cho `com.mb.callassistant.analysis` KHÔNG bị hạ — chỉ hoãn
thời điểm kiểm chứng (xem `unit-test-instructions.md`).

## Cấu hình

- `app.analysis.grade-threshold-a|b|c` (85/70/50), `app.analysis.llm-timeout-ms`
  (30000), `app.analysis.max-output-tokens` (2048),
  `app.analysis.recording-wait-max-attempts|ms`. Thêm vào
  `application.properties` và `application-demo.properties`.
- JaCoCo: thêm `com.mb.callassistant.analysis.*` (+`.web`, `.web.dto`) vào rule
  BUNDLE, sàn 0.80.

## Cách demo end-to-end offline

1. Chạy backend với profile `demo` (`SPRING_PROFILES_ACTIVE=demo`): H2 in-memory,
   `app.llm.provider=mock`, tài liệu seed trong `backend/policy-docs`.
2. `MockLlmClient` trả JSON hợp lệ cho `CALL_ANALYSIS` (5 tiêu chí → điểm 96,
   hạng A) và `COPILOT` (trích dẫn tài liệu lãi suất seed).
3. Mở S1 → "Gọi KH" → S2 → "Kết thúc" → tự sang S3: pipeline chạy ANALYZE với
   MOCK, cột điểm/transcript/keyin tự điền; sửa trường bắt buộc, "Lưu hồ sơ cuộc
   gọi" → hộp thoại S4; lịch sử S1 hiện tóm tắt AI + liên kết "Xem kết quả cuộc gọi".

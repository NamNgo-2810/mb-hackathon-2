## Review

**Verdict:** READY
**Reviewer:** aidlc-architecture-reviewer-agent
**Date:** 2026-10-09T10:09:29Z
**Iteration:** 1

Lượt rà soát tư vấn một lần trên phần mã U1 `u1-lead-call-foundation` đã hoàn tất (test được hoãn theo yêu cầu người dùng — không nêu thiếu test). Trọng tâm: lỗi biên dịch tiềm ẩn (mã chưa từng được biên dịch), bảo mật, và tuân thủ hợp đồng C1.

### Findings

| ID | Severity | Location | Finding | Required action | Status |
|---|---|---|---|---|---|
| R-01 | Medium | backend/src/main/java/com/mb/callassistant/service/CallService.java > createFromLead (kiểm BR3.2) | Giới hạn "một cuộc gọi chưa xử lý xong tại một thời điểm" được kiểm trên TOÀN hệ thống qua findByPipelineStatusIn(IN_CALL, PROCESSING), không lọc theo RM hiện tại. Khi có nhiều RM, một cuộc gọi của RM khác sẽ chặn RM hiện tại tạo cuộc gọi và trả 409 (contract C1 mô tả 409 là "đang có một cuộc gọi khác chưa xử lý xong"). Chạy demo một RM thì không lộ, nhưng là sai ngữ nghĩa quyền sở hữu khi U6 bật nhiều RM. | Lọc truy vấn in-flight theo rmId (ví dụ thêm findByRmIdAndPipelineStatusIn) để giới hạn áp dụng cho đúng RM hiện tại, không phải toàn hệ thống. | New |
| R-02 | Low | backend/src/main/java/com/mb/callassistant/service/CallService.java > getOwnedCall | Cuộc gọi có rmId = null (bản ghi cũ, trang demo cũ) được coi là của RM hiện tại nên bỏ qua kiểm 403. Đây là độ lệch đã ghi trong code-summary để không hỏng luồng cũ, nhưng khi login nhiều RM, bản ghi null-rmId lọt kiểm quyền. | Khi bật chế độ login, coi cuộc gọi không có rmId là không thuộc RM nào (404/403) thay vì mặc nhiên thuộc RM hiện tại; hoặc backfill rmId cho dữ liệu cũ trước khi bật login. | New |
| R-03 | Low | backend/src/main/java/com/mb/callassistant/web/dto/LeadDetail.java > trường content | Contract C1 mô tả LeadDetail.content là object (JSON); DTO phơi content dạng String (văn bản thô đã lưu). Giao diện S1 hiện render bằng whitespace-pre-line nên không vỡ, nhưng người tiêu thụ C1 trông đợi object có thể phải tự parse. Độ lệch đã ghi trong code-summary. | Giữ nguyên nếu các bên tiêu thụ C1 (S1/S2) chấp nhận chuỗi; nếu không, phơi content thành object theo contract hoặc ghi rõ trong C1 rằng content là chuỗi tự do. | New |
| R-04 | Low | backend/src/main/java/com/mb/callassistant/service/CallService.java > endCall và markEnded | endCall gọi cr.markEnded(now) (đã chuyển IN_CALL -> PROCESSING và tính durationSeconds) rồi lại tự set pipelineStep = FETCH_RECORDING + sttStatus = PROCESSING. Logic đúng và idempotent, nhưng duration/endedAt do markEnded tính còn step/status do endCall đặt — hai nơi cùng chạm vòng đời kết thúc, dễ lệch khi sửa sau này. | Gộp việc đặt pipelineStep/sttStatus khởi đầu vào markEnded (hoặc một phương thức vòng đời duy nhất) để chỉ một chỗ sở hữu bước chuyển "kết thúc lần đầu". | New |

### Validation Tool Results

| Tool | Result | Interpretation |
|---|---|---|
| Rà biên dịch thủ công (import, kiểu, phương thức tham chiếu, trùng tên lớp) | PASS | Không tìm thấy lỗi biên dịch. LeadStage.OPEN là Set/Collection nên findByOwnerRmIdAndStageIn khớp; mọi phương thức repository/service/exception được tham chiếu đều tồn tại; không có lớp CallService trùng tên (chỉ một ở package service); DTO/record đầy đủ. |
| Rà tham chiếu lớp hiện có (RecordingClient.latestFilename, CallPipelineService.runPipeline, RecordingService.downloadRecording, TranscriptTurnRepository, CallResponse/InitiateCallRequest/TranscriptTurnResponse) | PASS | Mọi lớp/method cũ mà mã mới gọi đều tồn tại với đúng chữ ký. |
| Rà import ES module frontend (api.js, screens.js, config.js) | PASS | public/js/config.js tồn tại và export CONFIG với backendBaseUrl/microsipBaseUrl/pollIntervalMs/pollMaxAttempts; S1/S2 import đúng screens.js (không phải ui.js); hàm api.js dùng đều có export. |
| Rà bảo mật (secrets, 403 quyền sở hữu, không log transcript) | PASS | Không hard-code khoá/secret (RecordingClient/Stt lấy từ AppProperties/env); passwordHash không vào DTO Rm (BR6.1); kiểm quyền 403/404 phân biệt trong LeadService/CallService; CORS mở method + header, không bật allowCredentials. |

### Summary

Mã U1 nhất quán, mọi tham chiếu chéo (Java và ES module) đều phân giải được và không có lỗi biên dịch rõ ràng; bảo mật và tuân thủ C1 ở mức tốt cho bối cảnh demo. Lưu ý đáng cân nhắc nhất là giới hạn một-cuộc-gọi BR3.2 đang kiểm toàn hệ thống thay vì theo RM — vô hại ở demo một RM nhưng cần sửa trước khi U6 bật nhiều RM.

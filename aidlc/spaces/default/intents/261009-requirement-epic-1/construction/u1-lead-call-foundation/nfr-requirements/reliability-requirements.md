# Yêu cầu Độ tin cậy — U1 `u1-lead-call-foundation`

Phạm vi độ tin cậy của unit nền tảng, kế thừa từ `NFR3` (Độ tin cậy) của
inception. U1 làm chủ máy trạng thái pipeline (SM2) và quy tắc kết thúc cuộc
gọi idempotent (BR4.1). Mục tiêu demo: lỗi trong một bước pipeline để lại
`CallRecord` ở trạng thái **phục hồi được**, không chặn việc lưu keyin về sau
(do U4), và cho phép thử lại thủ công. **Không HA, không sao lưu/khôi phục tự
động** cho demo chạy cục bộ.

## Idempotency và tính nhất quán trạng thái

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR3.1 | Kết thúc cuộc gọi idempotent: lần đầu đặt `endedAt` + chuyển `IN_CALL → PROCESSING` + khởi chạy pipeline đúng một lần; lần sau là no-op (BR4.1) | Gửi `POST /api/calls/{callId}/ended` hai lần cho cùng `callId` → pipeline chạy đúng một lần, lần hai trả trạng thái hiện tại (AC1.1.3); có test |
| NFR3.2 | Chỉ U1 ghi `pipelineStatus`/`pipelineStep` qua `CallService.updatePipeline` (BR4.2) | Không có đường ghi trạng thái pipeline nào ngoài `updatePipeline`; U4 chỉ cắm bước qua `CallPipelineStep`; test khẳng định ranh giới |
| NFR3.3 | Chuyển trạng thái chỉ theo tập hợp hợp lệ của SM2 (BR4.3) | Chuyển ngoài tập (ví dụ `DONE → PROCESSING`) bị từ chối; `pipelineStep` null khi ở trạng thái cuối; có test cho các chuyển hợp lệ và không hợp lệ |

## Chịu lỗi và trạng thái phục hồi được

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR3.4 | Lỗi phân biệt được của bước lấy ghi âm/STT dẫn tới trạng thái dự phòng đúng: `NO_RECORDING` (chưa có ghi âm) hoặc `STT_FAILED` (lỗi STT) (BR4.3, WF5) | Khi `CallPipelineStep` báo `RECORDING_NOT_READY` → `NO_RECORDING`; báo `STT_ERROR` → `STT_FAILED`; có test cho hai nhánh |
| NFR3.5 | Lỗi/timeout bước AI để lại `AI_FAILED` — trạng thái phục hồi được, cho phép chạy lại AI (`AI_FAILED → PROCESSING` do U4 kích hoạt) | `CallRecord` giữ `AI_FAILED` với `errorDetail`; chuyển trở lại `PROCESSING` khi retry; cuộc gọi không bị kẹt hay mất dữ liệu |
| NFR3.6 | Lỗi của một bước pipeline **không** làm mất `CallRecord`, không chặn việc lưu keyin về sau (do U4) | Sau lỗi bước, `CallRecord` vẫn đọc được qua `GET /api/calls/{callId}` với trạng thái dự phòng; keyin trống vẫn lưu được (ranh giới U4) |
| NFR3.7 | File ghi âm được giữ trên đĩa (không xóa khi bước lỗi), cho phép thử lại thủ công | Ghi âm vẫn truy cập được sau khi bước lỗi; `recordingName` giữ nguyên; retry không yêu cầu tải lại nguồn |

## Khả dụng và phục hồi (mức demo)

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR3.8 | **Không mục tiêu SLA/HA**; không failover tự động | Pass = một tiến trình cục bộ; mất tiến trình thì khởi động lại thủ công; ghi nhận rõ là chấp nhận cho demo |
| NFR3.9 | Phục hồi là **thủ công**: RM/nhà phát triển thử lại bước lỗi; không có job tự động retry/khôi phục | Sau lỗi, hệ thống chờ hành động thủ công (ví dụ retry AI của U4); không có cơ chế retry nền trong U1 |
| NFR3.10 | Không yêu cầu sao lưu/khôi phục dữ liệu cho demo (dữ liệu seed, chạy cục bộ) | Pass = không có backup bắt buộc; dữ liệu demo tái tạo được bằng seed |

## Suy giảm có kiểm soát

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR3.11 | Khi bước pipeline lỗi, trạng thái dự phòng được phản ánh rõ cho giao diện để màn Kết quả vẫn mở được (ranh giới U4) | `pipelineStatus` + `errorDetail` đủ để giao diện U4 chọn đúng cách hiển thị fallback (NO_RECORDING/STT_FAILED/AI_FAILED theo FR4.8) |

## Sources

- `requirements.md` — NFR3 (độ tin cậy), FR2.2, FR4.1, FR4.8
- `rules.md` (U1) — BR4.1 (idempotent), BR4.2 (chỉ U1 ghi trạng thái), BR4.3 (SM2)
- `functional-spec.md` (U1) — SM2, WF4 (kết thúc idempotent), WF5 (điều phối pipeline)
- `entities.md` (U1) — `CallRecord` (`pipelineStatus`, `pipelineStep`, `errorDetail`, `recordingName`)
- `contract-summary.md` — I2 (`CallService`, `CallPipelineStep`), mã lỗi `RECORDING_NOT_READY`/`STT_ERROR`/`AI_ERROR`
- `unit-of-work.md` — ranh giới U1/U4, Domain Design R-01

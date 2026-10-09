## Review

**Verdict:** READY
**Reviewer:** aidlc-architecture-reviewer-agent
**Date:** 2026-10-09T11:54:59Z
**Iteration:** 1

Lượt rà soát tư vấn (một lượt) cho mã sinh của unit `u5-task-calendar`. Không
raise thiếu test (người dùng đã hoãn). Mọi phụ thuộc U1 (`LeadService`,
`CurrentRmProvider`, `ForbiddenException` / `NotFoundException` /
`ValidationException`, `Customer.getFullName`, `Lead.getLeadCode` /
`getCustomerId`) đều phân giải đúng chữ ký qua hợp đồng và tệp sở hữu. Không
thấy lỗi biên dịch nào bên trong U5. Phân biệt 403/404 theo quyền sở hữu, kiểm
đầu vào ở biên, và escape dữ liệu động ở giao diện đều có mặt.

### Findings

| ID | Severity | Location | Finding | Required action | Status |
|---|---|---|---|---|---|
| R-01 | Major | backend/src/main/java/com/mb/callassistant/task/TaskService.java > createFromCall | I4 trong contract-summary.md ghi chữ ký `createFromCall(rmId, leadId, callId, TaskDraft[])` (4 tham số), nhưng mã hiện thực thêm tham số bắt buộc `callAiSummary` thành 5 tham số. Đây là thay đổi phá vỡ tương thích của giao diện Java mà U4 gọi: chỗ gọi của U4 sẽ không biên dịch được nếu vẫn truyền 4 đối số, hoặc gây sai thứ tự đối số nếu truyền 5. | Giữ chữ ký I4 đúng hợp đồng: hoặc để `aiSummary` đi theo mỗi `TaskDraft` (hoặc một value-object đầu vào), hoặc thêm một overload không phá vỡ tương thích; nếu thực sự cần đổi, cập nhật contract-summary.md và điều phối với U4 trước khi dựa vào. | New |
| R-02 | Minor | public/screens/5-lich-lam-viec.html > lưới chế độ xem Tuần vs public/js/calendar.js > renderWeek | Mockup gốc 5-lich-lam-viec.html vẽ lịch Tuần dạng lưới trục giờ (cột giờ 08:00–17:00 + 6 cột ngày, sự kiện định vị tuyệt đối theo giờ). Mã sinh dựng lưới `grid-cols-6` xếp thẻ chồng, không có trục giờ dọc, nên độ trung thực giao diện so với mockup bị giảm đáng kể dù vẫn hiển thị đúng công việc. | Cân nhắc dựng lưới trục giờ theo mockup (cột giờ + định vị theo dueAt), hoặc ghi nhận rõ đây là đơn giản hoá có chủ đích cho bản demo để người duyệt quyết định ở cổng phê duyệt. | New |
| R-03 | Minor | public/js/calendar.js > renderWeek / reload | Khoảng truy vấn tuần lấy T2–CN và tổng số (calendar-total-count) đếm cả việc Chủ nhật, nhưng renderWeek bỏ qua cột `col < 0` nên việc rơi vào Chủ nhật được tải về mà không bao giờ hiển thị trong lưới Tuần — lệch giữa con số 'Tổng' và số thẻ nhìn thấy. | Hiển thị việc Chủ nhật ở một chỗ (ví dụ gộp vào cột T7 'Ngoài giờ' hoặc một mục riêng), hoặc loại việc Chủ nhật khỏi tổng đếm của chế độ Tuần để con số khớp với lưới. | New |
| R-04 | Minor | aidlc/spaces/default/intents/261009-requirement-epic-1/inception/contract-design/contract-summary.md > ErrorEnvelope details | Tài liệu hợp đồng mô tả `error.details` là một object (`{ field, reason }`), còn ApiExceptionHandler thực tế trả về mảng `[{ field, reason }]` và calendar.js đọc đúng theo mảng. Hai đầu trong unit khớp nhau nên không có lỗi chạy, nhưng tài liệu hợp đồng lệch với hành vi thật. | Cập nhật mô tả `details` trong contract-summary.md thành dạng mảng để tài liệu khớp với envelope lỗi thực tế mà cả backend và giao diện đang dùng. | New |

### Summary

Mã U5 vững và biên dịch được trong phạm vi của chính nó; mối lo chính là chữ
ký I4 lệch hợp đồng (thêm tham số bắt buộc), có rủi ro làm hỏng chỗ gọi của U4
— cần nắn lại cho đúng hợp đồng hoặc điều phối rõ với U4 trước khi dựa vào.
Các điểm còn lại ở mức Minor (độ trung thực lưới Tuần so với mockup, việc Chủ
nhật ẩn khỏi lưới nhưng vẫn vào tổng đếm, và lệch tài liệu hợp đồng về
`details`).

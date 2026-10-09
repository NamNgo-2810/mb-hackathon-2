## Review

**Verdict:** READY
**Reviewer:** aidlc-architecture-reviewer-agent
**Date:** 2026-10-09T08:59:40Z
**Iteration:** 1

### Findings

| ID | Severity | Location | Finding | Required action | Status |
|---|---|---|---|---|---|
| R-01 | Minor | aidlc/spaces/default/intents/261009-requirement-epic-1/construction/u5-task-calendar/functional-design/functional-spec.md > UC6 bước 2 và mục 5 (bảng biên) & rules.md > BR1.2 | Thiết kế quy định PATCH một `taskId` không tồn tại của chính RM trả về `404 NOT_FOUND`, nhưng hợp đồng C4 cho `PATCH /api/tasks/{taskId}` chỉ liệt kê các phản hồi `200`, `400`, `403` — không có `404`. Một lập trình viên hiện thực theo đúng C4 sẽ không có mã trạng thái để diễn đạt tình huống 404 mà BR1.2 yêu cầu. | Bổ sung phản hồi `404` vào endpoint `PATCH /api/tasks/{taskId}` trong hợp đồng C4 (thay đổi dạng thêm vào, do U5 sở hữu), hoặc nêu rõ trong functional-spec vì sao 404 của BR1.2 được diễn đạt khác đi mà vẫn khớp C4. | New |
| R-02 | Minor | aidlc/spaces/default/intents/261009-requirement-epic-1/construction/u5-task-calendar/functional-design/rules.md > BR4.3 (đối chiếu C4 tham số `type`) | Hợp đồng C4 cho phép `GET /api/tasks?type=KHAC` (enum tham số `type` gồm cả `KHAC`), trong khi BR4.3 nói công việc `KHAC` chỉ hiện ở bộ lọc 'Tất cả'. Logic BR4.3 "IF type được chỉ định THEN trả Task có đúng type đó" không nói rõ hành vi khi `type=KHAC` được truyền trực tiếp — trả về `KHAC` (mâu thuẫn với 'chỉ hiện ở Tất cả') hay từ chối/trả rỗng. Người hiện thực phải đoán. | Nêu rõ hành vi của BR4.3 khi `type=KHAC` được truyền tường minh (ví dụ: vẫn trả `KHAC` khi lọc đích danh và chỉ ẩn khỏi các bộ lọc loại khác), hoặc gỡ `KHAC` khỏi enum tham số lọc trong C4 để nhất quán. | New |
| R-03 | Minor | aidlc/spaces/default/intents/261009-requirement-epic-1/construction/u5-task-calendar/functional-design/entities.md > khối YAML entity `Task` (thuộc tính) | Hợp đồng C4 schema `Task` mang hai trường hiển-thị-đọc `leadCode` và `customerName` (do U1 cung cấp), nhưng khối YAML nguồn-chân-lý của `Task` trong entities.md chỉ nhắc chung "một ít trường hiển-thị-đọc (denormalized)" ở phần văn xuôi và không liệt kê hai trường này. Nguồn chân lý thực thể và hợp đồng REST lệch nhau về các trường mà API thực sự trả. | Liệt kê `leadCode` và `customerName` (và `aiSummary` nếu coi là thuộc tính đọc của Task) như thuộc tính denormalized trong khối YAML `Task`, hoặc ghi chú rõ chúng là phần mở rộng chỉ-đọc do U1 cấp ở tầng hợp đồng, không thuộc mô hình thực thể lưu trữ. | New |

### Validation Tool Results

| Tool | Result | Interpretation |
|---|---|---|
| traceability (đọc tay traceability.json) | PASS | 12 AC của US3.2/US3.3/US3.4 đều có trong `upstream_ids` và `coverage` với trạng thái `OK`; mọi `BRx.y` đích (BR4.6, BR2.3, BR4.2, BR4.4, BR4.3, BR5.1, BR5.2, BR5.3, BR3.1, BR4.5, BR4.1) đều tồn tại trong rules.md; các quy tắc nền tảng không gắn AC được giải thích đầy đủ ở `reverse`. Không có BR mồ côi chưa giải thích. |

### Summary

Thiết kế vững và có thể hiện thực được: độ phủ AC đầy đủ, idempotent `(callId, clientKey)` và `clientKey` bắt buộc (BR2.1/BR2.2) chốt đúng ghi chú mở của Contract Design, cap 42 ngày / confirmPast / 403 / ranh giới múi giờ đều nhất quán với C4, và ranh giới sở hữu qua giao diện U1 (I1/assertOwned) được tôn trọng; Outlook/Zalo nằm ngoài phạm vi đúng A4. Ba điểm Minor cần người duyệt cân nhắc đều là lệch nhỏ giữa functional-spec/entities và hợp đồng C4 (404 cho PATCH, hành vi lọc `type=KHAC`, trường denormalized `leadCode`/`customerName`) — không chặn hiện thực nhưng nên làm rõ để tránh phải đoán.

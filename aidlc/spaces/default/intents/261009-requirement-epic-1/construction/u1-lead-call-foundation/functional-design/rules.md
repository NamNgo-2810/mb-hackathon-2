# Business Rules — U1 `u1-lead-call-foundation`

Quy tắc nghiệp vụ của unit nền tảng Lead & cuộc gọi. Khối YAML là **nguồn chân lý**.
Mỗi quy tắc có `id` theo định dạng `BR{nhóm}.{thứ tự}`. Nhóm:
- **BR1** — quyền sở hữu và truy cập (FR1.4)
- **BR2** — chi tiết & lịch sử lead (FR1.3, US4.3, US3.1)
- **BR3** — tạo cuộc gọi (FR1.5, US4.4)
- **BR4** — kết thúc cuộc gọi & pipeline (FR2.2, US1.1)
- **BR5** — chọn lead kế tiếp (FR4.9)
- **BR6** — bảo mật & quy ước dữ liệu (NFR2, contract-summary)

```yaml
rules:
  # ── BR1 — Quyền sở hữu và truy cập ──
  - id: BR1.1
    statement: "Mọi thao tác đọc/ghi lead, cuộc gọi, hoạt động chỉ được phép khi tài nguyên thuộc RM hiện tại."
    category: authorization
    applies_to: [Lead, CallRecord, Activity]
    trigger: "Nhận yêu cầu đọc hoặc ghi một tài nguyên có chủ sở hữu RM"
    logic: >
      IF tài nguyên tồn tại AND tài nguyên.ownerRmId != currentRm.rmId
      THEN từ chối với 403 FORBIDDEN, không tiết lộ tài nguyên có tồn tại hay không, dữ liệu không đổi.
    violation: "403 FORBIDDEN; không thay đổi dữ liệu"
    source: [FR1.4, NFR2]
  - id: BR1.2
    statement: "Khi tài nguyên thuộc đúng RM hiện tại nhưng không tồn tại, trả 404; chỉ phân biệt 404 với 403 trong phạm vi tài nguyên của chính RM."
    category: authorization
    applies_to: [Lead, CallRecord]
    trigger: "Truy cập tài nguyên theo id"
    logic: >
      IF không tìm thấy tài nguyên của currentRm THEN trả 404 NOT_FOUND.
      IF tài nguyên tồn tại nhưng thuộc RM khác THEN trả 403 (BR1.1), không trả 404.
    violation: "404 NOT_FOUND cho tài nguyên của chính RM; 403 cho tài nguyên của RM khác"
    source: [FR1.4]
  - id: BR1.3
    statement: "RM hiện tại được xác định theo chế độ chạy."
    category: policy
    applies_to: [RmUser]
    trigger: "Mọi yêu cầu cần danh tính RM (CurrentRmProvider.currentRm)"
    logic: >
      IF chế độ = demo THEN currentRm = RM mẫu cố định (không cần token).
      IF chế độ = login THEN currentRm = RM của token Bearer hợp lệ; thiếu/sai token -> 401 (U6 hiện thực).
    violation: "401 UNAUTHORIZED ở chế độ login khi thiếu/sai token"
    source: [FR1.1, ADR-007]

  # ── BR2 — Chi tiết & lịch sử lead ──
  - id: BR2.1
    statement: "Chi tiết lead trả về đủ các khối: thông tin KH, giai đoạn, Nội dung, Kịch bản bán, Quản lý hoạt động."
    category: constraint
    applies_to: [Lead, Customer, Activity]
    trigger: "GET /api/leads/{leadId} cho lead của RM"
    logic: >
      THEN trả LeadDetail gồm customer, các trường stage/priority/rankScore/slaDueAt,
      content, salesScript và (qua endpoint hoạt động) danh sách Activity.
    violation: "Thiếu khối bắt buộc coi là lỗi hiển thị chi tiết lead"
    source: [FR1.3]
  - id: BR2.2
    statement: "Lịch sử hoạt động sắp theo thời gian giảm dần; danh sách rỗng hiển thị trạng thái trống rõ ràng."
    category: constraint
    applies_to: [Activity]
    trigger: "GET /api/leads/{leadId}/activities"
    logic: >
      THEN trả mảng Activity sắp theo occurredAt giảm dần.
      IF mảng rỗng THEN giao diện hiển thị 'Chưa có hoạt động' (không để trống).
    violation: "Sai thứ tự hoặc không có trạng thái trống coi là vi phạm"
    source: [FR1.3, FR5.1]
  - id: BR2.3
    statement: "Hoạt động cuộc gọi thiếu tóm tắt AI vẫn hợp lệ và hiển thị 'Không có tóm tắt AI'."
    category: constraint
    applies_to: [Activity]
    trigger: "Hiển thị hoạt động loại CUOC_GOI trong lịch sử"
    logic: >
      IF Activity.aiSummary = null THEN giao diện hiển thị 'Không có tóm tắt AI';
      keyin RM đã nhập vẫn hiển thị bình thường.
    violation: "Ẩn hoạt động hoặc báo lỗi khi thiếu aiSummary là vi phạm"
    source: [FR5.1]

  # ── BR3 — Tạo cuộc gọi ──
  - id: BR3.1
    statement: "Cuộc gọi chỉ được tạo từ một lead của RM hiện tại và chỉ khi số điện thoại hợp lệ."
    category: validation
    applies_to: [CallRecord, Lead, Customer]
    trigger: "POST /api/calls với leadId"
    logic: >
      IF lead không thuộc currentRm THEN 403 (BR1.1).
      IF phoneNumber của KH không hợp lệ THEN 400 VALIDATION_FAILED, không tạo CallRecord.
      ELSE tạo CallRecord gắn leadId + rmId, pipelineStatus = IN_CALL, startedAt = thời điểm hiện tại.
    violation: "400 VALIDATION_FAILED (số không hợp lệ) hoặc 403 (lead không thuộc RM); không tạo cuộc gọi"
    source: [FR1.5]
  - id: BR3.2
    statement: "Chỉ cho phép một cuộc gọi chưa xử lý xong tại một thời điểm."
    category: constraint
    applies_to: [CallRecord]
    trigger: "POST /api/calls"
    logic: >
      IF đã tồn tại CallRecord ở trạng thái chưa kết thúc hoặc đang PROCESSING
      THEN 409 CONFLICT, không tạo cuộc gọi mới.
    violation: "409 CONFLICT"
    source: [FR4, Domain Design R-01]

  # ── BR4 — Kết thúc cuộc gọi & pipeline ──
  - id: BR4.1
    statement: "Kết thúc cuộc gọi là idempotent: xử lý lần đầu đặt endedAt và khởi chạy pipeline; các lần sau không chạy lại."
    category: constraint
    applies_to: [CallRecord]
    trigger: "POST /api/calls/{callId}/ended (RM bấm Kết thúc hoặc MicroSIP báo sự kiện end, có thể lặp)"
    logic: >
      IF endedAt chưa đặt THEN đặt endedAt, tính durationSeconds, chuyển pipelineStatus IN_CALL -> PROCESSING,
      khởi chạy pipeline đúng một lần.
      IF endedAt đã đặt THEN no-op, trả trạng thái cuộc gọi hiện tại (không chạy lại pipeline).
    violation: "Chạy pipeline nhiều hơn một lần cho cùng callId là vi phạm (AC1.1.3)"
    source: [FR2.2]
  - id: BR4.2
    statement: "U1 là bên duy nhất ghi trạng thái pipeline của CallRecord."
    category: authorization
    applies_to: [CallRecord]
    trigger: "Bất kỳ cập nhật pipelineStatus/pipelineStep nào"
    logic: >
      THEN chỉ CallService.updatePipeline (U1) được ghi pipelineStatus/pipelineStep/errorDetail.
      Các unit khác (U4) chỉ cắm bước qua CallPipelineStep và yêu cầu U1 cập nhật.
    violation: "Unit khác ghi trực tiếp trạng thái pipeline là vi phạm ranh giới sở hữu"
    source: [Contract I2, Units Generation R-01]
  - id: BR4.3
    statement: "Trạng thái pipeline chỉ chuyển theo máy trạng thái hợp lệ."
    category: constraint
    applies_to: [CallRecord]
    trigger: "updatePipeline"
    logic: >
      Chuyển hợp lệ: IN_CALL -> PROCESSING -> DONE;
      PROCESSING -> {NO_RECORDING | STT_FAILED | AI_FAILED} khi bước tương ứng lỗi;
      AI_FAILED -> PROCESSING khi chạy lại AI (do U4 kích hoạt).
      pipelineStep nhận FETCH_RECORDING/TRANSCRIBE/ANALYZE khi đang PROCESSING, null khi ở trạng thái cuối.
    violation: "Chuyển trạng thái ngoài tập cho phép bị từ chối"
    source: [FR4.1, FR4.8, contract-summary C1]

  # ── BR5 — Chọn lead kế tiếp ──
  - id: BR5.1
    statement: "Lead kế tiếp là lead ưu tiên cao nhất ở trạng thái CHUA_TIEP_CAN hoặc DANG_TIEP_CAN, loại lead vừa gọi."
    category: calculation
    applies_to: [Lead]
    trigger: "GET /api/leads/next?excludeLeadId"
    logic: >
      Tập ứng viên = lead của currentRm có stage ∈ {CHUA_TIEP_CAN, DANG_TIEP_CAN} AND leadId != excludeLeadId.
      Sắp theo priority (KHAI_THAC_NGAY > HOT_LEAD > THUONG) rồi rankScore giảm dần rồi slaDueAt tăng dần.
      IF tập rỗng THEN 204 No Content (giao diện báo 'Bạn đã liên hệ hết tiềm năng bán cần xử lý').
      ELSE trả lead đầu tiên.
    violation: "Trả lead sai trạng thái, hoặc trả lead vừa gọi, là vi phạm"
    source: [FR4.9]

  # ── BR6 — Bảo mật & quy ước dữ liệu ──
  - id: BR6.1
    statement: "passwordHash không bao giờ xuất hiện trong payload trả về hay log; mật khẩu thô không được lưu/log."
    category: policy
    applies_to: [RmUser]
    trigger: "Mọi phản hồi API hoặc dòng log liên quan RmUser"
    logic: "THEN loại trừ passwordHash khỏi mọi payload và log; chỉ lưu dạng băm BCrypt."
    violation: "Lộ passwordHash/mật khẩu thô là vi phạm bảo mật"
    source: [NFR2]
  - id: BR6.2
    statement: "Giá trị tiền là số nguyên VND; thời gian theo ISO-8601 có offset +07:00, ranh giới ngày theo Asia/Ho_Chi_Minh."
    category: validation
    applies_to: [Customer, Lead, CallRecord, Activity]
    trigger: "Đọc/ghi các trường tiền và thời gian"
    logic: >
      THEN estimatedIncome và các giá trị tiền dùng integer VND (không số thực);
      slaDueAt/occurredAt/startedAt/endedAt là datetime ISO-8601 có offset +07:00.
    violation: "Dùng số thực cho tiền hoặc thời gian không có offset là vi phạm quy ước"
    source: [contract-summary - Quy ước chung REST]
  - id: BR6.3
    statement: "Mọi dữ liệu nhập ở biên hệ thống được kiểm tra hợp lệ trước khi xử lý."
    category: validation
    applies_to: [Lead, CallRecord]
    trigger: "Nhận request tạo/sửa"
    logic: "THEN kiểm tra kiểu, bắt buộc, tập enum, định dạng; sai thì 400 VALIDATION_FAILED kèm details.field."
    violation: "400 VALIDATION_FAILED"
    source: [NFR2]
```

## Bảng tóm tắt quy tắc

| ID | Loại | Áp dụng cho | Tóm tắt | Nguồn |
|---|---|---|---|---|
| BR1.1 | authorization | Lead, Call, Activity | Chỉ truy cập tài nguyên của RM hiện tại; khác RM → 403 | FR1.4 |
| BR1.2 | authorization | Lead, Call | 404 cho tài nguyên của chính RM; 403 cho RM khác | FR1.4 |
| BR1.3 | policy | RmUser | RM hiện tại: demo = RM mẫu; login = theo token | FR1.1, ADR-007 |
| BR2.1 | constraint | Lead, Customer, Activity | Chi tiết lead đủ 5 khối | FR1.3 |
| BR2.2 | constraint | Activity | Lịch sử giảm dần theo thời gian; rỗng → trạng thái trống | FR1.3, FR5.1 |
| BR2.3 | constraint | Activity | Thiếu tóm tắt AI → "Không có tóm tắt AI" | FR5.1 |
| BR3.1 | validation | Call, Lead, Customer | Tạo cuộc gọi: lead của RM + số hợp lệ | FR1.5 |
| BR3.2 | constraint | CallRecord | Một cuộc gọi chưa xử lý xong tại một thời điểm → 409 | Domain R-01 |
| BR4.1 | constraint | CallRecord | Kết thúc idempotent, pipeline chạy đúng một lần | FR2.2 |
| BR4.2 | authorization | CallRecord | Chỉ U1 ghi trạng thái pipeline | Contract I2 |
| BR4.3 | constraint | CallRecord | Chuyển trạng thái pipeline theo máy trạng thái | FR4.1, FR4.8 |
| BR5.1 | calculation | Lead | Lead kế tiếp: ưu tiên cao nhất ở CHUA/DANG_TIEP_CAN, loại lead vừa gọi | FR4.9 |
| BR6.1 | policy | RmUser | Không lộ/log passwordHash, không lưu mật khẩu thô | NFR2 |
| BR6.2 | validation | nhiều | Tiền = integer VND; thời gian ISO-8601 +07:00 | contract-summary |
| BR6.3 | validation | Lead, Call | Kiểm tra hợp lệ đầu vào ở biên | NFR2 |

## Sources

- `requirements.md` — FR1.1–FR1.5, FR2.2, FR4.9, NFR2
- `user-stories/stories.md` — AC1.1.1–3, AC4.3.1–2, AC4.4.1–2
- `contract-summary.md` — quy ước REST, I1/I2, mã lỗi
- `components.md` — hành vi CallComponent/LeadComponent/AuthComponent

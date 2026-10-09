# Quy tắc nghiệp vụ — U5 `u5-task-calendar`

Quy tắc nghiệp vụ của unit Công việc & Lịch làm việc. Khối YAML là **nguồn chân
lý**. Mỗi quy tắc có `id` theo định dạng `BR{nhóm}.{thứ tự}`. Các nhóm:
- **BR1** — quyền sở hữu và truy cập (FR1.4, FR5.3)
- **BR2** — tạo công việc từ cuộc gọi, idempotent (FR4.7, I4)
- **BR3** — tạo công việc thủ công (FR5.4)
- **BR4** — truy vấn & hiển thị lịch (FR5.2, FR5.3, FR5.5)
- **BR5** — thao tác công việc: hoàn thành & đổi giờ (FR5.4)
- **BR6** — quy ước dữ liệu & chép tóm tắt (contract-summary, FR5.2)

Quy tắc dựa lại trên U1: `BR1.3` dưới đây gọi `LeadService.assertOwned` (I1) và
`CurrentRmProvider.currentRm` (U1 `BR1.3`). U5 không tự quyết định RM hiện tại,
chỉ nhận từ U1.

```yaml
rules:
  # ── BR1 — Quyền sở hữu và truy cập ──
  - id: BR1.1
    statement: "Mọi truy vấn và thao tác công việc chỉ áp dụng cho công việc của RM hiện tại."
    category: authorization
    applies_to: [Task]
    trigger: "Mọi endpoint C4 (GET/POST /api/tasks, PATCH /api/tasks/{taskId}) và createFromCall (I4)"
    logic: >
      THEN lọc theo Task.ownerRmId = currentRm.rmId.
      IF thao tác trỏ tới một Task có ownerRmId != currentRm.rmId
      THEN trả 403 FORBIDDEN, không tiết lộ Task có tồn tại hay không, dữ liệu không đổi.
    violation: "403 FORBIDDEN; dữ liệu không đổi"
    source: [FR1.4, NFR2]
  - id: BR1.2
    statement: "Truy cập một taskId của chính RM nhưng không tồn tại trả 404; chỉ phân biệt 404 với 403 trong phạm vi công việc của chính RM."
    category: authorization
    applies_to: [Task]
    trigger: "PATCH /api/tasks/{taskId} với taskId không khớp"
    logic: >
      IF không có Task nào của currentRm mang taskId THEN 404 NOT_FOUND.
      IF Task tồn tại nhưng thuộc RM khác THEN 403 (BR1.1), không trả 404.
    violation: "404 NOT_FOUND cho công việc của chính RM; 403 cho RM khác"
    source: [FR1.4]
  - id: BR1.3
    statement: "Công việc gắn lead chỉ hợp lệ khi lead thuộc RM hiện tại."
    category: authorization
    applies_to: [Task]
    trigger: "Tạo/sửa Task có leadId khác null (POST /api/tasks, createFromCall)"
    logic: >
      IF leadId khác null THEN gọi LeadService.assertOwned(currentRm.rmId, leadId) (I1).
      IF lead không thuộc currentRm THEN 403 FORBIDDEN, không tạo/sửa Task.
    violation: "403 FORBIDDEN; không tạo/sửa công việc"
    source: [FR1.4, contract-summary I1]

  # ── BR2 — Tạo từ cuộc gọi (idempotent) ──
  - id: BR2.1
    statement: "createFromCall là idempotent theo (sourceCallId, clientKey); cùng cặp khoá chỉ sinh đúng một Task."
    category: constraint
    applies_to: [Task, TaskDraft]
    trigger: "TaskService.createFromCall(rmId, leadId, callId, TaskDraft[]) (I4) — có thể gọi lặp khi lưu keyin hai lần"
    logic: >
      Với mỗi draft: khoá chống trùng = (callId, draft.clientKey).
      IF đã tồn tại Task với cùng (sourceCallId=callId, clientKey) THEN trả lại taskId đã có, không tạo mới.
      ELSE tạo Task mới với origin = AI_FROM_CALL, sourceCallId = callId.
    violation: "Tạo công việc trùng cho cùng (callId, clientKey) là vi phạm (AC2.3.6)"
    source: [FR4.7, contract-summary I4]
  - id: BR2.2
    statement: "clientKey là bắt buộc với createFromCall; thiếu clientKey thì từ chối toàn bộ lời gọi."
    category: validation
    applies_to: [TaskDraft]
    trigger: "createFromCall nhận TaskDraft[]"
    logic: >
      IF bất kỳ draft nào thiếu clientKey (null/rỗng) THEN từ chối lời gọi với lỗi VALIDATION_FAILED
      (details.field = clientKey), không tạo Task nào.
    violation: "Lời gọi bị từ chối; không tạo công việc nào"
    source: [contract-summary I4 - ghi chú mở đã chốt ở stage này]
  - id: BR2.3
    statement: "Task tạo từ cuộc gọi mang origin AI_FROM_CALL và một bản sao tóm tắt AI của cuộc gọi."
    category: policy
    applies_to: [Task]
    trigger: "createFromCall tạo Task mới"
    logic: >
      THEN đặt origin = AI_FROM_CALL, sourceCallId = callId, aiSummary = bản sao tóm tắt AI của cuộc gọi
      (do bên gọi cung cấp hoặc đọc kèm). Giá trị này độc lập, không đọc trực tiếp từ CallAnalysis sau đó.
    violation: "Thiếu origin/sourceCallId, hoặc đọc chéo CallAnalysis khi hiển thị, là vi phạm ranh giới"
    source: [FR5.2, domain-design TaskComponent]

  # ── BR3 — Tạo thủ công ──
  - id: BR3.1
    statement: "Tạo công việc (thủ công hoặc từ cuộc gọi) yêu cầu tiêu đề không rỗng và dueAt hợp lệ."
    category: validation
    applies_to: [Task, TaskDraft]
    trigger: "POST /api/tasks và mỗi TaskDraft trong createFromCall"
    logic: >
      IF title rỗng/thiếu THEN 400 VALIDATION_FAILED (details.field = title).
      IF dueAt thiếu/sai định dạng ISO-8601 +07:00 THEN 400 VALIDATION_FAILED (details.field = dueAt).
      ELSE tạo Task với status = TODO.
    violation: "400 VALIDATION_FAILED; không tạo công việc"
    source: [FR5.4, contract-summary C4]
  - id: BR3.2
    statement: "Loại KHAC chỉ dùng cho công việc không gắn lead; tạo thủ công/từ cuộc gọi dùng LICH_HEN | NHAC_GOI | TASK_ZALO."
    category: validation
    applies_to: [Task]
    trigger: "Tạo/sửa Task"
    logic: >
      IF type = KHAC AND leadId khác null THEN 400 VALIDATION_FAILED.
      createFromCall chỉ nhận type ∈ {LICH_HEN, NHAC_GOI, TASK_ZALO}.
    violation: "400 VALIDATION_FAILED"
    source: [FR5.3, A4, refined-mockups S5]

  # ── BR4 — Truy vấn & hiển thị lịch ──
  - id: BR4.1
    statement: "Truy vấn lịch giới hạn khoảng thời gian tối đa 42 ngày."
    category: validation
    applies_to: [Task]
    trigger: "GET /api/tasks?from&to"
    logic: >
      IF from/to thiếu hoặc from > to hoặc (to - from) > 42 ngày
      THEN 400 VALIDATION_FAILED.
      ELSE trả các Task của currentRm có dueAt trong [from, to].
    violation: "400 VALIDATION_FAILED"
    source: [FR5.5, contract-summary C4]
  - id: BR4.2
    statement: "Lịch tuần mặc định Thứ 2–Thứ 7, 08:00–17:00; công việc ngoài khung giờ gom vào dòng 'Ngoài giờ'."
    category: calculation
    applies_to: [Task]
    trigger: "Hiển thị chế độ Tuần (mặc định)"
    logic: >
      Khung = Thứ 2..Thứ 7, 08:00–17:00 theo Asia/Ho_Chi_Minh.
      IF dueAt rơi vào khung THEN đặt vào ô ngày/giờ tương ứng.
      ELSE (ngoài giờ hoặc Chủ nhật) gom vào dòng 'Ngoài giờ' của cột ngày đó.
    violation: "Đặt sai ô hoặc bỏ sót việc ngoài giờ là vi phạm hiển thị"
    source: [FR5.2, refined-mockups S5]
  - id: BR4.3
    statement: "Bộ lọc theo loại: Tất cả / LICH_HEN / NHAC_GOI / TASK_ZALO; công việc KHAC chỉ hiện ở 'Tất cả'."
    category: policy
    applies_to: [Task]
    trigger: "GET /api/tasks?type=... hoặc bộ lọc giao diện"
    logic: >
      IF type được chỉ định THEN trả Task có đúng type đó.
      Công việc type = KHAC chỉ nằm trong kết quả khi không lọc (hiển thị 'Tất cả').
    violation: "Hiện KHAC dưới một bộ lọc loại cụ thể là vi phạm"
    source: [FR5.3, refined-mockups S5]
  - id: BR4.4
    statement: "Lịch hiển thị tất cả công việc của RM — cả đã làm (DONE) lẫn sắp tới (TODO), gồm cả công việc không gắn lead."
    category: constraint
    applies_to: [Task]
    trigger: "GET /api/tasks trong khoảng thời gian"
    logic: >
      THEN kết quả gồm mọi Task của currentRm trong khoảng, không loại theo status.
      IF khoảng không có Task nào THEN giao diện báo 'Không có công việc trong tuần này'.
    violation: "Lọc mất việc DONE hoặc việc không gắn lead là vi phạm"
    source: [FR5.3]
  - id: BR4.5
    statement: "Chế độ xem Ngày / Tháng / Danh sách dùng chung truy vấn khoảng thời gian; Danh sách tách 'đã làm' và 'sắp tới'."
    category: policy
    applies_to: [Task]
    trigger: "Chuyển chế độ xem (US3.4)"
    logic: >
      Ngày/Tháng chỉ đổi khoảng [from, to] gửi lên (vẫn ≤ 42 ngày).
      Danh sách: nhóm theo status (DONE / TODO) rồi sắp theo dueAt tăng dần.
    violation: "Danh sách không tách done/upcoming là vi phạm hiển thị"
    source: [FR5.5, refined-mockups S5]
  - id: BR4.6
    statement: "Khối 'AI Key-in từ cuộc gọi' chỉ gồm công việc có sourceCallId trùng cuộc gọi vừa lưu; đánh dấu 'Vừa lưu từ cuộc gọi'."
    category: calculation
    applies_to: [Task]
    trigger: "GET /api/tasks?sourceCallId=... (panel trái S5)"
    logic: >
      THEN trả Task của currentRm có sourceCallId = tham số, kèm aiSummary để dựng thẻ.
      Giao diện tô 'just-created' cho các việc này (nguồn: tham số sourceCallId).
    violation: "Hiện công việc không thuộc cuộc gọi đó trong khối này là vi phạm"
    source: [FR5.2, FR5.3]

  # ── BR5 — Hoàn thành & đổi giờ ──
  - id: BR5.1
    statement: "Đánh dấu hoàn thành chuyển TODO -> DONE; là một chiều cho demo."
    category: constraint
    applies_to: [Task]
    trigger: "PATCH /api/tasks/{taskId} với status = DONE"
    logic: >
      IF status hiện tại = TODO THEN đặt status = DONE, completedAt = thời điểm hiện tại.
      IF đã DONE THEN no-op idempotent, trả trạng thái hiện tại.
    violation: "Chuyển DONE -> TODO không được hỗ trợ trong demo"
    source: [FR5.4]
  - id: BR5.2
    statement: "Đổi giờ sang thời điểm đã qua phải có cờ xác nhận confirmPast = true, nếu không thì từ chối."
    category: validation
    applies_to: [Task]
    trigger: "PATCH /api/tasks/{taskId} với dueAt mới"
    logic: >
      IF dueAt mới < thời điểm hiện tại AND confirmPast != true
      THEN 400 VALIDATION_FAILED (details.field = dueAt, reason = PAST_NOT_CONFIRMED).
      ELSE cập nhật dueAt.
    violation: "400 PAST_NOT_CONFIRMED khi đổi sang quá khứ mà chưa xác nhận (AC3.3.3)"
    source: [FR5.4, contract-summary C4]
  - id: BR5.3
    statement: "'Gọi ngay' và 'Chi tiết TNB' là điều hướng sang màn của U1, không phải thao tác dữ liệu của U5."
    category: policy
    applies_to: [Task]
    trigger: "RM bấm 'Gọi ngay' / 'Chi tiết TNB' trên thẻ công việc có leadId"
    logic: >
      THEN giao diện mở màn Thực hiện cuộc gọi (luồng FR1.5, U1) hoặc Chi tiết lead (U1) cho leadId.
      U5 không tạo cuộc gọi và không sửa lead; chỉ cung cấp leadId cho điều hướng.
    violation: "U5 tự tạo cuộc gọi hoặc ghi lead là vi phạm ranh giới unit"
    source: [FR5.4, contract-summary I2]

  # ── BR6 — Quy ước dữ liệu ──
  - id: BR6.1
    statement: "Mọi thời gian theo ISO-8601 offset +07:00; ranh giới ngày/khung giờ theo Asia/Ho_Chi_Minh."
    category: validation
    applies_to: [Task]
    trigger: "Đọc/ghi dueAt, createdAt, completedAt và tính khoảng/khung giờ"
    logic: >
      THEN mọi datetime mang offset +07:00; phép tính khoảng 42 ngày và khung 08:00–17:00
      dùng múi giờ Asia/Ho_Chi_Minh.
    violation: "Thời gian không có offset hoặc tính ngày sai múi giờ là vi phạm quy ước"
    source: [contract-summary - Quy ước chung REST, U1 BR6.2]
  - id: BR6.2
    statement: "Mọi dữ liệu nhập ở biên được kiểm tra kiểu, bắt buộc, tập enum và định dạng trước khi xử lý."
    category: validation
    applies_to: [Task, TaskDraft]
    trigger: "Nhận request tạo/sửa hoặc TaskDraft[]"
    logic: "THEN kiểm tra kiểu/bắt buộc/enum/định dạng; sai thì 400 VALIDATION_FAILED kèm details.field."
    violation: "400 VALIDATION_FAILED"
    source: [NFR2]
```

## Bảng tóm tắt quy tắc

| ID | Loại | Áp dụng cho | Tóm tắt | Nguồn |
|---|---|---|---|---|
| BR1.1 | authorization | Task | Chỉ công việc của RM hiện tại; khác RM → 403 | FR1.4 |
| BR1.2 | authorization | Task | 404 cho công việc của chính RM; 403 cho RM khác | FR1.4 |
| BR1.3 | authorization | Task | Công việc gắn lead: lead phải thuộc RM (assertOwned) | FR1.4, I1 |
| BR2.1 | constraint | Task | createFromCall idempotent theo (callId, clientKey) | FR4.7, I4 |
| BR2.2 | validation | TaskDraft | clientKey bắt buộc với createFromCall; thiếu → từ chối | I4 (chốt ở stage) |
| BR2.3 | policy | Task | Task từ cuộc gọi: origin AI_FROM_CALL + bản sao tóm tắt AI | FR5.2 |
| BR3.1 | validation | Task | Tạo cần title không rỗng + dueAt hợp lệ | FR5.4, C4 |
| BR3.2 | validation | Task | KHAC chỉ cho việc không gắn lead | FR5.3, A4 |
| BR4.1 | validation | Task | Khoảng truy vấn tối đa 42 ngày | FR5.5, C4 |
| BR4.2 | calculation | Task | Tuần T2–T7 08:00–17:00; ngoài giờ → dòng 'Ngoài giờ' | FR5.2 |
| BR4.3 | policy | Task | Lọc loại; KHAC chỉ hiện ở 'Tất cả' | FR5.3 |
| BR4.4 | constraint | Task | Hiện mọi việc của RM: done + upcoming, cả việc không lead | FR5.3 |
| BR4.5 | policy | Task | Ngày/Tháng/Danh sách; Danh sách tách done/upcoming | FR5.5 |
| BR4.6 | calculation | Task | Khối 'AI Key-in từ cuộc gọi' lọc theo sourceCallId | FR5.2 |
| BR5.1 | constraint | Task | Đánh dấu xong TODO → DONE, idempotent | FR5.4 |
| BR5.2 | validation | Task | Đổi giờ quá khứ cần confirmPast=true, else PAST_NOT_CONFIRMED | FR5.4, C4 |
| BR5.3 | policy | Task | 'Gọi ngay'/'Chi tiết TNB' là điều hướng sang U1 | FR5.4 |
| BR6.1 | validation | Task | Thời gian ISO-8601 +07:00, ngày theo Asia/Ho_Chi_Minh | contract-summary |
| BR6.2 | validation | Task | Kiểm tra hợp lệ đầu vào ở biên | NFR2 |

## Sources

- `inception/requirements-analysis/requirements.md` — FR1.4, FR4.7, FR5.2–FR5.5, NFR2, A4
- `inception/user-stories/stories.md` — AC3.2.*, AC3.3.*, AC3.4.*, AC2.3.6 (idempotent)
- `inception/contract-design/contract-summary.md` — C4 (tham số from/to, confirmPast, mã lỗi), I1 (assertOwned), I4 (createFromCall + ghi chú mở clientKey), quy ước thời gian/tiền
- `inception/domain-design/components.md` — TaskComponent (hành vi, chép tóm tắt AI, ranh giới)
- `inception/refined-mockups/mockups.md` — S5 (khung giờ, bộ lọc, 'Ngoài giờ', Danh sách), S6 (đổi giờ)
- `construction/u1-lead-call-foundation/functional-design/rules.md` — BR1.1/BR1.3 (ownership, CurrentRmProvider), BR6.2 (quy ước dữ liệu)

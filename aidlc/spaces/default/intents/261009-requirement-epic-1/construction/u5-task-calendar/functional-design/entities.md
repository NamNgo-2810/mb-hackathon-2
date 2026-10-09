# Mô hình thực thể — U5 `u5-task-calendar`

Unit `u5-task-calendar` (kind: `service`) sở hữu đúng **một** thực thể nghiệp vụ:
`Task` (công việc / tác vụ CRM của RM). Khối YAML dưới đây là **nguồn chân lý**
về hình dạng dữ liệu; sơ đồ ER trong `functional-spec.md` được dẫn xuất từ khối
này. Các thực thể `RmUser`, `Lead`, `CallRecord` thuộc unit khác (U1/U6) và chỉ
được tham chiếu, không sở hữu — U5 chỉ giữ khoá tham chiếu và một ít trường
hiển-thị-đọc (denormalized) do U1 cung cấp.

Quy ước chung (kế thừa `contract-summary.md` và U1 `rules.md` BR6.2): thời gian
dùng ISO-8601 có offset `+07:00`, ranh giới ngày theo múi giờ
`Asia/Ho_Chi_Minh`; tiền (nếu có) là số nguyên VND. Mọi thuộc tính thời gian
của `Task` đều lưu kèm offset.

```yaml
entities:
  - name: Task
    owner: u5-task-calendar
    identifier: taskId
    description: >
      Một công việc / tác vụ của RM hiển thị trên lịch làm việc (S5) và thao tác
      qua hộp thoại tạo/đổi giờ (S6). Có thể gắn với một lead (không bắt buộc),
      có thể sinh tự động từ một cuộc gọi (origin AI_FROM_CALL), tạo thủ công
      (MANUAL), hoặc nạp sẵn từ dữ liệu seed không gắn lead (SEED).
    attributes:
      - name: taskId
        type: string
        required: true
        unique: true
        description: Định danh công việc do server sinh.
      - name: ownerRmId
        type: string
        required: true
        references: RmUser.rmId (AuthComponent, U1)
        description: >
          RM sở hữu công việc. Mọi truy vấn/thao tác chỉ áp dụng cho công việc
          của RM hiện tại (BR1.1).
      - name: leadId
        type: string
        required: false
        nullable: true
        references: Lead.leadId (LeadComponent, U1)
        description: >
          Lead liên quan (nếu có). null với công việc không gắn lead (ví dụ họp
          giao ban, seed). Khi khác null, lead PHẢI thuộc ownerRmId (BR1.3).
      - name: sourceCallId
        type: string
        required: false
        nullable: true
        references: CallRecord.callId (CallComponent, U1)
        description: >
          Cuộc gọi gốc khi origin = AI_FROM_CALL. Dùng để lọc khối "AI Key-in từ
          cuộc gọi" (S5) và cho highlight 'just-created'. null với MANUAL/SEED.
      - name: clientKey
        type: string
        required: false
        nullable: true
        description: >
          Khoá chống trùng do bên tạo cung cấp. BẮT BUỘC khi tạo qua
          createFromCall (I4); cùng (sourceCallId, clientKey) chỉ sinh một Task
          (BR2.1). Không dùng cho tạo thủ công.
      - name: title
        type: string
        required: true
        min: 1
        max: 200
        description: Tiêu đề công việc. Bắt buộc với mọi đường tạo (BR3.1).
      - name: type
        type: enum
        required: true
        allowed_values: [LICH_HEN, NHAC_GOI, TASK_ZALO, KHAC]
        description: >
          Loại công việc. KHAC chỉ dùng cho công việc seed không gắn lead và chỉ
          hiện ở bộ lọc 'Tất cả' (BR4.3). Tạo thủ công/ từ cuộc gọi dùng
          LICH_HEN | NHAC_GOI | TASK_ZALO.
      - name: priority
        type: enum
        required: true
        default: THUONG
        allowed_values: [CAO, THUONG]
        description: Mức ưu tiên hiển thị trên thẻ công việc.
      - name: dueAt
        type: datetime
        required: true
        description: >
          Ngày giờ đến hạn, ISO-8601 +07:00. Quyết định ô ngày/giờ trên lịch và
          việc gom vào dòng 'Ngoài giờ' khi ở ngoài 08:00–17:00 (BR4.2).
      - name: durationMinutes
        type: integer
        required: false
        default: 30
        allowed_values: [15, 30, 60]
        description: Thời lượng dự kiến; mặc định 30 phút.
      - name: status
        type: enum
        required: true
        default: TODO
        allowed_values: [TODO, DONE]
        description: >
          Trạng thái vòng đời. Máy trạng thái ở functional-spec.md: TODO -> DONE
          (một chiều cho demo, BR5.1).
      - name: origin
        type: enum
        required: true
        allowed_values: [AI_FROM_CALL, MANUAL, SEED]
        description: >
          Nguồn gốc công việc. AI_FROM_CALL = sinh từ keyin cuộc gọi (I4);
          MANUAL = RM tạo tay (S6); SEED = dữ liệu mẫu (thường không gắn lead).
      - name: aiSummary
        type: string
        required: false
        nullable: true
        description: >
          Bản sao tóm tắt AI của cuộc gọi gốc, do createFromCall chép sang để
          thẻ panel trái S5 hiển thị độc lập (BR6.1). null khi origin != AI_FROM_CALL
          hoặc cuộc gọi không có tóm tắt.
      - name: note
        type: string
        required: false
        nullable: true
        max: 1000
        description: Ghi chú tự do của RM.
      - name: createdAt
        type: datetime
        required: true
        description: Thời điểm tạo, ISO-8601 +07:00.
      - name: completedAt
        type: datetime
        required: false
        nullable: true
        description: Thời điểm chuyển sang DONE; null khi còn TODO.
    entity_constraints:
      - "taskId là duy nhất."
      - >
        Khi origin = AI_FROM_CALL thì sourceCallId và clientKey đều phải có; cặp
        (sourceCallId, clientKey) là duy nhất để chống tạo trùng (BR2.1).
      - "Khi leadId khác null, lead phải thuộc ownerRmId (BR1.3)."
      - "type = KHAC chỉ hợp lệ khi leadId = null (công việc seed không gắn lead)."
      - "completedAt khác null khi và chỉ khi status = DONE."
    relationships:
      - with: RmUser (U1/U6)
        cardinality: "nhiều Task — một RmUser"
        direction: "Task.ownerRmId -> RmUser.rmId"
        note: "Bắt buộc; tham chiếu, không sở hữu."
      - with: Lead (U1)
        cardinality: "nhiều Task — không hoặc một Lead"
        direction: "Task.leadId -> Lead.leadId"
        note: "Không bắt buộc; tham chiếu, không sở hữu."
      - with: CallRecord (U1)
        cardinality: "nhiều Task — không hoặc một CallRecord"
        direction: "Task.sourceCallId -> CallRecord.callId"
        note: "Chỉ có khi origin = AI_FROM_CALL; tham chiếu, không sở hữu."

  # Giá trị truyền vào createFromCall / tạo thủ công — không phải thực thể lưu
  # trữ riêng, mà là hình dạng dữ liệu đầu vào để sinh Task. Giữ ở đây để làm rõ
  # ranh giới I4 với U4.
  - name: TaskDraft
    owner: u5-task-calendar
    description: >
      Bản nháp một công việc tiếp theo do U4 gửi qua TaskService.createFromCall
      (I4) hoặc do giao diện gửi khi tạo thủ công (C4 POST /api/tasks). Mỗi draft
      sinh ra tối đa một Task.
    kind: value-object
    attributes:
      - name: clientKey
        type: string
        required: true
        description: >
          Bắt buộc với createFromCall (quyết định của stage này); thiếu clientKey
          -> từ chối. Chống tạo trùng khi lưu keyin lặp (AC2.3.6 / BR2.1).
      - name: title
        type: string
        required: true
        min: 1
        max: 200
      - name: dueAt
        type: datetime
        required: true
      - name: type
        type: enum
        required: true
        allowed_values: [LICH_HEN, NHAC_GOI, TASK_ZALO]
      - name: priority
        type: enum
        required: true
        allowed_values: [CAO, THUONG]
      - name: durationMinutes
        type: integer
        required: false
        default: 30
      - name: aiSuggested
        type: boolean
        required: false
        description: Draft này do AI đề xuất hay RM thêm tay trong form keyin.
```

## Tóm tắt mô hình thực thể

U5 sở hữu duy nhất `Task`. Mọi dữ liệu lead/cuộc gọi/RM đều do U1 sở hữu, U5 chỉ
giữ khoá tham chiếu (`ownerRmId`, `leadId`, `sourceCallId`) và một trường tóm
tắt được chép sang (`aiSummary`) để lịch hiển thị độc lập ngay cả khi không gọi
chéo U1 — khớp quyết định domain-design là không để Lead/Task phụ thuộc đọc trực
tiếp vào CallAnalysis.

`TaskDraft` là value-object đầu vào (không lưu trữ riêng): nó mô tả một "công
việc tiếp theo" trước khi được vật chất hoá thành `Task`. Điểm quyết định của
stage này là `clientKey` **bắt buộc** cho `createFromCall` (I4) — đây là cách
chốt lại ghi chú mở của Contract Design: thiếu `clientKey` thì từ chối, nhờ đó
tính idempotent theo `(sourceCallId, clientKey)` luôn xác định.

| Thực thể | Loại | Định danh | Tham chiếu | Ghi chú |
|---|---|---|---|---|
| Task | entity (sở hữu) | taskId | RmUser, Lead?, CallRecord? | Thực thể lịch; origin AI_FROM_CALL/MANUAL/SEED |
| TaskDraft | value-object (đầu vào) | — (clientKey chống trùng) | — | Vật chất hoá thành Task; clientKey bắt buộc với createFromCall |

## Sources

- `inception/units-generation/unit-of-work.md` — mục U5 (TaskComponent, trách nhiệm, ghi chú A4)
- `inception/domain-design/components.md` — TaskComponent và thực thể `Task` (chủ sở hữu, thuộc tính)
- `inception/contract-design/contract-summary.md` — C4 (schema Task/TaskCreate), I4 (TaskService.createFromCall), quy ước thời gian/tiền, ghi chú mở về clientKey
- `inception/requirements-analysis/requirements.md` — FR5.2–FR5.5, A2 (dữ liệu seed), A4 (ẩn Outlook/Google, Zalo ZNS)
- `inception/user-stories/stories.md` — US3.2, US3.3, US3.4 và AC tương ứng
- `inception/refined-mockups/mockups.md` — S5, S6 (loại công việc, bộ lọc, trạng thái)
- `construction/u1-lead-call-foundation/functional-design/rules.md` — CurrentRmProvider (BR1.3), lead-ownership (BR1.1), quy ước BR6.2

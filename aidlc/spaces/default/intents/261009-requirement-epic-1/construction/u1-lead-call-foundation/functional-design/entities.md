# Entities — U1 `u1-lead-call-foundation`

Mô hình thực thể cho unit nền tảng Lead & cuộc gọi. Nguồn: `components.md`
(sở hữu thực thể), `requirements.md` (FR1.x), `contract-summary.md` (C1, I1, I2).
Unit này **sở hữu** `RmUser` (chế độ demo), `Customer`, `Lead`, `Activity` và
`CallRecord`. Các thực thể do unit khác sở hữu (`TranscriptTurn`, `CallAnalysis`,
`CallKeyin`, `Task`…) chỉ được tham chiếu, không định nghĩa ở đây.

Khối YAML dưới đây là **nguồn chân lý** của mô hình dữ liệu. Mô tả kỹ thuật-agnostic:
không gắn kiểu SQL, không framework. `logical type` là kiểu logic (string, integer,
decimal, datetime, date, boolean, enum, text, list).

```yaml
entities:
  - name: RmUser
    description: >
      Nhân viên quan hệ khách hàng (RM). Ở unit này chỉ cần hình dạng tối thiểu để
      chế độ demo trả về một RM mẫu cố định (ADR-007). Bản hiện thực đăng nhập thật
      do U6 bổ sung, không đổi hình dạng này.
    identifier: rmId
    attributes:
      - { name: rmId, type: string, required: true, unique: true, constraint: "định danh ổn định của RM" }
      - { name: username, type: string, required: true, unique: true, constraint: "duy nhất, dùng khi login (U6)" }
      - { name: passwordHash, type: string, required: false, constraint: "băm BCrypt; null/không dùng ở chế độ demo; không bao giờ trả ra API hay ghi log (NFR2)" }
      - { name: displayName, type: string, required: true, constraint: "tên hiển thị trên giao diện" }
    entity_constraints:
      - "username là duy nhất toàn hệ thống"
      - "passwordHash không bao giờ xuất hiện trong payload trả về hay log"
    relationships:
      - { to: Lead, cardinality: "1..*", direction: "RmUser phụ trách nhiều Lead", note: "mỗi Lead do đúng một RM phụ trách qua ownerRmId" }
      - { to: CallRecord, cardinality: "1..*", direction: "RmUser thực hiện nhiều CallRecord" }

  - name: Customer
    description: Khách hàng cá nhân (KHCN) gắn với lead. Dữ liệu demo (seed).
    identifier: customerId
    attributes:
      - { name: customerId, type: string, required: true, unique: true }
      - { name: fullName, type: string, required: true }
      - { name: segment, type: string, required: false, constraint: "phân khúc, ví dụ VIP/Thường" }
      - { name: phoneNumber, type: string, required: true, constraint: "số điện thoại; kiểm hợp lệ trước khi tạo cuộc gọi (FR1.5)" }
      - { name: age, type: integer, required: false, min: 0 }
      - { name: occupation, type: string, required: false }
      - { name: estimatedIncome, type: integer, required: false, constraint: "VND, số nguyên (quy ước tiền của contract-summary)" }
      - { name: address, type: string, required: false }
    entity_constraints:
      - "phoneNumber phải khác rỗng và theo định dạng số điện thoại VN hợp lệ để tạo được cuộc gọi"
    relationships:
      - { to: Lead, cardinality: "1..*", direction: "Customer có thể gắn với nhiều Lead", note: "mỗi Lead trỏ tới đúng một Customer qua customerId" }

  - name: Lead
    description: >
      Tiềm năng bán do một RM phụ trách. Mang trạng thái vòng đời (stage), mức ưu tiên,
      điểm xếp hạng, hạn SLA, nội dung tư vấn và kịch bản bán.
    identifier: leadId
    attributes:
      - { name: leadId, type: string, required: true, unique: true }
      - { name: leadCode, type: string, required: true, unique: true, constraint: "mã lead hiển thị cho người dùng" }
      - { name: customerId, type: string, required: true, references: "Customer.customerId" }
      - { name: ownerRmId, type: string, required: true, references: "RmUser.rmId", constraint: "RM phụ trách; cơ sở của kiểm tra quyền FR1.4" }
      - { name: productInterest, type: string, required: false, constraint: "sản phẩm KH quan tâm" }
      - { name: source, type: string, required: false, constraint: "nguồn lead" }
      - { name: priority, type: enum, required: true, allowed: [KHAI_THAC_NGAY, HOT_LEAD, THUONG], default: THUONG }
      - { name: rankScore, type: integer, required: false, min: 0, max: 10, constraint: "điểm xếp hạng /10" }
      - { name: slaDueAt, type: datetime, required: false, constraint: "ISO-8601 có offset +07:00" }
      - { name: stage, type: enum, required: true, allowed: [CHUA_TIEP_CAN, DANG_TIEP_CAN, CHUYEN_CO_HOI, TU_CHOI], default: CHUA_TIEP_CAN }
      - { name: content, type: text, required: false, constraint: "khối Nội dung: thông tin TNB, sản phẩm, dư nợ" }
      - { name: salesScript, type: text, required: false, constraint: "kịch bản bán" }
    entity_constraints:
      - "ownerRmId phải trỏ tới một RmUser tồn tại"
      - "stage chỉ nhận giá trị trong tập enum; chuyển trạng thái theo máy trạng thái ở functional-spec.md"
    relationships:
      - { to: Customer, cardinality: "*..1", direction: "nhiều Lead thuộc một Customer" }
      - { to: RmUser, cardinality: "*..1", direction: "nhiều Lead do một RM phụ trách" }
      - { to: Activity, cardinality: "1..*", direction: "Lead có nhiều Activity" }
      - { to: CallRecord, cardinality: "1..*", direction: "Lead phát sinh nhiều CallRecord" }

  - name: Activity
    description: >
      Hoạt động trong lịch sử của lead (cuộc gọi, hẹn gặp, SMS, khác). Hoạt động loại
      cuộc gọi mang bản sao tóm tắt/gợi ý AI (do U4 ghi khi lưu keyin) để lịch sử hiển
      thị được kể cả khi AI lỗi (US3.1). Unit U1 sở hữu thực thể và đọc/ghi nó; U4 ghi
      aiSummary/aiSuggestions qua giao diện LeadService.recordCallActivity (I1).
    identifier: activityId
    attributes:
      - { name: activityId, type: string, required: true, unique: true }
      - { name: leadId, type: string, required: true, references: "Lead.leadId" }
      - { name: type, type: enum, required: true, allowed: [CUOC_GOI, HEN_GAP, SMS, KHAC] }
      - { name: occurredAt, type: datetime, required: true, constraint: "ISO-8601 +07:00" }
      - { name: product, type: string, required: false }
      - { name: result, type: string, required: false }
      - { name: note, type: text, required: false }
      - { name: callId, type: string, required: false, references: "CallRecord.callId", constraint: "chỉ có với type=CUOC_GOI" }
      - { name: aiSummary, type: text, required: false, constraint: "null = 'Không có tóm tắt AI' (AC3.1.3)" }
      - { name: aiSuggestions, type: list, required: false, constraint: "danh sách chuỗi gợi ý AI" }
    entity_constraints:
      - "nếu type = CUOC_GOI thì callId phải trỏ tới một CallRecord; với type khác, callId để trống"
      - "lịch sử hoạt động trả về theo thứ tự occurredAt giảm dần (mới nhất trước)"
    relationships:
      - { to: Lead, cardinality: "*..1", direction: "nhiều Activity thuộc một Lead" }
      - { to: CallRecord, cardinality: "*..1", direction: "Activity loại cuộc gọi trỏ tới một CallRecord" }

  - name: CallRecord
    description: >
      Bản ghi một cuộc gọi, gắn với lead và RM. U1 **sở hữu** và là bên duy nhất ghi
      trạng thái pipeline (updatePipeline, I2). Mở rộng CallRecord có sẵn thêm leadId,
      rmId, pipelineStatus/Step và giữ nguyên các API/trường cũ (sttStatus) để không
      làm hỏng trang cũ.
    identifier: callId
    attributes:
      - { name: callId, type: string, required: true, unique: true }
      - { name: leadId, type: string, required: true, references: "Lead.leadId" }
      - { name: rmId, type: string, required: true, references: "RmUser.rmId" }
      - { name: phoneNumber, type: string, required: true, constraint: "số KH tại thời điểm gọi" }
      - { name: startedAt, type: datetime, required: true }
      - { name: endedAt, type: datetime, required: false, constraint: "null khi chưa kết thúc" }
      - { name: durationSeconds, type: integer, required: false, min: 0 }
      - { name: recordingName, type: string, required: false, constraint: "tên file ghi âm 'mới nhất' do pipeline gắn (U4 dùng)" }
      - { name: pipelineStatus, type: enum, required: true, allowed: [IN_CALL, PROCESSING, DONE, NO_RECORDING, STT_FAILED, AI_FAILED], default: IN_CALL }
      - { name: pipelineStep, type: enum, required: false, allowed: [FETCH_RECORDING, TRANSCRIBE, ANALYZE], constraint: "null khi không đang xử lý" }
      - { name: errorDetail, type: text, required: false, constraint: "mô tả lỗi khi ở trạng thái dự phòng" }
      - { name: sttStatus, type: string, required: false, constraint: "giữ để tương thích giao diện cũ" }
    entity_constraints:
      - "leadId phải thuộc rmId (bất biến quyền sở hữu, FR1.4)"
      - "endedAt chỉ được đặt một lần; đặt lại là no-op (kết thúc idempotent, AC1.1.3)"
      - "chỉ U1 ghi pipelineStatus/pipelineStep (qua CallService.updatePipeline)"
      - "tại một thời điểm chỉ có tối đa một CallRecord của hệ thống ở trạng thái chưa xử lý xong (giới hạn một cuộc gọi, Domain Design R-01)"
    relationships:
      - { to: Lead, cardinality: "*..1", direction: "nhiều CallRecord từ một Lead" }
      - { to: RmUser, cardinality: "*..1", direction: "nhiều CallRecord do một RM thực hiện" }
      - { to: Activity, cardinality: "1..1", direction: "mỗi CallRecord sinh một Activity loại cuộc gọi khi keyin được lưu", note: "Activity do U1 sở hữu, U4 kích hoạt ghi qua I1" }

referenced_external_entities:
  - { name: TranscriptTurn, owned_by: TranscriptionComponent (U4), note: "lượt transcript; U1 chỉ phục vụ qua C1 GET /transcript" }
  - { name: CallAnalysis, owned_by: CallAnalysisComponent (U4), note: "kết quả AI; không thuộc phạm vi U1" }
  - { name: CallKeyin, owned_by: KeyinComponent (U4), note: "hồ sơ keyin" }
  - { name: Task, owned_by: TaskComponent (U5), note: "công việc tạo từ keyin" }
```

## Tóm tắt tập thực thể

U1 là unit nền tảng, sở hữu năm thực thể cốt lõi của CRM:

- **RmUser** — RM đang thao tác. Chế độ demo trả về một RM mẫu cố định; U6 thêm đăng nhập thật mà không đổi hình dạng. `passwordHash` không bao giờ lộ ra API/log.
- **Customer** — khách hàng cá nhân, dữ liệu seed. `phoneNumber` phải hợp lệ để gọi được.
- **Lead** — tiềm năng bán do đúng một RM phụ trách (`ownerRmId`), là chỗ duy nhất đặt bất biến quyền sở hữu (FR1.4). Có máy trạng thái `stage` (Chưa tiếp cận → Đang tiếp cận → Chuyển cơ hội / Từ chối).
- **Activity** — lịch sử hoạt động của lead. Hoạt động cuộc gọi mang bản sao `aiSummary`/`aiSuggestions` để US3.1 hiển thị kể cả khi AI lỗi.
- **CallRecord** — vòng đời cuộc gọi và trạng thái pipeline. U1 là bên duy nhất ghi `pipelineStatus`/`pipelineStep`; kết thúc idempotent; mỗi lúc chỉ một cuộc gọi chưa xử lý xong.

Các thực thể của unit khác (`TranscriptTurn`, `CallAnalysis`, `CallKeyin`, `Task`) chỉ được tham chiếu qua khóa, không định nghĩa ở đây — giữ đúng ranh giới sở hữu ở `components.md` và `contract-summary.md`.

## Sources

- `components.md` — sở hữu thực thể và thuộc tính gốc
- `requirements.md` — FR1.1–FR1.5, FR4.9, NFR2
- `contract-summary.md` — C1 (schema REST), I1/I2 (giao diện Java), quy ước tiền/thời gian
- `unit-of-work.md` — mô tả và trách nhiệm U1

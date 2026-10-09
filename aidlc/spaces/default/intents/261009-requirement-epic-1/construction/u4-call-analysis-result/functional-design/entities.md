# Entities — U4 `u4-call-analysis-result`

Mô hình thực thể cho unit Phân tích & Kết quả cuộc gọi. Nguồn: `components.md`
(sở hữu thực thể), `requirements.md` (FR4.x, NFR1–NFR3), `contract-summary.md`
(C3, I2, I3, I4), `stories.md` (US2.x, US3.1). Unit này **sở hữu** `TranscriptTurn`,
`CallAnalysis`, `CallKeyin` và `AiFeedback`. Các thực thể do unit khác sở hữu
(`CallRecord`, `Lead`, `Activity`, `Task`) chỉ được tham chiếu, không định nghĩa ở đây.

Khối YAML dưới đây là **nguồn chân lý** của mô hình dữ liệu. Mô tả kỹ thuật-agnostic:
không gắn kiểu SQL, không framework. `logical type` là kiểu logic (string, integer,
decimal, datetime, date, boolean, enum, text, list, object).

```yaml
entities:
  - name: TranscriptTurn
    description: >
      Một lượt hội thoại đã chuyển thành văn bản, tách theo người nói RM/KH, kèm mốc
      thời gian. Mã STT có sẵn (TranscriptionComponent); U4 chỉ chỉnh để tách rõ lỗi
      "chưa có ghi âm" và "lỗi STT". Phục vụ cột 2 (hội thoại) và là đầu vào cho phân tích AI.
    identifier: turnId
    attributes:
      - { name: turnId, type: string, required: true, unique: true }
      - { name: callId, type: string, required: true, references: "CallRecord.callId" }
      - { name: speaker, type: enum, required: true, allowed: [RM, KH], constraint: "gán nhãn theo kênh stereo cấu hình" }
      - { name: startMs, type: integer, required: true, min: 0, constraint: "mốc bắt đầu lượt, mili-giây từ đầu ghi âm" }
      - { name: endMs, type: integer, required: false, min: 0, constraint: ">= startMs khi có" }
      - { name: text, type: text, required: true, constraint: "nội dung tiếng Việt của lượt" }
    entity_constraints:
      - "mỗi lượt thuộc đúng một CallRecord qua callId"
      - "transcript trả về theo thứ tự startMs tăng dần"
      - "lưu idempotent theo callId: chạy lại STT thay thế toàn bộ tập lượt của cuộc gọi, không nhân đôi"
    relationships:
      - { to: CallRecord, cardinality: "*..1", direction: "nhiều TranscriptTurn thuộc một CallRecord" }

  - name: CallAnalysis
    description: >
      Kết quả phân tích AI của một cuộc gọi: tóm tắt, gợi ý, chấm điểm 5 tiêu chí,
      giọng điệu, insight, giá trị keyin đề xuất (kèm mốc trích xuất) và danh sách
      công việc đề xuất. Mỗi cuộc gọi có tối đa một kết quả phân tích hiện hành; chạy
      lại AI thay thế kết quả cũ. Điểm tổng và hạng do server tự tính, không lấy từ LLM (ADR-006).
    identifier: analysisId
    attributes:
      - { name: analysisId, type: string, required: true, unique: true }
      - { name: callId, type: string, required: true, unique: true, references: "CallRecord.callId", constraint: "tối đa một CallAnalysis hiện hành / cuộc gọi" }
      - { name: summary, type: text, required: true, constraint: "tóm tắt cuộc gọi (US2.1, US3.1)" }
      - { name: suggestions, type: list, required: false, constraint: "danh sách chuỗi gợi ý tiếp theo" }
      - { name: criteriaScores, type: list, required: true, constraint: "đúng 5 phần tử CriterionScore (xem value_objects)" }
      - { name: totalScore, type: integer, required: true, min: 0, max: 100, constraint: "server tính = round(tổng 5 tiêu chí × 2); không dùng con số LLM trả" }
      - { name: grade, type: enum, required: true, allowed: [A, B, C, D], constraint: "server xếp theo ngưỡng cấu hình" }
      - { name: tone, type: enum, required: true, allowed: [TICH_CUC, TRUNG_TINH, TIEU_CUC] }
      - { name: insightStrength, type: text, required: false, constraint: "một điểm sáng nổi bật" }
      - { name: insightImprovement, type: text, required: false, constraint: "một gợi ý nâng cao" }
      - { name: suggestedKeyin, type: object, required: false, constraint: "bản đồ tên trường keyin → SuggestedValue; trường không có trong transcript bị bỏ (không bịa)" }
      - { name: suggestedTasks, type: list, required: false, constraint: "danh sách TaskDraft (xem value_objects), mỗi draft có clientKey" }
      - { name: model, type: string, required: false, constraint: "nhãn model Claude đã dùng (không chứa khóa API)" }
      - { name: createdAt, type: datetime, required: true, constraint: "ISO-8601 +07:00" }
    entity_constraints:
      - "criteriaScores có đúng 5 phần tử, mỗi score ∈ [0,10] bước 0,5; sai khoảng/bước → coi là AI thất bại (NFR3), không lưu CallAnalysis"
      - "totalScore luôn được tính lại từ criteriaScores, bỏ qua mọi tổng do LLM đưa (BR3.2)"
      - "grade suy ra từ totalScore theo ngưỡng cấu hình (A ≥ 85, B 70–84, C 50–69, D < 50)"
      - "mỗi SuggestedValue mang sourceMs tùy chọn (mốc trích xuất trong ghi âm)"
    relationships:
      - { to: CallRecord, cardinality: "1..1", direction: "mỗi CallRecord có tối đa một CallAnalysis hiện hành" }

  - name: CallKeyin
    description: >
      Hồ sơ keyin đã lưu của một cuộc gọi: giá trị các trường, nguồn từng trường
      (AI giữ nguyên / RM đã sửa / RM tự nhập) và thời điểm lưu. Lưu idempotent theo
      callId trong một giao dịch cùng với cập nhật giai đoạn lead, ghi hoạt động và
      tạo công việc. Hai trường bắt buộc: leadStage và callResult.
    identifier: keyinId
    attributes:
      - { name: keyinId, type: string, required: true, unique: true }
      - { name: callId, type: string, required: true, unique: true, references: "CallRecord.callId", constraint: "mỗi cuộc gọi có tối đa một keyin (idempotent, AC2.3.6)" }
      - { name: leadStage, type: enum, required: true, allowed: [CHUA_TIEP_CAN, DANG_TIEP_CAN, CHUYEN_CO_HOI, TU_CHOI], constraint: "trường bắt buộc" }
      - { name: callResult, type: string, required: true, constraint: "trường bắt buộc" }
      - { name: demandAmount, type: integer, required: false, min: 0, constraint: "VND, số nguyên (quy ước tiền)" }
      - { name: term, type: string, required: false, constraint: "kỳ hạn; để trống nếu transcript không nhắc (AC2.3.2)" }
      - { name: agreedRate, type: decimal, required: false, constraint: "lãi suất %/năm, 2 chữ số thập phân" }
      - { name: product, type: string, required: false }
      - { name: sourceOfFunds, type: string, required: false, constraint: "nguồn tiền luân chuyển" }
      - { name: expectedDate, type: date, required: false }
      - { name: channel, type: string, required: false, constraint: "kênh thực hiện" }
      - { name: objections, type: text, required: false, constraint: "băn khoăn / phản đối" }
      - { name: rmNote, type: text, required: false, constraint: "ghi chú của RM" }
      - { name: fieldSources, type: object, required: true, constraint: "tên trường → nguồn ∈ {AI_KEPT, RM_EDITED, RM_ENTERED} (chỉ tiêu tỷ lệ chấp nhận AI)" }
      - { name: savedAt, type: datetime, required: true, constraint: "ISO-8601 +07:00" }
    entity_constraints:
      - "leadStage và callResult phải khác rỗng; thiếu → VALIDATION_FAILED, không lưu (AC2.3.5)"
      - "lưu lặp cho cùng callId không tạo keyin thứ hai và không tạo công việc trùng (AC2.3.6)"
      - "demandAmount là integer VND (không số thực); agreedRate là decimal %/năm 2 chữ số thập phân"
    relationships:
      - { to: CallRecord, cardinality: "1..1", direction: "mỗi CallRecord có tối đa một CallKeyin" }

  - name: AiFeedback
    description: >
      Đánh giá chất lượng AI do RM nhập khi lưu hồ sơ cuộc gọi: điểm 1–10, các thẻ
      nhận xét nhanh và góp ý tự do. Mỗi cuộc gọi có tối đa một đánh giá; lưu cùng
      giao dịch keyin.
    identifier: feedbackId
    attributes:
      - { name: feedbackId, type: string, required: true, unique: true }
      - { name: callId, type: string, required: true, unique: true, references: "CallRecord.callId" }
      - { name: rating, type: integer, required: true, min: 1, max: 10, constraint: "thang 1–10" }
      - { name: tags, type: list, required: false, constraint: "danh sách thẻ nhận xét nhanh" }
      - { name: comment, type: text, required: false, constraint: "góp ý tự do, tối đa 1000 ký tự" }
    entity_constraints:
      - "rating ∈ [1,10]"
      - "mỗi cuộc gọi có tối đa một AiFeedback; lưu lặp cập nhật chứ không nhân đôi"
    relationships:
      - { to: CallRecord, cardinality: "1..1", direction: "mỗi CallRecord có tối đa một AiFeedback" }

value_objects:
  - name: CriterionScore
    description: Điểm một tiêu chí chấm cuộc gọi, kèm bằng chứng do LLM đưa.
    attributes:
      - { name: code, type: enum, required: true, allowed: [CHAO_HOI, KHAI_THAC_NHU_CAU, CHINH_SACH_LAI_SUAT, XU_LY_THAC_MAC, CHOT_CAM_KET] }
      - { name: name, type: string, required: true, constraint: "tên tiêu chí hiển thị (tiếng Việt theo mockup)" }
      - { name: score, type: decimal, required: true, min: 0, max: 10, constraint: "bước 0,5 (multipleOf 0.5)" }
      - { name: evidence, type: text, required: false, constraint: "trích dẫn/bằng chứng từ transcript" }
  - name: SuggestedValue
    description: Một giá trị keyin do AI đề xuất, kèm mốc trích xuất trong ghi âm.
    attributes:
      - { name: value, type: string, required: true, constraint: "string | integer | decimal tùy trường" }
      - { name: sourceMs, type: integer, required: false, min: 0, constraint: "mốc trích xuất; nguồn của huy hiệu 'AI · mm:ss'" }
  - name: TaskDraft
    description: >
      Một công việc tiếp theo do AI đề xuất hoặc RM thêm, dùng để tạo Task qua
      TaskService.createFromCall (I4). Mỗi dòng mang clientKey do giao diện sinh.
    attributes:
      - { name: clientKey, type: string, required: true, constraint: "khóa chống trùng; bắt buộc với createFromCall (U5 BR2.2)" }
      - { name: title, type: string, required: true, constraint: "tối đa 200 ký tự" }
      - { name: dueAt, type: datetime, required: true, constraint: "ISO-8601 +07:00" }
      - { name: durationMinutes, type: integer, required: false, default: 30 }
      - { name: type, type: enum, required: true, allowed: [LICH_HEN, NHAC_GOI, TASK_ZALO] }
      - { name: priority, type: enum, required: true, allowed: [CAO, THUONG] }
      - { name: aiSuggested, type: boolean, required: false, constraint: "true nếu dòng do AI đề xuất" }

referenced_external_entities:
  - { name: CallRecord, owned_by: "CallComponent (U1)", note: "vòng đời cuộc gọi + pipelineStatus/Step; U1 là bên duy nhất ghi trạng thái pipeline (I2). U4 đọc qua CallService.getOwnedCall và cập nhật bước ANALYZE qua updatePipeline." }
  - { name: Lead, owned_by: "LeadComponent (U1)", note: "cập nhật giai đoạn khi lưu keyin qua LeadService.updateStage (I1); U4 không định nghĩa Lead." }
  - { name: Activity, owned_by: "LeadComponent (U1)", note: "U4 ghi hoạt động cuộc gọi kèm bản sao aiSummary/aiSuggestions qua LeadService.recordCallActivity (I1); Activity do U1 sở hữu." }
  - { name: Task, owned_by: "TaskComponent (U5)", note: "U4 tạo công việc tiếp theo qua TaskService.createFromCall (I4), idempotent theo (callId, clientKey)." }
```

## Tóm tắt tập thực thể

U4 là unit lớn, sở hữu bốn thực thể của luồng sau cuộc gọi và ba value object mô tả hình dạng kết quả AI:

- **TranscriptTurn** — các lượt hội thoại tách RM/KH kèm mốc thời gian (mã STT có sẵn, U4 chỉ tách rõ hai loại lỗi). Phục vụ cột 2 và là đầu vào cho phân tích AI.
- **CallAnalysis** — kết quả AI của cuộc gọi: tóm tắt, gợi ý, điểm 5 tiêu chí, giọng điệu, insight, giá trị keyin đề xuất và công việc đề xuất. **Điểm tổng và hạng do server tự tính** từ `criteriaScores` (round(tổng × 2)), không dùng con số LLM trả; điểm ngoài khoảng/bước bị coi là AI thất bại (NFR3).
- **CallKeyin** — hồ sơ keyin đã lưu, kèm `fieldSources` ghi nguồn từng trường. Hai trường bắt buộc (`leadStage`, `callResult`); lưu idempotent theo `callId` trong một giao dịch.
- **AiFeedback** — đánh giá chất lượng AI (1–10, thẻ nhận xét, góp ý), mỗi cuộc gọi tối đa một.
- **CriterionScore / SuggestedValue / TaskDraft** — value object cho điểm tiêu chí, giá trị keyin đề xuất (kèm `sourceMs`) và công việc đề xuất (kèm `clientKey`).

Các thực thể của unit khác (`CallRecord`, `Lead`, `Activity`, `Task`) chỉ được tham chiếu qua khóa và qua các giao diện Java (I1, I2, I4) — giữ đúng ranh giới sở hữu ở `components.md` và `contract-summary.md`. Đặc biệt, U4 **không** ghi trực tiếp `pipelineStatus` mà chỉ gọi `CallService.updatePipeline` của U1.

## Sources

- `components.md` — sở hữu thực thể và thuộc tính gốc (CallAnalysisComponent, KeyinComponent, TranscriptionComponent)
- `requirements.md` — FR4.1–FR4.9, FR5.1, NFR1, NFR2, NFR3
- `contract-summary.md` — C3 (schema REST), I2/I3/I4 (giao diện Java), quy ước tiền/thời gian
- `unit-of-work.md` — mô tả và trách nhiệm U4
- `construction/u1-lead-call-foundation/functional-design/entities.md` — CallRecord (tham chiếu), ranh giới U1/U4
- `construction/u5-task-calendar/functional-design/rules.md` — createFromCall (clientKey bắt buộc, idempotent)

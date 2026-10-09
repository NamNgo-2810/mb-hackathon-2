# Entities — U3 `u3-copilot`

Mô hình thực thể của Copilot tra cứu chính sách. CopilotComponent sở hữu hai thực
thể lưu bền (`PolicyDocument`, `CopilotQueryLog`) và một số **value object** chỉ
tồn tại trong bộ nhớ khi xử lý một câu hỏi (`DocumentChunk`, `RetrievedChunk`,
`CopilotAnswer`, `Citation`). Khối YAML dưới đây là **nguồn chân lý**; kiểu dữ liệu
mô tả theo nghĩa logic, không gắn với công nghệ.

```yaml
entities:
  - name: PolicyDocument
    kind: persistent
    description: >
      Một tài liệu chính sách sản phẩm đã được trích sẵn ra file text/md và nạp vào
      chỉ mục khi khởi động. Là nguồn kiến thức duy nhất Copilot được phép trả lời.
    identifier: documentId
    attributes:
      - { name: documentId, type: string, required: true, unique: true, description: "Định danh tài liệu, suy ra ổn định từ đường dẫn file đã trích" }
      - { name: title, type: string, required: true, description: "Tên tài liệu lấy từ khối metadata ở đầu file (bắt buộc)" }
      - { name: effectiveDate, type: date, required: true, description: "Ngày hiệu lực lấy từ metadata (bắt buộc); dạng ISO-8601 (YYYY-MM-DD)" }
      - { name: sourcePdfPath, type: string, required: false, description: "Đường dẫn PDF gốc nếu metadata khai báo (tuỳ chọn)" }
      - { name: extractedTextPath, type: string, required: true, unique: true, description: "Đường dẫn file text/md đã trích trong thư mục cấu hình" }
      - { name: pageCount, type: integer, required: false, min: 1, description: "Số trang/mục nhận ra qua các dấu phân trang; null nếu tài liệu không đánh dấu trang" }
      - { name: loadedAt, type: datetime, required: true, description: "Thời điểm nạp vào chỉ mục; ISO-8601 có offset +07:00" }
    entity_constraints:
      - "Tài liệu thiếu title hoặc effectiveDate trong metadata bị bỏ qua khi nạp (không tạo PolicyDocument) và được ghi log (AC1.2.3)."
      - "File không đọc được hoặc không có nội dung chữ (PDF ảnh đã trích rỗng) bị bỏ qua và ghi log; không chặn việc nạp các file còn lại."
      - "extractedTextPath là duy nhất trong một lần nạp chỉ mục."
    relationships:
      - { with: DocumentChunk, cardinality: "1..*", direction: "PolicyDocument 1 — N DocumentChunk", description: "Mỗi tài liệu được chia thành nhiều đoạn theo trang/mục" }
      - { with: CopilotQueryLog, cardinality: "0..*", direction: "PolicyDocument N — M CopilotQueryLog", description: "Một tài liệu có thể bị trích dẫn bởi nhiều truy vấn (qua citedDocumentIds)" }

  - name: DocumentChunk
    kind: value-object
    description: >
      Một đoạn văn bản của tài liệu, cắt theo trang hoặc mục, là đơn vị để tìm kiếm
      từ khoá và để trích dẫn trang/mục. Chỉ tồn tại trong chỉ mục trong bộ nhớ.
    identifier: "(documentId, chunkIndex)"
    attributes:
      - { name: documentId, type: string, required: true, references: PolicyDocument, description: "Tài liệu chứa đoạn này" }
      - { name: chunkIndex, type: integer, required: true, min: 0, description: "Thứ tự đoạn trong tài liệu" }
      - { name: page, type: integer, required: false, min: 1, description: "Số trang nếu đoạn thuộc một trang; null nếu cắt theo mục" }
      - { name: section, type: string, required: false, description: "Nhãn mục/tiêu đề nếu đoạn cắt theo mục" }
      - { name: text, type: string, required: true, description: "Nội dung đoạn, giữ nguyên văn tài liệu" }
      - { name: normalizedText, type: string, required: true, description: "Bản text đã chuẩn hoá: bỏ dấu tiếng Việt, hạ thường, tách từ — dùng để chấm điểm từ khoá" }
    entity_constraints:
      - "Mỗi đoạn phải có ít nhất một trong hai: page hoặc section, để trích dẫn được nguồn."
    relationships:
      - { with: PolicyDocument, cardinality: "*..1", direction: "DocumentChunk N — 1 PolicyDocument", description: "Mỗi đoạn thuộc đúng một tài liệu" }
      - { with: RetrievedChunk, cardinality: "0..1", direction: "DocumentChunk 1 — 0..1 RetrievedChunk", description: "Một đoạn có thể được chọn làm kết quả tìm kiếm cho một câu hỏi" }

  - name: RetrievedChunk
    kind: value-object
    description: >
      Một DocumentChunk được chọn cho một câu hỏi cụ thể, kèm điểm khớp từ khoá.
      Là đầu vào để dựng khối DATA trong prompt và để kiểm chứng trích dẫn của LLM.
    identifier: "(documentId, chunkIndex) trong phạm vi một truy vấn"
    attributes:
      - { name: documentId, type: string, required: true, references: PolicyDocument }
      - { name: chunkIndex, type: integer, required: true, min: 0, references: DocumentChunk }
      - { name: score, type: number, required: true, min: 0, description: "Điểm khớp theo số từ khoá trùng (có dấu và không dấu)" }
      - { name: page, type: integer, required: false, description: "Sao từ DocumentChunk để trích dẫn" }
      - { name: section, type: string, required: false, description: "Sao từ DocumentChunk để trích dẫn" }
    entity_constraints:
      - "score > 0 (đoạn có điểm 0 không được chọn)."
      - "Số RetrievedChunk cho một truy vấn ≤ topK (mặc định cấu hình được)."
    relationships:
      - { with: CopilotAnswer, cardinality: "*..1", direction: "RetrievedChunk N — 1 CopilotAnswer", description: "Các đoạn tìm được là ngữ cảnh của đúng một câu trả lời" }

  - name: CopilotAnswer
    kind: value-object
    description: >
      Kết quả trả về cho một câu hỏi: trạng thái tìm thấy hay không, nội dung trả lời,
      danh sách trích dẫn đã kiểm chứng, và 2–4 câu hỏi gợi ý. Khớp schema C2.
    identifier: "theo truy vấn (không lưu bền)"
    attributes:
      - { name: answerFound, type: boolean, required: true, description: "false khi không có đoạn khớp hoặc LLM báo không có thông tin" }
      - { name: answer, type: string, required: true, description: "Nội dung trả lời (markdown đơn giản). Khi answerFound=false là đúng câu 'Không tìm thấy thông tin này trong tài liệu chính sách hiện có'" }
      - { name: citations, type: "array<Citation>", required: true, description: "Rỗng khi answerFound=false; chỉ gồm trích dẫn đã kiểm chứng với RetrievedChunk" }
      - { name: suggestions, type: "array<string>", required: true, min: 0, max: 4, description: "2–4 câu hỏi gợi ý khi answerFound=true; có thể rỗng khi không tìm thấy" }
    entity_constraints:
      - "answerFound=false ⇒ answer là câu không-tìm-thấy cố định AND citations rỗng (BR không gọi LLM)."
      - "answerFound=true ⇒ citations không rỗng, mọi Citation phải trùng một RetrievedChunk đã dùng."
    relationships:
      - { with: Citation, cardinality: "0..*", direction: "CopilotAnswer 1 — N Citation" }
      - { with: CopilotQueryLog, cardinality: "1..1", direction: "CopilotAnswer 1 — 1 CopilotQueryLog", description: "Mỗi câu trả lời sinh đúng một dòng nhật ký truy vấn" }

  - name: Citation
    kind: value-object
    description: Trích dẫn nguồn kèm ngày hiệu lực, hiển thị cạnh câu trả lời.
    identifier: "(documentId, page|section) trong một CopilotAnswer"
    attributes:
      - { name: documentId, type: string, required: false, references: PolicyDocument, description: "Có thể vắng nếu tài liệu không có id ổn định, nhưng title luôn có" }
      - { name: documentTitle, type: string, required: true }
      - { name: page, type: integer, required: false, description: "page hoặc section phải có ít nhất một" }
      - { name: section, type: string, required: false }
      - { name: effectiveDate, type: date, required: true, description: "Ngày hiệu lực của tài liệu được trích (bắt buộc, FR3.2)" }
    entity_constraints:
      - "Mỗi Citation phải map tới một RetrievedChunk đã nằm trong ngữ cảnh; trích dẫn không khớp bị loại (BR kiểm chứng trích dẫn)."
      - "Phải có ít nhất một trong page hoặc section."

  - name: CopilotQueryLog
    kind: persistent
    description: >
      Nhật ký mỗi lượt hỏi Copilot, phục vụ đếm lượt dùng cho OKR. Không lưu toàn văn
      câu trả lời; chỉ lưu đủ để đếm và truy vết.
    identifier: queryId
    attributes:
      - { name: queryId, type: string, required: true, unique: true }
      - { name: rmId, type: string, required: true, description: "RM hiện tại (CurrentRmProvider của U1); demo = RM mẫu" }
      - { name: callId, type: string, required: false, description: "Cuộc gọi đang diễn ra nếu có (panel Copilot trên S2)" }
      - { name: question, type: string, required: true, max: 1000, description: "Câu hỏi RM gửi, giữ nguyên văn" }
      - { name: answerFound, type: boolean, required: true }
      - { name: citedDocumentIds, type: "array<string>", required: true, description: "Rỗng khi không tìm thấy; dùng để đếm tài liệu được trích" }
      - { name: askedAt, type: datetime, required: true, description: "ISO-8601 có offset +07:00" }
    entity_constraints:
      - "Một lượt hỏi (một lời gọi POST /api/copilot/ask) tạo đúng một CopilotQueryLog, kể cả khi answerFound=false hoặc LLM lỗi."
    relationships:
      - { with: PolicyDocument, cardinality: "*..*", direction: "CopilotQueryLog N — M PolicyDocument (qua citedDocumentIds)" }
```

## Tóm tắt mô hình thực thể

Copilot có hai thực thể **lưu bền**: `PolicyDocument` (tài liệu chính sách đã trích,
mỗi tài liệu bắt buộc có `title` và `effectiveDate`) và `CopilotQueryLog` (nhật ký
mỗi lượt hỏi, để đếm lượt dùng theo OKR). Khi khởi động, mỗi `PolicyDocument` được
chia thành nhiều `DocumentChunk` theo trang/mục, kèm `normalizedText` đã bỏ dấu để
tìm từ khoá cả có dấu lẫn không dấu.

Khi RM hỏi, Copilot chọn các `DocumentChunk` khớp thành `RetrievedChunk` (điểm > 0),
nhúng chúng vào prompt và tạo một `CopilotAnswer` gồm `citations` (mỗi `Citation`
phải khớp một `RetrievedChunk` đã dùng, kèm `effectiveDate`) và 2–4 `suggestions`.
`DocumentChunk`, `RetrievedChunk`, `CopilotAnswer` và `Citation` là **value object**
chỉ tồn tại trong bộ nhớ của một lượt xử lý — không lưu xuống CSDL. Lịch sử tra cứu
và "cuộc trò chuyện mới" do giao diện giữ (C2 `history ≤ 10`), nên không có thực thể
hội thoại nào ở phía server.

## Sources

- `unit-of-work.md` (U3), `unit-of-work-story-map.md`
- `requirements.md` (FR3.1–FR3.6, NFR1, NFR2)
- `domain-design/components.md` (CopilotComponent, LlmClient), `domain-design/decisions.md` (ADR-003, ADR-004)
- `contract-design/contract-summary.md` (C2, I1, I3)
- `user-stories/stories.md` (US1.2, US1.3)

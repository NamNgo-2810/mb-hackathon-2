# Contract Summary

Đầu vào:
- `unit-of-work.md` và `unit-of-work-dependency.md`: 6 unit, 7 cạnh phụ thuộc
- `components.md`: thực thể và ranh giới
- `requirements.md`: FR, NFR1–NFR3
- API hiện có trong `api-documentation.md`

Các quyết định đã chốt (`contract-design-questions.md`):
- [Q1=A] Giữ tiền tố `/api/...`, không đánh phiên bản. Chỉ được **thêm** trường; muốn đổi theo kiểu phá vỡ tương thích thì tạo đường dẫn mới.
- [Q2=A] Khi bật chế độ `login`, xác thực bằng token Bearer gửi trong header `Authorization`.

Có hai loại hợp đồng:
- **REST** giữa trình duyệt (giao diện do Node phục vụ) và Spring Boot. Phần giao diện của mỗi unit gọi phần backend của chính unit đó và của các unit mà nó phụ thuộc.
- **Giao diện Java trong cùng tiến trình** giữa các unit backend (ADR-001).

## Bảng hợp đồng

| # | Provider Unit | Consumer | Mechanism | Owner |
|---|---|---|---|---|
| C1 | U1 `u1-lead-call-foundation` | External: giao diện web (S1, S2, S4, S5) | REST/JSON (OpenAPI) | U1 |
| C2 | U3 `u3-copilot` | External: giao diện web (panel Copilot S2) | REST/JSON (OpenAPI) | U3 |
| C3 | U4 `u4-call-analysis-result` | External: giao diện web (S3, S4, khối tóm tắt ở S1) | REST/JSON (OpenAPI) | U4 |
| C4 | U5 `u5-task-calendar` | External: giao diện web (S5, S6) | REST/JSON (OpenAPI) | U5 |
| C5 | U6 `u6-login-lead-list` | External: giao diện web (S0, danh sách lead S1) | REST/JSON (OpenAPI) | U6 |
| C6 | U1 (MicrosipBridge, Node, có sẵn) | External: giao diện web | REST + SSE (giữ nguyên `/api/microsip/*`) | U1 |
| I1 | U1 | U3, U4, U5, U6 | Giao diện Java: `CurrentRmProvider`, `LeadService` | U1 |
| I2 | U1 | U4 | Giao diện Java: `CallService`, `CallPipelineStep` (điểm mở rộng pipeline) | U1 |
| I3 | U2 `u2-llm-client` | U3, U4 | Giao diện Java: `LlmClient` | U2 |
| I4 | U5 | U4 | Giao diện Java: `TaskService.createFromCall` | U5 |

## Quy ước chung cho REST (áp dụng cho C1–C5)

- **Base URL**: `http://localhost:8080`. CORS chỉ cho phép origin của server Node (`http://localhost:3000`). Phải mở thêm các method `PUT`, `PATCH` và header `Authorization` so với cấu hình hiện tại.
- **Xác thực**: thêm header `Authorization: Bearer <token>`.
  - Ở chế độ `demo` (ADR-007), header này không bắt buộc. Server luôn coi người gọi là RM mẫu.
  - Ở chế độ `login`, thiếu token hoặc token sai thì trả `401 UNAUTHORIZED`.
- **Quyền**: truy cập lead, cuộc gọi hoặc công việc không thuộc RM hiện tại thì trả `403 FORBIDDEN` (FR1.4). Không tiết lộ dữ liệu đó có tồn tại hay không. Không tìm thấy dữ liệu của chính RM thì trả `404 NOT_FOUND`.
- **Định dạng lỗi** (giữ như hiện có):

```yaml
ErrorEnvelope:
  type: object
  required: [error]
  properties:
    error:
      type: object
      required: [code, message, timestamp]
      properties:
        code: { type: string, enum: [VALIDATION_FAILED, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT, RECORDING_NOT_READY, STT_ERROR, AI_ERROR, INTERNAL_ERROR] }
        message: { type: string, description: "Thông điệp tiếng Việt hiển thị được cho người dùng" }
        details: { type: object, additionalProperties: true, description: "Ví dụ: { field: 'leadStage' } khi VALIDATION_FAILED" }
        timestamp: { type: string, format: date-time }
```

- **Thời gian**: mọi giá trị dùng ISO-8601 có offset (`+07:00`). Ranh giới ngày tính theo múi giờ `Asia/Ho_Chi_Minh`.
- **Tiền**: dùng số nguyên VND (`integer`), không dùng số thực. Lãi suất là `number` theo đơn vị %/năm, làm tròn 2 chữ số thập phân.
- **Ghi idempotent**: các thao tác "lưu" (C3 keyin) và "kết thúc cuộc gọi" (C1) có gọi lặp lại thì kết quả vẫn như gọi một lần.
- **Timeout phía giao diện**: 10 giây cho REST thường, 35 giây cho `POST /api/copilot/ask` (NFR1: quá 30 giây thì báo lỗi).

## C1 — Lead & cuộc gọi (U1)

```yaml
openapi: 3.0.3
info: { title: U1 Lead & Call API, version: "1" }
paths:
  /api/me:
    get:
      summary: RM hiện tại (chế độ demo trả RM mẫu)
      responses:
        "200": { content: { application/json: { schema: { $ref: "#/components/schemas/Rm" } } } }
        "401": { description: Chế độ login, thiếu/sai token }
  /api/leads/demo:
    get:
      summary: Lead mẫu để mở thẳng S1 khi chưa có U6 (chỉ bật ở chế độ demo)
      responses:
        "200": { content: { application/json: { schema: { $ref: "#/components/schemas/LeadDetail" } } } }
        "404": { description: Không ở chế độ demo hoặc chưa seed }
  /api/leads/{leadId}:
    get:
      parameters: [{ name: leadId, in: path, required: true, schema: { type: string } }]
      responses:
        "200": { content: { application/json: { schema: { $ref: "#/components/schemas/LeadDetail" } } } }
        "403": { description: Lead không thuộc RM }
        "404": { description: Không tồn tại }
  /api/leads/{leadId}/activities:
    get:
      summary: Lịch sử hoạt động, mới nhất trước (US3.1 hiển thị aiSummary)
      responses:
        "200": { content: { application/json: { schema: { type: array, items: { $ref: "#/components/schemas/Activity" } } } } }
        "403": { description: Lead không thuộc RM }
  /api/leads/next:
    get:
      summary: Lead kế tiếp (FR4.9) — ưu tiên cao nhất ở CHUA_TIEP_CAN/DANG_TIEP_CAN, loại excludeLeadId
      parameters: [{ name: excludeLeadId, in: query, required: false, schema: { type: string } }]
      responses:
        "200": { content: { application/json: { schema: { $ref: "#/components/schemas/LeadSummary" } } } }
        "204": { description: Không còn lead cần xử lý }
  /api/calls:
    post:
      summary: Tạo cuộc gọi từ lead (FR1.5). Giao diện sau đó gọi /api/microsip/call (C6).
      requestBody: { content: { application/json: { schema: { type: object, required: [leadId], properties: { leadId: { type: string } } } } } }
      responses:
        "201": { content: { application/json: { schema: { $ref: "#/components/schemas/Call" } } } }
        "400": { description: VALIDATION_FAILED — số điện thoại không hợp lệ }
        "403": { description: Lead không thuộc RM }
        "409": { description: CONFLICT — đang có một cuộc gọi khác chưa xử lý xong (giới hạn một cuộc gọi tại một thời điểm) }
  /api/calls/{callId}/ended:
    post:
      summary: Kết thúc cuộc gọi, idempotent; lần đầu khởi chạy pipeline (có sẵn, mở rộng)
      responses:
        "200": { content: { application/json: { schema: { $ref: "#/components/schemas/Call" } } } }
        "403": { description: Không thuộc RM }
  /api/calls/{callId}:
    get:
      summary: Trạng thái cuộc gọi + pipeline (hỏi định kỳ 3 giây)
      responses:
        "200": { content: { application/json: { schema: { $ref: "#/components/schemas/Call" } } } }
  /api/calls/{callId}/transcript:
    get: { summary: Có sẵn — lượt RM/KH, responses: { "200": { description: "TranscriptTurn[] (mảng rỗng = chưa có)" } } }
  /api/calls/{callId}/recording:
    get: { summary: Có sẵn — audio/wav, responses: { "200": { description: audio/wav }, "404": { description: RECORDING_NOT_READY } } }
components:
  schemas:
    Rm: { type: object, required: [rmId, displayName], properties: { rmId: { type: string }, displayName: { type: string } } }
    LeadSummary:
      type: object
      required: [leadId, leadCode, customerName, productInterest, priority, stage]
      properties:
        leadId: { type: string }
        leadCode: { type: string }
        customerName: { type: string }
        productInterest: { type: string }
        source: { type: string }
        priority: { type: string, enum: [KHAI_THAC_NGAY, HOT_LEAD, THUONG] }
        rankScore: { type: integer, minimum: 0, maximum: 10 }
        slaDueAt: { type: string, format: date-time }
        stage: { $ref: "#/components/schemas/LeadStage" }
    LeadDetail:
      allOf:
        - $ref: "#/components/schemas/LeadSummary"
        - type: object
          properties:
            customer:
              type: object
              properties: { customerId: { type: string }, fullName: { type: string }, segment: { type: string }, phoneNumber: { type: string }, age: { type: integer }, occupation: { type: string }, estimatedIncome: { type: integer }, address: { type: string } }
            content: { type: object, additionalProperties: true, description: "Khối Nội dung (thông tin TNB, sản phẩm, dư nợ)" }
            salesScript: { type: string }
            phoneValid: { type: boolean }
    LeadStage: { type: string, enum: [CHUA_TIEP_CAN, DANG_TIEP_CAN, CHUYEN_CO_HOI, TU_CHOI] }
    Activity:
      type: object
      required: [activityId, type, occurredAt]
      properties:
        activityId: { type: string }
        type: { type: string, enum: [CUOC_GOI, HEN_GAP, SMS, KHAC] }
        occurredAt: { type: string, format: date-time }
        product: { type: string }
        result: { type: string }
        note: { type: string }
        callId: { type: string, nullable: true }
        aiSummary: { type: string, nullable: true, description: "null = 'Không có tóm tắt AI'" }
        aiSuggestions: { type: array, items: { type: string } }
    Call:
      type: object
      required: [callId, leadId, pipelineStatus]
      properties:
        callId: { type: string }
        leadId: { type: string }
        rmId: { type: string }
        phoneNumber: { type: string }
        startedAt: { type: string, format: date-time }
        endedAt: { type: string, format: date-time, nullable: true }
        durationSeconds: { type: integer, nullable: true }
        pipelineStatus: { type: string, enum: [IN_CALL, PROCESSING, DONE, NO_RECORDING, STT_FAILED, AI_FAILED] }
        pipelineStep: { type: string, enum: [FETCH_RECORDING, TRANSCRIBE, ANALYZE], nullable: true }
        errorDetail: { type: string, nullable: true }
        sttStatus: { type: string, description: "Giữ để tương thích với giao diện cũ" }
```

## C2 — Copilot (U3)

```yaml
openapi: 3.0.3
info: { title: U3 Copilot API, version: "1" }
paths:
  /api/copilot/ask:
    post:
      summary: Hỏi chính sách — chỉ trả lời từ tài liệu (FR3.2, FR3.3)
      requestBody:
        content:
          application/json:
            schema:
              type: object
              required: [question]
              properties:
                question: { type: string, minLength: 1, maxLength: 1000 }
                callId: { type: string, nullable: true }
                history:
                  type: array
                  maxItems: 10
                  description: "Ngữ cảnh trong phiên do giao diện giữ (rỗng = cuộc trò chuyện mới)"
                  items: { type: object, properties: { role: { type: string, enum: [user, assistant] }, text: { type: string } } }
      responses:
        "200":
          content:
            application/json:
              schema:
                type: object
                required: [answerFound, answer, citations, suggestions]
                properties:
                  answerFound: { type: boolean }
                  answer: { type: string, description: "Markdown đơn giản (đoạn, danh sách, bảng); giao diện escape trước khi hiển thị" }
                  citations:
                    type: array
                    items: { type: object, required: [documentTitle, effectiveDate], properties: { documentId: { type: string }, documentTitle: { type: string }, page: { type: integer, nullable: true }, section: { type: string, nullable: true }, effectiveDate: { type: string, format: date } } }
                  suggestions: { type: array, maxItems: 4, items: { type: string } }
        "400": { description: VALIDATION_FAILED }
        "502": { description: "AI_ERROR — LLM lỗi hoặc quá 30 giây (FR3.6)" }
  /api/copilot/documents:
    get:
      summary: Danh sách tài liệu đã nạp (chân cột lịch sử tra cứu)
      responses:
        "200": { content: { application/json: { schema: { type: array, items: { type: object, properties: { documentId: { type: string }, title: { type: string }, effectiveDate: { type: string, format: date } } } } } } }
```

Khi `answerFound=false`, giá trị `answer` là đúng câu "Không tìm thấy thông tin này trong tài liệu chính sách hiện có", và `citations` là mảng rỗng.

## C3 — Phân tích & keyin (U4)

```yaml
openapi: 3.0.3
info: { title: U4 Analysis & Keyin API, version: "1" }
paths:
  /api/calls/{callId}/analysis:
    get:
      summary: Kết quả AI (cột 1 + giá trị đề xuất cột 3)
      responses:
        "200": { content: { application/json: { schema: { $ref: "#/components/schemas/CallAnalysis" } } } }
        "404": { description: "Chưa có (đang xử lý hoặc AI lỗi — xem pipelineStatus ở C1)" }
  /api/calls/{callId}/analysis/retry:
    post:
      summary: Chạy lại riêng bước AI (FR4.8); chỉ khi có transcript
      responses:
        "202": { description: Đã nhận; trạng thái quay về PROCESSING/ANALYZE }
        "409": { description: "CONFLICT — chưa có transcript, hoặc AI đang chạy" }
  /api/calls/{callId}/keyin:
    get:
      responses:
        "200": { content: { application/json: { schema: { $ref: "#/components/schemas/Keyin" } } } }
        "404": { description: Chưa lưu }
    put:
      summary: Lưu hồ sơ cuộc gọi — idempotent theo callId (AC2.3.6). Một giao dịch — keyin + đánh giá AI + giai đoạn lead + hoạt động + công việc.
      requestBody: { content: { application/json: { schema: { $ref: "#/components/schemas/KeyinSave" } } } }
      responses:
        "200": { content: { application/json: { schema: { $ref: "#/components/schemas/KeyinSaved" } } } }
        "400": { description: "VALIDATION_FAILED — details.field = leadStage | callResult | nextTasks[i].title | nextTasks[i].dueAt" }
        "403": { description: Không thuộc RM }
components:
  schemas:
    CallAnalysis:
      type: object
      required: [summary, criteria, totalScore, grade, tone, suggestedKeyin, suggestedTasks]
      properties:
        summary: { type: string }
        suggestions: { type: array, items: { type: string } }
        criteria:
          type: array
          minItems: 5
          maxItems: 5
          items: { type: object, required: [code, name, score], properties: { code: { type: string, enum: [CHAO_HOI, KHAI_THAC_NHU_CAU, CHINH_SACH_LAI_SUAT, XU_LY_THAC_MAC, CHOT_CAM_KET] }, name: { type: string }, score: { type: number, minimum: 0, maximum: 10, multipleOf: 0.5 } } }
        totalScore: { type: integer, minimum: 0, maximum: 100, description: "Server tính = round(tổng × 2)" }
        grade: { type: string, enum: [A, B, C, D] }
        tone: { type: string, enum: [TICH_CUC, TRUNG_TINH, TIEU_CUC] }
        insightStrength: { type: string }
        insightImprovement: { type: string }
        suggestedKeyin:
          type: object
          description: "Mỗi trường có thể vắng (không có trong transcript thì để trống)"
          additionalProperties: { $ref: "#/components/schemas/SuggestedValue" }
        suggestedTasks: { type: array, items: { $ref: "#/components/schemas/TaskDraft" } }
    SuggestedValue:
      type: object
      required: [value]
      properties:
        value: { description: "string | integer | number tuỳ trường" }
        sourceMs: { type: integer, nullable: true, description: "Mốc trích xuất trong ghi âm — nguồn của huy hiệu 'AI · mm:ss'" }
    KeyinFields:
      type: object
      properties:
        leadStage: { $ref: "#/components/schemas/LeadStage" }
        callResult: { type: string }
        demandAmount: { type: integer, nullable: true, description: VND }
        term: { type: string, nullable: true }
        agreedRate: { type: number, nullable: true }
        product: { type: string, nullable: true }
        sourceOfFunds: { type: string, nullable: true }
        expectedDate: { type: string, format: date, nullable: true }
        channel: { type: string, nullable: true }
        objections: { type: string, nullable: true }
        rmNote: { type: string, nullable: true }
    KeyinSave:
      allOf:
        - $ref: "#/components/schemas/KeyinFields"
        - type: object
          required: [leadStage, callResult, fieldSources, nextTasks]
          properties:
            fieldSources:
              type: object
              description: "Tên trường → nguồn; giao diện xác định bằng cách so với suggestedKeyin"
              additionalProperties: { type: string, enum: [AI_KEPT, RM_EDITED, RM_ENTERED] }
            nextTasks: { type: array, items: { $ref: "#/components/schemas/TaskDraft" } }
            aiFeedback:
              type: object
              nullable: true
              properties: { rating: { type: integer, minimum: 1, maximum: 10 }, tags: { type: array, items: { type: string } }, comment: { type: string, maxLength: 1000 } }
    Keyin: { allOf: [{ $ref: "#/components/schemas/KeyinSave" }, { type: object, properties: { savedAt: { type: string, format: date-time } } }] }
    KeyinSaved:
      type: object
      properties:
        callId: { type: string }
        interactionCode: { type: string, description: "Ví dụ #CALL-20261008-01, hiển thị ở S4" }
        createdTaskIds: { type: array, items: { type: string } }
    TaskDraft:
      type: object
      required: [title, dueAt, type, priority]
      properties:
        clientKey: { type: string, description: "Khoá do giao diện sinh cho mỗi dòng — chống tạo trùng khi lưu lặp" }
        title: { type: string, maxLength: 200 }
        dueAt: { type: string, format: date-time }
        durationMinutes: { type: integer, default: 30 }
        type: { type: string, enum: [LICH_HEN, NHAC_GOI, TASK_ZALO] }
        priority: { type: string, enum: [CAO, THUONG] }
        aiSuggested: { type: boolean }
    LeadStage: { type: string, enum: [CHUA_TIEP_CAN, DANG_TIEP_CAN, CHUYEN_CO_HOI, TU_CHOI] }
```

**Quy tắc "chỉ điền vào trường trống" khi chạy lại AI** (AC2.4.3, Domain Design R-04): phía giao diện (U4) thực hiện quy tắc này. Khi có `CallAnalysis` mới, giao diện chỉ gán `suggestedKeyin` vào những trường RM chưa nhập. Server không ghi đè keyin, vì keyin chỉ được tạo khi người dùng gọi `PUT`.

**Mốc thời gian trích xuất** (Domain Design R-02) nằm ở `SuggestedValue.sourceMs` thuộc `CallAnalysis`. Còn `fieldSources` của keyin chỉ ghi nhận trường đó do AI điền và RM giữ nguyên, RM sửa lại, hay RM tự nhập.

## C4 — Công việc & lịch (U5)

```yaml
openapi: 3.0.3
info: { title: U5 Task API, version: "1" }
paths:
  /api/tasks:
    get:
      summary: Công việc của RM hiện tại trong khoảng thời gian
      parameters:
        - { name: from, in: query, required: true, schema: { type: string, format: date-time } }
        - { name: to, in: query, required: true, schema: { type: string, format: date-time }, description: "Tối đa 42 ngày (đủ cho chế độ Tháng)" }
        - { name: type, in: query, required: false, schema: { type: string, enum: [LICH_HEN, NHAC_GOI, TASK_ZALO, KHAC] } }
        - { name: sourceCallId, in: query, required: false, schema: { type: string }, description: "Dùng cho khối 'AI Key-in từ cuộc gọi'" }
      responses:
        "200": { content: { application/json: { schema: { type: array, items: { $ref: "#/components/schemas/Task" } } } } }
        "400": { description: "VALIDATION_FAILED — khoảng sai hoặc quá 42 ngày" }
    post:
      summary: Tạo công việc thủ công (S6)
      requestBody: { content: { application/json: { schema: { $ref: "#/components/schemas/TaskCreate" } } } }
      responses:
        "201": { content: { application/json: { schema: { $ref: "#/components/schemas/Task" } } } }
        "400": { description: "VALIDATION_FAILED — thiếu tiêu đề / dueAt" }
        "403": { description: leadId không thuộc RM }
  /api/tasks/{taskId}:
    patch:
      summary: Đánh dấu xong hoặc đổi giờ
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                status: { type: string, enum: [DONE] }
                dueAt: { type: string, format: date-time }
                confirmPast: { type: boolean, default: false, description: "Bắt buộc true khi dueAt ở quá khứ (AC3.3.3)" }
      responses:
        "200": { content: { application/json: { schema: { $ref: "#/components/schemas/Task" } } } }
        "400": { description: "VALIDATION_FAILED — details.field = dueAt, reason = PAST_NOT_CONFIRMED" }
        "403": { description: Không thuộc RM }
components:
  schemas:
    TaskCreate:
      type: object
      required: [title, type, dueAt]
      properties:
        title: { type: string, minLength: 1, maxLength: 200 }
        type: { type: string, enum: [LICH_HEN, NHAC_GOI, TASK_ZALO, KHAC] }
        dueAt: { type: string, format: date-time }
        durationMinutes: { type: integer, enum: [15, 30, 60], default: 30 }
        priority: { type: string, enum: [CAO, THUONG], default: THUONG }
        leadId: { type: string, nullable: true }
        note: { type: string, nullable: true }
    Task:
      allOf:
        - $ref: "#/components/schemas/TaskCreate"
        - type: object
          properties:
            taskId: { type: string }
            status: { type: string, enum: [TODO, DONE] }
            origin: { type: string, enum: [AI_FROM_CALL, MANUAL, SEED] }
            sourceCallId: { type: string, nullable: true }
            leadCode: { type: string, nullable: true }
            customerName: { type: string, nullable: true }
            aiSummary: { type: string, nullable: true, description: "Tóm tắt AI của cuộc gọi gốc (thẻ ở panel trái S5)" }
```

## C5 — Đăng nhập & danh sách lead (U6)

```yaml
openapi: 3.0.3
info: { title: U6 Auth & Lead list API, version: "1" }
paths:
  /api/auth/login:
    post:
      requestBody: { content: { application/json: { schema: { type: object, required: [username, password], properties: { username: { type: string, maxLength: 64 }, password: { type: string, maxLength: 128 } } } } } }
      responses:
        "200": { content: { application/json: { schema: { type: object, required: [token, expiresAt, rm], properties: { token: { type: string, description: "Chuỗi ngẫu nhiên ≥ 32 byte, chỉ lưu dạng băm ở server" }, expiresAt: { type: string, format: date-time }, rm: { $ref: "#/components/schemas/Rm" } } } } } }
        "401": { description: "UNAUTHORIZED — 'Sai tên đăng nhập hoặc mật khẩu' (không nói rõ sai trường nào)" }
  /api/auth/logout:
    post: { responses: { "204": { description: Token bị thu hồi } } }
  /api/leads:
    get:
      summary: Danh sách lead của RM hiện tại, sắp theo ưu tiên rồi hạn SLA
      responses:
        "200": { content: { application/json: { schema: { type: array, items: { $ref: "#/components/schemas/LeadSummary" } } } } }
components:
  schemas:
    Rm: { type: object, properties: { rmId: { type: string }, displayName: { type: string } } }
    LeadSummary: { description: "Như C1" }
```

## C6 — MicroSIP (Node, có sẵn)

Giữ nguyên `POST /api/microsip/call`, `GET /api/microsip/events` (SSE) và `GET /api/microsip/event` như trong `api-documentation.md`. Giao diện dùng sự kiện `end` trên SSE để gọi `POST /api/calls/{callId}/ended` (C1). Server phía C1 xử lý idempotent nên sự kiện có lặp lại cũng không sao.

## Hợp đồng nội bộ (giao diện Java)

```yaml
# Shared-schema: in-process Java interfaces (package-level public API of each unit)
I1_U1:
  CurrentRmProvider:
    currentRm(): "Rm — demo: RM mẫu; login: RM của token (U6 cung cấp bản hiện thực login)"
  LeadService:
    getOwnedLead(rmId, leadId): "LeadDetail | throws ForbiddenException | NotFoundException"
    assertOwned(rmId, leadId): "void | throws ForbiddenException"
    updateStage(rmId, leadId, LeadStage): void
    recordCallActivity(rmId, leadId, callId, product, result, note, aiSummary?, aiSuggestions[]): "activityId — idempotent theo callId"
    findNextLead(rmId, excludeLeadId?): "Optional<LeadSummary>"
I2_U1:
  CallService:
    getOwnedCall(rmId, callId): "Call | throws ForbiddenException | NotFoundException"
    updatePipeline(callId, pipelineStatus, pipelineStep?, errorDetail?): void
  CallPipelineStep:
    description: "Điểm mở rộng: U1 điều phối vòng đời; sau khi STT xong U1 gọi các bước đã đăng ký theo thứ tự. U4 đăng ký bước ANALYZE. U1 sở hữu CallRecord và chỉ U1 ghi trạng thái (qua updatePipeline)."
    run(callId, transcriptTurns[], leadContext): "StepResult{ok | failed(kind)}"
I3_U2:
  LlmClient:
    complete(LlmRequest{ systemPrompt, cachedContext?, userContent, maxOutputTokens, timeout, expectJson }): "LlmResponse{ text, model, usage }"
    errors: "LlmException(kind: TIMEOUT | UNAVAILABLE | RATE_LIMITED | BAD_RESPONSE | NOT_CONFIGURED)"
    note: "NOT_CONFIGURED khi chưa có ANTHROPIC_API_KEY — U3/U4 xử lý như AI_ERROR; test dùng bản giả lập"
I4_U5:
  TaskService:
    createFromCall(rmId, leadId, callId, TaskDraft[]): "taskId[] — idempotent theo (callId, clientKey)"
```

Ghi chú về ranh giới U1/U4 (Units Generation R-01):
- U1 sở hữu `CallRecord` và là bên duy nhất ghi trạng thái pipeline.
- Phần của U4 chỉ cắm vào qua `CallPipelineStep`, đồng thời gọi `updatePipeline` cho bước ANALYZE.
- Hai unit không sửa đè mã của nhau.

Theo TranscriptionComponent (thuộc U4, mã có sẵn), bước STT phải báo lỗi phân biệt được giữa `RECORDING_NOT_READY` và `STT_ERROR`. Nhờ đó U1 chọn đúng trạng thái `NO_RECORDING` hoặc `STT_FAILED`.

## Quy tắc sở hữu và thay đổi hợp đồng

- Mỗi hợp đồng thuộc về unit cung cấp (cột Owner). Chỉ chủ sở hữu được sửa spec.
- Thay đổi **thêm vào** (trường mới không bắt buộc, giá trị enum mới ở phía trả về) được làm ngay. Phía dùng phải bỏ qua các trường mình không biết.
- Thay đổi **phá vỡ tương thích** (xóa hoặc đổi tên trường, đổi kiểu, thêm trường bắt buộc vào request) không sửa tại chỗ. Phải tạo đường dẫn hoặc phương thức mới, và cập nhật tài liệu này [Q1=A].
- Các API cũ (`/api/calls/initiate`, `sttStatus`) giữ nguyên để không làm hỏng trang cũ.

## Open questions

| Contract | Question | Blocks |
|---|---|---|
| C2 | Nguồn tài liệu chính sách: có thể chỉ là file text/md; chưa chốt định dạng metadata (tên tài liệu, ngày hiệu lực, cách đánh dấu trang) — người dùng để sau (Domain Design R-03) | U3 |
| I3 | Model Claude cụ thể và khoá API — người dùng cung cấp khi tới bước này (O1) | U2 (bản thật), không chặn U3/U4 nhờ bản giả lập |
| C5 | Thời hạn token (đề xuất 8 giờ) | U6 |

## Sources

- `unit-of-work.md`, `unit-of-work-dependency.md`, `components.md`, `requirements.md`
- `api-documentation.md` (codekb)
- `contract-design-questions.md`

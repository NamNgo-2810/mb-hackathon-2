# CRM Call Assistant — backend

Spring Boot 3 (Java 17) service for the core slice: open a call record, download
the per-call recording from the FreeSWITCH/EC2 server, and run Vietnamese
speech-to-text with RM (agent) / KH (customer) separation.

The call itself is placed from the frontend via MicroSIP (the existing Node
`/api/microsip/call` route). **This backend never spawns MicroSIP.**

## What it does

| Step | Endpoint | Notes |
|---|---|---|
| Open a call | `POST /api/calls/initiate` `{phoneNumber, tnbCode}` | Creates a `CallRecord` (PENDING), returns the id (FR1.1) |
| End a call | `POST /api/calls/{id}/ended` | Marks ended, runs download → STT **async** (FR1.3) |
| Poll status | `GET /api/calls/{id}` | `sttStatus` = `PENDING \| PROCESSING \| DONE \| FAILED` |
| Download recording | `GET /api/calls/{id}/recording` | Streams the `.wav` (FR3.1) |
| Transcript | `GET /api/calls/{id}/transcript` | RM/KH-labeled turns in Vietnamese (FR4) |

## Prerequisites

- JDK 17
- Maven (or use your IDE's bundled Maven)
- Docker (for local Postgres), or run the `demo` profile for zero-infra H2

## Configuration (`application.properties`)

Config lives in `src/main/resources/application.properties` (default profile)
and `src/main/resources/application-demo.properties` (H2 demo profile). Every
value resolves from an environment variable first, with a safe local-demo
default after the `:` — so you can either **edit the properties file directly**
or **set the matching env var** (the env var wins). No real secrets live in
source (NFR3).

Keys you will usually set:

```properties
server.port=${BACKEND_PORT:8080}
spring.profiles.active=${SPRING_PROFILES_ACTIVE:default}   # or "demo" for H2 + seed
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/callassistant}
spring.datasource.username=${DB_USERNAME:callassistant}
spring.datasource.password=${DB_PASSWORD:callassistant}
app.recording.base-url=${RECORDING_BASE_URL:}             # EC2 recordings server
app.recording.api-key=${RECORDING_API_KEY:}
app.stt.base-url=${STT_BASE_URL:}                         # Whisper-compatible endpoint
app.stt.rm-channel=${STT_RM_CHANNEL:left}                 # which stereo channel is RM (left|right)
app.cors.allowed-origins=${FRONTEND_ORIGINS:http://localhost:3000,http://127.0.0.1:3000}
```

For a quick local run you can simply edit `app.recording.base-url` and
`app.stt.base-url` in `application.properties` and start the backend — no env
exporting required. `.env.example` documents the same keys for env-based setups.

## Run it

### 1. Start Postgres (default profile)

From the repo root:

```bash
docker compose up -d
```

### 2. Start the backend

```bash
cd backend
mvn spring-boot:run
# backend on http://localhost:8080
```

Zero-infra alternative (no Docker, in-memory H2 + seeded demo calls 1000–1004):

```bash
cd backend
SPRING_PROFILES_ACTIVE=demo mvn spring-boot:run
```

### 3. Start the frontend (existing Node demo)

From the repo root (unchanged from the existing demo):

```bash
npm install
npm start
# RM view:   http://localhost:3000/app.html
# Softphone: http://localhost:3000/
```

Open **http://localhost:3000/app.html**, enter a customer number + TNB code,
click **Call** (dials via MicroSIP), then **Kết thúc / End call** to run the
pipeline and show the transcript + recording link.

## STT locally (optional)

Uncomment the `whisper` service in the root `docker-compose.yml`, `docker
compose up -d`, and set `STT_BASE_URL=http://localhost:9000`. Any server exposing
the Whisper `POST /v1/audio/transcriptions` contract (`response_format=verbose_json`)
works; the provider is pluggable behind the `SttClient` interface.

## Recordings

The real FreeSWITCH box exposes exactly **two** HTTP endpoints under
`RECORDING_BASE_URL`:

- `GET /api/recordings/latest` — the filename of the most recent recording
  (JSON `{"name":"…"}` or a bare string; both handled).
- `GET /api/recordings/download/:name` — that `.wav`.

There is **one stereo `.wav` per call**, with the two legs on separate channels
(one leg = left, the other = right). On `POST /api/calls/{id}/ended` the backend
reads `latest` and stores the filename as the call's `recordingName` — that
filename **is** the call↔recording link (no UUID resolution, no dialplan key).
`SttService` then splits the stereo file into two mono channels and transcribes
each, labeling one RM and the other KH per `STT_RM_CHANNEL` (`left`|`right`,
default `left`=RM). See `../freeswitch/RECORDING-SETUP.md` to expose those
endpoints over HTTPS via nginx.

## Notes

- Auto-DDL (`ddl-auto=update`) is used for the demo; add migrations (Flyway) for
  production.
- No automated tests ship in this slice (hackathon decision); the existing Node
  demo is untouched and still works.

## U1 — Lead & call foundation (CRM epic)

Unit **u1-lead-call-foundation** adds the CRM lead/call surface on top of the
recording→STT slice above. Existing endpoints (`/api/calls/initiate`,
`sttStatus`) are preserved.

New REST (contract C1):

| Endpoint | Mục đích |
|---|---|
| `GET /api/me` | RM hiện tại (chế độ demo trả RM mẫu) |
| `GET /api/leads/demo` | Lead mẫu để mở thẳng S1 (chỉ profile `demo`) |
| `GET /api/leads/{leadId}` | Chi tiết lead (403 khác RM, 404 không tồn tại) |
| `GET /api/leads/{leadId}/activities` | Lịch sử hoạt động, mới nhất trước |
| `GET /api/leads/next?excludeLeadId=` | Lead kế tiếp (204 khi hết) |
| `POST /api/calls` `{leadId}` | Tạo cuộc gọi (201 / 400 / 403 / 409) |
| `POST /api/calls/{callId}/ended` | Kết thúc idempotent, chạy pipeline một lần |
| `GET /api/calls/{callId}` | Trạng thái cuộc gọi + pipeline |

Màn hình (phục vụ bởi Node, ADR-002):

- `http://localhost:3000/screens/1-chi-tiet-tiem-nang-ban.html` (hoặc `/lead`) — chi tiết lead + nút **Gọi KH**.
- `http://localhost:3000/screens/2-thuc-hien-cuoc-goi.html?callId=..&leadId=..` — màn thực hiện cuộc gọi + nút **Kết thúc**.

### Chế độ Auth (ADR-007)

- `app.auth.mode=demo` (mặc định, biến môi trường `AUTH_MODE`): luôn coi người
  gọi là một RM mẫu cố định, không cần token. Profile `demo` seed sẵn RM +
  customer + lead + activity.
- `app.auth.mode=login`: RM lấy từ token Bearer; bản hiện thực do U6 cung cấp.
  Khi chọn `login` mà chưa có U6, bean `CurrentRmProvider` sẽ không có — hãy giữ
  `demo` cho tới khi U6 xong.

### Biến môi trường thêm cho U1

```properties
app.auth.mode=${AUTH_MODE:demo}   # demo | login
```

Không hard-code secret; mật khẩu RM (U6) chỉ lưu băm BCrypt, không bao giờ trả
ra API/log (NFR2/BR6.1).

### Chạy test cục bộ (hoãn ở lượt này)

Lượt sinh mã này **chỉ hiện thực mã ứng dụng**; test được hoãn theo yêu cầu
("gen code for all units at once, let the test for later"). `pom.xml` đã sẵn
`spring-boot-starter-test` + JaCoCo (sàn 80% line cho package U1, **không hạ**).
Khi viết test ở lượt sau, chạy qua **IntelliJ → Maven tool window** (Maven không
trên PATH):

```
mvn -f backend/pom.xml -Dtest="com.mb.callassistant.lead.**.*Test,com.mb.callassistant.call.**.*Test,com.mb.callassistant.domain.CallRecordTest,com.mb.callassistant.DemoSeedIntegrationTest" verify
```

Báo cáo coverage: `backend/target/site/jacoco/index.html`.


## U2 — LLM gateway (`com.mb.callassistant.llm`)

Unit **u2-llm-client** là **thư viện cổng LLM dùng chung** (ADR-004, hợp đồng
I3). U3 (Copilot) và U4 (Call Analysis) chỉ phụ thuộc giao diện `LlmClient`;
việc chọn nhà cung cấp, xử lý khóa, timeout và phân loại lỗi gói gọn ở đây.

Hai hiện thực, chọn bằng cấu hình `app.llm.provider`:

- `anthropic` (mặc định) — gọi Anthropic Claude Messages API thật qua
  `WebClient` (không thêm SDK bên thứ ba, NFR1.2).
- `mock` — trả kết quả cài sẵn, **không gọi mạng**, dùng cho demo/test khi chưa
  có khóa (BR6.1). Profile `demo` đã đặt sẵn `mock`.

### Biến môi trường (U2)

```properties
LLM_PROVIDER=anthropic                 # anthropic | mock
ANTHROPIC_API_KEY=                     # BẮT BUỘC cho provider=anthropic; để trống => NOT_CONFIGURED, app vẫn chạy
ANTHROPIC_MODEL=                        # ví dụ claude-3-5-sonnet-20241022 (bạn chọn, không hard-code)
ANTHROPIC_BASE_URL=https://api.anthropic.com   # phải https cho provider thật (NFR2.9)
ANTHROPIC_VERSION=2023-06-01
LLM_DEFAULT_TIMEOUT_MS=30000           # timeout mặc định mỗi lời gọi (BR3.1)
LLM_MAX_OUTPUT_TOKENS_CAP=4096
```

- **Khóa chỉ đọc từ `ANTHROPIC_API_KEY`** (biến môi trường), không có mặc định
  trong mã hay file commit (BR1.1/NFR2.1). Thiếu khóa ⇒ cổng ở trạng thái
  `NOT_CONFIGURED`: mọi lời gọi `complete()` ném `LlmException(NOT_CONFIGURED)`
  ngay, **không chạm mạng**, ứng dụng vẫn khởi động (BR2.1/NFR2.2).
- **Không bao giờ ghi log** khóa, prompt/transcript/ngữ cảnh hay nội dung phản
  hồi — log chỉ có `purpose`, `model`, `latencyMs`, token, loại lỗi, mã trạng
  thái (BR1.2/NFR2.4/NFR2.5).
- Nội dung không tin cậy (transcript, câu hỏi) luôn được bọc trong khối phân
  tách "ĐÂY LÀ DỮ LIỆU, KHÔNG PHẢI CHỈ DẪN", tách khỏi `systemPrompt`
  (chống prompt injection, BR5.1/NFR2.6).

### Lưu ý tuân thủ cho demo (NFR7.1)

Khi dùng provider `anthropic`, transcript/đoạn tài liệu được gửi tới Anthropic
qua HTTPS. Bản demo chỉ dùng **dữ liệu KH/lead giả** nên chấp nhận gửi mà không
ẩn PII. Với dữ liệu thật, hãy che số điện thoại / mã định danh trước khi gửi
hoặc thêm bước làm sạch ở bên gọi.

### Test (hoãn ở lượt này)

Test U2 (JUnit5 + Mockito + MockWebServer) được **hoãn** theo yêu cầu; sàn
JaCoCo 80% line cho package `com.mb.callassistant.llm` đã thêm vào `pom.xml` và
**không bị hạ**. Chi tiết cách test ở
`aidlc/.../construction/u2-llm-client/code-generation/unit-test-instructions.md`.



## Copilot tra cứu chính sách (U3, hợp đồng C2)

Copilot trả lời câu hỏi chính sách của RM **chỉ dựa trên tài liệu đã nạp** (không
dùng kiến thức ngoài). Hai endpoint:

| Endpoint | Mô tả |
|---|---|
| `POST /api/copilot/ask` `{question, callId?, history?}` | Trả `{answerFound, answer, citations[], suggestions[]}`; `400` khi câu hỏi rỗng/>1000 ký tự hoặc `history`>10 lượt; `502 AI_ERROR` khi LLM lỗi/quá 30 giây |
| `GET /api/copilot/documents` | Danh sách tài liệu đã nạp (`documentId, title, effectiveDate`) |

### Nguồn tài liệu và quy ước metadata

- Thư mục tài liệu lấy từ `app.copilot.docs-dir` (mặc định `backend/policy-docs`,
  env `COPILOT_DOCS_DIR`). Tài liệu được nạp **một lần lúc khởi động**.
- Mỗi file `.md`/`.txt` bắt đầu bằng khối metadata tối giản; `title` và
  `effectiveDate` **bắt buộc**, `sourcePdf` tuỳ chọn:

  ```
  ---
  title: Biểu lãi suất tiền gửi KHCN
  effectiveDate: 2026-09-01
  sourcePdf: bieu-lai-suat-2026-09.pdf
  ---
  ```

- Trong thân file, dấu phân trang/mục trên dòng riêng: `[[page:3]]` hoặc một tiêu
  đề Markdown `## Mục 2.1`, để mỗi đoạn giữ được trang/mục cho trích dẫn.
- File thiếu metadata, không đọc được, hoặc rỗng sẽ **bị bỏ qua + ghi log**,
  không chặn các file còn lại. Chỉ mục rỗng ⇒ mọi câu hỏi trả câu "không tìm thấy".
- Hai file mẫu (`bieu-lai-suat-mau.md`, `dieu-kien-vay-mau.md`) là **dữ liệu giả
  chỉ để demo**, không phải số liệu thật.

### Gọi LLM qua U2 `LlmClient`

Copilot gọi mô hình qua gateway dùng chung U2 (`LlmClient`, hợp đồng I3) với
`expectJson=true` và timeout 30 giây; không tự thử lại. Lỗi/quá giờ → `502
AI_ERROR` chỉ hiện trong panel. Ở profile `demo`, `app.llm.provider=mock` nên
Copilot chạy **offline không cần `ANTHROPIC_API_KEY`** (câu trả lời giả lập). Đặt
`LLM_PROVIDER=anthropic` + `ANTHROPIC_API_KEY`/`ANTHROPIC_MODEL` để dùng Claude thật.

### Tham số cấu hình Copilot

```properties
app.copilot.docs-dir=${COPILOT_DOCS_DIR:backend/policy-docs}
app.copilot.top-k=${COPILOT_TOP_K:5}
app.copilot.max-history-turns=${COPILOT_MAX_HISTORY_TURNS:10}
app.copilot.max-question-length=${COPILOT_MAX_QUESTION_LENGTH:1000}
app.copilot.llm-timeout-ms=${COPILOT_LLM_TIMEOUT_MS:30000}
app.copilot.max-output-tokens=${COPILOT_MAX_OUTPUT_TOKENS:1024}
```

> Kiểm thử của U3 **được dời sang đợt sau** theo yêu cầu người dùng; sàn độ phủ
> JaCoCo 80% cho package `com.mb.callassistant.copilot` **không bị hạ**, chỉ được
> kiểm chứng ở đợt test sau (xem `unit-test-instructions.md`).



## Công việc & Lịch làm việc (U5, hợp đồng C4)

Unit **u5-task-calendar** thêm thực thể `Task` (công việc/tác vụ CRM) cùng màn
lịch tuần S5 và hộp thoại tạo/đổi giờ S6. Quyền sở hữu dựa lại U1
(`CurrentRmProvider`, `LeadService.assertOwned`).

REST (contract C4):

| Endpoint | Mục đích |
|---|---|
| `GET /api/tasks?from&to&type?&sourceCallId?` | Công việc của RM trong khoảng (≤ 42 ngày); `type` lọc loại (KHAC không lọt khi lọc loại cụ thể); `sourceCallId` cho khối "AI Key-in từ cuộc gọi". `400` khi khoảng sai/quá 42 ngày |
| `POST /api/tasks` | Tạo công việc thủ công (201); `400` thiếu tiêu đề/dueAt; `403` lead khác RM |
| `PATCH /api/tasks/{taskId}` | `status=DONE` (một chiều, idempotent) và/hoặc `dueAt`+`confirmPast`; `400` `PAST_NOT_CONFIRMED` khi đổi sang quá khứ chưa xác nhận; `403`/`404` theo quyền sở hữu |

Giao diện Java I4 cho U4: `TaskService.createFromCall(rmId, leadId, callId,
callAiSummary, TaskDraft[])` — idempotent theo `(callId, clientKey)`, trả
`List<String> taskId[]`.

Màn hình (phục vụ bởi Node):

- `http://localhost:3000/screens/5-lich-lam-viec.html` (hoặc `/calendar`) — lịch
  tuần + hộp thoại tạo/đổi giờ. Highlight "Vừa lưu từ cuộc gọi" qua
  `?sourceCallId=<callId>`.

Mọi thời gian theo ISO-8601 offset `+07:00`, ranh giới ngày/khung giờ
08:00–17:00 theo `Asia/Ho_Chi_Minh` (BR6.1). Profile `demo` seed vài công việc
tuần hiện tại, gồm một việc `KHAC` không gắn lead để minh hoạ bộ lọc "Tất cả".

> Kiểm thử U5 **được dời sang đợt sau** theo yêu cầu người dùng; sàn độ phủ
> JaCoCo 80% cho package `com.mb.callassistant.task` **không bị hạ**, chỉ được
> kiểm chứng ở đợt test sau (xem `unit-test-instructions.md`).



## U6 — Đăng nhập thật & danh sách lead (`com.mb.callassistant.auth`)

Unit **u6-login-lead-list** thêm chế độ đăng nhập thật (`app.auth.mode=login`)
và danh sách lead của RM hiện tại. Chế độ `demo` vẫn là **mặc định** — U6 chỉ
cung cấp bản hiện thực `login` của `CurrentRmProvider`; đổi chế độ chỉ qua cấu
hình, không sửa unit khác (ADR-007, BR3.1).

REST (hợp đồng C5):

| Endpoint | Mục đích |
|---|---|
| `POST /api/auth/login` `{username, password}` | Đăng nhập; đúng → `200 {token, expiresAt, rm}`; sai → `401` "Sai tên đăng nhập hoặc mật khẩu" (một thông điệp duy nhất); thiếu trường → `400` |
| `POST /api/auth/logout` | Thu hồi phiên của token hiện tại (`Authorization: Bearer`), trả `204` (idempotent) |
| `GET /api/leads` | Danh sách lead của RM hiện tại, sắp theo ưu tiên rồi hạn SLA; rỗng → `[]` |

Màn hình (phục vụ bởi Node):

- `http://localhost:3000/screens/0-dang-nhap.html` (hoặc `/login`) — màn đăng nhập S0.
- `http://localhost:3000/screens/leads-list.html` (hoặc `/leads`) — danh sách lead.

### Bật chế độ login

```properties
app.auth.mode=login        # env AUTH_MODE=login
app.auth.session-ttl-hours=8   # env AUTH_SESSION_TTL_HOURS (BR2.1)
app.auth.fail-delay-ms=0       # env AUTH_FAIL_DELAY_MS (tùy chọn, BR5.2)
```

Khi `app.auth.mode=login`, `LoginRmSeeder` seed hai tài khoản RM mẫu
(`rm.a`, `rm.b`) nếu chưa có. **Mật khẩu seed là giá trị rõ-ràng-là-demo**,
mặc định `demo1234`, ghi đè qua biến môi trường `AUTH_DEMO_PASSWORD` — không
bao giờ hard-code bí mật thật và không bao giờ log mật khẩu/token (BR5.1).

### Luồng xác thực

Mỗi request sau đăng nhập mang header `Authorization: Bearer <token>`.
`BearerTokenFilter` đặt token vào ngữ cảnh request để `LoginCurrentRmProvider`
tra phiên; token thiếu/sai/hết hạn/đã logout → `401 UNAUTHORIZED`, giao diện
xóa token và quay về S0 (AC4.1.3, BR2.2). Token Bearer ≥ 32 byte sinh bằng
`SecureRandom`; server chỉ lưu băm SHA-256, hạn 8 giờ cấu hình được (BR2.1).

### Lưu ý bảo mật (không khóa toàn bộ endpoint)

U6 dùng `spring-security-crypto` **chỉ để băm BCrypt** (không thêm
`spring-boot-starter-security`), nên chế độ `demo` và các endpoint khác không
bị khóa. Mật khẩu chỉ lưu băm BCrypt; token chỉ lưu băm SHA-256 (BR5.1).

> Kiểm thử U6 **được dời sang đợt sau** theo yêu cầu người dùng; sàn độ phủ
> JaCoCo 80% cho package `com.mb.callassistant.auth` **không bị hạ**, chỉ được
> kiểm chứng ở đợt test sau (xem `unit-test-instructions.md`).



## U4 — Phân tích & kết quả cuộc gọi (`com.mb.callassistant.analysis`)

Unit **u4-call-analysis-result** cắm bước `ANALYZE` vào pipeline của U1 (I2):
sau STT, `CallPipelineService` chạy các `CallPipelineStep` đã đăng ký;
`AnalyzeCallPipelineStep` ủy quyền `CallAnalysisService` gọi `LlmClient`
(purpose `CALL_ANALYSIS`, I3) để chấm điểm/tóm tắt. Server **tự tính**
`totalScore = round(tổng 5 tiêu chí × 2)` và hạng A/B/C/D theo ngưỡng cấu hình —
KHÔNG dùng tổng do LLM trả (BR3.1/BR3.2, ADR-006). Transcript nhúng như **dữ
liệu phân tách** (BR8.1); log mỗi bước, không log khóa/prompt (BR8.2). U4 không
ghi `pipelineStatus` trực tiếp — mọi cập nhật đi qua `CallService.updatePipeline`
(BR1.2).

REST (hợp đồng C3):

| Endpoint | Mục đích |
|---|---|
| `GET /api/calls/{callId}/analysis` | Kết quả phân tích (`criteria`, điểm, hạng, tone, insight, `suggestedKeyin`, `suggestedTasks`); `404` khi chưa có; `403` khác RM |
| `POST /api/calls/{callId}/analysis/retry` | Chạy lại AI; `202`; `409` khi chưa có transcript / đang chạy (BR1.4) |
| `GET /api/calls/{callId}/keyin` | Keyin đã lưu + đánh giá AI; `404` khi chưa lưu; `403` khác RM |
| `PUT /api/calls/{callId}/keyin` | Lưu một-giao-dịch (keyin + feedback + updateStage + recordCallActivity + createFromCall), idempotent theo callId; `200 {callId, interactionCode, createdTaskIds}`; `400` thiếu `leadStage`/`callResult`; `403` khác RM |

### Cấu hình `app.analysis.*`

```properties
app.analysis.grade-threshold-a=85   # ANALYSIS_GRADE_A (BR3.3)
app.analysis.grade-threshold-b=70   # ANALYSIS_GRADE_B
app.analysis.grade-threshold-c=50   # ANALYSIS_GRADE_C (dưới ngưỡng này là D)
app.analysis.llm-timeout-ms=30000   # ANALYSIS_LLM_TIMEOUT_MS (BR8.2/NFR1)
app.analysis.max-output-tokens=2048 # ANALYSIS_MAX_OUTPUT_TOKENS
```

### Demo offline end-to-end (profile `demo`, provider `mock`)

`MockLlmClient` trả JSON hợp lệ cho CẢ `COPILOT` (trích dẫn tài liệu seed trong
`backend/policy-docs`) lẫn `CALL_ANALYSIS` (5 tiêu chí → điểm 96, hạng A), nên
không cần `ANTHROPIC_API_KEY`. Luồng demo: S1 → gọi KH → S2 → kết thúc → S3
(pipeline chạy ANALYZE với MOCK, cột điểm/transcript/keyin tự điền) → lưu hồ sơ
→ hộp thoại điều hướng S4; lịch sử hoạt động ở S1 hiện tóm tắt AI + liên kết
"Xem kết quả cuộc gọi".

> Kiểm thử U4 **được dời sang đợt sau** theo yêu cầu người dùng; sàn độ phủ
> JaCoCo 80% cho package `com.mb.callassistant.analysis` **không bị hạ**, chỉ
> được kiểm chứng ở đợt test sau (xem `unit-test-instructions.md`).

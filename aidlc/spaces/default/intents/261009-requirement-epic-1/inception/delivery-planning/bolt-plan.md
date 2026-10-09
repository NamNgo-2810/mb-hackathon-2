# Bolt Plan

**Bolt** là một lượt xây hoàn chỉnh cho một phần công việc: qua thiết kế, viết mã và kiểm thử, kết thúc bằng một thứ chạy được. Mỗi Bolt ở đây gồm đúng **một unit** [Q2=A]. Unit lấy từ `unit-of-work.md`, thứ tự phải tuân theo `unit-of-work-dependency.md`.

Các quyết định đã chốt (`delivery-planning-questions.md`):
- **Thứ tự**: theo luồng demo [Q1=B].
- **Cách làm**: theo từng bước. Thiết kế tất cả unit trước, sau đó mới viết mã [Q3=B, Q5=A].
- **Người thực hiện**: làm trong một phiên. Chạy tuần tự, không làm song song.
- **Không có walking skeleton** (`team.md`). Bolt 1 là một Bolt bình thường.
- **Commit**: mỗi Bolt xong thì commit thẳng lên `main`. Trước lần commit đầu tiên phải chạy `git init`.
- **Lệnh kiểm tra**: hoãn lại. Người dùng sẽ tự chạy test backend bằng Maven wrapper trong IntelliJ. Code Generation **không** chạy lệnh Java.

| # | Bolt | Unit | Story | Demo kỳ vọng |
|---|---|---|---|---|
| 1 | Lead & nền tảng cuộc gọi | U1 `u1-lead-call-foundation` | US4.3, US4.4, US1.1 | Mở S1 của lead mẫu, bấm Gọi KH, MicroSIP gọi đi, S2 hiện đồng hồ đếm giờ, bấm Kết thúc thì trạng thái cuộc gọi chuyển sang đang xử lý |
| 2 | Cổng LLM | U2 `u2-llm-client` | (dùng chung) | Test với bản giả lập chạy qua; nếu đã có khóa API thì gửi một prompt thử tới Claude và nhận được trả lời |
| 3 | Copilot | U3 `u3-copilot` | US1.2, US1.3 | Trên S2, hỏi về lãi suất, nhận câu trả lời kèm nguồn và ngày hiệu lực; hỏi câu ngoài tài liệu thì nhận "không tìm thấy" |
| 4 | Công việc & Lịch | U5 `u5-task-calendar` | US3.2, US3.3, US3.4 | S5 hiển thị lịch tuần với dữ liệu mẫu; đánh dấu xong, đổi giờ, tạo công việc mới qua S6 |
| 5 | Phân tích & Kết quả cuộc gọi | U4 `u4-call-analysis-result` | US2.1–US2.4, US3.1 | Kết thúc cuộc gọi thì S3 hiện tiến trình, transcript, điểm và form đã điền sẵn; lưu xong hiện S4, chuyển sang S5 thấy công việc vừa tạo; tóm tắt AI xuất hiện trong lịch sử ở S1 |
| 6 | Đăng nhập & danh sách lead | U6 `u6-login-lead-list` | US4.1, US4.2 | Đăng nhập trên S0, mỗi RM chỉ thấy lead của mình; gọi API lead của RM khác thì bị trả 403 |

Thứ tự trên hợp lệ theo sơ đồ phụ thuộc: U3 cần U1 và U2; U5 cần U1; U4 cần U1, U2 và U5; U6 cần U1.

## Chi tiết từng Bolt

### Bolt 1 — U1 `u1-lead-call-foundation`
- **Definition of Done**:
  - Có dữ liệu mẫu (RM, KH, lead, hoạt động, công việc không gắn lead).
  - Chạy được các API ở C1: `/api/me`, `/api/leads/demo`, `/api/leads/{id}`, `/api/leads/{id}/activities`, `/api/leads/next`, `POST /api/calls`, `/ended` idempotent, `GET /api/calls/{id}` có `pipelineStatus`.
  - Có điểm mở rộng `CallPipelineStep` (I2).
  - Màn S1 và khung S2 dùng bộ token và sidebar thống nhất.
  - Kiểm tra quyền trả 403.
  - Các API cũ `/api/calls/*` vẫn chạy.
  - Test đạt độ phủ ≥ 80%, do người dùng tự chạy.
- **Giả thuyết cần chứng minh**: mô hình module trong Spring Boot cũ cùng chế độ demo đủ để dựng luồng lead → gọi → kết thúc mà không làm hỏng pipeline ghi âm/STT hiện có.

### Bolt 2 — U2 `u2-llm-client`
- **Definition of Done**:
  - Có giao diện `LlmClient` (I3), bản giả lập dùng cho test, và adapter Anthropic đọc khóa từ biến môi trường.
  - Mọi lời gọi có timeout, phân loại được lỗi; khi chưa có khóa thì báo `NOT_CONFIGURED`.
  - Test đạt độ phủ ≥ 80%.
- **Giả thuyết cần chứng minh**: một cổng LLM duy nhất, có bản giả lập, đủ để U3 và U4 phát triển và test mà chưa cần khóa API thật.

### Bolt 3 — U3 `u3-copilot`
- **Definition of Done**:
  - Nạp được file text/md trong thư mục tài liệu.
  - Tìm kiếm theo từ khóa, có hỗ trợ tiếng Việt không dấu.
  - `POST /api/copilot/ask` và `GET /api/copilot/documents` (C2) chạy được.
  - Panel trên S2 có câu gợi ý, lịch sử trong phiên, nút "cuộc trò chuyện mới".
  - Trả "không tìm thấy" mà không gọi LLM khi tài liệu không có nội dung liên quan.
  - Test đạt độ phủ ≥ 80%.
- **Giả thuyết cần chứng minh**: tìm theo từ khóa trên tài liệu text/md là đủ để trả lời đúng và trích dẫn đúng cho bộ tài liệu demo.

### Bolt 4 — U5 `u5-task-calendar`
- **Definition of Done**:
  - API C4 chạy được: lọc theo khoảng thời gian và loại, tạo mới, đánh dấu xong, đổi giờ có xác nhận khi là thời điểm đã qua.
  - `TaskService.createFromCall` idempotent (I4).
  - Màn S5 (tuần, ngày, tháng, danh sách) và S6.
  - Test đạt độ phủ ≥ 80%.
- **Giả thuyết cần chứng minh**: lịch làm việc hiển thị đúng công việc của RM, cả công việc thường lẫn công việc do keyin tạo ra.

### Bolt 5 — U4 `u4-call-analysis-result`
- **Definition of Done**:
  - Pipeline: ghi âm → STT (phân biệt được trường hợp không có ghi âm và STT lỗi) → bước AI qua `CallPipelineStep`.
  - CallAnalysis kiểm tra JSON trả về và tự tính điểm.
  - API C3: xem kết quả phân tích, chạy lại, lưu keyin (`PUT /keyin`) trong một giao dịch.
  - Màn S3 có đủ 3 cột và các trạng thái dự phòng; màn S4; khối tóm tắt AI ở S1.
  - Test đạt độ phủ ≥ 80%, gọi STT và LLM qua bản giả lập.
- **Giả thuyết cần chứng minh**: với transcript tiếng Việt thật, Claude cho ra tóm tắt, điểm và keyin đủ dùng để RM chỉ cần rà soát lại, không phải gõ từ đầu. Đây là rủi ro lớn nhất của epic.

### Bolt 6 — U6 `u6-login-lead-list`
- **Definition of Done**:
  - `POST /api/auth/login` và `/logout` chạy được. Token Bearer chỉ lưu ở dạng băm. Mật khẩu băm bằng BCrypt.
  - `CurrentRmProvider` có bản cho chế độ `login`, bật bằng cấu hình.
  - `GET /api/leads` chỉ trả lead của RM đang đăng nhập.
  - Có màn S0 và cột danh sách lead ở S1.
  - Test đạt độ phủ ≥ 80%.
- **Giả thuyết cần chứng minh**: đổi từ `demo` sang `login` không phải sửa các unit khác (ADR-007).

## Sources

- `unit-of-work.md`, `unit-of-work-dependency.md`, `unit-of-work-story-map.md`
- `contract-summary.md`, `components.md`, `requirements.md`, `stories.md`, `mockups.md`
- `team-practices.md`
- `delivery-planning-questions.md`

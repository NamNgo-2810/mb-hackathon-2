# Units of Work

Đầu vào:
- `components.md` (11 thành phần)
- `decisions.md` (ADR-001…007)
- `requirements.md`
- `stories.md` (US1.1–US4.4)

Cách chia: **theo tính năng (lát cắt dọc)**. Mỗi unit gồm cả phần backend và các màn hình mà nó sở hữu (`units-generation-questions.md`: Q1, Q2, Q3=B; người dùng đã duyệt kế hoạch). Không có walking skeleton (`team.md`).

Mô hình triển khai đã chốt ở ADR-001 và ADR-002:
- Mọi phần backend nằm chung **một ứng dụng Spring Boot**, mỗi unit là một hoặc vài package.
- Phần giao diện và MicroSIP chạy trên **server Node** hiện có.

Vì vậy "triển khai" của từng unit đều ở dạng *nhúng* (embedded) vào một trong hai tiến trình này.

## Danh sách unit

| Unit ID | Directory | Tên | Kind | Độ lớn | Triển khai |
|---|---|---|---|---|---|
| U1 | `u1-lead-call-foundation` | Lead & nền tảng cuộc gọi | service | M | Nhúng vào Spring Boot + trang S1/S2 trên Node |
| U2 | `u2-llm-client` | Cổng LLM | library | S | Thư viện nội bộ trong Spring Boot |
| U3 | `u3-copilot` | Copilot tra cứu chính sách | service | M | Nhúng vào Spring Boot + panel trên S2 |
| U4 | `u4-call-analysis-result` | Phân tích & Kết quả cuộc gọi | service | L | Nhúng vào Spring Boot + trang S3/S4 |
| U5 | `u5-task-calendar` | Công việc & Lịch làm việc | service | M | Nhúng vào Spring Boot + trang S5/S6 |
| U6 | `u6-login-lead-list` | Đăng nhập & danh sách lead | service | S | Nhúng vào Spring Boot + trang S0 + cột danh sách ở S1 |

## U1 — `u1-lead-call-foundation`

- **Mô tả**: phần nền mà các unit khác cần.
  - Có dữ liệu mẫu và chế độ demo (luôn trả về RM mẫu, ADR-007).
  - Xem chi tiết lead và lịch sử hoạt động.
  - Bắt đầu và kết thúc cuộc gọi.
  - Ứng dụng mở thẳng S1 cho lead mẫu.
- **Thành phần**:
  - AuthComponent (chế độ `demo`)
  - LeadComponent: Customer, Lead, Activity (đọc/ghi)
  - CallComponent: tạo cuộc gọi, kết thúc idempotent, phục vụ trạng thái
  - MicrosipBridge (Node, có sẵn): nối nút Gọi KH và sự kiện kết thúc
  - WebFrontend: S1 (chi tiết lead, Quản lý hoạt động), khung S2 (thông tin KH, đồng hồ, Kết thúc)
- **Trách nhiệm**:
  - Kiểm tra lead thuộc RM ở phía server (FR1.4).
  - Chọn "lead kế tiếp" (FR4.9). Phần này phục vụ U4.
  - Thống nhất sidebar và bộ token giao diện (`design-system-mapping.md`).
- **Ghi chú**:
  - Mở rộng `CallRecord` hiện có thêm `leadId`, `rmId`, `pipelineStatus/Step` và giữ nguyên các API cũ.
  - Seed dữ liệu theo nội dung mockup.

## U2 — `u2-llm-client`

- **Mô tả**: cổng gọi Anthropic Claude dùng chung cho các unit khác (ADR-004).
- **Thành phần**: LlmClient.
- **Trách nhiệm**:
  - Đọc khóa API và model từ biến môi trường, không bao giờ ghi khóa ra log.
  - Mọi lời gọi đều có timeout và bật prompt caching.
  - Phân loại lỗi: mạng / timeout / quá tải / nội dung.
  - Có bản giả lập để dùng khi test.
- **Ghi chú**: chưa có khóa API, người dùng sẽ cung cấp sau. Bản giả lập cho phép U3 và U4 phát triển và test trước khi có khóa.

## U3 — `u3-copilot`

- **Mô tả**: Copilot hỏi đáp chính sách trong lúc gọi (US-01).
- **Thành phần**:
  - CopilotComponent: PolicyDocument, CopilotQueryLog
  - WebFrontend: panel Copilot trên S2
- **Trách nhiệm**:
  - Nạp các file text/md trong thư mục tài liệu.
  - Tìm theo từ khóa (cả có dấu và không dấu).
  - Nhúng đoạn tìm được vào prompt dưới dạng dữ liệu.
  - Chỉ trả lời từ tài liệu, kèm nguồn, trang và ngày hiệu lực; trả "không tìm thấy" khi không có.
  - Đưa ra câu hỏi gợi ý và lịch sử tra cứu trong phiên.
- **Ghi chú**: chưa chốt nguồn và cách tạo file text/md (Domain Design R-03, để sau). Unit này chỉ yêu cầu có thư mục chứa file text/md, mỗi file có metadata tên tài liệu và ngày hiệu lực.

## U4 — `u4-call-analysis-result`

- **Mô tả**: luồng sau cuộc gọi và màn "Kết quả cuộc gọi" (US-02), kèm tóm tắt AI trong lịch sử (US3.1).
- **Thành phần**:
  - TranscriptionComponent: mã có sẵn, chỉ chỉnh để tách lỗi "chưa có ghi âm" và "lỗi STT"
  - CallComponent: thêm bước AI vào pipeline, các trạng thái dự phòng, chạy lại AI
  - CallAnalysisComponent
  - KeyinComponent: CallKeyin, AiFeedback
  - WebFrontend: S3, S4, khối "Tóm tắt AI" trong Quản lý hoạt động ở S1
- **Trách nhiệm**:
  - Pipeline: ghi âm → STT → Claude → lưu kết quả.
  - Chấm điểm: tổng = round(tổng 5 tiêu chí × 2), xếp hạng theo ngưỡng cấu hình.
  - Auto keyin, kèm mốc thời gian trích xuất cho từng trường.
  - Lưu keyin idempotent trong một giao dịch: cập nhật giai đoạn lead, ghi hoạt động (Lead), tạo công việc (Task).
  - Ghi nhận đánh giá AI của RM.
  - Khi chạy lại AI, chỉ điền vào các trường còn trống.
- **Ghi chú**: giới hạn một cuộc gọi tại một thời điểm, chấp nhận cho demo (Domain Design R-01).

## U5 — `u5-task-calendar`

- **Mô tả**: công việc và lịch làm việc của RM (phần bổ sung của US-03).
- **Thành phần**:
  - TaskComponent
  - WebFrontend: S5, S6
- **Trách nhiệm**:
  - API tạo công việc, dùng cho U4 và cho thao tác tạo thủ công.
  - Truy vấn theo khoảng thời gian và loại.
  - Đánh dấu xong, đổi giờ.
  - Các chế độ xem tuần / ngày / tháng / danh sách.
  - Dữ liệu mẫu cho các việc không gắn lead.
- **Ghi chú**: ẩn các nút Outlook/Google và Zalo ZNS (giả định A4).

## U6 — `u6-login-lead-list`

- **Mô tả**: đăng nhập thật và danh sách lead theo RM (US4.1, US4.2). Làm sau cùng [User Stories Q1].
- **Thành phần**:
  - AuthComponent (chế độ `login`): RmUser, mật khẩu băm BCrypt, phiên dùng được qua CORS
  - LeadComponent: danh sách lead theo RM
  - WebFrontend: S0, cột danh sách ở S1
- **Ghi chú**: chuyển từ chế độ `demo` sang `login` bằng cấu hình, không cần sửa các unit khác (ADR-007).

## Sources

- `components.md`, `decisions.md`, `requirements.md`, `stories.md`
- `units-generation-questions.md`
- `mockups.md` và `design-system-mapping.md` (Refined Mockups) cho phạm vi màn hình

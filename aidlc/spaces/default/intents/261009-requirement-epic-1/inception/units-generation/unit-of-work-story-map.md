# Story → Unit Map

Mỗi dòng ứng với một story trong `stories.md`. Cột **Unit** là unit triển khai chính. Cột **Cũng chạm** liệt kê các unit khác mà story đó cần (xem `unit-of-work.md` và `unit-of-work-dependency.md`). Nguồn yêu cầu: `requirements.md`.

| Story | Unit | Directory | Cũng chạm | Ghi chú |
|---|---|---|---|---|
| US1.1 | U1 | `u1-lead-call-foundation` | U4 (pipeline chạy sau khi kết thúc cuộc gọi) | Khung S2, đồng hồ, Kết thúc idempotent |
| US1.2 | U3 | `u3-copilot` | U2 (`u2-llm-client`), U1 (khung S2) | Trả lời có nguồn và ngày hiệu lực |
| US1.3 | U3 | `u3-copilot` | — | Gợi ý câu hỏi, lịch sử tra cứu |
| US2.1 | U4 | `u4-call-analysis-result` | U2 (`u2-llm-client`), U1 (trạng thái cuộc gọi) | Pipeline, STT, S3 cột 2, tóm tắt |
| US2.2 | U4 | `u4-call-analysis-result` | U2 (`u2-llm-client`) | Chấm điểm, S3 cột 1 |
| US2.3 | U4 | `u4-call-analysis-result` | U2 (`u2-llm-client`), U5 (tạo công việc), U1 (giai đoạn lead, hoạt động) | Auto keyin, đánh giá AI, lưu |
| US2.4 | U4 | `u4-call-analysis-result` | U1 (lead kế tiếp), U5 (mở S5) | Dự phòng khi lỗi, S4 |
| US3.1 | U4 | `u4-call-analysis-result` | U1 (S1, Activity) | Tóm tắt AI trong lịch sử |
| US3.2 | U5 | `u5-task-calendar` | U4 (công việc "vừa lưu từ cuộc gọi") | Lịch tuần, S5 |
| US3.3 | U5 | `u5-task-calendar` | U1 (Gọi ngay, Chi tiết TNB) | Thao tác công việc, S6 |
| US3.4 | U5 | `u5-task-calendar` | — | Ngày / Tháng / Danh sách |
| US4.1 | U6 | `u6-login-lead-list` | U1 (chế độ Auth) | S0, chế độ `login` |
| US4.2 | U6 | `u6-login-lead-list` | U1 (LeadComponent) | Danh sách lead theo RM |
| US4.3 | U1 | `u1-lead-call-foundation` | — | Chi tiết lead S1 |
| US4.4 | U1 | `u1-lead-call-foundation` | — | Gọi KH từ chi tiết lead |

## Phần dùng chung giữa nhiều unit

- **U2 `u2-llm-client`** không có story riêng. Unit này phục vụ US1.2 (U3) và US2.1, US2.2, US2.3 (U4).
- **Kiểm tra quyền theo lead (FR1.4)**: được làm ở U1 và dùng lại ở mọi unit có đọc hoặc ghi dữ liệu lead.
- **Bộ token giao diện và sidebar**: được dựng ở U1, các unit có giao diện khác (U3–U6) dùng lại.

## Thứ tự làm bên trong mỗi unit

- **U1**: dữ liệu mẫu và chế độ demo → API Lead/Activity → tạo/kết thúc cuộc gọi → S1 → khung S2.
- **U2**: giao diện `LlmClient` → bản giả lập → adapter Anthropic.
- **U3**: nạp tài liệu và tìm kiếm → dịch vụ hỏi đáp → panel S2.
- **U4**: tách lỗi trong Transcription → CallAnalysis (prompt, kiểm tra kết quả, tính điểm) → bước AI trong pipeline → Keyin → S3 → S4 → khối tóm tắt ở S1.
- **U5**: API Task → S5 tuần → S6 → các chế độ xem khác.
- **U6**: RmUser và đăng nhập → S0 → danh sách lead ở S1.

## Kiểm tra độ phủ

- 15/15 story đều đã được gán unit chính.
- Unit nào cũng xuất hiện ít nhất một lần: U1, U3, U4, U5, U6 có story chính; U2 có mặt ở cột "Cũng chạm" của US1.2, US2.1, US2.2, US2.3.

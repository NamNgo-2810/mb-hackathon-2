# Domain Design — Câu hỏi

Đầu vào: `requirements.md`, `stories.md`, bản phân tích mã nguồn (`architecture.md`, `component-inventory.md`) và `team-practices.md`.

Hiện trạng: dự án đang có một backend Spring Boot (cuộc gọi, ghi âm, STT) và một server Node (phục vụ trang web, điều khiển MicroSIP, phát sự kiện cuộc gọi qua SSE). Phần cần thêm gồm: lead và keyin, phân tích AI, Copilot, công việc/lịch và đăng nhập.

## Q1. Phần logic mới đặt ở đâu?

A. Mở rộng ngay trong backend Spring Boot hiện có, chia thành các module rõ ràng (Lead, Call, Analysis AI, Keyin, Copilot, Task, Auth), dùng chung một CSDL. Đây là đề xuất của mình: chạy local đơn giản, tận dụng được mã sẵn có.
B. Tách phần AI (Analysis và Copilot) ra một dịch vụ riêng, phần còn lại vẫn ở Spring Boot.
X. Other (please specify)

[Answer]:A

## Q2. Vai trò của server Node

A. Giữ nguyên Node: phục vụ các trang HTML và điều khiển MicroSIP/SSE. Trình duyệt gọi thẳng API Spring Boot (CORS đã cấu hình sẵn).
B. Spring Boot phục vụ luôn các trang HTML và API. Node chỉ còn lo điều khiển MicroSIP/SSE.
X. Other (please specify)

[Answer]: A

## Q3. Copilot tìm nội dung trong PDF chính sách bằng cách nào?

Anthropic không có API tạo embedding (vector). Muốn tìm theo vector thì phải dùng thêm một nhà cung cấp khác.

A. Đưa toàn bộ nội dung đã trích từ PDF vào prompt của Claude, kèm prompt caching. Cách này phù hợp nếu tổng số tài liệu ít (khoảng dưới 100 trang). Đơn giản và chính xác nhất cho bản demo.
B. Chia PDF thành các đoạn nhỏ, tìm đoạn liên quan bằng tìm kiếm toàn văn (full-text) trong CSDL, rồi chỉ gửi các đoạn đó cho Claude. Cách này phù hợp khi tài liệu nhiều.
C. Tìm theo vector, dùng thêm dịch vụ embedding (ví dụ Voyage AI) và pgvector.
X. Other (please specify)

[Answer]: Extract nội dung ra file text/md rồi tìm kiếm đơn giản, nhúng vào prompt gửi Claude

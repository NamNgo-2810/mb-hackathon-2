# Refined Mockups — Câu hỏi làm rõ

Đầu vào: 5 mockup HTML ở thư mục gốc dự án (`1-chi-tiet-tiem-nang-ban.html` … `5-lich-lam-viec.html`), `stories.md` và `requirements.md`. Vì phạm vi classic không có bước Rough Mockups, 5 mockup này là bản thiết kế gốc. Bước này chỉ tinh chỉnh chúng: bổ sung trạng thái màn hình, tương tác, cách ánh xạ thành phần và tiêu chí truy cập. Mình không vẽ lại từ đầu.

## Q1. Công nghệ dựng giao diện từ mockup

Mockup dùng HTML + Tailwind CSS (tải từ CDN) + icon Font Awesome / Material Symbols / Phosphor. Giao diện hiện có (`public/js/`) là JavaScript thuần chia module (ES module), không cần bước build.

A. Giữ đúng hướng mockup: các trang HTML tĩnh dùng Tailwind qua CDN, JavaScript thuần chia module (giống `public/js/`), không có bước build. Gom về một bộ icon duy nhất (Font Awesome).
B. Như A, nhưng biên dịch Tailwind cục bộ (Tailwind CLI) thay vì tải từ CDN, để chạy được khi không có mạng ngoài.
C. Dựng lại bằng một framework (React + Vite) với các component dùng lại được.
X. Other (please specify)

[Answer]:A

## Q2. Các màn chưa có mockup

Story US4.1 (Đăng nhập) và US4.2 (Danh sách lead) chưa có mockup. Hai story này ưu tiên thấp nhất và làm sau cùng. Màn "Trang chủ" trong hộp thoại sau khi lưu cũng chưa có mockup.

A. Mình tự thiết kế bản tối giản theo đúng phong cách các mockup sẵn có (màu MB, sidebar trái, font Inter). "Trang chủ" dẫn về danh sách tiềm năng bán.
B. Bạn sẽ cung cấp thêm mockup cho các màn này sau
X. Other (please specify)

[Answer]:A. Cần theo system design của file html

## Q3. Khổ màn hình hỗ trợ

Mockup được dựng cố định ở khổ 1440×1024 px.

A. Chỉ hỗ trợ đúng khổ desktop 1440 px như mockup (đủ cho demo)
B. Co giãn trong dải desktop 1280–1920 px; các cột giữ đúng tỉ lệ như mockup
X. Other (please specify)

[Answer]:B

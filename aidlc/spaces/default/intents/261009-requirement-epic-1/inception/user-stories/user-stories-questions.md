# User Stories — Kế hoạch & câu hỏi

## Kế hoạch đề xuất

- **Định dạng story**: "Là [persona], tôi muốn [mục tiêu], để [lợi ích]". Mỗi story tuân thủ tiêu chí INVEST. Tiêu chí nghiệm thu viết theo dạng Given/When/Then.
- **Mã định danh**: `US{nhóm}.{số thứ tự}`, ví dụ US1.1; tiêu chí nghiệm thu dùng `AC{nhóm}.{story}.{số}`. Nhóm 1 = US-01 Copilot, nhóm 2 = US-02 Kết quả cuộc gọi, nhóm 3 = US-03 Lịch sử và Lịch làm việc. Thứ tự này theo cách đánh số mới trong `requirements.md`.
- **Ưu tiên**: phân loại MoSCoW, lấy theo mức ưu tiên Must/Should/Could đã ghi trong `requirements.md`.
- **Cách chia**: chia theo 3 US của epic. Bên trong mỗi US, chia tiếp theo bước công việc hoặc theo cột màn hình.

## Q1. Đặt các story nền tảng (đăng nhập, danh sách/chi tiết lead, gọi KH từ lead — FR1, FR2) ở đâu?

Bạn đã chốt giữ 3 US. Tuy nhiên, phần nền tảng này là điều kiện để cả 3 US chạy được.

A. Tách thành nhóm **US0 — Nền tảng RM** (US0.1, US0.2…), đứng trước 3 US nghiệp vụ. 3 US của epic vẫn giữ nguyên.
B. Gộp vào các US hiện có: đăng nhập, lead và gọi thuộc US-01 (Copilot, vì đó là bước bắt đầu cuộc gọi); chi tiết lead thuộc US-03
X. Other (please specify)

[Answer]:Ưu tiên cuối, làm sau cùng, đừng hỏi lại nữa

## Q2. Persona

Đợt này chỉ có RM là người dùng. Bạn muốn mô tả persona thế nào?

A. Một persona: RM bán lẻ chăm sóc khách hàng cá nhân (như "RM Hoàng Yến" trên mockup)
B. Hai persona: RM mới vào nghề (cần Copilot và gợi ý nhiều) và RM giàu kinh nghiệm (cần keyin nhanh, ít phải sửa)
X. Other (please specify)

[Answer]:RM bán lẻ chăm sóc KHCN

## Q3. Độ chi tiết của story trong US-02 (Kết quả cuộc gọi)

A. Chia nhỏ: mỗi cột hoặc mỗi chức năng là một story (theo dõi trạng thái xử lý, hội thoại, chấm điểm, auto keyin, RM đánh giá AI, lưu hồ sơ, dự phòng khi lỗi, hộp thoại điều hướng), khoảng 8 story
B. Chia vừa: khoảng 4 story (xem kết quả và hội thoại; chấm điểm; auto keyin, đánh giá và lưu; dự phòng khi lỗi kèm điều hướng)
X. Other (please specify)

[Answer]:B

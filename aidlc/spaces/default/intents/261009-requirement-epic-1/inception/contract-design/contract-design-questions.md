# Contract Design — Câu hỏi

Đầu vào:
- `unit-of-work.md` và `unit-of-work-dependency.md` (6 unit, 7 cạnh phụ thuộc)
- `components.md`
- `requirements.md`
- API hiện có (`api-documentation.md`): `/api/calls/*` của Spring Boot và `/api/microsip/*` của Node

Những điểm đã có sẵn, không cần hỏi lại:
- Các unit trong backend giao tiếp với nhau bằng giao diện Java ngay trong cùng tiến trình (ADR-001).
- Giao diện gọi API REST của Spring Boot qua CORS (ADR-002).
- Giao diện hỏi trạng thái pipeline mỗi 3 giây (`interaction-spec.md`).
- Định dạng lỗi giữ nguyên dạng đang dùng `{ "error": { code, message, details?, timestamp } }`.

## Q1. Đường dẫn và đánh phiên bản API

A. Giữ tiền tố `/api/...` như hiện tại, không đánh số phiên bản (chỉ là demo). Chỉ được thêm trường mới; nếu cần thay đổi phá vỡ tương thích thì tạo đường dẫn mới.
B. Đặt tất cả API mới dưới `/api/v1/...`, còn các API cũ giữ nguyên `/api/calls/*`.
X. Other (please specify)

[Answer]: A

## Q2. Cách xác thực khi đăng nhập thật (U6)

Giao diện (Node, cổng 3000) và API (Spring Boot, cổng 8080) khác origin.

A. Đăng nhập trả về token (chuỗi ngẫu nhiên có hạn sử dụng). Giao diện lưu token trong `sessionStorage` và gửi kèm header `Authorization: Bearer <token>`. Cách này đơn giản với CORS.
B. Dùng phiên cookie `HttpOnly` gửi qua CORS có credentials. An toàn hơn trước XSS, nhưng phải cấu hình CORS và SameSite cẩn thận.
X. Other (please specify)

[Answer]: A

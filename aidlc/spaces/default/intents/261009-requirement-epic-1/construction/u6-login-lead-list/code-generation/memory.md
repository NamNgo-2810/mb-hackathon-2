 This file is kept up to date automatically while the stage runs.

## Interpretations
- 2026-10-09T19:03:00+07:00 — `GET /api/leads` đặt ở lớp `web` (LeadListController) tách khỏi `LeadController` của U1; collection path `/leads` không nhập nhằng với `/leads/{leadId}` (Spring ưu tiên path literal), nên không cần đổi U1.
- 2026-10-09T19:03:00+07:00 — Hai `CurrentRmProvider` loại trừ nhau bằng `@ConditionalOnProperty` theo `app.auth.mode` (demo matchIfMissing=true, login havingValue=login) → đúng một bean mỗi chế độ; demo giữ mặc định (ADR-007/BR3.1).

## Deviations
- 2026-10-09T19:03:00+07:00 — `LoginRmSeeder` seed thêm vài lead minh hoạ (bảng U1) khi chạy `login`, idempotent, không đổi hình dạng thực thể; cần vì `DemoDataSeeder` chỉ chạy ở profile `demo`.
- 2026-10-09T19:03:00+07:00 — Toàn bộ bước test được DỜI theo yêu cầu người dùng; không tạo file test, không chạy build/test. Sàn JaCoCo 80% cho package `auth` đã thêm, không hạ.

## Tradeoffs
- 2026-10-09T19:03:00+07:00 — Chỉ thêm `spring-security-crypto` (BCrypt) thay vì `spring-boot-starter-security` để không khóa toàn bộ endpoint; token/phiên tự quản qua `AuthSession` + filter nhẹ.
- 2026-10-09T19:03:00+07:00 — Sắp xếp danh sách lead bằng comparator trong controller (ordinal ưu tiên + slaDueAt null-last) thay vì chỉ dựa truy vấn repo, để thứ tự null tất định độc lập DB (BR4.2).

## Open questions
- 2026-10-09T19:03:00+07:00 — Chưa chạy `mvn verify` (môi trường không build theo yêu cầu); compile-correctness được rà thủ công theo mã U1–U5 hiện có. Đợt test sau sẽ xác minh sàn độ phủ.

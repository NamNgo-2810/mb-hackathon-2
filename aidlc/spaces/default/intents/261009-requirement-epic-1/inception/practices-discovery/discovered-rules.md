# Quy tắc phát hiện

> Nhóm đã chọn **không** nâng bất kỳ ứng viên nào thành quy tắc cứng ở bước
> phỏng vấn (Q6 = C). Các mục dưới `## Mandated` và `## Forbidden` vì vậy để
> trống (`None.`). Các đề xuất kỹ thuật được giữ lại dưới `## Recommendations`
> như hướng dẫn **không ràng buộc**.

## Mandated

None.

## Forbidden

None.

## Recommendations

Những khuyến nghị dưới đây là hướng dẫn tốt, **không bắt buộc**, không chặn
cổng phê duyệt. Nhóm có thể áp dụng tùy mức độ ưu tiên trong hackathon:

- Giữ secrets (API key, mật khẩu, token) **ngoài mã nguồn** bằng biến môi
  trường hoặc trình quản lý bí mật. Hiện có khóa thật đã bị commit trong
  `backend/src/main/resources/application.properties` và `.env`; nên **xoay
  (rotate) các khóa đã lộ** này càng sớm càng tốt.
- Dùng công cụ migration (**Flyway/Liquibase**) để quản lý thay đổi schema thay
  cho Hibernate `ddl-auto`, nhằm tránh lệch schema khi mở rộng.
- **Validate và làm sạch đầu vào ở biên hệ thống**: duy trì các kiểm soát sẵn
  có (Bean Validation ở biên web, allow-list cho tham số, escape HTML ở UI,
  không ghép chuỗi lệnh shell từ đầu vào người dùng) khi thêm endpoint mới.

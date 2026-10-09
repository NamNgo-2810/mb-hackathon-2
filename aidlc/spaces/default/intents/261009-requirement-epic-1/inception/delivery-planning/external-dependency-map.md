# External Dependency Map

**Bolt** là một lượt xây trọn một unit, đi qua thiết kế, viết mã và kiểm thử (`bolt-plan.md`). Bảng dưới liệt kê những thứ nằm ngoài tầm tay của AI và có thể làm chậm các Bolt.

| Phụ thuộc | Ai lo | Khi nào cần | Bolt bị chặn | Nếu chậm thì sao |
|---|---|---|---|---|
| Khóa API Anthropic và tên model Claude | Người dùng | Trước khi demo thật Bolt 3 và 5 | Bolt 3, 5 (chỉ phần chạy thật) | Dùng bản giả lập của U2. Giao diện báo "AI chưa được cấu hình" và chuyển sang dự phòng. Viết mã và test vẫn làm được. |
| Tài liệu chính sách sản phẩm (PDF gốc đã chuyển sang text/md, có tên và ngày hiệu lực) | Người dùng (nguồn chưa chốt, Domain Design R-03) | Trước khi demo Bolt 3 | Bolt 3 | Dùng 1–2 file text/md mẫu do nhóm tự viết để test và demo tạm. |
| FreeSWITCH trên EC2 và máy chủ ghi âm HTTP | Đã có, chạy độc lập bên ngoài | Bolt 1 (gọi điện), Bolt 5 (lấy ghi âm) | Bolt 1, 5 | Test dùng bản giả lập cho `RecordingClient`. Khi demo, máy chủ phải đang chạy và mở cổng (xem README phần 1e). |
| Google Cloud STT (khóa API) | Đã có trong cấu hình hiện tại (nên đổi khóa và chuyển sang biến môi trường) | Bolt 5 | Bolt 5 | Có thể chuyển sang Whisper (`app.stt.provider`). Test dùng bản giả lập. |
| MicroSIP cài trên máy RM | Người dùng (máy demo) | Bolt 1 | Bolt 1 | Dùng softphone trên trình duyệt `public/phone.html` để gọi thử. |
| Maven qua IntelliJ (chạy test Java, đo JaCoCo) | Người dùng | Cuối mỗi Bolt có backend (1–6) | Không chặn việc viết mã, chỉ chặn bước xác nhận test | Code Generation không chạy lệnh Java. Người dùng tự chạy rồi báo lại kết quả. |
| Kho Git (`git init`) | Người dùng | Trước lần commit đầu tiên (cuối Bolt 1) | Commit của Bolt 1 | Chưa commit được cho đến khi tạo kho. |

## Sources

- `bolt-plan.md`, `contract-summary.md` (Open questions), `components.md` (external_dependencies), `requirements.md` (O1, O2, O5)
- `team-practices.md`, `delivery-planning-questions.md`

# Units Generation — Câu hỏi

Đầu vào: `components.md` (11 thành phần), `decisions.md` (ADR-001…007), `requirements.md`, `stories.md`.

Mô hình triển khai đã chốt ở bước Domain Design gồm một backend Spring Boot chia module (ADR-001) và server Node chạy giao diện cùng MicroSIP (ADR-002). Nhóm không làm walking skeleton (`team.md`). Bước này quyết định cách gom các thành phần thành các đơn vị việc (unit) cần xây.

## Q1. Chia unit theo cách nào?

A. **Theo tính năng (lát cắt dọc)**. Mỗi unit gồm phần backend và các màn liên quan, đủ để demo riêng. Đề xuất gồm 6 unit:
   - U1 Lead & chi tiết lead: dữ liệu seed, S1, nút Gọi KH, chế độ demo
   - U2 Cổng LLM: LlmClient dùng chung
   - U3 Copilot: S2 và hỏi đáp chính sách
   - U4 Phân tích & Kết quả cuộc gọi: pipeline AI, S3, keyin, S4
   - U5 Công việc & Lịch: S5, S6, lịch sử AI
   - U6 Đăng nhập & danh sách lead: làm sau cùng
B. **Theo lớp kỹ thuật**. Chia 2 unit lớn: toàn bộ backend, và toàn bộ giao diện.
C. **Theo từng thành phần**. Khoảng 11 unit, mỗi thành phần ở `components.md` là một unit.
X. Other (please specify)

[Answer]: B

## Q2. Giao diện (các màn S0–S6) đặt ở đâu?

A. Mỗi màn nằm cùng unit tính năng sở hữu nó. Ví dụ: S3 và S4 nằm trong unit Phân tích & Kết quả cuộc gọi.
B. Tách riêng một unit giao diện (`ui`) cho mỗi tính năng. Số unit sẽ tăng lên gấp đôi.
X. Other (please specify)

[Answer]: A


## Q3. Câu bổ sung: Q1 và Q2 đang mâu thuẫn

Ở Q1 bạn chọn B, tức là chỉ có 2 unit: một unit backend và một unit giao diện. Với cách chia này không còn "unit tính năng", nên ở Q2 không thể chọn A (đặt mỗi màn chung với unit tính năng sở hữu nó). Ý bạn là cách nào dưới đây?

A. Giữ 2 unit theo Q1=B. Toàn bộ các màn S0–S6 nằm trong unit giao diện.
B. Chia theo tính năng như Q1=A (6 unit), mỗi màn đi cùng tính năng của nó như Q2=A.
X. Other (please specify)

[Answer]: B (người dùng đổi ý khi duyệt kế hoạch: "re-assign as Theo tính năng (lát cắt dọc)")

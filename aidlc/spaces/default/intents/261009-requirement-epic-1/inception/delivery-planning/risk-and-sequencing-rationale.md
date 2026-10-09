# Risk & Sequencing Rationale

**Bolt** là một lượt xây trọn một unit, đi qua thiết kế, viết mã và kiểm thử (`bolt-plan.md`).

## Nguyên tắc sắp thứ tự: giá trị demo trước

Người dùng chọn xếp Bolt theo luồng mà người xem demo trải qua [Q1=B]: chi tiết lead và gọi điện → Copilot trong lúc gọi → lịch → kết quả cuộc gọi → đăng nhập. Mỗi Bolt xong sẽ nối dài thêm câu chuyện demo một đoạn.

Không dùng chấm điểm WSJF. WSJF (Weighted Shortest Job First) xếp hạng việc theo công thức giá trị cộng mức khẩn cấp cộng mức giảm rủi ro, rồi chia cho độ lớn. Ở đây quyết định là chọn theo trình tự câu chuyện, không phải theo điểm số.

| Thứ tự | Bolt | Lý do đứng ở vị trí này |
|---|---|---|
| 1 | U1 | Là điểm bắt đầu của mọi luồng (S1 rồi đến gọi). Không phụ thuộc unit nào. |
| 2 | U2 | U3 và U4 đều cần đến. Unit nhỏ, có bản giả lập nên không bị chặn vì chưa có khóa API. |
| 3 | U3 | Bước kế tiếp trong luồng demo (trong lúc gọi). Chỉ cần U1 và U2. |
| 4 | U5 | U4 phụ thuộc vào U5 (tạo công việc khi lưu keyin), nên U5 phải xong trước U4. |
| 5 | U4 | Luồng sau cuộc gọi, là giá trị chính của epic. Cần U1, U2 và U5. |
| 6 | U6 | Ưu tiên thấp nhất, làm sau cùng (User Stories, Q1). |

**So với sơ đồ phụ thuộc** (`unit-of-work-dependency.md`): thứ tự này không vi phạm cạnh nào. Đây là một trong các thứ tự hợp lệ. Thứ tự rủi-ro-trước (U1 → U2 → U5 → U4 → U3 → U6) cũng hợp lệ, nhưng người dùng không chọn.

**Đánh đổi đã chấp nhận:** U4 là phần rủi ro nhất (chất lượng STT/AI với audio tổng đài, xem epic C-STT) nhưng lại đứng thứ 5. Nếu Claude hoặc STT cho kết quả kém, cả nhóm sẽ biết muộn.

Cách giảm rủi ro:
- Ở bước Functional Design (thiết kế theo từng bước, làm cho mọi unit trước khi viết mã), U4 được thiết kế sớm cùng các unit khác.
- U2 xong ở Bolt 2. Ngay khi người dùng cung cấp khóa API, có thể thử nhanh một prompt phân tích với transcript mẫu.

## Sổ rủi ro

| Rủi ro | Khả năng | Ảnh hưởng | Giảm thiểu |
|---|---|---|---|
| Chất lượng STT tiếng Việt trên audio tổng đài thấp, làm hỏng tóm tắt, điểm và keyin | Trung bình | Cao | Dùng Google STT đã chạy ổn trong mã hiện có. Có phương án dự phòng từng cột (FR4.8). Thử sớm với ghi âm thật ngay khi U2 có khóa API. |
| Claude trả JSON sai cấu trúc hoặc điểm nằm ngoài khoảng cho phép | Trung bình | Trung bình | Kiểm tra lại kết quả và chạy lại 1 lần; nếu vẫn sai thì hiện "Chưa có điểm AI" (NFR3). Server tự tính điểm tổng. |
| Chưa có khóa API Anthropic | Cao (hiện tại) | Trung bình | Dùng bản giả lập trong U2. Báo lỗi `NOT_CONFIGURED`, khi đó giao diện chuyển sang dự phòng. |
| Chưa có tài liệu chính sách, định dạng text/md chưa chốt | Cao (hiện tại) | Trung bình, chỉ ảnh hưởng U3 | Chốt một định dạng metadata tối thiểu ở Functional Design. Dùng 1–2 file mẫu để test. |
| Hai cuộc gọi chồng nhau, ghi âm "mới nhất" bị gán nhầm cuộc gọi | Thấp (demo chỉ một cuộc gọi tại một thời điểm) | Cao | Trả 409 khi đang có cuộc gọi chưa xử lý xong (C1). Ghi rõ giới hạn này khi demo. |
| Khóa API thật đã bị commit trong `application.properties` và `.env` | Đã xảy ra | Cao nếu repo bị chia sẻ | Chỉ là khuyến nghị (theo quyết định của nhóm). Khóa Anthropic mới lấy từ biến môi trường (NFR2). |
| Không có CI, không chạy test Java tự động trong quy trình | Cao | Trung bình | Người dùng tự chạy `verify` bằng IntelliJ trước mỗi commit. Mỗi Bolt có tiêu chí độ phủ ≥ 80% trong Definition of Done. |
| Thứ tự theo luồng demo đẩy rủi ro AI xuống cuối | Đã chấp nhận | Trung bình | Thiết kế U4 sớm (làm theo từng bước), thử prompt ngay sau Bolt 2. |

## Sources

- `bolt-plan.md`, `unit-of-work-dependency.md`, `requirements.md`, `contract-summary.md`, `components.md`
- `team-practices.md`, `delivery-planning-questions.md`
- `epic 1.md` (C-STT, C-SEC)

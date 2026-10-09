# Refined Mockups — AI phân tích cuộc gọi RM với Khách hàng

**Đầu vào:**
- 5 mockup HTML ở thư mục gốc dự án, là bản thiết kế gốc;
- `stories.md` (US1–US4);
- `requirements.md` (FR1–FR5, NFR1–NFR7);
- `team-practices.md` (chạy cục bộ, không có bước build).

Phạm vi classic không có bước Rough Mockups, vì vậy không có `wireframes.md` và `user-flow.md`. Tài liệu này mô tả **phần điều chỉnh so với mockup**: thêm các trạng thái, thêm hai màn còn thiếu, và thống nhất giao diện giữa 5 file. Mockup HTML vẫn là chuẩn hình ảnh.

**Quyết định đã chốt** (`refined-mockups-questions.md`):
- [Q1=A] Dùng HTML tĩnh, Tailwind qua CDN, JavaScript thuần dạng ES module, không có bước build. Chỉ dùng bộ icon Font Awesome.
- [Q2=A] Hai màn chưa có mockup do nhóm tự thiết kế và **bám đúng design system của các file HTML**.
- [Q3=B] Giao diện co giãn trong khoảng 1280–1920 px, giữ tỉ lệ cột như mockup.

## 1. Danh mục màn hình

| Mã | Màn | Mockup gốc | Story | Ghi chú |
|---|---|---|---|---|
| S0 | Đăng nhập | *(mới — thiết kế ở mục 3)* | US4.1 | Làm sau cùng |
| S1 | Chi tiết tiềm năng bán (có cột danh sách lead) | `1-chi-tiet-tiem-nang-ban.html` | US4.2, US4.3, US4.4, US3.1 | "Trang chủ" dẫn về màn này |
| S2 | Thực hiện cuộc gọi + Copilot | `2-thuc-hien-cuoc-goi.html` | US1.1, US1.2, US1.3 | |
| S3 | Kết quả cuộc gọi (3 cột) | `3-ket-qua-cuoc-goi.html` / `4-lua-chon-dieu-huong.html` | US2.1–US2.4 | |
| S4 | Hộp thoại "Đã lưu hồ sơ cuộc gọi" | `4-lua-chon-dieu-huong.html` (overlay) | US2.4 | |
| S5 | Lịch làm việc & Tác vụ CRM | `5-lich-lam-viec.html` | US3.2, US3.3, US3.4 | |
| S6 | Hộp thoại Tạo / Đổi giờ công việc | *(mới — mục 3)* | US3.3 | |

Luồng chính: S1 → (Gọi KH) → S2 → (Kết thúc) → S3 → (Lưu) → S4 → S5 / lead kế tiếp (S1) / S1. Khi chưa làm US4, ứng dụng mở thẳng S1 (màn chi tiết lead) cho lead mẫu, không qua màn đăng nhập và không có danh sách lead theo RM. Từ S1, RM bấm "Gọi KH" để vào luồng chính.

## 2. Phần điều chỉnh so với từng mockup

### Phần chung (mọi màn)
- **Sidebar trái thống nhất** theo mockup 2 và 5: Trang chủ, TNB, Cơ hội, Khách hàng, Công việc, AI Sales.
  - Trong đợt này chỉ **TNB** (mở S1) và **Công việc** (mở S5) hoạt động.
  - Các mục còn lại hiển thị trạng thái vô hiệu kèm tooltip "Sắp có".
  - Mockup 1 đang dùng một bộ menu khác (Dashboard, Danh bạ, RM 360…). Khi hiện thực sẽ thay bằng bộ thống nhất này.
- **Avatar RM** ở cuối sidebar lấy tên RM đang đăng nhập. Khi chưa có US4 thì dùng tên RM mẫu.
- **Toast** dùng chung cho thông báo thành công hoặc lỗi, kiểu toast xanh lá ở mockup 5. Toast tự ẩn sau 5 giây và đọc được bằng trình đọc màn hình (xem `accessibility-checklist.md`).

### S1 — Chi tiết tiềm năng bán (mockup 1)
- **Thêm** nút chính **"Gọi KH"** (màu `mb.blue`, icon `fa-phone`) ở đầu cột 3, cạnh tên lead. Theo FR1.5. Nút bị vô hiệu kèm chú thích "Số điện thoại không hợp lệ" khi số không hợp lệ (AC4.4.2).
- Cột 2 "Danh sách tiềm năng bán" chỉ hiển thị lead của RM đang đăng nhập (US4.2). Lead đang mở được tô viền xanh như mockup.
- **Thẻ hoạt động cuộc gọi** trong "Quản lý hoạt động" (US3.1):
  - thêm khối "✨ Tóm tắt AI" ở dạng chỉ đọc, mặc định thu gọn 2 dòng, có nút "Xem thêm / Thu gọn";
  - thêm liên kết "Xem kết quả cuộc gọi →" mở S3 của đúng cuộc gọi đó;
  - nếu cuộc gọi không có tóm tắt AI, hiển thị dòng xám "Không có tóm tắt AI" (AC3.1.3).
- Biểu tượng sửa/xóa trên thẻ hoạt động **không hoạt động** trong đợt này: ẩn đi, vì ngoài phạm vi.

### S2 — Thực hiện cuộc gọi (mockup 2)
- Khối trái giữ đúng mockup với 3 tab: Thông tin KH / Chi tiết TNB / Lịch sử cuộc gọi.
- Thanh điều khiển cuộc gọi: hai nút Mic/Loa/Giữ máy chỉ hiển thị (MicroSIP tự điều khiển phần này). Đồng hồ thời lượng và nút **Kết thúc** hoạt động thật (US1.1).
- Panel Copilot (US1.2, US1.3):
  - Mỗi câu trả lời có dòng cuối: "Hiệu lực: dd/mm/yyyy · Nguồn: <tên tài liệu>, mục/trang …".
  - Nút "Xem chính sách chi tiết" mở file PDF nguồn ở tab mới, tại trang được trích dẫn.
  - Nút "So sánh sản phẩm" **ẩn đi** vì ngoài phạm vi.
  - Nút đính kèm (kẹp giấy) và nút "AI Prompt mode" trong ô nhập **ẩn đi**.
  - Dòng chân cột lịch sử hiển thị "Dữ liệu: N tài liệu chính sách" thay cho "Policy MB v3.2".
- Các trạng thái mới của panel Copilot: xem mục 4.

### S3 — Kết quả cuộc gọi (mockup 3/4)
- **Thanh đầu**: nhãn trạng thái đổi từ "Đã kết nối" thành nhãn trạng thái của pipeline: Đang xử lý / Hoàn tất / Không có ghi âm / Lỗi STT / Lỗi AI.
- **Cột 1 — Chấm điểm** (US2.2):
  - Vòng điểm hiển thị điểm tổng do hệ thống tính (tổng 5 tiêu chí × 2), nhãn hạng A/B/C/D và tên hạng.
  - Mỗi tiêu chí hiển thị điểm "x / 10" kèm thanh tiến độ. Màu thanh theo điểm: ≥ 9 xanh lá, 7–8,5 xanh dương, 5–6,5 vàng, < 5 đỏ. Luôn có số đi kèm, không chỉ dựa vào màu.
  - Dòng "Tone of Voice" giữ lại. Dòng "Tốc độ" và "Độ ngắt quãng" là mức Could: ẩn đi nếu không tính được.
  - Khối "AI INSIGHT" gồm "💡 Điểm sáng nổi bật" và "🎯 Gợi ý nâng cao".
- **Cột 2 — Hội thoại** (US2.1): giữ đúng mockup. Mức Could: lượt đang phát được tô viền xanh, giống tin nhắn 3 trong mockup.
- **Cột 3 — AI Auto Keyin** (US2.3), có các phần **bổ sung** so với mockup:
  - Hai trường bắt buộc ở đầu form, đánh dấu `*`:
    - **Giai đoạn lead**: chọn một trong Chưa tiếp cận / Đang tiếp cận / Chuyển cơ hội / Từ chối;
    - **Kết quả cuộc gọi**: danh sách thả xuống kèm ô ghi chú ngắn.
  - Mỗi trường do AI điền có huy hiệu `AI · 00:45`, tức mốc thời gian trích xuất, giống huy hiệu "Trích xuất từ 00:45" trong mockup. Khi RM sửa trường, huy hiệu đổi thành `RM đã sửa` (màu xám).
  - Mọi trường đều **sửa được**. Mockup đang hiển thị dạng chữ tĩnh, khi hiện thực phải dùng input/select/textarea.
  - Trường "Nguồn tiền luân chuyển" giữ đúng nhãn như mockup.
  - **"Next Sales Action"** đổi thành danh sách **Công việc tiếp theo** sửa được. Mỗi dòng gồm: tiêu đề, ngày giờ, loại (Lịch hẹn / Nhắc gọi / Task Zalo), ưu tiên (Cao / Thường), và nút xóa dòng. Có nút "+ Thêm công việc".
  - Vùng "Đánh giá mức độ chính xác & phù hợp" giữ đúng mockup: thang 1–10, các thẻ nhận xét bật/tắt, ô góp ý.
  - Nút cuối cột "Lưu hồ sơ cuộc gọi & Đồng bộ CRM": trong lúc đang lưu, nút bị vô hiệu và hiện vòng xoay, để tránh bấm hai lần (AC2.3.6).

### S4 — Hộp thoại sau khi lưu (mockup 4)
- Giữ đúng mockup. Mã tương tác `#CALL-…` và tên KH lấy từ dữ liệu thật.
- "Liên hệ khách hàng tiếp theo": mở S1 của lead kế tiếp. Nếu không còn lead, hiện toast "Bạn đã liên hệ hết tiềm năng bán cần xử lý" rồi mở S1 với danh sách (AC2.4.5).

### S5 — Lịch làm việc (mockup 5)
- Giữ bố cục 2 panel.
- **Ẩn** nút "Outlook/Google" và nút "Mở Zalo ZNS" vì ngoài phạm vi (giả định A4). Trên thẻ Task Zalo, chỉ còn nút "Đã xong".
- Bộ lọc loại: Tất cả / Lịch hẹn / Nhắc gọi / Task Zalo. Công việc không gắn lead (ví dụ họp) chỉ hiện ở bộ lọc "Tất cả".
- Thẻ công việc đã xong: chữ gạch ngang, nền xám nhạt, có biểu tượng ✓.
- Nút "Đổi giờ" và "Tạo công việc mới" mở S6.
- Tuần mặc định là tuần chứa hôm nay. Thứ 2 – Thứ 7, 08:00–17:00. Công việc nằm ngoài khung giờ được gom vào một dòng "Ngoài giờ" ở đầu hoặc cuối cột ngày.

## 3. Thiết kế cho hai màn chưa có mockup

### S0 — Đăng nhập (US4.1)
Dùng nền `mb.surface`, thẻ trắng nằm giữa màn (rộng 400 px, bo góc `rounded-2xl`), logo MB ở trên cùng.

```
+--------------------------------------------------+
|                    [ MB logo ]                   |
|          MB Advance - AI Call Assistant          |
|                                                  |
|  Ten dang nhap                                   |
|  [______________________________]                |
|  Mat khau                                        |
|  [______________________________] [hien]         |
|                                                  |
|  ( ! Sai ten dang nhap hoac mat khau )           |
|                                                  |
|  [        Dang nhap (mb.blue)        ]           |
+--------------------------------------------------+
```
<!-- Text fallback: Thẻ đăng nhập căn giữa gồm logo MB, tiêu đề, ô tên đăng nhập, ô mật khẩu có nút hiện/ẩn, dòng lỗi màu đỏ kèm icon (chỉ hiện khi sai), và nút Đăng nhập màu xanh MB rộng toàn thẻ. -->

- Nhãn hiển thị phía trên ô nhập, không dùng placeholder để thay nhãn.
- Nhấn Enter trong ô mật khẩu là gửi form.
- Trong lúc gửi, nút bị vô hiệu và hiện vòng xoay.
- Dòng lỗi đi kèm icon `fa-circle-exclamation`, không chỉ dựa vào màu đỏ.

### S6 — Hộp thoại Tạo / Đổi giờ công việc (US3.3)
Hộp thoại rộng 480 px, phong cách giống S4.
- **Tạo mới**: Tiêu đề*, Loại (Lịch hẹn / Nhắc gọi / Task Zalo), Ngày*, Giờ bắt đầu*, Thời lượng (15/30/60 phút), Ưu tiên, Lead liên quan (không bắt buộc, chọn trong lead của RM), Ghi chú.
- **Đổi giờ**: chỉ có Ngày* và Giờ*.
- Nếu thời điểm đã qua, hộp thoại hiện cảnh báo vàng "Thời điểm này đã qua. Vẫn lưu?" với hai nút [Vẫn lưu] / [Chọn lại] (AC3.3.3).
- Nếu thiếu tiêu đề, hiện lỗi ngay dưới trường đó (AC3.3.5).

## 4. Trạng thái màn hình

| Màn | Đang tải | Trống | Lỗi | Một phần / biên |
|---|---|---|---|---|
| S1 | Khung xương (skeleton) cho 3 cột | "Chưa có tiềm năng bán" (danh sách); "Chưa có hoạt động" (lịch sử) | Thẻ lỗi kèm nút "Thử lại" | Tên KH dài bị cắt bằng `…`, có tooltip hiện đủ tên |
| S2 – Copilot | Bong bóng "Đang tìm trong tài liệu…" kèm 3 chấm động | Gợi ý 4 câu hỏi mẫu khi chưa hỏi gì | Bong bóng lỗi "Copilot đang gặp sự cố, vui lòng thử lại" kèm nút "Gửi lại" (quá 30 giây hoặc LLM lỗi) | Câu trả lời "Không tìm thấy thông tin này trong tài liệu chính sách hiện có" không có dòng nguồn |
| S3 | Thanh tiến trình 3 bước (lấy ghi âm → chuyển giọng nói → phân tích AI), mỗi cột hiện khung xương | — | Theo bảng dự phòng FR4.8: từng cột hiện thông báo riêng (xem `interaction-spec.md` mục 2.3) | Transcript dài thì cuộn trong cột; trường AI để trống hiện placeholder "Chưa có thông tin" |
| S5 | Khung xương lưới tuần | "Không có công việc trong tuần này" giữa lưới | Thẻ lỗi kèm nút "Thử lại" | Nhiều việc trùng giờ thì xếp cạnh nhau, chia đôi chiều rộng cột; từ 3 việc trở lên thì hiện "+N" |

## 5. Ánh xạ story → màn

| Story | Màn |
|---|---|
| US1.1, US1.2, US1.3 | S2 |
| US2.1, US2.2, US2.3 | S3 |
| US2.4 | S3 (dự phòng), S4 |
| US3.1 | S1 |
| US3.2, US3.3, US3.4 | S5, S6 |
| US4.1 | S0 |
| US4.2, US4.3, US4.4 | S1 |

## Sources

- Mockup gốc: `1-chi-tiet-tiem-nang-ban.html`, `2-thuc-hien-cuoc-goi.html`, `3-ket-qua-cuoc-goi.html`, `4-lua-chon-dieu-huong.html`, `5-lich-lam-viec.html`.
- `stories.md`, `requirements.md`, `team-practices.md`; câu trả lời trong `refined-mockups-questions.md`.

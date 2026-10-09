# Design System Mapping

Design system được rút ra từ 5 file mockup HTML (theo câu Q2: "Cần theo system design của file html"). Mỗi file khai báo màu `mb` trong `tailwind.config` hơi khác nhau, nên tài liệu này **chốt một bộ token chung**. Mọi màn, kể cả S0 và S6, đều dùng bộ token này qua một file cấu hình Tailwind dùng chung.

## 1. Token màu

| Token | Giá trị | Nguồn | Dùng cho |
|---|---|---|---|
| `mb.blue` | `#002896` | mockup 2, 5 | Nút chính, tiêu đề nhấn mạnh, mục sidebar đang chọn |
| `mb.lightBlue` | `#1B4DFF` | mockup 2, 5 | Trạng thái hover/active, liên kết |
| `mb.red` | `#DA251D` | mockup 2, 5 | Nút Kết thúc, lỗi, ưu tiên cao |
| `mb.surface` | `#F4F6F9` | mockup 2, 5 | Nền trang |
| `mb.border` | `#E2E8F0` | mockup 2, 5 | Viền thẻ |
| `mb.textPrimary` | `#1E293B` | mockup 5 | Chữ chính |
| `mb.textSecondary` | `#64748B` | mockup 5 | Chữ phụ |
| Màu ngữ nghĩa | Tailwind `emerald-*` (thành công), `amber-*` (cảnh báo, Hot Lead), `rose/red-*` (lỗi), `purple-*` (KH trong transcript) | cả 5 mockup | Huy hiệu, toast, nhãn |

Các giá trị khác đang có trong mockup 1 (`#1b4bcf`, `#306bf3`) và mockup 4 (`#0033a0`) được **quy về** `mb.blue` / `mb.lightBlue`. Tương phản chữ trắng trên nền `#002896` đạt trên 12:1, thỏa mức AA.

## 2. Chữ, khoảng cách, bo góc, đổ bóng

- **Font**: Inter (Google Fonts) với các độ đậm 400/500/600/700. Font dự phòng: `system-ui, sans-serif`.
- **Cỡ chữ** lấy theo mockup: tiêu đề màn `text-base font-bold`, tiêu đề khối `text-xs font-bold uppercase tracking-wider`, nội dung `text-xs`, chữ phụ `text-[11px]` / `text-[10px]`. Cỡ nhỏ nhất là **10 px**: các chữ 9 px trong mockup được nâng lên 10 px để dễ đọc.
- **Khoảng cách**: thang 4 px của Tailwind (`p-2`, `p-3`, `p-4`, `gap-3`, `gap-4`) như trong mockup.
- **Bo góc**: thẻ dùng `rounded-xl`, thẻ lớn và hộp thoại dùng `rounded-2xl`, huy hiệu dùng `rounded` hoặc `rounded-full`.
- **Đổ bóng**: `shadow-xs` / `shadow-sm` cho thẻ, `shadow-2xl` cho hộp thoại.

## 3. Icon

Theo [Q1=A], toàn bộ giao diện chỉ dùng **Font Awesome 6** (CDN). Material Symbols và Phosphor trong mockup được thay bằng icon Font Awesome tương đương:

| Mockup | Thay bằng |
|---|---|
| `auto_awesome` (Material) | `fa-wand-magic-sparkles` |
| `schedule` | `fa-regular fa-clock` |
| `phone_in_talk` / `call` | `fa-phone` |
| `sync` | `fa-rotate` |
| `ph-calendar` (Phosphor) | `fa-regular fa-calendar` |
| `ph-funnel` | `fa-filter` |
| Mũi tên quay lại | `fa-arrow-left` |

## 4. Ánh xạ thành phần → mẫu trong mockup

| Thành phần | Mẫu trong mockup | File |
|---|---|---|
| Sidebar | `aside w-[76px]`: icon kèm nhãn 10px, mục đang chọn có viền `border-blue-500` | 5 |
| Thanh đầu màn | `header h-[60–68px] bg-white`: nút "Quay lại", tiêu đề, huy hiệu trạng thái | 2, 4, 5 |
| Thẻ khối | `bg-white rounded-xl border border-slate-200 shadow-sm`, tiêu đề khối viết hoa | tất cả |
| Huy hiệu trạng thái | `rounded-full text-[10px] font-semibold` kèm chấm tròn màu | 2, 4 |
| Nút chính | `bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-bold` (sau đổi thành `mb.blue`) | 4 |
| Nút phụ | `bg-white border border-slate-200 text-slate-700 rounded-lg` | 2, 4 |
| Trường keyin | `bg-slate-50 border rounded-lg p-2.5` + nhãn `text-[11px] font-semibold` (sau đổi thành input sửa được) | 4 |
| Bong bóng transcript | RM: `bg-blue-50/50 border-blue-100`; KH: `bg-slate-50 border-slate-200`; avatar tròn có chữ "RM"/"KH" | 4 |
| Bong bóng chat Copilot | Câu hỏi bên phải `bg-blue-50`, câu trả lời bên trái `bg-slate-50`, chip gợi ý `rounded-full` | 2 |
| Thanh điểm tiêu chí | `h-1.5 bg-slate-100` với phần đã đạt `bg-emerald-500` hoặc `bg-blue-600` | 4 |
| Thẻ công việc trên lịch | `absolute rounded-xl p-2`; màu nền theo loại: xanh = lịch hẹn, vàng = Task Zalo, tím = họp, xám = khác | 5 |
| Toast | `bg-emerald-600 text-white rounded-xl shadow-lg`, góc trên bên phải | 5 |
| Hộp thoại | Lớp phủ `rgba(15,23,42,.55)` + `backdrop-blur`, thẻ `rounded-2xl` rộng 560px | 4 |

## 5. Bố cục co giãn (1280–1920 px) [Q3=B]

- Mockup cố định `width: 1440px` và `overflow:hidden` trên `body`. Khi hiện thực sẽ **bỏ** các giá trị cố định này và thay bằng `min-w-[1280px] max-w-[1920px] mx-auto h-screen`.
- Sidebar giữ cố định 76–80 px. Vùng nội dung dùng lưới 12 cột. Tỉ lệ cột theo mockup:
  - S1: 3 / 3 / 6
  - S2: 5 / 7
  - S3: 4 / 4 / 4
  - S5: panel trái 390 px cố định, lịch chiếm phần còn lại
- Chiều cao theo khung nhìn (`h-screen`). Mỗi cột tự cuộn riêng, giống cách mockup dùng `overflow-y-auto`.
- Dưới 1280 px, trang hiện thanh cuộn ngang thay vì vỡ bố cục. Các khổ nhỏ hơn nằm ngoài phạm vi (chỉ làm web desktop, theo `requirements.md` Q2).

## 6. Cách tổ chức mã giao diện

- Một file `public/js/tailwind-config.js` chứa bộ token ở mục 1. File này được nạp trước Tailwind CDN trên mọi trang.
- Mỗi màn có một file HTML riêng. Logic viết bằng ES module, theo cách tách sẵn có `config.js` / `api.js` / `ui.js` (`team-practices.md`: theo quy ước mã hiện có, không có bước build).

## Sources

- 5 mockup HTML ở thư mục gốc dự án; `refined-mockups-questions.md` (Q1–Q3); `requirements.md` (NFR5); `stories.md`; `team-practices.md`.

# Accessibility Checklist (WCAG 2.1 AA)

Mục tiêu là đạt WCAG 2.1 mức AA cho các màn S0–S6 (`mockups.md`). Danh sách này cụ thể hóa NFR5 trong `requirements.md`: dùng được bằng bàn phím, trạng thái luôn có chữ đi kèm, giao diện tiếng Việt. Mỗi mục đều kiểm được khi làm Build and Test bằng thao tác bàn phím và công cụ axe/Lighthouse.

## Nhận biết được (Perceivable)
- [ ] Mọi trang khai báo `<html lang="vi">`.
- [ ] Logo MB có `alt="MB Bank"`. Icon trang trí có `aria-hidden="true"`.
- [ ] Chữ thường đạt độ tương phản ≥ 4.5:1, chữ lớn và thành phần giao diện đạt ≥ 3:1. Phải kiểm riêng các chữ `text-slate-400` trên nền trắng trong mockup: những chữ mang thông tin thì đổi sang `text-slate-500`.
- [ ] Không có chữ nhỏ hơn 10 px (`design-system-mapping.md` mục 2).
- [ ] Không thông tin nào chỉ được thể hiện bằng màu:
  - [ ] thanh điểm tiêu chí luôn kèm số "x / 10";
  - [ ] nhãn người nói "RM"/"KH" là chữ;
  - [ ] loại công việc có nhãn chữ;
  - [ ] ưu tiên "Cao" có chữ, không chỉ dùng màu đỏ;
  - [ ] trạng thái pipeline luôn có chữ.
- [ ] Ghi âm có transcript kèm theo, cũng chính là cột 2 của S3.

## Thao tác được (Operable)
- [ ] Mọi chức năng dùng được chỉ bằng bàn phím, gồm:
  - [ ] gửi câu hỏi Copilot, chọn chip gợi ý;
  - [ ] phát/tua audio (Space, ←/→);
  - [ ] chọn điểm 1–10 khi đánh giá AI (nhóm radio, chuyển bằng phím mũi tên);
  - [ ] thêm/xóa công việc tiếp theo;
  - [ ] chuyển tuần và mở thẻ công việc trên lịch.
- [ ] Thứ tự Tab đi theo bố cục: sidebar → thanh đầu → các cột từ trái sang phải.
- [ ] Phần tử đang focus có viền rõ (`focus-visible:ring-2 ring-blue-500`, độ dày ≥ 2 px). Không được dùng `outline: none` mà không có viền thay thế.
- [ ] Hộp thoại S4 và S6 giữ focus bên trong. Phím Esc đóng hộp thoại, focus quay lại nút đã mở nó.
- [ ] Cập nhật trạng thái định kỳ (polling) không bắt người dùng phản hồi trong thời hạn nào. Phần tử động có thể tắt hiệu ứng (`prefers-reduced-motion`).
- [ ] Có liên kết "Bỏ qua tới nội dung chính" là phần tử focus đầu tiên của mỗi trang.

## Hiểu được (Understandable)
- [ ] Mọi ô nhập có `<label>` hiển thị, không dùng placeholder thay nhãn. Ô nhập Copilot dùng nhãn ẩn, chỉ trình đọc màn hình đọc được.
- [ ] Trường bắt buộc có dấu `*` kèm `aria-required="true"`.
- [ ] Thông báo lỗi nêu rõ trường nào và cách sửa, đặt ngay dưới trường đó và gắn với trường qua `aria-describedby`. Dữ liệu người dùng đã nhập được giữ nguyên (AC2.3.5, AC3.3.5, AC4.1.2).
- [ ] Sidebar, nhãn và tên nút thống nhất trên mọi màn (`mockups.md` mục 2, phần chung).
- [ ] Trước khi lưu một thời điểm đã qua, hệ thống luôn hỏi xác nhận (AC3.3.3).

## Bền vững (Robust)
- [ ] Ưu tiên dùng thẻ HTML gốc: `<button>`, `<a>`, `<label>`, `<ol>`, `<table>`. Không dùng `<div onclick>`.
- [ ] Vùng thay đổi động dùng `aria-live="polite"`: tiến trình pipeline, tin nhắn Copilot, toast.
- [ ] Hộp thoại có `role="dialog"`, `aria-modal="true"` và `aria-labelledby` trỏ tới tiêu đề.
- [ ] Mỗi `id` chỉ xuất hiện một lần trên một trang.

## Cách kiểm
1. Chạy Lighthouse/axe trên S1, S2, S3, S5 và đặt mục tiêu 0 lỗi nghiêm trọng.
2. Rút chuột và đi hết luồng S2 → S3 (lưu) → S4 → S5 chỉ bằng bàn phím.
3. Phóng to trình duyệt 200% ở khổ 1280 px. Nội dung phải đọc được: được phép cuộn ngang, nhưng không mất chức năng nào.

## Sources

- `requirements.md` (NFR5), `stories.md` (các AC dẫn ở trên), `mockups.md`, `interaction-spec.md`, `design-system-mapping.md`, `team-practices.md` (kiểm thử thủ công trên máy, không có CI).

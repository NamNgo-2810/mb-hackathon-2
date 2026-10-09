# Yêu cầu Khả năng mở rộng — U1 `u1-lead-call-foundation`

Phạm vi khả năng mở rộng của unit nền tảng, kế thừa từ `NFR1` (phần tải/đồng
thời của inception). Đây là **demo hackathon chạy cục bộ**: mục tiêu nhỏ, cố
định và tường minh, **không** đặt SLA quy mô doanh nghiệp. Ràng buộc nền tảng:
hệ thống **chỉ thực hiện một cuộc gọi tại một thời điểm** vì cơ chế nối ghi âm
theo "file mới nhất" (ràng buộc §4 của requirements, Domain Design R-01).

## Mục tiêu tải và đồng thời

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR1.10 | Hỗ trợ đúng **1 cuộc gọi đang diễn ra tại một thời điểm**; cuộc gọi thứ hai bị từ chối | `POST /api/calls` khi đã có cuộc gọi chưa xử lý xong trả `409 CONFLICT`, không tạo `CallRecord` mới (BR3.2) |
| NFR1.11 | Hỗ trợ **≤ 5 RM** mẫu (seed) truy cập đồng thời ở mức đọc lead | 5 phiên đọc đồng thời giữ p90 ≤ 500 ms (theo NFR1.1); không có lỗi tranh chấp dữ liệu |
| NFR1.12 | Hỗ trợ dữ liệu **≤ vài trăm lead** tổng cộng (seed), ≤ ~100 lead/RM | Các truy vấn danh sách/chi tiết/next giữ ngưỡng NFR1.1–NFR1.3 ở quy mô này; không yêu cầu phân trang cho demo |
| NFR1.13 | Nhịp hỏi trạng thái cuộc gọi 1 lần/3 giây cho 1 phiên gọi được chịu tải ổn định | Trong suốt một cuộc gọi ≤ 5 phút (≤ ~100 lần hỏi), `GET /api/calls/{callId}` giữ p90 ≤ 300 ms (NFR1.6) |

## Tăng trưởng dữ liệu

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR1.14 | `Activity` của một lead tăng theo số cuộc gọi nhưng vẫn ở quy mô demo | Với ≤ 100 Activity/lead, `GET /api/leads/{id}/activities` giữ p90 ≤ 500 ms (NFR1.2); không cần lưu trữ/nén |
| NFR1.15 | `CallRecord` tích lũy theo thời gian demo không gây suy giảm | Với ≤ vài trăm `CallRecord`, API đọc cuộc gọi giữ ngưỡng NFR1.6; không yêu cầu dọn dữ liệu tự động |

## Chiến lược mở rộng (có chủ đích KHÔNG mở rộng cho demo)

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR1.16 | **Không** scale ngang, **không** HA, **không** cân bằng tải cho demo | Chạy một tiến trình Spring Boot cục bộ; pass = không phụ thuộc nhiều instance; mọi trạng thái nằm trong một DB/một tiến trình |
| NFR1.17 | Giới hạn một-cuộc-gọi là ràng buộc kiến trúc đã chấp nhận cho demo, không phải nút nghẽn cần tối ưu | Ghi nhận rõ trong tài liệu; mở rộng nhiều cuộc gọi đồng thời nằm ngoài phạm vi (requirements §6) |

## Sources

- `requirements.md` — NFR1 (tải/đồng thời), ràng buộc §4 (một cuộc gọi), §6 (ngoài phạm vi nhiều cuộc gọi)
- `rules.md` (U1) — BR3.2 (giới hạn một cuộc gọi → `409`)
- `functional-spec.md` (U1) — WF2 (tạo cuộc gọi), WF3/WF4 (hỏi trạng thái, kết thúc)
- `unit-of-work.md` — U1 kind `service`, độ lớn M; Domain Design R-01
- `technology-stack.md` (codekb) — một ứng dụng Spring Boot, không hạ tầng mở rộng

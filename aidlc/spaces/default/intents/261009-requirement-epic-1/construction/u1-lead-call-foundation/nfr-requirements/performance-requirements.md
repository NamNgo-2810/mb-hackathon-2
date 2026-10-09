# Yêu cầu Hiệu năng — U1 `u1-lead-call-foundation`

Phạm vi hiệu năng của unit nền tảng Lead & cuộc gọi, kế thừa từ `NFR1`
(Hiệu năng) của inception. U1 **không** sở hữu bước AI/STT (đó là U4/U2); vì vậy
ngưỡng ≤ 120 giây của cả pipeline AI là ràng buộc của U4, không phải U1. Trách
nhiệm hiệu năng của U1 là các API đọc/ghi lead-cuộc gọi và phần điều phối vòng
đời pipeline (SM2) mà U1 làm chủ.

Bối cảnh: demo hackathon chạy cục bộ, **1 cuộc gọi đồng thời**, ≤ 5 RM, ≤ vài
trăm lead. Mọi ngưỡng đo trên máy phát triển cục bộ (backend Spring Boot 3.3.4
embedded Tomcat :8080, H2 in-memory ở profile `demo`), không phải môi trường
production.

## Mục tiêu thời gian phản hồi (API đồng bộ của U1)

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR1.1 | API đọc chi tiết lead `GET /api/leads/{leadId}` (và `GET /api/leads/demo`) trả về nhanh | p90 ≤ 500 ms, p100 ≤ 1000 ms khi chạy cục bộ với dữ liệu seed (≤ vài trăm lead); đo bằng mốc thời gian xử lý phía server |
| NFR1.2 | API lịch sử hoạt động `GET /api/leads/{leadId}/activities` trả về nhanh | p90 ≤ 500 ms với một lead có ≤ 100 Activity; danh sách đã sắp theo `occurredAt` giảm dần trong cùng truy vấn (không sắp xếp phía giao diện) |
| NFR1.3 | API chọn lead kế tiếp `GET /api/leads/next` | p90 ≤ 500 ms trên tập ≤ vài trăm lead của một RM; thực hiện lọc + sắp xếp (BR5.1) trong một truy vấn |
| NFR1.4 | API tạo cuộc gọi `POST /api/calls` (chỉ phần của U1: kiểm quyền, kiểm số, tạo `CallRecord`) | p90 ≤ 800 ms; không tính thời gian khởi tạo cuộc gọi phía MicroSIP (C6, bất đồng bộ) |
| NFR1.5 | API kết thúc cuộc gọi `POST /api/calls/{callId}/ended` trả `200` ngay sau khi đặt `endedAt` và kích hoạt pipeline | p90 ≤ 800 ms; việc khởi chạy pipeline (SM2 `IN_CALL → PROCESSING`) là bất đồng bộ, API không chờ pipeline hoàn tất |
| NFR1.6 | API hỏi trạng thái cuộc gọi `GET /api/calls/{callId}` (giao diện hỏi định kỳ 3 giây) | p90 ≤ 300 ms; chịu được nhịp hỏi 1 lần/3 giây của 1 phiên gọi mà không làm chậm các API khác |

## Ngân sách độ trễ cho mở màn (phần giao diện U1 sở hữu: S1, khung S2)

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR1.7 | Màn chi tiết lead (S1) và khung màn thực hiện cuộc gọi (S2) hiển thị xong dữ liệu lần đầu | ≤ 2 giây tính từ khi điều hướng tới màn, khi chạy cục bộ (phù hợp NFR1 inception "các màn mở trong ≤ 2 giây") |

## Ràng buộc thông lượng và tài nguyên

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR1.8 | U1 phục vụ đồng thời 1 cuộc gọi đang diễn ra cộng với nhịp hỏi trạng thái 3 giây mà không xếp hàng chờ | Trong lúc 1 cuộc gọi hoạt động, các API đọc lead khác vẫn giữ p90 ≤ 500 ms (không vượt ngưỡng NFR1.1) |
| NFR1.9 | Việc điều phối pipeline của U1 (gọi lần lượt `CallPipelineStep` qua `updatePipeline`, I2) không chặn luồng request HTTP | Bước pipeline chạy ngoài luồng xử lý request của `POST /api/calls/{callId}/ended`; đo được bằng việc API `ended` trả `200` trước khi pipeline đạt trạng thái cuối |

## Giới hạn ngoài phạm vi U1 (ghi để tránh nhầm)

- Ngưỡng ≤ 120 giây cho kết quả AI (transcript + tóm tắt + điểm + keyin) thuộc
  pipeline do U4/U2 thực hiện — U1 chỉ điều phối vòng đời và cập nhật `pipelineStatus`.
- Ngưỡng ≤ 15 giây (p90) và timeout 30 giây của Copilot thuộc U3.
- Timeout STT/LLM từng lời gọi thuộc U4/U2 (NFR3).

## Sources

- `requirements.md` — NFR1 (ngưỡng hiệu năng), FR1.3, FR1.5, FR2.2, FR4.9
- `functional-spec.md` (U1) — WF1, WF2, WF4, WF5, WF6, SM2
- `contract-summary.md` — C1 (các endpoint U1), timeout phía giao diện, I2 (`CallService`, `CallPipelineStep`)
- `unit-of-work.md` — phạm vi U1, ranh giới U1/U4
- `technology-stack.md` (codekb) — Spring Boot 3.3.4, embedded Tomcat :8080, H2 demo profile

# Yêu cầu Khả năng quan sát — U1 `u1-lead-call-foundation`

Phạm vi quan sát của unit nền tảng, kế thừa từ `NFR6` (Quan sát) của inception.
U1 điều phối pipeline (SM2) nên là nơi ghi log vòng đời cuộc gọi theo từng
bước. Mục tiêu demo: **log có cấu trúc** với định danh tương quan `callId`/`rmId`,
một **health endpoint** đơn giản; **không** có ngăn xếp giám sát ngoài (không
Prometheus/Grafana/APM), **không** tracing phân tán.

## Ghi log có cấu trúc theo bước pipeline

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR6.1 | Mỗi bước pipeline do U1 điều phối (FETCH_RECORDING, TRANSCRIBE, ANALYZE) ghi một dòng log kèm `callId`, bước, kết quả (thành công/lỗi) và thời gian chạy (ms) | Log có đủ 4 trường cho mỗi lần chuyển bước; kiểm bằng chạy một cuộc gọi và đọc log; mỗi chuyển `pipelineStatus`/`pipelineStep` sinh đúng một dòng |
| NFR6.2 | Mọi lỗi được ghi log, **không bỏ qua im lặng** (NFR6, guardrail Construction §Error Handling) | Mọi nhánh lỗi (NO_RECORDING, STT_FAILED, AI_FAILED) ghi log mức WARN/ERROR kèm `callId` và `errorDetail`; không có nhánh catch rỗng; kiểm bằng review + test |
| NFR6.3 | Log mang định danh tương quan `callId` và `rmId` để truy vết một cuộc gọi xuyên các bước | Lọc log theo một `callId` dựng lại được chuỗi IN_CALL → PROCESSING → trạng thái cuối; `rmId` có mặt trên các dòng liên quan quyền sở hữu |

## Không log dữ liệu nhạy cảm

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR6.4 | Log **không** chứa nội dung transcript, PII của KH, khóa/token hay `passwordHash` (gắn với NFR2.12/BR6.1) | Review mọi log statement của U1 + test khẳng định không in transcript/khóa/`passwordHash`; chỉ in định danh và trạng thái |

## Sức khỏe hệ thống

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR6.5 | Có một **health endpoint** đơn giản báo tiến trình backend đang sống | Gọi endpoint sức khỏe trả `200` khi ứng dụng chạy (ví dụ Spring Boot Actuator `/actuator/health` nếu bật, hoặc một endpoint tối thiểu); kiểm bằng gọi khi app chạy cục bộ |

## Chỉ số vận hành tối thiểu (mức demo)

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR6.6 | Thời gian chạy từng bước pipeline được ghi để quan sát hiệu năng (phục vụ NFR1) | Mỗi dòng log bước có trường thời lượng ms; đủ để tính thủ công p90 khi chạy demo nhiều lần |

## Ngoài phạm vi (ghi để tránh nhầm)

- Không ngăn xếp giám sát ngoài (Prometheus, Grafana, CloudWatch, APM).
- Không tracing phân tán (OpenTelemetry/Zipkin).
- Không dashboard/alerting tự động; quan sát bằng đọc log cục bộ cho demo.
- SLI/SLO chính thức không áp dụng (xem NFR3.8 — không mục tiêu SLA cho demo).

## Sources

- `requirements.md` — NFR6 (quan sát), NFR2 (không log bí mật), NFR1 (hiệu năng cần đo)
- `rules.md` (U1) — BR6.1 (không log `passwordHash`), BR4.2 (U1 ghi trạng thái pipeline)
- `functional-spec.md` (U1) — WF5 (điều phối pipeline), SM2 (các bước/trạng thái)
- `entities.md` (U1) — `CallRecord.errorDetail`, `pipelineStep`
- `aidlc/spaces/default/memory/phases/construction.md` — §Error Handling, §Security (không bỏ qua lỗi, không log bí mật)
- `technology-stack.md` (codekb) — Spring Boot 3.3.4 (log/Actuator sẵn trong BOM)

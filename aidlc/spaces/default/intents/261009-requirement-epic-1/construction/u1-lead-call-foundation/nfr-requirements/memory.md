<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is kept up to date automatically while the stage runs. Add observations at the review step, not by editing here directly.

## Interpretations
- 2026-10-09T16:26:56+07:00 — NFR5 (ngôn ngữ/khả dụng UI) đánh dấu N/A cho U1; U1 là kind=service chỉ sở hữu API + khung S1/S2, NFR UI đo được thuộc artifact kind=ui. Ngưỡng ≤120s của pipeline AI quy về U4/U2, U1 chỉ giữ NFR cho API đọc/ghi và điều phối SM2
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->

## Tradeoffs
- 2026-10-09T16:26:56+07:00 — Mục tiêu scalability đặt nhỏ và tường minh (1 cuộc gọi đồng thời, ≤5 RM, ≤ vài trăm lead), không SLA/HA/scale ngang; chấp nhận giới hạn một-cuộc-gọi (BR3.2/Domain R-01) là ràng buộc kiến trúc cho demo thay vì nút nghẽn cần tối ưu. Dọn secret đã commit (O5) ghi là khuyến nghị, không chặn gate
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->

## Open questions
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->

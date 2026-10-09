<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is kept up to date automatically while the stage runs. Add observations at the review step, not by editing here directly.

## Interpretations
- 2026-10-09T16:20:00+07:00 — Định nghĩa máy trạng thái phân tích riêng của U4 (SMA: PENDING_TRANSCRIPT → ANALYZING → ANALYZED/ANALYSIS_FAILED, với ANALYSIS_FAILED → ANALYZING qua chạy lại thủ công), ánh xạ vào CallRecord.pipelineStatus của U1 thay vì sửa U1; giải quyết phát hiện tư vấn u1 (SM2 có AI_FAILED nhưng không có đường ra) từ phía U4, giữ ranh giới chỉ-U1-ghi-pipelineStatus (BR1.2).
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->

## Tradeoffs
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->

## Open questions
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->

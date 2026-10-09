<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is kept up to date automatically while the stage runs. Add observations at the review step, not by editing here directly.

## Interpretations
- 2026-10-09T15:49:00+07:00 — U6 chỉ sở hữu đúng collection path `GET /api/leads`; mọi path con cụ thể (`/api/leads/demo`, `/api/leads/next`, `/api/leads/{leadId}`) thuộc U1 và literal sub-path thắng pattern `{leadId}`; giải quyết open note về sở hữu đường dẫn trong contract-design.
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->

## Tradeoffs
- 2026-10-09T15:49:30+07:00 — Chọn opaque Bearer token lưu dạng băm server-side (không JWT) để logout thu hồi được ngay; đánh đổi: cần tra cứu phiên mỗi request, chấp nhận được ở quy mô demo. Brute-force mitigation để ở mức optional theo brief.
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->

## Open questions
- 2026-10-09T15:50:00+07:00 — ADR-002 để ngỏ token-vs-cookie qua CORS; đã chốt dùng Bearer token (Q2=A contract-design), giao diện tự giữ token và gắn header Authorization — không dùng cookie, nên không cần credentials/SameSite; coi như đã giải quyết ở stage này.
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->

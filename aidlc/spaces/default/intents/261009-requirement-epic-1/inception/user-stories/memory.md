<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is kept up to date automatically while the stage runs. Add observations at the review step, not by editing here directly.

## Interpretations
- 2026-10-09T06:22:00Z — Q1 trả lời "Ưu tiên cuối, làm sau cùng, đừng hỏi lại nữa": hiểu là các story nền tảng (đăng nhập, lead, gọi từ lead) đứng thành nhóm riêng US4, ưu tiên thấp nhất (Could), làm sau cùng; không hỏi lại. Để US1–US3 vẫn demo được độc lập trước khi có US4, giả định dùng một RM và một lead mẫu cố định từ dữ liệu seed và mở màn thực hiện cuộc gọi trực tiếp.
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->

## Tradeoffs
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->

## Open questions
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->

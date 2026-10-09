<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is kept up to date automatically while the stage runs. Add observations at the review step, not by editing here directly.

## Interpretations
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->

## Deviations
- 2026-10-09T04:30:06Z — Q6=C: tất cả quy tắc cứng ứng viên (secrets, migration, input validation) hạ xuống mức khuyến nghị, không mandated; `## Mandated`/`## Forbidden` để `None.`.
- 2026-10-09T04:16:36Z — không có kho Git nào ở gốc dự án (`git status`/`branch`/`log` đều báo "not a git repository"); không suy ra được chiến lược nhánh/merge nên Way of Working để hoàn toàn ở mức đề xuất mặc định org.md và timestamp artifact ghi "no commit hash".
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->

## Tradeoffs
- 2026-10-09T04:30:06Z — Nhóm chốt Git local + commit thẳng `main`, không nhánh/CI (khác mặc định org trunk-based+squash); đổi tốc độ hackathon lấy việc mất gating tự động và lịch sử nhánh. Sàn 80% coverage giữ nguyên nhưng chuyển sang kiểm thủ công trước commit.
- 2026-10-09T04:16:36Z — chọn `test-after` làm Methodology vì kho hoàn toàn không có test và README xác nhận là quyết định hackathon; đề xuất sàn 80%+CI theo scope classic nhưng để nhóm nới cho bối cảnh demo thay vì áp cứng.
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->

## Open questions
- 2026-10-09T04:30:06Z — Secrets thật đã bị commit (Google STT key, X-API-Key, host EC2) cần xoay khóa; kho Git chưa `git init`; không CI nên sàn 80% phụ thuộc kỷ luật thủ công.
- 2026-10-09T04:16:36Z — secrets thật đang nằm trong `application.properties` và `.env` (working tree) dù `.gitignore` có ignore `.env`; cần nhóm xác nhận nâng ứng viên "không commit secret + xoay vòng key" thành quy tắc cứng và quyết thời điểm xoay key.
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->

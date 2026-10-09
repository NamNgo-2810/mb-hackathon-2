<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is kept up to date automatically while the stage runs. Add observations at the review step, not by editing here directly.

## Interpretations
- 2026-10-09T05:06:00Z — Dùng 5 mockup HTML ở thư mục gốc làm chuẩn giao diện; Người dùng chỉ trỏ tới màn 5 (Lịch làm việc) cho Q11/Q12, nhưng màn 1/2/4 trùng đúng 3 US của epic nên được dùng để suy ra trường form keyin, thang chấm điểm và luồng sau khi lưu. Các tính năng chỉ có trên mockup nhưng ngoài tầm demo (đồng bộ Outlook/Google, gửi Zalo ZNS thật) ghi là ngoài phạm vi.
- 2026-10-09T05:06:00Z — Q6=A (mỗi tiêu chí 0-100, điểm tổng là trung bình) lệch với mockup màn 4 (mỗi tiêu chí /10, tổng /100, có hạng A). Chọn theo mockup và tên tiêu chí của mockup, điểm tổng = tổng 5 tiêu chí × 2; ghi là giả định để người dùng sửa ở cổng duyệt.
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->

## Deviations
- 2026-10-09T05:06:00Z — Đổi tên 5 file mockup sang tên không dấu (1-chi-tiet-tiem-nang-ban.html … 5-lich-lam-viec.html) theo lựa chọn của người dùng, vì tên tiếng Việt có dấu không khớp mã hóa khi đọc; nội dung file không đổi.
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->

## Tradeoffs
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->

## Open questions
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->

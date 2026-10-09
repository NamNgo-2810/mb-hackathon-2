<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is kept up to date automatically while the stage runs. Add observations at the review step, not by editing here directly.

## Interpretations
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->


- 2026-10-09T15:49:00+07:00 — U6 chỉ sở hữu đúng collection path `GET /api/leads`; mọi path con cụ thể (`/api/leads/demo`, `/api/leads/next`, `/api/leads/{leadId}`) thuộc U1 và literal sub-path thắng pattern `{leadId}`; giải quyết open note về sở hữu đường dẫn trong contract-design.
<!-- aidlc-wave-memory:u6-login-lead-list:76a2ed8b1808a445e02c3d8dad7a45ad694be20d96718c8611a2f9faa5713637 -->


- 2026-10-09T15:49:00+07:00 — Chốt clientKey BẮT BUỘC cho TaskService.createFromCall (I4), thiếu thì từ chối; giải ghi chú mở Contract Design để idempotent theo (callId, clientKey) luôn xác định (BR2.1/BR2.2).
<!-- aidlc-wave-memory:u5-task-calendar:9872818fd82385e098bbbae6c8986cec97d34ba238e19e60586bb6f48a5dcb78 -->


- 2026-10-09T15:52:00+07:00 — Kiểm chứng trích dẫn (BR4.2) có thể ép answerFound=false khi lọc hết citation; diễn giải "chỉ trả lời từ tài liệu" (ADR-003/FR3.2) chặt hơn: câu trả lời có nội dung mà không trích dẫn được đoạn đã gửi thì coi như không tìm thấy, tránh trả lời không có nguồn.
<!-- aidlc-wave-memory:u3-copilot:f66703bceafa2011643dfebd46cf910a8413996e02dc046bf6027e409f1c5c45 -->


- 2026-10-09T16:20:00+07:00 — Định nghĩa máy trạng thái phân tích riêng của U4 (SMA: PENDING_TRANSCRIPT → ANALYZING → ANALYZED/ANALYSIS_FAILED, với ANALYSIS_FAILED → ANALYZING qua chạy lại thủ công), ánh xạ vào CallRecord.pipelineStatus của U1 thay vì sửa U1; giải quyết phát hiện tư vấn u1 (SM2 có AI_FAILED nhưng không có đường ra) từ phía U4, giữ ranh giới chỉ-U1-ghi-pipelineStatus (BR1.2).
<!-- aidlc-wave-memory:u4-call-analysis-result:bc423c57a16ebe64be27ab2bd708354e20802688b604f522312e03d4dd62555a -->

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->

## Tradeoffs
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->


- 2026-10-09T15:49:30+07:00 — Chọn opaque Bearer token lưu dạng băm server-side (không JWT) để logout thu hồi được ngay; đánh đổi: cần tra cứu phiên mỗi request, chấp nhận được ở quy mô demo. Brute-force mitigation để ở mức optional theo brief.
<!-- aidlc-wave-memory:u6-login-lead-list:10f844e99b91fdc7521486c5a9cf770409ab171b9b12bdd41fbae98cd41c5af7 -->

## Open questions
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->

- 2026-10-09T15:50:00+07:00 — ADR-002 để ngỏ token-vs-cookie qua CORS; đã chốt dùng Bearer token (Q2=A contract-design), giao diện tự giữ token và gắn header Authorization — không dùng cookie, nên không cần credentials/SameSite; coi như đã giải quyết ở stage này.
<!-- aidlc-wave-memory:u6-login-lead-list:c6c0a3c862daf09e58a38bb01bb2d42105c57f9169295da63541239ae83e9419 -->

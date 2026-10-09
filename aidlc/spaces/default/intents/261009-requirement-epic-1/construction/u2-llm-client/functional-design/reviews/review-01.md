## Review

**Verdict:** READY
**Reviewer:** aidlc-architecture-reviewer-agent
**Date:** 2026-10-09T08:37:51Z
**Iteration:** 1

### Findings

| ID | Severity | Location | Finding | Required action | Status |
|---|---|---|---|---|---|
| R-01 | Medium | aidlc/spaces/default/intents/261009-requirement-epic-1/construction/u2-llm-client/functional-design/entities.md > thực thể LlmError | Hợp đồng I3 trong `contract-summary.md` mô tả lỗi là một ngoại lệ được ném ra (`LlmException(kind: ...)`), còn thiết kế này lại mô hình hoá lỗi thành một đối tượng giá trị `LlmError` được **trả về** cho thành phần gọi. Hai bên dùng chung đúng 5 loại lỗi (NOT_CONFIGURED, TIMEOUT, UNAVAILABLE, RATE_LIMITED, BAD_RESPONSE), nhưng cơ chế báo lỗi (ném ngoại lệ hay trả giá trị) lại khác nhau. U3 và U4 hiện thực theo I3 nên sẽ bắt ngoại lệ; nếu U2 trả giá trị thì phần xử lý dự phòng ở U3/U4 không khớp. | Thống nhất một cơ chế báo lỗi duy nhất giữa thiết kế U2 và hợp đồng I3 — hoặc ghi rõ `complete()` ném ngoại lệ, hoặc cập nhật I3 để trả `LlmError` làm giá trị — và nêu rõ cách này trong `functional-spec.md` để U3/U4 hiện thực đúng. | New |
| R-02 | Low | aidlc/spaces/default/intents/261009-requirement-epic-1/construction/u2-llm-client/functional-design/entities.md > thực thể LlmRequest, thuộc tính purpose | Thuộc tính `purpose` (bắt buộc, enum CALL_ANALYSIS/COPILOT) không có trong chữ ký `complete(LlmRequest{...})` của hợp đồng I3. Tài liệu đã nói rõ nó chỉ dùng cho log/đo lường và không ảnh hưởng hành vi, nên đây là phần bổ sung hợp lệ theo quy tắc "chỉ thêm trường" của hợp đồng; chỉ là I3 chưa phản ánh. | Bổ sung `purpose` (và trường tuỳ chọn liên quan) vào mô tả chữ ký I3, hoặc ghi một dòng trong `functional-spec.md` nêu rõ đây là trường mở rộng nội bộ ngoài hợp đồng công khai, để tránh hiểu nhầm khi U3/U4 dựng `LlmRequest`. | New |
| R-03 | Low | aidlc/spaces/default/intents/261009-requirement-epic-1/construction/u2-llm-client/functional-design/entities.md > LlmRequest.timeoutMs và LlmConfig.defaultTimeoutMs | Hợp đồng I3 dùng tên trường `timeout`, còn thiết kế dùng `timeoutMs` cho yêu cầu và `defaultTimeoutMs` cho cấu hình. Ý nghĩa khớp nhau (mọi lời gọi đều có timeout — BR3.1, NFR3) nên không phải lỗi chức năng, nhưng tên khác nhau có thể gây lệch khi hiện thực giao diện Java dùng chung. | Dùng chung một tên trường giữa `functional-spec.md`/`entities.md` và I3, hoặc ghi chú ánh xạ tên (`timeout` ↔ `timeoutMs`) ngay tại chỗ để người hiện thực không nhầm. | New |

### Validation Tool Results

| Tool | Result | Interpretation |
|---|---|---|
| rà soát thủ công các khối source-of-truth YAML | PASS | Các khối `entities.md` và `rules.md` đọc được, ID quy tắc theo đúng dạng `BRx.y`, không có ID trùng hay tham chiếu gãy trong phạm vi unit. |
| đối chiếu traceability.json với rules.md | PASS | 11/11 quy tắc trong `reverse` đều được giải thích; U2 không có AC riêng nên `upstream_ids`/`coverage` rỗng là hợp lý theo `unit-of-work-story-map.md` (U2 chỉ xuất hiện ở cột "Cũng chạm"). Không có quy tắc orphan chưa giải thích. |
| đối chiếu với hợp đồng I3, ADR-004, NFR2, NFR3 | PASS (có lưu ý) | 5 loại lỗi khớp I3; cổng đơn, khoá từ biến môi trường, caching và bản MOCK khớp ADR-004; không ghi khoá/nội dung prompt vào log khớp NFR2; tách dữ liệu khỏi chỉ dẫn khớp NFR2 (chống injection); không tự thử lại ở thư viện khớp NFR3. Lưu ý về cơ chế báo lỗi và tên trường ghi ở phần Findings. |

### Summary

Thiết kế của U2 vững và có thể hiện thực được: máy trạng thái, luồng gọi, phân loại lỗi, timeout, tách dữ liệu khỏi chỉ dẫn và bản MOCK đều nhất quán với I3, ADR-004, NFR2 và NFR3; cách để trống `coverage` và giải thích mọi quy tắc là N/A trong `traceability.json` là hợp lý vì U2 không có story riêng. Điểm đáng cân nhắc nhất trước khi duyệt là làm rõ cơ chế báo lỗi (ném ngoại lệ theo I3 hay trả `LlmError` làm giá trị) để U3/U4 hiện thực phần dự phòng cho khớp; các khác biệt tên trường còn lại chỉ ở mức nhỏ.

## Review

**Verdict:** READY
**Reviewer:** aidlc-architecture-reviewer-agent
**Date:** 2026-10-09T10:54:43Z
**Iteration:** 1

Rà soát một lượt (advisory) toàn bộ mã U2 `u2-llm-client` đã sinh: giao diện I3
`LlmClient`, hai adapter `AnthropicLlmClient`/`MockLlmClient`, các value object,
validator, dựng thông điệp, kiểm JSON, cấu hình và tài liệu. Trọng tâm: lỗi biên
dịch có khả năng xảy ra, an toàn (khóa, log, HTTPS), và tuân thủ hợp đồng I3.
Không nêu phần test vì người dùng đã hoãn.

Kết luận: không phát hiện lỗi biên dịch nào; mã khớp với bean U1 hiện có
(`WebClient`, `ObjectMapper`, `AppProperties`), hai adapter loại trừ lẫn nhau qua
`@ConditionalOnProperty` nên không xung đột bean; khóa chỉ đọc từ env, không log
khóa/prompt/response, bắt buộc HTTPS; hợp đồng I3 (`LlmClient.complete`,
`LlmException` với 5 `Kind`, trường `timeout`) được tôn trọng. Các điểm dưới đây
là lưu ý về độ bền hành vi, không chặn phê duyệt.

### Findings

| ID | Severity | Location | Finding | Required action | Status |
|---|---|---|---|---|---|
| R-01 | Major | backend/src/main/java/com/mb/callassistant/llm/AnthropicLlmClient.java > isTimeout | Việc nhận diện quá hạn (timeout) dựa một phần vào so khớp chuỗi thông điệp "Timeout on blocking read" của Reactor khi `.block(Duration)` hết giờ. Nếu thông điệp đổi theo phiên bản thư viện, timeout sẽ bị phân loại nhầm thành BAD_RESPONSE (không retry) thay vì TIMEOUT (retry) — sai với phân loại lỗi mong đợi và ảnh hưởng cách bên gọi (U3/U4) chọn dự phòng | Phát hiện quá hạn một cách tường minh (ví dụ dùng `.timeout(Duration)` của Reactor để nhận `TimeoutException`, hoặc tự quản thời hạn) thay vì dựa vào văn bản thông điệp; đảm bảo mọi đường hết giờ đều về đúng loại TIMEOUT (retryable) | New |
| R-02 | Minor | backend/src/main/java/com/mb/callassistant/llm/AnthropicLlmClient.java > mapResponse | Khi phản hồi có cấu trúc hợp lệ nhưng không có khối văn bản (ví dụ chỉ có `tool_use`), mã ném BAD_RESPONSE với `providerStatus=null`; hành vi hợp lý nhưng không được nêu trong đặc tả 5 loại lỗi, dễ gây hiểu nhầm khi đọc log | Ghi chú rõ trong JavaDoc / đặc tả rằng "phản hồi không có nội dung văn bản" cũng thuộc nhóm BAD_RESPONSE, để bên gọi và người đọc log hiểu nhất quán | New |
| R-03 | Minor | backend/src/main/java/com/mb/callassistant/llm/MockLlmClient.java > complete | Adapter MOCK luôn trả `model="mock-claude"` cứng và `latencyMs=0`, bỏ qua `app.llm.model`; chấp nhận được cho demo nhưng khác hành vi adapter thật, có thể gây nhầm khi so sánh log/giao diện giữa hai chế độ | Cân nhắc phản chiếu `app.llm.model` (khi được đặt) vào `LlmResponse.model` của MOCK để log/giao diện nhất quán, hoặc ghi chú rõ đây là giá trị giả lập cố định | New |
| R-04 | Minor | backend/src/main/java/com/mb/callassistant/llm/AnthropicLlmClient.java > complete | Thiếu cấu hình phía nhà vận hành (model rỗng, hoặc base-url không phải https) được báo bằng `IllegalArgumentException` ngay trong mỗi lần gọi `complete`, không phải lúc khởi động; mỗi request LLM mới mới phát hiện lỗi cấu hình, và không phân biệt được với lỗi lập trình của bên gọi (validator cũng ném `IllegalArgumentException`) | Cân nhắc kiểm cấu hình bắt buộc (model, scheme https) sớm hơn — hoặc nêu rõ trong JavaDoc rằng lỗi cấu hình vận hành được báo qua `IllegalArgumentException` tại thời điểm gọi — để bên gọi phân biệt với lỗi tham số đầu vào | New |

### Summary

Mã U2 khớp hợp đồng I3, an toàn về khóa/log/HTTPS, và không có lỗi biên dịch rõ
ràng khi ghép với U1; lưu ý chính là độ bền của việc phân loại timeout dựa vào
chuỗi thông điệp — nên làm tường minh để không phân loại nhầm thành lỗi
không-retry.

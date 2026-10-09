## Review

**Verdict:** READY
**Reviewer:** aidlc-architecture-reviewer-agent
**Date:** 2026-10-09T12:20:49Z
**Iteration:** 1

Đây là lượt soát xét tư vấn (một lượt), hỗ trợ quyết định ở cổng duyệt. Mã U6
được dò theo hướng tìm lỗi biên dịch, lỗi nối bean, lỗi bảo mật và lệch hợp đồng
C5/C1, cùng việc chế độ demo còn chạy mặc định hay không. Không tìm thấy lỗi
Critical hay Major; mọi mục dưới đây là Minor để cân nhắc, không chặn duyệt.

### Findings

| ID | Severity | Location | Finding | Required action | Status |
|---|---|---|---|---|---|
| R-01 | Minor | backend/src/main/java/com/mb/callassistant/auth/LoginRmSeeder.java > hằng DEFAULT_DEMO_PASSWORD | Mật khẩu seed mặc định `demo1234` được ghi cứng trong mã (dùng khi không đặt biến môi trường AUTH_DEMO_PASSWORD); đây là giá trị demo rõ ràng và ghi đè được, nhưng vẫn là một thông tin đăng nhập mặc định nằm trong mã nguồn | Giữ như hiện tại cho demo nhưng ghi rõ trong README là chỉ dùng cho demo và PHẢI đặt AUTH_DEMO_PASSWORD trước khi dùng thật; hoặc không đặt mặc định và bắt buộc biến môi trường ở chế độ login | New |
| R-02 | Minor | backend/src/main/java/com/mb/callassistant/auth/AuthController.java > applyFailDelay | Độ trễ chống dò mật khẩu dùng `Thread.sleep` chặn thẳng luồng xử lý request; mặc định 0 (tắt) nên không ảnh hưởng, nhưng khi bật ở demo nhiều lần sai sẽ giữ luồng servlet | Giữ cho demo; nếu cần ở tải thật thì cân nhắc cơ chế không chặn luồng, hoặc ghi chú rõ đây chỉ là tùy chọn demo | New |
| R-03 | Minor | backend/src/main/java/com/mb/callassistant/config/DemoDataSeeder.java > @Profile("demo") so với application.properties `spring.profiles.active=default` | Dữ liệu lead mẫu chỉ được seed khi profile `demo` bật, trong khi profile mặc định là `default`; chế độ demo (app.auth.mode=demo) vẫn chạy và `GET /api/leads` vẫn trả `[]` đúng BR4.3, nhưng người chạy mặc định sẽ thấy danh sách rỗng thay vì có dữ liệu minh hoạ (điều kiện có sẵn từ U1, không phải lỗi U6) | Ghi rõ trong README của U6 rằng muốn danh sách lead có dữ liệu minh hoạ thì chạy với profile `demo` (hoặc ở chế độ login để LoginRmSeeder seed), tránh hiểu nhầm là lỗi | New |
| R-04 | Minor | backend/src/main/java/com/mb/callassistant/config/ApplicationConfig.java > corsConfigurer allowedHeaders | CORS mở `allowedHeaders("*")` nên header `Authorization` được chấp nhận (đúng C5/ADR-002); cho phép mọi header là đủ cho demo nhưng rộng hơn mức cần thiết | Giữ cho demo; nếu muốn chặt hơn thì liệt kê tường minh các header cần (gồm `Authorization`, `Content-Type`) | New |

### Summary

Mã U6 nhất quán và sẽ biên dịch được: hai bản `CurrentRmProvider` loại trừ nhau
theo `app.auth.mode` (demo mặc định qua `matchIfMissing=true`, login qua
`havingValue=login`) nên luôn đúng một bean mỗi chế độ; `GET /api/leads` tách
khỏi các path của U1 nên không nhập nhằng; bảo mật đạt yêu cầu (BCrypt cho mật
khẩu, SHA-256 cho token và chỉ lưu băm, một thông điệp 401 duy nhất, không log
bí mật, filter không tự chặn mà biên REST ném 401, CORS nhận header
Authorization). Chế độ demo vẫn là mặc định và endpoint vẫn giải quyết được.
Chỉ còn vài điểm Minor để cân nhắc, không chặn duyệt.

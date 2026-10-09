# Yêu cầu Bảo mật — U1 `u1-lead-call-foundation`

Phạm vi bảo mật của unit nền tảng, kế thừa từ `NFR2` (Bảo mật) của inception.
U1 là nơi đặt **bất biến quyền sở hữu** (FR1.4): mọi API đọc/ghi lead, cuộc gọi,
hoạt động phải kiểm tra phía server rằng tài nguyên thuộc RM hiện tại. U1 sở hữu
`RmUser` ở chế độ demo và `CurrentRmProvider`; bản hiện thực đăng nhập thật và
token Bearer do U6 bổ sung (không đổi hình dạng giao diện của U1).

Bối cảnh: demo chạy cục bộ, chỉ dùng **dữ liệu demo** (KH/lead giả, NFR7); vì
vậy không yêu cầu ẩn thông tin cá nhân trước khi gửi dịch vụ AI. Tuy nhiên các
quy tắc không-lộ-bí-mật và không-log-dữ-liệu-nhạy-cảm vẫn áp dụng.

## Xác thực và danh tính RM

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR2.1 | `CurrentRmProvider.currentRm()` xác định RM theo chế độ chạy (BR1.3): chế độ `demo` trả RM mẫu cố định (ADR-007); chế độ `login` lấy RM từ token Bearer hợp lệ | Ở chế độ `demo`, mọi request không cần header `Authorization` vẫn có `currentRm`. Ở chế độ `login`, request thiếu/sai token trả `401 UNAUTHORIZED` (bản hiện thực login do U6) |
| NFR2.2 | Chuyển `demo → login` chỉ bằng cấu hình, không sửa mã các API của U1 (ADR-007) | Thay đổi profile/biến cấu hình kích hoạt chế độ login mà không cần sửa chữ ký `CurrentRmProvider`; test chứng minh cùng một controller chạy được cả hai chế độ |

## Phân quyền theo lead (bất biến FR1.4)

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR2.3 | Mọi API đọc/ghi `Lead`, `CallRecord`, `Activity` kiểm tra quyền sở hữu phía server trước khi trả dữ liệu (BR1.1) | Request của RM A tới tài nguyên của RM B trả `403 FORBIDDEN`, dữ liệu không đổi; có test cho `GET /api/leads/{id}`, `GET /api/leads/{id}/activities`, `POST /api/calls`, `POST /api/calls/{id}/ended` |
| NFR2.4 | Phản hồi `403` không tiết lộ tài nguyên của RM khác có tồn tại hay không; chỉ phân biệt `404` trong phạm vi tài nguyên của chính RM (BR1.2) | Tài nguyên của RM khác → `403` (không phải `404`); tài nguyên không tồn tại của chính RM → `404`; test khẳng định hai nhánh |
| NFR2.5 | Quyền sở hữu được kiểm bằng `LeadService.assertOwned`/`getOwnedLead` và `CallService.getOwnedCall` (I1, I2) chứ không dựa vào tham số do giao diện gửi | Các bước ghi (tạo/kết thúc cuộc gọi) đều đi qua kiểm quyền tập trung; test không thể vượt quyền bằng cách sửa payload |

## Bảo vệ bí mật đăng nhập

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR2.6 | `passwordHash` dùng BCrypt; mật khẩu thô không bao giờ được lưu hoặc ghi log (BR6.1) | Không có đường ghi mật khẩu thô vào DB/log; `RmUser` lưu dạng băm BCrypt; test khẳng định payload login (nếu có) không rò vào log |
| NFR2.7 | `passwordHash` không xuất hiện trong bất kỳ payload API trả về hay dòng log nào (BR6.1) | Mọi DTO trả về (`Rm`, `LeadDetail`…) không chứa `passwordHash`; test serialize khẳng định trường bị loại trừ |

## Quản lý khóa bí mật dịch vụ (guardrail Construction)

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR2.8 | Khóa API Anthropic/Google và mọi bí mật được đọc từ **biến môi trường**, không hard-code trong mã nguồn hay file cấu hình được commit | Mã U1 không chứa literal khóa; cấu hình nhạy cảm tham chiếu biến môi trường (ví dụ `${ANTHROPIC_API_KEY}`); kiểm bằng grep/review ở gate |
| NFR2.9 | Khuyến nghị dọn bí mật đã commit: khóa API thật hiện nằm trong `backend/src/main/resources/application.properties` và `.env` cần được chuyển sang biến môi trường và loại khỏi file commit | **Khuyến nghị, không chặn demo** (O5). Pass = ghi nhận khuyến nghị trong tài liệu + tech-stack-decisions; không bắt buộc hoàn tất để qua gate |

## Kiểm tra đầu vào tại biên

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR2.10 | Mọi dữ liệu nhập ở biên của U1 được kiểm hợp lệ trước khi xử lý (BR6.3): kiểu, bắt buộc, tập enum, định dạng | Dữ liệu sai → `400 VALIDATION_FAILED` kèm `details.field`; dùng Jakarta Bean Validation (`spring-boot-starter-validation`); test cho `POST /api/calls` (số điện thoại không hợp lệ → `400`, không tạo `CallRecord`, BR3.1) |
| NFR2.11 | Số điện thoại KH được kiểm hợp lệ trước khi tạo cuộc gọi (BR3.1, BR6.3) | Số không hợp lệ → `400 VALIDATION_FAILED`, không tạo `CallRecord` (AC4.4.2) |

## Không ghi log dữ liệu nhạy cảm

| ID | Yêu cầu | Tiêu chí pass/fail đo được |
|---|---|---|
| NFR2.12 | Log của U1 không chứa nội dung transcript, khóa/token hay `passwordHash`; chỉ ghi định danh tương quan `callId`/`rmId` và kết quả bước | Review log statement + test khẳng định không có trường nhạy cảm trong log (liên kết với NFR6/observability) |

## Ngoài phạm vi (ghi để tránh nhầm)

- Hiện thực token Bearer, cấp/thu hồi token, thời hạn phiên: U6 (C5).
- Ẩn thông tin cá nhân trước khi gửi AI: không áp dụng vì chỉ dùng dữ liệu demo (NFR7).
- Chống prompt injection khi dựng prompt: thuộc U3 (Copilot) và U4 (phân tích),
  nơi transcript/PDF được đưa vào prompt như dữ liệu.

## Sources

- `requirements.md` — NFR2 (bảo mật), NFR7 (dữ liệu demo), FR1.4, FR1.5, O5
- `rules.md` (U1) — BR1.1, BR1.2, BR1.3, BR3.1, BR6.1, BR6.3
- `entities.md` (U1) — `RmUser.passwordHash` (BCrypt, không lộ), ràng buộc quyền sở hữu
- `contract-summary.md` — quy ước xác thực/quyền (401/403/404), `ErrorEnvelope`, I1/I2
- `functional-spec.md` (U1) — WF1, WF2, WF4 (kiểm quyền)
- ADR-007 — chế độ demo `CurrentRmProvider`

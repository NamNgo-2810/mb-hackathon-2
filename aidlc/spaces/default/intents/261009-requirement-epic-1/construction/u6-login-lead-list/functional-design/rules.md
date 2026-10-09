# Business Rules — U6 `u6-login-lead-list`

Quy tắc nghiệp vụ của unit đăng nhập thật và danh sách lead theo RM. Khối YAML là
**nguồn chân lý**. Mỗi quy tắc có `id` theo định dạng `BR{nhóm}.{thứ tự}`. Nhóm:

- **BR1** — đăng nhập & xác thực (FR1.1, NFR2)
- **BR2** — phiên & token Bearer (ADR-007, contract C5, NFR2)
- **BR3** — chọn chế độ RM hiện tại (ADR-007, I1)
- **BR4** — danh sách lead theo RM (FR1.2, FR1.4)
- **BR5** — bảo mật & chống dò mật khẩu (NFR2)

Lưu ý ranh giới: bất biến quyền sở hữu lead (403 khi lead thuộc RM khác) do U1 thực
thi (BR1.1 của U1). U6 không nhân bản quy tắc đó; các quy tắc dưới đây là phần U6
thêm hoặc chuyên biệt hóa.

```yaml
rules:
  # ── BR1 — Đăng nhập & xác thực ──
  - id: BR1.1
    statement: "Đăng nhập chỉ thành công khi username tồn tại và password khớp passwordHash (BCrypt) của tài khoản RM mẫu."
    category: authorization
    applies_to: [RmUser, AuthSession]
    trigger: "POST /api/auth/login với { username, password }"
    logic: >
      IF tồn tại RmUser có username = yêu cầu.username
         AND bcrypt_verify(yêu cầu.password, RmUser.passwordHash) = true
      THEN tạo AuthSession mới và trả { token, expiresAt, rm }.
      ELSE trả 401 UNAUTHORIZED với thông điệp duy nhất 'Sai tên đăng nhập hoặc mật khẩu'.
    violation: "401 UNAUTHORIZED; không tạo phiên"
    source: [FR1.1, NFR2]
  - id: BR1.2
    statement: "Thông điệp đăng nhập sai không được tiết lộ sai ở trường nào (username hay password)."
    category: policy
    applies_to: [RmUser]
    trigger: "Đăng nhập thất bại vì bất kỳ lý do thông tin đăng nhập nào"
    logic: >
      IF username không tồn tại OR password sai
      THEN trả cùng một thông điệp 'Sai tên đăng nhập hoặc mật khẩu'; không nói rõ trường nào sai.
    violation: "Lộ trường sai (ví dụ 'không có tài khoản này') là vi phạm bảo mật/chống dò tài khoản"
    source: [FR1.1, NFR2]

  # ── BR2 — Phiên & token Bearer ──
  - id: BR2.1
    statement: "Mỗi lần đăng nhập đúng sinh một token Bearer ngẫu nhiên ≥ 32 byte; server chỉ lưu bản băm."
    category: constraint
    applies_to: [AuthSession]
    trigger: "Tạo AuthSession khi đăng nhập thành công (BR1.1)"
    logic: >
      THEN sinh token ngẫu nhiên ≥ 32 byte, trả token THÔ cho giao diện đúng một lần;
      lưu AuthSession.tokenHash = hash(token); đặt createdAt = hiện tại,
      expiresAt = createdAt + thời hạn phiên (mặc định 8 giờ, cấu hình được), revokedAt = null.
    violation: "Lưu token thô, hoặc token ngắn hơn 32 byte, là vi phạm"
    source: [contract C5, ADR-007, NFR2]
  - id: BR2.2
    statement: "Một request ở chế độ login hợp lệ khi mang token Bearer tương ứng một phiên còn hiệu lực."
    category: authorization
    applies_to: [AuthSession]
    trigger: "Mọi request ở chế độ login mang header Authorization: Bearer <token>"
    logic: >
      Tìm AuthSession theo hash(token).
      IF không tìm thấy OR AuthSession.revokedAt != null OR hiện tại >= AuthSession.expiresAt
      THEN 401 UNAUTHORIZED (giao diện đưa người dùng về S0, AC4.1.3).
      ELSE currentRm = RmUser của AuthSession.rmId.
    violation: "401 UNAUTHORIZED khi thiếu token, token sai, phiên hết hạn hoặc đã thu hồi"
    source: [contract C5, FR1.1, ADR-007]
  - id: BR2.3
    statement: "Đăng xuất thu hồi phiên hiện tại ngay lập tức."
    category: constraint
    applies_to: [AuthSession]
    trigger: "POST /api/auth/logout với token hợp lệ"
    logic: >
      THEN đặt AuthSession.revokedAt = hiện tại cho phiên của token; trả 204.
      Mọi request sau dùng token đó rơi vào nhánh 401 của BR2.2.
    violation: "Phiên vẫn dùng được sau logout là vi phạm"
    source: [contract C5]

  # ── BR3 — Chế độ RM hiện tại ──
  - id: BR3.1
    statement: "U6 cung cấp bản hiện thực chế độ login của CurrentRmProvider; chế độ được chọn bằng cấu hình lúc khởi động, không sửa unit khác."
    category: policy
    applies_to: [AuthSession, RmUser]
    trigger: "Khởi động ứng dụng và mọi lời gọi CurrentRmProvider.currentRm (I1)"
    logic: >
      IF cấu hình chế độ = login THEN dùng bản hiện thực của U6: currentRm lấy từ AuthSession của token (BR2.2).
      IF cấu hình chế độ = demo THEN dùng bản hiện thực demo của U1 (RM mẫu cố định); U6 không tham gia.
      Việc đổi chế độ chỉ qua cấu hình (dependency injection), không đổi mã các unit tiêu dùng.
    violation: "Phải sửa mã unit khác khi đổi chế độ là vi phạm ranh giới ADR-007"
    source: [ADR-007, contract I1]

  # ── BR4 — Danh sách lead theo RM ──
  - id: BR4.1
    statement: "GET /api/leads chỉ trả lead mà RM hiện tại phụ trách."
    category: authorization
    applies_to: [Lead]
    trigger: "GET /api/leads"
    logic: >
      THEN tập trả về = { Lead : Lead.ownerRmId = currentRm.rmId }.
      Lead của RM khác không bao giờ xuất hiện trong danh sách (AC4.2.1). Việc chặn truy cập
      lead đơn lẻ của RM khác (403) do U1 thực thi (BR1.1 của U1).
    violation: "Trả lead của RM khác trong danh sách là vi phạm"
    source: [FR1.2, FR1.4]
  - id: BR4.2
    statement: "Danh sách lead sắp theo mức ưu tiên, rồi theo hạn SLA tăng dần."
    category: calculation
    applies_to: [Lead]
    trigger: "GET /api/leads"
    logic: >
      Sắp xếp theo priority (KHAI_THAC_NGAY > HOT_LEAD > THUONG);
      cùng mức ưu tiên thì theo slaDueAt tăng dần (hạn gần hơn đứng trước);
      slaDueAt null xếp sau các lead cùng mức ưu tiên có hạn.
    violation: "Sai thứ tự ưu tiên hoặc SLA là vi phạm"
    source: [FR1.2]
  - id: BR4.3
    statement: "Danh sách rỗng được biểu thị bằng trạng thái trống rõ ràng, không để trống im lặng."
    category: constraint
    applies_to: [Lead]
    trigger: "GET /api/leads trả mảng rỗng"
    logic: >
      IF RM hiện tại không có lead nào THEN API trả mảng rỗng []
      AND giao diện hiển thị 'Chưa có tiềm năng bán' (AC4.2.3).
    violation: "Hiển thị trống không có thông báo là vi phạm"
    source: [FR1.2]

  # ── BR5 — Bảo mật & chống dò mật khẩu ──
  - id: BR5.1
    statement: "Mật khẩu chỉ lưu dạng băm BCrypt; không lưu, không log mật khẩu thô hay token thô."
    category: policy
    applies_to: [RmUser, AuthSession]
    trigger: "Mọi thao tác đọc/ghi RmUser hoặc AuthSession, mọi dòng log"
    logic: >
      THEN passwordHash chỉ ở dạng BCrypt; token chỉ lưu dạng băm (tokenHash).
      Loại trừ passwordHash, token thô và tokenHash khỏi mọi payload trả về và log (NFR2).
    violation: "Lộ/log mật khẩu thô, token thô hoặc tokenHash là vi phạm bảo mật"
    source: [NFR2]
  - id: BR5.2
    statement: "(Tùy chọn cho demo) Giảm nhẹ dò mật khẩu bằng độ trễ nhỏ hoặc bộ đếm lần sai."
    category: policy
    applies_to: [AuthSession, RmUser]
    trigger: "Nhiều lần đăng nhập sai liên tiếp cho cùng username hoặc cùng nguồn"
    logic: >
      THEN có thể thêm độ trễ cố định nhỏ cho phản hồi thất bại, hoặc đếm số lần sai và
      tạm chặn ngắn. Đây là biện pháp OPTIONAL cho demo, không bắt buộc cho tiêu chí nghiệm thu.
    violation: "Không áp dụng không bị coi là lỗi; chỉ là khuyến nghị"
    source: [NFR2]
```

## Bảng tóm tắt quy tắc

| ID | Loại | Áp dụng cho | Tóm tắt | Nguồn |
|---|---|---|---|---|
| BR1.1 | authorization | RmUser, AuthSession | Đăng nhập đúng khi username tồn tại + password khớp BCrypt | FR1.1, NFR2 |
| BR1.2 | policy | RmUser | Sai thông tin → một thông điệp "Sai tên đăng nhập hoặc mật khẩu", không lộ trường | FR1.1, NFR2 |
| BR2.1 | constraint | AuthSession | Token Bearer ngẫu nhiên ≥ 32 byte, server chỉ lưu bản băm; hạn 8 giờ cấu hình được | C5, ADR-007, NFR2 |
| BR2.2 | authorization | AuthSession | Request login hợp lệ khi token khớp phiên chưa thu hồi, chưa hết hạn; ngược lại 401 | C5, FR1.1 |
| BR2.3 | constraint | AuthSession | Logout thu hồi phiên ngay (revokedAt) → 204 | C5 |
| BR3.1 | policy | AuthSession, RmUser | U6 là bản login của CurrentRmProvider; chọn chế độ bằng cấu hình, không sửa unit khác | ADR-007, I1 |
| BR4.1 | authorization | Lead | `GET /api/leads` chỉ trả lead của RM hiện tại | FR1.2, FR1.4 |
| BR4.2 | calculation | Lead | Sắp theo ưu tiên (KHAI_THAC_NGAY > HOT_LEAD > THUONG) rồi slaDueAt tăng dần | FR1.2 |
| BR4.3 | constraint | Lead | Danh sách rỗng → "Chưa có tiềm năng bán" | FR1.2 |
| BR5.1 | policy | RmUser, AuthSession | Không lộ/log mật khẩu thô, token thô, tokenHash; mật khẩu băm BCrypt | NFR2 |
| BR5.2 | policy | AuthSession, RmUser | (Optional demo) độ trễ nhỏ / đếm lần sai để giảm dò mật khẩu | NFR2 |

## Sources

- `requirements.md` — FR1.1 (đăng nhập), FR1.2 (danh sách lead theo RM), FR1.4 (quyền sở hữu), NFR2 (bảo mật)
- `user-stories/stories.md` — AC4.1.1–AC4.1.3, AC4.2.1–AC4.2.3
- `contract-summary.md` — C5 (`/api/auth/login`, `/api/auth/logout`, `/api/leads`), I1 (`CurrentRmProvider`, `LeadService`), quy ước xác thực Bearer
- `decisions.md` — ADR-002 (CORS), ADR-007 (chế độ demo/login chọn bằng cấu hình)
- `u1-lead-call-foundation/functional-design/rules.md` — BR1.1/BR1.2/BR1.3/BR6.1 của U1 mà U6 dựa vào (quyền sở hữu, danh tính RM, không lộ passwordHash)

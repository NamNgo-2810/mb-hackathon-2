# Entities — U6 `u6-login-lead-list`

Mô hình thực thể cho unit đăng nhập thật và danh sách lead theo RM (US4.1, US4.2).
Nguồn: `components.md` (sở hữu thực thể: AuthComponent, LeadComponent),
`decisions.md` (ADR-002, ADR-007), `contract-summary.md` (C5, I1),
`requirements.md` (FR1.1, FR1.2, FR1.4, NFR2).

U6 **không định nghĩa lại** `RmUser`, `Customer`, `Lead` — các thực thể này do U1
sở hữu và U6 chỉ tham chiếu/mở rộng cách dùng (xem
`u1-lead-call-foundation/functional-design/entities.md`). U6 **sở hữu một thực thể
mới**: `AuthSession` (phiên đăng nhập gắn với token Bearer). U6 cung cấp bản hiện
thực chế độ `login` của giao diện `CurrentRmProvider` (I1) mà U1 định nghĩa; việc
chọn chế độ `demo` hay `login` thực hiện bằng cấu hình lúc khởi động (dependency
injection), không sửa mã unit khác (ADR-007).

Khối YAML dưới đây là **nguồn chân lý** của mô hình dữ liệu U6. Mô tả
technology-agnostic: không gắn kiểu SQL, không framework. `logical type` là kiểu
logic (string, integer, datetime, boolean, enum, text).

```yaml
entities:
  # ── Thực thể MỚI do U6 sở hữu ──
  - name: AuthSession
    description: >
      Phiên đăng nhập của một RM ở chế độ login (ADR-007). Mỗi lần đăng nhập đúng
      sinh một token Bearer ngẫu nhiên; server chỉ lưu BẢN BĂM của token, không bao
      giờ lưu token thô (NFR2). Phiên có hạn (đề xuất 8 giờ cho demo, cấu hình được).
      Đăng xuất thu hồi phiên. Token hết hạn hoặc bị thu hồi -> 401 và giao diện quay
      về màn đăng nhập S0 (AC4.1.3).
    identifier: sessionId
    attributes:
      - { name: sessionId, type: string, required: true, unique: true, constraint: "định danh phiên ở server" }
      - { name: rmId, type: string, required: true, references: "RmUser.rmId", constraint: "RM sở hữu phiên; là danh tính trả về cho CurrentRmProvider" }
      - { name: tokenHash, type: string, required: true, unique: true, constraint: "băm (ví dụ SHA-256) của token Bearer ≥ 32 byte; KHÔNG lưu token thô, KHÔNG trả ra API/log (NFR2)" }
      - { name: createdAt, type: datetime, required: true, constraint: "ISO-8601 có offset +07:00" }
      - { name: expiresAt, type: datetime, required: true, constraint: "createdAt + thời hạn phiên (mặc định 8 giờ, cấu hình được)" }
      - { name: revokedAt, type: datetime, required: false, constraint: "được đặt khi logout; null khi phiên còn hiệu lực" }
    entity_constraints:
      - "tokenHash là duy nhất; không hai phiên dùng chung một token"
      - "token thô chỉ tồn tại trong phản hồi đăng nhập (trả cho giao diện), không bao giờ lưu lại"
      - "phiên còn hiệu lực KHI revokedAt = null AND thời điểm hiện tại < expiresAt"
      - "tokenHash và token thô không bao giờ xuất hiện trong log"
    relationships:
      - { to: RmUser, cardinality: "*..1", direction: "nhiều AuthSession thuộc một RmUser", note: "một RM có thể có nhiều phiên theo thời gian; chỉ phiên chưa thu hồi và chưa hết hạn là hợp lệ" }

referenced_external_entities:
  - name: RmUser
    owned_by: AuthComponent (U1 định nghĩa hình dạng tối thiểu; U6 dùng ở chế độ login)
    identifier: rmId
    attributes_used: [rmId, username, passwordHash, displayName]
    note: >
      U6 xác thực username + password đối chiếu passwordHash (BCrypt). U6 KHÔNG đổi
      hình dạng RmUser; chỉ yêu cầu passwordHash khác null cho các tài khoản RM mẫu
      được seed (FR1.1). passwordHash không bao giờ trả ra API hay ghi log (NFR2, BR
      của U1 BR6.1).
  - name: Lead
    owned_by: LeadComponent (U1)
    identifier: leadId
    attributes_used: [leadId, leadCode, customerId, ownerRmId, productInterest, source, priority, rankScore, slaDueAt, stage]
    note: >
      U6 chỉ ĐỌC danh sách Lead của RM hiện tại qua `GET /api/leads` (C5), lọc theo
      ownerRmId = currentRm.rmId và sắp xếp (xem rules.md BR của U6). Bất biến quyền
      sở hữu (ownerRmId) và kiểm tra 403 nằm ở U1 (BR1.1); U6 dựa vào đó.
  - name: Customer
    owned_by: LeadComponent (U1)
    identifier: customerId
    attributes_used: [customerId, fullName]
    note: "Danh sách lead hiển thị customerName lấy từ Customer; U6 không ghi Customer."
```

## Tóm tắt tập thực thể

U6 là unit nhỏ (kind: service) với một thực thể mới và hai thực thể tham chiếu:

- **AuthSession** (MỚI, do U6 sở hữu) — phiên đăng nhập ở chế độ `login`. Giữ `tokenHash`
  (chỉ bản băm của token Bearer, không bao giờ là token thô), `expiresAt` (hạn 8 giờ
  đề xuất, cấu hình được) và `revokedAt` (đặt khi đăng xuất). Phiên hợp lệ khi chưa thu
  hồi và chưa hết hạn; ngược lại trả 401 và giao diện về S0 (AC4.1.3).
- **RmUser** (tham chiếu, U1 sở hữu) — U6 xác thực `username` + `password` đối chiếu
  `passwordHash` BCrypt của các tài khoản RM mẫu được seed. `passwordHash` không bao giờ
  lộ ra API/log (NFR2).
- **Lead** / **Customer** (tham chiếu, U1 sở hữu) — U6 chỉ đọc danh sách lead của RM hiện
  tại qua `GET /api/leads`, lọc theo `ownerRmId` và sắp theo ưu tiên rồi hạn SLA.

Ranh giới sở hữu: U6 chỉ thêm `AuthSession` và cung cấp bản hiện thực `login` của
`CurrentRmProvider` (I1). Mọi kiểm tra quyền sở hữu lead (403) vẫn do U1 thực thi
(BR1.1 của U1); U6 không nhân bản logic đó mà gọi lại qua giao diện Java của U1.

## Sources

- `components.md` — AuthComponent (chế độ login), LeadComponent (danh sách lead theo RM), sở hữu RmUser/Lead/Customer
- `decisions.md` — ADR-002 (phiên qua CORS), ADR-007 (chế độ demo/login chọn bằng cấu hình)
- `contract-summary.md` — C5 (`/api/auth/login`, `/api/auth/logout`, `/api/leads`), I1 (`CurrentRmProvider`, `LeadService`)
- `requirements.md` — FR1.1 (đăng nhập), FR1.2 (danh sách lead theo RM), FR1.4 (quyền sở hữu), NFR2 (bảo mật, băm mật khẩu)
- `u1-lead-call-foundation/functional-design/entities.md` — hình dạng RmUser/Lead/Customer mà U6 tham chiếu

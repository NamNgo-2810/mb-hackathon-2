## Review

**Verdict:** READY
**Reviewer:** aidlc-architecture-reviewer-agent
**Date:** 2026-10-09T09:03:19Z
**Iteration:** 1

### Findings

| ID | Severity | Location | Finding | Required action | Status |
|---|---|---|---|---|---|
| R-01 | Medium | aidlc/spaces/default/intents/261009-requirement-epic-1/construction/u6-login-lead-list/functional-design/entities.md > referenced_external_entities > RmUser | Thiết kế U6 nói "RmUser do U1 sở hữu, U1 định nghĩa hình dạng tối thiểu" và chỉ coi RmUser là thực thể tham chiếu. Nhưng kế hoạch chia unit (unit-of-work.md, mục U6) lại giao hẳn RmUser, mật khẩu băm BCrypt và tài khoản RM mẫu cho chính U6, còn U1 chỉ làm AuthComponent chế độ demo (trả RM mẫu cố định, không tạo/seed tài khoản đăng nhập). Hai tài liệu mâu thuẫn về việc ai tạo bảng RmUser, ai seed tài khoản và cột passwordHash. Khi hiện thực, U6 và U1 dễ cùng cho rằng bên kia làm, dẫn tới thiếu bảng/seed hoặc làm trùng. | Chốt rõ trong U6 rằng ai sở hữu định nghĩa thực thể RmUser và dữ liệu seed tài khoản (gồm cột passwordHash BCrypt): nếu U6 là bên tạo bảng và seed thì sửa lại mô tả "U1 sở hữu/định nghĩa" cho khớp kế hoạch; nếu U1 là bên tạo thì nêu rõ U6 phụ thuộc vào bảng/seed đó và điều kiện phải có trước khi bật chế độ login. | New |
| R-02 | Low | aidlc/spaces/default/intents/261009-requirement-epic-1/construction/u6-login-lead-list/functional-design/entities.md > referenced_external_entities > RmUser > note | Ghi chú viện dẫn "BR của U1 BR6.1" cho quy tắc không lộ passwordHash, nhưng cùng unit U6 đã có BR5.1 nói đúng việc này. Người hiện thực không rõ phải theo quy tắc nào của unit nào, trong khi U6 vẫn tự đọc/ghi RmUser khi đăng nhập. | Nêu rõ U6 tự chịu BR5.1 của chính mình cho mọi đường trả về/log của luồng đăng nhập; chỉ viện dẫn BR6.1 của U1 như quy ước dùng chung, không thay cho quy tắc của U6. | New |
| R-03 | Low | aidlc/spaces/default/intents/261009-requirement-epic-1/construction/u6-login-lead-list/functional-design/functional-spec.md > mục 1 "Sở hữu đường dẫn" | Thiết kế dựa vào giả định "literal path (/demo, /next) được ưu tiên so với pattern {leadId} khi định tuyến" để tránh nhập nhằng giữa GET /api/leads của U6 và các path của U1, nhưng không nêu điều kiện kỹ thuật để điều đó đúng (thứ tự đăng ký route, hoặc khai báo tường minh). Nếu khung định tuyến không bảo đảm ưu tiên literal, GET /api/leads/demo hoặc /next có thể bị U6 bắt nhầm. | Ghi rõ ràng buộc để literal path thắng pattern (ví dụ U1 và U6 cùng dùng một quy ước đăng ký route, hoặc path collection của U6 được giới hạn không khớp sub-path), để người hiện thực không phải suy đoán. | New |

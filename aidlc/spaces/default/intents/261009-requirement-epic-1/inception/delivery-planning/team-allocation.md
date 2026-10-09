# Team Allocation

Theo `bolt-plan.md`, mỗi **Bolt** là một lượt xây trọn một unit, đi từ thiết kế qua viết mã đến kiểm thử. **Mob** là nhóm người cùng làm chung một phần việc.

Phạm vi classic không có bước Team Formation. Người dùng đã chọn làm trong một phiên (`delivery-planning-questions.md`, Q5=A). Vì vậy **mọi Bolt đều do aidlc-developer-agent (AI) thực hiện, tuần tự**, và người dùng duyệt ở từng điểm dừng.

| Bolt | Unit | Người thực hiện | Người duyệt / chạy test |
|---|---|---|---|
| 1 | U1 `u1-lead-call-foundation` | aidlc-developer-agent | Người dùng: tự chạy test backend bằng Maven wrapper trong IntelliJ |
| 2 | U2 `u2-llm-client` | aidlc-developer-agent | Người dùng |
| 3 | U3 `u3-copilot` | aidlc-developer-agent | Người dùng |
| 4 | U5 `u5-task-calendar` | aidlc-developer-agent | Người dùng |
| 5 | U4 `u4-call-analysis-result` | aidlc-developer-agent | Người dùng |
| 6 | U6 `u6-login-lead-list` | aidlc-developer-agent | Người dùng |

- **Quyền sở hữu unit**: một phiên (solo), không chia unit cho nhóm. Câu Q4=B ban đầu đã được thay bằng Q5=A.
- **Cách chạy**: theo từng bước và tuần tự. Thiết kế tất cả unit trước, sau đó viết mã từng unit theo thứ tự Bolt.
- **Chỉ có một nhóm** nên không cần bảng điều phối giữa nhiều nhóm (Program Board).
- **Phần việc của người dùng** ngoài việc duyệt:
  - cung cấp khóa API Anthropic và tài liệu chính sách (xem `external-dependency-map.md`);
  - chạy test Java bằng IntelliJ;
  - chạy `git init` trước lần commit đầu tiên.

## Sources

- `bolt-plan.md`, `delivery-planning-questions.md`, `team-practices.md`, `unit-of-work.md`

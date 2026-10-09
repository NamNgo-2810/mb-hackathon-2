# Code Generation Plan Approval

AI-DLC built this plan without asking because plan approval is off for this
piece of work. This file is the record and asks nothing; to look at a plan
before it is built, ask in chat.

## Plan Approval

Built without asking: plan approval is off for this piece of work (set by you).

- Builds: Copilot tra cứu chính sách (CopilotComponent, hợp đồng C2) trong package `com.mb.callassistant.copilot` — nạp + lập chỉ mục tài liệu text/md khi khởi động (`PolicyDocument`/`DocumentChunk`)...
- Touches: thêm package mới `backend/src/main/java/com/mb/callassistant/copilot/**` và controller `web/CopilotController.java`; mở rộng `config/AppProperties.java` (khối `Copilot`); thêm `app.copilot...
- Tests: JUnit5 + Mockito (mock `LlmClient`, thư mục tài liệu tạm) cho mọi lớp (nạp tài liệu, tìm từ khoá không dấu, nhánh rỗng, kiểm chứng trích dẫn, lỗi/timeout LLM → 502, JSON sai cấu trúc, log OK...

Full plan: aidlc/spaces/default/intents/261009-requirement-epic-1/construction/u3-copilot/code-generation/code-generation-plan.md
Test instructions: aidlc/spaces/default/intents/261009-requirement-epic-1/construction/u3-copilot/code-generation/unit-test-instructions.md

[Approval Fingerprint]: sha256:v3:858fe9070deff2de5d5080bee7ea8688ed874ce08298c2d80c42552b4dce2d05
[Planned Source]: e3976b897ca38d47c98d43dbcc590b970116ce344ca1092f058baec2548d7d09


[Answer]: Plan approval off

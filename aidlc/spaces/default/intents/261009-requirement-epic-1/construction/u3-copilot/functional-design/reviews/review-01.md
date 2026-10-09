## Review

**Verdict:** READY
**Reviewer:** aidlc-architecture-reviewer-agent
**Date:** 2026-10-09T08:56:24Z
**Iteration:** 1

### Findings

| ID | Severity | Location | Finding | Required action | Status |
|---|---|---|---|---|---|
| R-01 | Low | aidlc/spaces/default/intents/261009-requirement-epic-1/construction/u3-copilot/functional-design/rules.md > BR5.2 và functional-spec.md > WF2 bước 7 | Khi cổng LLM báo "chưa cấu hình" (NOT_CONFIGURED), U3 gộp luôn vào nhánh trả 502 AI_ERROR giống mọi lỗi khác. Thiết kế của U2 (`u2-llm-client/functional-spec.md` WF3) lại mô tả riêng thông điệp panel "AI chưa được cấu hình" cho tình huống này, hàm ý một lời nhắc khác với lỗi gọi LLM thông thường. Hành vi vẫn chạy được (cả hai đều hiện lỗi trong panel), nhưng thông điệp tiếng Việt người dùng thấy có thể không phân biệt được "chưa cấu hình khóa" với "LLM lỗi/timeout". | Làm rõ trong BR5.2 hoặc WF2 bước 7 rằng thông điệp 502 cho NOT_CONFIGURED nói đúng là "AI chưa được cấu hình", để khớp hướng dẫn của U2 và giúp người vận hành demo biết cần đặt khóa API. | New |
| R-02 | Low | aidlc/spaces/default/intents/261009-requirement-epic-1/construction/u3-copilot/functional-design/traceability.json > coverage AC1.3.2 | AC1.3.2 (cột "Lịch sử tra cứu" liệt kê mới→cũ, bấm để mở lại câu trả lời) được gán target BR6.2, nhưng BR6.2 chỉ nói về ngữ cảnh phiên ≤ 10 lượt gửi lên server — phần hiển thị và thao tác mở lại của lịch sử là hành vi phía giao diện (WF4), không có quy tắc nghiệp vụ server tương ứng. Việc gán này hợp lý như một proxy, nhưng phần UI thuần của AC1.3.2 chưa có nơi đặc tả hành vi rõ ràng ngoài một dòng ở WF4. | Cân nhắc ghi chú rõ AC1.3.2 là hành vi giao diện (do WF4 bước 1 phủ) hoặc bổ sung một dòng mô tả tương tác lịch sử tra cứu, để truy vết không hiểu nhầm BR6.2 là nơi hiện thực đầy đủ AC này. | New |
| R-03 | Low | aidlc/spaces/default/intents/261009-requirement-epic-1/construction/u3-copilot/functional-design/rules.md > BR5.1 ("timeoutMs=30000") so với contract-summary.md > I3 (`timeout`) | BR5.1 và WF2 gọi `LlmClient.complete(... timeoutMs=30000)`, trong khi hợp đồng I3 đặt tên tham số là `timeout`. Khác biệt chỉ là tên tham số minh họa, không đổi ngữ nghĩa (cùng là timeout 30 giây, khớp FR3.6/NFR1), nhưng dễ gây lẫn khi hiện thực nếu đọc đúng từng chữ. | Dùng đúng tên tham số của I3 (`timeout`) trong snippet minh họa, hoặc ghi chú rằng đây là tên logic, để lập trình viên không tạo tham số sai tên. | New |

### Validation Tool Results

| Tool | Result | Interpretation |
|---|---|---|
| Kiểm tra BRx.y trong traceability so với rules.md | PASS | Mọi target OK (BR2.1, BR2.2, BR3.1, BR3.2, BR3.3, BR4.1, BR5.1, BR5.2, BR6.1, BR6.2, BR1.1, BR1.2, BR2.3) đều tồn tại trong rules.md; không có target treo. |
| Phủ AC của US1.2/US1.3 | PASS | 8/8 AC (AC1.2.1–AC1.2.5, AC1.3.1–AC1.3.3) đều có dòng coverage trạng thái OK. |
| Rule orphan (reverse) | PASS | BR1.3, BR4.2, BR5.3, BR7.1, BR7.2, BR7.3 được giải thích N/A hợp lý; mọi BR còn lại đều có AC phủ. Không còn orphan. |
| Nhất quán C2 (502 AI_ERROR, câu "không tìm thấy", history ≤ 10) | PASS | Câu cố định, mã 502 AI_ERROR, history maxItems 10, citations yêu cầu documentTitle + effectiveDate đều khớp giữa rules/spec/entities và C2. |
| Nhất quán ADR-003 (keyword, không gọi LLM khi rỗng) | PASS | BR2.1/BR2.2 (khớp có dấu + không dấu, topK), BR3.1 (rỗng → không gọi LLM) khớp ADR-003. |
| Chống prompt injection (AC1.2.5) | PASS | BR2.3 + WF2 bước 6 đặt đoạn tài liệu trong khối DATA phân tách là dữ liệu, systemPrompt là chỉ dẫn duy nhất; thêm lớp phòng thủ ở I3 (BR5.1 của U2). |

### Summary

Thiết kế của U3 chặt chẽ và lập trình viên có thể hiện thực mà không cần hỏi thêm kiến trúc: luồng hỏi đáp, máy trạng thái, mô hình thực thể và quy tắc nghiệp vụ đều nhất quán với C2, ADR-003 và hợp đồng I3; quy ước metadata được nêu rõ là đề xuất cho demo (nguồn tài liệu người dùng còn hoãn), đúng như mong đợi. Ba quan sát đều ở mức Low và mang tính làm rõ (thông điệp NOT_CONFIGURED, cách truy vết AC1.3.2 thuần UI, tên tham số timeout), không chặn việc duyệt.

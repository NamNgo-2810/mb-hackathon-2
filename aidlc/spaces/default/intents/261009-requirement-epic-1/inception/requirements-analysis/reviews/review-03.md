## Review

**Verdict:** READY
**Reviewer:** aidlc-product-lead-agent
**Date:** 2026-10-09T06:01:57Z
**Iteration:** 1

### Findings

| ID | Severity | Location | Finding | Required action | Status |
|---|---|---|---|---|---|
| R-01 | Medium | aidlc/spaces/default/intents/261009-requirement-epic-1/inception/requirements-analysis/requirements.md > A5 và FR4.4 | Giả định A5 nói "điểm tổng /100 = tổng 5 tiêu chí × 2", nhưng mockup màn 4 hiển thị tổng 92/100 trong khi 5 tiêu chí (10 + 10 + 9.5 + 9 + 9.5 = 48) × 2 = 96, không khớp 92. Công thức tổng điểm vì vậy chưa chắc chắn, và chưa quyết dứt điểm giữa A5 (theo mockup) với Q6=A (trung bình) — vẫn để mở ở O3 | Chốt rõ công thức tính điểm tổng từ 5 tiêu chí (tổng ×2, hay trung bình, hay cách khác), xác nhận lại với người dùng rồi gỡ O3; nêu rõ con số mockup chỉ là minh họa nếu không dùng để ràng buộc | Resolved (reviewer) |
| R-02 | Medium | aidlc/spaces/default/intents/261009-requirement-epic-1/inception/requirements-analysis/requirements.md > FR4.4 (ngưỡng hạng) và FR4.5 (trường "Nguồn tiền") | Một số giá trị định lượng/nhãn trong yêu cầu không truy ngược được về mockup hay epic hay [Qn]: ngưỡng hạng A≥85/B70–84/C50–69/D<50 không xuất hiện trong mockup (mockup chỉ ghi "Hạng A - Xuất sắc"); tên trường "Nguồn tiền" so với mockup là "Nguồn tiền luân chuyển". Đây là các điểm người dùng nói sẽ chỉnh sau [Q5], nhưng hiện đang trình bày như giá trị chốt | Đánh dấu rõ ngưỡng hạng là đề xuất của nhóm (như A5/A1 đã làm cho tiêu chí/trường), hoặc dẫn nguồn; đồng bộ nhãn trường với mockup hoặc ghi chú lý do đặt tên khác | Resolved (reviewer) |
| R-03 | Low | aidlc/spaces/default/intents/261009-requirement-epic-1/inception/requirements-analysis/requirements.md > NFR1 (Copilot ≤ 15s / báo lỗi > 30s) và FR3.6 | NFR1 đặt ngưỡng Copilot trả lời ≤ 15 giây (≥90%) và báo lỗi khi quá 30 giây, nhưng hai con số này không truy về epic hay một [Qn] nào — có vẻ do nhóm tự đặt mà không ghi là giả định | Ghi rõ đây là ngưỡng do nhóm đề xuất cho demo (giả định), hoặc xác nhận với người dùng, để tránh hiểu nhầm là yêu cầu đã được chốt | Resolved (reviewer) |
| R-04 | Low | aidlc/spaces/default/intents/261009-requirement-epic-1/inception/requirements-analysis/requirements.md > FR4.8 và FR4.9 | Luồng chính đã có fallback tốt, nhưng còn vài góc chưa phủ: khi fallback (không có ghi âm/LLM lỗi) thì FR4.4 (chấm điểm) và FR4.3 (transcript/audio) hiển thị ra sao chưa nói rõ; và khi RM chọn "Liên hệ khách hàng tiếp theo" ở FR4.9 mà danh sách không còn lead nào thì xử lý thế nào chưa định nghĩa | Bổ sung tiêu chí cho trạng thái cột điểm/hội thoại khi fallback, và cho nhánh "không còn lead kế tiếp" ở FR4.9 (hoặc ghi là ngoài phạm vi có lý do) | Resolved (reviewer) |
| R-05 | Low | aidlc/spaces/default/intents/261009-requirement-epic-1/inception/requirements-analysis/requirements.md > O5 và NFR2 | O5 ghi nhận khóa API thật đang nằm trong `backend/src/main/resources/application.properties` và `.env` nhưng chỉ coi việc xử lý là "khuyến nghị". Đây là rủi ro lộ bí mật thực tế trong repo (dù team-practices để mức relaxed), người dùng nên biết rõ khi duyệt | Nêu rõ rủi ro và một hành động tối thiểu (ví dụ xoay khóa, gỡ khỏi file được commit) trước khi sang Construction, hoặc xác nhận chấp nhận rủi ro có chủ đích | Unresolved |
| R-06 | Low | aidlc/spaces/default/intents/261009-requirement-epic-1/inception/requirements-analysis/requirements.md > mục 7 (Câu hỏi còn mở) O4 | O4 vẫn gọi "Câu C-HIST của epic ... mục tóm tắt AI trong Lịch sử hoạt động". Nội dung đúng, nhưng vì đã đổi số US nên nên nêu kèm số US mới (US-03) để người đọc không phải tra ngược về số epic. Không phải lỗi nội dung, chỉ là nhất quán cách gọi tên sau khi đánh số lại. | Thêm chỉ dẫn US-03 bên cạnh tham chiếu C-HIST ở O4, hoặc xác nhận giữ nguyên vì O4 vốn trích thẳng ký hiệu của epic. | Unresolved |

> R-01 Reviewer note: FR4.4 nay chốt công thức điểm tổng (LLM chấm từng tiêu chí 0–10 bước 0,5; điểm tổng = tổng 5 tiêu chí × 2, làm tròn, luôn trong 0–100; hệ thống tự tính chứ không lấy số tổng của LLM) kèm ví dụ, và ghi rõ các con số trên mockup chỉ minh họa. O3 chuyển sang "Đã chốt". Không còn chỗ mở.

> R-02 Reviewer note: FR4.4 đánh dấu ngưỡng hạng A/B/C/D là "đề xuất cho demo, chỉnh được bằng cấu hình"; FR4.5 ghi danh sách trường form là "đề xuất cho demo, người dùng chỉnh sau"; nhãn trường đã đổi thành "Nguồn tiền luân chuyển", khớp đúng mockup `4-lua-chon-dieu-huong.html` (field "Nguồn tiền luân chuyển:").

> R-03 Reviewer note: NFR1 nay ghi rõ hai ngưỡng Copilot (≤ 15 giây; quá 30 giây báo lỗi) là "do nhóm đề xuất cho demo (người dùng xác nhận ở cổng duyệt) và chỉnh được bằng cấu hình". FR3.6 trỏ về NFR1 nhất quán.

> R-04 Reviewer note: FR4.8 bổ sung bảng hiển thị từng cột theo 3 loại lỗi (không có ghi âm / STT lỗi / LLM lỗi) kèm tiêu chí nghiệm thu; FR4.9 bổ sung nhánh "không còn lead kế tiếp" với tiêu chí nghiệm thu (báo "Bạn đã liên hệ hết tiềm năng bán cần xử lý" và mở danh sách).

> R-05 Reviewer note: Người dùng không yêu cầu đổi ở cổng này. O5 vẫn chỉ ghi khóa API thật đang nằm trong `backend/src/main/resources/application.properties` và `.env` ở mức "khuyến nghị" (lưu ý vận hành, không phải yêu cầu). NFR2 đã bắt khóa Anthropic mới lấy từ biến môi trường, nhưng rủi ro khóa cũ đang bị commit vẫn chưa có hành động tối thiểu nào được chốt trong tài liệu. Người dùng cân nhắc có chấp nhận rủi ro này cho demo hay không trước khi duyệt.

> R-06 Reviewer note: O4 vẫn mô tả hành vi của C-HIST (kèm liên kết mở màn "Kết quả cuộc gọi", chỉ RM giữ lead xem được) nhưng không nêu số US-03 cạnh tên câu C-HIST. Đây là ghi chú truy vết nhỏ; required action ban đầu cho phép "giữ nguyên", nên không chặn duyệt.

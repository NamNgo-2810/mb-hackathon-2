## Review

**Verdict:** READY
**Reviewer:** aidlc-product-lead-agent
**Date:** 2026-10-09T05:49:07Z
**Iteration:** 1

Đây là một lượt rà soát tư vấn (advisory) sau khi người dùng yêu cầu **đánh số lại user story**. Trọng tâm: kiểm tra việc đánh số lại có nhất quán xuyên suốt tài liệu không, và không có nội dung yêu cầu nào bị mất.

### Phát hiện về việc đánh số lại (nội dung thay đổi)

Việc đánh số lại **nhất quán và không mất nội dung**:

- Mục 1 (Phạm vi) nêu rõ cách đánh số mới và ánh xạ chéo với `epic 1.md`: US-01 = Copilot trong lúc gọi (là US-02 của epic), US-02 = màn "Kết quả cuộc gọi" 3 cột (là US-01 của epic), US-03 giữ nguyên. Ánh xạ này khớp đúng với mô tả 3 US trong `epic 1.md` mục 4.2.
- Tiêu đề các nhóm chức năng dùng số mới nhất quán: FR3 = "US-01: AI Sales Knowledge Copilot (trong lúc gọi)", FR4 = "US-02: Kết quả cuộc gọi", FR5 = "US-03: Lịch sử hoạt động + Lịch làm việc". FR2 ghi đúng là "khung chứa US-01" (màn thực hiện cuộc gọi chứa panel Copilot).
- Câu dặn đối chiếu "Khi đối chiếu với `epic 1.md` thì đổi chéo US-01 ↔ US-02" là chính xác và giúp người đọc không nhầm.
- Không thấy nội dung yêu cầu nào bị lược bỏ do thao tác đánh số: toàn bộ các trường keyin, 5 tiêu chí chấm điểm, pipeline, fallback, màn Lịch làm việc vẫn còn nguyên.

Hai điểm nhỏ người duyệt nên lưu ý (xem bảng New findings): một tham chiếu còn dùng số epic cũ trong mục Sources, và việc đánh số không khắc phục bất kỳ phát hiện mở nào trước đó — mọi phát hiện R-01..R-05 vẫn còn nguyên.

### Findings

| ID | Severity | Location | Finding | Required action | Status |
|---|---|---|---|---|---|
| R-01 | Medium | aidlc/spaces/default/intents/261009-requirement-epic-1/inception/requirements-analysis/requirements.md > A5 và FR4.4 | Giả định A5 nói "điểm tổng /100 = tổng 5 tiêu chí × 2", nhưng mockup màn 4 hiển thị tổng 92/100 trong khi 5 tiêu chí (10 + 10 + 9.5 + 9 + 9.5 = 48) × 2 = 96, không khớp 92. Công thức tổng điểm vì vậy chưa chắc chắn, và chưa quyết dứt điểm giữa A5 (theo mockup) với Q6=A (trung bình) — vẫn để mở ở O3 | Chốt rõ công thức tính điểm tổng từ 5 tiêu chí (tổng ×2, hay trung bình, hay cách khác), xác nhận lại với người dùng rồi gỡ O3; nêu rõ con số mockup chỉ là minh họa nếu không dùng để ràng buộc | Unresolved |
| R-02 | Medium | aidlc/spaces/default/intents/261009-requirement-epic-1/inception/requirements-analysis/requirements.md > FR4.4 (ngưỡng hạng) và FR4.5 (trường "Nguồn tiền") | Một số giá trị định lượng/nhãn trong yêu cầu không truy ngược được về mockup hay epic hay [Qn]: ngưỡng hạng A≥85/B70–84/C50–69/D<50 không xuất hiện trong mockup (mockup chỉ ghi "Hạng A - Xuất sắc"); tên trường "Nguồn tiền" so với mockup là "Nguồn tiền luân chuyển". Đây là các điểm người dùng nói sẽ chỉnh sau [Q5], nhưng hiện đang trình bày như giá trị chốt | Đánh dấu rõ ngưỡng hạng là đề xuất của nhóm (như A5/A1 đã làm cho tiêu chí/trường), hoặc dẫn nguồn; đồng bộ nhãn trường với mockup hoặc ghi chú lý do đặt tên khác | Unresolved |
| R-03 | Low | aidlc/spaces/default/intents/261009-requirement-epic-1/inception/requirements-analysis/requirements.md > NFR1 (Copilot ≤ 15s / báo lỗi > 30s) và FR3.6 | NFR1 đặt ngưỡng Copilot trả lời ≤ 15 giây (≥90%) và báo lỗi khi quá 30 giây, nhưng hai con số này không truy về epic hay một [Qn] nào — có vẻ do nhóm tự đặt mà không ghi là giả định | Ghi rõ đây là ngưỡng do nhóm đề xuất cho demo (giả định), hoặc xác nhận với người dùng, để tránh hiểu nhầm là yêu cầu đã được chốt | Unresolved |
| R-04 | Low | aidlc/spaces/default/intents/261009-requirement-epic-1/inception/requirements-analysis/requirements.md > FR4.8 và FR4.9 | Luồng chính đã có fallback tốt, nhưng còn vài góc chưa phủ: khi fallback (không có ghi âm/LLM lỗi) thì FR4.4 (chấm điểm) và FR4.3 (transcript/audio) hiển thị ra sao chưa nói rõ; và khi RM chọn "Liên hệ khách hàng tiếp theo" ở FR4.9 mà danh sách không còn lead nào thì xử lý thế nào chưa định nghĩa | Bổ sung tiêu chí cho trạng thái cột điểm/hội thoại khi fallback, và cho nhánh "không còn lead kế tiếp" ở FR4.9 (hoặc ghi là ngoài phạm vi có lý do) | Unresolved |
| R-05 | Low | aidlc/spaces/default/intents/261009-requirement-epic-1/inception/requirements-analysis/requirements.md > O5 và NFR2 | O5 ghi nhận khóa API thật đang nằm trong `backend/src/main/resources/application.properties` và `.env` nhưng chỉ coi việc xử lý là "khuyến nghị". Đây là rủi ro lộ bí mật thực tế trong repo (dù team-practices để mức relaxed), người dùng nên biết rõ khi duyệt | Nêu rõ rủi ro và một hành động tối thiểu (ví dụ xoay khóa, gỡ khỏi file được commit) trước khi sang Construction, hoặc xác nhận chấp nhận rủi ro có chủ đích | Unresolved |
| R-06 | Low | aidlc/spaces/default/intents/261009-requirement-epic-1/inception/requirements-analysis/requirements.md > mục 7 (Câu hỏi còn mở) O4 | O4 vẫn gọi "Câu C-HIST của epic ... mục tóm tắt AI trong Lịch sử hoạt động". Nội dung đúng, nhưng vì đã đổi số US nên nên nêu kèm số US mới (US-03) để người đọc không phải tra ngược về số epic. Không phải lỗi nội dung, chỉ là nhất quán cách gọi tên sau khi đánh số lại. | Thêm chỉ dẫn US-03 bên cạnh tham chiếu C-HIST ở O4, hoặc xác nhận giữ nguyên vì O4 vốn trích thẳng ký hiệu của epic. | New |

> R-01 Reviewer note: A5 vẫn ghi "điểm tổng /100 = tổng 5 tiêu chí × 2". Mockup màn 4 (`3-ket-qua-cuoc-goi.html` / `4-lua-chon-dieu-huong.html`) hiển thị tổng 92/100 trong khi 5 tiêu chí là 10+10+9.5+9+9.5 = 48 → ×2 = 96, không khớp 92. O3 vẫn mở. Lượt sửa chỉ đánh số lại US, không chạm công thức này.

> R-02 Reviewer note: FR4.4 vẫn trình bày ngưỡng hạng A≥85/B70–84/C50–69/D<50 như giá trị chốt, không dẫn nguồn. FR4.5 dùng nhãn "Nguồn tiền" trong khi mockup màn 4 ghi "Nguồn tiền luân chuyển" — nhãn chưa đồng bộ với mockup. Chưa thay đổi ở lượt này.

> R-03 Reviewer note: NFR1 vẫn nêu ngưỡng Copilot ≤ 15s và báo lỗi > 30s mà không dẫn về nguồn nào (chưa đánh dấu là đề xuất hay đã xác nhận). Chưa thay đổi ở lượt này.

> R-04 Reviewer note: FR4.8/FR4.9 vẫn chưa nói cột điểm và cột hội thoại hiển thị ra sao khi fallback (chỉ nói form keyin trống), và FR4.9 chưa định nghĩa nhánh "Liên hệ khách hàng tiếp theo" khi không còn lead kế tiếp trong danh sách. Chưa thay đổi ở lượt này.

> R-05 Reviewer note: O5 vẫn ghi khóa API thật nằm trong `backend/src/main/resources/application.properties` và `.env` được commit, và chỉ coi là khuyến nghị. Rủi ro và hành động tối thiểu chưa nêu dứt khoát. Chưa thay đổi ở lượt này.

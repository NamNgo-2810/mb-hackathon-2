## Review

**Verdict:** READY
**Reviewer:** aidlc-architecture-reviewer-agent
**Date:** 2026-10-09T11:20:18Z
**Iteration:** 1

Rà soát một lượt (advisory) toàn bộ mã U3 `u3-copilot` đã hoàn thiện: 24 file Java,
2 file frontend mới/sửa, cấu hình và tài liệu mẫu. Không biên dịch được tại chỗ
(không chạy Maven theo yêu cầu), nên các nhận định về lỗi biên dịch dựa trên đối
chiếu chữ ký phương thức/kiểu dữ liệu giữa các file. Trọng tâm: lỗi biên dịch, bảo
mật (prompt-injection, log, quyền sở hữu), tuân thủ hợp đồng C2, và độ trung thực
giao diện so với mockup gốc `2-thuc-hien-cuoc-goi.html`.

### Findings

| ID | Severity | Location | Finding | Required action | Status |
|---|---|---|---|---|---|
| R-01 | Minor | backend/src/main/java/com/mb/callassistant/copilot/CopilotQueryLog.java > constructor | Nhật ký truy vấn lưu nguyên văn `question` của RM vào trường bền trong bộ nhớ; quy tắc nội bộ nêu nhật ký không nên chứa toàn văn câu hỏi/nội dung nhạy cảm. Hiện chỉ nằm trong RAM và không ghi ra log slf4j nên rủi ro thấp, nhưng vẫn lệch chủ đích "chỉ đếm lượt cho OKR" | Lưu bản rút gọn hoặc băm/độ dài câu hỏi thay cho nguyên văn, hoặc ghi chú rõ đây là chấp nhận cho demo | New |
| R-02 | Minor | backend/src/main/java/com/mb/callassistant/web/dto/CopilotAskRequest.java > HistoryTurnDto.role | `role` của mỗi lượt lịch sử không được kiểm tra thuộc tập {user, assistant} như hợp đồng C2 mô tả; giá trị lạ vẫn được chấp nhận và đưa vào prompt | Thêm kiểm tra/khử giá trị `role` ngoài tập cho phép, hoặc ghi nhận là chấp nhận do giao diện tự kiểm soát | New |
| R-03 | Minor | public/screens/2-thuc-hien-cuoc-goi.html > khối Copilot (block 2) | So với mockup gốc, phần trả lời không hiển thị lại bong bóng câu hỏi của RM và không có nhãn thời gian mỗi lượt; cột lịch sử dùng danh sách phẳng thay cho mục "đang hoạt động" được tô đậm. Chức năng đủ, độ trung thực hình ảnh ở mức khá | Nếu cần sát mockup hơn, bổ sung bong bóng câu hỏi RM và trạng thái mục lịch sử đang chọn; không bắt buộc cho bản demo | New |
| R-04 | Minor | public/js/copilot.js > showStored | Khi mở lại một lượt từ cột lịch sử, các chip gợi ý được render lại và bấm vào sẽ gửi câu hỏi mới dựa trên lịch sử phiên hiện tại, không phải ngữ cảnh của lượt đã lưu; dễ gây nhầm khi RM xem lại lượt cũ | Cân nhắc chỉ hiển thị (không cho bấm) gợi ý khi xem lại lượt đã lưu, hoặc ghi chú hành vi này là chấp nhận | New |

### Summary

Không phát hiện lỗi biên dịch khả nghi: các chữ ký `LlmRequest`, `CurrentRmProvider.currentRmId()`,
`switch` trên `LlmException.Kind` (đủ 5 nhánh) và các DTO/record đều khớp. Mã an toàn
trước prompt-injection (tách chỉ dẫn khỏi khối DATA, hai lớp bọc), ghi đúng một log mỗi
lượt kể cả nhánh lỗi, và khớp hợp đồng C2 (shape phản hồi, cắt gợi ý ≤4, trích dẫn bắt
buộc title+effectiveDate+page/section). Các điểm còn lại đều là Minor, phù hợp để người
duyệt cân nhắc tại cổng chứ không chặn.

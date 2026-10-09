## Review

**Verdict:** READY
**Reviewer:** aidlc-architecture-reviewer-agent
**Date:** 2026-10-09T13:07:15Z
**Iteration:** 1

Lượt soát xét tư vấn một lần trên mã U4 đã hoàn tất. Tập trung vào lỗi biên dịch / lỗi khởi động, công thức chấm điểm, giao dịch/idempotent khi lưu, 403 theo quyền sở hữu, chống prompt injection, tính hợp lệ của JSON giả lập, và độ trung thực giao diện so với mockup gốc. Không nêu thiếu test (test đã được người dùng hoãn).

### Findings

| ID | Severity | Location | Finding | Required action | Status |
|---|---|---|---|---|---|
| R-01 | Major | backend/src/main/java/com/mb/callassistant/analysis/KeyinService.java > retryAnalysis | Phương thức bọc trong @Transactional nhưng lại gọi phân tích AI đồng bộ bên trong (CallAnalysisService.analyze thực hiện gọi LLM qua mạng với timeout tới 30s, chạy lại một lần nữa là tối đa ~60s, cộng updatePipeline). Việc giữ một giao dịch cơ sở dữ liệu mở suốt thời gian gọi LLM sẽ giữ kết nối trong pool rất lâu và dễ cạn pool khi có vài yêu cầu chạy lại song song; ngoài ra controller trả 202 'Đã nhận' hàm ý xử lý nền trong khi thực tế là chặn đồng bộ. | Tách lời gọi phân tích AI ra ngoài giao dịch: trong giao dịch chỉ đặt trạng thái PROCESSING/ANALYZE, rồi chạy phân tích ở ngoài giao dịch (hoặc chạy nền giống pipeline sau cuộc gọi) để kết nối không bị giữ trong lúc gọi LLM và để phản hồi 202 đúng nghĩa. | New |
| R-02 | Minor | backend/src/main/java/com/mb/callassistant/analysis/CallAnalysisService.java > runWithOneRetry | Với lỗi LLM không thể chạy lại (ví dụ TIMEOUT/NOT_CONFIGURED có isRetryable=false), vòng lặp thoát ngay ở lần 1; còn khi JSON không hợp lệ (AnalysisInvalidException) thì chạy lại đúng một lần. Hành vi khớp WF1, nhưng khác nhau giữa hai loại lỗi dễ gây hiểu nhầm 'luôn chạy lại một lần' khi đọc nhanh. | Thêm một dòng chú thích làm rõ: lỗi cấu trúc/điểm luôn được chạy lại một lần, còn lỗi LLM chỉ chạy lại khi isRetryable; giữ nguyên logic. | New |
| R-03 | Minor | public/js/call-result.js > setPipelinePill | Bản đồ nhãn trạng thái không có khóa IN_CALL (một giá trị hợp lệ của pipelineStatus trong hợp đồng C1); trạng thái này rơi vào nhánh mặc định 'Đang tải…'. Trên màn Kết quả cuộc gọi gần như không gặp IN_CALL nên không chặn, nhưng nhãn hiển thị sẽ không khớp trạng thái thật nếu gặp. | Thêm nhãn cho IN_CALL (ví dụ 'Đang trong cuộc gọi') hoặc ghi chú rõ rằng S3 chỉ mở sau khi cuộc gọi kết thúc nên IN_CALL không áp dụng. | New |

### Summary

Mã U4 nhất quán và khớp với các giao diện Java thật của U1/U2/U5: `CallService.getOwnedCall(callId)` một tham số, `TaskService.createFromCall(rmId, leadId, callId, callAiSummary, drafts)` đúng 5 tham số, hàm dựng `LlmRequest` đúng thứ tự trường, `TranscriptTurnRepository.findByCallIdOrderByStartMsAsc(Long)` có thật. Vòng phụ thuộc bean được phá đúng bằng `@Lazy CallService` trong cả `CallPipelineService` và `CallAnalysisService`; không còn chu trình eager. Công thức chấm điểm đúng (round(tổng×2), biên A≥85/B≥70/C≥50, mẫu 10+10+9,5+9+9,5=48→96→A); JSON giả lập của `MockLlmClient` hợp lệ cho cả COPILOT lẫn CALL_ANALYSIS. Chống prompt injection đặt transcript trong khối DATA của `userContent`; 403 theo quyền sở hữu qua `getOwnedCall`; lưu keyin idempotent theo callId trong một giao dịch; giao diện S3/S4 giữ bố cục 3 cột, vòng điểm, huy hiệu hạng, thanh tiến trình 3 bước và dự phòng lỗi của mockup gốc. Không phát hiện lỗi biên dịch hay lỗi khởi động. Mối lo lớn nhất là giao dịch bao quanh lời gọi LLM đồng bộ ở `retryAnalysis` (hiệu năng/robustness), nên cân nhắc trước khi phê duyệt nhưng không chặn biên dịch.

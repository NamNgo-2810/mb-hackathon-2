package com.mb.callassistant.analysis;

import com.mb.callassistant.domain.CallRecord;
import com.mb.callassistant.domain.PipelineStatus;
import com.mb.callassistant.domain.PipelineStep;
import com.mb.callassistant.lead.CurrentRmProvider;
import com.mb.callassistant.lead.LeadService;
import com.mb.callassistant.repository.TranscriptTurnRepository;
import com.mb.callassistant.service.CallService;
import com.mb.callassistant.task.TaskDraft;
import com.mb.callassistant.task.TaskService;
import com.mb.callassistant.web.ConflictException;
import com.mb.callassistant.web.NotFoundException;
import com.mb.callassistant.web.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lưu keyin một-giao-dịch & chạy lại AI (lớp nghiệp vụ, U4).
 *
 * <ul>
 *   <li>{@link #getOwnedAnalysis}/{@link #getOwnedKeyin}: qua
 *       {@link CallService#getOwnedCall} (I2) → 403 nếu khác RM, 404 nếu chưa có
 *       (BR5.5).</li>
 *   <li>{@link #saveKeyin}: trong một giao dịch (BR5.3) — kiểm bắt buộc
 *       {@code leadStage}+{@code callResult} (BR5.1); idempotent theo callId
 *       (upsert, BR5.2); lưu {@link CallKeyin}+{@link AiFeedback}; gọi
 *       {@link LeadService#updateStage} (I1), {@link LeadService#recordCallActivity}
 *       kèm bản sao từ CallAnalysis (BR5.4), và
 *       {@link TaskService#createFromCall} đúng 5 tham số (I4). Lỗi bất kỳ →
 *       hoàn tác toàn bộ.</li>
 *   <li>{@link #retryAnalysis}: 409 khi chưa có transcript / đang chạy (BR1.4),
 *       ngược lại đặt PROCESSING/ANALYZE và chạy lại (BR1.3).</li>
 * </ul>
 */
@Service
public class KeyinService {

    private static final Logger log = LoggerFactory.getLogger(KeyinService.class);
    private static final ZoneOffset VN_OFFSET = ZoneOffset.ofHours(7);

    private final CallService callService;
    private final CurrentRmProvider currentRmProvider;
    private final CallAnalysisRepository analyses;
    private final CallKeyinRepository keyins;
    private final AiFeedbackRepository feedbacks;
    private final AnalysisJson analysisJson;
    private final LeadService leadService;
    private final TaskService taskService;
    private final TranscriptTurnRepository turns;
    private final CallAnalysisService analysisService;

    public KeyinService(CallService callService, CurrentRmProvider currentRmProvider,
                        CallAnalysisRepository analyses, CallKeyinRepository keyins,
                        AiFeedbackRepository feedbacks, AnalysisJson analysisJson,
                        LeadService leadService, TaskService taskService,
                        TranscriptTurnRepository turns, CallAnalysisService analysisService) {
        this.callService = callService;
        this.currentRmProvider = currentRmProvider;
        this.analyses = analyses;
        this.keyins = keyins;
        this.feedbacks = feedbacks;
        this.analysisJson = analysisJson;
        this.leadService = leadService;
        this.taskService = taskService;
        this.turns = turns;
        this.analysisService = analysisService;
    }

    /** Phân tích của cuộc gọi (chỉ RM sở hữu); 403/404 (BR5.5). */
    @Transactional(readOnly = true)
    public CallAnalysis getOwnedAnalysis(Long callId) {
        callService.getOwnedCall(callId); // 403/404
        return analyses.findByCallId(String.valueOf(callId))
                .orElseThrow(() -> new NotFoundException("Chưa có kết quả phân tích cho cuộc gọi " + callId));
    }

    /** Keyin của cuộc gọi (chỉ RM sở hữu); 403/404 (BR5.5). */
    @Transactional(readOnly = true)
    public CallKeyin getOwnedKeyin(Long callId) {
        callService.getOwnedCall(callId); // 403/404
        return keyins.findByCallId(String.valueOf(callId))
                .orElseThrow(() -> new NotFoundException("Chưa có keyin cho cuộc gọi " + callId));
    }

    /** Đánh giá AI đã lưu (có thể vắng); dùng khi dựng DTO keyin. */
    @Transactional(readOnly = true)
    public AiFeedback findFeedback(Long callId) {
        return feedbacks.findByCallId(String.valueOf(callId)).orElse(null);
    }

    /**
     * Lưu hồ sơ cuộc gọi trong một giao dịch (BR5.3). Lỗi bất kỳ → hoàn tác.
     *
     * @throws ValidationException thiếu trường bắt buộc (400, BR5.1)
     */
    @Transactional
    public KeyinSaveResult saveKeyin(Long callId, KeyinSave req) {
        CallRecord call = callService.getOwnedCall(callId); // 403/404 (BR5.5)
        String rmId = currentRmProvider.currentRmId();
        String leadId = call.getLeadId();
        String callIdStr = String.valueOf(callId);

        // BR5.1 — hai trường bắt buộc; giữ nguyên dữ liệu (không lưu gì khi thiếu).
        if (req == null) {
            throw new ValidationException("leadStage", "Thiếu dữ liệu keyin");
        }
        if (req.leadStage() == null) {
            throw new ValidationException("leadStage", "leadStage là bắt buộc");
        }
        if (req.callResult() == null || req.callResult().isBlank()) {
            throw new ValidationException("callResult", "callResult là bắt buộc");
        }

        // (1) Lưu CallKeyin idempotent theo callId (BR5.2).
        CallKeyin keyin = keyins.findByCallId(callIdStr)
                .orElseGet(() -> new CallKeyin(UUID.randomUUID().toString(), callIdStr));
        keyin.setCallId(callIdStr);
        keyin.setLeadStage(req.leadStage());
        keyin.setCallResult(req.callResult());
        keyin.setDemandAmount(req.demandAmount());
        keyin.setTerm(req.term());
        keyin.setAgreedRate(req.agreedRate());
        keyin.setProduct(req.product());
        keyin.setSourceOfFunds(req.sourceOfFunds());
        keyin.setExpectedDate(req.expectedDate());
        keyin.setChannel(req.channel());
        keyin.setObjections(req.objections());
        keyin.setRmNote(req.rmNote());
        Map<String, FieldSource> fieldSources = req.fieldSources();
        keyin.setFieldSourcesJson(analysisJson.write(fieldSources == null ? Map.of() : fieldSources));
        keyin.setSavedAt(OffsetDateTime.now(VN_OFFSET));
        keyins.save(keyin);

        // (2) Lưu AiFeedback nếu có (BR5.3). Tối đa một / cuộc gọi.
        if (req.feedbackRating() != null) {
            int rating = req.feedbackRating();
            if (rating < 1 || rating > 10) {
                throw new ValidationException("rating", "Điểm đánh giá AI phải trong [1,10]");
            }
            String comment = req.feedbackComment();
            if (comment != null && comment.length() > 1000) {
                throw new ValidationException("comment", "Góp ý tối đa 1000 ký tự");
            }
            AiFeedback feedback = feedbacks.findByCallId(callIdStr)
                    .orElseGet(() -> new AiFeedback(UUID.randomUUID().toString(), callIdStr, rating));
            feedback.setCallId(callIdStr);
            feedback.setRating(rating);
            feedback.setTags(req.feedbackTags());
            feedback.setComment(comment);
            feedbacks.save(feedback);
        }

        // Bản sao tóm tắt/gợi ý AI tại thời điểm lưu (BR5.4); null khi AI lỗi.
        CallAnalysis analysis = analyses.findByCallId(callIdStr).orElse(null);
        String aiSummary = analysis == null ? null : analysis.getSummary();
        List<String> aiSuggestions = analysis == null
                ? null : analysisJson.readStringList(analysis.getSuggestionsJson());

        // (3) Cập nhật giai đoạn lead (I1) — chỉ khi gắn lead.
        if (leadId != null && !leadId.isBlank()) {
            leadService.updateStage(rmId, leadId, req.leadStage());
            // (4) Ghi hoạt động cuộc gọi kèm bản sao AI (I1, BR5.4).
            leadService.recordCallActivity(rmId, leadId, callIdStr,
                    req.product(), req.callResult(), req.rmNote(), aiSummary, aiSuggestions);
        }

        // (5) Tạo công việc tiếp theo (I4) — đúng 5 tham số thật của U5.
        List<TaskDraft> drafts = req.nextTasks() == null ? List.of() : req.nextTasks();
        List<String> createdTaskIds =
                taskService.createFromCall(rmId, leadId, callIdStr, aiSummary, drafts);

        String interactionCode = "INT-" + callIdStr;
        log.info("Lưu keyin call {} outcome=SAVED tasks={}", callId, createdTaskIds.size());
        return new KeyinSaveResult(callIdStr, interactionCode, createdTaskIds);
    }

    /**
     * Chạy lại phân tích AI (BR1.3/BR1.4). 409 khi chưa có transcript hoặc đang
     * chạy; ngược lại đặt {@code PROCESSING/ANALYZE} và chạy lại WF1.
     *
     * @throws ConflictException 409 (BR1.4)
     */
    @Transactional
    public void retryAnalysis(Long callId) {
        CallRecord call = callService.getOwnedCall(callId); // 403/404 (BR5.5)
        boolean hasTranscript = !turns.findByCallIdOrderByStartMsAsc(callId).isEmpty();
        if (!hasTranscript) {
            throw new ConflictException("Chưa có transcript để chạy lại phân tích AI");
        }
        if (call.getPipelineStatus() == PipelineStatus.PROCESSING) {
            throw new ConflictException("Phân tích AI đang chạy, không thể chạy lại");
        }
        // AI_FAILED -> PROCESSING (SMA/SM2); DONE là trạng thái cuối nên không chạy lại.
        if (call.getPipelineStatus() != PipelineStatus.AI_FAILED) {
            throw new ConflictException("Chỉ chạy lại được khi phân tích AI đã thất bại");
        }
        callService.updatePipeline(callId, PipelineStatus.PROCESSING, PipelineStep.ANALYZE, null);
        analysisService.analyze(callId, call.getLeadId());
    }
}

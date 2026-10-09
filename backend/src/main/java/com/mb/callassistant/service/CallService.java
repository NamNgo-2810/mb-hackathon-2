package com.mb.callassistant.service;

import com.mb.callassistant.domain.CallRecord;
import com.mb.callassistant.domain.PipelineStatus;
import com.mb.callassistant.domain.PipelineStep;
import com.mb.callassistant.domain.SttStatus;
import com.mb.callassistant.lead.CurrentRmProvider;
import com.mb.callassistant.lead.Customer;
import com.mb.callassistant.lead.Lead;
import com.mb.callassistant.lead.LeadService;
import com.mb.callassistant.repository.CallRecordRepository;
import com.mb.callassistant.web.ConflictException;
import com.mb.callassistant.web.ForbiddenException;
import com.mb.callassistant.web.NotFoundException;
import com.mb.callassistant.web.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Call-record lifecycle (FR1). The call↔recording mapping is demo-simple
 * (FR2.2): there is no UUID resolution and no dialplan key. On hang up, the
 * backend asks the FreeSWITCH recordings server for the most recent recording
 * filename ({@code GET /api/recordings/latest}) via {@link RecordingClient} and
 * stores it as {@link CallRecord#getRecordingName()} — that filename is the link
 * between the call and its single stereo {@code .wav}.
 */
@Service
public class CallService {

    private static final Logger log = LoggerFactory.getLogger(CallService.class);

    private final CallRecordRepository callRecords;
    private final CallPipelineService pipeline;
    private final RecordingClient recordingClient;
    private final LeadService leadService;
    private final CurrentRmProvider currentRmProvider;

    public CallService(CallRecordRepository callRecords,
                       CallPipelineService pipeline,
                       RecordingClient recordingClient,
                       LeadService leadService,
                       CurrentRmProvider currentRmProvider) {
        this.callRecords = callRecords;
        this.pipeline = pipeline;
        this.recordingClient = recordingClient;
        this.leadService = leadService;
        this.currentRmProvider = currentRmProvider;
    }

    /**
     * Create a call record (status PENDING). The frontend places the actual call
     * via MicroSIP; the backend never spawns it, and does no recording work yet
     * (FR1.1). The recording filename is resolved only when the call ends.
     */
    @Transactional
    public CallRecord initiate(String phoneNumber, String tnbCode) {
        CallRecord cr = new CallRecord(tnbCode, phoneNumber);
        cr.setStartedAt(Instant.now());
        cr.setSttStatus(SttStatus.PENDING);
        CallRecord saved = callRecords.save(cr);
        log.info("Initiated call {} (phone={}); PENDING until /ended.", saved.getId(), phoneNumber);
        return saved;
    }

    /**
     * Mark the call ended, record its duration, resolve and store the latest
     * recording filename (the call↔recording link), then start the async
     * pipeline. If the server has no filename yet, the record stays keyless and
     * the pipeline reports {@code RECORDING_NOT_READY} so the frontend keeps
     * polling (FR1.3).
     *
     * @return the updated record, or empty if no such call exists
     */
    @Transactional
    public Optional<CallRecord> markEnded(Long callId) {
        return callRecords.findById(callId).map(cr -> {
            if (cr.getStartedAt() != null) {
                long secs = Math.max(0, ChronoUnit.SECONDS.between(cr.getStartedAt(), Instant.now()));
                cr.setDurationSeconds((int) secs);
            }
            Optional<String> latest = recordingClient.latestFilename();
            latest.ifPresent(name -> {
                cr.setRecordingName(name);
                log.info("Mapped call {} to latest recording '{}'.", callId, name);
            });
            if (latest.isEmpty()) {
                log.info("No latest recording filename available yet for call {}; pipeline will retry.", callId);
            }
            cr.setSttStatus(SttStatus.PROCESSING);
            cr.setErrorDetail(null);
            CallRecord saved = callRecords.save(cr);
            pipeline.runPipeline(saved.getId());
            log.info("Call {} ended; pipeline started (recordingName={}).", saved.getId(), saved.getRecordingName());
            return saved;
        });
    }

    public Optional<CallRecord> find(Long callId) {
        return callRecords.findById(callId);
    }

    // ───────────────────────── Lead-based call flow (I2, FR1.5/FR2.2) ─────────────────────────

    /**
     * Tạo cuộc gọi từ một lead (FR1.5, BR3.1/BR3.2): kiểm quyền lead (403 nếu
     * khác RM), kiểm số điện thoại hợp lệ (400), kiểm giới hạn một cuộc gọi chưa
     * xử lý xong (409), rồi mở {@link PipelineStatus#IN_CALL}.
     */
    @Transactional
    public CallRecord createFromLead(String leadId) {
        String rmId = currentRmProvider.currentRmId();
        // 403/404 nếu lead không thuộc RM / không tồn tại (BR1.1/BR1.2).
        Lead lead = leadService.getOwnedLead(rmId, leadId);

        Customer customer = leadService.getCustomer(lead.getCustomerId())
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy khách hàng của tiềm năng bán " + leadId));
        if (!Customer.isValidPhone(customer.getPhoneNumber())) {
            throw new ValidationException("phoneNumber",
                    "Số điện thoại khách hàng không hợp lệ, không thể tạo cuộc gọi");
        }

        // Giới hạn một cuộc gọi chưa xử lý xong tại một thời điểm (BR3.2).
        boolean hasInFlight = !callRecords
                .findByPipelineStatusIn(List.of(PipelineStatus.IN_CALL, PipelineStatus.PROCESSING))
                .isEmpty();
        if (hasInFlight) {
            throw new ConflictException("Đang có một cuộc gọi khác chưa xử lý xong");
        }

        CallRecord cr = new CallRecord(leadId, rmId, customer.getPhoneNumber(), Instant.now());
        CallRecord saved = callRecords.save(cr);
        log.info("Created call {} from lead {} (rm={}); IN_CALL.", saved.getId(), leadId, rmId);
        return saved;
    }

    /**
     * Kết thúc cuộc gọi của RM hiện tại, idempotent (AC1.1.3, BR4.1). Kiểm quyền
     * (403 nếu khác RM). Lần đầu đặt endedAt + chạy pipeline đúng một lần; lần
     * sau no-op, trả trạng thái hiện tại.
     */
    @Transactional
    public CallRecord endCall(Long callId) {
        CallRecord cr = getOwnedCall(callId);
        boolean firstEnd = cr.markEnded(Instant.now());
        if (firstEnd) {
            // Giữ nhịp giao diện cũ: resolve recording + đặt sttStatus (tương thích).
            Optional<String> latest = recordingClient.latestFilename();
            latest.ifPresent(cr::setRecordingName);
            cr.setSttStatus(SttStatus.PROCESSING);
            cr.setPipelineStep(PipelineStep.FETCH_RECORDING);
            cr.setErrorDetail(null);
        }
        CallRecord saved = callRecords.save(cr);
        if (firstEnd) {
            pipeline.runPipeline(saved.getId()); // chạy đúng một lần (idempotent)
            log.info("Call {} ended; pipeline started once.", saved.getId());
        } else {
            log.info("Call {} already ended; no-op (idempotent).", saved.getId());
        }
        return saved;
    }

    /**
     * Cuộc gọi của RM hiện tại (I2, BR1.1/BR1.2): 403 nếu thuộc RM khác, 404 nếu
     * không tồn tại trong phạm vi RM này. Cuộc gọi cũ không gắn rmId (trang cũ)
     * được coi là của RM hiện tại để không hỏng luồng demo cũ.
     */
    @Transactional(readOnly = true)
    public CallRecord getOwnedCall(Long callId) {
        CallRecord cr = callRecords.findById(callId).orElse(null);
        if (cr == null) {
            throw new NotFoundException("Không tìm thấy cuộc gọi " + callId);
        }
        String rmId = currentRmProvider.currentRmId();
        if (cr.getRmId() != null && !cr.getRmId().equals(rmId)) {
            throw new ForbiddenException("Cuộc gọi không thuộc RM hiện tại");
        }
        return cr;
    }

    /**
     * Cập nhật trạng thái pipeline (I2, BR4.2/BR4.3). U1 là bên duy nhất ghi
     * trạng thái; chỉ cho phép chuyển theo máy trạng thái SM2, ngoài tập → ném
     * {@link ValidationException}.
     */
    @Transactional
    public void updatePipeline(Long callId, PipelineStatus target, PipelineStep step, String errorDetail) {
        CallRecord cr = callRecords.findById(callId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cuộc gọi " + callId));
        PipelineStatus current = cr.getPipelineStatus();
        if (!isValidTransition(current, target)) {
            throw new ValidationException("pipelineStatus",
                    "Chuyển trạng thái pipeline không hợp lệ: " + current + " -> " + target);
        }
        cr.setPipelineStatus(target);
        // pipelineStep chỉ có giá trị khi PROCESSING; null ở trạng thái cuối (BR4.3).
        cr.setPipelineStep(target == PipelineStatus.PROCESSING ? step : null);
        cr.setErrorDetail(errorDetail);
        callRecords.save(cr);
    }

    /** Tập chuyển hợp lệ SM2 (BR4.3). */
    static boolean isValidTransition(PipelineStatus from, PipelineStatus to) {
        if (from == to) {
            return true; // cập nhật step trong cùng trạng thái PROCESSING
        }
        return switch (from) {
            case IN_CALL -> to == PipelineStatus.PROCESSING;
            case PROCESSING -> to == PipelineStatus.DONE
                    || to == PipelineStatus.NO_RECORDING
                    || to == PipelineStatus.STT_FAILED
                    || to == PipelineStatus.AI_FAILED;
            case AI_FAILED -> to == PipelineStatus.PROCESSING; // chạy lại AI (U4)
            case DONE, NO_RECORDING, STT_FAILED -> false; // trạng thái cuối
        };
    }
}

package com.mb.callassistant.task;

import com.mb.callassistant.lead.CurrentRmProvider;
import com.mb.callassistant.lead.LeadService;
import com.mb.callassistant.web.ForbiddenException;
import com.mb.callassistant.web.NotFoundException;
import com.mb.callassistant.web.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Nghiệp vụ Công việc & Lịch làm việc (U5). Sở hữu {@link Task}; dựa lại U1 cho
 * danh tính RM hiện tại ({@link CurrentRmProvider}, I1) và kiểm quyền sở hữu lead
 * ({@link LeadService#assertOwned}, I1/BR1.3). Lộ I4
 * ({@link #createFromCall}) cho U4.
 *
 * <p>Quy ước thời gian: mọi mốc dùng offset {@code +07:00}, ranh giới ngày và
 * khung giờ theo {@code Asia/Ho_Chi_Minh} (BR6.1).
 */
@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    /** Múi giờ nghiệp vụ (BR6.1). */
    public static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    /** Giới hạn khoảng truy vấn lịch, đủ cho chế độ Tháng (BR4.1). */
    private static final long MAX_RANGE_DAYS = 42L;

    /** Thời lượng hợp lệ cho công việc (BR6.2). */
    private static final Set<Integer> VALID_DURATIONS = Set.of(15, 30, 60);

    /** Loại hợp lệ khi tạo (KHAC chỉ dành cho seed không gắn lead, BR3.2). */
    private static final Set<TaskType> CREATABLE_TYPES =
            EnumSet.of(TaskType.LICH_HEN, TaskType.NHAC_GOI, TaskType.TASK_ZALO);

    private final TaskRepository tasks;
    private final CurrentRmProvider currentRmProvider;
    private final LeadService leadService;

    public TaskService(TaskRepository tasks, CurrentRmProvider currentRmProvider, LeadService leadService) {
        this.tasks = tasks;
        this.currentRmProvider = currentRmProvider;
        this.leadService = leadService;
    }

    /** Thời điểm hiện tại có offset +07:00 (BR6.1). */
    private OffsetDateTime now() {
        return OffsetDateTime.now(VN);
    }

    // ── I4 — Tạo công việc từ cuộc gọi (phục vụ U4) ──

    /**
     * Tạo các công việc tiếp theo từ một cuộc gọi (I4). Idempotent theo
     * {@code (callId, draft.clientKey)}: cùng cặp khoá chỉ sinh đúng một
     * {@link Task} (BR2.1). Validate <b>mọi</b> draft trước khi tạo gì — thiếu
     * {@code clientKey} từ chối cả lời gọi (BR2.2).
     *
     * @param rmId         RM thao tác (phải khớp RM hiện tại, BR1.1)
     * @param leadId       lead gắn các công việc, hoặc null; khác null thì phải thuộc RM (BR1.3)
     * @param callId       cuộc gọi gốc — {@code sourceCallId} của mọi Task tạo ra
     * @param callAiSummary bản sao tóm tắt AI của cuộc gọi, chép sang Task (BR2.3); có thể null
     * @param drafts       danh sách công việc tiếp theo
     * @return danh sách {@code taskId} theo đúng thứ tự draft (tái dùng id cũ khi trùng khoá)
     */
    @Transactional
    public List<String> createFromCall(String rmId, String leadId, String callId,
                                       String callAiSummary, List<TaskDraft> drafts) {
        assertCurrentRm(rmId); // BR1.1
        if (callId == null || callId.isBlank()) {
            throw new ValidationException("callId", "callId là bắt buộc cho createFromCall");
        }
        if (leadId != null) {
            leadService.assertOwned(rmId, leadId); // BR1.3 — 403 nếu lead khác RM
        }
        List<TaskDraft> safeDrafts = drafts == null ? List.of() : drafts;

        // Validate toàn bộ trước khi tạo bất kỳ Task nào (BR2.2 / BR3.1 / BR3.2 / BR6.2).
        for (TaskDraft d : safeDrafts) {
            if (d.clientKey() == null || d.clientKey().isBlank()) {
                throw new ValidationException("clientKey",
                        "clientKey là bắt buộc với createFromCall; thiếu clientKey nên từ chối");
            }
            validateTitle(d.title());
            validateDueAt(d.dueAt());
            if (d.type() == null || !CREATABLE_TYPES.contains(d.type())) {
                throw new ValidationException("type",
                        "Loại công việc từ cuộc gọi phải là LICH_HEN, NHAC_GOI hoặc TASK_ZALO");
            }
            validateDuration(d.durationMinutes());
        }

        List<String> result = new ArrayList<>(safeDrafts.size());
        for (TaskDraft d : safeDrafts) {
            Task existing = tasks
                    .findByOwnerRmIdAndSourceCallIdAndClientKey(rmId, callId, d.clientKey())
                    .orElse(null);
            if (existing != null) {
                result.add(existing.getTaskId()); // BR2.1 — tái dùng, không tạo trùng
                continue;
            }
            Task task = new Task(UUID.randomUUID().toString(), rmId, d.title().trim(), d.type(),
                    d.dueAt(), TaskOrigin.AI_FROM_CALL, now());
            task.setLeadId(leadId);
            task.setSourceCallId(callId);
            task.setClientKey(d.clientKey());
            task.setPriority(d.priority() == null ? TaskPriority.THUONG : d.priority());
            task.setDurationMinutes(d.durationMinutes() == null ? 30 : d.durationMinutes());
            task.setAiSummary(callAiSummary); // BR2.3 — bản sao độc lập
            tasks.save(task);
            result.add(task.getTaskId());
        }
        return result;
    }

    // ── C4 — Tạo thủ công (POST /api/tasks) ──

    /**
     * Tạo công việc thủ công (S6). Validate tiêu đề, dueAt, thời lượng, loại; gắn
     * lead thì kiểm quyền (BR1.3) và cấm {@code KHAC} (BR3.2). Tạo với
     * {@code origin = MANUAL}, {@code status = TODO}.
     */
    @Transactional
    public Task createManual(TaskCreate req) {
        if (req == null) {
            throw new ValidationException("title", "Thiếu dữ liệu tạo công việc");
        }
        String rmId = currentRmProvider.currentRmId();
        validateTitle(req.title());
        validateDueAt(req.dueAt());
        if (req.type() == null) {
            throw new ValidationException("type", "Loại công việc là bắt buộc");
        }
        validateDuration(req.durationMinutes());

        String leadId = (req.leadId() == null || req.leadId().isBlank()) ? null : req.leadId();
        if (req.type() == TaskType.KHAC && leadId != null) {
            throw new ValidationException("type",
                    "Loại KHAC chỉ dùng cho công việc không gắn lead");
        }
        if (leadId != null) {
            leadService.assertOwned(rmId, leadId); // BR1.3
        }

        Task task = new Task(UUID.randomUUID().toString(), rmId, req.title().trim(), req.type(),
                req.dueAt(), TaskOrigin.MANUAL, now());
        task.setLeadId(leadId);
        task.setPriority(req.priority() == null ? TaskPriority.THUONG : req.priority());
        task.setDurationMinutes(req.durationMinutes() == null ? 30 : req.durationMinutes());
        task.setNote(req.note());
        tasks.save(task);
        return task;
    }

    // ── C4 — Truy vấn lịch (GET /api/tasks) ──

    /**
     * Công việc của RM hiện tại trong {@code [from, to]} (BR4.1/BR4.4). Khoảng
     * tối đa 42 ngày và {@code from <= to}, nếu không thì 400 (BR4.1). Khi
     * {@code type} chỉ định → lọc loại, {@code KHAC} không lọt (BR4.3). Khi
     * {@code sourceCallId} chỉ định → khối panel trái S5 (BR4.6).
     */
    @Transactional(readOnly = true)
    public List<Task> listTasks(OffsetDateTime from, OffsetDateTime to,
                                TaskType type, String sourceCallId) {
        String rmId = currentRmProvider.currentRmId();

        if (sourceCallId != null && !sourceCallId.isBlank()) {
            // Khối "AI Key-in từ cuộc gọi" — độc lập khỏi khoảng thời gian (BR4.6).
            return tasks.findByOwnerRmIdAndSourceCallIdOrderByDueAtAsc(rmId, sourceCallId);
        }

        validateRange(from, to); // BR4.1
        if (type != null) {
            return tasks.findByOwnerRmIdAndDueAtBetweenAndTypeOrderByDueAtAsc(rmId, from, to, type);
        }
        return tasks.findByOwnerRmIdAndDueAtBetweenOrderByDueAtAsc(rmId, from, to);
    }

    // ── C4 — Thao tác (PATCH /api/tasks/{taskId}) ──

    /**
     * Đánh dấu hoàn thành (BR5.1). 403 nếu công việc của RM khác, 404 nếu không
     * tồn tại trong phạm vi RM (BR1.1/BR1.2). TODO→DONE idempotent.
     */
    @Transactional
    public Task markDone(String taskId) {
        String rmId = currentRmProvider.currentRmId();
        Task task = getOwnedTask(rmId, taskId);
        task.markDone(now());
        tasks.save(task);
        return task;
    }

    /**
     * Đổi giờ (BR5.2). 403/404 theo quyền sở hữu. {@code newDueAt} ở quá khứ mà
     * {@code confirmPast != true} → 400 {@code VALIDATION_FAILED}
     * ({@code field = dueAt}, {@code reason = PAST_NOT_CONFIRMED}). Giữ nguyên
     * {@code status}.
     */
    @Transactional
    public Task reschedule(String taskId, OffsetDateTime newDueAt, boolean confirmPast) {
        String rmId = currentRmProvider.currentRmId();
        Task task = getOwnedTask(rmId, taskId);
        validateDueAt(newDueAt);
        if (newDueAt.isBefore(now()) && !confirmPast) {
            throw new ValidationException("dueAt", "PAST_NOT_CONFIRMED");
        }
        task.reschedule(newDueAt);
        tasks.save(task);
        return task;
    }

    /** Áp dụng PATCH: đổi giờ trước (nếu có), rồi đánh dấu xong (nếu có). */
    @Transactional
    public Task patch(String taskId, TaskStatus status, OffsetDateTime newDueAt, boolean confirmPast) {
        if (newDueAt == null && status == null) {
            throw new ValidationException("status",
                    "Cần ít nhất status=DONE hoặc dueAt để cập nhật công việc");
        }
        String rmId = currentRmProvider.currentRmId();
        Task task = getOwnedTask(rmId, taskId); // 403/404 một lần (BR1.1/BR1.2)
        if (newDueAt != null) {
            validateDueAt(newDueAt);
            if (newDueAt.isBefore(now()) && !confirmPast) {
                throw new ValidationException("dueAt", "PAST_NOT_CONFIRMED");
            }
            task.reschedule(newDueAt);
        }
        if (status == TaskStatus.DONE) {
            task.markDone(now());
        }
        tasks.save(task);
        return task;
    }

    /** Công việc của RM hiện tại; 403 nếu của RM khác, 404 nếu không tồn tại (BR1.1/BR1.2). */
    @Transactional(readOnly = true)
    public Task getOwnedTask(String rmId, String taskId) {
        Task owned = tasks.findByTaskIdAndOwnerRmId(taskId, rmId).orElse(null);
        if (owned != null) {
            return owned;
        }
        // Phân biệt 404 (của chính RM, không có) với 403 (của RM khác), không tiết lộ tồn tại.
        Task any = tasks.findById(taskId).orElse(null);
        if (any == null) {
            throw new NotFoundException("Không tìm thấy công việc " + taskId);
        }
        throw new ForbiddenException("Công việc không thuộc RM hiện tại");
    }

    // ── Trợ giúp ──

    private void assertCurrentRm(String rmId) {
        String current = currentRmProvider.currentRmId();
        if (rmId == null || !rmId.equals(current)) {
            throw new ForbiddenException("Thao tác không thuộc RM hiện tại");
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("title", "Tiêu đề công việc là bắt buộc");
        }
        if (title.trim().length() > 200) {
            throw new ValidationException("title", "Tiêu đề công việc tối đa 200 ký tự");
        }
    }

    private void validateDueAt(OffsetDateTime dueAt) {
        if (dueAt == null) {
            throw new ValidationException("dueAt", "dueAt là bắt buộc (ISO-8601 có offset +07:00)");
        }
    }

    private void validateDuration(Integer durationMinutes) {
        if (durationMinutes != null && !VALID_DURATIONS.contains(durationMinutes)) {
            throw new ValidationException("durationMinutes", "Thời lượng chỉ nhận 15, 30 hoặc 60 phút");
        }
    }

    private void validateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null) {
            throw new ValidationException("from", "from và to là bắt buộc");
        }
        if (from.isAfter(to)) {
            throw new ValidationException("from", "from phải nhỏ hơn hoặc bằng to");
        }
        if (Duration.between(from, to).toDays() > MAX_RANGE_DAYS) {
            throw new ValidationException("to", "Khoảng truy vấn tối đa 42 ngày");
        }
    }
}

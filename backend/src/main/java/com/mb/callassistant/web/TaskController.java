package com.mb.callassistant.web;

import com.mb.callassistant.lead.Customer;
import com.mb.callassistant.lead.CurrentRmProvider;
import com.mb.callassistant.lead.Lead;
import com.mb.callassistant.lead.LeadService;
import com.mb.callassistant.task.Task;
import com.mb.callassistant.task.TaskService;
import com.mb.callassistant.task.TaskType;
import com.mb.callassistant.web.dto.TaskCreateRequest;
import com.mb.callassistant.web.dto.TaskDto;
import com.mb.callassistant.web.dto.TaskPatchRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Task API (C4): lịch làm việc của RM hiện tại (S5) và tạo/đổi giờ công việc (S6).
 * <ul>
 *   <li>{@code GET /api/tasks} — công việc trong khoảng {@code [from, to]}, lọc
 *       {@code type}/{@code sourceCallId} tuỳ chọn.</li>
 *   <li>{@code POST /api/tasks} — tạo thủ công (201).</li>
 *   <li>{@code PATCH /api/tasks/{taskId}} — đánh dấu xong và/hoặc đổi giờ.</li>
 * </ul>
 * Lỗi 400/403/404 theo BR1.1/BR1.2/BR4.1/BR5.2 qua {@link ApiExceptionHandler}.
 * Mỗi DTO điền denormalised {@code leadCode}/{@code customerName} qua
 * {@link LeadService} khi công việc gắn lead (đọc trong phạm vi RM, BR1.3).
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final LeadService leadService;
    private final CurrentRmProvider currentRmProvider;

    public TaskController(TaskService taskService, LeadService leadService,
                          CurrentRmProvider currentRmProvider) {
        this.taskService = taskService;
        this.leadService = leadService;
        this.currentRmProvider = currentRmProvider;
    }

    /** Công việc của RM hiện tại trong khoảng; 400 khi khoảng sai/quá 42 ngày (BR4.1). */
    @GetMapping
    public List<TaskDto> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) TaskType type,
            @RequestParam(required = false) String sourceCallId) {
        List<Task> list = taskService.listTasks(from, to, type, sourceCallId);
        return list.stream().map(this::toDto).toList();
    }

    /** Tạo công việc thủ công (S6). 201 kèm Task; 400 thiếu tiêu đề/dueAt; 403 lead khác RM. */
    @PostMapping
    public ResponseEntity<TaskDto> create(@Valid @RequestBody TaskCreateRequest req) {
        Task task = taskService.createManual(req.toCreate());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(task));
    }

    /**
     * Đánh dấu xong và/hoặc đổi giờ. 200 kèm Task; 400 PAST_NOT_CONFIRMED (BR5.2);
     * 403 không thuộc RM; 404 công việc của chính RM không tồn tại (BR1.1/BR1.2).
     */
    @PatchMapping("/{taskId}")
    public TaskDto patch(@PathVariable String taskId, @RequestBody TaskPatchRequest req) {
        Task task = taskService.patch(taskId, req.status(), req.dueAt(), req.confirmPastFlag());
        return toDto(task);
    }

    /** Điền denormalised leadCode/customerName cho công việc gắn lead (null khi không gắn). */
    private TaskDto toDto(Task task) {
        if (task.getLeadId() == null) {
            return TaskDto.from(task);
        }
        String rmId = currentRmProvider.currentRmId();
        Lead lead = leadService.getOwnedLead(rmId, task.getLeadId());
        Customer customer = leadService.getCustomer(lead.getCustomerId()).orElse(null);
        return TaskDto.from(task, lead.getLeadCode(),
                customer == null ? null : customer.getFullName());
    }
}

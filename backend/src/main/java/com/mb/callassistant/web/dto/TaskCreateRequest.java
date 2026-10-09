package com.mb.callassistant.web.dto;

import com.mb.callassistant.task.TaskCreate;
import com.mb.callassistant.task.TaskPriority;
import com.mb.callassistant.task.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Body cho {@code POST /api/tasks} (C4): tạo công việc thủ công (S6). Kiểm hợp
 * lệ ở biên bằng bean validation; luật nghiệp vụ sâu hơn (loại KHAC, thời lượng,
 * quyền lead) do {@code TaskService} thực thi (BR3.1/BR3.2/BR6.2).
 */
public record TaskCreateRequest(

        @NotBlank(message = "Tiêu đề công việc là bắt buộc")
        @Size(max = 200, message = "Tiêu đề công việc tối đa 200 ký tự")
        String title,

        @NotNull(message = "Loại công việc là bắt buộc")
        TaskType type,

        @NotNull(message = "dueAt là bắt buộc (ISO-8601 có offset +07:00)")
        OffsetDateTime dueAt,

        Integer durationMinutes,

        TaskPriority priority,

        @Size(max = 64, message = "leadId quá dài")
        String leadId,

        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
        String note
) {

    /** Chuyển sang value-object nghiệp vụ. */
    public TaskCreate toCreate() {
        return new TaskCreate(title, type, dueAt, durationMinutes, priority, leadId, note);
    }
}

package com.mb.callassistant.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Truy cập dữ liệu {@link Task} (U5). Mọi truy vấn đều khoá theo
 * {@code ownerRmId} để thực thi quyền sở hữu ở lớp dữ liệu (BR1.1/BR1.2).
 */
public interface TaskRepository extends JpaRepository<Task, String> {

    /** Công việc của một RM (cơ sở ownership 403/404, BR1.1/BR1.2). */
    Optional<Task> findByTaskIdAndOwnerRmId(String taskId, String ownerRmId);

    /** Công việc của RM có dueAt trong [from, to], sắp theo giờ (lịch tuần, BR4.1/BR4.4). */
    List<Task> findByOwnerRmIdAndDueAtBetweenOrderByDueAtAsc(
            String ownerRmId, OffsetDateTime from, OffsetDateTime to);

    /** Như trên nhưng lọc theo loại; KHAC không lọt khi lọc loại cụ thể (BR4.3). */
    List<Task> findByOwnerRmIdAndDueAtBetweenAndTypeOrderByDueAtAsc(
            String ownerRmId, OffsetDateTime from, OffsetDateTime to, TaskType type);

    /** Công việc sinh từ một cuộc gọi — khối "AI Key-in từ cuộc gọi" (BR4.6). */
    List<Task> findByOwnerRmIdAndSourceCallIdOrderByDueAtAsc(String ownerRmId, String sourceCallId);

    /** Khoá idempotent của createFromCall: (sourceCallId, clientKey) (BR2.1). */
    Optional<Task> findByOwnerRmIdAndSourceCallIdAndClientKey(
            String ownerRmId, String sourceCallId, String clientKey);
}

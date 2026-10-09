package com.mb.callassistant.lead;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, String> {

    /** Lead của một RM (cơ sở kiểm quyền, FR1.4). */
    Optional<Lead> findByLeadIdAndOwnerRmId(String leadId, String ownerRmId);

    /** Mọi lead của RM, sắp theo ưu tiên rồi SLA (danh sách lead, phục vụ U6). */
    List<Lead> findByOwnerRmIdOrderByPriorityAscSlaDueAtAsc(String ownerRmId);

    /**
     * Tập ứng viên "lead kế tiếp" (BR5.1): lead của RM ở các giai đoạn đang mở.
     * Việc sắp xếp cuối cùng (priority → rankScore desc → slaDueAt asc) và loại
     * {@code excludeLeadId} do service làm, vì rankScore có thể null.
     */
    List<Lead> findByOwnerRmIdAndStageIn(String ownerRmId, Collection<LeadStage> stages);
}

package com.mb.callassistant.lead;

import com.mb.callassistant.web.ForbiddenException;
import com.mb.callassistant.web.NotFoundException;
import com.mb.callassistant.web.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * I1 — nghiệp vụ Lead & quyền sở hữu (FR1.3, FR1.4, FR4.9). U1 sở hữu
 * {@link Lead}, {@link Customer}, {@link Activity} và thực thi tập chuyển giai
 * đoạn hợp lệ (SM1). Các unit khác (U4/U6) gọi qua giao diện này.
 */
@Service
public class LeadService {

    private static final Logger log = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leads;
    private final CustomerRepository customers;
    private final ActivityRepository activities;

    public LeadService(LeadRepository leads, CustomerRepository customers, ActivityRepository activities) {
        this.leads = leads;
        this.customers = customers;
        this.activities = activities;
    }

    /**
     * Lead của RM hiện tại (BR1.1/BR1.2): 403 nếu thuộc RM khác, 404 nếu không
     * tồn tại trong phạm vi RM này.
     */
    @Transactional(readOnly = true)
    public Lead getOwnedLead(String rmId, String leadId) {
        Lead lead = leads.findById(leadId).orElse(null);
        if (lead == null) {
            throw new NotFoundException("Không tìm thấy tiềm năng bán " + leadId);
        }
        if (!lead.getOwnerRmId().equals(rmId)) {
            // Không tiết lộ tồn tại — 403 (BR1.1).
            throw new ForbiddenException("Tiềm năng bán không thuộc RM hiện tại");
        }
        return lead;
    }

    /** Kiểm quyền sở hữu, ném 403/404 như {@link #getOwnedLead} (BR1.1/BR1.2). */
    @Transactional(readOnly = true)
    public void assertOwned(String rmId, String leadId) {
        getOwnedLead(rmId, leadId);
    }

    /** Khách hàng gắn với lead (phục vụ chi tiết lead, BR2.1). */
    @Transactional(readOnly = true)
    public Optional<Customer> getCustomer(String customerId) {
        return customers.findById(customerId);
    }

    /** Mọi lead của RM, sắp theo ưu tiên rồi SLA (danh sách lead / fallback demo). */
    @Transactional(readOnly = true)
    public List<Lead> findLeadsByRm(String rmId) {
        return leads.findByOwnerRmIdOrderByPriorityAscSlaDueAtAsc(rmId);
    }

    /** Lịch sử hoạt động của lead của RM, mới nhất trước (BR2.2). */
    @Transactional(readOnly = true)
    public List<Activity> getActivities(String rmId, String leadId) {
        assertOwned(rmId, leadId);
        return activities.findByLeadIdOrderByOccurredAtDesc(leadId);
    }

    /**
     * Chuyển giai đoạn lead theo tập hợp lệ (SM1). Chuyển ngoài tập → 400
     * VALIDATION_FAILED (field=leadStage). Dành cho U4 kích hoạt khi lưu keyin.
     */
    @Transactional
    public void updateStage(String rmId, String leadId, LeadStage target) {
        Lead lead = getOwnedLead(rmId, leadId);
        if (target == null) {
            throw new ValidationException("leadStage", "Giai đoạn không hợp lệ");
        }
        if (lead.getStage() == target) {
            return; // không đổi
        }
        if (!lead.getStage().canTransitionTo(target)) {
            throw new ValidationException("leadStage",
                    "Không thể chuyển giai đoạn từ " + lead.getStage() + " sang " + target);
        }
        lead.setStage(target);
        leads.save(lead);
    }

    /**
     * Ghi một hoạt động loại cuộc gọi cho lead — idempotent theo {@code callId}
     * (I1). Dành cho U4 ghi tóm tắt/gợi ý AI khi lưu keyin. Gọi lại cùng callId
     * cập nhật bản ghi hiện có thay vì tạo trùng.
     *
     * @return activityId của hoạt động
     */
    @Transactional
    public String recordCallActivity(String rmId, String leadId, String callId,
                                     String product, String result, String note,
                                     String aiSummary, List<String> aiSuggestions) {
        assertOwned(rmId, leadId);
        if (callId == null || callId.isBlank()) {
            throw new ValidationException("callId", "callId là bắt buộc cho hoạt động cuộc gọi");
        }
        Activity activity = activities.findByCallId(callId).orElse(null);
        if (activity == null) {
            activity = new Activity(UUID.randomUUID().toString(), leadId, ActivityType.CUOC_GOI,
                    OffsetDateTime.now());
            activity.setCallId(callId);
        }
        activity.setProduct(product);
        activity.setResult(result);
        activity.setNote(note);
        activity.setAiSummary(aiSummary);
        if (aiSuggestions != null) {
            activity.setAiSuggestions(aiSuggestions);
        }
        activities.save(activity);
        return activity.getActivityId();
    }

    /**
     * Lead kế tiếp (BR5.1): ưu tiên cao nhất ở {@link LeadStage#OPEN}, loại
     * {@code excludeLeadId}. Sắp theo priority → rankScore giảm dần → slaDueAt
     * tăng dần. Rỗng → {@link Optional#empty()} (giao diện báo 204).
     */
    @Transactional(readOnly = true)
    public Optional<Lead> findNextLead(String rmId, String excludeLeadId) {
        return leads.findByOwnerRmIdAndStageIn(rmId, LeadStage.OPEN).stream()
                .filter(l -> excludeLeadId == null || !l.getLeadId().equals(excludeLeadId))
                .min(Comparator
                        .comparing((Lead l) -> l.getPriority().ordinal())
                        .thenComparing(l -> l.getRankScore() == null ? Integer.MIN_VALUE : l.getRankScore(),
                                Comparator.reverseOrder())
                        .thenComparing(Lead::getSlaDueAt,
                                Comparator.nullsLast(Comparator.naturalOrder())));
    }
}

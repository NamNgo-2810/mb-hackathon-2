package com.mb.callassistant.web;

import com.mb.callassistant.lead.CurrentRmProvider;
import com.mb.callassistant.lead.Lead;
import com.mb.callassistant.lead.LeadService;
import com.mb.callassistant.web.dto.LeadSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Danh sách lead của RM hiện tại (C5, U6): {@code GET /api/leads}. U6 chỉ sở hữu
 * ĐÚNG collection path này — U1 vẫn sở hữu {@code /api/leads/demo},
 * {@code /api/leads/next}, {@code /api/leads/{leadId}}; path literal được ưu tiên
 * so với pattern {@code {leadId}} nên không nhập nhằng.
 *
 * <p>RM hiện tại lấy qua {@link CurrentRmProvider} (chế độ login: RM của token,
 * BR2.2/BR4.1). Trả CHỈ lead của RM đó (BR4.1), sắp theo ưu tiên rồi hạn SLA tăng
 * dần (BR4.2); rỗng → {@code []} (BR4.3).
 */
@RestController
@RequestMapping("/api")
public class LeadListController {

    /**
     * So sánh ưu tiên theo thứ hạng nghiệp vụ: KHAI_THAC_NGAY > HOT_LEAD > THUONG
     * (BR4.2). Dùng {@code ordinal()} vì thứ tự khai báo enum {@code LeadPriority}
     * trùng với thứ tự ưu tiên giảm dần (đã xác minh ở LeadPriority.java), rồi
     * {@code slaDueAt} tăng dần với null xếp cuối cùng mức ưu tiên.
     */
    private static final Comparator<Lead> BY_PRIORITY_THEN_SLA =
            Comparator.comparingInt((Lead l) -> l.getPriority().ordinal())
                    .thenComparing(Lead::getSlaDueAt,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    private final CurrentRmProvider currentRmProvider;
    private final LeadService leadService;

    public LeadListController(CurrentRmProvider currentRmProvider, LeadService leadService) {
        this.currentRmProvider = currentRmProvider;
        this.leadService = leadService;
    }

    /** Danh sách lead của RM hiện tại (BR4.1–BR4.3). */
    @GetMapping("/leads")
    public List<LeadSummary> myLeads() {
        String rmId = currentRmProvider.currentRmId();
        return leadService.findLeadsByRm(rmId).stream()
                .sorted(BY_PRIORITY_THEN_SLA)
                .map(lead -> LeadSummary.from(lead,
                        leadService.getCustomer(lead.getCustomerId()).orElse(null)))
                .toList();
    }
}

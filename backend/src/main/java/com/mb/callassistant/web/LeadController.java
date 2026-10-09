package com.mb.callassistant.web;

import com.mb.callassistant.lead.Activity;
import com.mb.callassistant.lead.CurrentRmProvider;
import com.mb.callassistant.lead.Lead;
import com.mb.callassistant.lead.LeadService;
import com.mb.callassistant.lead.RmUser;
import com.mb.callassistant.web.dto.ActivityDto;
import com.mb.callassistant.web.dto.LeadDetail;
import com.mb.callassistant.web.dto.LeadSummary;
import com.mb.callassistant.web.dto.Rm;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Lead API (C1): RM hiện tại, lead mẫu demo, chi tiết lead, lịch sử hoạt động,
 * lead kế tiếp. Lỗi 403/404/204 theo BR1.1/BR1.2/BR5.1 qua
 * {@link ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/api")
public class LeadController {

    private final CurrentRmProvider currentRmProvider;
    private final LeadService leadService;

    public LeadController(CurrentRmProvider currentRmProvider, LeadService leadService) {
        this.currentRmProvider = currentRmProvider;
        this.leadService = leadService;
    }

    /** RM hiện tại (chế độ demo trả RM mẫu). */
    @GetMapping("/me")
    public Rm me() {
        RmUser rm = currentRmProvider.currentRm();
        return Rm.from(rm);
    }

    /**
     * Lead mẫu để mở thẳng S1 khi chưa có U6 — lead đầu tiên của RM hiện tại
     * (chế độ demo). 404 nếu chưa seed.
     */
    @GetMapping("/leads/demo")
    public LeadDetail demoLead() {
        String rmId = currentRmProvider.currentRmId();
        Lead lead = leadService.findNextLead(rmId, null)
                .or(() -> firstAnyLead(rmId))
                .orElseThrow(() -> new NotFoundException("Chưa có lead mẫu (chưa seed hoặc không ở chế độ demo)"));
        return LeadDetail.from(lead, leadService.getCustomer(lead.getCustomerId()).orElse(null));
    }

    /** Chi tiết lead của RM (BR2.1). 403 lead khác RM, 404 không tồn tại. */
    @GetMapping("/leads/{leadId}")
    public LeadDetail lead(@PathVariable String leadId) {
        String rmId = currentRmProvider.currentRmId();
        Lead lead = leadService.getOwnedLead(rmId, leadId);
        return LeadDetail.from(lead, leadService.getCustomer(lead.getCustomerId()).orElse(null));
    }

    /** Lịch sử hoạt động của lead, mới nhất trước (BR2.2). 403 lead khác RM. */
    @GetMapping("/leads/{leadId}/activities")
    public List<ActivityDto> activities(@PathVariable String leadId) {
        String rmId = currentRmProvider.currentRmId();
        List<Activity> list = leadService.getActivities(rmId, leadId);
        return list.stream().map(ActivityDto::from).toList();
    }

    /** Lead kế tiếp (FR4.9, BR5.1). 204 khi không còn lead cần xử lý. */
    @GetMapping("/leads/next")
    public ResponseEntity<LeadSummary> nextLead(@RequestParam(required = false) String excludeLeadId) {
        String rmId = currentRmProvider.currentRmId();
        Optional<Lead> next = leadService.findNextLead(rmId, excludeLeadId);
        if (next.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        Lead lead = next.get();
        LeadSummary summary = LeadSummary.from(lead,
                leadService.getCustomer(lead.getCustomerId()).orElse(null));
        return ResponseEntity.ok(summary);
    }

    /** Fallback cho lead demo khi mọi lead đã đóng: lead đầu tiên bất kỳ của RM. */
    private Optional<Lead> firstAnyLead(String rmId) {
        return leadService.findLeadsByRm(rmId).stream().findFirst();
    }
}

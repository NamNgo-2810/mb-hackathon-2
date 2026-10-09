package com.mb.callassistant.lead;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, String> {

    /** Lịch sử của một lead, mới nhất trước (BR2.2). */
    List<Activity> findByLeadIdOrderByOccurredAtDesc(String leadId);

    /** Hoạt động cuộc gọi đã ghi cho callId — nền tảng ghi idempotent (I1). */
    Optional<Activity> findByCallId(String callId);
}

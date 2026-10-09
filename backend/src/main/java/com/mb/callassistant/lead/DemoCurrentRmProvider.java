package com.mb.callassistant.lead;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Bản demo của {@link CurrentRmProvider} (ADR-007, BR1.3): luôn trả về một RM
 * mẫu cố định, không cần token. Chỉ hoạt động khi {@code app.auth.mode=demo}
 * (mặc định). U6 cung cấp bản login riêng.
 */
@Component
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "demo", matchIfMissing = true)
public class DemoCurrentRmProvider implements CurrentRmProvider {

    /** Id ổn định của RM mẫu; dùng chung với {@code DemoDataSeeder}. */
    public static final String DEMO_RM_ID = "rm-demo-001";
    public static final String DEMO_RM_USERNAME = "demo.rm";
    public static final String DEMO_RM_DISPLAY_NAME = "RM Demo";

    private final RmUserRepository rmUsers;

    public DemoCurrentRmProvider(RmUserRepository rmUsers) {
        this.rmUsers = rmUsers;
    }

    @Override
    public RmUser currentRm() {
        // Trả RM mẫu đã seed; nếu kho trống (chưa seed) dựng một bản tạm trong bộ
        // nhớ để chế độ demo không chặn thao tác.
        return rmUsers.findById(DEMO_RM_ID)
                .orElseGet(() -> new RmUser(DEMO_RM_ID, DEMO_RM_USERNAME, DEMO_RM_DISPLAY_NAME));
    }
}

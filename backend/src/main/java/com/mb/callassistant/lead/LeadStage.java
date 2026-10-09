package com.mb.callassistant.lead;

import java.util.EnumSet;
import java.util.Set;

/**
 * Giai đoạn vòng đời lead (SM1). Tập chuyển hợp lệ:
 * {@code CHUA_TIEP_CAN -> {DANG_TIEP_CAN, TU_CHOI}};
 * {@code DANG_TIEP_CAN -> {CHUYEN_CO_HOI, TU_CHOI}};
 * {@code CHUYEN_CO_HOI} và {@code TU_CHOI} là trạng thái cuối.
 */
public enum LeadStage {
    CHUA_TIEP_CAN,
    DANG_TIEP_CAN,
    CHUYEN_CO_HOI,
    TU_CHOI;

    /** Tập trạng thái "đang mở" — ứng viên lead kế tiếp (BR5.1). */
    public static final Set<LeadStage> OPEN = EnumSet.of(CHUA_TIEP_CAN, DANG_TIEP_CAN);

    /** True nếu được phép chuyển từ trạng thái này sang {@code target} (SM1). */
    public boolean canTransitionTo(LeadStage target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case CHUA_TIEP_CAN -> target == DANG_TIEP_CAN || target == TU_CHOI;
            case DANG_TIEP_CAN -> target == CHUYEN_CO_HOI || target == TU_CHOI;
            case CHUYEN_CO_HOI, TU_CHOI -> false;
        };
    }
}

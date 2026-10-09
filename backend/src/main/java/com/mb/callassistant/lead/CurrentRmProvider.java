package com.mb.callassistant.lead;

/**
 * I1: nguồn danh tính RM hiện tại (BR1.3, ADR-007). Ở chế độ demo trả về một RM
 * mẫu cố định (không cần token); ở chế độ login (U6) trả RM của token Bearer.
 * U1 chỉ ship bản demo.
 */
public interface CurrentRmProvider {

    /** RM đang thao tác. Không bao giờ null ở chế độ demo. */
    RmUser currentRm();

    /** Tiện ích: id của RM hiện tại. */
    default String currentRmId() {
        return currentRm().getRmId();
    }
}

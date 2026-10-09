package com.mb.callassistant.lead;

/**
 * Mức ưu tiên của lead. Thứ tự khai báo = thứ tự ưu tiên giảm dần
 * (KHAI_THAC_NGAY cao nhất) để sắp "lead kế tiếp" (BR5.1).
 */
public enum LeadPriority {
    KHAI_THAC_NGAY,
    HOT_LEAD,
    THUONG
}

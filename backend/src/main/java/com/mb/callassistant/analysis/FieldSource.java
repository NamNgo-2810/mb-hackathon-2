package com.mb.callassistant.analysis;

/**
 * Nguồn của từng trường keyin (BR4.3): dùng để đo tỷ lệ chấp nhận gợi ý AI.
 * <ul>
 *   <li>{@link #AI_KEPT} — RM giữ nguyên giá trị AI điền.</li>
 *   <li>{@link #RM_EDITED} — RM sửa giá trị AI đã điền.</li>
 *   <li>{@link #RM_ENTERED} — AI không điền, RM tự nhập.</li>
 * </ul>
 */
public enum FieldSource {
    AI_KEPT,
    RM_EDITED,
    RM_ENTERED
}

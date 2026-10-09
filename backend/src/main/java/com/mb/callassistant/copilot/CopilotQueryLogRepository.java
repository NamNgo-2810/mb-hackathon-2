package com.mb.callassistant.copilot;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Kho nhật ký truy vấn Copilot, lưu trong bộ nhớ (BR7.1). Mỗi lượt hỏi ghi đúng
 * một {@link CopilotQueryLog} để đếm lượt dùng cho OKR; demo không cần CSDL.
 */
@Repository
public class CopilotQueryLogRepository {

    private final List<CopilotQueryLog> logs = new CopyOnWriteArrayList<>();

    /** Ghi một dòng nhật ký (BR7.1). */
    public CopilotQueryLog save(CopilotQueryLog log) {
        logs.add(log);
        return log;
    }

    /** Toàn bộ nhật ký, theo thứ tự ghi (đếm OKR). */
    public List<CopilotQueryLog> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(logs));
    }

    /** Số lượt hỏi đã ghi. */
    public long count() {
        return logs.size();
    }
}

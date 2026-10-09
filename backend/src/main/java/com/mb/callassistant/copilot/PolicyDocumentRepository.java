package com.mb.callassistant.copilot;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Kho tài liệu + chỉ mục đoạn, lưu trong bộ nhớ (BR7.1 nền; AC1.3.2 qua
 * {@code GET /api/copilot/documents}). Tài liệu nạp một lần lúc khởi động bởi
 * {@link PolicyDocumentLoader}, nên không cần CSDL. Giữ danh sách
 * {@link PolicyDocument} theo thứ tự nạp và chỉ mục {@link DocumentChunk}.
 *
 * <p>Thread-safe cho một lần ghi lúc khởi động + nhiều lần đọc khi phục vụ.
 */
@Repository
public class PolicyDocumentRepository {

    private final List<PolicyDocument> documents = new CopyOnWriteArrayList<>();
    private final List<DocumentChunk> chunks = new CopyOnWriteArrayList<>();

    /** Nạp một tài liệu hợp lệ cùng các đoạn của nó (BR1.3). */
    public void add(PolicyDocument document, List<DocumentChunk> documentChunks) {
        documents.add(document);
        if (documentChunks != null) {
            chunks.addAll(documentChunks);
        }
    }

    /** Danh sách tài liệu đã nạp, theo thứ tự nạp (AC1.3.2). */
    public List<PolicyDocument> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(documents));
    }

    /** Toàn bộ đoạn trong chỉ mục để chấm điểm từ khoá (BR2.2). */
    public List<DocumentChunk> allChunks() {
        return Collections.unmodifiableList(new ArrayList<>(chunks));
    }

    /** Tìm tài liệu theo id (dùng khi kiểm chứng/điền trích dẫn). */
    public Optional<PolicyDocument> findById(String documentId) {
        if (documentId == null) {
            return Optional.empty();
        }
        return documents.stream().filter(d -> documentId.equals(d.getDocumentId())).findFirst();
    }

    /** Số tài liệu đã nạp; chỉ mục rỗng ⇒ mọi câu hỏi trả "không tìm thấy" (WF1.4). */
    public int documentCount() {
        return documents.size();
    }

    /** Xoá chỉ mục (dùng khi nạp lại). */
    public void clear() {
        documents.clear();
        chunks.clear();
    }
}

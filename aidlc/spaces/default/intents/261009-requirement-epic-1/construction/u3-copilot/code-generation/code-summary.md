# Code Summary — U3 `u3-copilot`

Copilot tra cứu chính sách (CopilotComponent, hợp đồng C2) đã được hiện thực theo
`functional-spec.md` (WF1–WF4), `entities.md`, `rules.md` (BR1–BR7) và các AC
US1.2/US1.3. Mã mới nằm trong package `com.mb.callassistant.copilot`, controller
`web/CopilotController.java`, các DTO `web/dto/Copilot*`, và panel Copilot trên
màn S2. Tích hợp U2 `LlmClient` (I3) và U1 `CurrentRmProvider` (I1) đúng hợp đồng.

## Files đã tạo / sửa

### Backend — package `copilot` (mới)
- `CopilotMessages.java` — hằng số câu "không tìm thấy" cố định (BR3.3).
- `PolicyDocument.java`, `DocumentChunk.java`, `RetrievedChunk.java`, `Citation.java`,
  `CopilotAnswer.java`, `CopilotQueryLog.java` — mô hình thực thể/value object
  (entities.md), ràng buộc page|section, score>0, nhánh không-tìm-thấy (BR1.3/BR3.1/BR4.1).
- `PolicyDocumentRepository.java`, `CopilotQueryLogRepository.java` — kho **in-memory**
  (`@Repository`): tài liệu nạp một lần lúc khởi động, log để đếm OKR (BR7.1).
- `TextNormalizer.java` — bỏ dấu tiếng Việt (NFKD + đ→d), hạ thường, tách từ (BR2.1).
- `PolicyDocumentLoader.java` — `ApplicationRunner` nạp/lập chỉ mục khi khởi động;
  đọc metadata `--- title/effectiveDate/sourcePdf ---`, cắt đoạn theo `[[page:n]]`/`## Mục`,
  bỏ qua + log file thiếu metadata/rỗng (BR1.1/BR1.2/BR1.3).
- `ChunkSearcher.java` — chấm điểm theo số từ khoá trùng, chọn ≤ topK, score>0 (BR2.2).
- `CopilotPromptBuilder.java` — systemPrompt (chỉ trả lời từ đoạn, JSON) + userContent
  (đoạn trong khối DATA + lịch sử ≤10 lượt) (BR2.3/BR6.2).
- `CitationVerifier.java` — kiểm chứng trích dẫn với đoạn đã gửi, loại trích dẫn bịa,
  rỗng ⇒ ép không-tìm-thấy, cắt gợi ý 2–4 (BR4.1/BR4.2/BR3.2/BR6.1).
- `CopilotService.java` — điều phối WF2: validate biên, RM qua I1, nhánh rỗng không gọi
  LLM, gọi `LlmClient.complete(expectJson, timeout 30s)`, bắt `LlmException`→`AiException`,
  JSON sai→BAD_RESPONSE, ghi đúng một log mỗi lượt (BR3.1/BR5.1/BR5.2/BR5.3/BR7.1/BR7.3).
- `AiException.java`, `CopilotValidationException.java` — lỗi 502 AI_ERROR và 400 VALIDATION_FAILED.

### Backend — web (sửa tại chỗ + mới)
- `web/CopilotController.java` (mới) — `POST /api/copilot/ask`, `GET /api/copilot/documents`.
- `web/dto/CopilotAskRequest.java`, `CopilotAnswerDto.java`, `CitationDto.java`,
  `CopilotDocumentDto.java` (mới) — khớp schema C2.
- `web/ApiExceptionHandler.java` (sửa) — thêm handler `AiException`→`502 AI_ERROR`,
  `CopilotValidationException`→`400 VALIDATION_FAILED`, thông điệp tiếng Việt (BR5.2).
- `config/AppProperties.java` (sửa) — thêm khối lồng `Copilot` (docsDir, topK,
  maxHistoryTurns, maxQuestionLength, llmTimeoutMs, maxOutputTokens).

### Cấu hình & build
- `application.properties`, `application-demo.properties` (sửa) — thêm `app.copilot.*`.
- `pom.xml` (sửa) — thêm `com.mb.callassistant.copilot.*` vào JaCoCo `includes` (sàn 0.80).
- `backend/policy-docs/bieu-lai-suat-mau.md`, `dieu-kien-vay-mau.md` (mới) — tài liệu
  mẫu GIẢ, chỉ để demo chạy.

### Frontend
- `public/screens/2-thuc-hien-cuoc-goi.html` (sửa) — thay slot placeholder bằng panel
  Copilot theo mockup gốc: cột lịch sử + "Cuộc trò chuyện mới", vùng câu trả lời +
  trích dẫn, chip gợi ý, ô nhập; `data-testid` trên mọi phần tử tương tác, nhãn
  accessible, escape toàn bộ text động.
- `public/js/copilot.js` (mới) — giữ lịch sử phiên ≤10 lượt, gọi `askCopilot`, render
  markdown đơn giản có escape, lỗi 502 CHỈ trong panel, "trò chuyện mới" = history rỗng.
- `public/js/api.js` (sửa) — thêm `askCopilot(question, callId, history)` và
  `getCopilotDocuments()`.

## Quyết định & lệch so với kế hoạch

- **Kho in-memory thay vì JPA**: `CallRecordRepository` thực chất là `JpaRepository`,
  nhưng kế hoạch U3 nêu rõ "in-memory". Để đúng ý định (tài liệu nạp một lần, log chỉ
  đếm OKR demo) và tránh thêm bảng/`@Entity` mới, `PolicyDocument` và `CopilotQueryLog`
  là POJO và hai repository là `@Repository` in-memory (`CopyOnWriteArrayList`). Không
  trùng lặp file, không đụng mã U1/U2.
- **502 AI_ERROR dùng `HttpStatus.BAD_GATEWAY`** — nhất quán với tiền lệ `STT_ERROR`
  (502) trong `ApiExceptionHandler`.
- **Chuẩn hoá tiếng Việt** xử lý riêng `đ/Đ → d/D` (không mang dấu tổ hợp NFKD).
- **Khối DATA hai lớp**: `CopilotPromptBuilder` tự đánh dấu đoạn là dữ liệu, và
  `LlmClient` (U2 `MessageBuilder`) còn bọc `userContent` trong fence DATA — chống
  prompt-injection theo cả hai lớp (BR2.3, AC1.2.5).

## Trạng thái kiểm thử

**Hoãn theo yêu cầu người dùng** ("gen code cho tất cả các unit một lượt, phần test
để sau"). Đợt này KHÔNG tạo file test và KHÔNG chạy `mvn verify`/`mvn test`/`npm test`.
Phương pháp **test-after** theo `## Testing Contract` giữ nguyên; sàn độ phủ **80%
dòng** cho package `com.mb.callassistant.copilot` **không bị hạ** — chỉ kiểm chứng ở
đợt test sau (xem `unit-test-instructions.md`). Các bước `(DỜI — test)` trong kế hoạch
để nguyên chưa tick.

## Cấu hình

- `app.copilot.docs-dir` (mặc định `backend/policy-docs`, env `COPILOT_DOCS_DIR`),
  `top-k=5`, `max-history-turns=10`, `max-question-length=1000`, `llm-timeout-ms=30000`,
  `max-output-tokens=1024`.
- Profile `demo`: `app.llm.provider=mock` ⇒ Copilot chạy offline, không cần
  `ANTHROPIC_API_KEY`. Profile mặc định dùng Claude thật khi có khoá.
- Không hardcode khoá/bí mật; log Copilot chỉ chứa rmId/answerFound/số đoạn/kind lỗi,
  không chứa toàn văn câu hỏi/tài liệu (BR7.3).

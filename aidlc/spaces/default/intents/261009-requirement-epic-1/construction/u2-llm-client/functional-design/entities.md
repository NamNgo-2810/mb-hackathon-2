# Entities — U2 `u2-llm-client`

U2 là một thư viện (`kind: library`). Thư viện này không lưu dữ liệu nghiệp vụ và không có bảng nào trong CSDL. Mọi thực thể bên dưới đều là **đối tượng giá trị** (value object): chúng chỉ tồn tại trong lúc gọi LLM. Riêng `LlmConfig` được nạp từ biến môi trường khi ứng dụng khởi động.

Nguồn: `components.md` (LlmClient), `contract-summary.md` (I3), `decisions.md` (ADR-004), `requirements.md` (NFR1–NFR3).

```yaml
entities:
  - name: LlmConfig
    description: Cấu hình cổng LLM, nạp một lần khi khởi động từ biến môi trường / cấu hình ứng dụng.
    kind: value-object
    attributes:
      - { name: apiKey, type: secret-string, required: false, constraints: "đọc từ ANTHROPIC_API_KEY; không bao giờ ghi log, không trả ra ngoài; vắng = chưa cấu hình" }
      - { name: model, type: string, required: true, default: "cấu hình (người dùng chọn khi có khoá)", constraints: "không rỗng" }
      - { name: provider, type: enum, required: true, allowed: [ANTHROPIC, MOCK], default: ANTHROPIC }
      - { name: defaultTimeoutMs, type: integer, required: true, default: 30000, min: 1000, max: 120000 }
      - { name: maxOutputTokensCap, type: integer, required: true, default: 4096, min: 1 }
    constraints:
      - "provider = ANTHROPIC và apiKey vắng ⇒ trạng thái NOT_CONFIGURED (không chặn khởi động ứng dụng)"
  - name: LlmRequest
    description: Một yêu cầu gửi LLM do U3 (Copilot) hoặc U4 (CallAnalysis) tạo.
    kind: value-object
    attributes:
      - { name: systemPrompt, type: text, required: true, constraints: "không rỗng; chỉ dẫn của hệ thống" }
      - { name: cachedContext, type: text, required: false, constraints: "phần nội dung cố định có thể dùng prompt caching (ví dụ khung chấm điểm, mô tả trường keyin)" }
      - { name: userContent, type: text, required: true, constraints: "dữ liệu không tin cậy (transcript, đoạn tài liệu, câu hỏi) — luôn được bọc trong khối phân tách là DỮ LIỆU" }
      - { name: maxOutputTokens, type: integer, required: true, min: 1, constraints: "≤ maxOutputTokensCap" }
      - { name: timeoutMs, type: integer, required: false, constraints: "vắng ⇒ defaultTimeoutMs; ≤ 120000" }
      - { name: expectJson, type: boolean, required: true, default: false }
      - { name: purpose, type: enum, required: true, allowed: [CALL_ANALYSIS, COPILOT], constraints: "dùng cho log/đo lường, không ảnh hưởng hành vi" }
  - name: LlmResponse
    description: Kết quả thành công của một lần gọi.
    kind: value-object
    attributes:
      - { name: text, type: text, required: true }
      - { name: model, type: string, required: true }
      - { name: inputTokens, type: integer, required: false, min: 0 }
      - { name: outputTokens, type: integer, required: false, min: 0 }
      - { name: cachedInputTokens, type: integer, required: false, min: 0 }
      - { name: latencyMs, type: integer, required: true, min: 0 }
      - { name: truncated, type: boolean, required: true, constraints: "true khi dừng vì chạm maxOutputTokens" }
  - name: LlmError
    description: Lỗi phân loại được, ném về thành phần gọi để chọn dự phòng.
    kind: value-object
    attributes:
      - { name: kind, type: enum, required: true, allowed: [NOT_CONFIGURED, TIMEOUT, UNAVAILABLE, RATE_LIMITED, BAD_RESPONSE] }
      - { name: message, type: string, required: true, constraints: "không chứa khoá API hay nội dung prompt" }
      - { name: retryable, type: boolean, required: true }
      - { name: providerStatus, type: integer, required: false }
relationships:
  - "LlmRequest → LlmResponse | LlmError: mỗi lần gọi trả đúng một trong hai"
  - "LlmConfig được dùng bởi mọi lần gọi; không có quan hệ lưu trữ"
```

## Tóm tắt

| Thực thể | Loại | Vai trò |
|---|---|---|
| LlmConfig | Cấu hình | Khóa API, model, timeout, nhà cung cấp (ANTHROPIC hoặc MOCK) |
| LlmRequest | Đầu vào | Gồm prompt hệ thống, ngữ cảnh cố định có thể cache, và dữ liệu người dùng được tách riêng |
| LlmResponse | Đầu ra | Văn bản trả về, model, số token, độ trễ |
| LlmError | Lỗi | 5 loại lỗi, kèm cờ cho biết có nên thử lại hay không |

## Sources

- `components.md`, `contract-summary.md` (I3), `decisions.md` (ADR-004), `requirements.md` (NFR1–NFR3), `unit-of-work.md` (U2)

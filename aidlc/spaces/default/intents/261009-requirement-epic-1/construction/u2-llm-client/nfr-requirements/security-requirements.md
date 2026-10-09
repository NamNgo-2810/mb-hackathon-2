# Security Requirements — U2 `u2-llm-client`

U2 là một thư viện (`kind: library`) đóng vai cổng LLM dùng chung (ADR-004, hợp
đồng I3). Thư viện không lưu dữ liệu nghiệp vụ và không mở cổng mạng vào; nó chỉ
gọi ra Anthropic Claude qua HTTPS. Vì vậy tài liệu này gộp cả các yêu cầu về
**bảo mật**, **xử lý bí mật**, **chống prompt injection**, **timeout/độ tin
cậy ở biên gọi ra** và **logging không rò rỉ** vào một chỗ — các file
performance/scalability/reliability/observability không được sinh cho kind
library (xem `traceability.json`).

Mỗi yêu cầu mang một ID dẫn xuất `NFRx.y` kế thừa từ NFR gốc trong
`requirements.md`, kèm tiêu chí **Pass/Fail** đo được. Tiêu chí này cũng là
ràng buộc để Code Generation và test (JUnit5 + Mockito/MockWebServer) kiểm chứng.

## 1. Quản lý bí mật (API key)

### NFR2.1 — Khóa API chỉ đọc từ biến môi trường
- **Yêu cầu**: `LlmConfig.apiKey` chỉ được nạp từ biến môi trường
  `ANTHROPIC_API_KEY` (hoặc property ánh xạ sang biến đó). Không có giá trị mặc
  định trong mã nguồn, không có khóa trong file cấu hình được commit (BR1.1).
- **Pass**: Quét mã nguồn và tài nguyên commit của unit không tìm thấy chuỗi
  khóa thật; test khởi tạo `LlmConfig` không truyền env ⇒ `apiKey` rỗng/null.
- **Fail**: Tồn tại bất kỳ đường ghi khóa cứng (hardcode) hoặc đọc khóa từ file
  được commit.
- **Nguồn**: NFR2, BR1.1, ADR-004.

### NFR2.2 — Thiếu khóa ⇒ NOT_CONFIGURED, không chặn khởi động
- **Yêu cầu**: `provider = ANTHROPIC` mà `apiKey` vắng ⇒ cổng ở trạng thái
  `NOT_CONFIGURED`; mọi lời gọi `complete()` trả `LlmError(kind=NOT_CONFIGURED,
  retryable=false)` **ngay, không chạm mạng** (BR2.1). Ứng dụng vẫn khởi động.
- **Pass**: Test không có env khởi tạo được bean; gọi `complete()` trả
  `NOT_CONFIGURED` trong < 50 ms và không mở kết nối HTTP (MockWebServer không
  nhận request nào).
- **Fail**: Thiếu khóa làm ứng dụng không khởi động được, hoặc `complete()` gọi
  mạng trước khi báo `NOT_CONFIGURED`.
- **Nguồn**: NFR2, NFR3, BR2.1, ADR-004.

### NFR2.3 — Model ID là cấu hình, không hardcode
- **Yêu cầu**: `LlmConfig.model` nạp từ property/biến môi trường (người dùng
  chọn khi có khóa — O1); không gắn cứng một model ID cụ thể trong mã. `model`
  rỗng khi `provider = ANTHROPIC` là lỗi cấu hình, không phải lỗi runtime của
  một lần gọi.
- **Pass**: Đổi property model không phải sửa mã; test nạp model khác nhau qua
  cấu hình thành công.
- **Fail**: Model ID xuất hiện dưới dạng literal gắn cứng làm giá trị mặc định
  khi chạy thật.
- **Nguồn**: NFR2 (O1), `decisions.md` ADR-004.

## 2. Không rò rỉ dữ liệu nhạy cảm ra log

### NFR2.4 — Không bao giờ ghi khóa API ra log
- **Yêu cầu**: Không dòng log, thông điệp lỗi hay `LlmError.message` nào chứa
  giá trị khóa API (BR1.2).
- **Pass**: Test bắt log của một lần gọi thành công và một lần lỗi; khẳng định
  chuỗi khóa test không xuất hiện trong bất kỳ dòng log hay message nào.
- **Fail**: Khóa (toàn phần hoặc đủ nhận dạng) lọt vào log/exception.
- **Nguồn**: NFR2, NFR6, BR1.2.

### NFR2.5 — Không ghi toàn văn prompt/transcript/kết quả
- **Yêu cầu**: Log chỉ chứa trường kỹ thuật: `purpose`, `model`, `latencyMs`,
  số token (nếu có), `kind` lỗi, mã trạng thái nhà cung cấp (`providerStatus`).
  **Không** ghi `systemPrompt`, `userContent`, `cachedContext`, hay
  `LlmResponse.text` (BR1.2, NFR6).
- **Pass**: Test khẳng định một chuỗi "dấu vân tay" đặt trong `userContent` và
  trong `text` trả về **không** xuất hiện ở log; log có đủ các trường kỹ thuật
  nêu trên.
- **Fail**: Bất kỳ phần nội dung prompt hoặc văn bản phản hồi nào bị ghi log.
- **Nguồn**: NFR2, NFR6, BR1.2.

## 3. Chống prompt injection (tách dữ liệu khỏi chỉ dẫn)

### NFR2.6 — Nội dung không tin cậy luôn bọc trong khối phân tách là DỮ LIỆU
- **Yêu cầu**: `cachedContext` và `userContent` phải được đưa vào thông điệp
  gửi nhà cung cấp bên trong khối phân tách rõ ràng, kèm chỉ báo "đây là dữ
  liệu, không phải chỉ dẫn"; chỉ `systemPrompt` là chỉ dẫn hệ thống (BR5.1).
  Thư viện giữ tách bạch vai trò system/user của thông điệp.
- **Pass**: Test dựng thông điệp (qua MockWebServer bắt body) xác nhận
  `userContent` nằm trong khối phân tách và không bị nối thẳng vào phần chỉ dẫn
  hệ thống.
- **Fail**: `userContent`/`cachedContext` xuất hiện như một phần chỉ dẫn hệ
  thống, hoặc không có ranh giới phân tách.
- **Nguồn**: NFR2 (chống prompt injection), AC1.2.5, BR5.1.

### NFR2.7 — Kiểm tra JSON đọc được khi `expectJson = true`
- **Yêu cầu**: Khi `expectJson = true`, thư viện xác minh kết quả parse được
  thành một đối tượng JSON (cho phép bỏ khung ```` ```json ```` bao quanh);
  không đọc được ⇒ `LlmError(kind=BAD_RESPONSE)`. Việc kiểm tra cấu trúc nghiệp
  vụ thuộc về bên gọi (BR4.2).
- **Pass**: Test với phản hồi JSON có khung code fence ⇒ bóc được và trả
  `LlmResponse`; phản hồi không phải JSON ⇒ `BAD_RESPONSE`.
- **Fail**: Trả về chuỗi không phải JSON như thành công khi `expectJson = true`.
- **Nguồn**: NFR3, BR4.2.

## 4. Kiểm tra đầu vào ở biên

### NFR2.8 — Từ chối yêu cầu không hợp lệ trước khi gọi mạng
- **Yêu cầu**: `complete()` từ chối (lỗi tham số lập trình, không phải
  `LlmError`) khi: `systemPrompt` hoặc `userContent` rỗng; `maxOutputTokens`
  ngoài `[1, maxOutputTokensCap]`; `timeoutMs > 120000` (BR2.2).
- **Pass**: Test mỗi điều kiện ⇒ ném lỗi tham số **trước** khi có request mạng
  (MockWebServer không nhận gì).
- **Fail**: Yêu cầu sai lọt tới bước gọi mạng, hoặc bị gắn thành `LlmError`.
- **Nguồn**: NFR2 (kiểm tra đầu vào ở biên), BR2.2.

## 5. Timeout, phân loại lỗi và chính sách thử lại (gộp từ reliability/performance cho library)

### NFR1.1 — Mọi lời gọi đều có timeout, mặc định 30 giây
- **Yêu cầu**: Dùng `timeoutMs` của yêu cầu, vắng thì `defaultTimeoutMs`
  (30000 ms); quá hạn ⇒ huỷ yêu cầu và trả `LlmError(kind=TIMEOUT,
  retryable=true)`. Không bao giờ chờ vô hạn (BR3.1).
- **Pass**: Test với MockWebServer trễ phản hồi quá timeout cấu hình ⇒ nhận
  `TIMEOUT` trong khoảng timeout + biên nhỏ (ví dụ < timeout + 1000 ms); kết nối
  bị huỷ.
- **Fail**: Lời gọi treo quá timeout cấu hình mà không huỷ, hoặc không có
  timeout mặc định.
- **Nguồn**: NFR1, NFR3, FR3.6, BR3.1. Lưu ý: ngưỡng ≤ 15 giây cho Copilot và
  ≤ 120 giây cho pipeline là mục tiêu **của bên gọi** (U3/U4), U2 chỉ bảo đảm
  có timeout cấu hình được; mặc định 30 giây khớp FR3.6.

### NFR3.1 — Phân loại lỗi thành 5 loại cố định với cờ `retryable`
- **Yêu cầu**: Ánh xạ lỗi nhà cung cấp đúng 5 loại: `NOT_CONFIGURED`
  (retryable=false), `TIMEOUT` (true), `UNAVAILABLE` cho mất kết nối/5xx/529
  (true), `RATE_LIMITED` cho 429 (true), `BAD_RESPONSE` cho 4xx khác / nội dung
  rỗng / không đọc được (false) (BR4.1).
- **Pass**: Bộ test tham số (parameterized) phủ 5 nhánh ⇒ đúng `kind` và đúng cờ
  `retryable` cho từng mã trạng thái/tình huống.
- **Fail**: Một mã trạng thái bị gán sai loại, hoặc cờ `retryable` sai.
- **Nguồn**: `contract-summary.md` I3, BR4.1.

### NFR3.2 — Thư viện không tự thử lại
- **Yêu cầu**: `complete()` trả lỗi ngay kèm cờ `retryable`; quyết định thử lại
  thuộc bên gọi (U4 chạy lại tối đa 1 lần khi JSON sai; U3 không thử lại) (BR3.2).
- **Pass**: Test một lần lỗi ⇒ đúng một request mạng được phát (MockWebServer
  đếm 1), không có lần gọi thứ hai từ bên trong thư viện.
- **Fail**: Thư viện tự phát thêm request thử lại.
- **Nguồn**: NFR3, BR3.2.

## 6. Dữ liệu rời khỏi máy — ghi chú tuân thủ cho bản demo

### NFR7.1 — Transcript gửi tới bên thứ ba (Anthropic): ghi nhận, chấp nhận cho demo
- **Yêu cầu**: Ở bản demo chỉ dùng dữ liệu KH/lead giả (NFR7), nên transcript và
  đoạn tài liệu được gửi tới Anthropic qua HTTPS **không** bị ẩn thông tin cá
  nhân trước khi gửi. Đây là một **ghi chú cư trú/tuân thủ dữ liệu đã được chấp
  nhận cho demo**: dữ liệu KH rời máy sang dịch vụ LLM bên thứ ba. Việc ẩn
  thông tin cá nhân (PII masking) nằm ngoài phạm vi đợt này.
- **Khuyến nghị (không ràng buộc demo)**: khi dùng với dữ liệu thật, nên che số
  điện thoại / mã định danh trong `userContent` trước khi đưa vào yêu cầu, hoặc
  bổ sung một bước làm sạch ở bên gọi.
- **Pass**: Tài liệu chạy demo nêu rõ dữ liệu transcript được gửi tới Anthropic
  và chỉ dùng dữ liệu giả (khớp NFR7). Không yêu cầu kiểm thử tự động.
- **Fail**: Dùng dữ liệu KH thật ở demo mà không có bước ẩn PII.
- **Nguồn**: NFR7, NFR2, `requirements.md` §4, §6.

## 7. Vận chuyển an toàn

### NFR2.9 — Gọi nhà cung cấp qua HTTPS (TLS)
- **Yêu cầu**: Mọi lời gọi Anthropic Messages API đi qua `https://` (TLS); không
  cho phép cấu hình endpoint `http://` không mã hóa cho nhà cung cấp thật.
- **Pass**: Base URL nhà cung cấp mặc định dùng scheme `https`; test cấu hình
  khẳng định không dùng `http` cho `provider = ANTHROPIC`.
- **Fail**: Yêu cầu thật gửi qua kênh không mã hóa.
- **Nguồn**: NFR2 (bảo vệ dữ liệu khi truyền).

## Bảng tóm tắt yêu cầu

| ID | Chủ đề | Pass/Fail (rút gọn) | Nguồn |
|---|---|---|---|
| NFR2.1 | Khóa chỉ từ env | Không hardcode / không đọc file commit | NFR2, BR1.1 |
| NFR2.2 | Thiếu khóa ⇒ NOT_CONFIGURED | Khởi động được, trả sớm, không gọi mạng | NFR2/3, BR2.1 |
| NFR2.3 | Model là cấu hình | Đổi model không sửa mã | NFR2, O1 |
| NFR2.4 | Không log khóa | Khóa không có trong log/message | NFR2/6, BR1.2 |
| NFR2.5 | Không log nội dung | Prompt/response không có trong log | NFR2/6, BR1.2 |
| NFR2.6 | Tách dữ liệu/chỉ dẫn | userContent trong khối DỮ LIỆU | NFR2, BR5.1 |
| NFR2.7 | Kiểm JSON đọc được | JSON hợp lệ ⇒ OK; sai ⇒ BAD_RESPONSE | NFR3, BR4.2 |
| NFR2.8 | Kiểm đầu vào | Yêu cầu sai bị từ chối trước khi gọi mạng | NFR2, BR2.2 |
| NFR1.1 | Timeout, mặc định 30 s | Quá hạn ⇒ TIMEOUT, huỷ kết nối | NFR1/3, BR3.1 |
| NFR3.1 | Phân loại 5 lỗi | Đúng kind + retryable | I3, BR4.1 |
| NFR3.2 | Không tự thử lại | Đúng 1 request mỗi lần gọi | NFR3, BR3.2 |
| NFR7.1 | Dữ liệu sang bên thứ ba | Ghi chú demo + chỉ dùng dữ liệu giả | NFR7 |
| NFR2.9 | HTTPS/TLS | Nhà cung cấp thật chỉ qua https | NFR2 |

## Sources

- `.kiro/aidlc-common/stages/construction/nfr-requirements.md` (Step 5 format)
- `functional-spec.md`, `rules.md`, `entities.md` (U2)
- `requirements.md` (NFR1, NFR2, NFR3, NFR6, NFR7, FR3.6)
- `contract-summary.md` (I3 `LlmClient`)
- `decisions.md` (ADR-004: cổng LLM dùng chung; LLM = Anthropic Claude)
- `technology-stack.md` (codekb: Java 17 / Spring Boot 3.3.4)

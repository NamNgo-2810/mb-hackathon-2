# Architecture Decision Records — Domain Design

Các ADR dưới đây giải thích những lựa chọn ranh giới trong `components.md`. Nguồn: `requirements.md`, `stories.md`, `architecture.md`, `component-inventory.md`, `team-practices.md` và câu trả lời ở `domain-design-questions.md`.

## ADR-001: Mở rộng backend Spring Boot thành khối nguyên khối chia module

### Status: Accepted

- **Context**: Đã có backend Spring Boot đảm nhận cuộc gọi, ghi âm và STT (`architecture.md`). Hệ thống chỉ chạy cục bộ, không có CI hay triển khai (`team-practices.md`). Thời gian hackathon ngắn.
- **Decision**: Toàn bộ phần logic mới (Lead, Keyin, CallAnalysis, Copilot, Task, Auth, LlmClient) được viết thành các package riêng ngay trong backend hiện có. Các module dùng chung một CSDL, nhưng mỗi module sở hữu bảng của mình. Module khác không ghi trực tiếp vào bảng đó mà phải đi qua dịch vụ của module sở hữu [Q1=A].
- **Consequences**:
  - (+) Chỉ cần chạy một tiến trình, dễ debug và tận dụng được mã cùng cấu hình sẵn có.
  - (+) Các thao tác ghi chạm nhiều bảng (lưu keyin) nằm trong một giao dịch.
  - (−) Không mở rộng quy mô riêng cho phần AI được. Đây là điều chấp nhận được ở bản demo.
  - **Bảo mật**: chỉ có một biên vào là API Spring Boot, nên việc kiểm tra quyền theo lead và kiểm tra dữ liệu đầu vào tập trung ở một chỗ.
- **Alternatives Rejected**:
  - Tách phần AI thành dịch vụ riêng [Q1=B]: phải quản lý thêm tiến trình và giao tiếp mạng nội bộ mà không mang lại lợi ích cho demo.
  - Microservices theo từng module: quá nặng so với nhóm và thời gian hiện có.

## ADR-002: Giữ server Node phục vụ giao diện và MicroSIP

### Status: Accepted

- **Context**: Server Node hiện đang phục vụ thư mục `public/` và điều khiển `microsip.exe` qua `execFile`, đồng thời phát sự kiện qua SSE (`component-inventory.md`). CORS của Spring Boot đã được cấu hình cho `/api/**`.
- **Decision**: Node tiếp tục phục vụ các trang HTML (S0–S6) và đảm nhận MicrosipBridge. Trình duyệt gọi thẳng API của Spring Boot [Q2=A].
- **Consequences**:
  - (+) Không phải di chuyển mã đang chạy.
  - (−) Có hai cổng khác nguồn gốc (origin) nên cần giữ cấu hình CORS đúng.
  - (−) Phiên đăng nhập (US4.1) phải hoạt động được qua CORS. Cách xử lý (token hay cookie kèm credentials) sẽ chốt ở bước NFR/Functional Design.
  - **Bảo mật**: CORS chỉ cho phép origin của server Node. MicroSIP chỉ được gọi với số điện thoại đã qua allow-list, giữ nguyên cách làm hiện tại.
- **Alternatives Rejected**:
  - Spring Boot phục vụ luôn HTML [Q2=B]: gọn hơn vì chỉ còn một origin, nhưng phải chuyển mã và vẫn cần Node cho MicroSIP.

## ADR-003: Copilot tìm trong file text/md đã trích từ PDF bằng tìm kiếm từ khóa, rồi nhúng kết quả vào prompt

### Status: Accepted

- **Context**: Copilot chỉ được trả lời từ tài liệu PDF và phải kèm nguồn cùng ngày hiệu lực (FR3.1–FR3.3, Q7a). Anthropic không có API embedding. Bộ tài liệu do người dùng cung cấp và có kích thước nhỏ.
- **Decision** [Q3]:
  - Trích trước mỗi PDF ra một file text/md, giữ đánh dấu trang, kèm tên tài liệu và ngày hiệu lực.
  - Khi có câu hỏi, chia văn bản thành đoạn theo trang hoặc mục, rồi tìm đoạn khớp bằng từ khóa đơn giản: so cả dạng có dấu và không dấu, chấm điểm theo số từ trùng.
  - Các đoạn tốt nhất được nhúng vào prompt gửi Claude, đặt trong phần phân tách rõ là dữ liệu.
  - Nếu không có đoạn nào khớp, hệ thống trả "không tìm thấy" và không gọi LLM.
- **Consequences**:
  - (+) Đơn giản, không cần CSDL vector hay nhà cung cấp thứ hai, và dễ kiểm thử.
  - (+) Trích dẫn được trang cụ thể nhờ giữ đánh dấu trang.
  - (−) Tìm theo từ khóa có thể bỏ sót các câu hỏi diễn đạt khác hẳn tài liệu. Có thể giảm bớt nhờ tìm cả dạng không dấu và để Claude tự nói "không tìm thấy".
  - (−) PDF dạng ảnh không trích được chữ, nên được ghi log và bỏ qua (AC1.2.3).
  - **Bảo mật**: nội dung tài liệu có thể chứa câu mệnh lệnh. Prompt luôn coi nội dung đó là dữ liệu (AC1.2.5).
- **Alternatives Rejected**:
  - Nhét toàn bộ tài liệu vào prompt: chi phí tăng theo kích thước tài liệu và dễ vượt giới hạn ngữ cảnh khi bổ sung tài liệu.
  - Tìm kiếm toàn văn trong CSDL: phức tạp hơn mức cần cho vài tài liệu.
  - Tìm theo vector (Voyage + pgvector): phải thêm nhà cung cấp và hạ tầng.

## ADR-004: Một cổng LLM dùng chung (LlmClient) với adapter Anthropic

### Status: Accepted

- **Context**: Cả CallAnalysis lẫn Copilot đều gọi Claude [Q4]. Khóa API sẽ được cung cấp sau qua biến môi trường. Test phải giả lập được LLM để đạt độ phủ 80% (NFR4).
- **Decision**: CallAnalysis và Copilot chỉ phụ thuộc vào một giao diện `LlmClient`. Adapter Anthropic đảm nhận khóa API, model, timeout, prompt caching và việc phân loại lỗi. Khi test thì dùng bản giả lập.
- **Consequences**:
  - (+) Đổi model hoặc nhà cung cấp chỉ phải sửa một chỗ, và test chạy không cần mạng.
  - (−) Thêm một lớp trừu tượng nhỏ.
  - **Bảo mật**: khóa API chỉ tồn tại trong adapter, được đọc từ biến môi trường và không bao giờ ghi log (NFR2).
- **Alternatives Rejected**:
  - Mỗi thành phần tự gọi SDK Anthropic: lặp lại phần xử lý khóa, timeout và lỗi, đồng thời khó giả lập khi test.

## ADR-005: Lead sở hữu Activity; Keyin ghi hoạt động kèm bản sao tóm tắt AI

### Status: Accepted

- **Context**: Lịch sử hoạt động của lead phải hiển thị tóm tắt và gợi ý AI của từng cuộc gọi. Trường hợp AI lỗi vẫn phải hiện keyin (US3.1, AC3.1.3).
- **Decision**: `Activity` thuộc LeadComponent. Khi lưu keyin, KeyinComponent ghi một hoạt động cuộc gọi chứa bản sao `aiSummary`/`aiSuggestions` tại thời điểm lưu.
- **Consequences**:
  - (+) Lead không cần phụ thuộc vào Call hay CallAnalysis, nên đồ thị phụ thuộc không có chu trình.
  - (+) Lịch sử giữ nguyên những gì RM đã xác nhận, kể cả khi sau đó RM chạy lại phân tích AI.
  - (−) Dữ liệu bị nhân đôi có chủ đích, giữa bản sao trong Activity và CallAnalysis. Bản trong Activity là "bản đã lưu cùng keyin".
- **Alternatives Rejected**:
  - Lịch sử đọc trực tiếp từ CallAnalysis: tạo phụ thuộc Lead → Call/Analysis và có nguy cơ tạo chu trình (Call → Lead).

## ADR-006: Call điều phối pipeline; CallAnalysis tự tính điểm tổng

### Status: Accepted

- **Context**: Pipeline hiện có (ghi âm → STT) nằm trong `CallPipelineService`. Cần thêm bước AI, các trạng thái dự phòng theo từng loại lỗi (FR4.8), và khả năng chạy lại riêng bước AI. Công thức điểm đã được chốt ở bước yêu cầu (FR4.4).
- **Decision**:
  - CallComponent tiếp tục điều phối pipeline và lưu `pipelineStatus`/`pipelineStep`.
  - Bước phân tích AI được gọi từ Call.
  - CallAnalysis kiểm tra JSON trả về, chạy lại tối đa một lần nếu sai, và **tự tính** điểm tổng (= round(tổng × 2)) cùng hạng. Hệ thống không dùng con số tổng do LLM trả về.
- **Consequences**:
  - (+) Điểm luôn nhất quán và kiểm thử được bằng các ví dụ cố định (AC2.2.1, AC2.2.4).
  - (+) Trạng thái pipeline chỉ nằm ở một chỗ.
  - (−) Call phụ thuộc trực tiếp vào CallAnalysis, tức là gọi đồng bộ bên trong một luồng bất đồng bộ. Cách này chấp nhận được với giới hạn một cuộc gọi tại một thời điểm.
- **Alternatives Rejected**:
  - Phát sự kiện "transcript sẵn sàng" để CallAnalysis tự lắng nghe: tách lỏng hơn, nhưng phải thêm cơ chế sự kiện và khó theo dõi trạng thái từng bước cho giao diện.

## ADR-007: Chế độ demo trước khi có đăng nhập (US4 làm sau cùng)

### Status: Accepted

- **Context**: Theo câu Q1 ở bước User Stories, các story nền tảng được làm sau cùng. Trong lúc chưa có US4, ứng dụng mở thẳng S1 của lead mẫu (`stories.md`).
- **Decision**: AuthComponent có hai chế độ cấu hình:
  - `demo`: luôn trả RM mẫu;
  - `login`: dùng phiên đăng nhập thật.

  Các thành phần khác chỉ hỏi Auth "RM hiện tại là ai", nên khi chuyển chế độ không phải sửa thành phần nào khác.
- **Consequences**:
  - (+) US1–US3 demo được ngay. Việc kiểm tra quyền theo lead vẫn chạy với RM mẫu.
  - (−) Chế độ `demo` **bỏ qua xác thực**. Chế độ này chỉ được bật khi chạy cục bộ và phải được ghi rõ trong tài liệu chạy demo.
- **Alternatives Rejected**:
  - Gắn cứng RM mẫu trong từng thành phần: sau này phải sửa ở nhiều chỗ.
  - Làm đăng nhập trước: trái với lựa chọn ưu tiên của người dùng.

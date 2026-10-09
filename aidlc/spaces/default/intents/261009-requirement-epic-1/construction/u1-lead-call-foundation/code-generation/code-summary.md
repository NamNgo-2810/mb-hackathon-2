# Tóm tắt sinh mã — U1 `u1-lead-call-foundation`

Nền tảng Lead & cuộc gọi cho CRM phân tích cuộc gọi: thực thể + REST (C1) cho
RM/Customer/Lead/Activity/CallRecord, kiểm quyền sở hữu phía server (FR1.4), tạo
cuộc gọi từ lead và kết thúc idempotent + điều phối pipeline (I2), giao diện S1
(chi tiết lead) và khung S2 (thực hiện cuộc gọi) theo mockup. Mã ứng dụng được
hiện thực đầy đủ; **test được hoãn** theo yêu cầu người dùng.

## Tệp tạo mới / sửa đổi

### Backend (Java, `com.mb.callassistant`)

Tạo mới:
- `domain/PipelineStatus.java`, `domain/PipelineStep.java` — enum trạng thái/bước pipeline (SM2).
- `lead/RmUser.java`, `lead/Customer.java`, `lead/Lead.java`, `lead/Activity.java` — thực thể do U1 sở hữu.
- `lead/LeadPriority.java`, `lead/LeadStage.java` (kèm tập chuyển hợp lệ SM1), `lead/ActivityType.java` — enum.
- `lead/RmUserRepository.java`, `lead/CustomerRepository.java`, `lead/LeadRepository.java`, `lead/ActivityRepository.java` — repository.
- `lead/CurrentRmProvider.java` (I1), `lead/DemoCurrentRmProvider.java` (RM mẫu demo), `lead/LeadService.java` (I1: getOwnedLead/assertOwned/updateStage/recordCallActivity/findNextLead).
- `service/CallPipelineStep.java` — điểm mở rộng pipeline (I2; U4 đăng ký ANALYZE sau).
- `web/ForbiddenException.java`, `web/NotFoundException.java`, `web/ValidationException.java`, `web/ConflictException.java` — lỗi 403/404/400/409.
- `web/LeadController.java` — C1 cho /api/me, /api/leads/demo, /{leadId}, /activities, /next.
- `web/dto/Rm.java`, `web/dto/LeadSummary.java`, `web/dto/LeadDetail.java`, `web/dto/CustomerDto.java`, `web/dto/ActivityDto.java`, `web/dto/CallDto.java`, `web/dto/CreateCallRequest.java` — DTO C1.

Sửa đổi tại chỗ (brownfield, không có bản `*_modified`):
- `domain/CallRecord.java` — thêm `leadId`, `rmId`, `endedAt`, `pipelineStatus`, `pipelineStep`, constructor theo lead và `markEnded` idempotent; giữ nguyên trường/endpoint cũ.
- `repository/CallRecordRepository.java` — thêm truy vấn cuộc gọi chưa xử lý xong (BR3.2).
- `service/CallService.java` — thêm `createFromLead`, `endCall` (idempotent, kiểm quyền), `getOwnedCall`, `updatePipeline` (I2) + máy trạng thái SM2; giữ `initiate`/`markEnded`/`find` cũ.
- `web/CallController.java` — thêm `POST /api/calls`; chuyển `/{id}/ended` và `/{id}` sang trả `CallDto` qua đường ownership (vẫn có `sttStatus` để tương thích); giữ `/initiate`, `/transcript`, `/recording`.
- `web/ApiExceptionHandler.java` — ánh xạ 403/404/400/409 theo `ErrorEnvelope`.
- `config/ApplicationConfig.java` — CORS mở thêm `PUT/PATCH/DELETE` + header `Authorization`.
- `config/AppProperties.java` — thêm khối `app.auth.mode`.
- `config/DemoDataSeeder.java` — seed RM mẫu + Customer + Lead + Activity (profile `demo`).
- `pom.xml` — thêm `spring-boot-starter-test` (scope test) + JaCoCo 0.8.12 (sàn 80% line cho package U1, gắn `verify`).
- `src/main/resources/application.properties`, `application-demo.properties` — `app.auth.mode`.
- `README.md` — tài liệu U1 (endpoint, chế độ demo, chạy test cục bộ).

### Frontend (`public/`) và Node (`server/`)

- `public/screens/1-chi-tiet-tiem-nang-ban.html` — S1 chi tiết lead (theo mockup), nút **Gọi KH**, `data-testid`, escape HTML.
- `public/screens/2-thuc-hien-cuoc-goi.html` — S2 thực hiện cuộc gọi (theo mockup), đồng hồ, tab, khung Copilot, nút **Kết thúc**, nghe SSE MicroSIP.
- `public/js/api.js` — thêm hàm C1 (getMe/getDemoLead/getLead/getLeadActivities/getNextLead/createCall/endCallById).
- `public/js/tailwind-config.js` — token Tailwind MB (S1).
- `public/js/screens.js` — helper escape + nhãn giai đoạn/ưu tiên/loại hoạt động + định dạng thời gian.
- `server/index.js` — route phục vụ S1/S2 (ADR-002); giữ nguyên MicroSIP/SSE.

## Quyết định & độ lệch (deviation)

1. **Gói mã cuộc gọi.** Kế hoạch nêu package `com.mb.callassistant.call` với `CallService`/`CallPipelineStep`. Mã hiện có đã có `CallService`/`CallPipelineService` trong `com.mb.callassistant.service` và `CallController` trong `web`. Để tránh **hai lớp `CallService` trùng tên** và tuân nguyên tắc brownfield (sửa tại chỗ), tôi **giữ `service`/`web`** và đặt `CallPipelineStep` vào `com.mb.callassistant.service`; mã lead mới đặt ở `com.mb.callassistant.lead`. Không có package `call`.
2. **Khoá chính CallRecord.** Giữ `Long id` tự tăng (đang dùng bởi endpoint cũ) thay vì chuyển sang String; DTO phơi `callId` dạng chuỗi (`String.valueOf(id)`) đúng contract C1.
3. **`LeadDetail.content`.** Contract mô tả `content` là object; để giữ compile-correct và đơn giản, lưu/phơi `content` dạng chuỗi (văn bản khối Nội dung). Giao diện tự diễn giải.
4. **SSE MicroSIP.** Server phát event SSE dạng `data: {type:'end'}` (không đặt tên event), nên S2 nghe `onmessage` và lọc `type==='end'` thay vì `addEventListener('end')`.
5. **Chế độ login.** `DemoCurrentRmProvider` chỉ hoạt động khi `app.auth.mode=demo` (mặc định). Nếu đặt `login` mà chưa có U6, bean `CurrentRmProvider` sẽ thiếu — giữ `demo` cho tới khi U6 xong.

## Trạng thái test

**Hoãn theo yêu cầu, chưa có test.** Theo yêu cầu người dùng ("gen code for all
units at once, let the test for later"), lượt này không viết file test. Hạ tầng
test đã sẵn trong `pom.xml` (spring-boot-starter-test + JaCoCo sàn 80% line, gắn
`verify`) để lượt test sau chạy ngay. Sàn 80% **không bị hạ**. Không chạy
Maven/mvnw ở lượt này (Maven không trên PATH; lập trình viên build/run trong
IntelliJ).

## Rủi ro biên dịch chưa kiểm chứng

- Không chạy được `mvn compile`/`verify` ở môi trường này; đã rà kỹ import, tên
  package, Lombok (dự án **không** dùng Lombok — getter/setter viết tay như mã
  hiện có), annotation JPA/Spring. Bảng `lead` được đặt tên trong dấu nháy
  (`"lead"`) để tránh từ khoá SQL.

# Tóm tắt sinh mã — U5 `u5-task-calendar`

Unit Công việc & Lịch làm việc (kind `service`): thực thể `Task` (JPA), nghiệp
vụ `TaskService` gồm I4 `createFromCall` (idempotent theo `(sourceCallId,
clientKey)`) và CRUD lịch C4, màn lịch tuần S5 + hộp thoại tạo/đổi giờ S6 trên
Node. Quyền sở hữu dựa lại U1 (`CurrentRmProvider`, `LeadService.assertOwned`).

## Tệp đã tạo / sửa

Backend (Java, `com.mb.callassistant`):

- `task/TaskType.java`, `task/TaskPriority.java`, `task/TaskStatus.java`,
  `task/TaskOrigin.java` — enum theo khuôn enum U1 (BR6.2).
- `task/Task.java` — `@Entity` (`@Table(name="task")`); `markDone(now)` idempotent,
  `reschedule(dueAt)` giữ `status`; bất biến `completedAt ⇔ DONE`, `KHAC` chỉ khi
  không gắn lead (BR5.1, BR3.2, BR6.1).
- `task/TaskDraft.java` (record, đầu vào I4), `task/TaskCreate.java` (record, tạo
  thủ công).
- `task/TaskRepository.java` — truy vấn dẫn xuất khoá theo `ownerRmId` (ownership,
  khoảng, lọc loại, sourceCallId, khoá idempotent).
- `task/TaskService.java` — `createFromCall` (I4), `createManual`, `listTasks`,
  `markDone`, `reschedule`, `patch`, `getOwnedTask`; cap 42 ngày, `confirmPast`,
  múi giờ `Asia/Ho_Chi_Minh`.
- `web/TaskController.java` — `GET/POST /api/tasks`, `PATCH /api/tasks/{taskId}`;
  điền denormalised `leadCode`/`customerName` qua `LeadService`.
- `web/dto/TaskDto.java`, `web/dto/TaskCreateRequest.java`,
  `web/dto/TaskPatchRequest.java` — DTO theo khuôn `web/dto/CallDto.java`.
- `config/DemoDataSeeder.java` *(sửa)* — inject `TaskRepository`, seed 4 Task cho
  tuần hiện tại gồm một việc `KHAC` không gắn lead (SEED) và một việc DONE.
- `backend/pom.xml` *(sửa)* — thêm `com.mb.callassistant.task.*` vào rule BUNDLE
  JaCoCo, giữ sàn `0.80`.
- `backend/README.md` *(sửa)* — mục U5 (endpoint C4, màn `/calendar`).

Frontend (Node, phục vụ tĩnh):

- `public/screens/5-lich-lam-viec.html` — màn S5 theo mockup gốc
  `5-lich-lam-viec.html`; `data-testid` trên phần tử tương tác, nhãn truy cập
  được, escape mọi text, ẩn nút Outlook/Google và Zalo ZNS (A4).
- `public/js/calendar.js` — logic lịch (tuần/ngày/tháng/danh sách), lọc loại,
  dòng "Ngoài giờ", tách done/upcoming, `confirmPast`, highlight 'just-created'
  qua `?sourceCallId=`.
- `public/js/api.js` *(sửa)* — thêm `listTasks`, `createTask`, `patchTask` (C4).
- `server/index.js` *(sửa)* — route `/screens/5-lich-lam-viec.html` và lối tắt
  `/calendar`.

## Quyết định thiết kế

- **`createFromCall` validate toàn bộ draft trước khi tạo** (BR2.2): thiếu
  `clientKey` ở bất kỳ draft nào từ chối cả lời gọi, không tạo Task nào — bảo toàn
  idempotent xác định theo `(callId, clientKey)` (BR2.1).
- **`aiSummary` truyền qua tham số `callAiSummary`** của `createFromCall` và chép
  vào mỗi Task mới (BR2.3) — thẻ panel trái S5 hiển thị độc lập, không đọc chéo
  `CallAnalysis` sau đó.
- **Phân biệt 403/404** đặt trong `TaskService.getOwnedTask`: tra theo
  `(taskId, ownerRmId)` trước; không có thì tra toàn cục để quyết định 404 (của
  chính RM) hay 403 (của RM khác), không tiết lộ tồn tại (BR1.1/BR1.2) — cùng kiểu
  với `LeadService.getOwnedLead`.
- **`PATCH` một giao dịch** (`TaskService.patch`): kiểm quyền một lần, đổi giờ
  trước (áp `confirmPast`), rồi đánh dấu xong, nên một request có thể làm cả hai.
- **Khoá idempotent và truy vấn khoá theo `ownerRmId`** ở repository để ownership
  được thực thi ngay ở lớp dữ liệu.
- **Lịch tuần dùng khoảng T2–CN** (không chỉ T2–T7) để không bỏ sót việc Chủ nhật
  khi gom "Ngoài giờ"; màn chỉ hiển thị cột T2–T7 theo mockup (BR4.2).
- **Highlight 'just-created'** dựa trên query param `sourceCallId` so với
  `task.sourceCallId` (BR4.6), tô viền xanh + huy hiệu "Vừa lưu từ cuộc gọi".
- **Điều hướng "Gọi ngay"/"Chi tiết TNB"** chỉ là chuyển sang màn U1 (`/lead`),
  U5 không tạo cuộc gọi/sửa lead (BR5.3).

## Trạng thái kiểm thử

**Hoãn theo yêu cầu người dùng** ("gen code cho tất cả các unit một lượt, phần
test để sau"). Lượt này **không tạo file test** và **không chạy** `mvn verify` /
`npm test`. Các bước test-after trong kế hoạch mang nhãn `(DỜI — test)` giữ
nguyên cho đợt sau. Sàn độ phủ **80% dòng** cho package
`com.mb.callassistant.task` **không bị hạ**; đã thêm include JaCoCo tương ứng,
chỉ được kiểm chứng ở đợt test sau. Phương pháp **test-after** theo `## Testing
Contract` của kế hoạch vẫn là nguồn chân lý.

## Sai lệch so với kế hoạch

- Repository đặt tên truy vấn kèm `OrderByDueAtAsc` (so với mô tả trần trong kế
  hoạch) để lịch/panel trả theo thứ tự giờ ổn định — thuần bổ sung, không đổi
  phạm vi.
- `createFromCall` nhận thêm tham số `callAiSummary` (ngoài `TaskDraft[]`) để chép
  tóm tắt AI (BR2.3); đúng tinh thần kế hoạch ("`aiSummary=` tham số truyền sang").
- Không có sai lệch nào khác; mọi file dùng chung được sửa tại chỗ, không tạo
  bản trùng.

## Cấu hình

- Không thêm property mới. Dùng `@Profile("demo")`, `CurrentRmProvider` và
  `LeadService` sẵn có của U1.
- `backend/pom.xml`: rule BUNDLE JaCoCo thêm `com.mb.callassistant.task.*`, sàn
  LINE `0.80` (không hạ).
- Múi giờ nghiệp vụ: `Asia/Ho_Chi_Minh` / offset `+07:00` (BR6.1); khung giờ lịch
  08:00–17:00, khoảng truy vấn tối đa 42 ngày.
- Không hardcode secret; mọi dữ liệu động ở giao diện được escape; đầu vào kiểm
  tra ở biên API (bean validation + kiểm nghiệp vụ trong service).

## Rủi ro biên dịch

- `createFromCall` có chữ ký 5 tham số (thêm `callAiSummary`); U4 khi gọi I4 phải
  truyền đúng thứ tự `(rmId, leadId, callId, callAiSummary, drafts)`.
- `PATCH` body `status` ánh xạ enum `TaskStatus`; giá trị ngoài enum do Jackson
  trả 400 ở biên — chấp nhận theo hợp đồng.
- Chưa chạy build (theo yêu cầu); đã rà kỹ import, chữ ký `LeadService`
  (`getOwnedLead`, `getCustomer`, `assertOwned`) và kiểu `Optional<Customer>` khớp
  mã hiện có.

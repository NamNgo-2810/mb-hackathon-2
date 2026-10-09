# Hướng dẫn chạy test — U5 `u5-task-calendar`

> Phạm vi: **chỉ** unit `u5-task-calendar`, package Java `com.mb.callassistant.task`
> (và các DTO U5 trong `com.mb.callassistant.web.dto`). Theo `## Testing Contract`
> của `code-generation-plan.md`: phương pháp **test-after**, công cụ **JUnit5 +
> Mockito (+ MockMvc)**, đo độ phủ bằng **JaCoCo qua `mvn verify`**.
>
> **Lưu ý về đợt sinh mã này:** theo yêu cầu người dùng ("gen code cho tất cả
> các unit một lượt, phần test để sau"), **chưa tạo file test và chưa chạy các
> lệnh dưới đây** trong đợt này. Tài liệu ghi lại **đúng lệnh phạm vi unit** để
> đợt test sau thực thi. Sàn độ phủ 80% dòng cho package U5 **không bị hạ**.

## Lệnh chạy test phạm vi unit (Maven)

Chạy ở gốc kho (không `cd`), nhắm **chỉ** các test của U5 theo mẫu lớp
`*Test` dưới package `com.mb.callassistant.task`:

```
mvn -f backend/pom.xml -Dtest="com.mb.callassistant.task.**.*Test" verify
```

- `-f backend/pom.xml`: trỏ tới module backend Spring Boot.
- `-Dtest="com.mb.callassistant.task.**.*Test"`: chỉ chạy test của U5; **không**
  kéo theo test của các unit khác (U1–U4). Giữ nguyên mẫu package này để phạm vi
  đúng bằng package mà kế hoạch khai báo.
- `verify`: chạy qua pha `test` + gắn JaCoCo (`prepare-agent` → `report` →
  `check`) để đo và kiểm sàn độ phủ.

Chạy nhanh **không đo độ phủ** (vòng lặp phát triển) khi chỉ muốn biết test
xanh/đỏ:

```
mvn -f backend/pom.xml -Dtest="com.mb.callassistant.task.**.*Test" test
```

Chạy **một lớp test** cụ thể khi khoanh vùng lỗi (ví dụ lớp service):

```
mvn -f backend/pom.xml -Dtest="com.mb.callassistant.task.TaskServiceTest" test
```

Xem báo cáo độ phủ sau khi `verify`: mở
`backend/target/site/jacoco/index.html` và lọc tới package
`com.mb.callassistant.task`.

### Chạy trong IntelliJ IDEA (Maven tool window)

- Mở **Maven tool window** (View → Tool Windows → Maven), chọn module backend.
- Để chạy đúng phạm vi U5: dùng **Run Anything** (nhấn `Ctrl` hai lần) rồi dán
  nguyên lệnh `mvn ... -Dtest="com.mb.callassistant.task.**.*Test" verify` ở trên;
  hoặc trong Maven tool window chạy lifecycle **`verify`** và truyền tham số
  `-Dtest=...` qua trường **Command line** của Run Configuration.
- Mục tiêu `verify` trong Maven tool window sẽ kích hoạt JaCoCo giống hệt dòng
  lệnh; không dùng nút ▶ "run test" mặc định của IDE cho lần đo độ phủ chính
  thức vì nó bỏ qua ràng buộc `check` của JaCoCo.
- Có thể bấm chuột phải vào package `com.mb.callassistant.task` trong cây dự án
  → **Run tests** để chạy nhanh trong IDE, nhưng lần kiểm sàn độ phủ cuối cùng
  trước khi commit vẫn phải là `mvn ... verify` ở trên (kỷ luật thủ công theo
  `team.md`).

## Mục tiêu độ phủ

- **Sàn ≥ 80% dòng (line coverage) cho package `com.mb.callassistant.task`**,
  thực thi qua rule BUNDLE JaCoCo trong `backend/pom.xml` (Step 8 của kế hoạch
  thêm `com.mb.callassistant.task.*` vào `includes`, sàn `0.80`). **Không hạ
  sàn** để làm bước xanh.
- Đo cục bộ **trước mỗi lần commit** (nhóm không có CI — theo `team.md`): chạy
  `mvn -f backend/pom.xml -Dtest="com.mb.callassistant.task.**.*Test" verify`,
  xác nhận `BUILD SUCCESS` và báo cáo JaCoCo đạt sàn, rồi mới commit.
- Giữ **bộ test hiện có xanh**: khi muốn chắc chắn không làm hỏng unit khác,
  chạy toàn bộ `mvn -f backend/pom.xml verify` một lượt trước khi commit lớn.

## Chiến lược mock

- **`TaskServiceTest`**: mock `TaskRepository`, `CurrentRmProvider`,
  `LeadService` bằng Mockito (`@Mock` + `@InjectMocks` hoặc dựng tay). Không chạm
  JPA/DB thật — chỉ kiểm logic nghiệp vụ (idempotent `createFromCall`,
  `clientKey` bắt buộc, ownership 403/404, cap 42 ngày + `from>to`, lọc `KHAC`,
  `confirmPast`/`PAST_NOT_CONFIRMED`, TODO→DONE idempotent).
- **`TaskControllerTest`**: `@WebMvcTest(TaskController.class)` + `MockMvc`, mock
  `TaskService` (và `CurrentRmProvider` nếu controller inject). Kiểm mã trạng
  thái/định dạng request-response (200/201/400/403/404) và ánh xạ lỗi qua
  `ApiExceptionHandler` sẵn có; không nạp toàn bộ context.
- **`TaskRepositoryTest`**: `@DataJpaTest` với H2 in-memory (không mock
  repository) để kiểm các truy vấn dẫn xuất (`findByOwnerRmIdAndDueAtBetween`,
  `findByOwnerRmIdAndSourceCallIdAndClientKey`, …).
- **`TaskTest`** (thực thể): kiểm thuần tuý `markDone()`/`reschedule()` và bất
  biến `completedAt ⇔ DONE`, không cần Spring.
- Múi giờ: cố định `Asia/Ho_Chi_Minh` / offset `+07:00` trong dữ liệu test; với
  các kiểm tra phụ thuộc "hiện tại" (quá khứ/tương lai của `confirmPast`, cap 42
  ngày) hãy tiêm `Clock`/thời điểm mốc hoặc dùng biên tương đối thay vì giờ hệ
  thống để test ổn định.

## Dữ liệu test

- Dùng builder/factory nội bộ cho `Task`/`TaskDraft` thay vì literal rải rác;
  đặt tên biến theo ý nghĩa (`ownedTask`, `otherRmTask`, `pastDueDraft`,
  `khacSeedTask`).
- Các ca tối thiểu cần phủ (ánh xạ BR/AC trong kế hoạch):
  - `createFromCall` gọi hai lần cùng `(sourceCallId, clientKey)` → một `Task`,
    trả lại `taskId` cũ (BR2.1, AC2.3.6).
  - Thiếu `clientKey` ở bất kỳ draft → từ chối cả lời gọi, không tạo gì
    (BR2.2).
  - `leadId` của RM khác → 403; `taskId` của chính RM không tồn tại → 404
    (BR1.1/BR1.2/BR1.3).
  - Khoảng > 42 ngày hoặc `from > to` → 400 `VALIDATION_FAILED` (BR4.1).
  - Lọc `type=KHAC` trả đúng tập `KHAC`; lọc loại cụ thể khác loại bỏ `KHAC`
    (BR4.3).
  - Đổi giờ sang quá khứ chưa `confirmPast` → 400 `PAST_NOT_CONFIRMED` (BR5.2).
  - `markDone` TODO→DONE đặt `completedAt`, gọi lại idempotent (BR5.1).
- Mỗi test độc lập (không chia sẻ state có thể thay đổi giữa các test); với
  `@DataJpaTest`, mỗi test tự dựng dữ liệu và rollback mặc định.

## Phần giao diện (Node) — ghi chú

- Hành vi JS thuần của màn lịch (`public/js/calendar.js`: gom ô giờ / "Ngoài
  giờ", lọc loại, tách done/upcoming, cờ `confirmPast`) thuộc lớp frontend, kiểm
  bằng `npm test` phạm vi file `calendar` ở đợt test sau — **không** nằm trong
  lệnh Maven phạm vi U5 ở trên. Giữ hai phạm vi tách biệt.

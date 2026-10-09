# Unit Test Instructions — U1 `u1-lead-call-foundation`

Chiến lược: **test-after**, Standard (5–8 test/lớp, test đơn vị + tích hợp cho
các biên quan trọng). Sàn **≥ 80% line coverage** cho các package của U1, đo
**thủ công trên máy lập trình viên trước mỗi commit** (team.md — không CI).

## 1. Thiết lập & cấu hình framework

### Backend (Spring Boot 3.3.4, Java 17)

- **spring-boot-starter-test** (JUnit 5 Jupiter, Mockito, AssertJ, Spring Test,
  MockMvc): thêm với `<scope>test</scope>`, **không ghi version** — phiên bản do
  Spring Boot parent quản lý.
- **JaCoCo** (ghim phiên bản, ví dụ `org.jacoco:jacoco-maven-plugin:0.8.12`):
  cấu hình `prepare-agent` + `report` gắn vào pha `verify`. Thêm luật kiểm sàn
  (`check` goal) **0.80 line coverage** cho các package U1
  (`com.mb.callassistant.lead.*`, `com.mb.callassistant.call.*`, và `CallRecord`
  trong `com.mb.callassistant.domain`). Ngưỡng này **không được hạ** để làm một
  bước qua (NFR4) — nếu thiếu phủ thì bổ sung test.
- **H2** (đã có trong `pom.xml`, `scope=runtime`): dùng cho `@DataJpaTest` và
  `@SpringBootTest` ở profile `demo` (in-memory, `MODE=PostgreSQL`).
- Các loại test dùng ở U1:
  - `@DataJpaTest` + H2 cho repository (Step 5–6).
  - Test POJO/Mockito thuần cho domain và service (Step 3–4, 7–10).
  - `@WebMvcTest` + MockMvc cho controller, mock service layer (Step 11–12).
  - `@SpringBootTest(webEnvironment=RANDOM_PORT)` profile `demo` cho seed +
    end-to-end REST tối thiểu (Step 13–14).

### Node (`server/`)

- U1 **không** thêm test Node bắt buộc: phần Node ở U1 chỉ là route tĩnh phục vụ
  S1/S2 (`server/index.js`, ADR-002), không có logic nghiệp vụ mới. Logic
  MicroSIP/SSE đã tồn tại và nằm ngoài phạm vi U1. Vì vậy mục này chỉ ghi nhận:
  khi một unit sau thêm logic Node, chạy `npm test` **có đo coverage** trong thư
  mục của nó. Ở U1 không có lệnh `npm test` nào thuộc phạm vi.

## 2. Chạy test CỦA UNIT NÀY (chỉ lệnh unit-scoped)

> **Xác minh runner (brownfield):** Kho chưa có test tự động nào trước lượt này
> (`backend/README.md` ghi rõ). `spring-boot-starter-test` (scope test) và
> JaCoCo 0.8.12 đã được thêm vào `backend/pom.xml` ở Step 2; luật kiểm sàn 80%
> gắn vào pha `verify`. Vì test được **hoãn** theo yêu cầu người dừng ("gen code
> for all units at once, let the test for later"), các file test chưa tồn tại —
> lệnh dưới đây là lệnh unit-scoped sẽ dùng ngay khi lớp test được viết ở lượt
> sau. Maven **không trên PATH**; chạy qua IntelliJ → Maven tool window.

Maven **không nằm trên PATH**: lập trình viên chạy các lệnh dưới đây qua
**IntelliJ → Maven tool window** (hoặc Run Configuration kiểu Maven), không chạy
từ terminal nền tảng.

Lệnh verify đã scope cho đúng package của U1 (chạy test + đo coverage + kiểm sàn
JaCoCo):

```
mvn -f backend/pom.xml -Dtest="com.mb.callassistant.lead.**.*Test,com.mb.callassistant.call.**.*Test,com.mb.callassistant.domain.CallRecordTest,com.mb.callassistant.DemoSeedIntegrationTest" verify
```

Chạy nhanh chỉ test (không gắn kiểm sàn, dùng khi phát triển từng lớp):

```
mvn -f backend/pom.xml -Dtest="com.mb.callassistant.lead.**.*Test,com.mb.callassistant.call.**.*Test,com.mb.callassistant.domain.CallRecordTest" test
```

Danh sách lớp test U1 (dùng khi muốn liệt kê tường minh thay cho wildcard):

```
mvn -f backend/pom.xml -Dtest="CallRecordTest,CustomerTest,LeadTest,LeadRepositoryTest,ActivityRepositoryTest,LeadServiceTest,DemoCurrentRmProviderTest,CallServiceTest,CallPipelineStepTest,LeadControllerTest,CallControllerTest,DemoSeedIntegrationTest" verify
```

Không dùng lệnh toàn dự án trần (ví dụ `mvn verify` hay `mvn test` không có
`-Dtest`): Build and Test chạy lệnh của từng unit, nên lệnh không scope sẽ chạy
lại toàn bộ suite mỗi unit một lần.

## 3. Mục tiêu độ phủ

- **≥ 80% line coverage** cho `com.mb.callassistant.lead.*`,
  `com.mb.callassistant.call.*`, và `CallRecord`.
- Báo cáo JaCoCo HTML sau `verify`:
  `backend/target/site/jacoco/index.html` — mở để kiểm phần chưa phủ.
- Nếu thiếu phủ: thêm test, **không** nới ngưỡng trong `pom.xml`.

## 4. Hướng dẫn mock/stub (không gọi mạng)

- Mock **`RecordingClient`**, **`RecordingService`**, **`SttService`** (và
  `GoogleSttClient`) trong test `CallService`/`CallPipelineService`: không tải
  `.wav`, không gọi Google STT, không chạm EC2.
- `CallPipelineStep` của U4 (bước ANALYZE) chưa tồn tại ở U1: test điểm mở rộng
  bằng một stub `CallPipelineStep` trả `ok`/`failed(kind)` để kiểm máy trạng
  thái SM2 (`PROCESSING → DONE`, `→ NO_RECORDING/STT_FAILED/AI_FAILED`,
  `AI_FAILED → PROCESSING`).
- `CurrentRmProvider`: ở test, bơm `DemoCurrentRmProvider` trả RM mẫu cố định
  hoặc một stub trả RM khác để kiểm 403 (BR1.1).
- `@WebMvcTest` mock `LeadService`/`CallService` bằng `@MockBean` — không nạp JPA
  hay DB.
- Không có khóa API trong test; không đọc biến môi trường thật (NFR2).

## 5. Quản lý dữ liệu test

- Repository/integration test dùng **H2 in-memory** (`ddl-auto=create-drop`,
  profile `demo`); mỗi test tự seed dữ liệu cần thiết, không phụ thuộc thứ tự
  chạy.
- Seed demo thật (`DemoDataSeeder`) chỉ dùng trong `DemoSeedIntegrationTest`
  (profile `demo`); test đơn vị khác tự dựng đối tượng (builder/factory), không
  dùng seed toàn cục để tránh fixture dùng chung dễ vỡ.
- Tiền = integer VND; thời gian = `Instant`/offset `+07:00` trong dữ liệu test
  (BR6.2).
- Dữ liệu test là KH/lead giả, không dùng dữ liệu khách hàng thật (NFR7).

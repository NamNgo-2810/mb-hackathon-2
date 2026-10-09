# Quyết định Công nghệ — U1 `u1-lead-call-foundation`

Unit này là **brownfield**: xây tiếp trên nền mã có sẵn và **không chọn hạ tầng
mới**. Tài liệu ghi lại U1 **tái sử dụng** gì từ `technology-stack.md` (codekb)
và các bổ sung nhỏ cần thiết (chủ yếu thư viện test theo thực hành của nhóm).

## Công nghệ tái sử dụng (giữ nguyên stack hiện có)

| Hạng mục | Lựa chọn | Phiên bản | Lý do (tái sử dụng, không đổi) |
|---|---|---|---|
| Ngôn ngữ backend | Java | 17 | `java.version=17` trong `backend/pom.xml`; brownfield, giữ nguyên |
| Framework backend | Spring Boot | 3.3.4 (`spring-boot-starter-parent`) | BOM quản lý phiên bản sẵn có; mọi phần backend nằm chung một ứng dụng (ADR-001/002) |
| REST MVC | `spring-boot-starter-web` | (quản lý bởi 3.3.4) | Phục vụ C1 (`/api/leads/*`, `/api/calls/*`), chạy embedded Tomcat :8080 |
| Kiểm hợp lệ đầu vào | `spring-boot-starter-validation` | (quản lý) | Jakarta Bean Validation cho NFR2.10/BR6.3 (`400 VALIDATION_FAILED` kèm `details.field`) |
| Persistence | `spring-boot-starter-data-jpa` (JPA/Hibernate) | (quản lý) | Giữ cách lưu trữ hiện có; U1 mở rộng `CallRecord` thêm `leadId`/`rmId`/`pipelineStatus`/`pipelineStep` |
| Datastore (mặc định) | PostgreSQL 16 (`postgres:16-alpine` qua Compose) | driver quản lý | Profile mặc định hiện có |
| Datastore (demo) | H2 in-memory (chế độ PostgreSQL) | quản lý, runtime | Profile `demo` chạy cục bộ, dữ liệu seed (ADR-007) |
| HTTP upstream (nếu cần) | `spring-boot-starter-webflux` `WebClient` | (quản lý) | Đã có; không bổ sung client HTTP mới cho U1 |
| Băm mật khẩu | BCrypt | theo Spring Security crypto có sẵn/được dùng | `RmUser.passwordHash` BCrypt (BR6.1); hiện thực login đầy đủ ở U6 |
| Giao diện + MicroSIP | server Node (`express ^4.19.2`, `dotenv ^16.4.5`) | như `package.json` | Trang S1 và khung S2 của U1 chạy trên Node hiện có; MicroSIP qua C6 giữ nguyên `/api/microsip/*` |

## Bổ sung nhỏ (chỉ phục vụ test, theo `team.md`)

Nhóm chốt test-after với **sàn 80% line coverage đo thủ công trước mỗi commit**;
backend dùng **JaCoCo qua `mvn verify`**, phần Node dùng **`npm test` có đo
coverage** (`team.md` §Testing Posture, NFR4). Do `pom.xml`/`package.json` hiện
chưa khai báo phụ thuộc test, U1 bổ sung các thư viện test chuẩn (không phải hạ
tầng runtime mới):

| Hạng mục | Lựa chọn | Lý do |
|---|---|---|
| Test backend | JUnit 5 + Mockito (qua `spring-boot-starter-test`, quản lý bởi 3.3.4) | Chuẩn Spring Boot; mock STT/LLM theo NFR3/NFR4; đã ngụ ý bởi sàn coverage của `team.md` |
| Đo coverage backend | JaCoCo (`jacoco-maven-plugin`) qua `mvn verify` | Thực thi sàn 80% line coverage thủ công trước commit (`team.md`) |
| Test phần Node (nếu U1 chạm server Node) | runner `npm test` có đo coverage | Theo `team.md`; chỉ thêm khi U1 thực sự thêm logic Node (phần giao diện tĩnh S1/S2 chủ yếu dùng DOM/ES modules) |

## Ràng buộc và điều KHÔNG làm

- **Không** chọn hạ tầng mới (không message broker, cache, container mới ngoài
  Postgres Compose đã có).
- **Không** CI/CD, chỉ chạy cục bộ; kiểm test + coverage thủ công trước commit
  (`team.md`, NFR4, requirements §4).
- **Không** linter/formatter mới ở giai đoạn này (`team.md` §Code Style); theo
  quy ước mã hiện có (chuẩn Java/Spring, camelCase cho JS).
- Khóa API Anthropic/Google lấy từ **biến môi trường** (NFR2.8). Khuyến nghị:
  dọn khóa đã commit trong `backend/src/main/resources/application.properties`
  và `.env` sang biến môi trường — **khuyến nghị, không chặn demo** (NFR2.9, O5).
- LLM (Anthropic Claude) và cổng LLM là của U2/U4; U1 không gọi LLM trực tiếp.

## Sources

- `technology-stack.md` (codekb) — Spring Boot 3.3.4/Java 17, Postgres 16/H2, Node `express`/`dotenv`, embedded Tomcat :8080
- `aidlc/spaces/default/memory/team.md` — §Testing Posture (test-after, 80% coverage thủ công, JaCoCo `mvn verify`, `npm test`), §Code Style, §Deployment (chỉ cục bộ)
- `requirements.md` — NFR2 (biến môi trường), NFR3 (mock STT/LLM), NFR4 (test-after), §4 ràng buộc
- `rules.md` (U1) — BR6.1 (BCrypt), BR6.3 (kiểm hợp lệ)
- `entities.md` (U1) — `RmUser.passwordHash`, mở rộng `CallRecord`
- `contract-summary.md` — C1, C6, I1/I2; ADR-001/002 (một ứng dụng Spring Boot), ADR-007 (demo/H2)
- `unit-of-work.md` — U1 nhúng Spring Boot + S1/S2 trên Node

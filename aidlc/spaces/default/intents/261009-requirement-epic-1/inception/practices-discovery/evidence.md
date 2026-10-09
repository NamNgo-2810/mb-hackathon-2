# Bằng chứng & quyết định phỏng vấn

Dự án brownfield, không gian `default`, lần chạy đầu tiên: `team.md` còn rỗng.
Mặc định `org.md` được dùng làm điểm tham chiếu; nơi nhóm chọn khác đều được
ghi lại rõ.

## Những gì đã kiểm tra / suy luận

- **Artifacts reverse-engineering** (`aidlc/spaces/default/codekb/mb-hackathon-2/`):
  `code-structure.md`, `technology-stack.md`, `dependencies.md`,
  `code-quality-assessment.md`, `architecture.md`, `business-overview.md`.
- **Cấu hình dự án:** `package.json` (Node: `express`, `dotenv`; không dep
  test/lint), `docker-compose.yml` (Postgres 16 + Whisper tùy chọn, credential
  demo), `.gitignore`, `backend/pom.xml` (Spring Boot 3.3.4, Java 17; không có
  plugin test/lint).
- **Git:** `git status`/`branch`/`log` đều báo "not a git repository" — chưa
  có kho Git ở gốc dự án.
- **CI/CD:** không có workflow dự án; `.github/` hiện có là của AI-DLC (ngoài
  phạm vi). Không có Jenkinsfile/buildspec, không Dockerfile ứng dụng.
- **Secrets:** khóa Google STT thật, `X-API-Key` recording và host EC2 nằm
  trong `backend/src/main/resources/application.properties` và `.env`.
- **Thời điểm:** `aidlc engine now`.

## Quyết định phỏng vấn (authoritative)

- **Q1 — Way of Working:** `X. Other — Git local`. Dùng Git chỉ cục bộ, không
  remote, không pull request.
- **Q1a — Rẽ nhánh khi dùng Git cục bộ:** `B`. Commit thẳng lên `main`, không
  dùng nhánh. (Khác mặc định `org.md` trunk-based + squash; là lựa chọn chốt
  của nhóm cho hackathon.)
- **Q2 — Walking Skeleton:** `C`. Không chạy walking skeleton; làm thẳng từng
  tính năng.
- **Q3 — Testing Posture:** `B`. Test-after, giữ sàn 80% coverage (xem Q7 về
  cách kiểm).
- **Q4 — Deployment:** `C`. Chỉ chạy cục bộ, không CI.
- **Q5 — Code Style:** `C`. Hoãn linter/formatter; theo quy ước ngôn ngữ hiện
  có.
- **Q6 — Quy tắc cứng:** `C`. Giữ tất cả ứng viên ở mức **khuyến nghị**, không
  thành quy tắc bắt buộc.
- **Q7 — Giải mâu thuẫn Q3/Q4:** `A`. Giữ sàn 80% nhưng **kiểm thủ công trên
  máy** (`mvn verify` + JaCoCo cho backend, `npm test` + coverage cho Node)
  trước mỗi commit; không có CI.

## Mâu thuẫn đã giải quyết

- **Q3 (sàn 80% + CI) ↔ Q4 (không CI):** hai lựa chọn ban đầu không tương
  thích. Giải quyết bằng **Q7 = A**: giữ sàn 80% coverage, nhưng xác minh
  **thủ công trên máy lập trình viên trước khi commit**, bỏ phần CI. Testing
  Posture và Deployment trong `team-practices.md` đã phản ánh thống nhất này.

## Bất định & rủi ro còn lại

- **Secrets thật đã bị commit** (Google STT key, `X-API-Key`, host EC2) đang
  tồn tại trong working tree — rủi ro lộ bí mật; khuyến nghị xoay khóa.
- **Chưa khởi tạo kho Git** ở gốc dự án; cần `git init` trước commit đầu tiên.
- **Không có CI** nghĩa là việc chạy test và giữ sàn 80% coverage hoàn toàn
  phụ thuộc kỷ luật thủ công — dễ bị bỏ sót khi gấp.
- **Schema drift:** `ddl-auto=update`/`create-drop` không migration vẫn là rủi
  ro, nay chỉ còn là khuyến nghị chứ không phải quy tắc chặn.

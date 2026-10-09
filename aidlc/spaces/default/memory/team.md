# Team-Level Rules

> This team's affirmed practices and corrections. Loaded after `org.md` as
> strict-additive guidance; contradictions with broader policy are rejected.
> Populated by the practices-discovery affirmation gate. Edit at the gate,
> not directly.

## Way of Working

- Dùng Git **chỉ cục bộ trên máy**: không có kho từ xa (không GitHub/GitLab),
  không pull request, không review qua remote.
- **Commit thẳng lên `main`, không dùng nhánh.** Mỗi phần việc hoàn thành thì
  commit trực tiếp vào `main`.
- Lựa chọn này **khác với mặc định `org.md`** (trunk-based với nhánh ngắn ngày
  + squash-merge). Nhóm chủ động chọn cách làm tối giản này cho bối cảnh
  hackathon và đây là thực hành đã được nhóm chốt.
- Lưu ý vận hành: hiện **chưa có kho Git nào được khởi tạo** ở gốc dự án; cần
  `git init` trước khi commit đầu tiên.

## Walking Skeleton

- **Không chạy walking skeleton.** Nhóm làm thẳng từng tính năng một, không
  dựng trước một lát cắt đứng đầu-cuối mỏng.
- Lý do: bối cảnh hackathon ưu tiên tốc độ, và luồng nền tảng
  record→transcribe đã tồn tại; nhóm xây tiếp từng tính năng mới trực tiếp.

## Testing Posture

- **Methodology**: test-after
- **Methodology evidence**: Kho hiện không có test tự động nào và
  `backend/README.md` ghi rõ đây là quyết định hackathon; nhóm chốt viết test
  sau khi hiện thực, khớp hướng này.
- **Ordering**: Hiện thực từng lớp có thể kiểm thử trước, rồi viết và chạy test
  cho lớp đó ngay sau đó, và xác nhận độ phủ cục bộ trước khi commit.
- Giữ **sàn 80% line coverage**, nhưng được kiểm tra **thủ công trên máy của
  lập trình viên trước mỗi lần commit** — chạy test và đo độ phủ, rồi mới
  commit.
- Công cụ: backend Spring Boot dùng **JaCoCo qua `mvn verify`**; phần Node
  dùng **`npm test` kèm đo coverage**.
- **Không có CI.** Việc chạy test và kiểm độ phủ hoàn toàn phụ thuộc kỷ luật
  thủ công trước khi commit.

## Guard Policy

<!-- Affirmed by the team. Mode: strict, relaxed, or off. Strict here holds for every intent, whatever one person asks in chat; relaxed or off here applies to every intent whose policy came from its scope, unless project.md or the person sets another; changing this line changes it. A section under the retired Change Control heading, written by an earlier release, is still read. -->

## Deployment

- **Chỉ chạy cục bộ** (local). Không có pipeline triển khai lên staging/EC2,
  không CI/CD.
- FreeSWITCH đang chạy trên EC2 là **phụ thuộc bên ngoài**, không phải mục tiêu
  triển khai của phần việc này.

## Code Style

- **Chưa thêm linter/formatter** nào ở giai đoạn này; việc dựng công cụ được
  hoãn lại.
- Tuân theo **quy ước mã hiện có** của từng ngôn ngữ (đặt tên theo chuẩn ngôn
  ngữ: camelCase cho JS, quy ước Java chuẩn cho Spring).
## Forbidden

<!-- Team-specific forbidden patterns -->

## Mandated

<!-- Team-specific mandates -->

## Corrections

<!-- Self-learning loop appends here. -->

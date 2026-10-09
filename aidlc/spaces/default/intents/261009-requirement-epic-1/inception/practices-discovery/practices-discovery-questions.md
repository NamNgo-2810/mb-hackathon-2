# Practices Discovery — Câu hỏi phỏng vấn

Bản nháp đã suy ra những gì có thể từ mã nguồn. Các câu hỏi dưới đây là những điểm mà mã nguồn không trả lời được — cần quyết định của nhóm.

## Q1. Cách làm việc với Git (Way of Working)

Thư mục dự án hiện chưa có kho Git, nên không có bằng chứng nào về cách rẽ nhánh hay gộp mã. Nhóm muốn làm việc thế nào?

A. Khởi tạo Git, làm theo trunk-based: nhánh tính năng ngắn (1-2 ngày), squash-merge vào `main` (mặc định của tổ chức)
B. Khởi tạo Git, dùng GitHub Flow: nhánh tính năng + pull request có review trước khi gộp vào `main`
C. Không dùng Git trong hackathon này
X. Other (please specify)

[Answer]: X. Other — Git local

## Q1a. Câu hỏi bổ sung: cách rẽ nhánh khi dùng Git cục bộ

Bạn chọn "Git local", tức là dùng Git trên máy, không có kho từ xa như GitHub hay GitLab. Khi đó vẫn cần thống nhất cách tổ chức nhánh trên máy:

A. Nhánh tính năng ngắn trên máy, squash-merge vào `main` khi xong (trunk-based, không có pull request)
B. Commit thẳng lên `main`, không dùng nhánh
C. Nhánh tính năng, merge thường (giữ lịch sử commit) vào `main`
X. Other (please specify)

[Answer]: B

## Q2. Xây dựng một lát cắt chạy suốt từ đầu đến cuối trước? (Walking Skeleton)

Build a thin end-to-end slice first? A walking skeleton is a minimal version that runs the whole way through, built first to prove the pieces connect before the real features go in. (Một bản tối giản chạy được trọn luồng, làm trước để chứng minh các phần kết nối được với nhau trước khi làm tính năng thật.) Hiện luồng ghi âm → chuyển giọng nói thành văn bản đã có; phần LLM và giao diện mới là phần mới.

A. Có — lát cắt đầu tiên: gọi điện → bản ghi lời thoại → tóm tắt bằng LLM, hiển thị trên giao diện mới
B. Có — nhưng chỉ đến bước bản ghi lời thoại (chưa có LLM)
C. Không — bỏ qua bước này, làm thẳng từng tính năng
X. Other (please specify)

[Answer]: C

## Q3. Mức độ kiểm thử (Testing Posture)

Hiện dự án chưa có test tự động nào. Với phạm vi classic, mặc định của tổ chức là viết test sau khi viết mã, độ phủ dòng tối thiểu 80% và chạy trong CI trước khi gộp. Nhóm muốn thế nào?

A. Viết test sau khi viết mã, mỗi thành phần có ít nhất test cho luồng chính; tạm hoãn ngưỡng 80% và CI
B. Viết test sau khi viết mã, đạt độ phủ 80% và chạy CI ngay từ bây giờ (mặc định tổ chức)
C. Không viết test mới cho hackathon
D. Chuyển sang TDD hoặc BDD (viết test/kịch bản trước khi viết mã)
X. Other (please specify)

[Answer]: B

## Q4. Triển khai (Deployment)

Hiện chưa có CI/CD. Hệ thống sẽ chạy demo ở đâu, và có cần một CI tối thiểu không?

A. Demo chạy cục bộ bằng Docker Compose + CI tối thiểu (build và chạy test)
B. Môi trường staging thật trên EC2, tự động triển khai khi gộp mã; production cần duyệt thủ công (mặc định tổ chức)
C. Chỉ chạy cục bộ, không có CI
X. Other (please specify)

[Answer]: C

## Q5. Định dạng và kiểm tra mã (Code Style)

Dự án chưa có cấu hình linter hay formatter nào, nên không có gì để làm theo. Nhóm muốn dùng gì?

A. Bộ tối thiểu: Prettier + ESLint cho JavaScript, Spotless (google-java-format) cho Java
B. Chỉ dùng formatter (Prettier, Spotless), chưa cần linter
C. Để sau, chưa thêm công cụ nào
X. Other (please specify)

[Answer]: C

## Q6. Các quy tắc bắt buộc (Mandated / Forbidden)

Bản nháp đề xuất một số quy tắc cứng. Những quy tắc được chọn sẽ trở thành luật bắt buộc cho mọi bước sau. Các đề xuất: (1) bí mật và khóa API phải nằm ngoài mã nguồn (biến môi trường/secret manager), không bao giờ commit khóa thật; (2) thay đổi cơ sở dữ liệu phải qua migration (Flyway/Liquibase), không dùng `ddl-auto`; (3) luôn kiểm tra dữ liệu đầu vào ở ranh giới hệ thống, không ghép chuỗi lệnh shell từ dữ liệu người dùng.

A. Áp dụng cả ba nhóm quy tắc
B. Áp dụng quy tắc về bí mật ngay; hoãn quy tắc migration
C. Chỉ giữ tất cả ở mức khuyến nghị, không thành luật bắt buộc
X. Other (please specify)

[Answer]: C

## Q7. Câu hỏi bổ sung: mâu thuẫn giữa Q3 và Q4

Ở Q3 bạn chọn đạt độ phủ 80% và chạy CI trước khi gộp mã. Ở Q4 bạn chọn không có CI. Hai lựa chọn này không đi cùng nhau được. Nhóm muốn giữ phương án nào?

A. Giữ độ phủ 80%, nhưng kiểm tra bằng cách chạy test và đo độ phủ thủ công trên máy (`mvn verify` / `npm test`) trước khi commit, không có CI
B. Thêm một CI tối thiểu chạy trên máy (git hook pre-commit chạy build + test + độ phủ)
C. Hạ yêu cầu: viết test sau, chỉ cần test luồng chính cho mỗi thành phần, bỏ ngưỡng 80%
X. Other (please specify)

[Answer]: A

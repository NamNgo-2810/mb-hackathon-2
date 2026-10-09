# Requirements Analysis — Câu hỏi làm rõ

Đầu vào: tài liệu `epic 1.md` (BM.02 — "(CRM) AI phân tích cuộc gọi RM với Khách hàng", 3 US: US-01, US-02, US-03) và mã nguồn hiện có (softphone FreeSWITCH/MicroSIP + backend Spring Boot đã có luồng ghi âm → chuyển giọng nói thành văn bản).

Epic được viết cho hệ thống thật của MB (tổng đài 3CX + SDK FPT, kho ghi âm nội bộ MB, MB Policy Center, APP + WEB), còn mã nguồn hiện tại là bản demo hackathon. Các câu hỏi dưới đây chốt cách nối hai bên.

## Q1. Phạm vi làm trong đợt này

Epic có 3 US: US-01 (màn "Kết quả cuộc gọi" 3 cột: AI chấm điểm + transcript/audio + AI tự điền form keyin), US-02 (Copilot tra cứu chính sách trong lúc gọi), US-03 (xem lại tóm tắt/gợi ý AI trong Lịch sử hoạt động của lead). Đợt này làm những US nào?

A. Cả 3 US (US-01, US-02, US-03)
B. US-01 và US-03 (luồng sau cuộc gọi), để US-02 sau
C. Chỉ US-01
D. US-01 và US-02, để US-03 sau
X. Other (please specify)

[Answer]: Thử xem có cách nào chia US hợp lý hơn không? Suggest cho tôi

## Q2. Nền tảng giao diện

Epic yêu cầu APP + WEB. Mã nguồn hiện chỉ có giao diện web (`public/`). Đợt này làm trên nền tảng nào?

A. Chỉ WEB (giao diện trình duyệt, thiết kế theo Figma)
B. WEB + giao diện tối ưu cho di động (responsive), chưa làm app riêng
C. WEB + APP di động riêng
X. Other (please specify)

[Answer]:Chỉ làm web

## Q3. Tổng đài và nguồn ghi âm

Epic dùng 3CX + SDK FPT và lấy ghi âm qua API kho nội bộ MB (C-AUDIO chưa chốt). Bản demo dùng FreeSWITCH trên EC2 + MicroSIP và lấy ghi âm qua máy chủ ghi âm HTTP trên EC2, ghép cuộc gọi với file ghi âm bằng "file mới nhất" (dễ nhầm khi có nhiều cuộc gọi cùng lúc). Đợt này làm thế nào?

A. Dùng FreeSWITCH/EC2 làm bản thay thế cho tổng đài MB; giữ cách ghép "file mới nhất"
B. Dùng FreeSWITCH/EC2, nhưng ghép cuộc gọi với file ghi âm bằng mã cuộc gọi (call-id/UUID) cho đúng
C. Thiết kế một lớp tích hợp có thể thay thế (adapter): demo chạy với FreeSWITCH/EC2, sau này cắm API kho ghi âm MB vào mà không sửa phần còn lại
X. Other (please specify)

[Answer]: A

## Q4. Dịch vụ LLM

Epic chưa chốt dịch vụ LLM (C-STT). Hiện chuyển giọng nói thành văn bản đang dùng Google Cloud STT. LLM dùng để tóm tắt, chấm điểm, điền form keyin và trả lời Copilot. Chọn dịch vụ nào?

A. Google Gemini (cùng hệ sinh thái Google Cloud với STT hiện có)
B. OpenAI (GPT)
C. AWS Bedrock (Claude hoặc mô hình khác)
D. Tự host mô hình mã nguồn mở (ví dụ qua Ollama)
E. Thiết kế lớp LLM có thể thay nhà cung cấp; demo dùng một nhà cung cấp (ghi rõ ở Other)
X. Other (please specify)

[Answer]: Đã có API Key Anthropic, khi làm đến đó tôi sẽ cung cấp

## Q5. Dữ liệu lead / cơ hội bán và form keyin

Epic giới hạn điểm chạm ở "Chi tiết lead / Cơ hội bán" (C-POINT) và form keyin hiện tại (Giai đoạn / Kết quả / Công việc — pageId 536766154, không truy cập được). Mã nguồn hiện chưa có dữ liệu lead hay form keyin. Đợt này xử lý thế nào?

A. Tự dựng bản rút gọn trong CSDL của dự án: danh sách lead, chi tiết lead, form keyin; mình đề xuất các trường (Giai đoạn, Kết quả, Công việc tiếp theo, ghi chú…) dựa trên epic, bạn sửa sau
B. Tự dựng như A, nhưng bạn sẽ cung cấp danh sách trường form keyin chính xác
C. Chỉ dữ liệu mẫu (seed) cố định, không có màn quản lý lead
X. Other (please specify)

[Answer]: A

## Q6. Tiêu chí AI chấm điểm cuộc gọi (US-01)

Epic nói "điểm 0-100, 5 tiêu chí kỹ năng, cảm xúc KH, insight" nhưng chưa định nghĩa 5 tiêu chí (ghi chú mục 4.4: cần chốt khung chuẩn mực kỹ năng RM). Chọn cách nào?

A. Dùng bộ đề xuất: (1) Mở đầu & chào hỏi, (2) Khai thác nhu cầu, (3) Tư vấn sản phẩm, (4) Xử lý từ chối, (5) Kết thúc & chốt bước tiếp theo — mỗi tiêu chí thang 0-100, điểm tổng là trung bình
B. Bộ tiêu chí cấu hình được (lưu trong cấu hình/CSDL), khởi đầu bằng bộ ở phương án A
C. Bạn sẽ cung cấp bộ tiêu chí chính xác
X. Other (please specify)

[Answer]: A

## Q7. Nguồn kiến thức cho Copilot (US-02)

Copilot trả lời dựa trên MB Policy Center, kèm nguồn trích dẫn và hiệu lực — nhưng chưa có link/quyền truy cập (C-POLICY). (Bỏ qua câu này nếu Q1 không chọn US-02.)

A. Dùng một bộ tài liệu chính sách mẫu (Markdown/PDF) đặt trong dự án, tìm kiếm theo nội dung (RAG) để trả lời kèm trích dẫn + ngày hiệu lực
B. Như A, và cho phép tải thêm tài liệu chính sách qua giao diện
C. Không cần tài liệu nguồn — LLM tự trả lời (không có trích dẫn)
X. Other (please specify)

[Answer]: Tôi sẽ Cung cấp một số chính sách sản phẩm có sẵn

## Q8. Người dùng và đăng nhập

Epic quy định "lead đang ở user nào thì user đó keyin". Mã nguồn hiện chưa có đăng nhập (README ghi "Login / per-user credentials" là phần hoãn lại). Đợt này thế nào?

A. Không đăng nhập; demo với một RM cố định (cấu hình sẵn)
B. Đăng nhập đơn giản (vài tài khoản RM mẫu), mỗi RM chỉ thấy và keyin lead của mình
C. Chọn RM từ danh sách thả xuống (không mật khẩu) để mô phỏng phân quyền theo lead
X. Other (please specify)

[Answer]: B

## Q9. Bảo vệ dữ liệu khách hàng khi gửi cho AI (C-SEC)

Transcript chứa thông tin khách hàng và được gửi cho dịch vụ STT/LLM bên ngoài. Đợt này cần làm gì?

A. Không xử lý thêm (dữ liệu demo, không phải khách hàng thật)
B. Ẩn thông tin nhận dạng cá nhân (số điện thoại, số CMND/CCCD, số tài khoản…) trong transcript trước khi gửi LLM
C. Như B, và ghi lại nhật ký (audit log) mỗi lần gửi dữ liệu cho dịch vụ AI
X. Other (please specify)

[Answer]: A

---

# Câu hỏi bổ sung

## Q1a. Cách chia user story (đề xuất theo yêu cầu ở Q1)

US-01 trong epic rất lớn (chấm điểm + transcript/audio + autofill keyin + lưu lịch sử), lại cần nền tảng chưa có (đăng nhập, lead, gọi từ lead). Đề xuất chia thành các lát cắt nhỏ, mỗi lát demo được độc lập:

- **US-00 — Nền tảng RM**: đăng nhập RM, danh sách lead của mình, chi tiết lead, bấm gọi KH từ chi tiết lead (nối FreeSWITCH/MicroSIP hiện có).
- **US-01a — Kết quả cuộc gọi: hội thoại**: kết thúc call → màn "Kết quả cuộc gọi" mở với trình phát audio + transcript tách lượt RM/KH + AI tóm tắt; trạng thái đang xử lý; fallback khi không có ghi âm/STT lỗi.
- **US-01b — AI Auto Keyin**: AI điền sẵn form keyin (giai đoạn/kết quả/công việc tiếp theo), đánh dấu trường do AI điền, RM sửa/xác nhận lưu, RM đánh giá chất lượng AI; lưu tóm tắt/gợi ý vào hoạt động cuộc gọi.
- **US-01c — AI chấm điểm cuộc gọi**: điểm tổng 0-100, 5 tiêu chí kỹ năng, cảm xúc KH, insight — cột thứ nhất của màn "Kết quả cuộc gọi".
- **US-02 — Copilot tra cứu chính sách** trong lúc gọi.
- **US-03 — Lịch sử hoạt động**: hiển thị tóm tắt/gợi ý AI của từng cuộc gọi trong chi tiết lead.

A. Dùng cách chia 6 story trên và làm cả 6 trong đợt này
B. Dùng cách chia trên, làm US-00, US-01a, US-01b, US-01c, US-03; để US-02 (Copilot) sau
C. Giữ nguyên 3 US như epic, làm cả 3
X. Other (please specify)

[Answer]:Vẫn giữ 3 US. US 3 bổ sung phần lịch làm việc tôi mô tả sau đây. Thay đổi yêu cầu: Sau khi auto keyin, user ấn lưu, nếu user muốn xem lịch làm việc --> Hiển thị màn lịch làm việc có tất cả công việc đã thực hiện và công việc tiếp theo cho user. 

## Q7a. Cách Copilot trả lời từ tài liệu chính sách bạn cung cấp

Bạn sẽ cung cấp tài liệu chính sách sản phẩm. Copilot nên trả lời thế nào, và tài liệu ở định dạng nào? (trả lời một chữ cho cách trả lời và một chữ cho định dạng, ví dụ "A, C")

A. Chỉ trả lời dựa trên các tài liệu đó, luôn kèm trích dẫn (tên tài liệu/mục) và ngày hiệu lực; nếu tài liệu không có thông tin thì nói rõ "không tìm thấy" thay vì tự đoán
B. Ưu tiên tài liệu, nhưng được phép trả lời bằng kiến thức chung của LLM khi tài liệu không có (ghi rõ là không có nguồn)
C. Định dạng: Markdown / văn bản thường
D. Định dạng: PDF hoặc Word
E. Định dạng: cả hai
X. Other (please specify)

[Answer]:A,D.PDF

## Q10. Mâu thuẫn giữa Q3 và Q8: nhiều RM gọi cùng lúc

Q3 chọn giữ cách ghép cuộc gọi với file ghi âm bằng "file mới nhất". Q8 chọn có nhiều tài khoản RM. Nếu hai RM kết thúc cuộc gọi gần nhau, hệ thống có thể gắn nhầm ghi âm của RM này cho RM kia (sai transcript, sai autofill). Nhóm muốn thế nào?

A. Chấp nhận rủi ro: demo chỉ thực hiện một cuộc gọi tại một thời điểm; ghi rõ giới hạn này
B. Đổi sang ghép bằng mã cuộc gọi (call-id/UUID) để nhiều RM gọi cùng lúc vẫn đúng
X. Other (please specify)

[Answer]:A


## Q11. Lịch làm việc (bổ sung vào US-03): hiển thị công việc của ai?

Theo Q1a, sau khi RM lưu keyin có thể mở màn "Lịch làm việc" gồm các công việc đã thực hiện và công việc tiếp theo. Lịch hiển thị phạm vi nào?

A. Tất cả công việc của RM đang đăng nhập (mọi lead), có bộ lọc theo lead
B. Chỉ công việc của lead vừa gọi
C. Mặc định mở theo lead vừa gọi, có nút chuyển sang xem toàn bộ công việc của RM
X. Other (please specify)

[Answer]:Tham khảo giao diện file 5. LỊCH LÀM VIỆC (UPDATED) ở đường dẫn D:\WebProjects\mb-hackathon-2

## Q12. Lịch làm việc: RM được thao tác gì?

A. Chỉ xem; bấm vào một công việc để mở chi tiết lead / kết quả cuộc gọi tương ứng
B. Xem + đánh dấu công việc tiếp theo là đã hoàn thành + đổi ngày giờ thực hiện
C. Như B, và tạo/sửa/xóa công việc thủ công
X. Other (please specify)

Kiểu hiển thị (chọn thêm một chữ):

D. Dạng lịch theo tuần/tháng
E. Dạng danh sách theo dòng thời gian (đã làm / sắp tới)

[Answer]:Tham khảo giao diện file 5. LỊCH LÀM VIỆC (UPDATED) ở đường dẫn D:\WebProjects\mb-hackathon-2

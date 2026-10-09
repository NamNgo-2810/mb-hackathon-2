# Đánh giá nhu cầu User Stories

## Quyết định

**Thực hiện.**

## Lý do

- Đây là tính năng có nhiều màn hình người dùng trực tiếp thao tác: chi tiết lead, thực hiện cuộc gọi kèm Copilot, kết quả cuộc gọi 3 cột, hộp thoại điều hướng và lịch làm việc (`requirements.md` FR1–FR5).
- Logic nghiệp vụ phức tạp: luồng AI sau cuộc gọi, các phương án dự phòng, chấm điểm, phân quyền theo lead, và tạo công việc từ keyin.
- Phần lớn yêu cầu đã có tiêu chí Given/When/Then. Tuy vậy, cần gom chúng thành các lát cắt giá trị theo góc nhìn RM, để có thể demo và nghiệm thu độc lập từng phần (epic mục 4.2: "mỗi US là một lát cắt demo/nghiệm thu độc lập").

## Các yếu tố đã cân nhắc

- Loại dự án: tính năng mới trên nền mã có sẵn (brownfield). Luồng ghi âm → STT đã có (`business-overview.md`, `component-inventory.md`).
- Phạm vi người dùng: RM là người dùng duy nhất trong đợt này. CBQL nằm ngoài phạm vi (`requirements.md` mục 6).
- Độ phức tạp: mức Standard, gồm 3 US của epic cộng phần nền tảng RM.

## Nơi story mang lại giá trị nhiều nhất

- Chia US-02 (Kết quả cuộc gọi) thành các story nhỏ theo từng cột và theo từng tình huống lỗi.
- Tách rõ phần nền tảng (đăng nhập, lead, gọi) khỏi 3 US nghiệp vụ.
- Tạo tiêu chí nghiệm thu có thể kiểm thử cho từng story, phù hợp cách nhóm viết test sau khi code với độ phủ tối thiểu 80% (`team-practices.md`).

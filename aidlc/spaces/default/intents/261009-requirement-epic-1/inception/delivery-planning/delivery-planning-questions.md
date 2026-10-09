# Delivery Planning — Câu hỏi

Đầu vào:
- `unit-of-work.md` và `unit-of-work-dependency.md`: 6 unit, sơ đồ phụ thuộc không có vòng lặp. U1 và U2 không phụ thuộc unit nào. U3 cần U1 và U2. U4 cần U1, U2 và U5. U5 và U6 chỉ cần U1.
- `contract-summary.md`
- `stories.md`
- `team-practices.md`: không làm walking skeleton, commit thẳng lên `main`, không có CI.

Định nghĩa: **Bolt** là một lượt xây trọn một phần công việc (gồm một hoặc vài unit) đi qua các bước thiết kế, viết mã và kiểm thử, xong thì phải chạy được. Bước này chọn **thứ tự** làm các Bolt trong phạm vi mà sơ đồ phụ thuộc cho phép.

## Q1. Ưu tiên làm gì trước?

A. **Rủi ro trước.** Làm sớm những phần chưa chắc chắn nhất: cổng LLM (U2), rồi pipeline STT + AI và màn kết quả (U4). Lý do: chất lượng STT/AI là rủi ro số 1 theo epic (C-STT). Vì U4 cần U5 nên thứ tự là U1 → U2 → U5 → U4 → U3 → U6.
B. **Giá trị demo trước.** Làm theo luồng mà người xem demo thấy: U1 (chi tiết lead, gọi) → U2 → U3 (Copilot trong lúc gọi) → U5 → U4 (kết quả) → U6.
C. **Kết hợp.** Làm U1 và U2 trước, sau đó ưu tiên U5 → U4 (luồng sau cuộc gọi là giá trị chính của epic), rồi mới đến U3 và U6.
X. Other (please specify)

[Answer]: B

## Q2. Độ lớn của một Bolt

A. Mỗi Bolt gồm đúng một unit, tổng cộng 6 Bolt. Mỗi Bolt xong là commit lên `main`.
B. Gộp các unit nhỏ lại, ví dụ U1 + U2 thành một Bolt và U5 + U4 thành một Bolt.
X. Other (please specify)

[Answer]: A

## Q3. Cách đi qua các bước xây dựng (Construction)

A. **Làm trọn từng unit.** Unit nào cũng đi hết thiết kế chức năng → viết mã → kiểm tra, xong mới sang unit kế tiếp. Đây là mặc định, chạy tuần tự.
B. **Làm theo từng bước.** Làm Functional Design cho tất cả unit, sau đó mới viết mã cho tất cả unit. Cách này cho phép viết mã song song nhiều unit một lúc.
X. Other (please specify)

[Answer]: B

## Q4. Ai làm phần xây dựng?

A. Mình làm hết trong phiên này, tuần tự theo thứ tự đã chốt.
B. Mỗi nhóm của bạn nhận một unit và tự duyệt phần việc của mình.
X. Other (please specify)

[Answer]: B


## Q5. Câu bổ sung: Q3 và Q4 không đi cùng nhau được

Ở Q4, bạn chọn mỗi nhóm nhận một unit (B). Khi các nhóm cùng sở hữu unit, mỗi unit bắt buộc phải đi trọn các bước của nó trước, tức là cách "làm trọn từng unit". Cách này trái với lựa chọn "làm theo từng bước" ở Q3 (B).

Ngoài ra, chế độ nhiều nhóm còn cần thư mục gốc dự án là một kho Git. Hiện dự án **chưa có kho Git** (chưa chạy `git init`).

A. **Một phiên, làm theo từng bước.** Giữ Q3=B. Mình làm hết trong phiên này: thiết kế tất cả unit trước, rồi mới viết mã.
B. **Nhiều nhóm, làm trọn từng unit.** Giữ Q4=B và đổi Q3 sang A. Trước khi bắt đầu phần xây dựng, cần `git init` ở thư mục gốc.
X. Other (please specify)

[Answer]: A

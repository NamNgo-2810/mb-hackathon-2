# MB Bank KHCN — Knowledge Base demo cho CRM Chatbot AI

## Mục đích
Bộ dữ liệu minh họa phục vụ demo chatbot AI nhúng trên giao diện CRM. Đây **không phải bộ chính sách nội bộ hoặc tài liệu phê duyệt chính thức của MB**.

## Phạm vi
- 6 nhóm/sản phẩm minh họa: vay mua nhà, vay mua ô tô, vay tiêu dùng tín chấp, tiết kiệm có kỳ hạn, thẻ tín dụng quốc tế, QR Trả Sau.
- 48 FAQ mẫu.
- Quy tắc gợi ý/bán chéo.
- Metadata nguồn, hiệu lực và trạng thái xác minh.
- Guardrails cho chatbot.

## Quy ước dữ liệu
- `VERIFIED_PUBLIC`: chỉ dùng khi nội dung đã được kiểm tra trực tiếp từ nguồn công khai chính thức.
- `SIMULATED`: dữ liệu giả lập cho demo, không được mô tả là chính sách MB thật.
- `UNVERIFIED`: chưa xác minh được chính sách hiện hành.
- `EXPIRED`: tài liệu hết hiệu lực.

Trong bộ demo này, các điều kiện khái quát được đánh dấu là nội dung mô tả chung; mọi lãi suất, hạn mức, tỷ lệ tài trợ, kỳ hạn tối đa, phí và điều kiện phê duyệt cần đối chiếu tài liệu chính thức hiện hành trước khi sử dụng. Không có số liệu giả lập nào được trình bày như chính sách chính thức.

## Cách dùng
1. Nạp `02_product_policies.md` vào kho tài liệu/RAG.
2. Nạp `01_product_catalog.csv`, `03_product_faq.csv`, `05_kb_metadata.csv` nếu hệ thống hỗ trợ dữ liệu có cấu trúc.
3. Dùng `04_cross_sell_rules.md` làm tài liệu gợi ý, không dùng để kết luận khách hàng đủ điều kiện.
4. Áp dụng `06_chatbot_guardrails.md` làm system/developer instructions của chatbot.
5. Trước demo, xác nhận với đội dự án rằng mọi câu trả lời về lãi suất, phí, hạn mức và điều kiện hiện hành đều trả lời “chưa có dữ liệu xác minh” nếu chưa bổ sung tài liệu.

## Nguồn tham khảo công khai để đội dự án đối chiếu
- Trang KHCN MB: https://www.mbbank.com.vn/home/ca-nhan
- FAQ MB: https://www.mbbank.com.vn/faq
- Trang MB Priority: https://www.mbbank.com.vn/home/ca-nhan-cao-cap

Các liên kết chỉ là điểm bắt đầu tra cứu danh mục; không có nghĩa mọi trường chính sách trong bộ demo đã được xác minh từ các trang này.

# Yêu cầu — (CRM) AI phân tích cuộc gọi RM với Khách hàng (bản demo hackathon)

## 1. Phân tích mục đích

**Vấn đề:** cuộc gọi RM → KH đã được ghi âm, nhưng chưa ai khai thác. Sau mỗi cuộc gọi, RM phải gõ tay toàn bộ form keyin. Việc này tốn thời gian, dễ bỏ sót, và dữ liệu lead không đồng đều. (`epic 1.md` mục 2.1)

**Mục tiêu đợt này:** thêm một tầng AI vào luồng gọi → keyin hiện có. Tầng này tự phân tích ghi âm sau cuộc gọi, rồi điền sẵn form keyin, chấm điểm cuộc gọi và tóm tắt kèm gợi ý. Trong lúc gọi, RM tra cứu chính sách bằng Copilot. Sau khi gọi, RM xem lại tóm tắt AI trong lịch sử lead và xem công việc trên lịch làm việc. RM chỉ còn việc rà soát, sửa và xác nhận.

**Loại yêu cầu:** tính năng mới, xây trên nền mã có sẵn (brownfield). Phạm vi gồm nhiều thành phần (backend Spring Boot, server Node, giao diện web), độ phức tạp mức Standard.

**Phạm vi:** đúng 3 US của epic [Q1a], đánh số lại theo thứ tự diễn ra trong luồng làm việc của RM (trong lúc gọi → sau cuộc gọi → xem lại) theo yêu cầu của người dùng ở cổng duyệt:
- **US-01**: AI Sales Knowledge Copilot trong lúc gọi (là US-02 trong `epic 1.md`).
- **US-02**: màn "Kết quả cuộc gọi" 3 cột, gồm AI chấm điểm, phân tích hội thoại và AI Auto Keyin (là US-01 trong `epic 1.md`).
- **US-03**: tóm tắt/gợi ý AI trong Lịch sử hoạt động của lead (giữ số như epic). Theo yêu cầu bổ sung của người dùng, US-03 có thêm **màn Lịch làm việc** [Q1a].

Từ đây trở đi, mọi ký hiệu US trong tài liệu này dùng cách đánh số mới. Khi đối chiếu với `epic 1.md` thì đổi chéo US-01 ↔ US-02.

Để 3 US chạy được, cần thêm phần nền tảng RM (đăng nhập, lead, gọi từ chi tiết lead). Mã nguồn hiện chưa có phần này (`business-overview.md`, `code-structure.md`).

**Hiện trạng mã nguồn liên quan** (`business-overview.md`, `architecture.md`, `code-structure.md`):
- Đã có luồng gọi qua FreeSWITCH/MicroSIP.
- Đã có pipeline bất đồng bộ: tạo bản ghi cuộc gọi → kết thúc → lấy file ghi âm "mới nhất" từ EC2 → tách kênh stereo RM/KH → chuyển giọng nói thành văn bản tiếng Việt bằng Google STT → lưu và trả transcript.
- Phần **chưa có**: LLM, lead/keyin, đăng nhập, Copilot, lịch, và giao diện theo mockup.

**Chuẩn giao diện:** 5 mockup HTML ở thư mục gốc dự án:
- `1-chi-tiet-tiem-nang-ban.html`
- `2-thuc-hien-cuoc-goi.html`
- `3-ket-qua-cuoc-goi.html`
- `4-lua-chon-dieu-huong.html`
- `5-lich-lam-viec.html`

Mockup cố định khung 1440px, chỉ dùng cho web [Q2, Q11, Q12].

## 2. Yêu cầu chức năng

Quy ước ưu tiên: **Must** = bắt buộc cho demo; **Should** = nên có; **Could** = làm nếu còn thời gian.

### FR1 — Nền tảng RM: đăng nhập, lead, gọi từ chi tiết lead (phục vụ US-01/02/03)

- **FR1.1 (Must)**: Hệ thống có đăng nhập bằng tên đăng nhập và mật khẩu cho vài tài khoản RM mẫu, tạo sẵn bằng dữ liệu seed [Q8]. Khi phiên đăng nhập hết hạn, RM được đưa về màn đăng nhập.
  - *Given* RM có tài khoản hợp lệ, *When* RM đăng nhập đúng mật khẩu, *Then* RM vào được danh sách lead của mình.
  - *Given* mật khẩu sai, *When* RM đăng nhập, *Then* màn hình báo "Sai tên đăng nhập hoặc mật khẩu" và không tạo phiên.
- **FR1.2 (Must)**: Danh sách tiềm năng bán chỉ hiển thị lead mà RM đang đăng nhập phụ trách. Mỗi lead hiển thị mã lead, sản phẩm, nguồn, SLA còn lại, điểm xếp hạng /10, mức ưu tiên ("Khai thác ngay" / "Hot Lead") và trạng thái (mockup màn 1) [Q5, Q8].
  - *Given* RM A và RM B đều có lead, *When* RM A mở danh sách, *Then* không thấy lead nào của RM B.
  - *Given* RM chưa có lead nào, *When* mở danh sách, *Then* màn hình báo "Chưa có tiềm năng bán", không để trống.
- **FR1.3 (Must)**: Chi tiết lead hiển thị:
  - thông tin KH (mã, tên, phân khúc, số điện thoại);
  - các bước trạng thái lead: Chưa tiếp cận → Đang tiếp cận → Chuyển cơ hội / Từ chối;
  - khối "Nội dung": thông tin tiềm năng bán, sản phẩm, dư nợ;
  - khối "Kịch bản bán";
  - "Quản lý hoạt động", tức lịch sử hoạt động (mockup màn 1) [Q5].
- **FR1.4 (Must)**: Mọi API đọc hoặc ghi lead, cuộc gọi, keyin, công việc đều kiểm tra phía server rằng lead đó thuộc RM đang đăng nhập ("lead đang ở user nào thì user đó keyin", epic mục 4.2).
  - *Given* RM A đã đăng nhập, *When* RM A gọi API lấy chi tiết hoặc lưu keyin của lead thuộc RM B, *Then* API trả 403 và không thay đổi dữ liệu.
- **FR1.5 (Must)**: Chi tiết lead có nút **Gọi KH**. Khi bấm, hệ thống:
  - tạo bản ghi cuộc gọi gắn với lead và RM;
  - bắt đầu cuộc gọi qua MicroSIP hoặc softphone FreeSWITCH hiện có [Q3];
  - mở màn "Thực hiện cuộc gọi".

  Cuộc gọi chỉ được phát sinh từ chi tiết lead (C-POINT, epic mục 4.2).
  - *Given* số điện thoại KH không hợp lệ, *When* RM bấm Gọi KH, *Then* màn hình báo lỗi và không tạo cuộc gọi.

### FR2 — Thực hiện cuộc gọi (khung chứa US-01)

- **FR2.1 (Must)**: Màn "Thực hiện cuộc gọi" (mockup màn 2) gồm hai khối:
  - **Khối trái**: các tab Thông tin KH / Chi tiết TNB / Lịch sử cuộc gọi, thẻ "Tiềm năng bán đang kích hoạt" và "Lịch sử tương tác gần nhất".
  - **Khối phải**: panel Copilot (FR3).

  Thanh trên cùng hiển thị trạng thái "Đang gọi", đồng hồ thời lượng và nút **Kết thúc**.
- **FR2.2 (Must)**: Cuộc gọi kết thúc khi RM bấm Kết thúc, hoặc khi MicroSIP báo sự kiện kết thúc. Khi đó hệ thống đánh dấu cuộc gọi đã kết thúc, khởi chạy pipeline phân tích (FR4.1) và chuyển sang màn "Kết quả cuộc gọi".
  - *Given* sự kiện kết thúc đến hai lần cho cùng một cuộc gọi, *When* hệ thống xử lý, *Then* pipeline chỉ chạy một lần.

### FR3 — US-01: AI Sales Knowledge Copilot (trong lúc gọi)

- **FR3.1 (Must)**: Copilot dùng nguồn kiến thức là các file **PDF chính sách sản phẩm** do người dùng cung cấp, đặt trong một thư mục cấu hình của dự án [Q7, Q7a]. Mỗi tài liệu có tên và **ngày hiệu lực**. Khi khởi động, hệ thống trích văn bản và lập chỉ mục để tìm kiếm theo nội dung.
  - *Given* một file PDF không đọc được (hỏng hoặc chỉ có ảnh), *When* lập chỉ mục, *Then* hệ thống ghi log tên file lỗi, bỏ qua file đó và vẫn nạp các file còn lại.
- **FR3.2 (Must)**: RM đặt câu hỏi bằng ngôn ngữ tự nhiên, ví dụ về lãi suất, phí, điều kiện hay sản phẩm. Copilot **chỉ trả lời dựa trên nội dung tài liệu**, và mỗi câu trả lời luôn kèm **nguồn trích dẫn** (tên tài liệu và mục/trang) cùng **ngày hiệu lực** (mockup màn 2) [Q7a].
  - *Given* tài liệu có biểu lãi suất 12 tháng, *When* RM hỏi "Lãi suất gửi 12 tháng cho khách VIP là bao nhiêu?", *Then* câu trả lời nêu đúng con số trong tài liệu, kèm tên tài liệu và ngày hiệu lực.
- **FR3.3 (Must)**: Khi tài liệu không chứa thông tin cần thiết, Copilot trả lời rõ "Không tìm thấy thông tin này trong tài liệu chính sách hiện có" và không tự đoán [Q7a].
  - *Given* câu hỏi về sản phẩm không có trong tài liệu, *When* RM hỏi, *Then* Copilot trả lời "không tìm thấy" và không đưa ra con số nào.
- **FR3.4 (Should)**: Sau mỗi câu trả lời, Copilot đưa 2–4 **gợi ý câu hỏi tiếp theo**. RM bấm vào một gợi ý thì câu đó được gửi đi như câu hỏi mới.
- **FR3.5 (Should)**: Panel có **Lịch sử tra cứu** trong phiên và nút "Cuộc trò chuyện mới".
- **FR3.6 (Must)**: Lỗi dịch vụ LLM hoặc quá thời gian (NFR1) chỉ hiện thông báo trong panel Copilot, không làm gián đoạn cuộc gọi hay màn hình chính.

### FR4 — US-02: Kết quả cuộc gọi (AI chấm điểm + Phân tích hội thoại + AI Auto Keyin)

- **FR4.1 (Must)**: Pipeline bất đồng bộ sau cuộc gọi chạy theo thứ tự:
  1. Lấy file ghi âm "mới nhất" từ máy chủ ghi âm EC2 [Q3, Q10].
  2. Tách kênh RM/KH và chuyển thành văn bản tiếng Việt bằng Google STT (đã có trong mã).
  3. Gọi **Anthropic Claude** [Q4] với transcript và thông tin lead để nhận về kết quả có cấu trúc: tóm tắt, chấm điểm (FR4.4), giá trị keyin đề xuất (FR4.5) và công việc tiếp theo.
  4. Lưu kết quả gắn với cuộc gọi.
  - *Given* ghi âm và STT thành công, *When* pipeline chạy xong, *Then* cuộc gọi có transcript, tóm tắt, điểm và keyin đề xuất, và trạng thái là "Hoàn tất".
- **FR4.2 (Must)**: Trong lúc pipeline chạy, màn "Kết quả cuộc gọi" hiển thị trạng thái đang xử lý theo từng bước (đang lấy ghi âm / đang chuyển giọng nói / đang phân tích AI) và tự cập nhật khi có kết quả, không cần tải lại trang.
- **FR4.3 (Must)**: **Cột 2 — Phân tích hội thoại** (mockup màn 3/4) gồm:
  - trình phát audio: phát/tạm dừng, tua, đổi tốc độ, hiển thị thời gian;
  - transcript tách lượt RM/KH, mỗi lượt có mốc thời gian;
  - nút tải file `.wav`.

  **Could**: làm nổi bật lượt đang phát, và hiện nhãn "AI nhận diện" ý định của KH.
- **FR4.4 (Must)**: **Cột 1 — AI chấm điểm cuộc gọi** (mockup màn 4) hiển thị:
  - thẻ KH;
  - **điểm tổng /100** và **hạng** (xem công thức bên dưới);
  - **5 tiêu chí** chấm theo thang /10:
    1. Chào hỏi & nhận diện KH
    2. Khai thác nhu cầu & số tiền
    3. Nắm vững chính sách & lãi suất
    4. Xử lý thắc mắc & thuyết phục
    5. Chốt cam kết & bước tiếp theo
  - **cảm xúc / giọng điệu** của cuộc gọi (Tích cực / Trung tính / Tiêu cực);
  - **AI Insight**: một điểm sáng nổi bật và một gợi ý nâng cao.

  **Công thức điểm tổng** (nhóm đề xuất cho demo; người dùng giao cho nhóm quyết định ở cổng duyệt): LLM chấm từng tiêu chí theo thang 0–10, bước 0,5. Điểm tổng = (tổng 5 tiêu chí) × 2, làm tròn tới số nguyên gần nhất, nên luôn nằm trong 0–100. Hệ thống tự tính điểm tổng từ 5 tiêu chí chứ không lấy con số tổng do LLM trả về. Các con số trên mockup chỉ để minh họa giao diện, không ràng buộc công thức.
  - Ví dụ: 10 + 10 + 9,5 + 9 + 9,5 = 48 → điểm tổng 96.

  **Ngưỡng hạng** (đề xuất cho demo, chỉnh được bằng cấu hình): A "Xuất sắc" ≥ 85; B "Tốt" 70–84; C "Trung bình" 50–69; D "Cần cải thiện" < 50.

  [Q6; tên tiêu chí theo mockup — xem giả định A5]
  - *Given* LLM trả về điểm tiêu chí ngoài khoảng 0–10, *When* lưu kết quả, *Then* hệ thống từ chối kết quả đó và chạy lại một lần (NFR3); nếu vẫn lỗi thì hiển thị "Chưa có điểm AI".
- **FR4.5 (Must)**: **Cột 3 — AI Auto Keyin** (mockup màn 4) điền sẵn form keyin.

  Các trường form [Q5; nhãn lấy theo mockup màn 4, danh sách trường là đề xuất cho demo, người dùng chỉnh sau]:
  - Giai đoạn lead (Chưa tiếp cận / Đang tiếp cận / Chuyển cơ hội / Từ chối)
  - Kết quả cuộc gọi
  - Nhu cầu / số tiền
  - Kỳ hạn
  - Lãi suất chốt
  - Sản phẩm chốt
  - Nguồn tiền luân chuyển
  - Thời gian dự kiến
  - Kênh thực hiện
  - Băn khoăn / phản đối (đã giải đáp hay chưa)
  - **Công việc tiếp theo**: mỗi việc có tiêu đề, ngày giờ, loại (Lịch hẹn / Nhắc gọi / Task Zalo) và mức ưu tiên
  - Ghi chú của RM

  Mỗi trường do AI điền có **đánh dấu "AI"** kèm mốc thời gian trong ghi âm nơi trích xuất. RM sửa được mọi trường.
  - *Given* AI đã điền một trường, *When* RM sửa trường đó, *Then* dấu "AI" chuyển thành "RM đã sửa".
  - *Given* transcript không nhắc tới kỳ hạn, *When* AI điền form, *Then* trường Kỳ hạn để trống, không bịa giá trị.
- **FR4.6 (Must)**: Màn có vùng **RM đánh giá chất lượng AI**, gồm thang 1–10, các thẻ nhận xét nhanh và ô góp ý tự do (mockup màn 4).
- **FR4.7 (Must)**: Khi RM bấm **"Lưu hồ sơ cuộc gọi"**, hệ thống:
  - lưu keyin;
  - lưu **AI tóm tắt và gợi ý** vào bản ghi hoạt động cuộc gọi, đủ để US-03 hiển thị lại (epic node C8);
  - với từng trường, lưu việc RM giữ nguyên hay đã sửa giá trị AI. Dữ liệu này phục vụ chỉ tiêu "tỷ lệ RM chấp nhận gợi ý AI" (epic mục 3.1);
  - cập nhật giai đoạn lead;
  - **tạo công việc** từ mục "Công việc tiếp theo";
  - lưu đánh giá chất lượng AI.
  - *Given* RM bấm Lưu hai lần liên tiếp, *When* hệ thống xử lý, *Then* chỉ có một bản ghi keyin và các công việc không bị tạo trùng.
  - *Given* thiếu trường bắt buộc (Giai đoạn lead, Kết quả cuộc gọi), *When* RM bấm Lưu, *Then* màn hình chỉ ra trường thiếu, giữ nguyên những gì RM đã nhập và không lưu.
- **FR4.8 (Must)**: Có **phương án dự phòng (fallback)** cho các trường hợp: không có ghi âm, STT lỗi, hoặc LLM lỗi/quá thời gian. Khi đó màn vẫn mở được, kèm thông báo lý do, với form keyin trống để RM nhập tay và lưu bình thường (epic: "không có audio vẫn mở được form trống").
  - *Given* sau thời gian chờ có giới hạn vẫn chưa có file ghi âm, *When* pipeline dừng, *Then* trạng thái là "Không có ghi âm" và form trống vẫn lưu được.

  Cách hiển thị từng cột theo loại lỗi (nhóm đề xuất cho demo; người dùng giao cho nhóm quyết định ở cổng duyệt):

  | Tình huống | Cột 1 — Chấm điểm | Cột 2 — Hội thoại | Cột 3 — Keyin |
  |---|---|---|---|
  | Không có ghi âm | "Chưa có điểm AI — không có ghi âm" | Không có trình phát; báo "Không tìm thấy ghi âm cho cuộc gọi này" | Form trống, RM nhập tay |
  | Có ghi âm, STT lỗi | "Chưa có điểm AI — chưa có transcript" | Có trình phát và nút tải `.wav`; báo "Không chuyển được giọng nói thành văn bản" | Form trống, RM nhập tay |
  | Có transcript, LLM lỗi/quá thời gian | "Chưa có điểm AI"; không hiện tóm tắt | Có trình phát và transcript đầy đủ | Form trống, RM nhập tay; có nút **Thử phân tích AI lại** |

  - *Given* STT thành công nhưng LLM lỗi, *When* RM mở màn, *Then* RM vẫn nghe được audio và đọc được transcript, còn cột 1 hiển thị "Chưa có điểm AI".
  - *Given* RM bấm "Thử phân tích AI lại" và lần này LLM thành công, *When* có kết quả, *Then* form chỉ được điền vào các trường RM chưa nhập, không ghi đè lên những gì RM đã gõ.
- **FR4.9 (Must)**: Sau khi lưu thành công, hệ thống hiện hộp thoại "Đã lưu hồ sơ cuộc gọi thành công!" (mockup màn 4) với 3 lựa chọn:
  - **Liên hệ khách hàng tiếp theo**: mở lead kế tiếp trong danh sách của RM. Lead kế tiếp là lead có ưu tiên cao nhất ở trạng thái "Chưa tiếp cận" hoặc "Đang tiếp cận", không tính lead vừa gọi (nhóm đề xuất cho demo);
  - **Chi tiết lịch làm việc**: mở màn Lịch làm việc (FR5.2);
  - **Đóng & Quay lại trang chủ**.
  - *Given* RM không còn lead nào ở trạng thái "Chưa tiếp cận" hoặc "Đang tiếp cận", *When* RM chọn "Liên hệ khách hàng tiếp theo", *Then* hệ thống báo "Bạn đã liên hệ hết tiềm năng bán cần xử lý" và mở danh sách tiềm năng bán.

### FR5 — US-03: Lịch sử hoạt động + Lịch làm việc

- **FR5.1 (Must)**: Trong "Quản lý hoạt động" ở chi tiết lead, mỗi cuộc gọi đã lưu hiển thị thêm **AI tóm tắt và gợi ý**, chỉ đọc, thu gọn/mở rộng được, đọc từ dữ liệu FR4.7 lưu. Kèm theo là liên kết mở lại màn "Kết quả cuộc gọi" của cuộc gọi đó (xem câu hỏi mở O4).
  - *Given* cuộc gọi đã lưu khi AI lỗi (fallback), *When* xem lịch sử, *Then* hoạt động vẫn hiển thị keyin RM nhập, cùng dòng "Không có tóm tắt AI".
- **FR5.2 (Must)**: Màn **"Lịch làm việc & Tác vụ CRM"** (mockup màn 5) [Q1a, Q11, Q12] gồm:
  - **Thanh đầu**: tên cuộc gọi và KH vừa đồng bộ.
  - **Panel trái**:
    - khối "AI Key-in từ cuộc gọi" gồm các công việc vừa tạo từ cuộc gọi vừa lưu, mỗi việc có mức ưu tiên, giờ, sản phẩm, giá trị và tóm tắt AI;
    - khối "Lịch trình còn lại trong ngày".
  - **Panel phải**: lịch dạng **tuần** (Thứ 2 – Thứ 7, 08:00–17:00), có nút tuần trước/sau và nút "Hôm nay".
- **FR5.3 (Must)**: Lịch hiển thị **tất cả công việc của RM đang đăng nhập** ở mọi lead, cả việc đã làm lẫn sắp tới, gồm cả công việc không gắn lead (ví dụ họp giao ban) lấy từ dữ liệu seed. Có bộ lọc theo loại: Tất cả / Lịch hẹn / Nhắc gọi / Task Zalo (mockup màn 5).
  - *Given* RM vừa lưu keyin với 2 công việc tiếp theo, *When* mở lịch, *Then* 2 công việc nằm đúng ngày giờ và được đánh dấu "Vừa lưu từ cuộc gọi".
  - *Given* RM không có công việc nào trong tuần, *When* xem tuần đó, *Then* lịch báo "Không có công việc trong tuần này".
- **FR5.4 (Must)**: Trên mỗi công việc, RM có thể:
  - **Đã xong**: đánh dấu hoàn thành;
  - **Đổi giờ**: đổi ngày giờ;
  - **Gọi ngay**: mở luồng gọi FR1.5 cho lead của công việc;
  - **Chi tiết TNB**: mở chi tiết lead.

  Ngoài ra RM **tạo được công việc mới** bằng tay (mockup màn 5).
  - *Given* RM đổi giờ một công việc sang thời điểm đã qua, *When* lưu, *Then* màn hình cảnh báo và yêu cầu xác nhận.
- **FR5.5 (Should)**: Lịch chuyển được giữa các chế độ xem **Ngày / Tháng / Danh sách**. Chế độ mặc định là Tuần.

## 3. Yêu cầu phi chức năng

- **NFR1 — Hiệu năng**:
  - Với cuộc gọi dài tối đa 5 phút, kể từ khi đã có file ghi âm, kết quả AI (transcript, tóm tắt, điểm, keyin) có trên màn hình trong ≤ 120 giây ở ≥ 90% lần chạy demo.
  - Copilot trả lời trong ≤ 15 giây ở ≥ 90% câu hỏi. Quá 30 giây thì báo lỗi (FR3.6). Hai ngưỡng này do nhóm đề xuất cho demo (người dùng xác nhận ở cổng duyệt) và chỉnh được bằng cấu hình.
  - Các màn danh sách, chi tiết và lịch mở trong ≤ 2 giây khi chạy cục bộ.
- **NFR2 — Bảo mật**:
  - Mật khẩu lưu dạng băm (BCrypt). Không lưu và không ghi log mật khẩu thô.
  - Phân quyền theo lead được kiểm tra ở server (FR1.4).
  - Khóa API Anthropic và Google lấy từ biến môi trường, **không ghi vào mã nguồn hay file cấu hình được commit** (guardrail giai đoạn Construction).
  - Nội dung transcript và tài liệu PDF được đưa vào prompt như **dữ liệu**, có phân tách rõ, để không làm thay đổi chỉ dẫn của hệ thống (chống prompt injection).
  - Mọi dữ liệu nhập ở biên hệ thống đều được kiểm tra hợp lệ.
- **NFR3 — Độ tin cậy**:
  - Kết quả LLM được kiểm tra theo một cấu trúc JSON cố định. Kết quả sai cấu trúc được chạy lại một lần, nếu vẫn sai thì chuyển sang fallback (FR4.8).
  - Lỗi AI không bao giờ chặn việc lưu keyin.
  - Mỗi lần gọi STT/LLM có timeout.
- **NFR4 — Kiểm thử** (`team-practices.md`):
  - Viết test sau khi hiện thực (test-after), độ phủ dòng ≥ 80%, đo thủ công trên máy trước mỗi commit: backend dùng JaCoCo qua `mvn verify`, phần Node dùng `npm test` có đo độ phủ.
  - Không có CI.
  - Các lời gọi STT/LLM được giả lập (mock) trong test.
- **NFR5 — Ngôn ngữ & khả dụng**:
  - Toàn bộ giao diện bằng tiếng Việt, bố cục theo 5 mockup ở khổ desktop 1440px.
  - Các chức năng chính dùng được bằng bàn phím.
  - Trạng thái không chỉ thể hiện bằng màu sắc mà luôn có chữ đi kèm.
- **NFR6 — Quan sát**: Mỗi bước pipeline (lấy ghi âm, STT, LLM, lưu) ghi log kèm mã cuộc gọi, thời gian chạy và kết quả (thành công/lỗi). Lỗi luôn được ghi log, không được bỏ qua im lặng.
- **NFR7 — Dữ liệu**: Chỉ dùng dữ liệu demo (KH và lead giả), không dùng thông tin khách hàng thật. Vì vậy không ẩn thông tin cá nhân trước khi gửi dịch vụ AI [Q9].

## 4. Ràng buộc

- Chỉ làm **web**, không làm app di động [Q2].
- Tổng đài và ghi âm dùng **FreeSWITCH trên EC2 + MicroSIP**, thay cho 3CX/SDK FPT. File ghi âm được nối với cuộc gọi bằng cách lấy **"file mới nhất"** [Q3]. Vì vậy demo **chỉ thực hiện một cuộc gọi tại một thời điểm** [Q10].
- STT dùng Google Cloud Speech-to-Text (đã có). LLM dùng **Anthropic Claude**; người dùng sẽ cung cấp API key khi tới bước này [Q4].
- Backend xây tiếp trên Spring Boot `backend/` hiện có. Server Node `server/` giữ vai trò điều khiển MicroSIP và phát sự kiện cuộc gọi qua SSE (`architecture.md`).
- Chỉ chạy cục bộ. Git chỉ dùng trên máy, commit thẳng lên `main`, không có CI (`team-practices.md`).

## 5. Giả định

- **A1**: Các trường form keyin ở FR4.5 do nhóm đề xuất dựa trên mockup màn 4 và epic, vì form gốc (pageId 536766154) không truy cập được. Người dùng sẽ chỉnh sau [Q5].
- **A2**: Dữ liệu RM, KH, lead, kịch bản bán và các công việc không gắn lead (họp, đào tạo…) là dữ liệu seed tự dựng, khớp nội dung trên mockup [Q5].
- **A3**: Mỗi file PDF chính sách đều có ghi ngày hiệu lực. Nếu không ghi được trong file, ngày hiệu lực khai báo kèm trong một file cấu hình nhỏ đặt cạnh các PDF [Q7a].
- **A4**: Nút "Outlook/Google", "Mở Zalo ZNS" và mẫu tin nhắn soạn sẵn trên mockup màn 5 chỉ hiển thị, không tích hợp thật. "Task Zalo" chỉ là một loại công việc.
- **A5**: **Thang điểm theo mockup**: mỗi tiêu chí chấm trên thang /10, điểm tổng /100, có hạng A/B/C/D. Công thức và ngưỡng hạng xem FR4.4; người dùng giao cho nhóm đề xuất cho demo. Cách này thay cho phương án Q6=A (mỗi tiêu chí 0–100, lấy trung bình). Tên 5 tiêu chí lấy theo mockup và tương ứng với bộ tiêu chí ở Q6.
- **A6**: Các chỉ số "tốc độ nói (từ/phút)" và "độ ngắt quãng" trên mockup được tính từ mốc thời gian của transcript (mức Could). Nếu không tính được thì ẩn đi.
- **A7**: Pipeline giữ cơ chế chờ ghi âm có giới hạn đã có trong mã (trạng thái chưa sẵn sàng → thử lại có giới hạn) (`business-overview.md`, BR-nonterminal-not-ready).

## 6. Ngoài phạm vi

- App di động (APP), chỉ làm web [Q2].
- Toàn bộ "Tầm nhìn mở rộng" ở epic mục 4.4: Coaching Cockpit cho CBQL, màn quản trị và lưu trữ cuộc gọi CBQL, CBQL ghi đè điểm, QA 100%, cảnh báo tuân thủ, phân tích lý do thất bại, dự báo khả năng chốt, cross-sell tự động, kịch bản bán theo ngữ cảnh, rating KH (VoC).
- Tích hợp thật với 3CX/SDK FPT, kho ghi âm nội bộ MB, MB Policy Center, đồng bộ Outlook/Google, gửi Zalo ZNS.
- Nối file ghi âm bằng mã cuộc gọi và hỗ trợ nhiều cuộc gọi đồng thời [Q10].
- Ẩn thông tin cá nhân và audit log khi gửi dữ liệu cho dịch vụ AI [Q9].
- Tải tài liệu chính sách lên qua giao diện. Tài liệu được đặt sẵn vào thư mục [Q7].
- Dashboard đo OKR. Đợt này chỉ ghi lại dữ liệu cần cho việc đo (FR4.7).

## 7. Câu hỏi còn mở

- **O1**: Người dùng sẽ cung cấp API key Anthropic và chọn model Claude cụ thể khi tới bước hiện thực [Q4].
- **O2**: Người dùng sẽ cung cấp các file PDF chính sách sản phẩm, kèm ngày hiệu lực nếu trong file không ghi [Q7, giả định A3].
- **O3**: Đã chốt: công thức điểm tổng và ngưỡng hạng do nhóm đề xuất cho demo (FR4.4).
- **O4**: Câu C-HIST của epic: mục tóm tắt AI trong Lịch sử hoạt động có kèm liên kết nghe lại audio không? Đợt này dự kiến kèm liên kết mở màn "Kết quả cuộc gọi", trong đó có audio. Vì có đăng nhập theo RM, chỉ RM giữ lead mới xem được.
- **O5**: Lưu ý vận hành, không phải yêu cầu: khóa API thật đang nằm trong `backend/src/main/resources/application.properties` và `.env`. Nhóm đã chọn chỉ coi việc xử lý là khuyến nghị (`team-practices.md`). Khóa Anthropic mới vẫn phải lấy từ biến môi trường (NFR2).

## Sources

- `epic 1.md`: phiếu BM.02, nguồn chính cho mục đích, 3 US, luồng nghiệp vụ, tiêu chí nghiệm thu, OKR và phạm vi mở rộng.
- Mockup giao diện: `1-chi-tiet-tiem-nang-ban.html`, `2-thuc-hien-cuoc-goi.html`, `4-lua-chon-dieu-huong.html`, `5-lich-lam-viec.html`. `3-ket-qua-cuoc-goi.html` được coi là cùng bố cục với màn 4 nhưng không có hộp thoại sau khi lưu.
- Phân tích mã nguồn: `business-overview.md`, `architecture.md`, `code-structure.md` (`aidlc/spaces/default/codekb/mb-hackathon-2/`).
- Thực hành của nhóm: `team-practices.md` (Practices Discovery).
- Câu trả lời của người dùng: `requirements-analysis-questions.md`, gồm Q1–Q12, Q1a và Q7a. Ký hiệu [Qn] trong tài liệu này trỏ tới các câu đó.

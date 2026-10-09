# User Stories — AI phân tích cuộc gọi RM với Khách hàng

Nguồn yêu cầu: `requirements.md`, theo cách đánh số US mới (US-01 Copilot, US-02 Kết quả cuộc gọi, US-03 Lịch sử và Lịch làm việc). Persona: **P1 — RM bán lẻ chăm sóc KHCN** (`personas.md`). Các câu trả lời kế hoạch nằm trong `user-stories-questions.md`.

## Bản đồ story

| Nhóm | Ý nghĩa | Story | Ưu tiên |
|---|---|---|---|
| US1 | US-01 — Copilot trong lúc gọi | US1.1, US1.2, US1.3 | Must, Must, Should |
| US2 | US-02 — Kết quả cuộc gọi | US2.1, US2.2, US2.3, US2.4 | Must |
| US3 | US-03 — Lịch sử hoạt động và Lịch làm việc | US3.1, US3.2, US3.3, US3.4 | Must, Must, Must, Should |
| US4 | Nền tảng RM: đăng nhập, lead, gọi từ lead | US4.1–US4.4 | Could, **làm sau cùng** [Q1] |

**Cách demo độc lập trước khi có US4**: hệ thống dùng một RM mẫu và một lead mẫu cố định từ dữ liệu seed, và **mở thẳng màn chi tiết lead (S1) của lead mẫu**. Màn này không qua bước đăng nhập và không có danh sách lead theo RM. Từ S1, RM bấm "Gọi KH" để vào luồng US1–US3.

---

## US1 — Copilot trong lúc gọi (US-01)

### US1.1 — Màn thực hiện cuộc gọi
**Là** RM, **tôi muốn** xem thông tin KH và lead ngay trong lúc gọi rồi kết thúc cuộc gọi trên cùng một màn, **để** tư vấn đúng ngữ cảnh mà không phải chuyển qua lại giữa các màn.
- Ưu tiên: **Must** · Yêu cầu: FR2.1, FR2.2 · Mockup: `2-thuc-hien-cuoc-goi.html`
- **AC1.1.1**: *Given* RM đang gọi cho lead mẫu, *When* màn mở ra, *Then* màn hiển thị trạng thái "Đang gọi", đồng hồ thời lượng tăng dần, khối thông tin KH (các tab Thông tin KH / Chi tiết TNB / Lịch sử cuộc gọi) và panel Copilot.
- **AC1.1.2**: *Given* cuộc gọi đang diễn ra, *When* RM bấm **Kết thúc**, *Then* cuộc gọi được đánh dấu đã kết thúc, pipeline phân tích bắt đầu chạy và màn chuyển sang "Kết quả cuộc gọi".
- **AC1.1.3**: *Given* MicroSIP báo sự kiện kết thúc hai lần cho cùng một cuộc gọi, *When* hệ thống xử lý, *Then* pipeline chỉ chạy một lần.
- INVEST: nhỏ, kiểm thử được bằng API kết thúc cuộc gọi cộng với giao diện. Phụ thuộc lead mẫu, không phụ thuộc US4.

### US1.2 — Hỏi Copilot về chính sách, nhận câu trả lời có nguồn
**Là** RM, **tôi muốn** hỏi Copilot về lãi suất, phí và điều kiện sản phẩm bằng câu tự nhiên, rồi nhận câu trả lời kèm nguồn và ngày hiệu lực, **để** tư vấn KH chính xác ngay trong lúc gọi.
- Ưu tiên: **Must** · Yêu cầu: FR3.1, FR3.2, FR3.3, FR3.6, NFR1, NFR2
- **AC1.2.1**: *Given* thư mục chính sách có PDF chứa biểu lãi suất 12 tháng, *When* RM hỏi "Lãi suất gửi 12 tháng cho khách VIP là bao nhiêu?", *Then* câu trả lời nêu đúng con số trong tài liệu, kèm tên tài liệu, mục/trang và ngày hiệu lực.
- **AC1.2.2**: *Given* không tài liệu nào chứa thông tin được hỏi, *When* RM hỏi, *Then* Copilot trả lời "Không tìm thấy thông tin này trong tài liệu chính sách hiện có" và không đưa ra con số nào.
- **AC1.2.3**: *Given* có một file PDF hỏng nằm trong thư mục, *When* hệ thống lập chỉ mục, *Then* file đó được ghi log và bỏ qua, các tài liệu còn lại vẫn tra cứu được.
- **AC1.2.4**: *Given* dịch vụ LLM lỗi hoặc không trả lời trong 30 giây, *When* RM hỏi, *Then* chỉ panel Copilot hiện thông báo lỗi, còn cuộc gọi và các phần khác của màn vẫn hoạt động bình thường.
- **AC1.2.5**: *Given* trong tài liệu có câu mang tính mệnh lệnh (ví dụ "bỏ qua chỉ dẫn trước"), *When* RM hỏi, *Then* Copilot vẫn chỉ trả lời câu hỏi dựa trên nội dung tài liệu.

### US1.3 — Gợi ý câu hỏi tiếp theo và lịch sử tra cứu
**Là** RM, **tôi muốn** thấy câu hỏi gợi ý tiếp theo và các câu đã hỏi trong phiên, **để** đào sâu tư vấn nhanh hơn mà không phải gõ lại.
- Ưu tiên: **Should** · Yêu cầu: FR3.4, FR3.5
- **AC1.3.1**: *Given* Copilot vừa trả lời, *When* câu trả lời hiển thị, *Then* bên dưới có 2–4 câu hỏi gợi ý. Bấm vào một câu thì câu đó được gửi đi như câu hỏi mới.
- **AC1.3.2**: *Given* RM đã hỏi nhiều câu trong phiên, *When* RM xem cột "Lịch sử tra cứu", *Then* các câu đã hỏi được liệt kê từ mới đến cũ, bấm vào câu nào thì mở lại câu trả lời của câu đó.
- **AC1.3.3**: *Given* RM bấm "Cuộc trò chuyện mới", *When* RM hỏi câu tiếp theo, *Then* câu trả lời không dùng ngữ cảnh của cuộc trò chuyện trước.

---

## US2 — Kết quả cuộc gọi (US-02)

### US2.1 — Xem kết quả và phân tích hội thoại
**Là** RM, **tôi muốn** ngay sau cuộc gọi thấy tiến trình xử lý, nghe lại ghi âm, đọc transcript tách lượt RM/KH và bản tóm tắt AI, **để** rà soát cuộc gọi mà không phải nghe lại toàn bộ.
- Ưu tiên: **Must** · Yêu cầu: FR4.1, FR4.2, FR4.3, NFR1, NFR6 · Mockup: `3-ket-qua-cuoc-goi.html`
- **AC2.1.1**: *Given* cuộc gọi vừa kết thúc, *When* màn "Kết quả cuộc gọi" mở, *Then* màn hiển thị lần lượt các trạng thái "đang lấy ghi âm", "đang chuyển giọng nói", "đang phân tích AI" và tự cập nhật, không cần tải lại trang.
- **AC2.1.2**: *Given* đã có ghi âm và STT thành công, *When* xem cột 2, *Then* có trình phát audio (phát/tạm dừng, tua, đổi tốc độ), transcript tách lượt RM/KH kèm mốc thời gian, và nút tải `.wav`.
- **AC2.1.3**: *Given* có file ghi âm của một cuộc gọi dài tối đa 5 phút, *When* pipeline chạy, *Then* transcript và tóm tắt AI hiện ra trong vòng ≤ 120 giây.
- **AC2.1.4**: *Given* pipeline đã chạy xong, *When* xem log, *Then* mỗi bước (lấy ghi âm, STT, LLM, lưu) đều có dòng log ghi mã cuộc gọi, thời gian chạy và kết quả.

### US2.2 — AI chấm điểm cuộc gọi
**Là** RM, **tôi muốn** thấy điểm cuộc gọi, điểm từng tiêu chí kỹ năng, cảm xúc KH và nhận xét AI, **để** biết mình làm tốt điều gì và cần cải thiện điều gì.
- Ưu tiên: **Must** · Yêu cầu: FR4.4 · Mockup: `4-lua-chon-dieu-huong.html` (cột 1)
- **AC2.2.1**: *Given* LLM trả về điểm 10; 10; 9,5; 9; 9,5 cho 5 tiêu chí, *When* hiển thị cột 1, *Then* điểm tổng là 96/100, hạng A, và từng tiêu chí hiển thị điểm /10 kèm thanh tiến độ.
- **AC2.2.2**: *Given* đã có kết quả AI, *When* xem cột 1, *Then* có thẻ KH, giọng điệu cuộc gọi (Tích cực / Trung tính / Tiêu cực), một điểm sáng nổi bật và một gợi ý nâng cao.
- **AC2.2.3**: *Given* LLM trả về điểm một tiêu chí nằm ngoài khoảng 0–10, *When* hệ thống kiểm tra, *Then* kết quả bị từ chối và được chạy lại một lần. Nếu lần chạy lại vẫn lỗi, màn hiển thị "Chưa có điểm AI".
- **AC2.2.4**: *Given* điểm tổng tính được là 84, *When* xếp hạng, *Then* hạng là B (biên 85 thuộc hạng A, 70–84 thuộc hạng B).

### US2.3 — AI Auto Keyin, đánh giá AI và lưu hồ sơ
**Là** RM, **tôi muốn** form keyin được AI điền sẵn để tôi chỉ cần sửa, đánh giá chất lượng AI rồi lưu, **để** giảm thời gian keyin và không bỏ sót thông tin.
- Ưu tiên: **Must** · Yêu cầu: FR4.5, FR4.6, FR4.7 · Mockup: `4-lua-chon-dieu-huong.html` (cột 3)
- **AC2.3.1**: *Given* transcript có nhắc "500 triệu", "12 tháng", "5,8%", *When* AI điền form, *Then* các trường Nhu cầu/số tiền, Kỳ hạn và Lãi suất chốt có giá trị tương ứng. Mỗi trường mang dấu "AI" kèm mốc thời gian nơi trích xuất.
- **AC2.3.2**: *Given* transcript không nhắc tới kỳ hạn, *When* AI điền form, *Then* trường Kỳ hạn để trống.
- **AC2.3.3**: *Given* RM sửa một trường do AI điền, *When* RM sửa xong, *Then* dấu của trường đổi thành "RM đã sửa". Khi lưu, hệ thống ghi lại cho từng trường việc giữ nguyên hay đã sửa.
- **AC2.3.4**: *Given* RM đã chọn điểm đánh giá AI (1–10), các thẻ nhận xét và góp ý, *When* RM bấm "Lưu hồ sơ cuộc gọi", *Then* hệ thống lưu keyin, tóm tắt/gợi ý AI vào hoạt động cuộc gọi, đánh giá AI, cập nhật giai đoạn lead, và tạo một công việc cho mỗi mục "Công việc tiếp theo".
- **AC2.3.5**: *Given* còn trống trường bắt buộc (Giai đoạn lead hoặc Kết quả cuộc gọi), *When* RM bấm Lưu, *Then* màn chỉ ra trường còn thiếu, giữ nguyên dữ liệu đã nhập và không lưu.
- **AC2.3.6**: *Given* RM bấm Lưu hai lần liên tiếp, *When* hệ thống xử lý, *Then* chỉ có một bản keyin và công việc không bị tạo trùng.

### US2.4 — Dự phòng khi lỗi và điều hướng sau khi lưu
**Là** RM, **tôi muốn** vẫn keyin được khi không có ghi âm hoặc AI lỗi, và sau khi lưu được chọn bước tiếp theo, **để** công việc không bao giờ bị chặn và tôi chuyển tiếp nhanh.
- Ưu tiên: **Must** · Yêu cầu: FR4.8, FR4.9, NFR3 · Mockup: `4-lua-chon-dieu-huong.html` (hộp thoại)
- **AC2.4.1**: *Given* sau thời gian chờ có giới hạn vẫn không có ghi âm, *When* pipeline dừng, *Then* cột 1 hiển thị "Chưa có điểm AI — không có ghi âm", cột 2 báo "Không tìm thấy ghi âm cho cuộc gọi này", form trống và vẫn lưu được.
- **AC2.4.2**: *Given* có ghi âm nhưng STT lỗi, *When* mở màn, *Then* vẫn có trình phát và nút tải `.wav`, kèm thông báo "Không chuyển được giọng nói thành văn bản", form trống.
- **AC2.4.3**: *Given* có transcript nhưng LLM lỗi, *When* RM bấm "Thử phân tích AI lại" và lần này thành công, *Then* hệ thống chỉ điền vào các trường RM chưa nhập.
- **AC2.4.4**: *Given* lưu thành công, *When* hộp thoại hiện ra, *Then* có 3 lựa chọn: "Liên hệ khách hàng tiếp theo", "Chi tiết lịch làm việc", "Đóng & Quay lại trang chủ". "Chi tiết lịch làm việc" mở màn Lịch làm việc (US3.2).
- **AC2.4.5**: *Given* RM không còn lead nào ở trạng thái Chưa tiếp cận hoặc Đang tiếp cận, *When* RM chọn "Liên hệ khách hàng tiếp theo", *Then* hệ thống báo "Bạn đã liên hệ hết tiềm năng bán cần xử lý" và mở danh sách tiềm năng bán.

---

## US3 — Lịch sử hoạt động và Lịch làm việc (US-03)

### US3.1 — Xem tóm tắt AI trong lịch sử hoạt động
**Là** RM, **tôi muốn** thấy tóm tắt và gợi ý AI của từng cuộc gọi trong lịch sử hoạt động của lead, **để** nhớ lại bối cảnh trước khi gọi lần sau.
- Ưu tiên: **Must** · Yêu cầu: FR5.1 · Mockup: `1-chi-tiet-tiem-nang-ban.html` (Quản lý hoạt động)
- **AC3.1.1**: *Given* lead có một cuộc gọi đã lưu kèm tóm tắt AI, *When* RM xem "Quản lý hoạt động", *Then* hoạt động cuộc gọi hiển thị tóm tắt và gợi ý AI ở dạng chỉ đọc, có thể thu gọn hoặc mở rộng.
- **AC3.1.2**: *Given* hoạt động cuộc gọi đó, *When* RM bấm liên kết "Xem kết quả cuộc gọi", *Then* màn Kết quả cuộc gọi của đúng cuộc gọi đó mở ra.
- **AC3.1.3**: *Given* cuộc gọi được lưu trong trường hợp AI lỗi, *When* xem lịch sử, *Then* hoạt động vẫn hiển thị keyin RM đã nhập, kèm dòng "Không có tóm tắt AI".

### US3.2 — Xem lịch làm việc tuần
**Là** RM, **tôi muốn** xem toàn bộ công việc đã làm và sắp tới trên lịch tuần, nổi bật các việc vừa tạo từ cuộc gọi, **để** sắp xếp thời gian và không quên hẹn với KH.
- Ưu tiên: **Must** · Yêu cầu: FR5.2, FR5.3 · Mockup: `5-lich-lam-viec.html`
- **AC3.2.1**: *Given* RM vừa lưu keyin có 2 công việc tiếp theo, *When* RM mở lịch từ hộp thoại, *Then* panel trái liệt kê 2 việc đó trong khối "AI Key-in từ cuộc gọi" (kèm ưu tiên, giờ, sản phẩm, giá trị, tóm tắt AI), và trên lịch tuần chúng nằm đúng ngày giờ với nhãn "Vừa lưu từ cuộc gọi".
- **AC3.2.2**: *Given* RM có công việc ở nhiều lead và cả công việc không gắn lead, *When* xem lịch, *Then* tất cả công việc của RM đều hiển thị, cả việc đã làm lẫn việc sắp tới.
- **AC3.2.3**: *Given* RM chọn bộ lọc "Nhắc gọi", *When* lọc, *Then* lịch chỉ còn công việc thuộc loại Nhắc gọi.
- **AC3.2.4**: *Given* tuần đang xem không có công việc nào, *When* chuyển tới tuần đó, *Then* lịch báo "Không có công việc trong tuần này".
- **AC3.2.5**: *Given* hôm nay còn công việc chưa tới giờ, *When* xem panel trái, *Then* khối "Lịch trình còn lại trong ngày" liệt kê các việc đó theo giờ.

### US3.3 — Thao tác công việc trên lịch
**Là** RM, **tôi muốn** đánh dấu xong, đổi giờ, gọi ngay hoặc tạo công việc mới ngay trên lịch, **để** cập nhật kế hoạch mà không phải đi qua nhiều màn.
- Ưu tiên: **Must** · Yêu cầu: FR5.4
- **AC3.3.1**: *Given* một công việc chưa xong, *When* RM bấm **Đã xong**, *Then* công việc chuyển sang trạng thái đã hoàn thành và hiển thị là đã làm.
- **AC3.3.2**: *Given* RM bấm **Đổi giờ** và chọn một thời điểm trong tương lai, *When* lưu, *Then* công việc chuyển sang ô giờ mới trên lịch.
- **AC3.3.3**: *Given* RM đổi giờ sang thời điểm đã qua, *When* lưu, *Then* hệ thống cảnh báo và chỉ lưu khi RM xác nhận.
- **AC3.3.4**: *Given* một công việc gắn với lead, *When* RM bấm **Gọi ngay**, *Then* màn Thực hiện cuộc gọi của lead đó mở ra.
- **AC3.3.5**: *Given* RM bấm "Tạo công việc mới" và bỏ trống tiêu đề, *When* lưu, *Then* màn báo thiếu tiêu đề và không tạo công việc.

### US3.4 — Chuyển chế độ xem lịch
**Là** RM, **tôi muốn** chuyển lịch giữa các chế độ Ngày, Tuần, Tháng và Danh sách, **để** xem kế hoạch ở độ chi tiết phù hợp.
- Ưu tiên: **Should** · Yêu cầu: FR5.5
- **AC3.4.1**: *Given* lịch đang ở chế độ Tuần (mặc định), *When* RM chọn "Tháng", *Then* lịch hiển thị các công việc của cả tháng.
- **AC3.4.2**: *Given* RM chọn "Danh sách", *When* xem, *Then* công việc được liệt kê theo thời gian, tách thành hai phần "đã làm" và "sắp tới".

---

## US4 — Nền tảng RM (ưu tiên cuối, làm sau cùng [Q1])

### US4.1 — Đăng nhập
**Là** RM, **tôi muốn** đăng nhập bằng tài khoản của mình, **để** chỉ mình tôi xem và keyin được các lead của tôi.
- Ưu tiên: **Could** · Yêu cầu: FR1.1, NFR2
- **AC4.1.1**: *Given* một tài khoản RM mẫu hợp lệ, *When* RM đăng nhập đúng mật khẩu, *Then* RM vào được danh sách lead của mình.
- **AC4.1.2**: *Given* mật khẩu sai, *When* RM đăng nhập, *Then* màn báo "Sai tên đăng nhập hoặc mật khẩu" và không tạo phiên.
- **AC4.1.3**: *Given* phiên đăng nhập đã hết hạn, *When* RM thao tác, *Then* RM được đưa về màn đăng nhập. Mật khẩu chỉ được lưu dưới dạng băm, không lưu dạng thô.

### US4.2 — Danh sách lead của tôi
**Là** RM, **tôi muốn** thấy danh sách tiềm năng bán mình phụ trách, có ưu tiên và trạng thái, **để** chọn KH cần gọi.
- Ưu tiên: **Could** · Yêu cầu: FR1.2, FR1.4
- **AC4.2.1**: *Given* cả RM A và RM B đều có lead, *When* RM A mở danh sách, *Then* không có lead nào của RM B.
- **AC4.2.2**: *Given* RM A gọi API lấy lead của RM B, *When* server xử lý, *Then* trả về 403 và dữ liệu không đổi.
- **AC4.2.3**: *Given* RM chưa có lead nào, *When* mở danh sách, *Then* màn hiển thị "Chưa có tiềm năng bán".

### US4.3 — Chi tiết lead
**Là** RM, **tôi muốn** xem thông tin KH, giai đoạn lead, nội dung tiềm năng và kịch bản bán, **để** chuẩn bị trước khi gọi.
- Ưu tiên: **Could** · Yêu cầu: FR1.3 · Mockup: `1-chi-tiet-tiem-nang-ban.html`
- **AC4.3.1**: *Given* một lead của RM, *When* mở chi tiết, *Then* màn hiển thị thông tin KH, thanh tiến trình giai đoạn (Chưa tiếp cận → Đang tiếp cận → Chuyển cơ hội / Từ chối), khối Nội dung, khối Kịch bản bán và Quản lý hoạt động.
- **AC4.3.2**: *Given* lead chưa có hoạt động nào, *When* xem Quản lý hoạt động, *Then* màn hiển thị "Chưa có hoạt động".

### US4.4 — Gọi KH từ chi tiết lead
**Là** RM, **tôi muốn** bấm gọi KH ngay từ chi tiết lead, **để** cuộc gọi tự gắn với lead và với tôi.
- Ưu tiên: **Could** · Yêu cầu: FR1.5
- **AC4.4.1**: *Given* lead có số điện thoại hợp lệ, *When* RM bấm **Gọi KH**, *Then* hệ thống tạo bản ghi cuộc gọi gắn với lead và RM, cuộc gọi bắt đầu qua MicroSIP/softphone và màn Thực hiện cuộc gọi mở ra.
- **AC4.4.2**: *Given* số điện thoại không hợp lệ, *When* RM bấm Gọi KH, *Then* màn báo lỗi và không tạo cuộc gọi.

---

## Phụ thuộc giữa các story

- US2.1 → US2.2, US2.3: chấm điểm và auto keyin dùng kết quả pipeline của US2.1.
- US2.3 → US2.4 (điều hướng sau khi lưu), US3.1 (tóm tắt trong lịch sử), US3.2 (công việc vừa tạo).
- US1.1 → US2.1: kết thúc cuộc gọi kích hoạt pipeline.
- US4.x được làm sau cùng. Trước khi có US4, các story khác chạy với RM mẫu và lead mẫu (xem phần "Cách demo độc lập").

## Ghi chú INVEST

- Mỗi story có thể demo độc lập với dữ liệu seed, đúng tinh thần "mỗi US là một lát cắt demo/nghiệm thu độc lập" của epic mục 4.2.
- Story đã khớp kích thước mức vừa cho US2 theo [Q3]. Story lớn nhất là US2.3, có 6 tiêu chí nghiệm thu.
- Mọi tiêu chí nghiệm thu đều kiểm thử tự động được. Các lời gọi STT/LLM sẽ được giả lập (mock) để đạt độ phủ 80% theo NFR4.

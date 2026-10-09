# Refined Mockups — CRM VoIP Call Assistant (RM Flow)

Design source: BA Figma (https://www.figma.com/design/ObEPaidO5YFTYB5kxk5hju/Untitled?node-id=0-1),
content supplied by the user for the 3 RM screens [Q1]. Product: MB (Military
Bank) RM lead-management + AI call assistant. UI language: Vietnamese. The 2
manager results views are deferred to a later pass [Q2].

Scope note: the live in-call AI copilot (S2) and the AI Sales Performance
scorecard (S3) are in-scope, real LLM-backed features; surrounding CRM data
(customer, lead, interaction history, policy knowledge) is mock data [Q3].

## Design Language

- Vietnamese throughout; MB brand palette (deep navy/blue primary with an accent
  highlight), white card surfaces on a light background [Q3].
- Card-based CRM layout; left/right panel split on wide screens.
- Status + priority badges: `⚡ Khai thác ngay`, `🔥 Hot Lead`, lead-rating
  `N/10`; lead statuses `Chưa tiếp cận` / `Đang tiếp cận` / `Chuyển cơ hội` /
  `Từ chối` [Q1].
- Primary brand font per Figma (fallback: system sans). Monetary values in
  Vietnamese convention (`tr.đ`, `VNĐ`).

## Screen S1 — Lead / Customer Detail (`Thông tin chi tiết`)

Maps to FR1.1 (clicking the customer phone number starts the call) [requirements:FR1.1].

```
+--------------------------------------------------------------+
| < QUAY LẠI        Thông tin chi tiết                         |
+-------------------------+------------------------------------+
| Customer header         | Danh sách tiềm năng bán (list)     |
|  10432052               |  L120825003485 - Tiền gửi tiết     |
|  TRAN THI PHUONG HIEN   |  kiệm dài hạn - Sinh nhật          |
|  INDIV-MASS POTENTIAL   |  [⚡ Khai thác ngay] [🔥 Hot Lead] |
|  (phone) 0964040001 ◄── click to CALL (FR1.1)                |
|  Mức độ ưu tiên: ...    |  Each item: Nguồn, SLA còn lại,    |
|                         |  Điểm xếp hạng N/10, status badge  |
| Quản lý hoạt động       +------------------------------------+
|  (activity timeline)    | Detail tabs:                       |
|  • Chưa tiếp cận        |  1. Thông tin tiềm năng bán        |
|  • Đang tiếp cận        |  2. Thông tin sản phẩm             |
|  • Chuyển cơ hội        |  3. Dư nợ                          |
|  • Từ chối              |  (Trigger loại, số lượng SP, giá   |
|  Bộ lọc (filter)        |   trị, CASA BQ, dư nợ, tiết kiệm)  |
|                         |                                    |
|                         | KỊCH BẢN BÁN (sales script)        |
|                         |  1. Dấu hiệu                       |
|                         |  2. Kịch bản bán (greeting + offer)|
|                         | Đánh giá chất lượng Lead           |
+-------------------------+------------------------------------+
```

<!-- Text fallback: S1 is a two-panel lead-detail screen. Left: customer header
(id, name, segment, clickable phone that starts the call, priority), and an
activity-management timeline with status entries and a filter. Right: a
sales-potential list (each with source, SLA, rating, status badge), detail tabs
(potential/product/debt), a sales-script block, and a lead-quality rating. -->

- **Primary action**: clicking the customer phone number initiates the call via
  MicroSIP (FR1.1) and transitions to S2.
- Mocked CRM data populates the lead list, product/debt panels, and timeline
  (FR7.3).

## Screen S2 — In-Call with AI Sales Knowledge Copilot (`Thực hiện cuộc gọi`)

```
+--------------------------------------------------------------+
| < Quay lại   Thực hiện cuộc gọi                              |
|  TNB: L120825003485 - Nguyễn Phương Thảo                     |
|  [Đang gọi  03:25]                         [ Kết thúc ]  ◄── end call (FR1.3)
+----------------------+---------------------------------------+
| THÔNG TIN TIỀM NĂNG  | AI Sales Knowledge  • Copilot Active  |
|  Khách hàng Cá nhân  |  (Trợ lý tra cứu chính sách & sản phẩm)|
|  PT Nguyễn Phương    |  [ Chat thread ]                      |
|  Thảo  VIP INDIV-MASS|  Q: "Lãi suất tiền gửi 12 tháng...?"  |
|  0361 624 367 | HN   |  A: (rate table: kỳ hạn / cơ bản / VIP)|
|  Tuổi 34, NV văn phòng|  Nguồn: MB Policy Center v3.2        |
|  Thu nhập ~40tr/thg  |                                       |
|                      | GỢI Ý CÂU HỎI TIẾP THEO               |
| TIỀM NĂNG BÁN ĐANG   |  • Tất toán trước hạn thì lãi suất... |
| KÍCH HOẠT            |  • Có sản phẩm nào lãi suất cao hơn...|
|  Hot Lead (9/10)     |  • Điều kiện trở thành khách VIP?     |
|  Sản phẩm quan tâm,  |                                       |
|  nguồn, ngày tạo,    | Quick Actions: Ghi chú | Tạo hoạt     |
|  trạng thái          |  động | Tạo cơ hội | Công việc        |
|                      |                                       |
| Lịch sử tương tác    | Ghi chú nhanh trong cuộc gọi:         |
|  (3 sự kiện)         |  [ textarea ] [ Lưu Note ] (auto-CRM) |
+----------------------+---------------------------------------+
```

<!-- Text fallback: S2 is the in-call screen. Top bar shows the lead, a live
call timer, and an End button. Left panel: customer info and the active sales
potential plus recent interaction history. Right panel: a live AI copilot chat
answering MB policy/product/rate questions (grounded in mock policy data),
AI-suggested next questions, quick actions, and a quick-note box that saves to
CRM. -->

- **AI copilot** (in scope, LLM-backed): answers RM policy/product/rate
  questions during the call, grounded in mocked MB policy knowledge [Q3].
- **End call** (`Kết thúc`): the RM marks the call ended; this triggers the
  post-call pipeline (recording download → Vietnamese STT → AI) and navigates to
  S3 when results are ready (FR1.3) [requirements:FR1.3].
- AI-suggested follow-up questions update as the conversation proceeds.

## Screen S3 — Post-Call Result (`Kết quả cuộc gọi`) — core demo screen

Maps to FR4 (transcript w/ speaker turns), FR5 (summary/form/suggestion),
FR6.2/6.3 (result view), FR3 (.wav download) [requirements].

```
+--------------------------------------------------------------+
| < Quay lại   Kết quả cuộc gọi      [Đã kết nối]              |
|  Mã TNB L120825003485 | 08/10/2026 10:32 | Thời lượng 08:45  |
|  RM: Hoàng Yến Chibi | KH: Nguyễn Phương Thảo VIP 🔥 9/10    |
+----------------------+-----------------+---------------------+
| PHÂN TÍCH HỘI THOẠI  | AI AUTO KEYIN   | AI SALES PERFORMANCE|
| AI Voice-to-Text     | (8 CRM fields   |  Score 92/100       |
| [audio player 1.0x   |  auto-extracted)|  Tone: Tích cực     |
|  01:15 / 04:18]      | • Nhu cầu gửi:  |  Tốc độ 145 từ/phút |
|                      |   500.000.000 ₫ |  Ngắt quãng 1.2s    |
| RM Yến  00:15        |   (từ 00:45)    | CHI TIẾT TIÊU CHÍ:  |
|  "Em chào chị Thảo.."| • Kỳ hạn: 12T   |  1. Chào hỏi 10/10  |
| KH Thảo 00:45        | • Lãi suất:5.8% |  2. Khai thác 10/10 |
|  "Chào em, chị đang  | • Sản phẩm chốt |  3. Chính sách 9.5  |
|   có 500 triệu.."    | • Nguồn tiền    |  4. Xử lý 9.0       |
| RM Yến  01:20 (▶play)| • Thời gian dự  |  5. Chốt 9.5        |
| KH Thảo 02:15        |   kiến          +---------------------+
| ... (RM/KH turns     | • Kênh: App MB  | AI INSIGHT          |
|  with timestamps)    |                 | 💡 Điểm sáng...     |
|                      | [Sửa thủ công]  | 🎯 Cross-sell...    |
|                      | [Đồng bộ CRM]   | Next Sales Action:  |
|                      |                 |  • Gửi SMS/Zalo...  |
|                      | Ghi chú RM:     |  • Gọi nhắc T2 09:00|
|                      | [textarea][Lưu] |                     |
+----------------------+-----------------+---------------------+
| Đánh giá độ chính xác [1..10]   [Lưu hồ sơ] [Tải file .wav]  |
+--------------------------------------------------------------+
```

<!-- Text fallback: S3 is the post-call result. Three columns. Left: the
Vietnamese transcript (AI voice-to-text) with an audio player and turns labeled
RM vs KH (customer) with timestamps. Middle: AI auto-keyin — 8 CRM fields
extracted from the call, each with its extraction time, plus manual-edit and
CRM-sync actions and an RM note box. Right: AI Sales Performance scorecard
(overall score, tone, pace, per-criterion scores) and AI Insight with highlights,
cross-sell, and auto-created Next Sales Actions. Footer: accuracy rating, save
record, and download .wav. -->

- **Transcript** (FR4): Vietnamese, each turn labeled RM / KH from per-leg
  recording; clicking a turn seeks the audio player.
- **AI auto-keyin** (FR5.2): 8 CRM fields extracted from the transcript, each
  showing its source timestamp; `Sửa thủ công` lets the RM edit before `Đồng bộ
  CRM`.
- **AI Sales Performance** (in scope): overall score + per-criterion breakdown,
  tone, pace — computed by the LLM over the transcript [Q3].
- **AI Insight / Next Sales Action** (FR5.3): highlights, cross-sell suggestion,
  and auto-created follow-up tasks = the recommended selling scenario.
- **`.wav` download** (FR3): downloads the call recording from the backend.

## Screen States (applied to each screen)

- **Loading / processing** (S3 after `Kết thúc`): a processing state while the
  recording download → STT → AI pipeline runs ("Đang phân tích cuộc gọi…"),
  since results appear only when ready (FR1.3).
- **Empty**: S1 lead list with no potentials shows a guidance message.
- **Error**: pipeline failure shows a plain Vietnamese error with a retry
  (demo-level; failure handling accepted knowingly per requirements R-05).
- **Success / populated**: the states drawn above.
- **Partial**: long transcripts scroll within the transcript column; long field
  values wrap without breaking the card layout.

## Assumptions & Open Questions

- The 2 manager results views are not specced yet; they are a later pass [Q2][assumption].
- Exact MB hex colors and typography come from the Figma file; the specs name
  roles (primary/accent/surface) to be bound to the Figma tokens at build
  [Q1][assumption].

# Interaction Specification — CRM VoIP Call Assistant (RM Flow)

Component-level specs for the 3 RM screens, following the component-spec
template. Vietnamese UI, MB design language [mockups][Q3].

## CallTriggerPhoneLink (S1)

| Field | Value |
|---|---|
| Component | CallTriggerPhoneLink |
| Description | Customer phone number that starts a MicroSIP call when clicked |
| Category | navigation / action |

### States
| State | Description | Trigger |
|---|---|---|
| default | Phone number shown as an actionable link | page load |
| hover | Underline + call icon | mouseover |
| focus | Visible focus ring | Tab |
| loading | "Đang khởi tạo cuộc gọi…" while MicroSIP dials | click → backend |
| error | Inline error if the call cannot start | dial failure |

### Accessibility
| Requirement | Implementation |
|---|---|
| ARIA role | button (actionable), `aria-label="Gọi <tên khách hàng>"` |
| Keyboard | Enter/Space activates |
| Contrast | WCAG AA 4.5:1 |
| Screen reader | Announces "Gọi <customer>, <phone>" |

## CopilotChat (S2)

| Field | Value |
|---|---|
| Component | CopilotChat |
| Description | Live AI Q&A over MB policy/product/rate knowledge during the call |
| Category | input / display (feedback) |

### States
| State | Description | Trigger |
|---|---|---|
| default | Empty thread + prompt placeholder | S2 load |
| loading | Streaming/typing indicator while LLM answers | question submitted |
| success | Answer rendered (may include a rate table) with source line | LLM returns |
| error | "Không truy vấn được, thử lại" with retry | LLM/API failure |
| empty | Suggested starter questions shown | no messages yet |

### Accessibility
| Requirement | Implementation |
|---|---|
| ARIA | `aria-live="polite"` on the answer region |
| Keyboard | Enter submits; Shift+Enter newline; results reachable by Tab |
| Screen reader | New answers announced politely |

## CallTimerEndControl (S2)

| Field | Value |
|---|---|
| Component | CallTimerEndControl |
| Description | Shows elapsed call time and the End-call action |
| Category | feedback / action |

### States
| State | Description | Trigger |
|---|---|---|
| default | `Đang gọi` + MM:SS counting up | call connected |
| ending | Button shows pending state | `Kết thúc` clicked |

Behavior: `Kết thúc` marks the call ended (FR1.3), kicks off the post-call
pipeline, and routes to S3's processing state. Destructive-ish action → a brief
confirm or an undo window is acceptable; for the demo a single click is fine.

## TranscriptPanel (S3)

| Field | Value |
|---|---|
| Component | TranscriptPanel |
| Description | Vietnamese transcript with RM/KH speaker turns + synced audio player |
| Category | display |

### States
| State | Description | Trigger |
|---|---|---|
| processing | "Đang phân tích cuộc gọi…" skeleton | pipeline running (FR1.3) |
| success | Turns labeled RM / KH with timestamps | STT complete |
| playing | Current turn highlighted; audio position synced | play / seek |
| error | Error + retry | STT failure (demo R-05) |

### Accessibility
| Requirement | Implementation |
|---|---|
| Speaker label | Each turn names the speaker in text (not color-only): "RM" / "KH" |
| Audio controls | Native `<audio>`; keyboard play/pause/seek |
| Focus | Clicking/activating a turn seeks audio and keeps focus on the turn |

## AutoKeyinForm (S3)

| Field | Value |
|---|---|
| Component | AutoKeyinForm |
| Description | 8 CRM fields auto-extracted from the call; editable before CRM sync |
| Category | input |

### States
| State | Description | Trigger |
|---|---|---|
| extracting | Field skeletons while LLM extracts | pipeline running |
| populated | Fields filled, each with source timestamp | extraction complete |
| editing | `Sửa thủ công` → fields become editable | user edits |
| syncing / synced | `Đồng bộ CRM` persists; "Đã đồng bộ" | sync action |
| error | Field-level validation / sync error | invalid or failed sync |

### Accessibility
| Requirement | Implementation |
|---|---|
| Labels | Visible label above each field (not placeholder-only) |
| Validation | Specific inline messages ("Số tiền không hợp lệ") |
| Keyboard | Full keyboard editing and submit |

## SalesPerformanceCard + AIInsight (S3)

| Field | Value |
|---|---|
| Component | SalesPerformanceCard / AIInsightCard |
| Description | LLM-computed call scorecard; highlights, cross-sell, Next Sales Actions |
| Category | display |

### States
| State | Description | Trigger |
|---|---|---|
| computing | Skeleton while LLM scores the call | pipeline running |
| success | Overall score, per-criterion rows, insight, auto-created tasks | LLM returns |
| error | "Không chấm điểm được" + retry | LLM failure |

Behavior: Next Sales Actions render as a short task list (the recommended
selling scenario, FR5.3). Scores are not color-only (numeric + label).

## WavDownload (S3)

| Field | Value |
|---|---|
| Component | WavDownload |
| Description | Downloads the call recording `.wav` from the backend (FR3) |
| Category | action |

### States
| State | Description | Trigger |
|---|---|---|
| default | `Tải file audio (.wav)` button | S3 load |
| loading | Pending while backend fetches from EC2 | click |
| error | "Không tải được bản ghi" | recording not found / fetch error (FR3.2) |

## Responsive Behavior

| Breakpoint | Behavior |
|---|---|
| desktop (>1024px) | S1 two-panel; S2 two-panel; S3 three-column as drawn |
| tablet (768–1024px) | S3 collapses to two columns (transcript full width above form + insight) |
| mobile (<768px) | All panels stack vertically; transcript, then form, then performance/insight; sticky call controls on S2 |

## Global Interaction Rules

- Primary action per screen section is singular and prominent (brand accent).
- Loading uses skeletons in-context (not full-page blocks); the S3 processing
  state is explicit because results appear only when ready (FR1.3).
- Errors are inline, with icon + text (never color-only), and offer retry.

## Assumptions & Open Questions

- Exact animation timings and micro-interactions follow the Figma; defaults are
  <300ms with `prefers-reduced-motion` respected [assumption].

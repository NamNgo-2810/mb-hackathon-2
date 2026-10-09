# Design System Mapping — CRM VoIP Call Assistant

Maps the MB Figma design language to implementable UI tokens and components. Hex
values and exact type come from the Figma file; roles below are bound to Figma
tokens at build [mockups][Q1][Q3].

## Design Tokens (roles)

| Token role | Usage | Source |
|---|---|---|
| color.primary | MB brand deep navy/blue — headers, primary actions | Figma (bind) |
| color.accent | Highlight for active state, score, CTA | Figma (bind) |
| color.surface | White card surfaces | Figma |
| color.background | Light app background | Figma |
| color.status.notContacted | `Chưa tiếp cận` badge | Figma |
| color.status.contacting | `Đang tiếp cận` badge | Figma |
| color.status.opportunity | `Chuyển cơ hội` badge | Figma |
| color.status.rejected | `Từ chối` badge | Figma |
| color.priority.hot | `🔥 Hot Lead` | Figma |
| color.priority.now | `⚡ Khai thác ngay` | Figma |
| typography.heading / body | Section titles / content | Figma (fallback system sans) |
| spacing scale | 4 / 8 / 16 / 24 / 32 / 48 px | design-agent wireframing-guide |
| radius / shadow | Card corner radius + elevation | Figma |

## Component Library Mapping

| UI element | Component | Screens |
|---|---|---|
| Lead/potential card | PotentialCard (badges, SLA, rating N/10, status) | S1, S2 |
| Status badge | StatusBadge (4 lead statuses, non-color-only) | S1, S2 |
| Priority badge | PriorityBadge (🔥 / ⚡) | S1, S2 |
| Phone call trigger | CallTriggerPhoneLink | S1 |
| Activity timeline | ActivityTimeline | S1 |
| Sales script block | SalesScriptPanel | S1 |
| Call timer + end | CallTimerEndControl | S2 |
| AI copilot chat | CopilotChat | S2 |
| Suggested questions | SuggestedQuestionList | S2 |
| Quick note | QuickNoteBox (auto-save to CRM) | S2, S3 |
| Transcript w/ turns | TranscriptPanel + AudioPlayer | S3 |
| Auto-filled form | AutoKeyinForm (8 fields, editable) | S3 |
| Performance scorecard | SalesPerformanceCard | S3 |
| AI insight / next action | AIInsightCard | S3 |
| Download recording | WavDownload | S3 |
| Rating control | AccuracyRating (1–10) | S3 |

## Naming / Code Style

- Component names PascalCase; follows the project's language-idiomatic
  conventions (org `## Code Style` defers to project linter) [memory:org Code Style].
- The existing demo is Node/static HTML; the new UI is a fresh implementation
  (framework chosen at code-generation) matching these component roles.

## Assumptions & Open Questions

- Exact token values are bound from the Figma file during implementation; this
  mapping names the roles so the build and the design share one vocabulary
  [assumption].

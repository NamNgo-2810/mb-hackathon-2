# Intent Backlog — CRM VoIP Call Assistant

Prioritized proto-Units (MoSCoW). Ordered dependency-first per the agreed
sequencing [Q2]. These are coarse capability slices for a one-day build, not
final Units (Units Generation is skipped for this plan).

## Must Have

| ID | Proto-Unit | Description | Depends on | Source |
|----|-----------|-------------|-----------|--------|
| B1 | Call + recording capture | Reuse existing FreeSWITCH/softphone call; configure per-leg recording and call→recording mapping | — | [S1][S3][S4] |
| B2 | Recording download API | Spring Boot endpoint to fetch a call's `.wav`(s) from the EC2 server | B1 | [S2] |
| B3 | Vietnamese speech-to-text | Transcribe the recording with caller/callee separation | B2 | [S5] |
| B4 | AI smart tasks | LLM produces call summary, auto-filled form fields, and a recommended selling scenario from the transcript | B3 | [S6] |
| B5 | Data model + local DB | Database design; DB running in Docker; persist calls, transcripts, form data, suggestions (mock-friendly) | — | [S7][S9] |
| B6 | Figma-matched UI | Web UI implementing the BA's Figma design: initiate call, show summary/form/suggestion, review/edit | B2, B4, B5 | [S8] |

## Should Have

| ID | Proto-Unit | Description | Source |
|----|-----------|-------------|--------|
| B7 | Mock data seeding | Seed convincing customer/call data so the UI looks complete; `phone_number` from FreeSWITCH default users | [S9] |

## Could Have

| ID | Proto-Unit | Description | Source |
|----|-----------|-------------|--------|
| B8 | Visual polish | Extra animation/refinement beyond the Figma match | [Q1] |

## Won't Have (this time)

- Real auth, production deploy/CI/monitoring, external CRM integration, TURN
  server, and multi-language STT [S:out-of-scope].

## Assumptions & Open Questions

- The Vietnamese STT provider is not yet chosen; B3 carries that open decision
  into the speech-to-text design/implementation step [assumption].

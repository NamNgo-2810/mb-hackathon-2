# Code Generation Plan — Core Slice (Call → Recording Download → Vietnamese STT)

## Summary

- Builds: a Spring Boot backend that downloads the per-call recording `.wav` from the EC2/FreeSWITCH server and runs Vietnamese speech-to-text with caller/callee separation; the call itself is triggered from the frontend via MicroSIP (reusing the existing Node `/api/microsip/call` route). Plus the FreeSWITCH config to map and per-leg-record calls.
- Touches: new `backend/` Spring Boot service (controllers, services, STT client, config, tests), `docker-compose.yml` (local Postgres), `freeswitch/` config snippets + a step-by-step guide, and a minimal web trigger reusing the existing MicroSIP route.
- Tests: none for now (hackathon — no new tests; the existing demo stays untouched/green). Tests can be added tomorrow.
- Frontend: a simple, deliberately change-friendly web page (plain HTML/CSS/JS, no build step) built from what we know so far — a lead/call screen that triggers MicroSIP and a post-call screen stub that shows the transcript and recording — structured so tomorrow's Figma-matched UI can replace it easily.

Scope: the thin end-to-end core only (FR1 call-trigger, FR2 recording mapping/per-leg, FR3 recording-download API, FR4 Vietnamese STT with caller/callee separation). The AI smart tasks (FR5), full Figma UI (FR6), and the copilot/scorecard are deferred to later passes per the user's instruction.

## Testing Contract

```json
{
  "version": 1,
  "methodology": "test-after",
  "source": "org",
  "ordering": "implement each applicable testable layer, then write and run that layer's tests.",
  "scope": "refactor",
  "test_strategy": "standard",
  "project_type": "brownfield",
  "applicable_notes": [
    {
      "layer": "org",
      "text": "We treat tests as a first-class deliverable in every Bolt. The specific\nmethodology (TDD, BDD, ATDD, or classic test-after) is affirmed at\npractices-discovery and recorded in `team.md` under this heading with explicit\n`Methodology` and `Ordering` fields; Code Generation resolves those fields\nindependently from coverage, tooling, and scope notes.\n\nWhen no posture has been affirmed, our default per scope is:\n- **Methodology**: test-after\n- **Ordering**: implement each applicable testable layer, then write and run\n  that layer's tests.\n- `mvp`, `enterprise`, `feature`, `infra`, `classic` add an 80% line-coverage\n  floor and CI execution before merge.\n- `bugfix`, `security-patch` add a targeted regression for the specific\n  bug/vulnerability and require the existing suite to remain green.\n- `express` uses the Minimal strategy: requirement-driven unit tests (one per\n  requirement, with a happy-path floor per component); existing tests remain\n  green.\n- `poc`, `refactor`, `workshop` add no extra new-test floor and require the\n  existing suite to remain green.\n\nThe active `Test Strategy` still applies in every scope and determines test\nvolume/types. Scope floors are additive; they never reduce or replace the\nselected strategy.\n\nBuild and Test verifies defined coverage floors and affirmed quality targets;\nthey may not be weakened to make a step pass.\n\nAffirm a stricter posture in `team.md` if the team commits to one."
    }
  ],
  "obligations": {
    "strategy": "standard",
    "strategy_volume": [
      "Five to eight tests per component.",
      "Unit tests plus integration tests for key boundaries.",
      "Add E2E, performance, or security tests when requirements demand them."
    ],
    "scope_floor": [
      "Keep the existing test suite green.",
      "This scope adds no extra new-test floor beyond the selected test strategy."
    ],
    "combination_rule": "Apply every selected-strategy obligation and every scope-floor obligation; neither replaces the other, and a targeted scope regression may add the narrowest necessary test type beyond the strategy default."
  },
  "plan_profile": {
    "methodology": "test-after",
    "runner_step": "Verify the existing test runner/configuration and record the exact unit-scoped command.",
    "runner_ready_before_first_test": true,
    "testable_layers": [
      "Data model / database behavior",
      "Repository / data access",
      "Business logic",
      "API / endpoint",
      "Frontend behavior"
    ],
    "steps": [
      "Project structure and production configuration skeleton.",
      "Verify the existing test runner/configuration and record the exact unit-scoped command.",
      "Data model / database behavior - implement.",
      "Data model / database behavior - write and run its tests after implementation.",
      "Repository / data access - implement.",
      "Repository / data access - write and run its tests after implementation.",
      "Business logic - implement.",
      "Business logic - write and run its tests after implementation.",
      "API / endpoint - implement.",
      "API / endpoint - write and run its tests after implementation.",
      "Frontend behavior - implement.",
      "Frontend behavior - write and run its tests after implementation.",
      "Environment/build configuration.",
      "Documentation and traceability."
    ]
  },
  "input_sha256": "sha256:322f3cce51ad835b556974215be0ae0a5aba55c673c92ff6b1b327e7eb2426e8",
  "contract_sha256": "sha256:84a32ec0d9dd634774d2a290b523e6e0d5e6f77259469f3112500519cb132392"
}
```

## Implementation Steps

### Step 1 — Spring Boot project skeleton (production config)
- [x] Create `backend/` as a Spring Boot 3.x (Java 17) Maven project: `pom.xml` (web, validation, data-jpa, postgresql, WebClient for upstream calls), `src/main/java/com/mb/callassistant/CallAssistantApplication.java`, `application.yml`.
- [x] Config properties, all from environment variables (NFR3 — no hardcoded secrets): `FS_API_BASE_URL`, `FS_API_KEY`, `RECORDING_BASE_URL`, `STT_PROVIDER`, `STT_BASE_URL`, `STT_API_KEY`.
- [x] `.env.example` documenting every variable (no real values).
- Implements: scaffold for FR1–FR4.

### Step 2 — Data model / persistence
- [x] JPA entities: `CallRecord` (id, tnbCode, phoneNumber, rmName, startedAt, durationSeconds, recordingKey, recordingPath, sttStatus, createdAt) and `TranscriptTurn` (id, callId, speaker [RM|KH], startMs, endMs, textVi). `phone_number` seeded from FreeSWITCH default users (FR7.3 mock-friendly).
- [x] JPA auto-DDL for local Docker Postgres; a small mock-seed loader under a demo profile.

### Step 3 — Repository / data access
- [x] Spring Data JPA repositories: `CallRecordRepository`, `TranscriptTurnRepository` (find turns by call ordered by startMs).

### Step 4 — Recording download service (FR3)
- [x] `RecordingService.downloadRecording(call)` downloads the single stereo `.wav` from the recording server by the call's `recordingName` (via `RecordingClient` → `GET /api/recordings/download/:name`), returning the bytes. Clear error when a recording is not found/not ready (FR3.2). Replaces reliance on random-UUID filenames (FR2.2).
- Revision note (real FreeSWITCH, latest-filename mapping): `recordingKey` is replaced by `recordingName` — the filename of the most recent recording. The real box exposes only `GET /api/recordings/latest` and `GET /api/recordings/download/:name`; a small `RecordingClient` (WebClient) wraps both (`latestFilename()`, `download(name)`), env-driven base + optional `X-API-Key`. `RecordingService.downloadRecording(call)` downloads the single stereo `.wav` by `recordingName`, with bounded retry/backoff so a late `.wav` is retried, and `RecordingNotReadyException` → non-terminal `RECORDING_NOT_READY` (FR1.3) vs terminal `FAILED`. The prior UUID-resolution + per-leg (`-rm`/`-kh`) approach was removed (it did not work on the real box).

### Step 5 — Vietnamese STT via stereo split (FR4)
- [x] `SttService.transcribe(callId, stereoWav)` splits the single stereo `.wav` into its two mono channels (left/right) with a dependency-light plain-Java `WavStereoSplitter` (16-bit PCM frame de-interleaving; no external binary), sends each mono stream to the configured Vietnamese STT provider behind an `SttClient` interface (Whisper-compatible endpoint via `STT_BASE_URL`), and labels one channel RM and the other KH. The side→speaker mapping is configurable (`app.stt.rm-channel: left|right`, default left=RM) so it can be flipped without a code change (FR4.2). Turns from both channels are merged in start-time order. If the file is not 16-bit PCM stereo, it falls back to a single stream (documented limitation). Persist turns; set `sttStatus`.
- Revision note (stereo split): replaces the prior per-leg files + provider-diarization approach. There is exactly ONE stereo `.wav` per call (legs on separate channels); the backend does the split itself.

### Step 6 — Call-record + pipeline + API endpoints (FR1, FR3)
- [x] `POST /api/calls/initiate { phoneNumber, tnbCode }` creates a `CallRecord` (PENDING; no key work now) and returns the record id (the FRONTEND places the call via MicroSIP; the backend does NOT spawn MicroSIP) (FR1.1).
- Revision note (latest-filename mapping): initiate now just creates a PENDING record. On `/ended`, the backend calls `GET /api/recordings/latest` via `RecordingClient`, stores the returned filename as `recordingName`, then runs the pipeline — that filename is the call↔recording link, exposed on `GET /api/calls/{id}`. The old background `FreeSwitchClient`/`ChannelUuidResolver` channel-UUID resolution was removed entirely.
- [x] `POST /api/calls/{id}/ended` marks the call ended and starts the pipeline: download recording (FR3) → STT (FR4); status reflects progress (FR1.3 — results when ready).
- [x] `GET /api/calls/{id}` returns the record + status; `GET /api/calls/{id}/recording` streams the `.wav` (FR3.1); `GET /api/calls/{id}/transcript` returns turns with RM/KH labels (FR4).
- [x] CORS enabled for the local frontend; input validation + boundary error handling (phase guardrails).

### Step 7 — Simple, change-friendly frontend (FR1) — built to be replaced tomorrow
- [x] Plain static frontend (no build step), kept intentionally simple and modular so the Figma-matched UI can swap in tomorrow. Reuse/extend the existing `public/` demo:
  - `public/app.html` (or extend existing) — a lead/call view: a customer phone-number button that (a) `POST`s to the backend `/api/calls/initiate`, then (b) triggers MicroSIP via the existing Node `/api/microsip/call` route, and shows an "Kết thúc / End call" button that `POST`s `/api/calls/{id}/ended`.
  - A post-call view stub that polls `/api/calls/{id}` and, when ready, renders the Vietnamese transcript (RM/KH turns) and a `.wav` download link.
  - Keep markup/data separate: a small `public/js/api.js` wrapping the backend calls and `public/js/ui.js` rendering, with `data-testid` on interactive elements and clear TODO anchors where the Figma design/components land tomorrow.
  - Config: a single `public/js/config.js` holding the backend base URL so it is easy to change.

### Step 8 — FreeSWITCH recordings access (FR2) + guide
- [x] The real box exposes exactly two HTTP endpoints (`GET /api/recordings/latest`, `GET /api/recordings/download/:name`) over one base, and records ONE stereo `.wav` per call (legs on separate channels). No dialplan snippet is shipped — the prior `crm-recording.dialplan.xml` was removed.
- [x] `freeswitch/RECORDING-SETUP.md`: step-by-step EC2 guide documenting the two endpoints and the recordings directory, confirming stereo recording, and keeping a detailed, copy-pasteable **NGINX** section — now reverse-proxying those two endpoints (and/or serving the recordings dir statically) over HTTPS, with a non-colliding illustrative port (NOT 8080), curl tests for both endpoints, and the `RECORDING_BASE_URL` mapping.
- Revision note (real FreeSWITCH): dropped all per-leg/`uuid_record`/dialplan instructions and the FreeSWITCH REST (`FS_API_BASE_URL`) proxy. The mapping is now the "latest recording filename"; the nginx guide proxies `/api/recordings/latest` + `/api/recordings/download/:name` and fixes the port so it does not clash with the Spring Boot backend on 8080.

### Step 9 — Environment/build configuration
- [x] `docker-compose.yml`: local Postgres (optionally a Whisper STT container) for the demo DB (FR7.2).
- [x] `backend/README.md`: how to run the backend + compose + frontend.

### Step 10 — Documentation and traceability
- [x] `code-summary.md`, `traceability.json` mapping FR1–FR4 to files.

## Story-to-Code Traceability
- FR1 (call via MicroSIP) → Step 7 frontend triggers MicroSIP (existing `/api/microsip/call`) + Step 6 initiate endpoint opens the CallRecord
- FR1.3 (results after ended) → Step 6 `/ended` + status
- FR2 (recording mapping via latest filename; stereo recording) → Step 8 recordings endpoints/guide, Step 4 `RecordingClient.latestFilename()`/`download()`
- FR3 (recording download API) → Step 4 service + Step 6 `/recording`
- FR4 (Vietnamese STT, caller/callee) → Step 5 STT service + Step 6 `/transcript`
- FR7 (local Docker DB, mock data) → Steps 2, 9

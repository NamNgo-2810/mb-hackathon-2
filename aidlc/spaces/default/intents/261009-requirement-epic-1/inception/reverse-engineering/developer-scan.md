# Developer Code Scan — CRM Call Assistant / FreeSWITCH Softphone (full rescan)

## Developer Code Scan Results

### Scan Coverage

- **Analyzed deeply**:
  - `package.json` — Node app manifest
  - `docker-compose.yml` — local Postgres (+ optional Whisper) infra
  - `.env` — root env (legacy FreeSWITCH REST + MicroSIP paths; contains a live key)
  - `.gitignore` — project section (AI-DLC section skipped per brief)
  - `server/index.js`, `server/config.js`, `server/upstream.js`
  - `server/routes/callRoutes.js`, `server/routes/microsipRoutes.js`
  - `public/js/config.js`, `public/js/api.js`, `public/js/ui.js`
  - `backend/pom.xml` — Maven/Spring Boot manifest
  - `backend/.env.example`, `backend/.env` (contains live keys)
  - `backend/src/main/resources/application.properties`, `application-demo.properties`
  - All Java under `backend/src/main/java/com/mb/callassistant/` (24 files):
    `CallAssistantApplication`, `web/` (CallController, CallResponse,
    TranscriptTurnResponse, InitiateCallRequest, ApiExceptionHandler),
    `service/` (CallService, CallPipelineService, RecordingService,
    RecordingClient, SttService, SttClient, GoogleSttClient,
    WhisperHttpSttClient, WavStereoSplitter, SttSegment, SttException,
    RecordingNotReadyException), `domain/` (CallRecord, TranscriptTurn,
    SttStatus, Speaker), `repository/` (CallRecordRepository,
    TranscriptTurnRepository), `config/` (AppProperties, ApplicationConfig,
    DemoDataSeeder)
- **Skimmed only**:
  - `public/phone.html` (SIP.js WebRTC softphone — read head: structure/intent)
  - `public/app.html` (RM demo view — read head: structure/intent)
  - `public/index.html` (legacy REST test page — read head)
  - `backend/README.md`, root `README.md`, `RUN-DEMO.md`,
    `backend/GOOGLE-STT-SETUP.md`, `freeswitch/RECORDING-SETUP.md`,
    `epic 1.md` (documentation — read for context, not source)
- **Left out** (per `code-analysis-guide.md` "What to Skip"; not opened):
  - `backend/target/` — Maven build output
  - `backend/.idea/`, `backend/.mvn/` — IDE/wrapper tooling
  - `node_modules/` (gitignored; not present/not opened)
  - AI-DLC install trees: `.kiro/`, `aidlc/`, and AI-DLC-marked sections of
    `AGENTS.md` / `.gitignore` / MCP entries (per brief)

### Packages Found
- `fs-call-test` (root) — Node 1.0.0 — JavaScript — Express server + static frontend for the FreeSWITCH softphone demo and MicroSIP control.
- `call-assistant` (`backend/`) — `com.mb` 0.0.1-SNAPSHOT — Java 17 — Spring Boot 3 REST service: recording download + Vietnamese STT with RM/KH speaker separation.

### Build System
- **Type**: npm (root Node app); Maven via Spring Boot parent (backend).
- **Config Files**: `package.json` (root); `backend/pom.xml`, `backend/.mvn/`; `docker-compose.yml` (Postgres + optional Whisper).
- **Build Dependencies**: root → `express`, `dotenv`. backend → `spring-boot-starter-parent:3.3.4` managing web, validation, data-jpa, webflux, `postgresql` (runtime), `h2` (runtime).

### APIs Discovered
- Backend REST (`/api/calls/*`) — `backend/.../web/CallController.java` — 5 endpoints (initiate, ended, get, transcript, recording).
- Node BFF REST (`/api/*`) — `server/routes/` — legacy `/call*` (3) + `/microsip/*` (7, incl. SSE) + `/healthz`.
- External (consumed): FreeSWITCH recordings server (`/api/recordings/latest`, `/download/:name`); Google Cloud Speech-to-Text (`/v1/speech:recognize`); Whisper-compatible STT (`/v1/audio/transcriptions`); legacy FreeSWITCH call API (`/api/calls`).
- SIP-over-WebSocket (browser → FreeSWITCH, `wss://host:7443`) — not an HTTP API; carried by SIP.js in `public/phone.html`.

### Frameworks & Libraries
- Node: Express `^4.19.2`, dotenv `^16.4.5`; SIP.js (browser, ESM from esm.sh — unpinned).
- Java: Spring Boot `3.3.4` (Web MVC, Validation, Data JPA/Hibernate, WebFlux WebClient), PostgreSQL driver, H2.
- Full version detail in `technology-stack.md`.

### Test Coverage
- **Test Directories**: none (`backend/src/test/` absent; no `*.test.js`).
- **Test Frameworks**: none wired (no JUnit dependency in `pom.xml`, no test runner in `package.json`).
- **Coverage Config**: absent. Backend README states "No automated tests ship in this slice (hackathon decision)."

### Code Quality Indicators
- **Linting**: none configured (no ESLint/Prettier/Checkstyle config; one inline `eslint-disable` comment in `server/index.js`).
- **CI/CD**: none (no project CI workflow; `.github/` holds only AI-DLC-authored content, excluded).
- **Documentation**: strong — rich Javadoc on every backend class, extensive module comments in frontend JS, 5 Markdown guides (root/backend/freeswitch).

### Technical Debt Signals
- **Committed live secrets** (HIGH): real Google STT API key + recording `X-API-Key` in `backend/.../application.properties`; recording key again in root `.env`. Directly contradicts the in-code NFR3 "from env, never hardcoded" claim.
- No tests, no linting, no CI — nothing guards regressions for the new work.
- `ddl-auto=update` (Postgres) / `create-drop` (H2) — no Flyway/Liquibase migrations (README flags this for production).
- Call↔recording mapping is "latest filename" (demo-simple) — races under concurrent calls (`CallService.markEnded`).
- `TranscriptTurn.callId` is an FK-by-value (no JPA relationship / DB FK constraint).
- Whole recording `.wav` buffered in memory (64 MB cap) — not stream-friendly for large/long calls.
- SIP.js imported unpinned from a CDN (esm.sh) with no SRI/version lock.
- Several `TODO (Figma UI, tomorrow)` markers across `public/js/*` and `public/app.html`.

## Handoff Summary
- **Intent-relevant finding**: The CRM call-assistant feature is already partially built. `backend/` (Spring Boot 3, Java 17) implements FR1 (call record lifecycle), FR3 (recording download with bounded retry — `RecordingService`/`RecordingClient`), and FR4 (Vietnamese STT with RM/KH stereo split — `SttService`/`WavStereoSplitter`/`GoogleSttClient`+`WhisperHttpSttClient`), persisted via JPA (`CallRecord`, `TranscriptTurn`). A placeholder RM UI exists at `public/app.html` with a clean `config.js`/`api.js`/`ui.js` split explicitly staged for the Figma redesign. The LLM summarization, form auto-fill, and sales-scenario suggestion described in the new intent are NOT present yet.
- **Risks / follow-up**: Live secrets committed in cleartext must be rotated and moved to env/secret manager before further work (`application.properties`, root `.env`). No DB migrations, no tests, no CI/lint — the new DB design (new intent) will collide with `ddl-auto=update` unless migrations are introduced. The call↔recording "latest" mapping does not survive concurrent calls and will need a real correlation key for production.

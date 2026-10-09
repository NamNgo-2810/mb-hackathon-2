# Dependencies

## External Dependencies

### Backend (Maven, via Spring Boot 3.3.4 BOM)
- `spring-boot-starter-web`, `spring-boot-starter-validation`,
  `spring-boot-starter-data-jpa`, `spring-boot-starter-webflux`
- `org.postgresql:postgresql` (runtime), `com.h2database:h2` (runtime)
- Transitively: Hibernate ORM, Jackson, Reactor Netty (WebClient), embedded
  Tomcat, SLF4J/Logback. (Versions pinned by the 3.3.4 BOM; see
  `technology-stack.md`.)

### Node server (npm)
- `express@^4.19.2`, `dotenv@^16.4.5` (direct). Lockfile `package-lock.json`
  present. Core `node:` modules used: `path`, `fs`, `os`, `child_process`.

### Frontend (browser)
- SIP.js — unpinned ESM import from esm.sh.

### External services (network dependencies)
- FreeSWITCH recordings HTTP server (EC2) — `RecordingClient`.
- Google Cloud Speech-to-Text — `GoogleSttClient`.
- Whisper-compatible STT server — `WhisperHttpSttClient`.
- Legacy FreeSWITCH REST API — Node `upstream.js`.
- MicroSIP desktop executable — `microsipRoutes.js` (`execFile`).

## Internal Cross-Component Dependencies

### Backend call graph (depends-on)
```
CallController -> CallService, RecordingService, TranscriptTurnRepository
CallService    -> CallRecordRepository, CallPipelineService, RecordingClient
CallPipelineService -> CallRecordRepository, RecordingService, SttService
RecordingService    -> RecordingClient, AppProperties, CallRecordRepository
RecordingClient     -> WebClient, AppProperties
SttService          -> SttClient, TranscriptTurnRepository, AppProperties, WavStereoSplitter
GoogleSttClient     -> WebClient, AppProperties           (implements SttClient)
WhisperHttpSttClient-> WebClient, AppProperties           (implements SttClient)
ApplicationConfig   -> AppProperties                      (provides WebClient, CORS)
DemoDataSeeder      -> CallRecordRepository               (demo profile only)
Repositories        -> CallRecord / TranscriptTurn (entities)
Web DTOs            -> CallRecord / TranscriptTurn (mappers)
```

### Node server (depends-on)
```
server/index.js  -> config, routes/callRoutes, routes/microsipRoutes
callRoutes.js    -> upstream, config
microsipRoutes.js-> config, node core (child_process, fs, path, os)
upstream.js      -> config
```

### Frontend (depends-on)
```
app.html -> js/api.js, js/ui.js (via module script)
js/api.js-> js/config.js
js/ui.js -> js/api.js (recordingUrl)
```

## Cross-codebase coupling (contract, not import)

- Frontend `js/api.js` → backend `/api/calls/*` (REST). Base URL in
  `js/config.js` (`backendBaseUrl: http://localhost:8080`).
- Frontend `js/api.js` → Node `/api/microsip/*` (REST + SSE).
- Backend `RecordingClient` → EC2 recordings endpoints (HTTP contract, no
  shared code).
- `DemoDataSeeder` seeds phones 1000–1004 to match the FreeSWITCH default users
  used by the softphone demo (conceptual coupling only).

## Circular references
- None observed in the backend (clean controller → service → repository flow)
  or the Node/front-end modules.

## Notable dependency risks
- **CDN/unpinned SIP.js** — no version lock or integrity check.
- **No DB migration tool** — schema is created by Hibernate `ddl-auto`
  (`update`/`create-drop`); the new intent's database design must introduce
  Flyway/Liquibase to avoid drift. See `code-quality-assessment.md`.

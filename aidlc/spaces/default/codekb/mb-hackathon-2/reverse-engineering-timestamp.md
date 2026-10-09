# Reverse Engineering Timestamp

## Run Record

- Date: 2026-10-09
- Commit: unknown

## Scan Summary

Full rescan of the whole repository at the workspace root. Scanned the
project's own code only: the Node/Express softphone demo (`server/`,
`public/`), the Spring Boot `call-assistant` backend (`backend/`), and root
build/config files (`package.json`, `docker-compose.yml`, `.env`, project
section of `.gitignore`). AI-DLC install trees (`.kiro/`, `aidlc/`) and
AI-DLC-authored content under `.github/`/`.agents/`, build output
(`backend/target/`), IDE/tooling (`backend/.idea/`, `backend/.mvn/`), and
dependency folders were left unopened per the scan brief and
`code-analysis-guide.md`.

## Scope of Analysis

```yaml
scope_version: 1
kind: full
intent: 261009-requirement-epic-1
fingerprint: unknown
analyzed:
  paths:
    - ./
  components:
    - CallController
    - ApiExceptionHandler
    - Web DTOs (CallResponse, TranscriptTurnResponse, InitiateCallRequest)
    - CallService
    - CallPipelineService
    - RecordingService
    - RecordingClient
    - SttService
    - SttClient (interface) + GoogleSttClient + WhisperHttpSttClient
    - WavStereoSplitter
    - SttSegment / SttException / RecordingNotReadyException
    - CallRecord (entity)
    - TranscriptTurn (entity)
    - SttStatus (enum)
    - Speaker (enum)
    - CallRecordRepository / TranscriptTurnRepository
    - CallAssistantApplication
    - AppProperties
    - ApplicationConfig
    - DemoDataSeeder
    - ExpressApp (server/index.js)
    - ServerConfig (server/config.js)
    - FreeswitchUpstreamClient (server/upstream.js)
    - CallRoutes (server/routes/callRoutes.js)
    - MicrosipRoutes (server/routes/microsipRoutes.js)
    - SoftphonePage (public/phone.html)
    - RmReviewView (public/app.html)
    - LegacyCallTestPage (public/index.html)
    - FrontendConfig (public/js/config.js)
    - FrontendApi (public/js/api.js)
    - FrontendUi (public/js/ui.js)
shallow:
  paths:
    - public/phone.html
    - public/app.html
    - public/index.html
```

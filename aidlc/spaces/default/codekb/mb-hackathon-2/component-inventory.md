# Component Inventory

Complete component list with responsibilities and dependencies. Component names
are stable headings referenced by the Scope of Analysis block.

## Backend — web layer

### CallController
- **Responsibility**: REST surface for the call lifecycle (initiate, ended, get, transcript, recording).
- **Depends on**: CallService, RecordingService, TranscriptTurnRepository, ApiExceptionHandler.

### ApiExceptionHandler
- **Responsibility**: `@RestControllerAdvice` mapping exceptions to the JSON error envelope.
- **Depends on**: RecordingNotReadyException, SttException.

### Web DTOs (CallResponse, TranscriptTurnResponse, InitiateCallRequest)
- **Responsibility**: request/response records at the boundary; Bean Validation on input.
- **Depends on**: CallRecord, TranscriptTurn (for `from(...)` mappers).

## Backend — service layer

### CallService
- **Responsibility**: call-record lifecycle; resolves the "latest" recording filename on `ended` and kicks off the pipeline.
- **Depends on**: CallRecordRepository, CallPipelineService, RecordingClient.

### CallPipelineService
- **Responsibility**: async post-call orchestration (download → transcribe); owns the `SttStatus` transitions.
- **Depends on**: CallRecordRepository, RecordingService, SttService.

### RecordingService
- **Responsibility**: download the stereo `.wav` with bounded linear backoff; throw `RecordingNotReadyException` when none within budget.
- **Depends on**: RecordingClient, AppProperties, CallRecordRepository.

### RecordingClient
- **Responsibility**: HTTP client for the two recordings endpoints (`latest`, `download/:name`); defensive filename parsing; optional `X-API-Key`.
- **Depends on**: WebClient, AppProperties, Jackson (local ObjectMapper).

### SttService
- **Responsibility**: split stereo, transcribe each channel, label RM/KH per `rm-channel`, merge by start time, persist idempotently.
- **Depends on**: SttClient, TranscriptTurnRepository, AppProperties, WavStereoSplitter.

### SttClient (interface) + GoogleSttClient + WhisperHttpSttClient
- **Responsibility**: pluggable STT provider abstraction; Google (default) and Whisper-compatible implementations selected by `app.stt.provider`.
- **Depends on**: WebClient, AppProperties; emit `SttSegment`; throw `SttException`.

### WavStereoSplitter
- **Responsibility**: pure-Java de-interleave of a 16-bit PCM stereo WAV into two mono WAVs; single-stream fallback otherwise.
- **Depends on**: none (plain Java).

### SttSegment / SttException / RecordingNotReadyException
- **Responsibility**: STT segment record; STT failure and not-ready exception types.
- **Depends on**: none.

## Backend — domain + persistence

### CallRecord (entity)
- **Responsibility**: one customer call; carries `recordingName` (the call↔recording link), `sttStatus`, `errorDetail`, timing. Table `call_record`.
- **Fields**: id, tnbCode, phoneNumber, rmName, startedAt, durationSeconds, recordingName, recordingPath, sttStatus, errorDetail, createdAt.

### TranscriptTurn (entity)
- **Responsibility**: one labeled utterance; FK-by-value `callId`, `speaker`, `startMs`, `endMs`, `textVi` (len 4000). Table `transcript_turn`, index `(call_id,start_ms)`.

### SttStatus (enum)
- **Values**: `PENDING`, `PROCESSING`, `RECORDING_NOT_READY` (non-terminal), `DONE`, `FAILED`.

### Speaker (enum)
- **Values**: `RM` (agent), `KH` (customer).

### CallRecordRepository / TranscriptTurnRepository
- **Responsibility**: Spring Data JPA. `findByRecordingName`; `findByCallIdOrderByStartMsAsc`, `deleteByCallId`.

## Backend — config

### CallAssistantApplication
- **Responsibility**: Spring Boot entry point; `@EnableAsync`.

### AppProperties
- **Responsibility**: typed binding of `app.recording.*`, `app.stt.*`, `app.cors.*`.

### ApplicationConfig
- **Responsibility**: shared `WebClient` (raised in-memory codec limit) and CORS mapping for `/api/**`.

### DemoDataSeeder
- **Responsibility**: seeds mock call records (phones 1000–1004) under the `demo` profile.

## Node demo server

### ExpressApp (server/index.js)
- **Responsibility**: app bootstrap, static serving, router mounting, JSON error handler.
- **Depends on**: config, callRoutes, microsipRoutes.

### ServerConfig (server/config.js)
- **Responsibility**: env loading, domain sanitisation, MicroSIP path resolution.

### FreeswitchUpstreamClient (server/upstream.js)
- **Responsibility**: thin fetch client for the legacy FreeSWITCH REST API with timeout/abort.

### CallRoutes (server/routes/callRoutes.js)
- **Responsibility**: legacy `/api/call*` BFF with extension/uuid allow-lists.
- **Depends on**: upstream, config.

### MicrosipRoutes (server/routes/microsipRoutes.js)
- **Responsibility**: MicroSIP control via `execFile`, SSE event fan-out, `microsip.ini` auto-config.
- **Depends on**: config, node `child_process`/`fs`/`path`/`os`.

## Frontend

### SoftphonePage (public/phone.html)
- **Responsibility**: SIP.js WebRTC softphone registering directly to FreeSWITCH over WSS.

### RmReviewView (public/app.html)
- **Responsibility**: placeholder RM call-assistant screen (Vietnamese); staged for Figma redesign.
- **Depends on**: public/js config, api, ui.

### LegacyCallTestPage (public/index.html)
- **Responsibility**: legacy REST test page for the Node `/api/call` route.

### FrontendConfig (public/js/config.js)
- **Responsibility**: single source of frontend endpoints + polling knobs.

### FrontendApi (public/js/api.js)
- **Responsibility**: fetch wrappers (backend + MicroSIP) and `pollUntilReady`.

### FrontendUi (public/js/ui.js)
- **Responsibility**: DOM rendering of status, Vietnamese transcript turns, recording link; HTML escaping.

Cross-package dependencies are summarised in `dependencies.md`.

# Code Generation — Code Summary (Core Slice, revised for real FreeSWITCH)

Core end-to-end slice: open a call record → on hang up, map the call to the
**most recent recording filename** → download that single stereo `.wav` from the
FreeSWITCH/EC2 server → **split the stereo file into its two channels** and run
Vietnamese speech-to-text with RM/KH separation. The call is placed from the
frontend via MicroSIP (existing Node route); the backend never spawns MicroSIP.
Brownfield: the existing Node/Express demo (`server/`, `public/phone.html`,
`public/index.html`) is untouched; `public/` was extended with new files only.

## Revision summary (match the REAL FreeSWITCH setup)

The real box exposes **only two HTTP endpoints** and produces **one stereo `.wav`
per call** (the two legs on separate channels). This revision replaces the prior
UUID-resolution + per-leg (`-rm`/`-kh`) dialplan approach, which did not work on
the real box.

- **R-01 — latest-filename mapping (core fix).** On `POST /api/calls/initiate`
  the backend just creates a `PENDING` record. On `POST /api/calls/{id}/ended`
  it calls `GET <RECORDING_BASE_URL>/api/recordings/latest`, stores the returned
  filename as `CallRecord.recordingName`, and runs the pipeline. **That filename
  IS the call↔recording link** — no UUID resolution, no dialplan key, no SIP
  headers. A new `RecordingClient` (WebClient) wraps the two endpoints
  (`latestFilename()`, `download(name)`), parsing a JSON `{"name":…}` object or a
  bare string defensively; base URL + optional `X-API-Key` from env (NFR3).
- **R-02 — stereo split for RM/KH (replaces per-leg).** `SttService` splits the
  single stereo `.wav` into left/right mono channels with a dependency-light
  plain-Java `WavStereoSplitter` (16-bit PCM frame de-interleaving; walks RIFF
  chunks; no external binary), transcribes each channel, and labels one RM and
  the other KH. The side→speaker mapping is **configurable**:
  `app.stt.rm-channel` (`left`|`right`, default `left`=RM, `right`=KH) — flip
  without a code change. Turns from both channels are merged in start-time order.
  If the file is not 16-bit PCM stereo, it falls back to a single stream labeled
  RM (documented limitation).
- **R-03 — bounded retry/backoff (kept).** `RecordingService.downloadRecording`
  downloads by `recordingName` with simple linear backoff so a `.wav` that is a
  second late is retried, and backfills `recordingName` from `latest` if the
  record has none yet. `RecordingNotReadyException` → non-terminal
  `RECORDING_NOT_READY` (keep polling, FR1.3); STT/unexpected errors → terminal
  `FAILED`.
- **R-04 — removed abandoned pieces.** Deleted `FreeSwitchClient.java`,
  `ChannelUuidResolver.java` (and their `@Async` channel-UUID wiring),
  `RecordingLeg.java`, `SftpRecordingFallback.java`,
  `NoopSftpRecordingFallback.java`, and `freeswitch/crm-recording.dialplan.xml`.
  `recordingKey` (UUID) became `recordingName` (filename).

## Files changed this revision

### Backend — `backend/`
- `service/RecordingClient.java` **(new)** — WebClient wrapper for the two real
  endpoints: `latestFilename()` (`GET /api/recordings/latest`, JSON-or-string
  defensive parse) and `download(name)` (`GET /api/recordings/download/:name`,
  path-segment encoded). Env base + optional `X-API-Key` (NFR3).
- `service/WavStereoSplitter.java` **(new)** — plain-Java 16-bit PCM stereo
  de-interleave into two mono WAVs; single-stream fallback for non-stereo input.
- `service/CallService.java` — `initiate` creates a `PENDING` record only;
  `markEnded` reads `latest`, stores `recordingName`, runs the pipeline. Removed
  the `FreeSwitchClient`/`ChannelUuidResolver` dependencies.
- `service/RecordingService.java` — downloads the single stereo `.wav` by
  `recordingName` via `RecordingClient`; bounded retry/backoff; backfills the
  name from `latest`; throws `RecordingNotReadyException` within budget. Returns
  `byte[]` (was `List<RecordingLeg>`).
- `service/SttService.java` — stereo-split transcription; configurable
  `rm-channel` mapping; merge-by-start-time; single-stream fallback. Signature
  is now `transcribe(callId, byte[] stereoWav)`.
- `service/CallPipelineService.java` — passes the downloaded `byte[]` to
  `SttService`; status semantics unchanged.
- `web/CallController.java` — `/recording` streams the single stereo `byte[]`.
- `web/CallResponse.java` — exposes `recordingName` (was `recordingKey`).
- `domain/CallRecord.java` — `recordingKey` → `recordingName`
  (`recording_name` column); doc rewritten to the latest-filename mapping.
- `repository/CallRecordRepository.java` — `findByRecordingName` (was
  `findByRecordingKey`).
- `config/AppProperties.java` — dropped the `freeswitch.*` block; added
  `recording.api-key` and `stt.rm-channel` (all env-driven).
- `config/DemoDataSeeder.java` — seeds `recordingName` (`demo-<phone>.wav`).
- `resources/application.yml` — dropped `app.freeswitch.*`; added
  `recording.api-key` (`RECORDING_API_KEY`) and `stt.rm-channel`
  (`STT_RM_CHANNEL`).
- `.env.example` — rewritten: removed `FS_API_*`/`FS_RESOLVE_*`; documented the
  two recordings endpoints, `RECORDING_API_KEY`, and `STT_RM_CHANNEL` (safe
  placeholders only, NFR3).
- `README.md` — initiate/config/Recordings sections updated to the
  latest-filename + stereo-split reality.

### FreeSWITCH — `freeswitch/`
- `crm-recording.dialplan.xml` **(removed)** — dialplan approach abandoned.
- `RECORDING-SETUP.md` — rewritten: documents the two endpoints and the
  recordings directory, confirms stereo recording, and keeps a detailed
  copy-pasteable **NGINX** section reverse-proxying `/api/recordings/latest` +
  `/api/recordings/download/:name` (and a static-serve alternative) over HTTPS.
  The illustrative upstream port is `9100` with an explicit note that it must not
  be `8080` (the Spring Boot backend). Includes self-signed cert steps, curl
  tests for both endpoints, and the `RECORDING_BASE_URL` mapping. All
  per-leg/`uuid_record`/dialplan and `FS_API_BASE_URL` content was dropped.

### Frontend — `public/`
- No structural change. The flow is unchanged (initiate → MicroSIP via the
  existing Node route → End → poll status → transcript + `.wav` link). The UI
  never referenced `recordingKey`, and the poll loop already treats only
  `DONE`/`FAILED` as terminal, so `RECORDING_NOT_READY` keeps polling (FR1.3).
  The legacy `public/index.html` REST test page (its own `currentUuid` for the
  Node `/api/call` route) is untouched.

## Unchanged core components (from prior builds)

STT client/segment (`SttClient`, `WhisperHttpSttClient`, `SttSegment`,
`SttException`), `RecordingNotReadyException`, `SttStatus` (incl.
`RECORDING_NOT_READY`), DTOs/response views (`InitiateCallRequest`,
`TranscriptTurnResponse`), `TranscriptTurn`, `Speaker`, repositories,
`ApiExceptionHandler`, `ApplicationConfig` (WebClient + CORS), `pom.xml`,
`docker-compose.yml`, and the frontend `app.html`/`config.js`/`api.js`/`ui.js`.

## Deviations / notes

- **No automated tests** written this run — user decision stands (hackathon,
  refactor scope). Production code only; the existing Node demo stays
  green/untouched.
- **Build not verified in this environment:** Maven (and no Maven wrapper) is not
  installed on the machine running code generation (JDK 21 is present), so
  `mvn compile`/`package` could not be executed here. Code is written to compile
  against Spring Boot 3.3.4 / Java 17; verification is deferred to Build and Test
  (3.6). A careful manual review confirmed no dangling references after the
  refactor (grep across the backend for all removed symbols returns nothing).
- **Stereo-split limitation.** The splitter handles 16-bit PCM stereo (the
  common FreeSWITCH recording format). A non-stereo or non-PCM `.wav` is
  transcribed as a single stream and labeled RM; flip
  `STT_RM_CHANNEL=left|right` if the two channels are reversed.
- **"Latest" is read at hang-up**, so run one call's full lifecycle before the
  next in the demo; the download also retries with backoff for a late `.wav`.
- The backend streams the single stereo recording from `GET /{id}/recording`.

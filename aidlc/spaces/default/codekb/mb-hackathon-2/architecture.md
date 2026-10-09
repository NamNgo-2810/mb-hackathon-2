# Architecture

## System Overview

Two cooperating codebases plus external systems:

1. **Node/Express demo server** (`server/`) — serves the static frontend,
   controls the local MicroSIP desktop client, fans call events to the browser
   over SSE, and keeps a legacy FreeSWITCH REST BFF route.
2. **Spring Boot "call-assistant" backend** (`backend/`) — the post-call
   pipeline: open a call record, download the recording from the FreeSWITCH/EC2
   recording server, transcribe it (Vietnamese, RM/KH stereo split), persist and
   serve the transcript.
3. **Browser frontend** (`public/`) — a SIP.js WebRTC softphone
   (`phone.html`), a placeholder RM review view (`app.html` + `js/*`), and a
   legacy REST test page (`index.html`).
4. **External systems** — FreeSWITCH (SIP/WSS + recordings HTTP server on EC2),
   Google Cloud Speech-to-Text (default STT), optional self-hosted Whisper,
   PostgreSQL (or in-memory H2 in the demo profile).

## Architectural Style

**Hybrid, loosely-coupled services** — not a single monolith:
- The browser talks SIP/WebRTC **directly** to FreeSWITCH (no server in the
  media/signaling path for the softphone).
- The Node server is a thin BFF / device-control shim (MicroSIP + legacy REST).
- The Spring Boot backend is a conventional layered REST service
  (controller → service → repository) with a pluggable STT provider (Strategy
  via the `SttClient` interface and `@ConditionalOnProperty`).

Evidence: `public/phone.html` imports SIP.js and connects to `wss://host:7443`;
`server/index.js` serves pages and mounts routers only; `CallController` →
`CallService`/`RecordingService`/`CallPipelineService` → JPA repositories.

## Component Relationships

```mermaid
flowchart TB
  subgraph Browser
    PH["phone.html (SIP.js softphone)"]
    APP["app.html + js/config,api,ui"]
  end
  subgraph Node["Node/Express (server/)"]
    IDX["index.js"]
    MR["microsipRoutes.js"]
    CR["callRoutes.js"]
    UP["upstream.js"]
  end
  subgraph Backend["Spring Boot (backend/)"]
    CC["CallController"]
    CS["CallService"]
    PS["CallPipelineService (async)"]
    RS["RecordingService"]
    RC["RecordingClient"]
    SS["SttService"]
    WS["WavStereoSplitter"]
    GC["GoogleSttClient / WhisperHttpSttClient"]
    REPO["JPA repositories"]
    DB[("PostgreSQL / H2")]
  end
  subgraph External
    FSW["FreeSWITCH (SIP/WSS)"]
    REC["Recordings HTTP server (EC2)"]
    STT["Google STT / Whisper"]
    MS["MicroSIP.exe (local)"]
  end

  PH -->|SIP over WSS + WebRTC audio| FSW
  APP -->|REST| CC
  APP -->|REST| MR
  MR -->|execFile| MS
  MR -->|SSE events| APP
  CR --> UP -->|REST| FSW
  CC --> CS --> PS
  PS --> RS --> RC -->|HTTP| REC
  PS --> SS --> WS
  SS --> GC -->|HTTP| STT
  CS --> REPO --> DB
  SS --> REPO
```

<!-- Text fallback: The Browser has two parts — phone.html (a SIP.js softphone)
and app.html with its js modules. phone.html speaks SIP-over-WSS and WebRTC
audio directly to FreeSWITCH. app.html calls the Spring Boot CallController over
REST and the Node microsipRoutes over REST, and receives SSE events back from
microsipRoutes. microsipRoutes runs MicroSIP.exe locally via execFile. The Node
callRoutes uses upstream.js to call FreeSWITCH's legacy REST API. In the
backend, CallController delegates to CallService, which triggers the async
CallPipelineService. The pipeline uses RecordingService (via RecordingClient,
HTTP to the EC2 recordings server) to fetch the .wav, then SttService (which
uses WavStereoSplitter to de-interleave stereo and GoogleSttClient/
WhisperHttpSttClient over HTTP to the STT provider). CallService and SttService
persist through JPA repositories to PostgreSQL or H2. -->

## Data Flow

A customer call becomes: call record (PENDING) → call ends → recording filename
resolved from "latest" → `.wav` downloaded (bounded retry) → stereo split into
RM/KH mono channels → each channel transcribed in Vietnamese → turns merged by
start time and persisted → UI polls status, then renders transcript + recording
link.

## Key Design Decisions

- **Browser-direct SIP/WebRTC** keeps the server out of the media path
  (simplifies the demo; shifts TLS/cert and NAT/TURN concerns to the client +
  FreeSWITCH).
- **Pluggable STT** behind `SttClient` (`google` default, `whisper` alternate)
  selected by `app.stt.provider` — swap providers without touching `SttService`.
- **In-process stereo split** in pure Java (`WavStereoSplitter`) — no ffmpeg
  dependency; limited to 16-bit PCM stereo, else single-stream fallback.
- **Async pipeline** (`@EnableAsync` + `@Async`) so `/ended` returns
  immediately and the UI polls `GET /api/calls/{id}`.
- **Config-first secrets** (`AppProperties`, env-wins) — intent is good, but
  live keys were committed anyway (see `code-quality-assessment.md`).

## Improvement Opportunities

- Introduce DB migrations (Flyway/Liquibase) in place of `ddl-auto`.
- Replace "latest filename" mapping with a real call↔recording correlation key.
- Stream recordings instead of fully buffering (64 MB cap today).
- Add tests, linting, and CI before layering the new LLM/UI work on top.

## Interaction Diagrams

### Post-call transcription pipeline (FR1/FR3/FR4)

```mermaid
sequenceDiagram
  participant UI as app.html (api.js)
  participant CC as CallController
  participant CS as CallService
  participant PS as CallPipelineService
  participant RS as RecordingService
  participant REC as EC2 Recordings
  participant SS as SttService
  participant STT as STT provider
  participant DB as DB

  UI->>CC: POST /api/calls/initiate
  CC->>CS: initiate(phone, tnb)
  CS->>DB: save CallRecord (PENDING)
  CC-->>UI: 201 CallResponse(id)
  UI->>CC: POST /api/calls/{id}/ended
  CC->>CS: markEnded(id)
  CS->>REC: GET /api/recordings/latest
  REC-->>CS: filename
  CS->>DB: save (PROCESSING, recordingName)
  CS->>PS: runPipeline(id) [async]
  CC-->>UI: CallResponse(PROCESSING)
  PS->>RS: downloadRecording(id)
  RS->>REC: GET /download/:name (bounded retry)
  REC-->>RS: .wav bytes
  PS->>SS: transcribe(id, stereoWav)
  SS->>STT: recognize left + right channels
  STT-->>SS: segments
  SS->>DB: save TranscriptTurns
  PS->>DB: status DONE
  loop poll
    UI->>CC: GET /api/calls/{id}
    CC-->>UI: sttStatus
  end
  UI->>CC: GET /api/calls/{id}/transcript
  CC-->>UI: RM/KH turns
```

<!-- Text fallback: The UI posts initiate; CallController calls CallService,
which saves a PENDING CallRecord and returns id. The UI posts {id}/ended;
CallService asks the EC2 recordings server for the latest filename, saves
PROCESSING with that recordingName, and starts the async CallPipelineService,
returning immediately. The pipeline calls RecordingService, which downloads the
.wav from the recordings server with bounded retry, then SttService, which sends
the left and right channels to the STT provider, maps the returned segments to
TranscriptTurns, and persists them; the pipeline sets status DONE. Meanwhile the
UI polls GET /api/calls/{id} until the status is terminal, then fetches the
transcript. -->

### Softphone call + MicroSIP event fan-out

```mermaid
sequenceDiagram
  participant B as Browser
  participant FS as FreeSWITCH
  participant MS as MicroSIP.exe
  participant MR as microsipRoutes

  B->>FS: SIP REGISTER over WSS
  B->>FS: INVITE + WebRTC media
  Note over B,FS: phone.html path is fully browser to FreeSWITCH
  B->>MR: POST /api/microsip/call {number}
  MR->>MS: execFile microsip.exe [number]
  MS-->>MR: (cmdCallEnd) GET /api/microsip/event?type=end
  MR-->>B: SSE event (type=end)
```

<!-- Text fallback: For the SIP.js softphone, the browser registers and sends an
INVITE with WebRTC media straight to FreeSWITCH; the Node server plays no part.
For MicroSIP control, the browser posts to /api/microsip/call and
microsipRoutes runs microsip.exe with the number as an argv argument. MicroSIP's
lifecycle hooks call GET /api/microsip/event, and microsipRoutes broadcasts
those as Server-Sent Events back to the browser. -->

# API Documentation

## Internal API — Spring Boot backend (`/api/calls`)

Source: `backend/.../web/CallController.java`. Base URL `http://localhost:8080`
(configurable). CORS allows `GET,POST,OPTIONS` from `app.cors.allowed-origins`.
Errors use the envelope `{ "error": { code, message, details?, timestamp } }`
(`ApiExceptionHandler`).

| Method | Path | Request | Success | Notes |
|---|---|---|---|---|
| POST | `/api/calls/initiate` | JSON `{ phoneNumber, tnbCode }` (validated) | `201` `CallResponse` | Opens a `CallRecord` (PENDING). |
| POST | `/api/calls/{id}/ended` | path `id` | `200` `CallResponse` | Marks ended, resolves recording, starts async pipeline. `404` if unknown. |
| GET | `/api/calls/{id}` | path `id` | `200` `CallResponse` | Poll pipeline status. `404` if unknown. |
| GET | `/api/calls/{id}/transcript` | path `id` | `200` `TranscriptTurnResponse[]` | `404` if call unknown; empty list = not ready. |
| GET | `/api/calls/{id}/recording` | path `id` | `200` `audio/wav` bytes | `Content-Disposition: inline; filename="call-{id}.wav"`. `404` if not ready. |

### Contracts

- **InitiateCallRequest** — `phoneNumber` (`@NotBlank`, regex
  `^[A-Za-z0-9_.+*#@:-]{1,128}$`), `tnbCode` (`@NotBlank`, `@Size max 32`,
  regex `^[A-Za-z0-9_-]{1,32}$`).
- **CallResponse** — `{ id, tnbCode, phoneNumber, rmName, startedAt,
  durationSeconds, recordingName, sttStatus, errorDetail, createdAt }`.
  `sttStatus` ∈ `PENDING | PROCESSING | RECORDING_NOT_READY | DONE | FAILED`.
- **TranscriptTurnResponse** — `{ id, speaker (RM|KH), startMs, endMs, textVi }`.

### Error codes (envelope `code`)

`VALIDATION_FAILED` (400), `RECORDING_NOT_READY` (404), `STT_ERROR` (502),
`INTERNAL_ERROR` (500), plus HTTP-status-named codes from
`ResponseStatusException`.

## Internal API — Node demo server (`/api`)

Source: `server/routes/`. Base URL `http://localhost:3000` (default).

### Legacy FreeSWITCH BFF — `callRoutes.js`

| Method | Path | Request | Notes |
|---|---|---|---|
| POST | `/api/call` | `{ from, to }` (ext allow-list `^[A-Za-z0-9_.+*#-]{1,32}$`) | Proxies to upstream `POST /api/calls`; maps 201/400/409/502. |
| GET | `/api/call/:uuid` | path `uuid` (`^[A-Za-z0-9_-]{8,64}$`) | Status via upstream. |
| DELETE | `/api/call/:uuid` | path `uuid` | Hang up (404 treated as already ended). |

### MicroSIP control — `microsipRoutes.js`

| Method | Path | Request | Notes |
|---|---|---|---|
| POST | `/api/microsip/call` | `{ number }` (`^[A-Za-z0-9_.+*#@:-]{1,128}$`) | `execFile` MicroSIP with the number. |
| POST | `/api/microsip/hangup` | — | `microsip.exe /hangupall`. |
| POST | `/api/microsip/answer` | — | `microsip.exe /answer`. |
| GET | `/api/microsip/events` | — | **SSE** stream of call lifecycle events. |
| GET | `/api/microsip/event` | query `type`, `cid` | Hook target MicroSIP's `cmd*` scripts hit; broadcasts to SSE clients; `204`. |
| POST | `/api/microsip/configure` | — | Writes `.cmd` hook scripts and upserts `microsip.ini` cmd* keys. |

### Misc

| Method | Path | Notes |
|---|---|---|
| GET | `/healthz` | `{ ok: true }`. |
| GET | `/` | serves `public/phone.html`. |
| GET | `/call-test` | serves `public/index.html`. |

## External APIs (consumed)

| System | Endpoint(s) | Caller | Contract |
|---|---|---|---|
| FreeSWITCH recordings server (EC2) | `GET <base>/api/recordings/latest`, `GET <base>/api/recordings/download/:name` | `RecordingClient` | `latest` returns JSON `{name|filename|file|recording|latest}` or a bare string; `download` returns `.wav` bytes. Optional `X-API-Key`. |
| Google Cloud Speech-to-Text | `POST https://speech.googleapis.com/v1/speech:recognize?key=…` | `GoogleSttClient` | LINEAR16 inline base64, `languageCode=vi-VN`, `enableWordTimeOffsets`, `audioChannelCount=1`. |
| Whisper-compatible STT | `POST <base>/v1/audio/transcriptions` | `WhisperHttpSttClient` | multipart `file` + `model=whisper-1` + `language` + `response_format=verbose_json`; optional Bearer auth. |
| Legacy FreeSWITCH call API | `POST/GET/DELETE /api/calls[/:uuid]` | Node `upstream.js` | `X-API-Key` header; JSON. |
| FreeSWITCH SIP/WSS | `wss://<host>:7443` (SIP over WebSocket + WebRTC) | `public/phone.html` (SIP.js) | Not HTTP; registration + INVITE/media. |

Status lifecycle for `sttStatus` is defined in `component-inventory.md`
(`SttStatus` enum).

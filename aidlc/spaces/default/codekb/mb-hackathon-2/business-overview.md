# Business Overview

## Domain

A hackathon demo in the **relationship-manager (RM) telephony / CRM** space. A
Relationship Manager calls a customer (Khách hàng, "KH") over VoIP, and the
system captures and transcribes that conversation so the RM has a written,
speaker-separated record of the call.

## Purpose

Turn a live RM→customer phone call into a browser-visible, Vietnamese
transcript with the two speakers (RM vs KH) separated, plus a downloadable
recording. This is the foundation for a larger "call assistant" vision
(summaries, form auto-fill, sales-scenario suggestions) that is **not yet
built** in the current code.

## Key Functionality (what exists today)

| Capability | Where | Notes |
|---|---|---|
| Place a call from the browser (WebRTC softphone) | `public/phone.html` | SIP.js registers directly to FreeSWITCH over SIP-over-WebSocket; audio via WebRTC. |
| Place a call via the desktop MicroSIP client | `server/routes/microsipRoutes.js` | Node spawns `microsip.exe`; live call events fan out to the browser over SSE. |
| Legacy "place a call via FreeSWITCH REST API" | `server/routes/callRoutes.js` | Optional BFF route; needs `FS_API_*` env. |
| Open a call record | `POST /api/calls/initiate` (backend) | Creates a `CallRecord` (status PENDING). |
| End a call and run the post-call pipeline | `POST /api/calls/{id}/ended` | Resolves the recording, downloads it, transcribes it — asynchronously. |
| Download the per-call recording | `GET /api/calls/{id}/recording` | Streams the stereo `.wav`. |
| View the RM/KH Vietnamese transcript | `GET /api/calls/{id}/transcript` | Turns labeled RM or KH with time windows. |
| Placeholder RM review screen | `public/app.html` + `public/js/*` | Deliberately simple, staged for a Figma redesign. |

## Business Actors

- **RM (Relationship Manager)** — the agent, "our side"; one stereo channel.
- **KH (Khách hàng / customer)** — the called party; the other stereo channel.

## Business Rules (observed, demo-grade)

- **BR-latest-mapping**: a call is linked to its recording by asking the
  recording server for the *most recent* filename when the call ends — there is
  no UUID or dialplan correlation key (demo-simple; races under concurrency).
- **BR-stereo-split**: exactly one stereo `.wav` per call; the two legs are on
  separate channels. RM is the `left` channel by default, flippable to `right`
  via config without code change.
- **BR-nonterminal-not-ready**: a recording that has not flushed yet is a
  *non-terminal* state (`RECORDING_NOT_READY`) — the UI keeps polling.
- **BR-vi-language**: transcription is Vietnamese (`vi-VN`); user-facing UI text
  is Vietnamese.

## Relationship to the Planned Work

The new intent (classic scope) adds Figma design, database design,
speech-to-text integration, a Spring Boot backend, and UI for a full
call-assistant (record → transcribe → **LLM summary + form fill + sales
scenario**). Today's code already delivers the record→transcribe backbone and a
Spring Boot backend; the **LLM/summary/form/sales-scenario** layer and the
**Figma-matched UI** are the net-new parts.

Cross-references: system shape in `architecture.md`; endpoints in
`api-documentation.md`; components in `component-inventory.md`.

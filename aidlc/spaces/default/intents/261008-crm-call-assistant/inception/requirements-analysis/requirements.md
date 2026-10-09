# Requirements — CRM VoIP Call Assistant

## Intent Analysis

The RM wants to finish a customer call with the wrap-up already done: instead of
manually writing notes and deciding the next sell, the system captures the call,
transcribes it (Vietnamese, caller/callee separated), and uses an LLM to produce
a summary, auto-fill a CRM form, and recommend a selling scenario — surfaced in
a web UI that matches the BA's Figma design [intent-statement][scope-document].
This is a one-day hackathon demo built beside an existing calling demo; the goal
is a convincing end-to-end flow, not a production system.

## Functional Requirements

### FR1 — Call initiation (via MicroSIP)
- FR1.1 The RM initiates a customer call from the web UI; the call is placed through MicroSIP on the RM's machine (the WebRTC in-browser softphone path is discarded in favor of MicroSIP for stability and sound quality) [Q2].
- FR1.2 The call connects the RM to a customer identified by a `phone_number` drawn from the FreeSWITCH default users [intent-statement:Q4].
- FR1.3 The RM marks the call as ended in the web UI after hanging up; the UI does not track live call state. On "call ended," the backend begins the recording→STT→AI pipeline and the UI shows the results when ready (no live calling/in-call signaling is required) [Q2][requirements-analysis review R-01].

### FR2 — Call recording capture and mapping
- FR2.1 Each call is recorded on the FreeSWITCH/EC2 server [intent-statement].
- FR2.2 FreeSWITCH is configured so each recording maps to its call rather than using the current random-UUID naming; the mapping lets the backend fetch the correct recording for a given call [intent-statement:Q6].
- FR2.3 The call is recorded with caller and callee on separate channels (per-leg / stereo recording) to enable speaker separation [intent-statement:Q7].

### FR3 — Recording download API (Spring Boot)
- FR3.1 A Spring Boot backend exposes an API that downloads a call's recording `.wav`(s) from the EC2 server given the call identifier [scope-document:S2].
- FR3.2 The API surfaces a clear error when a recording is not found or not yet available for the requested call [phase:inception error handling].

### FR4 — Vietnamese speech-to-text with speaker separation
- FR4.1 The backend transcribes the call recording to Vietnamese text [intent-statement].
- FR4.2 The transcript attributes each turn to caller or callee, primarily from the per-leg recording (provider diarization is the fallback) [intent-statement:Q7].

### FR5 — AI smart tasks
- FR5.1 From the transcript, an LLM produces a concise call summary [intent-statement:Q3].
- FR5.2 The LLM auto-fills a CRM form. The fields shown are governed by the Figma design; the fallback field set is: customer name, phone number, call date/time, call purpose/topic, customer needs/pain points, outcome/disposition, follow-up action, next-contact date [Q1].
- FR5.3 The LLM produces a recommended selling scenario: a product/offer to pitch next plus a few talking points for the next contact [intent-statement:Q3].

### FR6 — Web UI (Figma-matched)
- FR6.1 The UI closely matches the BA-provided Figma design (https://www.figma.com/design/ObEPaidO5YFTYB5kxk5hju/Untitled?node-id=0-1) [intent-statement:Q5].
- FR6.2 After a call, the UI displays the call summary, the auto-filled form (editable by the RM), and the recommended selling scenario [intent-statement:Q3].
- FR6.3 The UI displays the Vietnamese transcript with caller/callee turns alongside the summary/form/suggestion [Q3].

### FR7 — Persistence (local Dockerized DB)
- FR7.1 A database stores the call record, its transcript, the AI-generated summary, the form data, and the sales suggestion [scope-document:S7].
- FR7.2 The database runs locally in a Docker container for the demo [intent-statement:Q8].
- FR7.3 Mock data populates the UI convincingly where real data is not available; `phone_number` values come from the FreeSWITCH default users [intent-statement:Q4][scope-document:S9].

## Non-Functional Requirements

- NFR1 (Usability) The post-call result (summary, form, suggestion, transcript) renders in a single reviewable view matching the Figma layout; the demo is showable end to end in under ~10 minutes [intent-statement:Q1].
- NFR2 (Reliability) The call path uses MicroSIP for stable audio and recording quality in the demo environment [Q2].
- NFR3 (Security — demo level) Secrets (FreeSWITCH API key, EC2 host, any STT/LLM keys) are read from environment/config, never hardcoded in source or committed; the existing tracked `.env` secret is flagged for rotation [phase:inception security][code-quality-assessment].
- NFR4 (Maintainability) The new Spring Boot backend is a clean module beside the existing demo; the Windows-only MicroSIP control scaffolding from the Node demo is not treated as a backend contract [architecture][code-quality-assessment].

## Constraints

- C1 Backend is Spring Boot [desc].
- C2 FreeSWITCH runs on EC2 and already produces recordings; it must be reconfigured (guided step by step) for mappable, per-leg recordings [intent-statement:Q6][Q7].
- C3 Speech-to-text must support Vietnamese [intent-statement].
- C4 The DB runs locally in Docker [intent-statement:Q8].
- C5 One-day hackathon time box; demoed locally [desc].

## Assumptions

- A1 A Vietnamese speech-to-text provider/model is available for the demo; the specific choice is deferred to the speech-to-text implementation step (self-hosted Whisper vs. a cloud API with Vietnamese support) [intent-statement][assumption].
- A2 MicroSIP is installed and controllable on the RM's machine, and FreeSWITCH records calls placed through it [Q2][assumption].
- A3 The Figma design provides the concrete field set and layout for the form and result view [intent-statement:Q5][assumption].

## Out of Scope

- Real user authentication / per-RM login and permissions [scope-document].
- Production deployment, CI/CD, and monitoring [scope-document].
- Integration with a real external CRM system of record — data stays mocked locally [scope-document].
- A TURN server / production-grade media reliability across NAT [scope-document].
- Multi-language speech-to-text — Vietnamese only [scope-document].
- The in-browser WebRTC softphone as the call mechanism — superseded by MicroSIP [Q2].

## Open Questions

- OQ1 Which Vietnamese speech-to-text provider/model (resolved at the speech-to-text implementation step) [A1].
- OQ2 Exact form fields and layout, pending the Figma design detail [A3].

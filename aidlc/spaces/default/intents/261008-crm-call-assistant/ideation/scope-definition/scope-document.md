# Scope Document — CRM VoIP Call Assistant

## Scope Boundary

### In Scope (added on top of the existing calling demo)

| # | Capability | Source |
|---|-----------|--------|
| S1 | Web VoIP call from the RM's browser/machine to a customer (building on the existing FreeSWITCH + softphone demo) | [intent-statement][desc] |
| S2 | Backend API (Spring Boot) to download the call recording `.wav` from the EC2 server | [intent-statement][desc] |
| S3 | FreeSWITCH configuration so each call's recording maps to its call (replacing the current random-UUID naming), guided step by step | [intent-statement:Q6] |
| S4 | Per-leg (stereo) FreeSWITCH recording so caller and callee are on separate channels (primary caller/callee separation) | [intent-statement:Q7] |
| S5 | Vietnamese speech-to-text integration, with caller/callee separation (provider diarization as fallback) | [intent-statement:Q7][desc] |
| S6 | AI smart tasks: call summary, auto-filled CRM form fields, and a recommended selling scenario | [intent-statement:Q3] |
| S7 | Database design and a local DB running in Docker | [intent-statement:Q8][desc] |
| S8 | UI implementation that closely matches the BA-provided Figma design | [intent-statement:Q1][Q5] |
| S9 | Mock data that convincingly populates the UI; `phone_number` sourced from FreeSWITCH default users | [intent-statement:Q4] |

### Out of Scope (this first version) [intent-statement:Q4][Q3]

- Real user authentication / per-RM login and permissions.
- Production deployment, CI/CD, and monitoring.
- Integration with a real external CRM system of record (data stays mocked locally).
- A TURN server / production-grade media reliability across NAT.
- Multi-language speech-to-text (Vietnamese only).

## Minimum Viable Scope

The MVS is the full end-to-end chain, because a convincing hackathon demo
requires every link to work together [Q1][intent-statement:Q1]:

web call (S1) → recording download API (S2) with mappable recordings (S3) →
per-leg recording (S4) → Vietnamese STT (S5) → AI summary + form-fill + sales
suggestion (S6), surfaced in a Figma-matched UI (S8) over a local Dockerized DB
(S7) with mock data (S9).

The only Could-have is additional visual polish beyond the Figma match [Q1].

## Sequencing

Dependency-first: stand up a thin end-to-end slice (call → recording → STT → AI
→ UI skeleton) first to de-risk integration, then make the UI match the Figma
design and sharpen the AI outputs [Q2].

## Value Stream

RM places call (S1) → call recorded per-leg and mapped (S3, S4) → recording
retrieved by backend (S2) → transcribed with speaker separation (S5) → AI
produces summary, form, and sales suggestion (S6) → RM reviews/edits in the
Figma-matched UI (S8) → outcome persisted to the local DB (S7). The customer
outcome: the RM finishes a call with the wrap-up already drafted and a
recommended next sell.

## Assumptions & Open Questions

- A Vietnamese speech-to-text provider is available for the demo; the specific
  provider is not yet chosen (speaker separation primarily via per-leg
  recording, provider diarization as fallback) [intent-statement][assumption].

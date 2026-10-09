# Intent Statement — CRM VoIP Call Assistant

## Problem Statement

A Relationship Manager (RM) in a financial CRM today handles customer calls and
then manually logs call notes and decides the next sales action, which is slow
and inconsistent [Q2][desc]. This initiative lets an RM place a VoIP call to a
customer from the web, and after the call automatically produces a call summary,
fills a CRM form, and suggests a selling scenario for the next contact — removing
the manual wrap-up work [Q2][Q3][desc].

## Target Customer

- The primary user is a Relationship Manager in a financial/CRM setting who
  places outbound customer calls and records the outcome [Q2].
- The pain being removed is manual, time-consuming after-call note-taking and
  the manual decision of what to sell next; the system automates note-taking and
  form-fill and recommends the next sales move [Q2][Q3].

## Success Metrics

- A hackathon demo in which the RM places a call from the web, hangs up, and the
  app shows an AI-generated summary, an auto-filled form, and a sales suggestion
  derived from the real call recording, with the UI closely matching the BA's
  Figma design [Q1].
- The end-to-end chain works: call → recording download from the EC2 server →
  Vietnamese speech-to-text that separates caller from callee → AI summary,
  form-fill, and sales-scenario suggestion [Q1][Q3][desc].
- The AI outputs include a call summary, auto-filled CRM form fields, and a short
  recommended selling scenario (product/offer to pitch next plus a few talking
  points) [Q3].

## Initiative Trigger

A one-day CRM-domain hackathon: a working VoIP calling demo already exists
(FreeSWITCH on EC2 plus a browser softphone), and the opportunity is to layer
the AI-assisted call-capture and sales-suggestion experience on top of it
[desc].

## Initial Scope Signal

- **Workflow-selected scope**: `refactor` (workflow-selected) [scope].
- **User-confirmed product boundary** — out of scope for this first version
  [Q4]:
  - Real user authentication / per-RM login and permissions.
  - Production deployment, CI/CD, and monitoring.
  - Integration with a real external CRM system of record — data stays mocked
    locally.
  - A TURN server / production-grade media reliability across NAT.
  - Multi-language speech-to-text — Vietnamese only for the demo.
- **In-scope deliverables** to add on top of the existing calling demo: Figma
  design, database design, Vietnamese speech-to-text integration (with
  caller/callee separation), a Spring Boot backend including an API to download
  the recording `.wav` from the EC2 server, and the UI implementation matching
  the Figma design [desc][Q1][Q4][Q5].
- **Design source**: the BA-provided Figma file the UI must closely match
  (https://www.figma.com/design/ObEPaidO5YFTYB5kxk5hju/Untitled?node-id=0-1) [Q5].
- **Recording-to-call mapping**: recordings on EC2 are currently named by random
  UUID with no pattern, so FreeSWITCH must be configured (guided step by step)
  so each call's recording is named/stored in a way that maps to its call — this
  is an in-scope build task [Q6].
- **Caller/callee separation**: primary approach is to configure FreeSWITCH to
  record the two call legs separately (stereo / per-leg), with speech-to-text
  provider diarization as the fallback [Q7].
- **Database hosting**: the DB runs locally in a Docker container for the demo
  [Q8].
- **Data handling**: mock data is acceptable as long as it populates the UI
  convincingly; the `phone_number` field uses the FreeSWITCH default users [Q4].

## Assumptions & Open Questions

- A Vietnamese speech-to-text provider is available for the demo; the specific
  provider is not yet chosen (speaker separation is primarily handled by
  per-leg FreeSWITCH recording, with provider diarization as fallback) [Q7][assumption].

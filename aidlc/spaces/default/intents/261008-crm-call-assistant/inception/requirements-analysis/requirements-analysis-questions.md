# Requirements Analysis — Questions

## Sources

- [desc] Initial description: "Build a feature that allows an RM (Relationship Manager) to call a customer through the Web using VoIP, then auto-summarize the call and fill the data into a form, and use AI to suggest a selling scenario. ... What needs to be added: Figma design, database design, speech-to-text integration, backend source code (prefer Spring Boot), and UI implementation."
- [scope] Workflow-selected scope: `refactor`.

## Q1. What fields should the auto-filled CRM form contain for the demo?

The AI fills a form from the call. Which fields do you want (I'll infer sensible CRM defaults if you leave it to me)?

A. Customer name, phone number (from FreeSWITCH default users), call date/time, call purpose/topic, customer needs/pain points, outcome/disposition, follow-up action, next-contact date
B. A smaller set — just summary, key points, and follow-up action
C. Up to you — pick sensible CRM fields for the demo
D. Not yet defined
X. Other (please specify)

[Answer]: A. Customer name, phone number (from FreeSWITCH default users), call date/time, call purpose/topic, customer needs/pain points, outcome/disposition, follow-up action, next-contact date — the Figma design is authoritative for the exact fields shown; these are the fallback set. **Mode:** chat

## Q2. For the browser call, do we keep the existing SIP.js WebRTC softphone path (browser → FreeSWITCH over wss) as the call mechanism, or route calls through MicroSIP?

The intent mentioned invoking MicroSIP on the RM machine; the existing working demo is the in-browser SIP.js softphone. Which is the call path for the demo?

A. Keep the existing in-browser SIP.js WebRTC softphone (browser registers to FreeSWITCH over wss) — reuse the proven path
B. Use MicroSIP on the RM machine (the server spawns/controls MicroSIP)
C. Up to you — pick whichever is most reliable for a one-day demo
D. Not yet defined
X. Other (please specify)

[Answer]: B. Use MicroSIP on the RM machine for the call path (discard the in-browser WebRTC softphone) for stability and better sound quality. Recording still occurs on FreeSWITCH, so the recording→STT→AI chain is unaffected. **Mode:** chat

## Q3. Besides the summary, form, and sales suggestion, should the UI also show the transcript (with caller/callee labels)?

A. Yes — show the Vietnamese transcript with caller/callee turns, alongside the summary/form/suggestion
B. No — transcript is internal only; the UI shows summary + form + sales suggestion
C. Up to you
D. Not yet defined
X. Other (please specify)

[Answer]: A. Yes — show the Vietnamese transcript with caller/callee turns alongside the summary/form/suggestion. **Mode:** chat

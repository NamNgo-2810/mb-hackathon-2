# Scope Definition & Prioritization — Questions

## Sources

- [desc] Initial description: "Build a feature that allows an RM (Relationship Manager) to call a customer through the Web using VoIP, then auto-summarize the call and fill the data into a form, and use AI to suggest a selling scenario. ... What needs to be added: Figma design, database design, speech-to-text integration, backend source code (prefer Spring Boot), and UI implementation. This is a CRM-domain hackathon project."
- [scope] Workflow-selected scope: `refactor`.

## Q1. For the one-day demo, which capabilities are Must-have vs. Should/Could-have?

The end-to-end chain is: web call → recording download → Vietnamese speech-to-text (caller/callee separated) → AI summary + auto-filled form + sales suggestion, with a Figma-matched UI.

A. Must: call + recording download API + STT + AI summary + form-fill + Figma-matched UI. Should: sales-scenario suggestion. Could: polish/extras
B. Must: the entire chain including the sales-scenario suggestion (it's a headline feature). Should: visual polish. Could: nothing else
C. Not yet defined
X. Other (please specify)

[Answer]: B. The entire chain is Must-have, including the sales-scenario suggestion (a headline smart task). Visual polish is the only Could-have. **Mode:** chat

## Q2. What is the build sequencing preference?

A. Dependency-first / thin end-to-end slice: get a call → recording → STT → AI → UI skeleton working end to end early, then make the UI match Figma and add the sales suggestion
B. Value-first: build the AI smart tasks and polished UI first, wire real call/recording last
C. Not yet defined
X. Other (please specify)

[Answer]: A. Dependency-first — build a thin end-to-end slice (call → recording → STT → AI → UI skeleton) first, then match the Figma design and sharpen the AI outputs. **Mode:** chat

## Q3. Is there anything in the stated out-of-scope list you'd actually like pulled INTO scope for the demo, or anything else to explicitly exclude?

Current out-of-scope: real auth/login, production deploy + CI/CD + monitoring, external CRM integration (data mocked), TURN server, multi-language STT (Vietnamese only).

A. Keep the out-of-scope list exactly as captured in the intent statement
B. I want to change it (specify what to add/remove)
C. Not yet defined
X. Other (please specify)

[Answer]: A. Keep the out-of-scope list exactly as captured in the intent statement. **Mode:** chat

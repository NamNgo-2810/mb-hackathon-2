# Intent Capture & Framing — Questions

## Sources

- [desc] Initial description: "Build a feature that allows an RM (Relationship Manager) to call a customer through the Web using VoIP, then auto-summarize the call and fill the data into a form, and use AI to suggest a selling scenario. A FreeSWITCH server runs on EC2 with a working calling demo. MicroSIP installed on the RM machine (which opens the web) is invoked to call the customer; after the call, the recording file saved on the EC2 instance is downloaded, a speech-to-text API is called, and an LLM performs the smart tasks (summary, form fill, sales scenario suggestion). Currently only the calling-demo source code exists. What needs to be added: Figma design, database design, speech-to-text integration, backend source code (prefer Spring Boot), and UI implementation. This is a CRM-domain hackathon project."
- [scope] Workflow-selected scope: `refactor`.

## Q1. What does a successful hackathon demo look like? (the one thing judges must see work end to end)

This frames the success metric for the whole build.

A. RM places a call from the web, hangs up, and the app shows an AI-generated summary + filled form + sales suggestion from the real recording
B. The full flow works AND the UI closely matches the BA's Figma design (polished, "stunning")
C. Each piece works in isolation (call, download, speech-to-text, AI) even if not fully wired end to end
D. Not yet defined
X. Other (please specify)

[Answer]: B. The full flow works end to end (call → recording download → Vietnamese speech-to-text with caller/callee separation → AI summary + auto-filled form + sales suggestion) AND the UI closely matches the BA's Figma design. **Mode:** chat

## Q2. Who is the primary user and what pain are we solving for them?

A. A Relationship Manager (RM) at a bank/financial CRM who spends time manually logging call notes and deciding next sales actions — we automate the note-taking and suggest the next sell
B. A call-center agent handling high call volume who needs fast after-call wrap-up
C. Both RM and a supervisor who reviews call outcomes
D. Not yet defined
X. Other (please specify)

[Answer]: A. A Relationship Manager (RM) in a financial CRM who manually logs call notes and decides the next sales action; we automate note-taking/form-fill and suggest the next sell. **Mode:** chat

## Q3. For the AI "smart tasks," which outputs matter for the demo, and roughly what should the suggested selling scenario contain?

A. Call summary + auto-filled CRM form fields + a short recommended sales scenario (e.g., product to pitch + talking points for next contact)
B. Summary + form only; sales suggestion is a nice-to-have
C. Summary + form + sales suggestion + customer sentiment/intent signals
D. Not yet defined
X. Other (please specify)

[Answer]: A. Call summary + auto-filled CRM form fields + a short recommended selling scenario (product/offer to pitch next + a few talking points for the next contact). Speech-to-text must separate caller vs. callee so the summary attributes who said what. **Mode:** chat

## Q4. What should this first version deliberately leave OUT (out of scope for the hackathon)?

(select all that apply)

A. Real user authentication / per-RM login and permissions
B. Production deployment, CI/CD, and monitoring
C. Integration with a real/external CRM system of record (data stays mocked locally)
D. Multi-language speech-to-text (English/one language only for the demo)
E. TURN server / production-grade media reliability across NAT
X. Other (please specify)

[Answer]: A, B, C, E, and X. Speech-to-text is Vietnamese only for the demo (single language). Data is mocked as long as it fills the UI; `phone_number` uses the FreeSWITCH default users. **Mode:** chat

## Assumption Confirmation

One assumption remains in the intent statement (not yet a confirmed fact):

- A Vietnamese speech-to-text provider is available for the demo; the specific provider is not yet chosen (caller/callee separation is primarily handled by per-leg FreeSWITCH recording, with provider diarization as fallback).

Options:

A. Accept assumptions
B. Convert to follow-up questions

[Answer]: A. Accept assumptions

## Q5. Figma design source (the design the UI must match)

[Answer]: https://www.figma.com/design/ObEPaidO5YFTYB5kxk5hju/Untitled?node-id=0-1&p=f&t=9YGcFo6GfD0C5LIG-0 — this is the BA-provided Figma design the UI must closely match. **Mode:** chat

## Q6. Recording-to-call mapping on FreeSWITCH

Recordings are stored on EC2 and downloadable, but named by random UUID with no pattern, so mapping a recording file to its call is currently very hard. How do we solve it?

[Answer]: Configure FreeSWITCH so each call's recording is named/stored in a mappable way (e.g., record filename keyed by the call UUID / a known pattern), with the user making the FreeSWITCH changes guided step by step. This is an accepted in-scope task for the build. **Mode:** chat

## Q7. Caller/callee speaker separation approach

[Answer]: Open to configuring FreeSWITCH to record the two call legs separately (stereo / per-leg recording) as the primary approach for caller/callee separation, as long as it is instructed step by step; speech-to-text provider diarization is the fallback. **Mode:** chat

## Q8. Where to run the database for the demo

[Answer]: Run the DB locally in a Docker container (demo runs locally; no managed/cloud DB needed for the hackathon). **Mode:** chat

# Refined Mockups & UX Design — Questions

## Sources

- [desc] Initial description: "...use AI to suggest a selling scenario. ... What needs to be added: Figma design, ... UI implementation."
- [scope] Workflow-selected scope: `refactor`.

## Q1. I can't open the Figma file directly. How would you like me to match it?

The design must closely match the BA's Figma (https://www.figma.com/design/ObEPaidO5YFTYB5kxk5hju/Untitled?node-id=0-1), but I can't render that link. To produce accurate mockup specs, I need the design detail.

A. You'll paste screenshots of the key Figma frames (I'll read them as images)
B. You'll describe the screens/sections, layout, colors, and components in chat
C. Proceed with my best-guess layout from the requirements (summary + editable form + sales suggestion + Vietnamese transcript), and you'll correct it against Figma at the approval gate
D. Not yet defined
X. Other (please specify)

[Answer]: X. No Figma connector is available in this session (the shipped MCP servers are Context7 + four AWS ones; no Figma MCP). The user supplied the design content for the 3 RM screens as pasted text (MB / Military Bank CRM, Vietnamese). Mockups are specced from that content; the 2 manager screens are deferred to a later pass. **Mode:** chat

## Q2. How many distinct screens/views does the design have, and what are they?

A. One main screen: initiate call + post-call result (summary, form, suggestion, transcript) in one view
B. Two screens: a call/dialer screen and a separate post-call result screen
C. A list/dashboard of calls plus a call-detail screen
D. Up to you — infer a sensible layout from the requirements
X. Other (please specify)

[Answer]: X. 5 screens total: 3 for the RM flow (handled now) + 2 manager results views (deferred). RM screens: (S1) Lead/customer detail — clicking the customer phone number starts the call; (S2) In-call screen with live AI Sales Knowledge Copilot; (S3) Post-call result — Vietnamese transcript with RM/KH turns, AI auto-keyin form (8 fields), AI Sales Performance scorecard, AI Insight + Next Sales Action, .wav download, manual edit, CRM sync. **Mode:** chat

## Q3. Any design-system specifics to honor (colors, fonts, component style)?

A. Follow the Figma's colors/typography exactly (I'll get them from your screenshots/description)
B. No strict system — a clean, modern CRM look is fine
C. Up to you
D. Not yet defined
X. Other (please specify)

[Answer]: A/X. Match the MB design: Vietnamese UI, MB brand palette (deep navy/blue + accent), card-based CRM layout, status/priority badges (⚡ Khai thác ngay, 🔥 Hot Lead, lead-rating N/10). SCOPE DECISION: the live in-call AI Sales Knowledge Copilot (S2) AND the AI Sales Performance scorecard (S3) are IN SCOPE as real LLM-backed features; surrounding CRM data (customer, lead, history, policy) is mocked. All AI outputs are real LLM calls over the (mock or real) transcript/context. **Mode:** chat

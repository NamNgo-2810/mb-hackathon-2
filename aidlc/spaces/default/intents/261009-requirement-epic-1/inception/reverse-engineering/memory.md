<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is kept up to date automatically while the stage runs. Add observations at the review step, not by editing here directly.

## Interpretations
- 2026-10-09T10:48:34Z — The repo is really two codebases: the original Node/Express FreeSWITCH softphone demo (public/ + server/) and a newer Spring Boot "call-assistant" backend already begun under backend/. The CRM call-assistant feature the new intent describes is PARTLY implemented today (recording download + Vietnamese STT with RM/KH stereo split); the LLM summary/form-fill/sales-scenario and Figma UI pieces are not present yet.
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->

## Tradeoffs
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->

## Open questions
- 2026-10-09T10:48:34Z — Live secrets are committed in cleartext: a real Google Cloud STT API key and recording X-API-Key in backend/src/main/resources/application.properties, and the same recording key in the root .env. These contradict the code's own NFR3 "no hardcoded secrets" claim and should be rotated and moved to env/secret manager before the new work builds on this.
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->

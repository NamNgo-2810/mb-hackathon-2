# Code Quality Assessment

## Test Coverage

- **Automated tests**: none. There is no `backend/src/test/` tree, no JUnit
  dependency in `pom.xml`, and no `*.test.js` or test runner in `package.json`.
  The backend README states plainly: "No automated tests ship in this slice
  (hackathon decision)."
- **Coverage config**: absent (no JaCoCo, nyc, etc.).
- **Testability hooks present**: `public/js/ui.js` adds `data-testid`
  attributes to interactive/transcript elements, so a future E2E suite has
  stable selectors — but no suite exists yet.

## Linting / Formatting

- **No linter or formatter configured** — no ESLint/Prettier config, no
  Checkstyle/Spotless/PMD in `pom.xml`, no editorconfig.
- One inline `// eslint-disable-next-line no-unused-vars` in `server/index.js`
  (the Express error middleware signature) — a vestige, with no ESLint actually
  wired.

## CI/CD

- **None for the project.** No project CI workflow is present. The repository's
  `.github/` content is AI-DLC-authored (excluded from this scan), and there is
  no pipeline that builds, tests, or deploys the Node or Spring Boot code.

## Documentation Quality

- **Strong, for a hackathon.**
  - Every backend Java class carries meaningful Javadoc tying behavior to
    requirement IDs (FR1/FR3/FR4/NFR3).
  - Frontend `js/*` modules carry clear module-level comments and explicit
    `TODO (Figma UI, tomorrow)` markers describing the planned redesign seam.
  - Five Markdown guides: root `README.md`, `RUN-DEMO.md`, `backend/README.md`,
    `backend/GOOGLE-STT-SETUP.md`, `freeswitch/RECORDING-SETUP.md`, plus
    `.env.example` files documenting every env key.

## Security Findings

- **HIGH — committed live secrets (contradicts the code's own NFR3).**
  - `backend/src/main/resources/application.properties` contains a real-looking
    Google Cloud Speech-to-Text **API key** and a recording-server
    **`X-API-Key`** as literal default values, plus a hardcoded EC2 recording
    host/IP.
  - The root `.env` contains the same recording API key and the EC2 host, and
    is **not** ignored by the project `.gitignore` with respect to its own
    checked-in state (the `.gitignore` lists `.env`, but a `.env` with a live
    key is present in the working tree).
  - These must be rotated and moved to environment/secret-manager values before
    the new work builds on this code. (Secret values are intentionally not
    reproduced here.)
- **MEDIUM — permissive demo defaults.** `docker-compose.yml` ships a default
  Postgres password; `application.properties` ships default DB creds. Fine for
  a demo, must be overridden for any shared/live environment.
- **Positive controls present**: command-injection-safe MicroSIP invocation
  (`execFile` + argv array + strict `NUMBER_RE`), Bean Validation allow-lists on
  the backend request body, HTML escaping in `ui.js`, a sanitised error envelope
  that omits stack traces, `x-powered-by` disabled, and CORS restricted to the
  configured origins and `GET,POST,OPTIONS`.

## Technical Debt

| Signal | Location | Impact |
|---|---|---|
| No tests / lint / CI | whole repo | No regression safety net for the new feature work. |
| Hardcoded secrets | `application.properties`, root `.env` | Credential exposure; see Security. |
| `ddl-auto=update`/`create-drop`, no migrations | `application*.properties` | Schema drift risk; the new DB-design intent needs Flyway/Liquibase. |
| "latest filename" call↔recording mapping | `CallService.markEnded`, `RecordingClient` | Races under concurrent calls; no real correlation key. |
| FK-by-value `TranscriptTurn.callId` | `domain/TranscriptTurn.java` | No DB-level referential integrity. |
| Whole `.wav` buffered in memory (64 MB cap) | `ApplicationConfig`, `RecordingService` | Not stream-friendly for long calls. |
| Single-stream RM-labeled fallback for non-16-bit-PCM-stereo WAV | `SttService`, `WavStereoSplitter` | Speaker separation silently lost for unexpected formats. |
| Unpinned CDN SIP.js | `public/phone.html` | Supply-chain / availability risk. |
| `TODO (Figma UI, tomorrow)` markers | `public/app.html`, `public/js/*` | Explicitly unfinished UI (the net-new intent work). |
| `R-04` self-noted change | `CallPipelineService.setStatus` comment | Prior `@Transactional` removed as misleading on a self-invoked `@Async` path. |

## Overall

Clean, well-documented, conventionally-structured demo code with sensible
boundary controls — but **unguarded** (no tests/lint/CI) and carrying
**committed live secrets** and **no DB migrations**. These three gaps should be
closed before the new LLM/summary/Figma-UI work is layered on. See
`architecture.md` → Improvement Opportunities.

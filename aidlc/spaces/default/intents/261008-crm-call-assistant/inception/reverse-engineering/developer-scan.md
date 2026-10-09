## Developer Code Scan Results

The repository today is a **single Node.js/Express app** that serves two static
browser pages for placing phone calls through FreeSWITCH, plus two small REST
BFF route groups. There is no build step, no database, no Spring Boot, no AI or
speech code — those are planned additions, not present. The call audio never
touches this server: the WebRTC page registers the browser straight to
FreeSWITCH over SIP-over-WebSocket; the server only serves pages and relays a
few control/status calls.

### Scan Coverage

- **Analyzed deeply**:
  - `package.json`
  - `package-lock.json` (read for resolved dependency versions only)
  - `server/index.js`
  - `server/config.js`
  - `server/upstream.js`
  - `server/routes/callRoutes.js`
  - `server/routes/microsipRoutes.js`
  - `public/phone.html`
  - `public/index.html`
  - `.env`
  - `.gitignore` (project-owned portion; AI-DLC-marked block excluded)
  - `README.md`
- **Skimmed only**: (none — the project source is small enough that every source file was read in full)
- **Left out** (not opened, per "What to Skip" and the AI-DLC-exclusion directive):
  - `node_modules/` (installed dependencies)
  - `.kiro/`, `.claude/`, `.codex/`, `.cursor/`, `.opencode/`, `.aidlc/` and the `aidlc/` workspace (AI-DLC install)
  - AI-DLC-marked sections of `AGENTS.md` and `.gitignore`
  - `.git/` (VCS internals)

### Packages Found

- **fs-call-test** (`package.json` name) — single npm package, `version 1.0.0`, `private: true` — JavaScript (CommonJS, Node.js) — a minimal Express server + static softphone/test pages for calling via FreeSWITCH.
  - `server/` module — Express app wiring, config loading, and route groups.
  - `public/` — static browser pages (no module system server-side; `phone.html` loads SIP.js as an ES module from a CDN).

### Build System

- **Type**: npm (no bundler, no compile step — plain Node.js + static HTML served as-is)
- **Config Files**: `package.json`, `package-lock.json` (lockfileVersion 3), `.env` (runtime config via dotenv)
- **Build Dependencies**: none. Scripts: `start` → `node server/index.js`; `dev` → `node --watch server/index.js`
- **Runtime deps**: `express` (declared `^4.19.2`, resolved `4.22.3`), `dotenv` (declared `^16.4.5`, resolved `16.6.1`). No devDependencies, no test/lint/CI tooling declared.

### APIs Discovered

Server-exposed HTTP endpoints (Express, all under `server/`):

- **Pages / health**
  - `GET /` — serves `public/phone.html` (WebRTC softphone) — `server/index.js`
  - `GET /call-test` — serves `public/index.html` (legacy REST test page) — `server/index.js`
  - `GET /healthz` — `{ ok: true }` liveness — `server/index.js`
  - `express.static` on `public/` for remaining assets
- **Legacy FreeSWITCH BFF** (`server/routes/callRoutes.js`, mounted at `/api`) — proxies to an upstream FreeSWITCH REST API via `server/upstream.js`:
  - `POST /api/call` `{ from, to }` → originates a call; validates extensions against `EXT_RE`; returns upstream `{ uuid }` on 201; maps upstream 400/409/5xx to friendly errors
  - `GET /api/call/:uuid` → call status (uuid validated against `UUID_RE`)
  - `DELETE /api/call/:uuid` → hang up (upstream 404 treated as already ended)
- **MicroSIP desktop control** (`server/routes/microsipRoutes.js`, mounted at `/api`) — spawns a local `microsip.exe`; same-PC only:
  - `POST /api/microsip/call` `{ number }` → dials via MicroSIP (number validated against `NUMBER_RE`, run via `execFile` with array args to avoid shell injection)
  - `POST /api/microsip/hangup` → `/hangupall`
  - `POST /api/microsip/answer` → `/answer`
  - `GET /api/microsip/events` → Server-Sent Events stream to the browser (25s keep-alive)
  - `GET /api/microsip/event?type=&cid=` → ingest point for MicroSIP `cmdCall*` hooks; broadcasts to SSE clients; returns 204
  - `POST /api/microsip/configure` → writes `.cmd` hook scripts and upserts `cmdCall*` keys into `microsip.ini`

Upstream (external) FreeSWITCH REST API consumed by `server/upstream.js` (base from `FS_API_BASE_URL`, auth via `X-API-Key`, 15s timeout):
  - `POST /api/calls`, `GET /api/calls/{uuid}`, `DELETE /api/calls/{uuid}`

Client-side external integrations (`public/phone.html`):
  - SIP.js `SimpleUser` registers to FreeSWITCH over `wss://` (SIP-over-WebSocket) and carries audio via WebRTC; configurable STUN/TURN ICE servers; extensive in-page ICE/RTP diagnostics.

### Frameworks & Libraries

- **Express 4.22.3** — HTTP server / routing (server-side)
- **dotenv 16.6.1** — environment config loading
- **SIP.js 0.21.2** — SIP user agent (client-side, imported from `https://esm.sh` ESM CDN in `phone.html`; not an npm dependency)
- **Node core**: `node:path`, `node:fs`, `node:os`, `node:child_process` (`execFile`), `AbortController`/`fetch` (Node 18+ global fetch assumed)
- Browser APIs: WebRTC (`RTCPeerConnection`), `EventSource` (SSE), `localStorage`

### Test Coverage

- **Test Directories**: none
- **Test Frameworks**: none declared or present
- **Coverage Config**: absent

### Code Quality Indicators

- **Linting**: no linter config present; code carries two `eslint-disable` comments (`server/index.js`), implying ESLint is expected but not configured in-repo
- **CI/CD**: none (no `.github/workflows` project files, no pipeline config)
- **Documentation**: strong `README.md` (setup, FreeSWITCH WSS/WebRTC config, debugging playbook); generous inline comments across server and client code; no API spec (OpenAPI) file

### Technical Debt Signals

- **Secrets committed to the repo**: `.env` is tracked and contains a real-looking `FS_API_KEY` value and a hardcoded EC2 host, despite `.gitignore` listing `.env` (the file predates/overrides the ignore, so it is on disk and in scope). Rotate the key and untrack `.env`.
- **Hardcoded machine-specific absolute path**: `MICROSIP_PATH=C:\Users\xichd\AppData\Local\MicroSIP\MicroSIP.exe` in `.env` ties config to one developer's PC.
- **Windows-only, same-PC-only feature**: the entire MicroSIP route group assumes a local Windows `microsip.exe`, writes `.cmd` scripts, and edits `microsip.ini` — not portable, not server-deployable.
- **No authentication / authorization** anywhere: `from` extension comes from the page; all `/api/*` routes are open. README explicitly notes "Login / per-user credentials … deferred".
- **Client-side dependency on a public CDN** (`esm.sh`) for SIP.js — a network/availability single point of failure for the softphone, with no fallback or pinned local copy.
- **No persistence layer**: SSE client set and call state are in-memory only; nothing survives a restart.
- **Fragile `execFile` error check** in `runMicroSip` — the multi-condition `err.code` guard is hard to reason about and may mis-handle real spawn failures.
- **No tests, linter, or CI** to guard regressions before the planned expansion.
- `.env.example` is referenced in the README "Files" section but was not found at the project root (only `.env` exists).

## Handoff Summary

- **Intent-relevant finding**: The existing codebase is a **calling-demo only** — an Express BFF plus two static pages. Call signaling/audio is browser↔FreeSWITCH direct (SIP.js/WebRTC over `wss://`, `public/phone.html`); the server merely serves pages and relays optional REST/MicroSIP control calls. There is **no backend domain logic, no database, no AI/speech, and no auth** to build on — the planned Spring Boot backend, DB, Vietnamese STT, AI tasks, and Figma UI are all greenfield additions sitting beside (or replacing) this Node demo. The one reusable architectural asset is the proven WebRTC-to-FreeSWITCH registration/media path in `phone.html` and the FreeSWITCH REST contract in `server/upstream.js` (`POST/GET/DELETE /api/calls`).
- **Risks / follow-up**:
  - A live API key and host are committed in `.env` (tracked despite `.gitignore`) — flag for rotation and removal before anything ships.
  - The MicroSIP control path is Windows-/local-only and will not translate to a deployed Spring Boot backend; treat it as demo scaffolding, not a contract to preserve.
  - No auth, tests, linting, or CI exist — the new backend will introduce all of these from scratch; the architect should not assume any safety net around the current code.
  - SIP.js is loaded from a public CDN with a fixed version (0.21.2); a production UI should vendor/pin it. The upstream FreeSWITCH REST API shape (`/api/calls`) is defined externally and only inferred from `upstream.js` — confirm it against the actual FreeSWITCH deployment.

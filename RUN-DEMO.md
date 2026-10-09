# RUN THE FLOW — local demo (core slice)

A step-by-step guide to run the end-to-end core slice on your machine:
**web → MicroSIP call → (hang up) → recording download → Vietnamese STT → transcript**.

The pieces:
- **Node demo server** (existing) — serves the web UI and the MicroSIP control route, port **3000**.
- **Spring Boot backend** (new) — recording download + Vietnamese STT, port **8080**.
- **Postgres** (Docker) — the demo database, port **5432**.
- **MicroSIP** — on your machine, placing the real call.
- **FreeSWITCH on EC2** — records the call, exposes `/api/recordings/latest` and `/api/recordings/download/:name`.

---

## 0. Prerequisites

- Docker Desktop running
- JDK 17 (check: `java -version`)
- Maven (check: `mvn -v`) — or run the backend from your IDE
- Node.js (check: `node -v`) — for the existing demo server
- MicroSIP installed and registered to your FreeSWITCH, as in the current demo

---

## 1. Start Postgres in Docker

From the repo root (`D:\WebProjects\mb-hackathon-2`):

```powershell
docker compose up -d
```

This starts `callassistant-postgres` (Postgres 16) on **localhost:5432** with:
- db `callassistant`, user `callassistant`, password `callassistant` (demo default).

Check it's healthy:

```powershell
docker compose ps
```

Wait until the postgres service shows `healthy`. To see logs: `docker compose logs -f postgres`.
(To stop later: `docker compose down`; to also wipe the DB: `docker compose down -v`.)

### Windows / Docker Desktop notes
- Use **Docker Desktop** with the **WSL2** backend (Settings → General → "Use the
  WSL 2 based engine"). Start Docker Desktop before running `docker compose`.
- Commands are the same `docker compose ...` in **PowerShell** — run them from the
  repo root. If `docker compose` is not found, use the older `docker-compose`.
- If **port 5432** is already in use (a local Postgres service), either stop it,
  or change the mapping in `docker-compose.yml` to e.g. `"5433:5432"` and set
  `DB_URL=jdbc:postgresql://localhost:5433/callassistant`.
- The compose file uses a named volume (`pgdata`), which lives inside the WSL2 VM
  — no Windows path mounting or line-ending issues to worry about.
- First `docker compose up` pulls the Postgres image; allow a minute on first run.

---

## 2. Configure the backend (`application.properties`)

Config lives in `backend\src\main\resources\application.properties` (and
`application-demo.properties` for the H2 demo profile). Each value reads an
environment variable first, falling back to a safe local default — so for a
local run you can just **edit the properties file**. Open
`backend\src\main\resources\application.properties` and set:

```properties
# Database (matches the docker compose Postgres below — defaults already fit)
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/callassistant}
spring.datasource.username=${DB_USERNAME:callassistant}
spring.datasource.password=${DB_PASSWORD:callassistant}

# Your EC2 FreeSWITCH recordings server (the two-endpoint API):
app.recording.base-url=${RECORDING_BASE_URL:https://your-ec2-host}

# Vietnamese STT (see step 4 for a local option):
app.stt.base-url=${STT_BASE_URL:http://localhost:9000}
app.stt.rm-channel=${STT_RM_CHANNEL:left}   # flip to right if RM/KH are reversed
```

Edit the value after each `:` directly, or leave the `${VAR:default}` form and
set the env var — either works, the env var wins. The DB defaults already match
the Docker Postgres in step 1, so for a local demo you usually only set
`app.recording.base-url` and `app.stt.base-url`.

---

## 3. Start the backend

```powershell
cd backend
mvn spring-boot:run
```

Backend comes up on **http://localhost:8080**. First run downloads Maven
dependencies — give it a minute. On start it auto-creates the tables in Postgres.

Smoke-check (new terminal):
```powershell
curl http://localhost:8080/api/calls/1
```
(A 404 is fine — it just means no call yet.)

### Zero-infra alternative (no Postgres, no EC2 needed to see the UI)
If you only want to click through the UI without the DB or real STT:
```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="demo"; mvn spring-boot:run
```
This uses in-memory H2 and seeds mock calls for extensions 1000–1004.

---

## 4. (Optional) Run Vietnamese STT locally with Whisper

If you don't have an STT endpoint yet, run one in Docker:

1. Open `docker-compose.yml`, uncomment the `whisper:` service block.
2. `docker compose up -d` again.
3. Set `STT_BASE_URL=http://localhost:9000` in `backend\.env` (and re-export).

Any server speaking the Whisper `POST /v1/audio/transcriptions` contract works.
For better Vietnamese, bump `ASR_MODEL` to `small` or `medium` (slower).

Without an STT endpoint, the call flow still runs up to transcription, then the
status becomes `FAILED` at the STT step — the recording download still works.

---

## 5. Start the frontend (existing Node demo)

From the repo root, in another terminal:

```powershell
npm install
npm start
```

Serves on **http://localhost:3000**:
- RM view: **http://localhost:3000/app.html**
- Softphone (existing): http://localhost:3000/

Make sure MicroSIP is open and registered (as in the current demo), and that
`MICROSIP_PATH` in the root `.env` points at your `microsip.exe`.

---

## 6. Run one call end-to-end

1. Open **http://localhost:3000/app.html**.
2. Enter a customer number (a FreeSWITCH extension, e.g. `1001`) and a TNB code.
3. Click **Call** → the page opens a call record in the backend and triggers
   MicroSIP to dial. Talk a little on both ends so there's audio on each channel.
4. Click **Kết thúc / End call** → the backend reads `/api/recordings/latest`,
   stores that filename, downloads the `.wav`, splits the two stereo channels,
   and runs Vietnamese STT per channel.
5. The page polls status and, when it reaches `DONE`, shows the **transcript**
   with **RM / KH** turns and a **download .wav** link.

If RM and KH look swapped in the transcript, flip `STT_RM_CHANNEL` to `right`
(re-export + restart the backend) — no code change needed.

---

## 7. Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| Backend won't start, DB error | Postgres not healthy yet (`docker compose ps`), or `DB_*` env not exported in the same shell |
| `Call` does nothing | MicroSIP not running/registered, or `MICROSIP_PATH` wrong in root `.env` |
| Status stuck at `PROCESSING`/`RECORDING_NOT_READY` | Recording not yet written on EC2; the backend retries ~6× — give it a few seconds. Check `RECORDING_BASE_URL` reachable |
| Status `FAILED` right after End | `/api/recordings/latest` unreachable, or STT endpoint not set/running. Check `RECORDING_BASE_URL` and `STT_BASE_URL` |
| Transcript shows one speaker only | The `.wav` wasn't 16-bit PCM stereo, or both legs are on one channel — confirm the recording is true stereo |
| CORS error in browser console | `FRONTEND_ORIGINS` must include `http://localhost:3000` |
| Can't reach EC2 recordings | Expose the two endpoints over HTTPS via nginx — see `freeswitch/RECORDING-SETUP.md` |

---

## 8. Reset between runs

- Stop services: `Ctrl+C` in the backend and Node terminals.
- Stop Postgres: `docker compose down` (keep data) or `docker compose down -v` (wipe DB).

The recording↔call mapping uses the **latest** recording at hang-up, so run one
full call at a time during the demo (end a call before starting the next).

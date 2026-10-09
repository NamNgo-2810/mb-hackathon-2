# FreeSWITCH WebRTC Softphone (demo)

A browser becomes a SIP phone. It registers directly to FreeSWITCH over
**SIP-over-WebSocket** using [SIP.js](https://sipjs.com/) and carries the audio
with WebRTC. No REST API in the call path — the browser talks straight to
FreeSWITCH.

```
Browser (SIP.js + WebRTC)  ──WSS──►  FreeSWITCH (Sofia WSS profile)
         audio (SRTP/DTLS)  ◄──────►
```

- Page: `public/phone.html` (served at `/`). Imports SIP.js as an ES module
  from esm.sh — no build step.
- The old REST test page is still at `/call-test`, and `/api/call` still works
  if the `FS_API_*` env vars are set. Both are optional now.

## Run the web app

```bash
npm install
npm start
# Softphone: http://localhost:3000/
```

The `.env` is only needed for the legacy `/call-test` page. The softphone needs
nothing server-side — all its settings are typed into the page.

---

## Part 1 — FreeSWITCH side (WebSocket + WebRTC)

The browser connects to a **SIP-over-WebSocket** port on FreeSWITCH. The default
`internal` Sofia profile usually already enables both:

- `ws-binding`  → `:5066` (plain WebSocket)
- `wss-binding` → `:7443` (TLS WebSocket)

### 1a. Check what's enabled

SSH to the EC2 box, then:

```bash
fs_cli
# in fs_cli:
sofia status
sofia status profile internal        # look for WS-BINDING / WSS-BINDING lines
```

If you see `WS-BINDING` / `WSS-BINDING` with ports, it's on. If not, enable it.

### 1b. Enable it (if missing)

Edit the internal profile, usually
`/usr/local/freeswitch/conf/sip_profiles/internal.xml`, and ensure these params
exist inside `<settings>`:

```xml
<param name="ws-binding"  value=":5066"/>
<param name="wss-binding" value=":7443"/>
```

Then reload Sofia:

```bash
# in fs_cli:
sofia profile internal restart reloadxml
sofia status profile internal   # confirm the bindings now appear
```

### 1c. Which URL does the browser use? (the important part)

WebRTC needs a **secure context**. The browser enforces this:

| Page is served from | FreeSWITCH WebSocket you can use |
|---|---|
| `http://localhost:3000` (demo) | **`wss://`** to the remote EC2 host. Plain `ws://` to a *remote* host is blocked as insecure. |
| `https://your-app` | `wss://` only |

So for your remote EC2 FreeSWITCH, use **`wss://<ec2-host>:7443`** even during
local development. `ws://` only works if FreeSWITCH is on `localhost` too.

### 1d. The self-signed certificate (no new certs needed)

FreeSWITCH auto-generates a self-signed cert for `wss://` on first run. The
browser will refuse it silently, so **prime it once**:

1. Open `https://<ec2-host>:7443` directly in the browser.
2. You'll get a certificate warning → click **Advanced → Proceed**.
3. Now the browser trusts it for this session, and `wss://<ec2-host>:7443` from
   the softphone page will connect.

(The page shows a reminder about this. It's the one manual step for a demo
without a real cert.)

### 1e. Media / NAT

For a quick same-network demo, direct media often works. On AWS behind NAT you
may get signaling (ringing) but no audio — that's a NAT/media problem, solved
with a TURN server (coturn). For a hackathon demo on the same LAN/VPN you can
usually skip TURN. If audio is one-way or missing, that's the first thing to add.

---

## Part 2 — Using the softphone

Open `http://localhost:3000/` and fill in:

| Field | Example | Notes |
|---|---|---|
| SIP WebSocket URL | `wss://<ec2-host>:7443` | See 1c/1d above |
| SIP domain | `<ec2-host>` or your SIP domain | The part after `@` in the SIP address |
| Extension | `1000` | A FreeSWITCH default user |
| Password | `1234` | The default user's SIP password (see `conf/directory/default.xml` / `vars.xml` `default_password`) |

Click **Register** → the pill turns green ("Registered"). Enter a destination
extension (e.g. `1001`) and click **Call**. Allow microphone access when asked.
Run a second extension (another browser tab, or a softphone like Zoiper) to
answer.

---

## Part 3 — Debugging on the FreeSWITCH server

Everything below runs on the EC2 box.

### See who is registered

This is the fix for `USER_NOT_REGISTERED`:

```bash
fs_cli -x "sofia status profile internal reg"
```

Each registered extension shows with its contact IP. If the one you're calling
isn't listed, no phone is logged in as that extension.

### Turn up logging and watch SIP live

```bash
fs_cli
/log 7                            # DEBUG level
sofia global siptrace on          # print every SIP packet
sofia loglevel all 9
```

Then place a call and watch the INVITE / 100 / 180 / 200 / ACK flow. Where it
stops tells you what failed (auth, codec, registration, media).

### Tail the log file

```bash
tail -f /usr/local/freeswitch/log/freeswitch.log
```

### Visual SIP ladder (best tool)

```bash
sudo sngrep        # install with: sudo apt-get install sngrep  (or yum)
```

`sngrep` draws the SIP dialog as a flow diagram in real time — the quickest way
to see exactly where a call breaks.

### WebSocket-specific checks

```bash
# Is the WS/WSS port actually listening?
sudo ss -lntp | grep -E '5066|7443'

# From your machine, can you reach the WSS port?  (expect a TLS handshake)
#   openssl s_client -connect <ec2-host>:7443
```

Also check the **AWS security group** allows inbound `7443/tcp` (and the RTP
media range, typically `16384-32768/udp`) from your IP.

### Browser side

Open DevTools → Console on the softphone page. The page logs each step
(WebSocket connected, Registered, call events). The on-page **Log** panel shows
the same. Common failures:

| Symptom | Likely cause |
|---|---|
| "Could not connect/register" immediately | Wrong WS URL, port blocked, or self-signed cert not accepted (do step 1d) |
| Registers, call rings, but no audio | NAT/media — add a TURN server |
| 401/403 on register | Wrong extension password |
| Mic permission denied | Page not in a secure context, or user blocked the prompt |

---

## Files

```
public/
  phone.html   WebRTC softphone (SIP.js via ESM) — served at /
  index.html   legacy REST test page — served at /call-test
server/
  index.js     serves the pages; /api/call BFF route (legacy, optional)
  config.js    env (FS_API_* now optional)
  upstream.js  FreeSWITCH REST client (legacy)
  routes/callRoutes.js
.env.example
```

## What's deferred

Login / per-user credentials, a proper TLS cert, a TURN server for reliable
media across NAT, and the recordings UI. For a same-network demo the above is
enough to show a browser-to-browser (or browser-to-softphone) call with audio.

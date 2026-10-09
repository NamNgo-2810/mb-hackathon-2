# FreeSWITCH recordings — the two HTTP endpoints + NGINX over HTTPS

This guide describes the recording setup the Spring Boot backend actually talks
to, and how to expose it to the backend over HTTPS with **NGINX**. You run the
NGINX steps **on the EC2 box** (SSH in).

> **This replaces the earlier per-leg / dialplan approach.** There is **no
> `uuid_record`, no `${uuid}-rm.wav` / `${uuid}-kh.wav`, and no FreeSWITCH REST
> UUID resolution.** The real box records **one stereo `.wav` per call** (the two
> call legs on separate channels) and serves it through two small HTTP endpoints.

## How the mapping works (read this first)

The call↔recording link is **demo-simple**: the filename of the *most recent
recording* is the link. There are no custom SIP headers, no dialplan key, and no
UUID lookup.

1. The RM dials the bare number in MicroSIP as normal.
2. The frontend tells the backend "a call to `<phoneNumber>/<tnbCode>` just
   started" (`POST /api/calls/initiate`) — the record is created `PENDING`.
3. The RM hangs up and clicks **End call** (`POST /api/calls/{id}/ended`).
4. The backend calls **`GET <RECORDING_BASE_URL>/api/recordings/latest`**, gets
   the newest recording's filename, and stores it as the call's
   `recordingName`. That filename **is** the mapping.
5. The backend downloads it with
   **`GET <RECORDING_BASE_URL>/api/recordings/download/<name>`**, splits the one
   stereo `.wav` into its left/right channels, and runs Vietnamese STT per
   channel → RM/KH turns.

> **Timing:** because "latest" is read at hang-up, keep one call's full lifecycle
> (initiate → end → pipeline) before starting the next, so "latest" is that
> call's recording. This is fine for a one-at-a-time demo. The backend also
> retries the download with bounded backoff for a `.wav` that is a second late.

---

## 1. The two endpoints and the recordings directory

The recording server (already running on the box) must expose, under one base:

| Endpoint | Returns |
|---|---|
| `GET /api/recordings/latest` | The most recent recording's **filename**. JSON `{"name":"<file>.wav"}` or a bare string — the backend handles both. |
| `GET /api/recordings/download/:name` | The **`.wav` bytes** for `:name` (`Content-Type: audio/wav`). |

Recordings land on disk in the FreeSWITCH recordings directory, by default:

```
/usr/local/freeswitch/recordings/
```

Each call produces **one stereo `.wav`** there. Confirm FreeSWITCH is recording
in stereo (both legs on separate channels) — this is what lets the backend split
caller from callee. If your profile records mono, enable stereo recording
(e.g. `set RECORD_STEREO=true` in your existing recording path) so the single
file has two channels; the backend's splitter needs 16-bit PCM **stereo**.

Check a file is stereo (sox):

```bash
sudo apt-get install -y sox          # if soxi is missing
soxi /usr/local/freeswitch/recordings/<some-recording>.wav
# expect: Channels = 2
```

---

## 2. NGINX — expose the two endpoints over HTTPS

The backend needs **one HTTPS base URL** (`RECORDING_BASE_URL`) that serves both
`/api/recordings/latest` and `/api/recordings/download/:name`. Do the below on
the EC2 box.

> **Port note (avoid the 8080 collision):** the Spring Boot backend listens on
> **8080**. Do **not** reuse 8080 here. NGINX terminates TLS on **443**; the
> recording server behind it listens on an **illustrative `9100`** below (use
> whatever port your recording server actually uses — just not 8080). If you run
> the recording server and the backend on the same host, keeping them on
> different ports is what prevents the clash.

### 2a. Install NGINX

```bash
# Ubuntu/Debian:
sudo apt-get update && sudo apt-get install -y nginx
# Amazon Linux 2:
# sudo amazon-linux-extras install -y nginx1
# Amazon Linux 2023 / RHEL:
# sudo dnf install -y nginx

sudo systemctl enable nginx
sudo systemctl start nginx
```

### 2b. Create a self-signed TLS cert (fine for a demo)

```bash
sudo mkdir -p /etc/nginx/certs
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /etc/nginx/certs/crm-selfsigned.key \
  -out    /etc/nginx/certs/crm-selfsigned.crt \
  -subj "/CN=$(curl -s http://169.254.169.254/latest/meta-data/public-hostname || echo localhost)"
```

### 2c. Server block — reverse-proxy the two endpoints

Create `/etc/nginx/conf.d/crm.conf` (Amazon Linux) **or**
`/etc/nginx/sites-available/crm` + symlink on Ubuntu:

```nginx
# CRM Call Assistant — HTTPS front for the recordings HTTP API.
# RECORDING_BASE_URL = https://<host>   (NO path suffix — the backend appends
# /api/recordings/latest and /api/recordings/download/<name>).
server {
    listen 443 ssl;
    server_name _;                      # or your EC2 public DNS name

    ssl_certificate     /etc/nginx/certs/crm-selfsigned.crt;
    ssl_certificate_key /etc/nginx/certs/crm-selfsigned.key;

    # Allow large .wav downloads.
    client_max_body_size 100m;

    # Reverse-proxy BOTH recording endpoints to the recording server.
    # 9100 is ILLUSTRATIVE — point proxy_pass at wherever your recording
    # server actually listens. It MUST NOT be 8080 (that is the Spring Boot
    # backend).
    location /api/recordings/ {
        proxy_pass http://127.0.0.1:9100;    # <- the recording server's real host:port (NOT 8080)
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        # Pass the optional API key header through unchanged:
        proxy_set_header X-API-Key         $http_x_api_key;
    }
}

# Optional: redirect plain HTTP to HTTPS.
server {
    listen 80;
    server_name _;
    return 301 https://$host$request_uri;
}
```

> **Alternative — serve the `.wav` files statically.** If the recording server
> does not already implement `download/:name`, you can serve the recordings
> directory directly and keep only `latest` proxied. Replace the download part
> with a static location:
>
> ```nginx
>     # Static .wav files: /api/recordings/download/<name> -> the recordings dir.
>     location /api/recordings/download/ {
>         alias /usr/local/freeswitch/recordings/;   # trailing slash matters
>         autoindex off;
>         types { audio/wav wav; }
>         default_type audio/wav;
>     }
>     # 'latest' still needs the small HTTP endpoint (it has to pick the newest file):
>     location = /api/recordings/latest {
>         proxy_pass http://127.0.0.1:9100/api/recordings/latest;   # NOT 8080
>     }
> ```

### 2d. Open the firewall / AWS security group

Allow inbound **443/tcp** (and 80 if you use the redirect) from your backend's
IP in the EC2 **security group**. On the box, if a local firewall is active:

```bash
sudo ufw allow 443/tcp     # Ubuntu, if ufw is on
```

### 2e. Test NGINX config and reload

```bash
sudo nginx -t && sudo systemctl reload nginx
```

---

## 3. Verify with curl

```bash
# -k accepts the self-signed cert. Add -H "X-API-Key: <key>" only if your
# recording server requires it (then set RECORDING_API_KEY in the backend .env).

# (a) latest filename:
curl -k "https://<ec2-host>/api/recordings/latest"
# expect: {"name":"<something>.wav"}  (or a bare "<something>.wav")

# (b) download that file (substitute the name from (a)):
curl -k -o /tmp/latest.wav "https://<ec2-host>/api/recordings/download/<name>.wav"
soxi /tmp/latest.wav      # expect: Channels = 2 (stereo)
```

---

## 4. Point the backend at this NGINX base

In `backend/.env`:

```bash
# NO path suffix: the backend appends /api/recordings/latest and
# /api/recordings/download/<name> itself.
RECORDING_BASE_URL=https://<ec2-host>
# Only if your recording server requires it (sent as X-API-Key):
RECORDING_API_KEY=
```

So:

| Backend call | Through NGINX to |
|---|---|
| `GET https://<ec2-host>/api/recordings/latest` | recording server `…:9100/api/recordings/latest` |
| `GET https://<ec2-host>/api/recordings/download/<name>` | recording server `…:9100/api/recordings/download/<name>` (or the static `alias`) |

If a channel is reversed in your audio (RM where you expect KH), flip the
backend's `STT_RM_CHANNEL` from `left` to `right` — no code or NGINX change.

---

## 5. Troubleshooting

- **`latest` returns nothing / 404** → no recording exists yet, or the recording
  server isn't writing to the recordings directory. Place a test call, hang up,
  then re-run the `latest` curl.
- **Backend says "not ready"** → the call hasn't ended, the `.wav` hasn't flushed
  yet (the backend retries with backoff), or `RECORDING_BASE_URL` is wrong.
- **502 on `/api/recordings/...`** → `proxy_pass` host:port is wrong, or the
  recording server isn't listening there. Confirm it is **not** 8080.
- **Transcript has only one speaker** → the `.wav` isn't 16-bit PCM **stereo**,
  so the backend couldn't split it and transcribed it as a single stream
  (labeled RM). Enable stereo recording in FreeSWITCH and verify with
  `soxi …` (`Channels = 2`).
- **RM/KH swapped** → set `STT_RM_CHANNEL=right` in the backend `.env`.
- **Wrong recording mapped to a call** → "latest" was read while another call's
  recording was newest. Run one call's full lifecycle before the next in the demo.

# Google Cloud Speech-to-Text — setup (step by step)

The backend now uses **Google Cloud Speech-to-Text** by default
(`app.stt.provider=google`). It calls the REST API
`POST https://speech.googleapis.com/v1/speech:recognize?key=<API_KEY>` with the
mono audio of each stereo channel, `languageCode=vi-VN`, and word time offsets.

You said the **Cloud Speech-to-Text API is already enabled** — so you mainly need
an **API key** restricted to that API.

## 1. Create an API key

1. Go to **Google Cloud Console → APIs & Services → Credentials**
   (https://console.cloud.google.com/apis/credentials), with your project selected.
2. Click **+ CREATE CREDENTIALS → API key**. Copy the key.
3. (Recommended) Click the key → **Edit API key**:
   - **API restrictions → Restrict key → select "Cloud Speech-to-Text API"**.
   - **Application restrictions**: for a local demo "None" is fine; tighten later.
4. Save.

> Note: `speech:recognize` with an API key works for this REST flow. (Service
> accounts / OAuth are the alternative, but the API key is simplest for the demo.)

## 2. Confirm the API is enabled

**APIs & Services → Enabled APIs & services** should list **Cloud Speech-to-Text
API**. If not: **+ ENABLE APIS AND SERVICES → search "Speech-to-Text" → Enable**.

## 3. Point the backend at it

In `backend/src/main/resources/application.properties` (or as env vars):

```properties
app.stt.provider=google
app.stt.base-url=https://speech.googleapis.com
app.stt.api-key=<YOUR_GOOGLE_API_KEY>          
```

Easiest: set the env var in the shell that runs the backend, so the key is not
committed:

```powershell
$env:STT_API_KEY = "AIza...your-key..."
```

Then start the backend (`mvn spring-boot:run`).

## 4. Test the key directly (optional)

```powershell
curl "https://speech.googleapis.com/v1/speech:recognize?key=$env:STT_API_KEY" `
  -H "Content-Type: application/json" `
  -d '{ "config": { "encoding": "LINEAR16", "languageCode": "vi-VN" }, "audio": { "content": "" } }'
```

An empty-audio request returns an empty `results` (HTTP 200) — that confirms the
key and API reach. An `API_KEY_SERVICE_BLOCKED` / 403 means the key restriction
or enablement needs fixing (step 1–2).

## 5. Run a call

End a call in the UI → the backend splits the stereo `.wav` into left/right mono,
sends each channel to Google, and labels RM/KH per `app.stt.rm-channel`.

## Important limits & notes

- **Synchronous `recognize` caps at ~1 minute of audio.** For a short demo call
  this is fine. **For longer calls** Google requires the async
  `longrunningrecognize` flow with the audio in a **GCS bucket** — not wired in
  this slice. If your demo calls run longer than ~1 minute, keep them short, or
  ask me to add the long-audio (GCS + longrunningrecognize) path.
- The audio must be **LINEAR16 PCM WAV** (what FreeSWITCH records and what the
  stereo splitter emits). The request omits `sampleRateHertz` so Google reads it
  from the WAV header.
- Billing must be enabled on the project for Speech-to-Text to return results.
- To switch back to self-hosted Whisper later: set `STT_PROVIDER=whisper` and
  `STT_BASE_URL` to the Whisper endpoint — no code change.

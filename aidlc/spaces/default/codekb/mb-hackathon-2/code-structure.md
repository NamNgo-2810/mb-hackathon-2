# Code Structure

## Repository Layout (project code only)

```
/ (root — Node app "fs-call-test")
  package.json              npm manifest (express, dotenv)
  docker-compose.yml        Postgres (+ optional Whisper) for the backend
  .env / .env.example?      root env (legacy FreeSWITCH REST + MicroSIP)
  server/
    index.js                Express app bootstrap, routing, error handler
    config.js               env loading + domain sanitisation + MicroSIP path
    upstream.js             thin client for the legacy FreeSWITCH REST API
    routes/
      callRoutes.js         legacy /api/call* BFF endpoints
      microsipRoutes.js     /api/microsip/* control + SSE + ini auto-config
  public/
    phone.html              SIP.js WebRTC softphone (served at /)
    index.html              legacy REST test page (served at /call-test)
    app.html                placeholder RM review view (CRM call assistant)
    js/
      config.js             frontend endpoint config (ES module)
      api.js                fetch wrappers (backend + MicroSIP) + poll helper
      ui.js                 DOM rendering (status, transcript, recording link)
  backend/ (Spring Boot "call-assistant")
    pom.xml                 Maven manifest (Spring Boot 3.3.4, Java 17)
    .env.example / .env     backend env (DB, recording, STT)
    src/main/resources/
      application.properties        default profile (Postgres)
      application-demo.properties    demo profile (H2 + seed)
    src/main/java/com/mb/callassistant/
      CallAssistantApplication.java  @SpringBootApplication @EnableAsync
      web/                           controllers + request/response records
      service/                       pipeline, recording, STT logic
      domain/                        JPA entities + enums
      repository/                    Spring Data JPA interfaces
      config/                        properties binding, WebClient, CORS, seeder
```

## File Classification

| Category | Files |
|---|---|
| **Controller/Handler** | `web/CallController.java`; Node `server/routes/callRoutes.js`, `server/routes/microsipRoutes.js`; `server/index.js` (bootstrap + error handler) |
| **Service/UseCase** | `service/CallService.java`, `CallPipelineService.java`, `RecordingService.java`, `SttService.java` |
| **Client/Adapter** | `service/RecordingClient.java`, `GoogleSttClient.java`, `WhisperHttpSttClient.java`; `server/upstream.js` |
| **Utility/Helper** | `service/WavStereoSplitter.java`; `public/js/ui.js`, `public/js/api.js` |
| **Model/Entity** | `domain/CallRecord.java`, `TranscriptTurn.java` |
| **DTO/Record** | `web/CallResponse.java`, `TranscriptTurnResponse.java`, `InitiateCallRequest.java`, `service/SttSegment.java` |
| **Enum** | `domain/SttStatus.java`, `domain/Speaker.java` |
| **Repository/DAO** | `repository/CallRecordRepository.java`, `TranscriptTurnRepository.java` |
| **Error handling** | `web/ApiExceptionHandler.java`, `service/SttException.java`, `RecordingNotReadyException.java`, `service/SttClient.java` (interface) |
| **Configuration** | `config/AppProperties.java`, `ApplicationConfig.java`, `*.properties`, `server/config.js`, `public/js/config.js` |
| **Seed/Fixture** | `config/DemoDataSeeder.java` |
| **Static/View** | `public/phone.html`, `index.html`, `app.html` |

## Code Patterns Observed

- **Layered backend**: controller → service → repository; DTO records at the
  web boundary (`CallResponse.from(...)`, `TranscriptTurnResponse.from(...)`).
- **Strategy for STT**: `SttClient` interface with two beans chosen by
  `@ConditionalOnProperty(app.stt.provider)` (`google` default, `whisper`).
- **Typed configuration**: `@ConfigurationProperties(prefix="app")` binds
  `app.recording.*`, `app.stt.*`, `app.cors.*`; env-wins via `${VAR:default}`.
- **Consistent error envelope**: `@RestControllerAdvice` maps exceptions to a
  `{error:{code,message,details?,timestamp}}` JSON shape; "not ready" → 404 so
  the client can keep polling.
- **Boundary validation**: Jakarta Bean Validation on `InitiateCallRequest`
  (`@NotBlank`/`@Pattern`/`@Size`); regex allow-lists in the Node routes.
- **Injection-safe shell**: MicroSIP invoked via `execFile(path, argsArray)`
  (never a shell string) with a strict `NUMBER_RE` allow-list.
- **Frontend separation of concerns**: `config.js` (endpoints) / `api.js`
  (fetch) / `ui.js` (DOM) — explicitly staged for a Figma redesign (TODO
  markers throughout).
- **Idempotent persistence**: `SttService.transcribe` deletes prior turns for a
  call before saving the fresh set.

Entity/field details are in `component-inventory.md`; endpoint contracts in
`api-documentation.md`; versions in `technology-stack.md`.

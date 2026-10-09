# Technology Stack

## Languages

| Language | Where | Version signal |
|---|---|---|
| Java | `backend/` | `java.version=17` (pom.xml) |
| JavaScript (CommonJS) | `server/` | Node runtime (`node:` core modules used) |
| JavaScript (ES modules) | `public/js/` | native `import`/`export`, no bundler |
| HTML/CSS | `public/*.html` | inline styles, no build step |
| YAML | `docker-compose.yml` | Compose services |

## Backend — Spring Boot (Maven)

Source: `backend/pom.xml`. Managed by `spring-boot-starter-parent:3.3.4`
(versions below inherited from that BOM unless noted).

| Dependency | Version | Purpose |
|---|---|---|
| `spring-boot-starter-parent` | 3.3.4 | BOM / plugin management |
| `spring-boot-starter-web` | (managed by 3.3.4) | REST MVC, embedded Tomcat |
| `spring-boot-starter-validation` | (managed) | Jakarta Bean Validation on request bodies |
| `spring-boot-starter-data-jpa` | (managed) | JPA / Hibernate persistence |
| `spring-boot-starter-webflux` | (managed) | reactive `WebClient` for upstream HTTP |
| `org.postgresql:postgresql` | (managed, runtime) | PostgreSQL JDBC driver |
| `com.h2database:h2` | (managed, runtime) | in-memory DB for the `demo` profile |
| `spring-boot-maven-plugin` | (managed) | runnable jar / `spring-boot:run` |

- **JDK**: 17.
- **Runtime server**: embedded Tomcat (default for `starter-web`), port 8080.
- **Datastore**: PostgreSQL 16 (`postgres:16-alpine` via Compose) default
  profile; H2 (PostgreSQL mode, in-memory) in the demo profile.

## Node demo server (npm)

Source: `package.json`.

| Dependency | Version | Purpose |
|---|---|---|
| `express` | `^4.19.2` | HTTP server / routing |
| `dotenv` | `^16.4.5` | `.env` loading |

- **Scripts**: `start` = `node server/index.js`; `dev` = `node --watch …`.
- No dev/test/lint dependencies declared.

## Frontend libraries

| Library | Version | Source | Purpose |
|---|---|---|---|
| SIP.js | unpinned | ESM import from esm.sh (CDN) | SIP-over-WebSocket signaling + WebRTC in `phone.html` |

- No framework (no React/Vue/etc.); plain DOM + ES modules.
- No build tooling (no Webpack/Vite/tsconfig).

## External services / infrastructure

| Service | Role | Config key |
|---|---|---|
| FreeSWITCH (on EC2) | SIP/WSS registrar + media; recordings HTTP server | `app.recording.base-url`, `FS_*` |
| Google Cloud Speech-to-Text | default Vietnamese STT provider | `app.stt.provider=google`, `app.stt.*` |
| Whisper-compatible server | alternate STT provider | `app.stt.provider=whisper` |
| PostgreSQL 16 | backend datastore (default profile) | `spring.datasource.*` |
| Docker Compose | local Postgres (+ optional Whisper) | `docker-compose.yml` |
| MicroSIP (Windows desktop) | optional local softphone controlled by Node | `MICROSIP_PATH` |

## Observed limitations / unpinned items

- SIP.js is loaded unpinned from a CDN (no version lock, no SRI).
- Node transitive versions are not inspected here (lockfile present:
  `package-lock.json`); only direct deps are documented.

See `dependencies.md` for the dependency relationship graph and
`code-quality-assessment.md` for CI/lint/test posture.

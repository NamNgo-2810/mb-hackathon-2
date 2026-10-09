# Unit Test Instructions — Core Slice

## Decision for this build
No new automated tests are written for this hackathon core slice (user decision).
The `refactor` scope adds no new-test floor, and there is no existing test suite
to keep green. Tests can be added tomorrow.

## When tests are added later (reference)
- Framework: JUnit 5 + Spring Boot Test, MockWebServer for upstream HTTP
  (recording fetch, STT), H2/Testcontainers for the data layer.
- Suggested scoped command (backend module only, not the Node demo):
  ```
  mvn -q -f backend/pom.xml test
  ```
- Target when added (standard strategy): ~5-8 tests per component across the
  recording-download service, STT service, and API endpoints; happy path + at
  least two error/edge cases each.

## Manual verification for the demo (today)
- Backend starts: `mvn -q -f backend/pom.xml spring-boot:run` with env vars set.
- `docker compose up -d` brings up local Postgres.
- Frontend: open the page, click a customer phone number → MicroSIP dials;
  click End → backend runs download + STT; transcript + `.wav` link appear.

// Frontend runtime configuration — the one place to change endpoints.
// No build step: this is a plain ES module imported by app.html.
//
// TODO (Figma UI, tomorrow): keep this file as the single source of endpoints
// so the Figma-matched MB screens reuse the same config without edits.

export const CONFIG = {
  // Spring Boot backend (recording download + Vietnamese STT).
  // Override by editing this value for your environment.
  backendBaseUrl: 'http://localhost:8080',

  // The existing Node demo server (serves this page + the MicroSIP control route).
  // Same origin as this page by default, so an empty string keeps relative URLs.
  microsipBaseUrl: '',

  // How often (ms) the post-call view polls the backend for pipeline status.
  pollIntervalMs: 2000,

  // Give up polling after this many attempts (safety net for the demo).
  pollMaxAttempts: 120,
};

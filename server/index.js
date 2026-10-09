'use strict';

const path = require('node:path');
const express = require('express');
const config = require('./config');
const callRoutes = require('./routes/callRoutes');
const microsipRoutes = require('./routes/microsipRoutes');

const app = express();
app.disable('x-powered-by');
app.use(express.json({ limit: '32kb' }));

app.get('/healthz', (req, res) => res.json({ ok: true }));

// WebRTC softphone (SIP.js) is the default page. The browser registers directly
// to FreeSWITCH over SIP/WSS and carries the audio — this server just serves the
// static page and plays no part in the call.
app.get('/', (req, res) => res.sendFile(path.resolve(__dirname, '../public/phone.html')));

// Legacy REST test page + its BFF route stay available for later development.
app.get('/call-test', (req, res) => res.sendFile(path.resolve(__dirname, '../public/index.html')));

// U1 CRM screens (ADR-002): Node serves S1 (lead detail) and S2 (in-call).
app.get('/screens/1-chi-tiet-tiem-nang-ban.html', (req, res) =>
  res.sendFile(path.resolve(__dirname, '../public/screens/1-chi-tiet-tiem-nang-ban.html')));
app.get('/screens/2-thuc-hien-cuoc-goi.html', (req, res) =>
  res.sendFile(path.resolve(__dirname, '../public/screens/2-thuc-hien-cuoc-goi.html')));
// Shortcut: /lead opens S1 (demo lead when no ?leadId=).
app.get('/lead', (req, res) =>
  res.sendFile(path.resolve(__dirname, '../public/screens/1-chi-tiet-tiem-nang-ban.html')));

// U5 CRM screen: Node serves S5 (lịch làm việc & tác vụ CRM).
app.get('/screens/5-lich-lam-viec.html', (req, res) =>
  res.sendFile(path.resolve(__dirname, '../public/screens/5-lich-lam-viec.html')));
// Shortcut: /calendar opens S5 (highlight 'just-created' via ?sourceCallId=).
app.get('/calendar', (req, res) =>
  res.sendFile(path.resolve(__dirname, '../public/screens/5-lich-lam-viec.html')));

// U6 CRM screens: S0 (đăng nhập) + danh sách lead (chế độ app.auth.mode=login).
app.get('/screens/0-dang-nhap.html', (req, res) =>
  res.sendFile(path.resolve(__dirname, '../public/screens/0-dang-nhap.html')));
app.get('/screens/leads-list.html', (req, res) =>
  res.sendFile(path.resolve(__dirname, '../public/screens/leads-list.html')));
// Shortcuts: /login opens S0, /leads opens the lead list.
app.get('/login', (req, res) =>
  res.sendFile(path.resolve(__dirname, '../public/screens/0-dang-nhap.html')));
app.get('/leads', (req, res) =>
  res.sendFile(path.resolve(__dirname, '../public/screens/leads-list.html')));

app.use('/api', callRoutes);

// MicroSIP control (spawns the local microsip.exe). Same-PC only.
app.use('/api', microsipRoutes);

// Static assets (phone.html, index.html, etc.)
app.use(express.static(path.resolve(__dirname, '../public')));

// Error handler — turn malformed-JSON (and anything else) into a clean JSON
// response instead of leaking an HTML stack trace with internal file paths.
// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  if (err && err.type === 'entity.parse.failed') {
    return res.status(400).json({ error: 'Request body must be valid JSON.' });
  }
  console.error('[server] error:', err && err.message);
  if (res.headersSent) return;
  res.status(500).json({ error: 'Something went wrong.' });
});

app.listen(config.port, () => {
  console.log(`Softphone:  http://localhost:${config.port}/`);
  console.log(`REST test:  http://localhost:${config.port}/call-test`);
  if (config.apiBaseUrl) console.log(`Upstream API: ${config.apiBaseUrl}`);
});

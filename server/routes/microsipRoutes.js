'use strict';

const express = require('express');
const fs = require('node:fs');
const { execFile } = require('node:child_process');
const config = require('../config');

const router = express.Router();

// MicroSIP accepts the same number formats as its dialpad. Keep it tight to
// avoid anything weird reaching the command line. Allow digits, extensions and
// a basic SIP URI form (user@host[:port]).
const NUMBER_RE = /^[A-Za-z0-9_.+*#@:-]{1,128}$/;

// Run microsip.exe with args passed as an array (never a shell string), so a
// user-supplied number can't be used for command injection.
function runMicroSip(args) {
  return new Promise((resolve, reject) => {
    if (!fs.existsSync(config.microsipPath)) {
      const e = new Error('MicroSIP not found at: ' + config.microsipPath);
      e.code = 'ENOENT';
      return reject(e);
    }
    execFile(config.microsipPath, args, { windowsHide: false, timeout: 10000 }, (err) => {
      // MicroSIP returns immediately; it hands the command to the already
      // running instance. A non-zero exit is rare, but surface it if present.
      if (err && err.code !== 0 && err.killed !== true && err.signal == null && err.code != null && typeof err.code === 'number') {
        return reject(err);
      }
      resolve();
    });
  });
}

// POST /microsip/call { number }
router.post('/microsip/call', async (req, res) => {
  const number = typeof req.body?.number === 'string' ? req.body.number.trim() : '';
  if (!NUMBER_RE.test(number)) {
    return res.status(400).json({ error: 'Enter a valid number/extension to dial.' });
  }
  try {
    await runMicroSip([number]);
    return res.json({ ok: true, number, action: 'call' });
  } catch (e) {
    return mapError(res, e);
  }
});

// POST /microsip/hangup — hang up all calls
router.post('/microsip/hangup', async (req, res) => {
  try {
    await runMicroSip(['/hangupall']);
    return res.json({ ok: true, action: 'hangup' });
  } catch (e) {
    return mapError(res, e);
  }
});

// POST /microsip/answer — answer the current incoming call
router.post('/microsip/answer', async (req, res) => {
  try {
    await runMicroSip(['/answer']);
    return res.json({ ok: true, action: 'answer' });
  } catch (e) {
    return mapError(res, e);
  }
});

function mapError(res, e) {
  if (e && e.code === 'ENOENT') {
    return res.status(500).json({
      error: 'MicroSIP executable not found. Set MICROSIP_PATH in .env to microsip.exe, ' +
        'and make sure MicroSIP is installed and running on this PC.',
    });
  }
  console.error('[microsip] error:', e && e.message);
  return res.status(500).json({ error: 'Could not run MicroSIP command.' });
}

// ---------------------------------------------------------------------------
// Live call events from MicroSIP.
//
// MicroSIP can run a command on call lifecycle events (cmdCallStart/End/etc.,
// configured in microsip.ini). We point those at `GET /api/microsip/event`,
// and fan the events out to the browser over Server-Sent Events (SSE).
//
//   MicroSIP --(cmdCallEnd runs curl)--> /api/microsip/event?type=end&cid=NN
//                                               |
//                                       broadcast to SSE clients --> browser
// ---------------------------------------------------------------------------

const sseClients = new Set();

function broadcast(event) {
  const payload = `data: ${JSON.stringify(event)}\n\n`;
  for (const res of sseClients) {
    try { res.write(payload); } catch (_) { /* client gone; cleaned on close */ }
  }
}

// Browser subscribes here to receive call events.
router.get('/microsip/events', (req, res) => {
  res.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    Connection: 'keep-alive',
  });
  res.write('retry: 3000\n\n');
  res.write(`data: ${JSON.stringify({ type: 'connected' })}\n\n`);
  sseClients.add(res);

  // Keep-alive comment every 25s so proxies don't drop the idle connection.
  const ka = setInterval(() => { try { res.write(': keep-alive\n\n'); } catch (_) {} }, 25000);

  req.on('close', () => {
    clearInterval(ka);
    sseClients.delete(res);
  });
});

// MicroSIP's cmd* hooks hit this. GET so a simple `curl <url>` works as the
// configured command. `type` is start|end|incoming|answer; `cid` is caller ID.
router.get('/microsip/event', (req, res) => {
  const type = String(req.query.type || 'unknown');
  const cid = typeof req.query.cid === 'string' ? req.query.cid : '';
  broadcast({ type, cid, at: Date.now() });
  console.log(`[microsip] event: ${type}${cid ? ' cid=' + cid : ''}`);
  // Respond tiny + fast; MicroSIP doesn't care about the body.
  res.status(204).end();
});

// ---------------------------------------------------------------------------
// Auto-configure microsip.ini so the cmd* hooks point back at this server.
// MicroSIP must be closed when this runs (it rewrites its ini on exit).
// ---------------------------------------------------------------------------

const path = require('node:path');
const os = require('node:os');

function microsipIniPath() {
  // microsip.ini lives next to the exe for the portable build, or in
  // %APPDATA%\MicroSIP\microsip.ini for the installer. Prefer the exe folder,
  // fall back to APPDATA.
  const exeDir = path.dirname(config.microsipPath);
  const beside = path.join(exeDir, 'microsip.ini');
  if (fs.existsSync(beside)) return beside;
  const appdata = path.join(os.homedir(), 'AppData', 'Roaming', 'MicroSIP', 'microsip.ini');
  return appdata;
}

// MicroSIP invokes the hook like ShellExecute(file=<cmd>, params=<callerID>).
// So the hook value must be an EXECUTABLE/script, and MicroSIP appends the
// caller ID as an argument — there is no %token% substitution. We therefore
// generate one tiny .cmd per event that forwards both the event type (baked in)
// and %1 (the caller ID MicroSIP appends) to our /event endpoint.
function writeHookScripts(dir, port) {
  const base = `http://127.0.0.1:${port}/api/microsip/event`;
  const events = { start: 'cmdCallStart', end: 'cmdCallEnd', incoming: 'cmdIncomingCall', answer: 'cmdCallAnswer' };
  const paths = {};
  for (const [type, key] of Object.entries(events)) {
    const file = path.join(dir, `msnotify_${type}.cmd`);
    // %~1 = the caller ID MicroSIP passes (unquoted). curl ships with Win10+.
    const script =
      '@echo off\r\n' +
      `curl -s -m 3 "${base}?type=${type}&cid=%~1" >nul 2>&1\r\n`;
    fs.writeFileSync(file, script, 'utf8');
    paths[key] = file;
  }
  return paths;
}

// POST /microsip/configure — generate hook scripts and point microsip.ini at them.
router.post('/microsip/configure', (req, res) => {
  const iniPath = microsipIniPath();
  const dir = path.dirname(iniPath);

  let scripts;
  try {
    scripts = writeHookScripts(dir, config.port);
  } catch (e) {
    return res.status(500).json({ error: 'Could not write hook scripts to ' + dir + ': ' + e.message });
  }

  let content = '';
  try {
    if (fs.existsSync(iniPath)) content = fs.readFileSync(iniPath, 'utf8');
  } catch (e) {
    return res.status(500).json({ error: 'Could not read microsip.ini at ' + iniPath });
  }

  // Upsert each cmd* key = full path to its generated script.
  const lines = content.split(/\r?\n/);
  for (const [key, scriptPath] of Object.entries(scripts)) {
    const idx = lines.findIndex((l) => l.trim().toLowerCase().startsWith(key.toLowerCase() + '='));
    const entry = `${key}=${scriptPath}`;
    if (idx >= 0) lines[idx] = entry;
    else lines.push(entry);
  }
  const next = lines.join('\r\n');

  try {
    fs.writeFileSync(iniPath, next, 'utf8');
  } catch (e) {
    return res.status(500).json({
      error: 'Could not write microsip.ini at ' + iniPath + '. Fully quit MicroSIP first (it rewrites ' +
        'its ini on exit), then try again.',
    });
  }

  return res.json({
    ok: true,
    iniPath,
    scripts,
    note: 'Hooks written. Fully quit MicroSIP (tray icon → Exit) and reopen it so the new settings load.',
  });
});

module.exports = router;

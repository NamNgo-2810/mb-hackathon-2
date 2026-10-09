'use strict';

const express = require('express');
const config = require('../config');
const upstream = require('../upstream');

const router = express.Router();

const EXT_RE = /^[A-Za-z0-9_.+*#-]{1,32}$/;
const UUID_RE = /^[A-Za-z0-9_-]{8,64}$/;

function friendly(res, e) {
  console.error('[call] upstream error:', e.detail || e.message);
  return res.status(502).json({ error: 'Phone system unavailable, try again.' });
}

// POST /call { from, to }
// No login yet, so `from` comes from the page (a FreeSWITCH default extension).
router.post('/call', async (req, res) => {
  const from = typeof req.body?.from === 'string' ? req.body.from.trim() : '';
  const to = typeof req.body?.to === 'string' ? req.body.to.trim() : '';
  if (!EXT_RE.test(from) || !EXT_RE.test(to)) {
    return res.status(400).json({ error: 'Enter valid extensions (e.g. 1000 and 1001).' });
  }

  let result;
  try {
    result = await upstream.startCall({ from, to, ...(config.domain ? { domain: config.domain } : {}) });
  } catch (e) {
    return friendly(res, e);
  }

  if (result.status === 201 && result.body?.uuid) {
    return res.status(201).json(result.body);
  }
  if (result.status === 400) {
    // The extensions passed our local check, so a 400 here is from upstream —
    // surface its actual reason (e.g. "invalid domain") instead of guessing.
    console.warn('[call] upstream 400:', result.body?.error);
    return res.status(400).json({ error: result.body?.error || 'Invalid call request.' });
  }
  if (result.status === 409) {
    console.warn('[call] originate failed:', result.body?.detail);
    return res.status(409).json({ error: 'Could not place the call. Check both phones are registered and not busy.' });
  }
  console.error('[call] unexpected upstream status', result.status);
  return res.status(502).json({ error: 'Phone system unavailable, try again.' });
});

// GET /call/:uuid — status
router.get('/call/:uuid', async (req, res) => {
  const { uuid } = req.params;
  if (!UUID_RE.test(uuid)) return res.status(400).json({ error: 'invalid call id' });
  let result;
  try {
    result = await upstream.callStatus(uuid);
  } catch (e) {
    return friendly(res, e);
  }
  if (result.status === 200 && result.body) return res.json({ uuid, status: result.body.status });
  if (result.status === 400) return res.status(400).json({ error: 'invalid call id' });
  return res.status(502).json({ error: 'Phone system unavailable, try again.' });
});

// DELETE /call/:uuid — hang up (404 treated as already ended)
router.delete('/call/:uuid', async (req, res) => {
  const { uuid } = req.params;
  if (!UUID_RE.test(uuid)) return res.status(400).json({ error: 'invalid call id' });
  let result;
  try {
    result = await upstream.hangup(uuid);
  } catch (e) {
    return friendly(res, e);
  }
  if (result.status === 200 || result.status === 404) return res.json({ uuid, status: 'hungup' });
  if (result.status === 400) return res.status(400).json({ error: 'invalid call id' });
  return res.status(502).json({ error: 'Phone system unavailable, try again.' });
});

module.exports = router;

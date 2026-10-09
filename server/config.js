'use strict';

require('dotenv').config();

// The WebRTC softphone needs no server-side config, so the REST API vars are
// optional now. The legacy /api/call route only works if they're set.
function optional(name) {
  const v = process.env[name];
  return v ? String(v).trim() : '';
}

// The upstream requires `domain` to be a bare host: ^[A-Za-z0-9.-]{1,253}$
// Be forgiving about a mis-set env (e.g. a full URL) by stripping scheme/path/
// port, then drop it entirely if it still doesn't qualify so it can't 400 calls.
function sanitizeDomain(raw) {
  let d = (raw || '').trim();
  if (!d) return '';
  d = d.replace(/^[a-z]+:\/\//i, ''); // strip scheme
  d = d.split('/')[0];                // strip path
  d = d.split(':')[0];                // strip port
  if (!/^[A-Za-z0-9.-]{1,253}$/.test(d)) {
    console.warn(`[config] FS_DOMAIN "${raw}" is not a bare host; ignoring it.`);
    return '';
  }
  return d;
}

// Path to the local MicroSIP executable. The server spawns it to place/hang up
// calls. Only works when the server runs on the same PC as MicroSIP.
// Default to the usual install location; override with MICROSIP_PATH.
const DEFAULT_MICROSIP = 'C:\\Program Files\\MicroSIP\\microsip.exe';

module.exports = {
  apiBaseUrl: optional('FS_API_BASE_URL').replace(/\/+$/, ''),
  apiKey: optional('FS_API_KEY'),
  domain: sanitizeDomain(process.env.FS_DOMAIN),
  port: Number(process.env.PORT || 3000),
  timeoutMs: 15000,
  microsipPath: optional('MICROSIP_PATH') || DEFAULT_MICROSIP,
};

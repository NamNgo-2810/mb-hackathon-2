'use strict';

const config = require('./config');

// Thin client for the FreeSWITCH API. The API key stays here, server-side only.
async function call(method, path, json) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), config.timeoutMs);
  try {
    const res = await fetch(config.apiBaseUrl + path, {
      method,
      signal: controller.signal,
      headers: {
        'X-API-Key': config.apiKey,
        ...(json ? { 'Content-Type': 'application/json' } : {}),
      },
      body: json ? JSON.stringify(json) : undefined,
    });
    let body;
    const text = await res.text();
    if (text) {
      try { body = JSON.parse(text); } catch { body = { error: 'invalid upstream response' }; }
    }
    return { status: res.status, body };
  } catch (err) {
    const isAbort = err && (err.name === 'AbortError' || err.code === 'ABORT_ERR');
    const e = new Error('upstream unreachable');
    e.status = 502;
    e.detail = isAbort ? 'timed out' : (err && err.message) || 'network error';
    throw e;
  } finally {
    clearTimeout(timer);
  }
}

module.exports = {
  startCall: (payload) => call('POST', '/api/calls', payload),
  callStatus: (uuid) => call('GET', `/api/calls/${encodeURIComponent(uuid)}`),
  hangup: (uuid) => call('DELETE', `/api/calls/${encodeURIComponent(uuid)}`),
};

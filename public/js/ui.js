// Rendering helpers — DOM manipulation only, no fetch. Keep this minimal and
// swappable so tomorrow's Figma-matched components can replace it screen by
// screen.
//
// TODO (Figma UI, tomorrow): replace these renderers with the MB design-system
// components for the 3 RM screens. The data shapes (CallResponse, transcript
// turns) are stable, so only the markup here changes.

import { recordingUrl } from './api.js';

/** Set the small status pill text + dot colour. */
export function setStatus(el, text, kind) {
  if (!el) return;
  const dotClass = { ok: 'ok', warn: 'warn', err: 'err', busy: 'warn' }[kind] || '';
  el.innerHTML = `<span class="dot ${dotClass}"></span><span>${escapeHtml(text)}</span>`;
}

/** Show a one-line error message (empty string clears it). */
export function showError(el, message) {
  if (!el) return;
  el.textContent = message || '';
  el.classList.toggle('hidden', !message);
}

/** Human label for a pipeline status. */
export function statusLabel(sttStatus) {
  switch (sttStatus) {
    case 'PENDING': return 'Chờ bắt đầu (Pending)';
    case 'PROCESSING': return 'Đang xử lý ghi âm + phiên âm (Processing)…';
    case 'RECORDING_NOT_READY': return 'Bản ghi âm chưa sẵn sàng, đang chờ… (Recording not ready)';
    case 'DONE': return 'Hoàn tất (Done)';
    case 'FAILED': return 'Lỗi (Failed)';
    default: return sttStatus || 'Unknown';
  }
}

/** Render the Vietnamese transcript as RM/KH turns (FR4). */
export function renderTranscript(container, turns) {
  if (!container) return;
  container.innerHTML = '';
  if (!turns || turns.length === 0) {
    const p = document.createElement('p');
    p.className = 'muted';
    p.textContent = 'Chưa có nội dung phiên âm.';
    container.appendChild(p);
    return;
  }
  for (const t of turns) {
    const row = document.createElement('div');
    row.className = `turn turn-${(t.speaker || '').toLowerCase()}`;
    row.setAttribute('data-testid', `transcript-turn-${t.id}`);

    const who = document.createElement('span');
    who.className = 'who';
    who.textContent = t.speaker === 'RM' ? 'RM' : 'KH';

    const time = document.createElement('span');
    time.className = 'time';
    time.textContent = formatMs(t.startMs);

    const text = document.createElement('span');
    text.className = 'text';
    text.textContent = t.textVi || '';

    row.append(who, time, text);
    container.appendChild(row);
  }
}

/** Render the recording download link for a call (FR3.1). */
export function renderRecordingLink(el, callId) {
  if (!el) return;
  el.innerHTML = '';
  const a = document.createElement('a');
  a.href = recordingUrl(callId);
  a.textContent = '⬇ Tải bản ghi âm (.wav)';
  a.setAttribute('data-testid', 'recording-download-link');
  a.setAttribute('download', `call-${callId}.wav`);
  el.appendChild(a);
}

function formatMs(ms) {
  const total = Math.floor((ms || 0) / 1000);
  const m = String(Math.floor(total / 60)).padStart(2, '0');
  const s = String(total % 60).padStart(2, '0');
  return `${m}:${s}`;
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

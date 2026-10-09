// Thin API wrapper: all network calls to the backend + the Node MicroSIP route
// live here, so the UI layer (ui.js) and the Figma UI tomorrow stay free of
// fetch plumbing.
//
// TODO (Figma UI, tomorrow): the 3 RM screens call these same functions; do not
// inline fetch() in the view code.

import { CONFIG } from './config.js';

// --- Auth token store (chế độ login, U6) -------------------------------------
// Token Bearer được giữ trong sessionStorage (chỉ trong phiên tab trình duyệt).
// Ở chế độ demo không có token nên các request không gắn header Authorization và
// không có chuyển hướng về S0.

const TOKEN_KEY = 'mb.auth.token';

/** Lưu token Bearer sau khi đăng nhập (chế độ login). */
export function setAuthToken(token) {
  try {
    if (token) sessionStorage.setItem(TOKEN_KEY, token);
    else sessionStorage.removeItem(TOKEN_KEY);
  } catch (_) { /* storage không khả dụng: bỏ qua */ }
}

/** Token Bearer hiện tại, hoặc null nếu chưa đăng nhập / chế độ demo. */
export function getAuthToken() {
  try {
    return sessionStorage.getItem(TOKEN_KEY);
  } catch (_) {
    return null;
  }
}

/** Xóa token và đưa người dùng về màn đăng nhập S0 (chỉ gọi ở chế độ login). */
export function clearAuthAndRedirect() {
  setAuthToken(null);
  if (!window.location.pathname.endsWith('/screens/0-dang-nhap.html')) {
    window.location.href = '/screens/0-dang-nhap.html';
  }
}

/** Header chung cho request backend; gắn Authorization khi có token (BR2.2). */
function backendHeaders(extra) {
  const headers = Object.assign({}, extra || {});
  const token = getAuthToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return headers;
}

async function toJsonOrThrow(res) {
  const text = await res.text();
  let body = null;
  if (text) {
    try { body = JSON.parse(text); } catch (_) { body = { raw: text }; }
  }
  if (!res.ok) {
    // Chế độ login: 401 nghĩa là token thiếu/sai/hết hạn → xóa token + về S0
    // (AC4.1.3). Chỉ chuyển hướng khi đang CÓ token (tức chế độ login).
    if (res.status === 401 && getAuthToken()) {
      clearAuthAndRedirect();
    }
    const msg = body && body.error ? (body.error.message || body.error) : `HTTP ${res.status}`;
    const err = new Error(msg);
    err.status = res.status;
    err.body = body;
    throw err;
  }
  return body;
}

// --- Backend (Spring Boot) ---------------------------------------------------

/** Open a call record; returns the created CallResponse (FR1.1). */
export async function initiateCall(phoneNumber, tnbCode) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/calls/initiate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ phoneNumber, tnbCode }),
  });
  return toJsonOrThrow(res);
}

/** Mark a call ended; starts the download → STT pipeline (FR1.3). */
export async function endCall(callId) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/calls/${encodeURIComponent(callId)}/ended`, {
    method: 'POST',
  });
  return toJsonOrThrow(res);
}

/** Fetch the call record + pipeline status. */
export async function getCall(callId) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/calls/${encodeURIComponent(callId)}`);
  return toJsonOrThrow(res);
}

/** Fetch the RM/KH-labeled transcript turns (FR4). */
export async function getTranscript(callId) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/calls/${encodeURIComponent(callId)}/transcript`);
  return toJsonOrThrow(res);
}

/** URL to stream/download the recording .wav (FR3.1). */
export function recordingUrl(callId) {
  return `${CONFIG.backendBaseUrl}/api/calls/${encodeURIComponent(callId)}/recording`;
}

// --- C5: Đăng nhập / đăng xuất & danh sách lead (U6) --------------------------

/**
 * Đăng nhập (C5, BR1.1/BR1.2). Đúng → lưu token Bearer rồi trả
 * { token, expiresAt, rm }. Sai → ném Error (err.status=401) với thông điệp duy
 * nhất "Sai tên đăng nhập hoặc mật khẩu". KHÔNG kích hoạt chuyển hướng 401 ở đây
 * (chưa có token lúc đăng nhập).
 */
export async function login(username, password) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  const body = await toJsonOrThrow(res);
  if (body && body.token) setAuthToken(body.token);
  return body;
}

/** Đăng xuất (C5, BR2.3): thu hồi phiên ở server rồi xóa token cục bộ. */
export async function logout() {
  try {
    await fetch(`${CONFIG.backendBaseUrl}/api/auth/logout`, {
      method: 'POST',
      headers: backendHeaders(),
    });
  } finally {
    setAuthToken(null);
  }
}

/** Danh sách lead của RM hiện tại (C5, BR4.1–BR4.3). Trả mảng LeadSummary ([] khi rỗng). */
export async function listLeads() {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/leads`, {
    headers: backendHeaders(),
  });
  return toJsonOrThrow(res);
}

// --- C1: Lead & call (U1) ----------------------------------------------------

/** RM hiện tại (chế độ demo trả RM mẫu; chế độ login trả RM của token). */
export async function getMe() {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/me`, { headers: backendHeaders() });
  return toJsonOrThrow(res);
}

/** Lead mẫu để mở thẳng S1 khi chưa có U6 (chế độ demo). */
export async function getDemoLead() {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/leads/demo`, { headers: backendHeaders() });
  return toJsonOrThrow(res);
}

/** Chi tiết một lead (BR2.1). */
export async function getLead(leadId) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/leads/${encodeURIComponent(leadId)}`, {
    headers: backendHeaders(),
  });
  return toJsonOrThrow(res);
}

/** Lịch sử hoạt động của lead, mới nhất trước (BR2.2). */
export async function getLeadActivities(leadId) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/leads/${encodeURIComponent(leadId)}/activities`, {
    headers: backendHeaders(),
  });
  return toJsonOrThrow(res);
}

/** Lead kế tiếp (FR4.9). Trả null khi 204 (không còn lead). */
export async function getNextLead(excludeLeadId) {
  const q = excludeLeadId ? `?excludeLeadId=${encodeURIComponent(excludeLeadId)}` : '';
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/leads/next${q}`, { headers: backendHeaders() });
  if (res.status === 204) return null;
  return toJsonOrThrow(res);
}

/** Tạo cuộc gọi từ một lead (FR1.5). Trả Call. */
export async function createCall(leadId) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/calls`, {
    method: 'POST',
    headers: backendHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify({ leadId }),
  });
  return toJsonOrThrow(res);
}

/** Kết thúc cuộc gọi, idempotent (AC1.1.3). Trả Call. */
export async function endCallById(callId) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/calls/${encodeURIComponent(callId)}/ended`, {
    method: 'POST',
    headers: backendHeaders(),
  });
  return toJsonOrThrow(res);
}

// --- C2: Copilot tra cứu chính sách (U3) -------------------------------------

/**
 * Hỏi Copilot một câu hỏi chính sách (FR3.2, FR3.3). Trả về
 * { answerFound, answer, citations[], suggestions[] } (C2). 400 khi câu hỏi
 * không hợp lệ; 502 (err.status) khi AI lỗi/quá giờ — giao diện chỉ hiện trong panel.
 * @param {string} question câu hỏi
 * @param {string|null} callId cuộc gọi đang diễn ra nếu có
 * @param {Array<{role:string,text:string}>} history lịch sử phiên (≤ 10 lượt)
 */
export async function askCopilot(question, callId, history) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/copilot/ask`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      question,
      callId: callId || null,
      history: Array.isArray(history) ? history.slice(-10) : [],
    }),
  });
  return toJsonOrThrow(res);
}

/** Danh sách tài liệu chính sách đã nạp (chân cột lịch sử tra cứu). */
export async function getCopilotDocuments() {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/copilot/documents`);
  return toJsonOrThrow(res);
}

// --- C4: Công việc & lịch (U5) -----------------------------------------------

/**
 * Công việc của RM hiện tại trong khoảng [from, to] (C4, BR4.1/BR4.4). Lọc theo
 * loại (type) và/hoặc cuộc gọi (sourceCallId) tuỳ chọn. Trả mảng Task.
 * @param {{from?:string,to?:string,type?:string,sourceCallId?:string}} params
 */
export async function listTasks(params = {}) {
  const q = new URLSearchParams();
  if (params.from) q.set('from', params.from);
  if (params.to) q.set('to', params.to);
  if (params.type && params.type !== 'ALL') q.set('type', params.type);
  if (params.sourceCallId) q.set('sourceCallId', params.sourceCallId);
  const qs = q.toString();
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/tasks${qs ? `?${qs}` : ''}`);
  return toJsonOrThrow(res);
}

/** Tạo công việc thủ công (C4, S6). Trả Task vừa tạo (201). */
export async function createTask(body) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/tasks`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  return toJsonOrThrow(res);
}

/**
 * Đánh dấu xong và/hoặc đổi giờ (C4, BR5.1/BR5.2). Trả Task. Lỗi 400
 * PAST_NOT_CONFIRMED khi đổi sang quá khứ chưa xác nhận (err.body.error.details).
 * @param {string} taskId
 * @param {{status?:string,dueAt?:string,confirmPast?:boolean}} body
 */
export async function patchTask(taskId, body) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/tasks/${encodeURIComponent(taskId)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  return toJsonOrThrow(res);
}

// --- C3: Phân tích & keyin cuộc gọi (U4) -------------------------------------

/**
 * Kết quả phân tích AI của cuộc gọi (C3, WF3). Trả CallAnalysisDto; 404 khi
 * chưa có (đang xử lý hoặc AI lỗi) — gọi nơi dùng tự bắt err.status===404.
 */
export async function getAnalysis(callId) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/calls/${encodeURIComponent(callId)}/analysis`, {
    headers: backendHeaders(),
  });
  return toJsonOrThrow(res);
}

/**
 * Chạy lại phân tích AI (C3, WF6). 202 Accepted → trả true; 409 (chưa có
 * transcript / đang chạy, err.status===409).
 */
export async function retryAnalysis(callId) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/calls/${encodeURIComponent(callId)}/analysis/retry`, {
    method: 'POST',
    headers: backendHeaders(),
  });
  if (res.status === 202) return true;
  return toJsonOrThrow(res); // ném với err.status (vd 409) để nơi gọi xử lý
}

/** Keyin đã lưu của cuộc gọi (C3). 404 khi chưa lưu. */
export async function getKeyin(callId) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/calls/${encodeURIComponent(callId)}/keyin`, {
    headers: backendHeaders(),
  });
  return toJsonOrThrow(res);
}

/**
 * Lưu hồ sơ cuộc gọi (C3, WF4). Trả { callId, interactionCode, createdTaskIds }.
 * 400 thiếu bắt buộc (err.body.error.details[].field = leadStage|callResult);
 * 403 khác RM.
 * @param {string} callId
 * @param {object} body KeyinSaveDto (leadStage, callResult, fieldSources, nextTasks, ...)
 */
export async function saveKeyin(callId, body) {
  const res = await fetch(`${CONFIG.backendBaseUrl}/api/calls/${encodeURIComponent(callId)}/keyin`, {
    method: 'PUT',
    headers: backendHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(body),
  });
  return toJsonOrThrow(res);
}

/**
 * Poll getCall mỗi 3 giây đến khi pipelineStatus là trạng thái cuối
 * (DONE/NO_RECORDING/STT_FAILED/AI_FAILED) hoặc hết lượt (BR7.3). Dùng cho màn
 * Kết quả cuộc gọi (U4), khác pollUntilReady (dựa trên sttStatus) của slice cũ.
 * @param {string} callId
 * @param {function(object):void} onTick nhận mỗi snapshot CallDto.
 * @returns {Promise<object>} snapshot cuối.
 */
export async function pollCallStatus(callId, onTick) {
  const TERMINAL = ['DONE', 'NO_RECORDING', 'STT_FAILED', 'AI_FAILED'];
  const maxAttempts = CONFIG.pollMaxAttempts;
  for (let i = 0; i < maxAttempts; i++) {
    const call = await getCall(callId);
    if (typeof onTick === 'function') onTick(call);
    if (TERMINAL.includes(call.pipelineStatus)) return call;
    await new Promise((r) => setTimeout(r, 3000));
  }
  throw new Error('Hết thời gian chờ pipeline cuộc gọi.');
}

// --- MicroSIP (existing Node route) ------------------------------------------

/** Place the call via the local MicroSIP client (reuses the existing demo route) (FR1.1). */
export async function microsipCall(number) {
  const res = await fetch(`${CONFIG.microsipBaseUrl}/api/microsip/call`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ number }),
  });
  return toJsonOrThrow(res);
}

/** Hang up all MicroSIP calls. */
export async function microsipHangup() {
  const res = await fetch(`${CONFIG.microsipBaseUrl}/api/microsip/hangup`, { method: 'POST' });
  return toJsonOrThrow(res);
}

// --- Status polling helper ---------------------------------------------------

/**
 * Poll getCall until sttStatus is terminal (DONE/FAILED) or attempts run out.
 * @param {function(object):void} onTick called with each CallResponse snapshot.
 * @returns {Promise<object>} the final CallResponse.
 */
export async function pollUntilReady(callId, onTick) {
  for (let i = 0; i < CONFIG.pollMaxAttempts; i++) {
    const call = await getCall(callId);
    if (typeof onTick === 'function') onTick(call);
    if (call.sttStatus === 'DONE' || call.sttStatus === 'FAILED') return call;
    await new Promise((r) => setTimeout(r, CONFIG.pollIntervalMs));
  }
  throw new Error('Timed out waiting for the transcript to be ready.');
}

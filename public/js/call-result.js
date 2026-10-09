// Màn Kết quả cuộc gọi S3 (U4). Poll pipeline (BR7.3), hiển thị điểm/transcript,
// dự phòng theo loại lỗi (BR7.2), auto-fill keyin + huy hiệu nguồn (BR4.1/BR4.3),
// lưu một-giao-dịch (WF4), chạy lại AI chỉ điền trường trống (BR4.2), điều hướng S4.
import { getCall, getLead, getTranscript, recordingUrl, getAnalysis, getKeyin,
         saveKeyin, retryAnalysis, pollCallStatus, getNextLead } from '/js/api.js';
import { escapeHtml, queryParam } from '/js/screens.js';

const callId = queryParam('callId');
const leadId = queryParam('leadId');

const KEYIN_FIELDS = ['demandAmount', 'term', 'agreedRate', 'product', 'sourceOfFunds',
                      'expectedDate', 'channel', 'objections'];
const CRITERIA_LABELS = {
  CHAO_HOI: '1. Chào hỏi & Nhận diện KH',
  KHAI_THAC_NHU_CAU: '2. Khai thác nhu cầu & Số tiền',
  CHINH_SACH_LAI_SUAT: '3. Nắm vững chính sách & Lãi suất',
  XU_LY_THAC_MAC: '4. Xử lý thắc mắc & Thuyết phục',
  CHOT_CAM_KET: '5. Chốt cam kết & Next Step',
};
const TONE_LABELS = { TICH_CUC: 'Tích cực', TRUNG_TINH: 'Trung tính', TIEU_CUC: 'Tiêu cực' };

let feedbackRating = null;
// Nguồn gợi ý AI theo trường (để tính fieldSources + quy tắc "chỉ điền trường trống").
const aiSuggested = {}; // fieldName -> { value, present:boolean }

function $(sel) { return document.querySelector(sel); }
function testid(id) { return document.querySelector(`[data-testid="${id}"]`); }
function showError(msg) { const el = $('#error-banner'); el.textContent = msg || ''; el.classList.toggle('hidden', !msg); }

function setPipelinePill(status) {
  const map = {
    PROCESSING: ['Đang xử lý…', 'bg-blue-50 text-blue-700 border-blue-200', 'bg-blue-500'],
    DONE: ['Hoàn tất', 'bg-emerald-50 text-emerald-700 border-emerald-200', 'bg-emerald-500'],
    NO_RECORDING: ['Không có ghi âm', 'bg-amber-50 text-amber-700 border-amber-200', 'bg-amber-500'],
    STT_FAILED: ['Lỗi chuyển giọng nói', 'bg-rose-50 text-rose-700 border-rose-200', 'bg-rose-500'],
    AI_FAILED: ['Lỗi phân tích AI', 'bg-rose-50 text-rose-700 border-rose-200', 'bg-rose-500'],
  };
  const [label, cls] = map[status] || ['Đang tải…', 'bg-slate-50 text-slate-600 border-slate-200', 'bg-slate-400'];
  const pill = $('#pipeline-pill');
  pill.className = `inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${cls}`;
  testid('pipeline-status-text').textContent = label;
}

function setStep(step, status) {
  const order = ['FETCH_RECORDING', 'TRANSCRIBE', 'ANALYZE'];
  const reached = status === 'DONE' ? order.length : (order.indexOf(step) + 1 || 0);
  document.querySelectorAll('[data-step]').forEach((el, i) => {
    const dot = el.querySelector('.step-dot');
    dot.className = 'step-dot w-2 h-2 rounded-full ' + (i < reached ? 'bg-blue-600' : 'bg-slate-300');
  });
}

function escapeAttr(v) { return escapeHtml(v); }

function renderAnalysis(a) {
  testid('score-total').textContent = a.totalScore;
  const arc = $('#score-arc');
  arc.setAttribute('stroke-dasharray', `${Math.max(0, Math.min(100, a.totalScore))}, 100`);
  testid('score-grade').textContent = `Hạng ${a.grade || '--'}`;
  testid('score-tone').textContent = TONE_LABELS[a.tone] || a.tone || '--';
  testid('insight-strength').textContent = a.insightStrength || '--';
  testid('insight-improvement').textContent = a.insightImprovement || '--';

  const host = testid('criteria-list');
  host.innerHTML = '';
  (a.criteria || []).forEach((c) => {
    const pct = Math.max(0, Math.min(100, Number(c.score) * 10));
    const row = document.createElement('div');
    row.innerHTML =
      `<div class="flex justify-between text-[11px] mb-1">
         <span class="text-slate-600">${escapeHtml(CRITERIA_LABELS[c.code] || c.name || c.code)}</span>
         <span class="font-bold text-blue-600 font-mono">${escapeHtml(String(c.score))} / 10</span>
       </div>
       <div class="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden"><div class="h-full bg-blue-600 rounded-full" style="width:${pct}%"></div></div>`;
    host.appendChild(row);
  });

  // Auto-fill keyin từ suggestedKeyin, huy hiệu AI · mm:ss (BR4.1).
  const sk = a.suggestedKeyin || {};
  KEYIN_FIELDS.forEach((f) => {
    const sv = sk[f];
    if (sv && sv.value != null && sv.value !== '') {
      aiSuggested[f] = { value: String(sv.value), present: true };
      const input = document.querySelector(`[name="${f}"]`);
      if (input && !input.value) input.value = sv.value;
      setBadge(f, sv.sourceMs);
    }
  });

  // Công việc đề xuất → dòng task (mỗi dòng clientKey sinh ở client).
  const taskHost = testid('task-list');
  taskHost.innerHTML = '';
  (a.suggestedTasks || []).forEach((t) => addTaskRow(t.title, t.type, t.priority));
  if ((a.suggestedTasks || []).length === 0) addTaskRow('', '', '');
}

function setBadge(field, sourceMs) {
  const badge = document.querySelector(`[data-badge="${field}"]`);
  if (!badge) return;
  if (sourceMs != null) {
    const total = Math.floor(sourceMs / 1000);
    const mm = String(Math.floor(total / 60)).padStart(2, '0');
    const ss = String(total % 60).padStart(2, '0');
    badge.textContent = `AI · ${mm}:${ss}`;
  } else {
    badge.textContent = 'AI';
  }
  badge.className = 'field-badge text-[9px] px-1 rounded bg-blue-100 text-blue-700 font-bold';
}

function markEdited(field) {
  const badge = document.querySelector(`[data-badge="${field}"]`);
  if (badge && aiSuggested[field]) { badge.textContent = 'RM đã sửa'; badge.className = 'field-badge text-[9px] px-1 rounded bg-amber-100 text-amber-700 font-bold'; }
}

function computeFieldSources() {
  const sources = {};
  KEYIN_FIELDS.forEach((f) => {
    const input = document.querySelector(`[name="${f}"]`);
    const val = input ? String(input.value || '') : '';
    const ai = aiSuggested[f];
    if (ai && ai.present) {
      sources[f] = (val === String(ai.value)) ? 'AI_KEPT' : 'RM_EDITED';
    } else if (val) {
      sources[f] = 'RM_ENTERED';
    }
  });
  return sources;
}

let taskSeq = 0;
function addTaskRow(title, type, priority) {
  const clientKey = `task-${Date.now()}-${taskSeq++}`;
  const host = testid('task-list');
  const row = document.createElement('div');
  row.className = 'border border-slate-200 rounded p-2 space-y-1';
  row.dataset.clientKey = clientKey;
  row.innerHTML =
    `<input data-field="title" aria-label="Tiêu đề công việc" placeholder="Tiêu đề công việc" value="${escapeAttr(title || '')}" class="w-full border border-slate-200 rounded px-2 py-1 text-xs" data-testid="task-title" />
     <div class="grid grid-cols-3 gap-1">
       <select data-field="type" aria-label="Loại công việc" class="border border-slate-200 rounded px-1 py-1 text-[11px]">
         <option value="NHAC_GOI" ${type==='NHAC_GOI'?'selected':''}>Nhắc gọi</option>
         <option value="LICH_HEN" ${type==='LICH_HEN'?'selected':''}>Lịch hẹn</option>
         <option value="TASK_ZALO" ${type==='TASK_ZALO'?'selected':''}>Task Zalo</option>
       </select>
       <select data-field="priority" aria-label="Ưu tiên" class="border border-slate-200 rounded px-1 py-1 text-[11px]">
         <option value="THUONG" ${priority==='THUONG'?'selected':''}>Thường</option>
         <option value="CAO" ${priority==='CAO'?'selected':''}>Cao</option>
       </select>
       <input data-field="dueAt" type="datetime-local" aria-label="Hạn" class="border border-slate-200 rounded px-1 py-1 text-[11px]" />
     </div>`;
  host.appendChild(row);
}

function collectTasks() {
  const list = [];
  document.querySelectorAll('#task-list > div').forEach((row) => {
    const title = row.querySelector('[data-field="title"]').value.trim();
    if (!title) return;
    const type = row.querySelector('[data-field="type"]').value;
    const priority = row.querySelector('[data-field="priority"]').value;
    const dueLocal = row.querySelector('[data-field="dueAt"]').value;
    // dueAt bắt buộc với createFromCall; mặc định +1 ngày nếu RM chưa chọn.
    const due = dueLocal ? new Date(dueLocal) : new Date(Date.now() + 86400000);
    list.push({
      clientKey: row.dataset.clientKey,
      title, type, priority,
      dueAt: toOffsetIso(due),
      durationMinutes: 30,
      aiSuggested: true,
    });
  });
  return list;
}

// ISO-8601 với offset +07:00 (BR8.3).
function toOffsetIso(d) {
  const vn = new Date(d.getTime() + 7 * 3600000);
  const p = (n) => String(n).padStart(2, '0');
  return `${vn.getUTCFullYear()}-${p(vn.getUTCMonth()+1)}-${p(vn.getUTCDate())}T${p(vn.getUTCHours())}:${p(vn.getUTCMinutes())}:${p(vn.getUTCSeconds())}+07:00`;
}

function renderTranscript(turns) {
  const host = testid('transcript-list');
  host.innerHTML = '';
  (turns || []).forEach((t) => {
    const isRm = t.speaker === 'RM';
    const row = document.createElement('div');
    row.className = 'flex items-start gap-2';
    row.setAttribute('data-testid', `transcript-turn-${escapeAttr(t.id)}`);
    const total = Math.floor((t.startMs || 0) / 1000);
    const time = `${String(Math.floor(total/60)).padStart(2,'0')}:${String(total%60).padStart(2,'0')}`;
    row.innerHTML =
      `<span class="w-6 h-6 rounded-full ${isRm?'bg-blue-100 text-blue-700':'bg-purple-100 text-purple-700'} font-bold text-[10px] flex items-center justify-center shrink-0 mt-0.5">${isRm?'RM':'KH'}</span>
       <div class="flex-1 ${isRm?'bg-blue-50/50 border-blue-100':'bg-slate-50 border-slate-200'} border rounded-xl rounded-tl-none p-2.5">
         <div class="flex justify-between items-center mb-1"><span class="font-bold text-[11px] ${isRm?'text-blue-900':'text-purple-900'}">${isRm?'RM':'Khách hàng'}</span><span class="text-[10px] text-slate-400 font-mono">${time}</span></div>
         <p class="text-slate-700 leading-relaxed text-[11px]">${escapeHtml(t.textVi || '')}</p>
       </div>`;
    host.appendChild(row);
  });
}

function showScoreFallback(message) {
  testid('col-score').querySelector('.flex-1').classList.add('hidden');
  const fb = testid('score-fallback');
  fb.classList.remove('hidden');
  fb.textContent = message;
}

function showTranscriptFallback(message) {
  const fb = testid('transcript-fallback');
  fb.classList.remove('hidden');
  fb.textContent = message;
}

function setupAudio(hasRecording) {
  const audio = testid('recording-audio');
  const dl = testid('download-wav');
  if (hasRecording) {
    audio.src = recordingUrl(callId);
    dl.href = recordingUrl(callId);
    dl.setAttribute('download', `call-${callId}.wav`);
    dl.classList.remove('hidden');
  } else {
    audio.classList.add('hidden');
  }
}

async function loadAnalysisAndKeyin() {
  try {
    const a = await getAnalysis(callId);
    renderAnalysis(a);
    testid('score-fallback').classList.add('hidden');
  } catch (e) {
    if (e.status === 404) showScoreFallback('Chưa có điểm AI.');
    else showError(e.message || 'Không tải được kết quả phân tích');
  }
  // Keyin đã lưu (nếu có) — hiển thị lại.
  try {
    const k = await getKeyin(callId);
    applySavedKeyin(k);
  } catch (_) { /* 404: chưa lưu, bỏ qua */ }
}

function applySavedKeyin(k) {
  const set = (name, val) => { const el = document.querySelector(`[name="${name}"]`); if (el && val != null) el.value = val; };
  set('leadStage', k.leadStage); set('callResult', k.callResult); set('demandAmount', k.demandAmount);
  set('term', k.term); set('agreedRate', k.agreedRate); set('product', k.product);
  set('sourceOfFunds', k.sourceOfFunds); set('expectedDate', k.expectedDate); set('channel', k.channel);
  set('objections', k.objections); set('rmNote', k.rmNote);
  if (k.feedbackRating) selectFeedback(k.feedbackRating);
  if (k.feedbackComment) set('feedbackComment', k.feedbackComment);
}

function applyFallbackByStatus(status) {
  // BR7.2 — dự phòng theo loại lỗi; form keyin trống vẫn lưu được.
  if (status === 'NO_RECORDING') {
    showScoreFallback('Chưa có điểm AI — không có ghi âm.');
    setupAudio(false);
    showTranscriptFallback('Không tìm thấy ghi âm cho cuộc gọi này.');
  } else if (status === 'STT_FAILED') {
    showScoreFallback('Chưa có điểm AI — chưa có transcript.');
    setupAudio(true);
    showTranscriptFallback('Không chuyển được giọng nói thành văn bản.');
  } else if (status === 'AI_FAILED') {
    showScoreFallback('Chưa có điểm AI.');
    setupAudio(true);
    testid('retry-analysis').classList.remove('hidden');
    loadTranscriptOnly();
  }
}

async function loadTranscriptOnly() {
  try { renderTranscript(await getTranscript(callId)); } catch (_) { /* bỏ qua */ }
}

async function onTerminal(call) {
  const status = call.pipelineStatus;
  setStep(call.pipelineStep, status);
  if (status === 'DONE') {
    setupAudio(true);
    await loadTranscriptOnly();
    await loadAnalysisAndKeyin();
  } else {
    applyFallbackByStatus(status);
  }
}

// --- Feedback scale ---------------------------------------------------------
function buildFeedback() {
  const host = testid('feedback-scale');
  for (let i = 1; i <= 10; i++) {
    const b = document.createElement('button');
    b.type = 'button'; b.textContent = i; b.dataset.rating = i;
    b.setAttribute('data-testid', `feedback-${i}`); b.setAttribute('aria-label', `Đánh giá AI ${i} điểm`);
    b.className = 'h-7 rounded text-[11px] font-bold text-slate-700 bg-white hover:bg-slate-100 border border-slate-200';
    b.addEventListener('click', () => selectFeedback(i));
    host.appendChild(b);
  }
}
function selectFeedback(rating) {
  feedbackRating = rating;
  testid('feedback-scale').querySelectorAll('button').forEach((b) => {
    const on = Number(b.dataset.rating) === rating;
    b.className = `h-7 rounded text-[11px] font-bold border ${on ? 'text-white bg-blue-600 border-blue-600' : 'text-slate-700 bg-white hover:bg-slate-100 border-slate-200'}`;
  });
}

// --- Save (WF4) -------------------------------------------------------------
async function doSave() {
  const btn = testid('save-keyin');
  const leadStageEl = document.querySelector('[name="leadStage"]');
  const callResultEl = document.querySelector('[name="callResult"]');
  showError('');
  // Kiểm hai trường bắt buộc phía client; thiếu → cuộn tới, giữ dữ liệu (AC2.3.5).
  if (!leadStageEl.value) { leadStageEl.focus(); showError('Vui lòng chọn giai đoạn lead.'); return; }
  if (!callResultEl.value.trim()) { callResultEl.focus(); showError('Vui lòng nhập kết quả cuộc gọi.'); return; }

  const num = (name) => { const v = document.querySelector(`[name="${name}"]`).value; return v === '' ? null : v; };
  const body = {
    leadStage: leadStageEl.value,
    callResult: callResultEl.value.trim(),
    demandAmount: num('demandAmount') == null ? null : Number(num('demandAmount')),
    term: num('term'),
    agreedRate: num('agreedRate') == null ? null : Number(num('agreedRate')),
    product: num('product'),
    sourceOfFunds: num('sourceOfFunds'),
    expectedDate: num('expectedDate'),
    channel: num('channel'),
    objections: num('objections'),
    rmNote: num('rmNote'),
    fieldSources: computeFieldSources(),
    nextTasks: collectTasks(),
    feedbackRating,
    feedbackComment: document.querySelector('[name="feedbackComment"]').value || null,
  };
  btn.disabled = true; // tránh bấm hai lần (AC2.3.6)
  try {
    await saveKeyin(callId, body);
    openS4();
  } catch (e) {
    const field = e.body && e.body.error && e.body.error.details && e.body.error.details[0];
    showError((field ? `Thiếu/không hợp lệ: ${field.field}. ` : '') + (e.message || 'Không lưu được hồ sơ'));
    btn.disabled = false;
  }
}

// --- Retry AI (WF6, BR4.2: chỉ điền trường trống) ---------------------------
async function doRetry() {
  const btn = testid('retry-analysis');
  btn.disabled = true;
  try {
    await retryAnalysis(callId);
    const call = await pollCallStatus(callId, (c) => { setPipelinePill(c.pipelineStatus); setStep(c.pipelineStep, c.pipelineStatus); });
    if (call.pipelineStatus === 'DONE') {
      btn.classList.add('hidden');
      await applyRetryResult();
    } else {
      applyFallbackByStatus(call.pipelineStatus);
    }
  } catch (e) {
    showError(e.status === 409 ? 'Chưa thể chạy lại (chưa có transcript hoặc đang chạy).' : (e.message || 'Chạy lại thất bại'));
  } finally {
    btn.disabled = false;
  }
}

async function applyRetryResult() {
  try {
    const a = await getAnalysis(callId);
    renderCriteriaAndInsights(a);
    // Chỉ điền trường RM chưa nhập (BR4.2).
    let filled = 0;
    const sk = a.suggestedKeyin || {};
    KEYIN_FIELDS.forEach((f) => {
      const input = document.querySelector(`[name="${f}"]`);
      const sv = sk[f];
      if (input && !input.value && sv && sv.value != null && sv.value !== '') {
        input.value = sv.value;
        aiSuggested[f] = { value: String(sv.value), present: true };
        setBadge(f, sv.sourceMs);
        filled++;
      }
    });
    const note = testid('ai-fill-note');
    note.textContent = `Đã điền ${filled} trường từ AI.`;
    note.classList.remove('hidden');
    testid('score-fallback').classList.add('hidden');
    testid('col-score').querySelector('.flex-1').classList.remove('hidden');
  } catch (_) { /* bỏ qua */ }
}

function renderCriteriaAndInsights(a) { renderAnalysis(a); }

// --- S4 dialog --------------------------------------------------------------
function openS4() { testid('s4-dialog').classList.remove('hidden'); }
function wireS4() {
  testid('s4-next-lead').addEventListener('click', async () => {
    try {
      const next = await getNextLead(leadId);
      if (!next) { const n = testid('s4-note'); n.textContent = 'Bạn đã liên hệ hết tiềm năng bán cần xử lý.'; n.classList.remove('hidden'); setTimeout(() => { window.location.href = '/screens/leads-list.html'; }, 1200); return; }
      window.location.href = `/screens/1-chi-tiet-tiem-nang-ban.html?${new URLSearchParams({ leadId: next.leadId }).toString()}`;
    } catch (e) { showError(e.message || 'Không lấy được lead kế tiếp'); }
  });
  testid('s4-calendar').addEventListener('click', () => {
    window.location.href = `/screens/5-lich-lam-viec.html?${new URLSearchParams({ sourceCallId: callId }).toString()}`;
  });
  testid('s4-home').addEventListener('click', () => { window.location.href = '/screens/leads-list.html'; });
}

// --- Init -------------------------------------------------------------------
async function init() {
  if (!callId) { showError('Thiếu callId'); return; }
  buildFeedback();
  wireS4();
  testid('add-task').addEventListener('click', () => addTaskRow('', '', ''));
  testid('save-keyin').addEventListener('click', doSave);
  testid('retry-analysis').addEventListener('click', doRetry);
  KEYIN_FIELDS.forEach((f) => {
    const el = document.querySelector(`[name="${f}"]`);
    if (el) el.addEventListener('input', () => markEdited(f));
  });

  // Mã TNB ở header.
  if (leadId) {
    try { const lead = await getLead(leadId); testid('s3-lead-code').textContent = lead.leadCode || leadId; } catch (_) { testid('s3-lead-code').textContent = leadId; }
  }

  try {
    const first = await getCall(callId);
    setPipelinePill(first.pipelineStatus);
    setStep(first.pipelineStep, first.pipelineStatus);
    const terminal = ['DONE', 'NO_RECORDING', 'STT_FAILED', 'AI_FAILED'];
    if (terminal.includes(first.pipelineStatus)) {
      await onTerminal(first);
    } else {
      const call = await pollCallStatus(callId, (c) => { setPipelinePill(c.pipelineStatus); setStep(c.pipelineStep, c.pipelineStatus); });
      await onTerminal(call);
    }
  } catch (e) {
    showError(e.message || 'Không tải được trạng thái cuộc gọi');
  }
}

init();

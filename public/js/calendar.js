// Lịch làm việc & Tác vụ CRM (S5/S6, U5). Chỉ gọi API qua api.js; mọi dữ liệu
// động được escape trước khi chèn vào innerHTML (NFR tránh XSS). Múi giờ nghiệp
// vụ là Asia/Ho_Chi_Minh / offset +07:00 (BR6.1); phần giao diện chỉ hiển thị,
// server là nơi thực thi quy tắc.

import { getMe, listTasks, createTask, patchTask } from './api.js';
import { escapeHtml, queryParam } from './screens.js';

// ── Hằng số khung giờ (BR4.2) ──
const DAY_START_HOUR = 8;   // 08:00
const DAY_END_HOUR = 17;    // 17:00
const VN_OFFSET = '+07:00';

const TYPE_LABELS = {
  LICH_HEN: 'Lịch hẹn',
  NHAC_GOI: 'Nhắc gọi',
  TASK_ZALO: 'Task Zalo',
  KHAC: 'Khác',
};
const PRIORITY_LABELS = { CAO: 'Cao', THUONG: 'Thường' };
const WEEKDAY_LABELS = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];

// ── Trạng thái màn ──
const state = {
  view: 'WEEK',       // DAY | WEEK | MONTH | LIST
  filter: 'ALL',      // ALL | LICH_HEN | NHAC_GOI | TASK_ZALO
  anchor: new Date(), // ngày neo của khoảng đang xem
  tasks: [],
  sourceCallId: null, // highlight 'just-created'
  dialogMode: 'create', // create | reschedule
  rescheduleTaskId: null,
};

// ───────────────────────── Pure helpers (kiểm thử được) ─────────────────────

/** Thứ 2 (00:00) của tuần chứa `date`. */
export function startOfWeek(date) {
  const d = new Date(date);
  const day = (d.getDay() + 6) % 7; // 0 = Thứ 2
  d.setDate(d.getDate() - day);
  d.setHours(0, 0, 0, 0);
  return d;
}

/** Khoảng [from, to] cho một chế độ xem quanh ngày neo. */
export function rangeFor(view, anchor) {
  const a = new Date(anchor);
  if (view === 'DAY') {
    const from = new Date(a); from.setHours(0, 0, 0, 0);
    const to = new Date(a); to.setHours(23, 59, 59, 999);
    return { from, to };
  }
  if (view === 'MONTH') {
    const from = new Date(a.getFullYear(), a.getMonth(), 1, 0, 0, 0, 0);
    // Giữ ≤ 42 ngày: lấy đúng từ đầu tháng tới cuối tháng.
    const to = new Date(a.getFullYear(), a.getMonth() + 1, 0, 23, 59, 59, 999);
    return { from, to };
  }
  // WEEK và LIST dùng khoảng tuần (T2–CN để không bỏ sót việc Chủ nhật 'Ngoài giờ').
  const from = startOfWeek(a);
  const to = new Date(from);
  to.setDate(from.getDate() + 6);
  to.setHours(23, 59, 59, 999);
  return { from, to };
}

/** ISO-8601 có offset +07:00 từ một Date (không đổi mốc thời gian). */
export function toIsoVn(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T`
    + `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}${VN_OFFSET}`;
}

/** True khi `dueAt` nằm ngoài khung T2–T7 08:00–17:00 (gom 'Ngoài giờ', BR4.2). */
export function isOutOfHours(dueAt) {
  const d = new Date(dueAt);
  const weekday = (d.getDay() + 6) % 7; // 0 = T2 … 6 = CN
  if (weekday > 5) return true;         // Chủ nhật
  const h = d.getHours();
  return h < DAY_START_HOUR || h >= DAY_END_HOUR;
}

/** Chỉ số cột ngày (0 = T2 … 5 = T7); -1 nếu là Chủ nhật. */
export function dayColumn(dueAt) {
  const weekday = (new Date(dueAt).getDay() + 6) % 7;
  return weekday <= 5 ? weekday : -1;
}

/**
 * Tách danh sách công việc theo trạng thái cho chế độ Danh sách (BR4.5): nhóm
 * 'đã làm' (DONE) và 'sắp tới' (TODO), mỗi nhóm sắp theo dueAt tăng dần.
 */
export function splitByStatus(tasks) {
  const byDue = (a, b) => new Date(a.dueAt) - new Date(b.dueAt);
  return {
    upcoming: tasks.filter((t) => t.status !== 'DONE').sort(byDue),
    done: tasks.filter((t) => t.status === 'DONE').sort(byDue),
  };
}

/** HH:mm hiển thị từ ISO. */
function hhmm(iso) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** dd/MM hiển thị từ ISO/Date. */
function ddmm(value) {
  const d = new Date(value);
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}`;
}

// ───────────────────────── DOM helpers ─────────────────────────

const $ = (sel) => document.querySelector(sel);
const $testid = (id) => document.querySelector(`[data-testid="${id}"]`);

function showError(message) {
  const banner = $('#error-banner');
  $('[data-testid="error-message"]').textContent = message;
  banner.classList.remove('hidden');
}

function clearError() {
  $('#error-banner').classList.add('hidden');
}

function showToast(message) {
  const toast = $('#toast');
  $('#toast-message').textContent = message;
  toast.classList.remove('hidden');
  setTimeout(() => toast.classList.add('hidden'), 5000);
}

/** Thẻ công việc dùng trong lưới/danh sách; mọi text đã escape. */
function taskCardHtml(task, extraClass = '') {
  const done = task.status === 'DONE';
  const justCreated = state.sourceCallId && task.sourceCallId === state.sourceCallId;
  const classes = [
    'relative rounded-xl p-2 shadow-xs bg-white border border-slate-200',
    done ? 'task-done bg-slate-50' : '',
    justCreated ? 'just-created' : '',
    extraClass,
  ].filter(Boolean).join(' ');
  const prio = PRIORITY_LABELS[task.priority] || '';
  const typeLabel = TYPE_LABELS[task.type] || escapeHtml(task.type);
  const leadLine = task.customerName
    ? `<p class="text-[10px] text-slate-500 truncate">${escapeHtml(task.customerName)}`
      + (task.leadCode ? ` <span class="font-mono">(${escapeHtml(task.leadCode)})</span>` : '')
      + `</p>`
    : '';
  const summary = task.aiSummary
    ? `<p class="text-[10px] text-slate-600 bg-blue-50/70 rounded p-1 mt-1 line-clamp-2">`
      + `<span class="font-semibold text-blue-700">Tóm tắt AI:</span> ${escapeHtml(task.aiSummary)}</p>`
    : '';
  const justCreatedBadge = justCreated
    ? `<span class="inline-flex items-center gap-1 text-[9px] font-bold uppercase bg-blue-100 text-blue-800 px-1.5 py-0.5 rounded" data-testid="just-created-badge">Vừa lưu từ cuộc gọi</span>`
    : '';
  // Nút 'Gọi ngay' / 'Chi tiết TNB' chỉ hiện khi có lead (điều hướng U1, BR5.3).
  const navButtons = task.leadId
    ? `<button type="button" class="px-2 py-1 rounded-lg bg-blue-700 text-white text-[10px] font-semibold" data-testid="calendar-task-call-btn" data-lead="${escapeHtml(task.leadId)}">Gọi ngay</button>`
      + `<button type="button" class="px-2 py-1 rounded-lg bg-white border border-slate-200 text-slate-700 text-[10px] font-medium" data-testid="calendar-task-lead-btn" data-lead="${escapeHtml(task.leadId)}">Chi tiết TNB</button>`
    : '';
  const doneBtn = done
    ? `<span class="text-[10px] text-emerald-600 font-semibold flex items-center gap-0.5"><i class="fa-solid fa-check" aria-hidden="true"></i> Đã xong</span>`
    : `<button type="button" class="px-2 py-1 rounded-lg bg-emerald-600 text-white text-[10px] font-semibold" data-testid="calendar-task-done-btn" data-task="${escapeHtml(task.taskId)}">Đã xong</button>`;
  const rescheduleBtn = `<button type="button" class="px-2 py-1 rounded-lg bg-white border border-slate-200 text-slate-700 text-[10px] font-medium" data-testid="calendar-task-reschedule-btn" data-task="${escapeHtml(task.taskId)}" data-due="${escapeHtml(task.dueAt)}">Đổi giờ</button>`;

  return `
    <div class="${classes}" data-testid="calendar-task-card" data-task-id="${escapeHtml(task.taskId)}">
      <div class="flex items-center justify-between gap-1 mb-0.5">
        <span class="text-[9px] font-bold uppercase text-slate-500">${escapeHtml(typeLabel)}${prio ? ` · ${escapeHtml(prio)}` : ''}</span>
        <span class="text-[10px] font-semibold text-slate-700">${escapeHtml(hhmm(task.dueAt))}</span>
      </div>
      ${justCreatedBadge}
      <h4 class="text-[11px] font-bold text-slate-900 leading-snug">${escapeHtml(task.title)}</h4>
      ${leadLine}
      ${summary}
      <div class="flex items-center flex-wrap gap-1 mt-1.5">
        ${doneBtn}
        ${rescheduleBtn}
        ${navButtons}
      </div>
    </div>`;
}

// ───────────────────────── Render các chế độ xem ─────────────────────────

function renderWeek(container, tasks) {
  const weekStart = startOfWeek(state.anchor);
  const outByCol = Array.from({ length: 6 }, () => []);
  const inByCol = Array.from({ length: 6 }, () => []);
  for (const t of tasks) {
    const col = dayColumn(t.dueAt);
    if (col < 0) continue; // Chủ nhật hiển thị ở hàng 'Ngoài giờ' của T7? Theo mockup chỉ T2–T7.
    (isOutOfHours(t.dueAt) ? outByCol : inByCol)[col].push(t);
  }
  const header = WEEKDAY_LABELS.map((label, i) => {
    const d = new Date(weekStart); d.setDate(weekStart.getDate() + i);
    return `<div class="p-2 text-center"><div class="text-[11px] font-medium text-slate-500">${escapeHtml(label)}</div>`
      + `<div class="text-xs font-bold text-slate-800">${escapeHtml(ddmm(d))}</div></div>`;
  }).join('');

  const columns = Array.from({ length: 6 }, (_, col) => {
    const outRow = outByCol[col].length
      ? `<div class="mb-1 p-1 rounded-lg bg-amber-50/70 border border-amber-100" data-testid="calendar-out-of-hours">`
        + `<div class="text-[9px] font-bold uppercase text-amber-700 mb-1">Ngoài giờ</div>`
        + outByCol[col].map((t) => taskCardHtml(t)).join('') + `</div>`
      : '';
    const inRows = inByCol[col].map((t) => taskCardHtml(t)).join('');
    return `<div class="p-1.5 space-y-1.5 border-l border-slate-100 min-h-[120px]" data-testid="calendar-day-col-${col}">${outRow}${inRows}</div>`;
  }).join('');

  container.innerHTML = `
    <div class="grid grid-cols-6 bg-slate-100 shrink-0">${header}</div>
    <div class="grid grid-cols-6 flex-1 overflow-y-auto custom-scrollbar">${columns}</div>`;
}

function renderDay(container, tasks) {
  const inHours = tasks.filter((t) => !isOutOfHours(t.dueAt));
  const outHours = tasks.filter((t) => isOutOfHours(t.dueAt));
  const section = (title, list, testid) => list.length
    ? `<div class="mb-2" data-testid="${testid}"><div class="text-[10px] font-bold uppercase text-slate-500 mb-1">${escapeHtml(title)}</div>`
      + `<div class="space-y-1.5">${list.map((t) => taskCardHtml(t)).join('')}</div></div>`
    : '';
  container.innerHTML = `<div class="p-2 overflow-y-auto custom-scrollbar flex-1">`
    + section('Trong giờ (08:00–17:00)', inHours, 'calendar-day-in-hours')
    + section('Ngoài giờ', outHours, 'calendar-day-out-hours')
    + `</div>`;
}

function renderMonth(container, tasks) {
  const byDay = new Map();
  for (const t of tasks) {
    const key = ddmm(t.dueAt);
    if (!byDay.has(key)) byDay.set(key, []);
    byDay.get(key).push(t);
  }
  const blocks = [...byDay.entries()].map(([day, list]) => `
    <div class="p-2 rounded-xl bg-white border border-slate-200" data-testid="calendar-month-day">
      <div class="text-[11px] font-bold text-slate-700 mb-1">${escapeHtml(day)}</div>
      <div class="space-y-1.5">${list.map((t) => taskCardHtml(t)).join('')}</div>
    </div>`).join('');
  container.innerHTML = `<div class="p-2 grid grid-cols-2 gap-2 overflow-y-auto custom-scrollbar flex-1">${blocks}</div>`;
}

function renderList(container, tasks) {
  const { upcoming, done } = splitByStatus(tasks);
  const section = (title, list, testid) =>
    `<div class="mb-3" data-testid="${testid}"><div class="text-[11px] font-bold uppercase text-slate-500 mb-1.5">${escapeHtml(title)} (${list.length})</div>`
    + `<div class="space-y-1.5">${list.map((t) => taskCardHtml(t)).join('') || '<p class="text-[11px] text-slate-400">Không có công việc.</p>'}</div></div>`;
  container.innerHTML = `<div class="p-3 overflow-y-auto custom-scrollbar flex-1">`
    + section('Sắp tới', upcoming, 'calendar-list-upcoming')
    + section('Đã làm', done, 'calendar-list-done')
    + `</div>`;
}

function renderView() {
  const container = $('#calendar-view');
  const empty = $('#calendar-empty');
  const total = $testid('calendar-total-count');
  total.textContent = String(state.tasks.length);

  if (state.tasks.length === 0) {
    container.classList.add('hidden');
    empty.classList.remove('hidden');
    return;
  }
  empty.classList.add('hidden');
  container.classList.remove('hidden');

  if (state.view === 'DAY') renderDay(container, state.tasks);
  else if (state.view === 'MONTH') renderMonth(container, state.tasks);
  else if (state.view === 'LIST') renderList(container, state.tasks);
  else renderWeek(container, state.tasks);
}

function renderLeftPanels() {
  // Lịch trình còn lại trong ngày: việc TODO của hôm nay có dueAt >= hiện tại.
  const now = new Date();
  const todayKey = ddmm(now);
  const agenda = state.tasks
    .filter((t) => t.status !== 'DONE' && ddmm(t.dueAt) === todayKey && new Date(t.dueAt) >= now)
    .sort((a, b) => new Date(a.dueAt) - new Date(b.dueAt));
  const agendaList = $('#daily-agenda-list');
  const agendaEmpty = $('#daily-agenda-empty');
  $testid('daily-agenda-count').textContent = `${agenda.length} công việc`;
  if (agenda.length === 0) {
    agendaList.innerHTML = '';
    agendaEmpty.classList.remove('hidden');
  } else {
    agendaEmpty.classList.add('hidden');
    agendaList.innerHTML = agenda.map((t) => `
      <div class="p-2.5 rounded-xl bg-slate-50 flex items-start gap-2.5" data-testid="daily-agenda-item">
        <div class="w-10 text-center shrink-0 pt-0.5">
          <span class="text-[11px] font-bold text-slate-700 block">${escapeHtml(hhmm(t.dueAt))}</span>
          <span class="text-[9px] text-slate-400 uppercase">${escapeHtml(String(t.durationMinutes || 30))}p</span>
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-xs font-semibold text-slate-900 truncate">${escapeHtml(t.title)}</p>
          <p class="text-[11px] text-slate-500 truncate">${escapeHtml(TYPE_LABELS[t.type] || t.type)}</p>
        </div>
      </div>`).join('');
  }
}

async function renderAiKeyin() {
  const list = $('#ai-keyin-list');
  const empty = $('#ai-keyin-empty');
  const count = $testid('ai-keyin-count');
  if (!state.sourceCallId) {
    list.innerHTML = '';
    empty.textContent = 'Chưa gắn cuộc gọi — mở lịch từ hộp thoại sau khi lưu cuộc gọi.';
    empty.classList.remove('hidden');
    count.textContent = '';
    return;
  }
  try {
    const items = await listTasks({ sourceCallId: state.sourceCallId });
    if (items.length === 0) {
      list.innerHTML = '';
      empty.textContent = 'Chưa có công việc nào từ cuộc gọi vừa lưu.';
      empty.classList.remove('hidden');
      count.textContent = '';
      return;
    }
    empty.classList.add('hidden');
    count.textContent = `${items.length} việc vừa tạo`;
    list.innerHTML = items.map((t) => taskCardHtml(t)).join('');
  } catch (err) {
    showError(`Không tải được khối AI Key-in: ${err.message}`);
  }
}

// ───────────────────────── Tải dữ liệu ─────────────────────────

async function reload() {
  clearError();
  const loading = $('#calendar-loading');
  const container = $('#calendar-view');
  const empty = $('#calendar-empty');
  loading.classList.remove('hidden');
  container.classList.add('hidden');
  empty.classList.add('hidden');

  const { from, to } = rangeFor(state.view, state.anchor);
  const label = state.view === 'MONTH'
    ? `Tháng ${from.getMonth() + 1}/${from.getFullYear()}`
    : `${ddmm(from)} - ${ddmm(to)}/${to.getFullYear()}`;
  $testid('calendar-range-label').textContent = label;

  try {
    state.tasks = await listTasks({
      from: toIsoVn(from),
      to: toIsoVn(to),
      type: state.filter,
    });
    loading.classList.add('hidden');
    renderView();
    renderLeftPanels();
  } catch (err) {
    loading.classList.add('hidden');
    showError(err.message || 'Không tải được lịch công việc.');
  }
}

// ───────────────────────── Hộp thoại S6 ─────────────────────────

function openDialog(mode, task) {
  state.dialogMode = mode;
  state.rescheduleTaskId = task ? task.taskId : null;
  const dialog = $('#task-dialog');
  const title = $testid('task-dialog-title');
  const form = $('#task-form');
  form.reset();
  $('#task-form-error').classList.add('hidden');
  $('#task-title-error').classList.add('hidden');
  $('#past-warning').classList.add('hidden');

  const createOnly = document.querySelectorAll('[data-section="create-only"]');
  if (mode === 'reschedule') {
    title.textContent = 'Đổi giờ công việc';
    createOnly.forEach((el) => el.classList.add('hidden'));
    $('#field-title').classList.add('hidden');
    if (task) {
      const d = new Date(task.dueAt);
      const pad = (n) => String(n).padStart(2, '0');
      $('#task-date').value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
      $('#task-time').value = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
    }
  } else {
    title.textContent = 'Tạo công việc mới';
    createOnly.forEach((el) => el.classList.remove('hidden'));
    $('#field-title').classList.remove('hidden');
  }
  dialog.classList.remove('hidden');
}

function closeDialog() {
  $('#task-dialog').classList.add('hidden');
}

/** Ghép ngày + giờ của form thành ISO +07:00. */
function dialogDueIso() {
  const date = $('#task-date').value;
  const time = $('#task-time').value;
  if (!date || !time) return null;
  return `${date}T${time}:00${VN_OFFSET}`;
}

async function submitDialog(confirmPast) {
  const errorEl = $('#task-form-error');
  errorEl.classList.add('hidden');
  const submitBtn = $testid('task-create-form-submit');
  const dueAt = dialogDueIso();

  if (state.dialogMode === 'reschedule') {
    if (!dueAt) { errorEl.textContent = 'Vui lòng chọn ngày và giờ.'; errorEl.classList.remove('hidden'); return; }
    submitBtn.disabled = true;
    try {
      await patchTask(state.rescheduleTaskId, { dueAt, confirmPast: !!confirmPast });
      closeDialog();
      showToast('Đã đổi giờ công việc.');
      await reload();
    } catch (err) {
      if (isPastNotConfirmed(err)) {
        $('#past-warning').classList.remove('hidden');
      } else {
        errorEl.textContent = err.message; errorEl.classList.remove('hidden');
      }
    } finally {
      submitBtn.disabled = false;
    }
    return;
  }

  // create
  const title = $('#task-title').value.trim();
  if (!title) {
    $('#task-title-error').classList.remove('hidden');
    $('#task-title').focus();
    return;
  }
  if (!dueAt) { errorEl.textContent = 'Vui lòng chọn ngày và giờ.'; errorEl.classList.remove('hidden'); return; }
  const leadId = $('#task-lead').value.trim();
  const body = {
    title,
    type: $('#task-type').value,
    dueAt,
    durationMinutes: Number($('#task-duration').value),
    priority: $('#task-priority').value,
    leadId: leadId || null,
    note: $('#task-note').value.trim() || null,
  };
  submitBtn.disabled = true;
  try {
    await createTask(body);
    closeDialog();
    showToast('Đã tạo công việc mới.');
    await reload();
  } catch (err) {
    errorEl.textContent = err.message;
    errorEl.classList.remove('hidden');
  } finally {
    submitBtn.disabled = false;
  }
}

/** Nhận diện lỗi PAST_NOT_CONFIRMED từ envelope lỗi (BR5.2). */
function isPastNotConfirmed(err) {
  const details = err && err.body && err.body.error && err.body.error.details;
  if (!Array.isArray(details)) return false;
  return details.some((d) => d && d.reason === 'PAST_NOT_CONFIRMED');
}

// ───────────────────────── Sự kiện ─────────────────────────

function wireEvents() {
  // Bộ lọc loại
  document.querySelectorAll('[data-filter]').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.filter = btn.getAttribute('data-filter');
      document.querySelectorAll('[data-filter]').forEach((b) => {
        const active = b === btn;
        b.setAttribute('aria-pressed', String(active));
        b.classList.toggle('bg-white', active);
        b.classList.toggle('text-blue-700', active);
        b.classList.toggle('font-semibold', active);
      });
      reload();
    });
  });

  // Chế độ xem
  document.querySelectorAll('[data-view]').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.view = btn.getAttribute('data-view');
      document.querySelectorAll('[data-view]').forEach((b) => {
        const active = b === btn;
        b.setAttribute('aria-pressed', String(active));
        b.classList.toggle('bg-blue-700', active);
        b.classList.toggle('text-white', active);
        b.classList.toggle('shadow-2xs', active);
      });
      reload();
    });
  });

  // Điều hướng khoảng
  $testid('calendar-prev-btn').addEventListener('click', () => { shiftAnchor(-1); reload(); });
  $testid('calendar-next-btn').addEventListener('click', () => { shiftAnchor(1); reload(); });
  $testid('calendar-today-btn').addEventListener('click', () => { state.anchor = new Date(); reload(); });

  // Mở hộp thoại tạo
  $testid('task-create-open-btn').addEventListener('click', () => openDialog('create', null));
  $testid('task-dialog-close').addEventListener('click', closeDialog);
  $testid('task-form-cancel').addEventListener('click', closeDialog);

  // Submit form
  $('#task-form').addEventListener('submit', (e) => { e.preventDefault(); submitDialog(false); });
  $testid('reschedule-past-confirm').addEventListener('click', () => submitDialog(true));
  $testid('reschedule-past-cancel').addEventListener('click', () => $('#past-warning').classList.add('hidden'));

  // Thao tác trên thẻ (uỷ quyền sự kiện)
  document.addEventListener('click', (e) => {
    const doneBtn = e.target.closest('[data-testid="calendar-task-done-btn"]');
    if (doneBtn) { markTaskDone(doneBtn.getAttribute('data-task')); return; }
    const resBtn = e.target.closest('[data-testid="calendar-task-reschedule-btn"]');
    if (resBtn) {
      openDialog('reschedule', { taskId: resBtn.getAttribute('data-task'), dueAt: resBtn.getAttribute('data-due') });
      return;
    }
    const callBtn = e.target.closest('[data-testid="calendar-task-call-btn"]');
    if (callBtn) { // Điều hướng sang màn U1 (BR5.3) — U5 không tạo cuộc gọi.
      window.location.href = `/lead?leadId=${encodeURIComponent(callBtn.getAttribute('data-lead'))}&action=call`;
      return;
    }
    const leadBtn = e.target.closest('[data-testid="calendar-task-lead-btn"]');
    if (leadBtn) {
      window.location.href = `/lead?leadId=${encodeURIComponent(leadBtn.getAttribute('data-lead'))}`;
    }
  });

  // Thử lại
  $testid('error-retry-btn').addEventListener('click', reload);
}

function shiftAnchor(direction) {
  const a = new Date(state.anchor);
  if (state.view === 'DAY') a.setDate(a.getDate() + direction);
  else if (state.view === 'MONTH') a.setMonth(a.getMonth() + direction);
  else a.setDate(a.getDate() + direction * 7);
  state.anchor = a;
}

async function markTaskDone(taskId) {
  try {
    await patchTask(taskId, { status: 'DONE' });
    showToast('Đã đánh dấu hoàn thành.');
    await reload();
  } catch (err) {
    showError(err.message || 'Không cập nhật được công việc.');
  }
}

// ───────────────────────── Khởi động ─────────────────────────

async function init() {
  state.sourceCallId = queryParam('sourceCallId');
  wireEvents();
  try {
    const me = await getMe();
    const name = me && me.displayName ? me.displayName : 'RM';
    $testid('current-rm-name').textContent = name;
    const initials = name.split(/\s+/).map((w) => w[0]).slice(-2).join('').toUpperCase();
    $testid('current-rm-avatar').textContent = initials || 'RM';
  } catch (_) {
    $testid('current-rm-name').textContent = 'RM';
  }
  await renderAiKeyin();
  await reload();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}

// Helpers hiển thị dùng chung cho S1 (chi tiết lead) và S2 (thực hiện cuộc gọi).
// Chỉ thao tác DOM/chuỗi; không fetch. Mọi dữ liệu từ backend đều được escape
// trước khi chèn vào HTML (NFR tránh XSS).

/** Escape HTML cho mọi dữ liệu động trước khi chèn vào innerHTML. */
export function escapeHtml(value) {
  return String(value == null ? '' : value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/** Nhãn tiếng Việt cho giai đoạn lead. */
export function stageLabel(stage) {
  switch (stage) {
    case 'CHUA_TIEP_CAN': return 'Chưa tiếp cận';
    case 'DANG_TIEP_CAN': return 'Đang tiếp cận';
    case 'CHUYEN_CO_HOI': return 'Chuyển cơ hội';
    case 'TU_CHOI': return 'Từ chối';
    default: return stage || '';
  }
}

/** Nhãn + emoji cho mức ưu tiên. */
export function priorityLabel(priority) {
  switch (priority) {
    case 'KHAI_THAC_NGAY': return '⚡ Khai thác ngay';
    case 'HOT_LEAD': return '🔥 Hot Lead';
    case 'THUONG': return 'Thường';
    default: return priority || '';
  }
}

/** Nhãn loại hoạt động. */
export function activityTypeLabel(type) {
  switch (type) {
    case 'CUOC_GOI': return 'Cuộc gọi';
    case 'HEN_GAP': return 'Hẹn gặp';
    case 'SMS': return 'Nhắn tin SMS';
    case 'KHAC': return 'Khác';
    default: return type || '';
  }
}

/** Định dạng thời lượng mm:ss từ số giây. */
export function formatDuration(totalSeconds) {
  const s = Math.max(0, Math.floor(totalSeconds || 0));
  const m = String(Math.floor(s / 60)).padStart(2, '0');
  const sec = String(s % 60).padStart(2, '0');
  return `${m}:${sec}`;
}

/** Định dạng ISO datetime sang "dd/MM/yyyy - HH:mm" (hiển thị). */
export function formatDateTime(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return escapeHtml(iso);
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} - ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Đọc query param. */
export function queryParam(name) {
  return new URLSearchParams(window.location.search).get(name);
}

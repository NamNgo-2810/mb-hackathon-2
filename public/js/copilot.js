// Panel Copilot (U3, hợp đồng C2) trên màn Thực hiện cuộc gọi (S2).
// Giữ lịch sử phiên phía client (≤ 10 lượt gửi lên), gọi askCopilot, render
// câu trả lời + trích dẫn + gợi ý có escape, hiển thị lỗi 502 CHỈ trong panel.
// "Cuộc trò chuyện mới" = xoá lịch sử + gửi history rỗng (AC1.3.3, BR6.2).

import { askCopilot } from '/js/api.js';
import { escapeHtml } from '/js/screens.js';

const MAX_HISTORY_TURNS = 10;

/**
 * Khởi tạo panel Copilot.
 * @param {string|null} callId cuộc gọi đang diễn ra (gắn vào mỗi câu hỏi).
 */
export function initCopilot(callId) {
  const input = document.querySelector('[data-testid="copilot-input"]');
  const sendBtn = document.querySelector('[data-testid="copilot-send"]');
  const answerEl = document.querySelector('[data-testid="copilot-answer"]');
  const citationsEl = document.querySelector('[data-testid="copilot-citations"]');
  const suggestionsEl = document.querySelector('[data-testid="copilot-suggestions"]');
  const historyEl = document.querySelector('[data-testid="copilot-history"]');
  const newChatBtn = document.querySelector('[data-testid="copilot-new-chat"]');
  const errorEl = document.querySelector('[data-testid="copilot-error"]');

  if (!input || !sendBtn || !answerEl) {
    return; // panel chưa gắn trong màn
  }

  // Lịch sử phiên: [{ role:'user'|'assistant', text }]; và danh sách cặp hỏi–đáp để hiển thị cột.
  let history = [];
  let topics = []; // [{ question, answer }]
  let busy = false;

  function showError(msg) {
    if (!errorEl) return;
    errorEl.textContent = msg || '';
    errorEl.classList.toggle('hidden', !msg);
  }

  function renderCitations(citations) {
    if (!citationsEl) return;
    citationsEl.innerHTML = '';
    if (!citations || citations.length === 0) {
      citationsEl.classList.add('hidden');
      return;
    }
    citationsEl.classList.remove('hidden');
    for (const c of citations) {
      const where = c.page != null ? `trang ${escapeHtml(c.page)}`
        : (c.section ? `mục ${escapeHtml(c.section)}` : '');
      const eff = c.effectiveDate ? ` · Hiệu lực: ${escapeHtml(c.effectiveDate)}` : '';
      const div = document.createElement('div');
      div.className = 'text-[11px] text-slate-600 border-t border-slate-200/80 pt-1.5 flex items-center gap-1';
      div.innerHTML =
        `<i class="fa-regular fa-file-lines text-blue-600"></i>` +
        `<span>Nguồn: <strong class="text-slate-700">${escapeHtml(c.documentTitle)}</strong>` +
        (where ? ` (${where})` : '') + `${eff}</span>`;
      citationsEl.appendChild(div);
    }
  }

  function renderSuggestions(suggestions) {
    if (!suggestionsEl) return;
    suggestionsEl.innerHTML = '';
    if (!suggestions || suggestions.length === 0) {
      suggestionsEl.classList.add('hidden');
      return;
    }
    suggestionsEl.classList.remove('hidden');
    for (const s of suggestions) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className =
        'px-2.5 py-1.5 bg-slate-100 hover:bg-blue-50 hover:text-blue-700 border border-slate-200 ' +
        'hover:border-blue-300 rounded-full text-[11px] text-slate-700 transition-all font-medium';
      btn.setAttribute('data-testid', 'copilot-suggestion');
      btn.textContent = s; // textContent tự escape
      // Bấm gợi ý = gửi đúng chuỗi đó như câu hỏi mới (WF3).
      btn.addEventListener('click', () => submit(s));
      suggestionsEl.appendChild(btn);
    }
  }

  function renderHistory() {
    if (!historyEl) return;
    historyEl.innerHTML = '';
    if (topics.length === 0) {
      historyEl.innerHTML =
        '<p class="text-[11px] text-slate-400 px-1" data-testid="copilot-history-empty">Chưa có lượt tra cứu</p>';
      return;
    }
    // Mới nhất trước (AC1.3.2).
    for (let i = topics.length - 1; i >= 0; i--) {
      const t = topics[i];
      const a = document.createElement('button');
      a.type = 'button';
      a.className =
        'w-full text-left flex items-center gap-2 px-2.5 py-2 rounded-lg text-slate-600 ' +
        'hover:bg-slate-100 hover:text-slate-900 text-xs font-medium transition-colors';
      a.setAttribute('data-testid', 'copilot-history-item');
      a.innerHTML =
        `<i class="fa-regular fa-file-lines text-slate-400 shrink-0 text-xs"></i>` +
        `<span class="truncate">${escapeHtml(t.question)}</span>`;
      // Bấm mở lại câu trả lời đã lưu (hiển thị, không gọi lại server).
      a.addEventListener('click', () => showStored(t));
      historyEl.appendChild(a);
    }
  }

  function showStored(topic) {
    showError('');
    answerEl.innerHTML = renderMarkdown(topic.answer.answer);
    renderCitations(topic.answer.citations);
    renderSuggestions(topic.answer.suggestions);
  }

  // Markdown rất đơn giản + escape: đoạn, danh sách, in đậm. Mọi text đều escape trước.
  function renderMarkdown(text) {
    const safe = escapeHtml(text || '');
    const lines = safe.split('\n');
    let html = '';
    let inList = false;
    for (const raw of lines) {
      const line = raw.trimEnd();
      if (/^[-*]\s+/.test(line)) {
        if (!inList) { html += '<ul class="list-disc pl-4 space-y-1">'; inList = true; }
        html += `<li>${inlineMd(line.replace(/^[-*]\s+/, ''))}</li>`;
      } else {
        if (inList) { html += '</ul>'; inList = false; }
        if (line.trim() !== '') html += `<p class="mb-2">${inlineMd(line)}</p>`;
      }
    }
    if (inList) html += '</ul>';
    return html || '<p class="text-slate-400">(Không có nội dung)</p>';
  }

  // In đậm **...** trên văn bản ĐÃ escape (an toàn: dấu * không bị escape).
  function inlineMd(escaped) {
    return escaped.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  }

  async function submit(questionText) {
    const question = (questionText != null ? questionText : input.value || '').trim();
    if (!question || busy) return;
    busy = true;
    sendBtn.disabled = true;
    showError('');
    answerEl.innerHTML = '<p class="text-slate-400 text-xs" data-testid="copilot-loading">Đang tra cứu…</p>';
    renderCitations([]);
    renderSuggestions([]);

    try {
      const ans = await askCopilot(question, callId, history);
      answerEl.innerHTML = renderMarkdown(ans.answer);
      renderCitations(ans.citations);
      renderSuggestions(ans.suggestions);

      // Cập nhật lịch sử phiên (≤ 10 lượt gửi lên lần sau) và cột hiển thị.
      history.push({ role: 'user', text: question });
      history.push({ role: 'assistant', text: ans.answer });
      if (history.length > MAX_HISTORY_TURNS) {
        history = history.slice(history.length - MAX_HISTORY_TURNS);
      }
      topics.push({ question, answer: ans });
      renderHistory();
      input.value = '';
    } catch (e) {
      // Lỗi (gồm 502 AI_ERROR) CHỈ hiện trong panel; không chạm phần còn lại của màn (BR5.2).
      const msg = e && e.message ? e.message : 'Có lỗi khi tra cứu. Vui lòng thử lại.';
      showError(msg);
      answerEl.innerHTML = '';
    } finally {
      busy = false;
      sendBtn.disabled = false;
    }
  }

  function newChat() {
    // "Cuộc trò chuyện mới": xoá lịch sử phía client; lần hỏi kế gửi history rỗng (AC1.3.3).
    history = [];
    topics = [];
    showError('');
    answerEl.innerHTML =
      '<p class="text-slate-400 text-xs text-center" data-testid="copilot-empty">' +
      'Hỏi về sản phẩm, chính sách, lãi suất, quy trình phê duyệt…</p>';
    renderCitations([]);
    renderSuggestions([]);
    renderHistory();
    input.focus();
  }

  sendBtn.addEventListener('click', () => submit());
  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') { e.preventDefault(); submit(); }
  });
  if (newChatBtn) newChatBtn.addEventListener('click', newChat);

  // Trạng thái ban đầu.
  newChat();
}

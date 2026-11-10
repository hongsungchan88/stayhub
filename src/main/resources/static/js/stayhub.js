/*
 * stayhub 공통 자바스크립트 — 모든 화면이 같이 씁니다. 화면마다 복사하지 마세요.
 * 레이아웃(templates/admin/layout.html)과 로그인 화면이 이미 불러오므로, 내 화면에서는 함수만 부르면 됩니다.
 *
 * 들어 있는 함수 (정책정의서 "공통 UI"·"에러 처리")
 *   api(method, url, body)        /api/** 호출. 성공하면 data 를 돌려주고, 실패하면 서버 message 로 에러를 던집니다
 *   toast(message, type)          우측 상단 토스트 3초. type: 'success'(기본) | 'warning' | 'error'
 *   showError(error)              api() 에서 받은 에러를 빨간 토스트로 (목록 조회·삭제 실패 등)
 *   showFormError(form, error)    입력값 오류: 필드 아래 인라인 메시지 + 빨간 테두리. 필드를 모르면 폼 위에
 *   clearFormError(form)          위 표시를 지움
 *   formToJson(form)              폼 입력값 → API 로 보낼 객체 (빈칸은 null, number 칸은 숫자)
 *   fillForm(form, data)          API 로 받은 객체 → 폼 입력값
 *   formatMoney(120000)           "120,000원"
 *   formatDate(value)             "2026-11-10"        (목록)
 *   formatDateTime(value)         "2026-11-10 18:24"  (상세·로그)
 *   renderRows(tbody, items, fn)  표 본문 그리기. 비었으면 "조회된 데이터가 없습니다" 가운데 표시
 *   renderPagination(el, page, onClick)  페이지 번호 (PageResponse 그대로 넘김, 20건 단위는 서버가 정함)
 *   confirmDelete(message)        삭제 확인 팝업. 기본 포커스 [취소]. 확인이면 true
 *   esc(text)                     HTML 특수문자 바꾸기. renderRows 안에서 사용자가 입력한 값은 꼭 esc() 로 감싸세요
 *   logout()                      로그아웃 후 로그인 화면으로
 */

const SERVER_ERROR_MESSAGE = '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.';

/**
 * /api/** 호출. ApiResponse { success, data, message } 를 풀어 data 를 돌려줍니다.
 *   const page = await api('GET', '/api/guidebooks?page=0');
 *   await api('POST', '/api/guidebooks', { title: '...' });
 *
 * 실패하면 서버 message 를 담은 Error 를 던집니다 → catch 에서 showError / showFormError 로 보여주세요.
 * 아래는 api() 가 알아서 처리하고, 던지는 에러에 shown = true 를 붙입니다(showError 등이 다시 띄우지 않음).
 *   401 → /login?expired (세션 만료)   403 → /403 안내 페이지   500·네트워크 오류 → 서버 오류 팝업
 * 성공인데 message 가 있으면(예: 중복 제목 경고) 노란 토스트로 보여줍니다.
 */
async function api(method, url, body) {
  const options = { method, headers: { Accept: 'application/json' } };
  if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(url, options);
  } catch (networkError) {
    return stop(() => alert(SERVER_ERROR_MESSAGE));
  }
  if (response.status === 401) return stop(() => location.href = '/login?expired');
  if (response.status === 403) return stop(() => location.href = '/403');
  if (response.status >= 500) return stop(() => alert(SERVER_ERROR_MESSAGE));

  let json;
  try {
    json = await response.json();
  } catch (notJson) {
    return stop(() => alert(SERVER_ERROR_MESSAGE));
  }

  if (!json.success) {
    const error = new Error(json.message || SERVER_ERROR_MESSAGE);
    error.status = response.status;
    throw error;
  }
  if (json.message) toast(json.message, 'warning');
  return json.data;
}

/** 이미 처리(이동·팝업)한 실패. 화면 코드의 catch 로 가되 다시 보여주지 않게 shown 을 붙입니다. */
function stop(handle) {
  handle();
  const error = new Error(SERVER_ERROR_MESSAGE);
  error.shown = true;
  throw error;
}

/* ---------- 토스트 ---------- */

const TOAST_STYLE = {
  success: { class: 'bg-success', title: '완료' },
  warning: { class: 'bg-warning', title: '확인' },
  error: { class: 'bg-danger', title: '오류' },
};
const TOAST_STORAGE_KEY = 'stayhub.toasts';

/**
 * 우측 상단 토스트 3초 (정책 "저장 성공").
 * 토스트를 띄운 직후 다른 화면으로 이동해도 다음 화면에서 남은 시간만큼 이어서 보여줍니다.
 * 그래서 "저장 → 토스트 → 목록으로 이동" 을 그냥 순서대로 쓰면 됩니다.
 */
function toast(message, type = 'success', delay = 3000) {
  const style = TOAST_STYLE[type] || TOAST_STYLE.success;
  $(document).Toasts('create', {
    class: style.class, title: style.title, body: esc(message),
    position: 'topRight', autohide: true, delay,
  });
  const saved = JSON.parse(sessionStorage.getItem(TOAST_STORAGE_KEY) || '[]');
  saved.push({ message, type, until: Date.now() + delay });
  sessionStorage.setItem(TOAST_STORAGE_KEY, JSON.stringify(saved));
}

// 이전 화면에서 아직 시간이 남은 토스트를 이어서 보여줍니다.
$(function () {
  const saved = JSON.parse(sessionStorage.getItem(TOAST_STORAGE_KEY) || '[]');
  sessionStorage.removeItem(TOAST_STORAGE_KEY);
  saved.filter(t => t.until > Date.now() + 300).forEach(t => toast(t.message, t.type, t.until - Date.now()));
});

function showError(error) {
  if (!error.shown) toast(error.message, 'error');
}

/* ---------- 폼 ---------- */

/**
 * 입력값 오류 표시 (정책 "입력값 오류").
 * 서버 검증 실패 메시지는 "title: 제목을 입력해 주세요., content: 내용을 입력해 주세요." 모양입니다.
 * 앞의 이름과 같은 name 의 입력칸이 폼에 있으면 그 칸 아래에, 없으면 폼 위 [data-form-error] 칸에 보여줍니다.
 */
function showFormError(form, error) {
  clearFormError(form);
  if (error.shown) return;

  const leftovers = [];
  for (const part of error.message.split(/, (?=[A-Za-z]\w*: )/)) {
    const match = part.match(/^([A-Za-z]\w*): (.*)$/s);
    const input = match && form.elements[match[1]];
    if (input && input.insertAdjacentElement) {
      input.classList.add('is-invalid');
      const feedback = document.createElement('div');
      feedback.className = 'invalid-feedback';
      feedback.textContent = match[2];
      input.insertAdjacentElement('afterend', feedback);
    } else {
      leftovers.push(part);
    }
  }
  const top = form.querySelector('[data-form-error]');
  if (leftovers.length && top) {
    top.textContent = leftovers.join(' ');
    top.classList.remove('d-none');
  } else if (leftovers.length) {
    toast(leftovers.join(' '), 'error');
  }
}

function clearFormError(form) {
  form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
  form.querySelectorAll('.invalid-feedback').forEach(el => el.remove());
  const top = form.querySelector('[data-form-error]');
  if (top) top.classList.add('d-none');
}

/** 폼 → 객체. name 이 있는 칸만. 빈칸은 null(서버 @NotNull·@NotBlank 가 메시지를 줌), number 칸은 숫자, 체크박스는 true/false. */
function formToJson(form) {
  const body = {};
  for (const el of form.elements) {
    if (!el.name || (el.type === 'radio' && !el.checked)) continue;
    if (el.type === 'checkbox') body[el.name] = el.checked;
    else if (el.value === '') body[el.name] = null;
    else body[el.name] = el.type === 'number' ? Number(el.value) : el.value;
  }
  return body;
}

/** 객체 → 폼. data 의 키와 같은 name 의 칸을 채웁니다. */
function fillForm(form, data) {
  for (const el of form.elements) {
    if (!el.name || !(el.name in data)) continue;
    const value = data[el.name];
    if (el.type === 'checkbox') el.checked = Boolean(value);
    else if (el.type === 'radio') el.checked = el.value === String(value);
    else el.value = value ?? '';
  }
}

/* ---------- 표시 형식 ---------- */

function formatMoney(amount) {
  return amount == null ? '' : Number(amount).toLocaleString('ko-KR') + '원';
}

// 서버는 날짜를 "2026-11-10T18:24:47.123" 모양(ISO)으로 보냅니다. 앞부분만 잘라 씁니다.
function formatDate(value) {
  return value ? String(value).slice(0, 10) : '';
}

function formatDateTime(value) {
  return value ? String(value).slice(0, 16).replace('T', ' ') : '';
}

function esc(text) {
  return String(text ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/* ---------- 목록 ---------- */

/**
 * 표 본문 그리기 (정책 "빈 목록").
 *   renderRows(tbody, page.content, g => `<tr><td>${esc(g.title)}</td></tr>`);
 * 비어 있으면 제목 줄(th) 칸 수만큼 합친 한 칸에 "조회된 데이터가 없습니다" 를 가운데 표시합니다.
 */
function renderRows(tbody, items, rowHtml) {
  if (items.length === 0) {
    const columns = tbody.closest('table').querySelectorAll('thead th').length || 1;
    tbody.innerHTML = `<tr><td colspan="${columns}" class="text-center text-muted py-5">조회된 데이터가 없습니다</td></tr>`;
    return;
  }
  tbody.innerHTML = items.map(rowHtml).join('');
}

/**
 * 페이지 번호 (정책 "페이징 단위"). 서버의 PageResponse 를 그대로 넘깁니다.
 *   renderPagination(document.getElementById('pagination'), page, load);
 * 번호를 누르면 onClick(페이지번호) 를 부릅니다. 번호는 0부터, 화면에는 1부터 보입니다. 10개씩 끊어 보여줍니다.
 */
function renderPagination(container, page, onClick) {
  container.innerHTML = '';
  if (page.totalPages <= 1) return;

  const first = Math.floor(page.page / 10) * 10;
  const last = Math.min(first + 10, page.totalPages) - 1;
  const ul = document.createElement('ul');
  ul.className = 'pagination pagination-sm justify-content-center m-0';
  const add = (label, target, { active = false, disabled = false } = {}) => {
    const li = document.createElement('li');
    li.className = 'page-item' + (active ? ' active' : '') + (disabled ? ' disabled' : '');
    const a = document.createElement('a');
    a.className = 'page-link';
    a.href = '#';
    a.textContent = label;
    a.addEventListener('click', e => { e.preventDefault(); if (!disabled && !active) onClick(target); });
    li.appendChild(a);
    ul.appendChild(li);
  };
  add('‹', first - 1, { disabled: first === 0 });
  for (let i = first; i <= last; i++) add(String(i + 1), i, { active: i === page.page });
  add('›', last + 1, { disabled: last >= page.totalPages - 1 });
  container.appendChild(ul);
}

/* ---------- 팝업 ---------- */

/**
 * 삭제 확인 팝업 (정책 "삭제 확인"). 레이아웃의 #confirmModal 을 씁니다.
 *   if (!(await confirmDelete('삭제할까요?'))) return;
 * 열리면 [취소] 에 포커스가 가서 Enter 를 눌러도 지워지지 않습니다.
 */
function confirmDelete(message) {
  return new Promise(resolve => {
    const $modal = $('#confirmModal');
    let confirmed = false;
    $modal.find('.modal-body').text(message || '삭제하시겠습니까?');
    $modal.find('[data-confirm]').off('click').on('click', () => { confirmed = true; $modal.modal('hide'); });
    $modal.off('shown.bs.modal hidden.bs.modal')
      .on('shown.bs.modal', () => $modal.find('[data-cancel]').trigger('focus'))
      .on('hidden.bs.modal', () => resolve(confirmed));
    $modal.modal('show');
  });
}

/* ---------- 로그아웃 ---------- */

async function logout() {
  try {
    await api('POST', '/api/auth/logout');
  } catch (alreadyHandled) {
    return;
  }
  location.href = '/login';
}

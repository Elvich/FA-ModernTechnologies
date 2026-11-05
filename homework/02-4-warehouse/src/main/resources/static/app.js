const api = '/api/equipment';
const editor = document.querySelector('#editor');
const filters = document.querySelector('#filters');
const message = document.querySelector('#message');
let editingId = null;
let listRequest = 0;
async function request(url, options) {
  const response = await fetch(url, options);
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || body.detail || 'Ошибка запроса: ' + response.status);
  }
  return response.status === 204 ? null : response.json();
}
function reset() { editingId = null; editor.reset(); document.querySelector('#form-title').textContent = 'Добавить технику'; }
function notice(error) { message.textContent = error.message; }
async function load() {
  const requestId = ++listRequest;
  const rows = await request(api + '?' + new URLSearchParams(new FormData(filters)));
  if (requestId !== listRequest) return;
  const tbody = document.querySelector('#records');
  tbody.replaceChildren();
  document.querySelector('#counter').textContent = 'Найдено записей: ' + rows.length;
  if (!rows.length) {
    const tr = tbody.insertRow(); const td = tr.insertCell(); td.colSpan = 7; td.textContent = 'Записей нет. Добавьте технику или измените поиск.';
  }
  rows.forEach(record => {
    const tr = tbody.insertRow();
    ['id','kind','equipmentGroup','importedOn','exportedOn','driverName'].forEach(key => { tr.insertCell().textContent = record[key] ?? '—'; });
    const actions = tr.insertCell();
    const edit = document.createElement('button'); edit.textContent = 'Изменить';
    edit.onclick = () => {
      editingId = record.id;
      ['kind','equipmentGroup','importedOn','exportedOn','driverName'].forEach(key => { editor.elements[key].value = record[key] ?? ''; });
      document.querySelector('#form-title').textContent = 'Редактировать запись № ' + record.id;
      editor.elements.kind.focus();
    };
    const remove = document.createElement('button'); remove.textContent = 'Удалить'; remove.className = 'secondary';
    remove.onclick = async () => {
      if (!confirm('Удалить запись № ' + record.id + '?')) return;
      remove.disabled = true;
      try { await request(api + '/' + record.id, { method: 'DELETE' }); if (editingId === record.id) reset(); await refresh(); message.textContent = 'Запись удалена.'; }
      catch(error) { notice(error); } finally { remove.disabled = false; }
    };
    actions.append(edit, remove);
  });
}
async function histogram() {
  const days = await request(api + '/histogram');
  const container = document.querySelector('#histogram'); container.replaceChildren();
  const max = Math.max(1, ...days.map(day => day.count));
  days.forEach(day => {
    const row = document.createElement('div'); row.className = 'day';
    const date = document.createElement('span'); date.textContent = day.date;
    const track = document.createElement('div'); track.className = 'track';
    const bar = document.createElement('div'); bar.className = 'bar'; bar.style.width = 100 * day.count / max + '%';
    const count = document.createElement('strong'); count.textContent = day.count;
    track.append(bar); row.append(date,track,count); container.append(row);
  });
}
async function refresh() { await Promise.all([load(), histogram()]); }
editor.onsubmit = async event => {
  event.preventDefault(); message.textContent = '';
  const payload = Object.fromEntries(new FormData(editor));
  payload.exportedOn ||= null;
  if (payload.exportedOn && payload.exportedOn < payload.importedOn) { notice(new Error('Дата вывоза не может быть раньше даты ввоза')); return; }
  const save = editor.querySelector('[type=submit]'); save.disabled = true;
  try {
    await request(api + (editingId === null ? '' : '/' + editingId), {method: editingId === null ? 'POST' : 'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify(payload)});
    reset(); await refresh(); message.textContent = 'Запись сохранена.';
  } catch(error) { notice(error); } finally { save.disabled = false; }
};
filters.onsubmit = event => { event.preventDefault(); load().catch(notice); };
filters.elements.order.onchange = () => load().catch(notice);
document.querySelector('#cancel').onclick = reset;
refresh().catch(notice);

'use strict';
const form = document.querySelector('#bookForm');
const message = document.querySelector('#message');
let editingId = null;
let requestNumber = 0;
function notify(text, error = false) { message.textContent = text; message.classList.toggle('error', error); }
async function api(path, options = {}) {
  const response = await fetch(path, {headers: {'Content-Type': 'application/json'}, ...options});
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || 'Не удалось выполнить запрос (' + response.status + ')');
  }
  return response.status === 204 ? null : response.json();
}
function dateLabel(value) { return value ? value.split('-').reverse().join('.') : 'Не сдана'; }
function resetForm() {
  editingId = null; form.reset();
  document.querySelector('#formTitle').textContent = 'Добавить книгу';
  document.querySelector('#save').textContent = 'Добавить';
  document.querySelector('#cancel').hidden = true;
}
function edit(book) {
  editingId = book.id;
  for (const key of ['title', 'publisher', 'issuedOn', 'studentName', 'returnedOn']) form.elements.namedItem(key).value = book[key] || '';
  document.querySelector('#formTitle').textContent = 'Редактировать книгу № ' + book.id;
  document.querySelector('#save').textContent = 'Сохранить';
  document.querySelector('#cancel').hidden = false;
  form.elements.namedItem('title').focus();
}
async function remove(book) {
  if (!confirm('Удалить книгу «' + book.title + '»?')) return;
  try {
    await api('/api/books/' + book.id, {method: 'DELETE'});
    if (editingId === book.id) resetForm();
    await refresh(); notify('Книга удалена.');
  } catch (error) { notify(error.message, true); }
}
function renderBooks(books) {
  const body = document.querySelector('#books'); body.replaceChildren();
  for (const book of books) {
    const row = document.createElement('tr');
    for (const value of [book.id, book.title, book.publisher, dateLabel(book.issuedOn), book.studentName, dateLabel(book.returnedOn)]) {
      const cell = document.createElement('td'); cell.textContent = value; row.append(cell);
    }
    const actions = document.createElement('td');
    for (const [text, handler, className] of [['Изменить', () => edit(book), 'secondary'], ['Удалить', () => remove(book), 'danger']]) {
      const button = document.createElement('button'); button.type = 'button'; button.textContent = text; button.className = className; button.addEventListener('click', handler); actions.append(button);
    }
    row.append(actions); body.append(row);
  }
  document.querySelector('#count').textContent = books.length;
  document.querySelector('#empty').hidden = books.length !== 0;
}
function renderChart(days) {
  const chart = document.querySelector('#chart'); chart.replaceChildren();
  const max = Math.max(1, ...days.map(day => day.count));
  for (const day of days) {
    const item = document.createElement('div'); item.className = 'day'; item.setAttribute('role', 'listitem');
    item.setAttribute('aria-label', dateLabel(day.date) + ': ' + day.count);
    const count = document.createElement('strong'); count.textContent = day.count;
    const bar = document.createElement('div'); bar.className = 'bar'; bar.style.height = (day.count / max * 150) + 'px';
    const label = document.createElement('span'); label.textContent = dateLabel(day.date).slice(0, 5);
    item.append(count, bar, label); chart.append(item);
  }
}
async function refresh() {
  const current = ++requestNumber;
  const query = new URLSearchParams({q: document.querySelector('#search').value, direction: document.querySelector('#direction').value});
  const [books, days, today] = await Promise.all([api('/api/books?' + query), api('/api/books/histogram'), api('/api/today')]);
  if (current !== requestNumber) return;
  renderBooks(books); renderChart(days); document.querySelector('#today').textContent = dateLabel(today);
}
function reload() { refresh().catch(error => notify(error.message, true)); }
form.addEventListener('submit', async event => {
  event.preventDefault();
  const data = Object.fromEntries(new FormData(form)); data.returnedOn = data.returnedOn || null;
  const save = document.querySelector('#save'); save.disabled = true;
  try {
    await api('/api/books' + (editingId === null ? '' : '/' + editingId), {method: editingId === null ? 'POST' : 'PUT', body: JSON.stringify(data)});
    resetForm(); await refresh(); notify('Книга сохранена.');
  } catch (error) { notify(error.message, true); } finally { save.disabled = false; }
});
document.querySelector('#cancel').addEventListener('click', resetForm);
document.querySelector('#find').addEventListener('click', reload);
document.querySelector('#direction').addEventListener('change', reload);
document.querySelector('#search').addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); reload(); } });
document.querySelector('#search').addEventListener('search', reload);
reload();

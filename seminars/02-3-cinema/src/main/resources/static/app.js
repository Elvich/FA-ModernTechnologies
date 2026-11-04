'use strict';
const api = '/api/screenings';
const form = document.querySelector('#screening-form');
const message = document.querySelector('#message');
let editingId = null;
let rows = [];
let loadSequence = 0;

function notify(text, error = false) {
  message.textContent = text;
  message.classList.toggle('error', error);
}

async function request(path, options = {}) {
  const response = await fetch(path, options);
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || `Ошибка запроса: ${response.status}`);
  }
  return response.status === 204 ? null : response.json();
}

function cancelEditing() {
  editingId = null;
  form.reset();
  document.querySelector('#form-title').textContent = 'Новый сеанс';
  document.querySelector('#save').textContent = 'Добавить';
  document.querySelector('#cancel').hidden = true;
}

function edit(screening) {
  editingId = screening.id;
  for (const field of ['filmTitle', 'studio', 'startsAt', 'ticketCapacity', 'soldTickets']) {
    form.elements.namedItem(field).value = screening[field];
  }
  document.querySelector('#form-title').textContent = `Редактирование сеанса № ${screening.id}`;
  document.querySelector('#save').textContent = 'Сохранить';
  document.querySelector('#cancel').hidden = false;
  form.scrollIntoView({behavior: 'smooth', block: 'center'});
}

function renderRows() {
  const body = document.querySelector('#rows');
  body.replaceChildren();
  for (const screening of rows) {
    const tr = document.createElement('tr');
    for (const value of [screening.id, screening.filmTitle, screening.studio,
      screening.startsAt.replace('T', ' '), screening.ticketCapacity, screening.soldTickets]) {
      const td = document.createElement('td');
      td.textContent = String(value);
      tr.append(td);
    }
    const actions = document.createElement('td');
    const editButton = document.createElement('button');
    editButton.textContent = 'Изменить';
    editButton.type = 'button';
    editButton.addEventListener('click', () => edit(screening));
    const deleteButton = document.createElement('button');
    deleteButton.textContent = 'Удалить';
    deleteButton.type = 'button';
    deleteButton.className = 'danger';
    deleteButton.addEventListener('click', async () => {
      if (!confirm(`Удалить сеанс «${screening.filmTitle}»?`)) return;
      deleteButton.disabled = true;
      try {
        await request(`${api}/${screening.id}`, {method: 'DELETE'});
        if (editingId === screening.id) cancelEditing();
        notify('Сеанс удалён.');
        await load();
      } catch (error) { notify(error.message, true); }
      finally { deleteButton.disabled = false; }
    });
    actions.append(editButton, deleteButton);
    tr.append(actions);
    body.append(tr);
  }
  document.querySelector('#counter').textContent = `Записей: ${rows.length}`;
  document.querySelector('#empty').hidden = rows.length !== 0;
  document.querySelector('#empty').textContent = document.querySelector('#query').value.trim()
    ? 'По вашему запросу сеансы не найдены.' : 'Сеансов пока нет. Добавьте первый сеанс.';
}

function renderHistogram(days) {
  const histogram = document.querySelector('#histogram');
  histogram.replaceChildren();
  const max = Math.max(1, ...days.map(day => day.count));
  for (const day of days) {
    const column = document.createElement('div');
    column.className = 'day';
    column.title = `${day.date}: ${day.count}`;
    const count = document.createElement('span');
    count.className = 'count';
    count.textContent = String(day.count);
    const space = document.createElement('div');
    space.className = 'bar-space';
    const bar = document.createElement('div');
    bar.className = 'bar';
    bar.style.height = `${day.count / max * 100}%`;
    space.append(bar);
    const date = document.createElement('span');
    date.className = 'date';
    date.textContent = day.date.split('-').reverse().join('.');
    column.append(count, space, date);
    histogram.append(column);
  }
  histogram.setAttribute('aria-label', days.map(day => `${day.date}: ${day.count} сеансов`).join('; '));
}

async function load() {
  const sequence = ++loadSequence;
  const params = new URLSearchParams({q: document.querySelector('#query').value,
    direction: document.querySelector('#direction').value});
  const [data, days] = await Promise.all([request(`${api}?${params}`), request(`${api}/histogram`)]);
  if (sequence !== loadSequence) return;
  rows = data;
  renderRows();
  renderHistogram(days);
}

form.addEventListener('submit', async event => {
  event.preventDefault();
  const data = Object.fromEntries(new FormData(form));
  data.ticketCapacity = Number(data.ticketCapacity);
  data.soldTickets = Number(data.soldTickets);
  if (data.soldTickets > data.ticketCapacity) {
    notify('Проданных билетов не может быть больше, чем мест на сеанс.', true);
    return;
  }
  const button = document.querySelector('#save');
  button.disabled = true;
  try {
    await request(editingId === null ? api : `${api}/${editingId}`, {
      method: editingId === null ? 'POST' : 'PUT',
      headers: {'Content-Type': 'application/json'}, body: JSON.stringify(data)
    });
    cancelEditing();
    notify('Сеанс сохранён.');
    await load();
  } catch (error) { notify(error.message, true); }
  finally { button.disabled = false; }
});
document.querySelector('#cancel').addEventListener('click', cancelEditing);
document.querySelector('#filters').addEventListener('submit', event => {
  event.preventDefault();
  load().catch(error => notify(error.message, true));
});
document.querySelector('#direction').addEventListener('change', () => load().catch(error => notify(error.message, true)));
document.querySelector('#reset-filters').addEventListener('click', () => {
  document.querySelector('#filters').reset();
  load().catch(error => notify(error.message, true));
});
load().catch(error => notify(`Не удалось загрузить данные: ${error.message}`, true));

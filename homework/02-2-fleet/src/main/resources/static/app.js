'use strict';
const byId = id => document.getElementById(id);
const form = byId('car-form');
const fields = ['brand', 'manufactureYear', 'registeredOn', 'ownerName', 'mileage'];
const maxYear = new Date().getFullYear() + 1;
byId('manufactureYear').max = String(maxYear);
let refreshSequence = 0;

function message(text, type = '') {
    byId('message').textContent = text;
    byId('message').className = type;
}

async function api(path, options = {}) {
    const response = await fetch(path, options);
    if (!response.ok) {
        let detail;
        try { detail = await response.json(); } catch { detail = {}; }
        throw new Error(detail.message || 'Не удалось выполнить запрос: ' + response.status);
    }
    return response.status === 204 ? null : response.json();
}

function resetForm() {
    form.reset();
    byId('car-id').value = '';
    byId('form-title').textContent = 'Добавить машину';
    byId('save-button').textContent = 'Добавить';
    byId('cancel').hidden = true;
}

function edit(car) {
    byId('car-id').value = String(car.id);
    for (const field of fields) byId(field).value = car[field];
    byId('form-title').textContent = 'Редактирование машины № ' + car.id;
    byId('save-button').textContent = 'Сохранить';
    byId('cancel').hidden = false;
    byId('brand').focus();
    form.scrollIntoView({behavior: 'smooth', block: 'start'});
}

async function remove(car) {
    if (!confirm('Удалить машину «' + car.brand + '» (ID ' + car.id + ')?')) return;
    try {
        await api('/api/cars/' + car.id, {method: 'DELETE'});
        if (byId('car-id').value === String(car.id)) resetForm();
        message('Машина удалена.', 'success');
        await refresh();
    } catch (error) { message(error.message, 'error'); }
}

function renderCars(cars) {
    const body = byId('cars');
    body.replaceChildren();
    for (const car of cars) {
        const row = document.createElement('tr');
        for (const value of [car.id, car.brand, car.manufactureYear, car.registeredOn, car.ownerName, car.mileage]) {
            const cell = document.createElement('td');
            cell.textContent = String(value);
            row.append(cell);
        }
        const actions = document.createElement('td');
        const editButton = document.createElement('button');
        editButton.type = 'button';
        editButton.className = 'secondary';
        editButton.textContent = 'Изменить';
        editButton.addEventListener('click', () => edit(car));
        const deleteButton = document.createElement('button');
        deleteButton.type = 'button';
        deleteButton.className = 'danger';
        deleteButton.textContent = 'Удалить';
        deleteButton.addEventListener('click', () => remove(car));
        actions.append(editButton, deleteButton);
        row.append(actions);
        body.append(row);
    }
    byId('count').textContent = 'Записей: ' + cars.length;
    byId('empty').hidden = cars.length !== 0;
}

function renderHistogram(days) {
    const maximum = Math.max(1, ...days.map(day => day.count));
    const chart = byId('histogram');
    chart.replaceChildren();
    for (const day of days) {
        const column = document.createElement('div');
        column.className = 'day';
        const area = document.createElement('div');
        area.className = 'bar-space';
        const bar = document.createElement('div');
        bar.className = 'bar';
        bar.style.height = (day.count / maximum * 100) + '%';
        area.append(bar);
        const count = document.createElement('strong');
        count.textContent = String(day.count);
        const label = document.createElement('span');
        label.textContent = day.date.slice(8) + '.' + day.date.slice(5, 7);
        column.title = day.date + ': ' + day.count;
        column.append(area, count, label);
        chart.append(column);
    }
}

async function refresh() {
    const sequence = ++refreshSequence;
    const params = new URLSearchParams({q: byId('query').value, direction: byId('direction').value});
    try {
        const [cars, days] = await Promise.all([
            api('/api/cars?' + params), api('/api/cars/histogram')
        ]);
        if (sequence !== refreshSequence) return;
        renderCars(cars);
        renderHistogram(days);
    } catch (error) {
        if (sequence === refreshSequence) throw error;
    }
}

form.addEventListener('submit', async event => {
    event.preventDefault();
    const id = byId('car-id').value;
    const car = Object.fromEntries(fields.map(field => [field, byId(field).value]));
    car.manufactureYear = Number(car.manufactureYear);
    car.mileage = Number(car.mileage);
    byId('save-button').disabled = true;
    try {
        await api('/api/cars' + (id ? '/' + id : ''), {
            method: id ? 'PUT' : 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(car)
        });
        resetForm();
        message(id ? 'Изменения сохранены.' : 'Машина добавлена.', 'success');
        await refresh();
    } catch (error) { message(error.message, 'error'); }
    finally { byId('save-button').disabled = false; }
});
byId('cancel').addEventListener('click', resetForm);
byId('filter-form').addEventListener('submit', event => {
    event.preventDefault();
    message('');
    refresh().catch(error => message(error.message, 'error'));
});
byId('direction').addEventListener('change', () => refresh().catch(error => message(error.message, 'error')));
refresh().catch(error => message(error.message, 'error'));

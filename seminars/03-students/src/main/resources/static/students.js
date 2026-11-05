const form = document.querySelector('#student-form');
const message = document.querySelector('#message');
const cancel = document.querySelector('#cancel');
let editingId = null;

async function request(method, path, body) {
  const response = await fetch(path, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  if (!response.ok) {
    const messages = { 400: 'Проверьте ID, имя и фамилию.', 404: 'Студент не найден. Обновите страницу.', 409: 'Студент с таким ID уже существует.' };
    throw new Error(messages[response.status] || 'Не удалось выполнить запрос. Попробуйте ещё раз.');
  }
}

function resetForm() {
  editingId = null;
  form.reset();
  form.elements.id.disabled = false;
  cancel.hidden = true;
  document.querySelector('#form-title').textContent = 'Добавить студента';
}

cancel.addEventListener('click', resetForm);
form.addEventListener('submit', async event => {
  event.preventDefault();
  message.textContent = '';
  const student = { firstName: form.elements.firstName.value.trim(), lastName: form.elements.lastName.value.trim() };
  const submit = form.querySelector('[type="submit"]');
  submit.disabled = true;
  try {
    if (editingId !== null) {
      await request('PUT', '/db/students/' + editingId, student);
    } else {
      student.id = Number(form.elements.id.value);
      await request('POST', '/db/students', student);
    }
    window.location.reload();
  } catch (error) {
    message.textContent = error.message;
  } finally {
    submit.disabled = false;
  }
});

document.querySelector('tbody').addEventListener('click', async event => {
  const button = event.target.closest('button[data-action]');
  if (!button) return;
  const cells = button.closest('tr').cells;
  const id = cells[0].textContent;
  if (button.dataset.action === 'edit') {
    editingId = id;
    form.elements.id.value = id;
    form.elements.id.disabled = true;
    form.elements.firstName.value = cells[1].textContent;
    form.elements.lastName.value = cells[2].textContent;
    cancel.hidden = false;
    document.querySelector('#form-title').textContent = 'Редактировать студента';
    form.elements.firstName.focus();
    return;
  }
  if (!window.confirm('Удалить студента №' + id + '?')) return;
  button.disabled = true;
  try {
    await request('DELETE', '/db/students/' + id);
    window.location.reload();
  } catch (error) {
    message.textContent = error.message;
    button.disabled = false;
  }
});

// 응답자 페이지: 토큰 발급 → 설문 렌더 → 답안 제출.
const slug = location.pathname.split('/').pop();
const tokenKey = `al_token_${slug}`;

async function ensureToken() {
  const cached = localStorage.getItem(tokenKey);
  if (cached) return cached;
  const r = await fetch(`/api/public/surveys/${slug}/responses`, { method: 'POST' }).then(r => r.json());
  localStorage.setItem(tokenKey, r.access_token);
  return r.access_token;
}

async function render() {
  const token = await ensureToken();
  const data = await fetch(`/api/public/responses/${token}`).then(r => r.json());
  document.getElementById('title').textContent = data.survey.title;
  document.getElementById('deadline').textContent = data.survey.deadline;
  const form = document.getElementById('f');
  const answers = new Map(data.answers.map(a => [a.question_id, a]));
  data.survey.questions.forEach(q => {
    const div = document.createElement('div');
    const existing = answers.get(q.id);
    if (q.type === 'TEXT') {
      div.innerHTML = `<label>${q.text}<textarea name="${q.id}">${existing?.text_value ?? ''}</textarea></label>`;
    }
    if (q.type === 'SCALE') {
      div.innerHTML = `<label>${q.text} (1-5)<input type="number" min="1" max="5" name="${q.id}" data-kind="scale" value="${existing?.scale_value ?? ''}"></label>`;
    }
    if (q.type === 'SINGLE_CHOICE') {
      const opts = q.options.map(o =>
        `<label><input type="radio" name="${q.id}" value="${o.id}" data-kind="option" ${existing?.question_option_id === o.id ? 'checked' : ''}>${o.text}</label>`,
      ).join('');
      div.innerHTML = `<fieldset><legend>${q.text}</legend>${opts}</fieldset>`;
    }
    form.appendChild(div);
  });
}

document.getElementById('submit').onclick = async () => {
  const token = localStorage.getItem(tokenKey);
  const form = document.getElementById('f');
  const items = [];
  new FormData(form).forEach((v, k) => {
    const el = form.querySelector(`[name="${k}"]`);
    const kind = el.dataset.kind;
    if (kind === 'scale') items.push({ question_id: Number(k), scale_value: Number(v) });
    else if (kind === 'option') items.push({ question_id: Number(k), question_option_id: Number(v) });
    else items.push({ question_id: Number(k), text_value: String(v) });
  });
  const r = await fetch(`/api/public/responses/${token}/answers`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(items),
  });
  alert(r.ok ? '제출되었습니다.' : `오류: ${r.status}`);
};

render();

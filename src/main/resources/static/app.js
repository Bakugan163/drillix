// =============================================================
//  Drillix — Frontend (MVP)
// =============================================================

const API_BASE = 'http://localhost:8080';

const ENUMS = {
  wellType: { VERTICAL: 'Вертикальная', DIRECTIONAL: 'Наклонно-направленная', HORIZONTAL: 'Горизонтальная', SIDE_TRACK: 'ЗБС' },
  sectionType: { DIRECTION: 'Направление', CONDUCTOR: 'Кондуктор', PILOT: 'Пилотная', TECHNICAL: 'Техническая', PRODUCTION: 'Эксплуатационная', LINER: 'Хвостовик' },
  trajectoryTask: { VERTICAL: 'Вертикальный участок', BUILD: 'Набор угла', HOLD: 'Стабилизация', DROP: 'Сброс угла', CORRECTION: 'Коррекция' },
  equipmentType: { BIT: 'Долото', CALIPER: 'Калибратор', STABILIZER: 'Стабилизатор', MOTOR: 'ВЗД', RSS: 'РУС', MWD: 'Телесистема (MWD/LWD)', NMDC: 'НУБТ', DC: 'УБТ', SUB: 'Переводник' },
  role: { ROLE_ADMIN: 'Администратор', ROLE_TECHNOLOGIST: 'Технолог' },
  recommendationStatus: { DRAFT: 'Черновик', APPROVED: 'Согласована', REJECTED: 'Отклонена' },
};

const state = {
  auth: sessionStorage.getItem('auth') || null,
  username: sessionStorage.getItem('username') || null,
  role: sessionStorage.getItem('role') || null,
  route: { page: 'wells', wellId: null, sectionId: null },
  equipmentCache: []
};

// --- Утилиты ---
function escapeHtml(str) { if (!str) return ''; return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;'); }
function label(map, key) { return map[key] || key || '—'; }
function fmtDate(v) { if (!v) return '—'; try { const d = new Date(v); if (isNaN(d.getTime())) return '—'; return d.toLocaleDateString('ru-RU'); } catch { return '—'; } }
function fmtDateTime(v) { if (!v) return '—'; try { const d = new Date(v); if (isNaN(d.getTime())) return '—'; return d.toLocaleString('ru-RU'); } catch { return '—'; } }
function showFormError(form, msg) { const el = form.querySelector('.form-error'); if (el) { el.textContent = msg; el.hidden = false; } else { alert(msg); } }

// --- API ---
async function api(path, opts = {}) {
  const headers = new Headers(opts.headers || {});
  if (state.auth) headers.set('Authorization', 'Basic ' + state.auth);
  if (opts.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  
  const res = await fetch(API_BASE + path, { ...opts, headers });
  
  if (res.status === 401) { clearAuth(); showLogin(); throw new Error('401: Неверный логин или пароль'); }
  
  if (!res.ok) {
    let text = await res.text();
    throw new Error(`HTTP ${res.status}: ${text || res.statusText}`);
  }
  if (res.status === 204) return null;
  const ct = res.headers.get('content-type') || '';
  if (ct.includes('application/json')) return res.json();
  return res.text();
}

// --- Auth & Роли ---
function setAuth(u, p, r = null) { 
  state.auth = btoa(unescape(encodeURIComponent(`${u}:${p}`))); 
  state.username = u; 
  if (r) state.role = r;
  sessionStorage.setItem('auth', state.auth); 
  sessionStorage.setItem('username', u); 
  if (r) sessionStorage.setItem('role', r);
}
function clearAuth() { state.auth = null; state.username = null; state.role = null; sessionStorage.removeItem('auth'); sessionStorage.removeItem('username'); sessionStorage.removeItem('role'); }
function showLogin() { document.getElementById('app').hidden = true; document.getElementById('login-screen').hidden = false; }
function showApp() { 
  document.getElementById('login-screen').hidden = true; 
  document.getElementById('app').hidden = false; 
  document.getElementById('current-user').textContent = `${state.username} (${label(ENUMS.role, state.role)})`; 
  const navUsers = document.querySelector('#nav button[data-page="users"]');
  if (navUsers) navUsers.style.display = (state.role === 'ROLE_ADMIN') ? 'block' : 'none';
}

// --- Router & UI ---
function parseRoute() {
  const parts = (location.hash || '#/wells').replace(/^#\/?/, '').split('/').filter(Boolean);
  const r = { page: parts[0] || 'wells', wellId: null, sectionId: null };
  if (r.page === 'wells') {
    if (parts[1]) r.wellId = parts[1];
    if (parts[2] === 'sections') r.sectionId = parts[3];
  }
  return r;
}
function navigate(hash) { if (location.hash === hash) render(); else location.hash = hash; }
function setBreadcrumbs(items) { document.getElementById('breadcrumbs').innerHTML = items.map((it, i) => i === 0 ? (it.href ? `<a href="${it.href}">${it.label}</a>` : it.label) : `<span class="sep"> / </span>${it.href ? `<a href="${it.href}">${it.label}</a>` : it.label}`).join(''); }
function openModal(title, el) { document.getElementById('modal-title').textContent = title; document.getElementById('modal-body').innerHTML = ''; document.getElementById('modal-body').appendChild(el); document.getElementById('modal').hidden = false; }
function closeModal() { document.getElementById('modal').hidden = true; }

// =============================================================
//  VIEW: Скважины
// =============================================================
async function renderWells() {
  setBreadcrumbs([{ label: 'Скважины' }]);
  const el = document.getElementById('content');
  el.innerHTML = '<p>Загрузка...</p>';
  const wells = await api('/api/wells');
  
  el.innerHTML = `
    <div class="page-header">
      <h1>Скважины</h1>
      <button class="btn primary" id="create-well">+ Создать скважину</button>
    </div>
    <div class="table-wrap">
      <table>
        <thead><tr><th>№</th><th>Месторождение</th><th>№ скв.</th><th>Тип</th><th>Глубина</th><th>Начало</th><th>Окончание</th><th></th></tr></thead>
        <tbody>
          ${wells.map((w, idx) => `
            <tr>
              <td>${idx + 1}</td>
              <td><strong>${escapeHtml(w.field)}</strong></td>
              <td>${escapeHtml(w.wellNumber)}</td>
              <td>${label(ENUMS.wellType, w.wellType)}</td>
              <td>${w.projectDepth} м</td>
              <td>${fmtDate(w.drillingStartDate || w.drillinggStartDate)}</td>
              <td>${fmtDate(w.completionDate)}</td>
              <td class="actions">
                <button class="btn small primary" data-action="open" data-id="${w.id}">Открыть</button>
                <button class="btn small" data-action="edit" data-id="${w.id}">Изменить</button>
                <button class="btn small danger" data-action="del" data-id="${w.id}">X</button>
              </td>
            </tr>
          `).join('') || '<tr><td colspan="8" class="empty">Нет скважин</td></tr>'}
        </tbody>
      </table>
    </div>
  `;

  document.getElementById('create-well').onclick = () => openWellForm();
  el.querySelectorAll('button[data-action]').forEach(btn => {
    const w = wells.find(x => x.id == btn.dataset.id);
    btn.onclick = () => {
      if (btn.dataset.action === 'open') navigate(`#/wells/${w.id}`);
      if (btn.dataset.action === 'edit') openWellForm(w);
      if (btn.dataset.action === 'del' && confirm('Удалить скважину и все её секции?')) api(`/api/wells/${w.id}`, {method: 'DELETE'}).then(render);
    };
  });
}

function openWellForm(w = null) {
  const isEdit = !!w;
  const form = document.createElement('form'); form.className = 'form';
  form.innerHTML = `
    <label>Месторождение *<input name="field" required value="${w?.field || ''}"></label>
    <label>Номер скважины *<input name="wellNumber" required value="${w?.wellNumber || ''}"></label>
    <label>Тип скважины *
      <select name="wellType">${Object.entries(ENUMS.wellType).map(([k,v]) => `<option value="${k}" ${w?.wellType===k?'selected':''}>${v}</option>`).join('')}</select>
    </label>
    <label>Проектная глубина (м) *<input name="projectDepth" type="number" required value="${w?.projectDepth || ''}"></label>
    <label>Дата начала бурения<input name="drillingStartDate" type="date" value="${w?.drillingStartDate || w?.drillinggStartDate || ''}"></label>
    <label>Дата окончания<input name="completionDate" type="date" value="${w?.completionDate || ''}"></label>
    <p class="form-error" hidden></p>
    <div class="form-actions"><button type="button" class="btn" onclick="closeModal()">Отмена</button><button type="submit" class="btn primary">Сохранить</button></div>
  `;
  form.onsubmit = async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const body = Object.fromEntries(fd.entries());
    body.projectDepth = Number(body.projectDepth);
    
    const startDate = form.querySelector('input[name="drillingStartDate"]').value;
    const endDate = form.querySelector('input[name="completionDate"]').value;
    
    if (startDate && endDate && endDate < startDate) return showFormError(form, 'Дата окончания не может быть раньше даты начала');
    if (startDate) body.drillingStartDate = startDate;
    if (endDate) body.completionDate = endDate;
    
    try {
      await api(isEdit ? `/api/wells/${w.id}` : '/api/wells', { method: isEdit ? 'PUT' : 'POST', body: JSON.stringify(body) });
      closeModal(); render();
    } catch (err) { showFormError(form, err.message); }
  };
  openModal(isEdit ? 'Изменить скважину' : 'Новая скважина', form);
}

// =============================================================
//  VIEW: Секции скважины
// =============================================================
async function renderWellDetail(wellId) {
  const el = document.getElementById('content'); el.innerHTML = '<p>Загрузка...</p>';
  const [well, sections] = await Promise.all([api(`/api/wells/${wellId}`), api(`/api/wells/${wellId}/sections`)]);
  
  setBreadcrumbs([{ label: 'Скважины', href: '#/wells' }, { label: `${well.field} / ${well.wellNumber}` }]);
  el.innerHTML = `
    <div class="page-header">
      <h1>${escapeHtml(well.field)} / ${escapeHtml(well.wellNumber)}</h1>
      <button class="btn primary" id="add-sec">+ Добавить секцию</button>
    </div>
    <div class="card">
      <dl>
        <dt>Тип</dt><dd>${label(ENUMS.wellType, well.wellType)}</dd>
        <dt>Проектная глубина</dt><dd>${well.projectDepth} м</dd>
        <dt>Даты</dt><dd>${fmtDate(well.drillingStartDate || well.drillinggStartDate)} — ${fmtDate(well.completionDate)}</dd>
      </dl>
    </div>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>№</th>
            <th>Тип секции</th>
            <th>Интервал (м)</th>
            <th>Ø Долота</th>
            <th>Траектория</th>
            <th>Зенит</th>
            <th>Условия бурения</th> 
            <th></th>
          </tr>
        </thead>
        <tbody>
          ${sections.map((s, idx) => `
            <tr>
              <td>${idx + 1}</td>
              <td><strong>${label(ENUMS.sectionType, s.type)}</strong></td>
              <td>${s.depthFrom} – ${s.depthTo}</td>
              <td>${s.bitDiametr || s.bitDiameter} мм</td>
              <td>${label(ENUMS.trajectoryTask, s.trajectoryTask)}</td>
              <td>${s.maxZenithAngle ? s.maxZenithAngle + '°' : '—'}</td>
              <td class="muted" style="max-width:200px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;" title="${escapeHtml(s.drillingCondition || '')}">
                ${escapeHtml(s.drillingCondition || '—')}
              </td>
              <td class="actions">
                <button class="btn small primary" data-action="open" data-id="${s.id}">КНБК</button>
                <button class="btn small" data-action="edit" data-id="${s.id}">Изменить</button>
                <button class="btn small danger" data-action="del" data-id="${s.id}">X</button>
              </td>
            </tr>
          `).join('') || '<tr><td colspan="8" class="empty">Секций нет</td></tr>'}
        </tbody>
      </table>
    </div>
  `;
  
  document.getElementById('add-sec').onclick = () => openSectionForm(well, sections);
  el.querySelectorAll('button[data-action]').forEach(btn => {
    const s = sections.find(x => x.id == btn.dataset.id);
    btn.onclick = () => {
      if (btn.dataset.action === 'open') navigate(`#/wells/${wellId}/sections/${s.id}`);
      if (btn.dataset.action === 'edit') openSectionForm(well, sections, s);
      if (btn.dataset.action === 'del' && confirm('Удалить секцию?')) api(`/api/wells/${wellId}/sections/${s.id}`, {method: 'DELETE'}).then(render);
    };
  });
}

function openSectionForm(well, existingSections = [], s = null) {
  const isEdit = !!s;
  const wellId = well.id;
  
  const form = document.createElement('form'); form.className = 'form';
  form.innerHTML = `
    <p class="muted" style="margin-top:0;">Проектная глубина скважины: <strong>${well.projectDepth} м</strong></p>
    <label>Тип секции *
      <select name="type">${Object.entries(ENUMS.sectionType).map(([k,v]) => `<option value="${k}" ${s?.type===k?'selected':''}>${v}</option>`).join('')}</select>
    </label>
    <div style="display:flex;gap:12px;">
      <label style="flex:1">Кровля (от), м *<input name="depthFrom" type="number" required value="${s?.depthFrom || ''}"></label>
      <label style="flex:1">Забой (до), м *<input name="depthTo" type="number" required value="${s?.depthTo || ''}"></label>
    </div>
    <label>Диаметр долота (мм) *<input name="bitDiametr" type="number" step="0.1" required value="${s?.bitDiametr || s?.bitDiameter || ''}"></label>
    <label>Задача по траектории *
      <select name="trajectoryTask">${Object.entries(ENUMS.trajectoryTask).map(([k,v]) => `<option value="${k}" ${s?.trajectoryTask===k?'selected':''}>${v}</option>`).join('')}</select>
    </label>
    <label>Макс. зенитный угол (°)<input name="maxZenithAngle" type="number" step="0.1" value="${s?.maxZenithAngle || ''}"></label>
    
    <label>Условия бурения (геология, осложнения)
      <textarea name="drillingCondition" rows="3">${s?.drillingConditions || s?.drillingCondition || ''}</textarea>
    </label>
    
    <p class="form-error" hidden></p>
    <div class="form-actions"><button type="button" class="btn" onclick="closeModal()">Отмена</button><button type="submit" class="btn primary">Сохранить</button></div>
  `;
  
  form.onsubmit = async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    
    const depthFromStr = fd.get('depthFrom');
    const depthToStr = fd.get('depthTo');
    const bitDiametrStr = fd.get('bitDiametr');
    const maxZenithAngleStr = fd.get('maxZenithAngle');
    const drillingCondition = fd.get('drillingCondition');
    const currentType = fd.get('type');

    if (!depthFromStr || !depthToStr || !bitDiametrStr) {
      return showFormError(form, 'Заполните все обязательные поля');
    }

    const depthFrom = Number(depthFromStr);
    const depthTo = Number(depthToStr);
    const bitDiametr = Number(bitDiametrStr);

    if (isNaN(bitDiametr) || bitDiametr <= 0) {
      return showFormError(form, 'Диаметр долота должен быть положительным числом');
    }

    if (depthTo <= depthFrom) return showFormError(form, 'Забой должен быть больше кровли');
    if (depthTo > well.projectDepth) return showFormError(form, `Забой секции не может быть больше проектной глубины (${well.projectDepth} м)`);

    // ПРОВЕРКА НА ПЕРЕСЕЧЕНИЕ С УЧЕТОМ ПИЛОТНОЙ СЕКЦИИ
    if (currentType !== 'PILOT') {
      for (const sec of existingSections) {
        if (isEdit && sec.id === s.id) continue;
        if (sec.type === 'PILOT') continue; // Игнорируем пересечения с пилотом

        // Строгие знаки: 50 < 50 - ложь, значит стыковка (0-50 и 50-1200) разрешена
        if (depthFrom < sec.depthTo && depthTo > sec.depthFrom) {
          const typeName = label(ENUMS.sectionType, sec.type);
          return showFormError(
            form, 
            `Пересечение интервалов! Введенный интервал (${depthFrom}–${depthTo} м) пересекается с секцией «${typeName}» (${sec.depthFrom}–${sec.depthTo} м).`
          );
        }
      }
    }

    const body = {
      type: currentType,
      depthFrom: depthFrom,
      depthTo: depthTo,
      bitDiametr: bitDiametr,
      trajectoryTask: fd.get('trajectoryTask')
    };

    if (maxZenithAngleStr) body.maxZenithAngle = Number(maxZenithAngleStr);
    
    if (drillingCondition && drillingCondition.trim()) {
      const text = drillingCondition.trim();
      body.drillingCondition = text; 
      body.drillingConditions = text;
    }

    try {
      await api(isEdit ? `/api/wells/${wellId}/sections/${s.id}` : `/api/wells/${wellId}/sections`, { 
        method: isEdit ? 'PUT' : 'POST', body: JSON.stringify(body) 
      });
      closeModal(); render();
    } catch (err) { showFormError(form, err.message); }
  };
  openModal(isEdit ? 'Изменить секцию' : 'Новая секция', form);
}

// =============================================================
//  VIEW: Секция -> Рекомендации КНБК
// =============================================================
async function renderSectionDetail(wellId, sectionId) {
  const el = document.getElementById('content'); el.innerHTML = '<p>Загрузка...</p>';
  const [well, section, recs] = await Promise.all([
    api(`/api/wells/${wellId}`), 
    api(`/api/wells/${wellId}/sections/${sectionId}`), 
    api(`/api/wells/${wellId}/sections/${sectionId}/recommendations`)
  ]);
  
  setBreadcrumbs([{ label: 'Скважины', href: '#/wells' }, { label: `${well.field} / ${well.wellNumber}`, href: `#/wells/${wellId}` }, { label: 'КНБК' }]);
  
  el.innerHTML = `
    <div class="page-header">
      <h1>Секция ${label(ENUMS.sectionType, section.type)} (${section.depthFrom}–${section.depthTo}м)</h1>
      <button class="btn primary" id="gen-ai">✨ Сгенерировать КНБК</button>
    </div>
    
    <div class="alert alert-warning">
      <strong>Внимание:</strong> Все результаты генерируются нейросетью как черновик и требуют обязательной проверки технологом.
    </div>

    <div class="card">
      <h2>Параметры секции</h2>
      <dl>
        <dt>Диаметр долота</dt><dd>${section.bitDiametr || section.bitDiameter || '—'} мм</dd>
        <dt>Траектория</dt><dd>${label(ENUMS.trajectoryTask, section.trajectoryTask)}</dd>
        <dt>Зенитный угол</dt><dd>${section.maxZenithAngle ? section.maxZenithAngle + '°' : '—'}</dd>
        <dt>Условия</dt><dd>${escapeHtml(section.drillingConditions || section.drillingCondition || '—')}</dd>
      </dl>
    </div>

    <h2 style="font-size:18px;margin-bottom:12px;">История рекомендаций</h2>
    <div class="table-wrap">
      <table>
        <thead><tr><th>№</th><th>Статус</th><th>Модель AI</th><th>Дата</th><th></th></tr></thead>
        <tbody>
          ${recs.map((r, idx) => {
            // Защита от null/undefined
            const status = r.status || 'DRAFT'; 
            const statusClass = status === 'DRAFT' ? 'draft' : status.toLowerCase();
            const recId = r.recommendationId || '0';
            const modelName = r.model || '—';
            const dateStr = fmtDateTime(r.createdAt || r.createAt || r.createDAt);
            
            return `
            <tr>
              <td>${idx + 1}</td>
              <td><span class="badge ${statusClass}">${label(ENUMS.recommendationStatus, status)}</span></td>
              <td>${escapeHtml(modelName)}</td>
              <td>${dateStr}</td>
              <td class="actions">
                <button class="btn small primary" data-action="view" data-id="${recId}">Открыть</button>
                <button class="btn small" data-action="audit" data-id="${recId}">Аудит</button>
                <button class="btn small" data-action="export" data-id="${recId}" title="Скачать Excel">⤓ Excel</button>
                <button class="btn small" data-action="approve" data-id="${recId}" style="color:var(--success)" title="Согласовать">✓</button>
                <button class="btn small danger" data-action="reject" data-id="${recId}" title="Отклонить">✗</button>
                <button class="btn small danger" data-action="delete" data-id="${recId}" title="Удалить">X</button>
              </td>
            </tr>
          `}).join('') || '<tr><td colspan="5" class="empty">Нет рекомендаций</td></tr>'}
        </tbody>
      </table>
    </div>
  `;

  document.getElementById('gen-ai').onclick = () => startAiGeneration(wellId, sectionId);
  
  el.querySelectorAll('button[data-action]').forEach(btn => {
    const r = recs.find(x => x.recommendationId == btn.dataset.id);
    
    btn.onclick = async () => {
      if (btn.dataset.action === 'view') {
        openBhaViewer(wellId, sectionId, r, section);
      }
      if (btn.dataset.action === 'audit') {
        openAuditModal(r);
      }
      if (btn.dataset.action === 'export') {
        await exportRecommendation(wellId, sectionId, r.recommendationId); 
      }
      if (btn.dataset.action === 'approve') {
        if(confirm('Согласовать данную КНБК?')) {
          try {
            await api(`/api/wells/${wellId}/sections/${sectionId}/recommendations/${r.recommendationId}/approve`, {method: 'POST'});
            render();
          } catch (err) {
            alert('Ошибка при смене статуса: ' + err.message);
          }
        }
      }
      if (btn.dataset.action === 'reject') {
        if(confirm('Отклонить данную КНБК?')) {
          try {
            await api(`/api/wells/${wellId}/sections/${sectionId}/recommendations/${r.recommendationId}/reject`, {method: 'POST'});
            render();
          } catch (err) {
            alert('Ошибка при смене статуса: ' + err.message);
          }
        }
      }
      if (btn.dataset.action === 'delete') {
        if (confirm('Вы уверены, что хотите навсегда удалить эту рекомендацию КНБК?')) {
          try {
            await api(`/api/wells/${wellId}/sections/${sectionId}/recommendations/${r.recommendationId}`, { method: 'DELETE' });
            render();
          } catch (err) {
            alert('Ошибка при удалении: ' + err.message);
          }
        }
      }
    };
  });
}

// =============================================================
//  Экспорт КНБК в Excel
// =============================================================
async function exportRecommendation(wellId, sectionId, recId) {
  try {
    const url = `${API_BASE}/api/wells/${wellId}/sections/${sectionId}/recommendations/${recId}/export`;
    const headers = new Headers();
    if (state.auth) headers.set('Authorization', 'Basic ' + state.auth);

    const res = await fetch(url, { headers });
    
    if (!res.ok) {
      const errText = await res.text();
      throw new Error(`Сервер вернул ошибку ${res.status}: ${errText}`);
    }

    const blob = await res.blob();
    
    const downloadUrl = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.style.display = 'none';
    a.href = downloadUrl;
    a.download = `КНБК_${recId}_Секция_${sectionId}.xlsx`; 
    document.body.appendChild(a);
    a.click();
    
    setTimeout(() => {
      document.body.removeChild(a);
      window.URL.revokeObjectURL(downloadUrl);
    }, 100);
    
  } catch (err) {
    alert('Не удалось скачать Excel: ' + err.message);
  }
}

// =============================================================
//  AI Генерация
// =============================================================
function startAiGeneration(wellId, sectionId) {
  const div = document.createElement('div');
  div.innerHTML = `
    <div style="text-align:center; padding: 40px 20px;">
      <div class="spinner" style="width:48px; height:48px; border-width: 4px; margin-bottom: 20px;"></div>
      <h3 style="margin-bottom:8px;">Нейросеть подбирает КНБК...</h3>
      <p style="font-size:24px; font-weight:bold; margin-top:20px;" id="gen-timer">0 сек</p>
    </div>
    <p class="form-error" hidden id="gen-err"></p>
    <div class="form-actions" id="gen-actions"><button type="button" class="btn" onclick="closeModal()">Отменить</button></div>
  `;
  openModal('AI-генерация', div);
  
  const controller = new AbortController();
  const start = Date.now();
  const interval = setInterval(() => { document.getElementById('gen-timer').textContent = Math.floor((Date.now() - start)/1000) + ' сек'; }, 1000);
  const timeout = setTimeout(() => controller.abort(), 180000); // 180 секунд

  api(`/api/wells/${wellId}/sections/${sectionId}/recommendations`, { method: 'POST', signal: controller.signal })
    .then(() => { 
      clearInterval(interval); 
      clearTimeout(timeout); 
      closeModal(); 
      setTimeout(() => render(), 500); 
    })
    .catch(err => {
      clearInterval(interval); 
      clearTimeout(timeout);
      document.getElementById('gen-err').textContent = err.name === 'AbortError' ? 'Таймаут (180 сек). Модель не успела ответить.' : 'Ошибка: ' + err.message;
      document.getElementById('gen-err').hidden = false;
      document.getElementById('gen-actions').innerHTML = `
        <button type="button" class="btn" onclick="closeModal()">Закрыть</button>
        <button type="button" class="btn primary" onclick="closeModal(); startAiGeneration(${wellId}, ${sectionId})">Повторить</button>
      `;
    });
}

// =============================================================
//  Просмотр КНБК (Схема)
// =============================================================
async function openBhaViewer(wellId, sectionId, rec, section) {
  const div = document.createElement('div');
  div.innerHTML = '<div style="text-align:center; padding:20px;"><div class="spinner"></div></div>';
  openModal(`КНБК №${rec.recommendationId}`, div);

  const elements = rec.elements || [];
  const justificationText = rec.justtification || rec.justification || 'Нет обоснования';

  div.innerHTML = `
    <div class="card" style="margin-bottom:16px;">
      <dl>
        <dt>Обоснование AI</dt>
        <dd style="white-space:pre-wrap; font-size:13px; background:#f7fafc; padding:12px; border-radius:6px;">${escapeHtml(justificationText)}</dd>
      </dl>
    </div>
    
    <h3>Схема компоновки (от долота вверх)</h3>
    <div class="bha-scheme">
      ${elements.sort((a,b) => a.orderNo - b.orderNo).map(el => `
        <div class="bha-element">
          <div class="order">${el.orderNo}</div>
          <div class="info">
            <div class="name">${el.equipmentId ? `ID: ${el.equipmentId} — ${escapeHtml(el.equipmentName || 'Оборудование')}` : escapeHtml(el.freeParams || el.params || 'Без описания')}</div>
            <div class="sub">
              Тип: <span class="badge type">${label(ENUMS.equipmentType, el.type)}</span>
              ${el.comment ? `<span style="margin-left: 12px; color: var(--muted);">💬 ${escapeHtml(el.comment)}</span>` : ''}
            </div>
          </div>
        </div>
      `).join('')}
    </div>

    <div class="form-actions">
      <button type="button" class="btn" onclick="closeModal()">Закрыть</button>
      <button type="button" class="btn primary" id="btn-edit-bha">Редактировать состав</button>
    </div>
  `;

  document.getElementById('btn-edit-bha').onclick = () => openBhaEditor(wellId, sectionId, rec.recommendationId, elements, section);
}

// =============================================================
//  Редактор КНБК
// =============================================================
async function openBhaEditor(wellId, sectionId, recId, elements, section) {
  const div = document.createElement('div');
  div.innerHTML = '<div style="text-align:center; padding:20px;"><div class="spinner"></div>Загрузка...</div>';
  openModal('Редактирование КНБК', div);

  if (state.equipmentCache.length === 0) state.equipmentCache = await api('/api/equipment');
  
  const bitDiameter = Number(section.bitDiametr || section.bitDiameter || 0);
  const allowedEquipment = state.equipmentCache.filter(e => Number(e.diameter) <= bitDiameter);

  let rows = (elements || []).map(el => ({
    equipmentId: el.equipmentId ?? '',
    type: el.type ?? '',
    params: el.freeParams || el.params || '',
    comment: el.comment ?? '',
  }));
  if (rows.length === 0) rows.push({ orderNo: 1, equipmentId: null, type: 'BIT', params: '', comment: '' });

  const form = document.createElement('form');
  form.className = 'form';
  
  function renderRows() {
    const container = form.querySelector('#rows-container');
    container.innerHTML = rows.map((r, i) => `
      <div class="element-editor-row" data-idx="${i}">
        <div class="order">${i + 1}</div>
        
        <select class="mode-select">
          <option value="db" ${r.equipmentId ? 'selected' : ''}>Из БД</option>
          <option value="free" ${!r.equipmentId ? 'selected' : ''}>Свободный</option>
        </select>

        <div class="db-mode" style="display:${r.equipmentId ? 'block' : 'none'}">
          <select class="eq-select" style="width:100%">
            <option value="">-- Выберите --</option>
            ${allowedEquipment.map(e => `<option value="${e.id}" ${r.equipmentId == e.id ? 'selected' : ''}>${label(ENUMS.equipmentType, e.type)} — ${escapeHtml(e.name)} (${e.diameter}мм)</option>`).join('')}
          </select>
        </div>

        <div class="free-mode" style="display:${!r.equipmentId ? 'flex' : 'none'}; gap:8px;">
          <select class="free-type" style="width:100px">
            ${Object.entries(ENUMS.equipmentType).map(([k,v]) => `<option value="${k}" ${r.type === k ? 'selected' : ''}>${v}</option>`).join('')}
          </select>
          <input class="free-params" placeholder="Текстовое описание" value="${r.params || ''}" style="flex:1">
        </div>

        <input class="comment-input" placeholder="Комментарий технолога" value="${escapeHtml(r.comment || '')}">

        <button type="button" class="icon-btn" onclick="this.closest('.element-editor-row').remove()">×</button>
      </div>
    `).join('');
    
    container.querySelectorAll('.element-editor-row').forEach((el, i) => {
      el.querySelector('.order').textContent = i + 1;
      el.querySelector('.mode-select').onchange = (e) => {
        el.querySelector('.db-mode').style.display = e.target.value === 'db' ? 'block' : 'none';
        el.querySelector('.free-mode').style.display = e.target.value === 'free' ? 'flex' : 'none';
      };
    });
  }

  form.innerHTML = `
    <p class="alert alert-info">Позиция 1 — всегда долото. Показано оборудование с диаметром до ${bitDiameter} мм.</p>
    <div id="rows-container"></div>
    <button type="button" class="btn" id="add-row" style="margin-top:12px; align-self:flex-start;">+ Добавить элемент</button>
    <p class="form-error" hidden></p>
    <div class="form-actions">
      <button type="button" class="btn" onclick="closeModal()">Отмена</button>
      <button type="submit" class="btn primary">Сохранить КНБК</button>
    </div>
  `;
  
  div.innerHTML = ''; 
  div.appendChild(form); 
  renderRows();

  form.querySelector('#add-row').onclick = () => { 
    rows.push({ equipmentId: null, type: 'DC', params: '', comment: '' }); 
    renderRows(); 
  };

  form.onsubmit = async (e) => {
    e.preventDefault();
    const payloadElements = [];
    const rowEls = form.querySelectorAll('.element-editor-row');
    
    for (let i = 0; i < rowEls.length; i++) {
      const el = rowEls[i];
      const isDb = el.querySelector('.mode-select').value === 'db';
      const commentVal = el.querySelector('.comment-input').value.trim();

      const payload = {
        equipmentId: isDb ? Number(el.querySelector('.eq-select').value) : null,
        type: isDb ? null : el.querySelector('.free-type').value,
        params: isDb ? null : el.querySelector('.free-params').value,
        comment: commentVal || null 
      };
      
      if (isDb && !payload.equipmentId) return showFormError(form, `Строка ${i+1}: выберите оборудование`);
      if (!isDb && !payload.params) return showFormError(form, `Строка ${i+1}: введите текст`);
      
      payloadElements.push(payload);
    }

    try {
      await api(`/api/wells/${wellId}/sections/${sectionId}/recommendations/${recId}/bha`, { 
        method: 'PUT', 
        body: JSON.stringify({ elements: payloadElements }) 
      });
      closeModal(); 
      render();
    } catch (err) { showFormError(form, err.message); }
  };
}

// =============================================================
//  Аудит
// =============================================================
function openAuditModal(rec) {
  const div = document.createElement('div');
  div.innerHTML = `
    <div style="margin-bottom:16px;"><strong>Модель:</strong> ${escapeHtml(rec.model)}<br><strong>Дата:</strong> ${fmtDateTime(rec.createdAt)}</div>
    <label style="font-weight:bold; font-size:13px;">Сырой ответ AI:</label>
    <pre style="background:#2d3748; color:#a0aec0; padding:16px; border-radius:6px; overflow-x:auto; font-size:12px; margin-top:8px;">${escapeHtml(rec.rawResponse || rec.rawAiResponse || 'Отсутствует')}</pre>
    <div class="form-actions"><button class="btn" onclick="closeModal()">Закрыть</button></div>
  `;
  openModal('Аудит генерации', div);
}

// =============================================================
//  Справочник оборудования
// =============================================================
async function renderEquipment() {
  setBreadcrumbs([{ label: 'Справочник оборудования' }]);
  const el = document.getElementById('content'); el.innerHTML = '<p>Загрузка...</p>';
  
  try {
    const items = await api('/api/equipment');
    const isAdmin = state.role === 'ROLE_ADMIN';
    
    el.innerHTML = `
      <div class="page-header">
        <h1>Справочник оборудования</h1>
        ${isAdmin ? '<button class="btn primary" id="add-eq">+ Добавить</button>' : ''}
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>№</th><th>Тип</th><th>Наименование</th><th>Диаметр</th><th>Описание</th>${isAdmin ? '<th></th>' : ''}</tr></thead>
          <tbody>
            ${items.map((e, idx) => `
              <tr>
                <td>${idx + 1}</td>
                <td><span class="badge type">${label(ENUMS.equipmentType, e.type)}</span></td>
                <td>${escapeHtml(e.name)}</td>
                <td>${e.diameter} мм</td>
                <td class="muted" style="max-width:300px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${escapeHtml(e.description || '')}</td>
                ${isAdmin ? `
                  <td class="actions">
                    <button class="btn small" data-action="edit" data-id="${e.id}">Изменить</button>
                    <button class="btn small danger" data-action="del" data-id="${e.id}">X</button>
                  </td>
                ` : ''}
              </tr>
            `).join('') || `<tr><td colspan="${isAdmin ? 6 : 5}" class="empty">Справочник пуст</td></tr>`}
          </tbody>
        </table>
      </div>
    `;
    
    if (isAdmin) {
      document.getElementById('add-eq').onclick = () => openEquipmentForm();
      el.querySelectorAll('button[data-action]').forEach(btn => {
        const e = items.find(x => x.id == btn.dataset.id);
        btn.onclick = () => {
          if (btn.dataset.action === 'edit') openEquipmentForm(e);
          if (btn.dataset.action === 'del' && confirm('Удалить позицию?')) api(`/api/equipment/${e.id}`, {method: 'DELETE'}).then(render).catch(err => alert(err.message));
        };
      });
    }
  } catch (err) {
    if (err.message.includes('403')) {
      el.innerHTML = '<div class="alert alert-warning">У вас нет прав на просмотр справочника оборудования. Обратитесь к администратору.</div>';
    } else {
      el.innerHTML = `<div class="alert alert-warning">Ошибка загрузки: ${escapeHtml(err.message)}</div>`;
    }
  }
}

function openEquipmentForm(e = null) {
  const isEdit = !!e;
  const form = document.createElement('form'); form.className = 'form';
  form.innerHTML = `
    <label>Тип *<select name="type">${Object.entries(ENUMS.equipmentType).map(([k,v]) => `<option value="${k}" ${e?.type===k?'selected':''}>${v}</option>`).join('')}</select></label>
    <label>Наименование *<input name="name" required value="${e?.name || ''}"></label>
    <label>Диаметр (мм) *<input name="diameter" type="number" step="0.1" required value="${e?.diameter || ''}"></label>
    <label>Описание<textarea name="description" rows="3">${e?.description || ''}</textarea></label>
    <p class="form-error" hidden></p>
    <div class="form-actions"><button type="button" class="btn" onclick="closeModal()">Отмена</button><button type="submit" class="btn primary">Сохранить</button></div>
  `;
  form.onsubmit = async (ev) => {
    ev.preventDefault();
    const body = Object.fromEntries(new FormData(form).entries());
    body.diameter = Number(body.diameter);
    try {
      await api(isEdit ? `/api/equipment/${e.id}` : '/api/equipment', { method: isEdit ? 'PUT' : 'POST', body: JSON.stringify(body) });
      closeModal(); render();
    } catch (err) { showFormError(form, err.message); }
  };
  openModal(isEdit ? 'Изменить оборудование' : 'Новое оборудование', form);
}

// =============================================================
//  Пользователи (Только для Админа)
// =============================================================
async function renderUsers() {
  if (state.role !== 'ROLE_ADMIN') return navigate('#/wells');
  
  setBreadcrumbs([{ label: 'Пользователи' }]);
  const el = document.getElementById('content');
  const users = await api('/api/users');
  el.innerHTML = `
    <div class="page-header"><h1>Пользователи</h1></div>
    <div class="table-wrap">
      <table>
        <thead><tr><th>№</th><th>Логин</th><th>Роль</th><th>Действия</th></tr></thead>
        <tbody>
          ${users.map((u, idx) => `
            <tr>
              <td>${idx + 1}</td>
              <td><strong>${escapeHtml(u.username)}</strong></td>
              <td>${label(ENUMS.role, u.role)}</td>
              <td>
                ${u.username === state.username 
                  ? '<span class="muted" style="font-size:12px;">(Это вы)</span>' 
                  : `<button class="btn small danger" onclick="api('/api/users/${u.id}',{method:'DELETE'}).then(render).catch(e=>alert(e.message))">Удалить</button>`}
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>
  `;
}

// =============================================================
//  Инициализация
// =============================================================
async function render() {
  if (!state.auth) return showLogin();
  showApp();
  state.route = parseRoute();
  
  document.querySelectorAll('#nav button').forEach(b => {
    b.classList.toggle('active', b.dataset.page === state.route.page);
  });

  try {
    if (state.route.page === 'wells') {
      if (state.route.sectionId) await renderSectionDetail(state.route.wellId, state.route.sectionId);
      else if (state.route.wellId) await renderWellDetail(state.route.wellId);
      else await renderWells();
    } else if (state.route.page === 'equipment') await renderEquipment();
    else if (state.route.page === 'users') await renderUsers();
  } catch (e) {
    if (e.message.includes('404')) {
      console.warn("Объект не найден, перенаправление на главную...");
      alert('Скважина или секция не найдена. Возможно, она была удалена.');
      navigate('#/wells');
    } else {
      document.getElementById('content').innerHTML = `<div class="alert alert-warning">Ошибка: ${escapeHtml(e.message)}</div>`;
    }
  }
}

function init() {
  document.querySelectorAll('#nav button').forEach(btn => btn.onclick = () => navigate(`#/${btn.dataset.page}`));
  document.getElementById('logout-btn').onclick = () => { clearAuth(); showLogin(); };
  document.getElementById('modal-close').onclick = closeModal;
  document.getElementById('modal').onclick = (e) => { if (e.target.id === 'modal') closeModal(); };
  window.addEventListener('hashchange', render);

  // Логин
  document.getElementById('login-form').onsubmit = async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const user = fd.get('username').trim();
    const pass = fd.get('password');
    setAuth(user, pass);
    
    try {
      await api('/api/wells'); // Базовая проверка
      
      let role = 'ROLE_TECHNOLOGIST';
      try {
        const users = await api('/api/users');
        const me = users.find(x => x.username === user);
        if (me) role = me.role;
      } catch (err) { /* ignore */ }
      
      state.role = role;
      sessionStorage.setItem('role', role);
      
      location.hash = '#/wells';
      render();
    } catch (err) {
      clearAuth();
      document.getElementById('login-error').textContent = err.message;
      document.getElementById('login-error').hidden = false;
    }
  };

  // Регистрация
  document.getElementById('show-register').onclick = () => {
    const form = document.createElement('form'); form.className = 'form';
    form.innerHTML = `
      <label>Логин *<input name="username" required minlength="1" autocomplete="off"></label>
      <label>Пароль *<input name="password" type="password" required minlength="1" autocomplete="new-password"></label>
      <p class="muted" style="font-size:12px;">По умолчанию будет выдана роль Технолога.</p>
      <p class="form-error" hidden></p>
      <div class="form-actions">
        <button type="button" class="btn" onclick="closeModal()">Отмена</button>
        <button type="submit" class="btn primary">Зарегистрироваться</button>
      </div>
    `;
    form.onsubmit = async (ev) => {
      ev.preventDefault();
      const fd = new FormData(form);
      const body = {
        username: fd.get('username').trim(),
        password: fd.get('password'),
        role: 'ROLE_TECHNOLOGIST'
      };
      try {
        await api('/api/users', { method: 'POST', body: JSON.stringify(body) });
        alert('Аккаунт создан! Теперь вы можете войти.');
        closeModal();
      } catch (err) { showFormError(form, err.message); }
    };
    openModal('Регистрация технолога', form);
  };

  if (state.auth) render(); else showLogin();
}

document.addEventListener('DOMContentLoaded', init);
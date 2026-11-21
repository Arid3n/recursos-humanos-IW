// Graba la demo del sistema RRHH sobre la interfaz real (Angular + Spring Boot).
//
// Requisitos: backend en :8080 sobre una base de datos LIMPIA (datos semilla), frontend en :4200,
//             y `npm i playwright` + Chromium instalado en el directorio desde el que se ejecuta.
// Uso:        OUT_DIR=./salida [SLOW=1] node record-demo.mjs
//             SLOW=0.15 ejecuta el guion rapido (solo para depurar); SLOW=1 es la velocidad de la demo.
// Salida:     OUT_DIR/video.webm y OUT_DIR/chapters.json (capitulos con su marca de tiempo).
import { createRequire } from 'module';
import fs from 'fs';
import path from 'path';

const require = createRequire(process.cwd() + '/');
const { chromium } = require('playwright');

const APP = 'http://localhost:4200';
const API = 'http://localhost:8080/api/v1';
const PWD = 'admin123';
const K = parseFloat(process.env.SLOW || '1');
const OUT = process.env.OUT_DIR || './salida';
fs.mkdirSync(OUT, { recursive: true });

const sleep = (ms) => new Promise((r) => setTimeout(r, ms * K));

// ───────────────────────── overlay inyectado en la pagina (cursor, rotulo, tarjetas) ─────────────────────────
const overlay = () => {
  const css = `
  body{padding-bottom:96px !important}
  .mat-mdc-snack-bar-container{margin-bottom:84px !important}
  #demo-layer{position:fixed;inset:0;pointer-events:none;z-index:2147483647;font-family:'Segoe UI',Roboto,Arial,sans-serif}
  #demo-cap{position:absolute;left:0;right:0;bottom:0;display:none;align-items:center;gap:14px;padding:12px 22px;
    background:linear-gradient(0deg,rgba(15,17,23,.96),rgba(15,17,23,.88));color:#f8fafc;font-size:19px;
    border-top:2px solid #6366f1;box-shadow:0 -6px 24px rgba(0,0,0,.35)}
  #demo-cap b{background:#6366f1;color:#fff;border-radius:6px;padding:3px 10px;font-size:15px;letter-spacing:.5px;white-space:nowrap}
  #demo-cap span{line-height:1.3}
  #demo-cursor{position:absolute;width:22px;height:22px;left:-40px;top:-40px;transition:none}
  #demo-cursor svg{filter:drop-shadow(0 2px 3px rgba(0,0,0,.55))}
  #demo-mark{position:absolute;left:0;bottom:0;width:72px;height:6px;display:flex}
  #demo-mark i{width:6px;height:6px;display:block}
  .demo-ripple{position:absolute;width:12px;height:12px;border-radius:50%;border:3px solid #f59e0b;transform:translate(-50%,-50%);
    animation:demoRip .7s ease-out forwards}
  @keyframes demoRip{to{width:64px;height:64px;opacity:0}}
  #demo-card{position:absolute;inset:0;display:none;align-items:center;justify-content:center;background:rgba(10,12,20,.9);
    backdrop-filter:blur(4px)}
  #demo-card .box{max-width:980px;width:86%;color:#f8fafc}
  #demo-card h1{font-size:42px;margin:0 0 10px;color:#fff}
  #demo-card h2{font-size:30px;margin:0 0 10px;color:#a5b4fc}
  #demo-card p,#demo-card li{font-size:21px;line-height:1.5;color:#cbd5e1}
  #demo-card .tag{display:inline-block;background:#6366f1;color:#fff;border-radius:8px;padding:4px 14px;font-size:18px;margin-bottom:14px}
  #demo-card pre{background:#0b1020;border:1px solid #2e3350;border-radius:10px;padding:14px 18px;font-size:16px;line-height:1.45;
    color:#e2e8f0;margin:10px 0;white-space:pre-wrap;word-break:break-word;font-family:'JetBrains Mono',Consolas,monospace}
  #demo-card .ok{color:#4ade80;font-weight:700}#demo-card .ko{color:#f87171;font-weight:700}
  #demo-card .req{color:#fbbf24;font-weight:700;font-size:19px}`;
  const build = () => {
    if (document.getElementById('demo-layer')) return;
    const l = document.createElement('div');
    l.id = 'demo-layer';
    l.innerHTML = `<style>${css}</style><div id="demo-cap"><b></b><span></span></div><div id="demo-card"><div class="box"></div></div><div id="demo-mark"></div>
      <div id="demo-cursor"><svg width="22" height="22" viewBox="0 0 22 22"><path d="M2 1l16 9-7 2 4 8-3 1-4-8-6 5z" fill="#fff" stroke="#111" stroke-width="1.4"/></svg></div>`;
    document.documentElement.appendChild(l);
    try {
      const c = JSON.parse(sessionStorage.getItem('demo-cap') || 'null');
      if (c) window.__demoCap(c.hu, c.text);
      const m = sessionStorage.getItem('demo-mark');
      if (m !== null) window.__demoMark(+m);
      const p = JSON.parse(sessionStorage.getItem('demo-pos') || 'null');
      if (p) window.__demoCursor(p.x, p.y);
    } catch {}
  };
  window.__demoCap = (hu, text) => {
    const el = document.getElementById('demo-cap');
    if (!el) return;
    sessionStorage.setItem('demo-cap', JSON.stringify({ hu, text }));
    el.style.display = text ? 'flex' : 'none';
    el.querySelector('b').textContent = hu;
    el.querySelector('span').textContent = text;
  };
  window.__demoMark = (id) => {
    const el = document.getElementById('demo-mark');
    if (!el) return;
    sessionStorage.setItem('demo-mark', String(id));
    el.innerHTML = '';
    for (let b = 11; b >= 0; b--) { const i = document.createElement('i'); i.style.background = (id >> b) & 1 ? '#fff' : '#000'; el.appendChild(i); }
  };
  window.__demoCursor = (x, y) => {
    const c = document.getElementById('demo-cursor');
    if (c) { c.style.left = x + 'px'; c.style.top = y + 'px'; }
    sessionStorage.setItem('demo-pos', JSON.stringify({ x, y }));
  };
  window.__demoRipple = (x, y) => {
    const l = document.getElementById('demo-layer');
    if (!l) return;
    const r = document.createElement('div');
    r.className = 'demo-ripple'; r.style.left = x + 'px'; r.style.top = y + 'px';
    l.appendChild(r); setTimeout(() => r.remove(), 800);
  };
  window.__demoCard = (html) => {
    const c = document.getElementById('demo-card');
    if (!c) return;
    c.style.display = html ? 'flex' : 'none';
    if (html) c.querySelector('.box').innerHTML = html;
  };
  window.addEventListener('mousemove', (e) => window.__demoCursor(e.clientX, e.clientY), true);
  if (document.documentElement) build(); else document.addEventListener('DOMContentLoaded', build);
  document.addEventListener('DOMContentLoaded', build);
};

// ───────────────────────── arranque ─────────────────────────
const browser = await chromium.launch(process.env.CHROME_PATH ? { executablePath: process.env.CHROME_PATH } : {});
const ctx = await browser.newContext({
  viewport: { width: 1280, height: 720 },
  locale: 'es-ES',
  timezoneId: 'Europe/Madrid',
  recordVideo: { dir: OUT, size: { width: 1280, height: 720 } },
  acceptDownloads: true,
});
await ctx.addInitScript(overlay);
const page = await ctx.newPage();
const t0 = Date.now();
const chapters = [];
const cards = [];
let shotN = 0;
const snap = async (n) => { if (process.env.SHOTS) await page.screenshot({ path: path.join(OUT, `shot-${String(++shotN).padStart(2, '0')}-${n}.png`) }); };
let results = [];
const errs = [];

const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
const cap = (hu, text) => page.evaluate(([h, t]) => window.__demoCap(h, t), [hu, text]);
const card = async (html, ms) => {
  cards.push({ t: (Date.now() - t0) / 1000, ms: ms * K });
  await page.evaluate((h) => window.__demoCard(h), html);
  await sleep(ms);
  await page.evaluate(() => window.__demoCard(null));
  await sleep(300);
};
const chapter = async (hu, title, text) => {
  chapters.push({ hu, title, text: text || title, t: Math.max(0, (Date.now() - t0) / 1000 - 0.5) });
  chapters.at(-1).id = chapters.length - 1;
  await page.evaluate((i) => window.__demoMark(i), chapters.length - 1);
  console.log(`[${(chapters.at(-1).t).toFixed(0)}s] ${hu} · ${title}`);
  await cap(hu, text || title);
  await sleep(900);
};
const huCard = async (hu, title, bullets, ms = 5200) => {
  chapters.push({ hu, title: 'Introducción: ' + title, text: title, t: Math.max(0, (Date.now() - t0) / 1000 - 0.3), intro: true });
  chapters.at(-1).id = chapters.length - 1;
  await page.evaluate((i) => window.__demoMark(i), chapters.length - 1);
  console.log(`[${chapters.at(-1).t.toFixed(0)}s] ${hu} ▶ ${title}`);
  await card(`<span class="tag">${hu}</span><h1>${esc(title)}</h1><ul>${bullets.map((b) => `<li>${esc(b)}</li>`).join('')}</ul>`, ms);
};

async function moveTo(loc) {
  await loc.evaluate((e) => e.scrollIntoView({ block: 'center', behavior: 'instant' }));
  await sleep(150);
  const b = await loc.boundingBox();
  if (!b) return null;
  const x = b.x + Math.min(b.width / 2, 120), y = b.y + b.height / 2;
  await page.mouse.move(x, y, { steps: Math.max(6, Math.round(28 * Math.min(K * 2, 1))) });
  return { x, y };
}
async function click(loc, after = 1000) {
  loc = loc.first();
  const p = await moveTo(loc);
  await sleep(350);
  if (p) await page.evaluate(([x, y]) => window.__demoRipple(x, y), [p.x, p.y]);
  await loc.click({ force: true });
  await sleep(after);
}
async function type(loc, text, delay = 55) {
  loc = loc.first();
  await moveTo(loc);
  await sleep(200);
  await loc.click({ force: true });
  await loc.focus();
  await loc.fill('');
  await loc.pressSequentially(text, { delay: delay * K });
  await sleep(450);
}
const nav = (label) => click(page.locator('a.nav-btn', { hasText: new RegExp('^\\s*' + label + '\\s*$') }), 1700);
async function pickMat(selectLoc, optionName, after = 700) {
  await click(selectLoc, 800);
  await click(page.getByRole('option', { name: optionName }), after);
}
async function login(user, pwd = PWD, ok = true) {
  const link = page.locator('a.login-nav-btn');
  if (await link.count()) await click(link, 1200);
  await type(page.locator('input[formcontrolname=username]'), user, 50);
  await type(page.locator('input[formcontrolname=password]'), pwd, 70);
  await click(page.locator('button[type=submit]'), ok ? 2200 : 1800);
}
async function logout() {
  await click(page.locator('.logout-btn'), 1600);
}
async function snack(ms = 3500) { await sleep(ms); }

// ───────────────────────── API (tarjetas con peticion y respuesta reales) ─────────────────────────
async function api(user, method, p, body, pwd = PWD) {
  const r = await fetch(API + p, {
    method,
    headers: { Authorization: 'Basic ' + Buffer.from(`${user}:${pwd}`).toString('base64'), 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  });
  const buf = Buffer.from(await r.arrayBuffer());
  let txt = buf.toString('utf8');
  if (txt.includes('\uFFFD')) txt = buf.toString('latin1');
  let js; try { js = JSON.parse(txt); } catch { js = txt; }
  return { status: r.status, body: js };
}
async function apiCard(title, user, method, p, body, { pwd = PWD, ms = 7500, show = true } = {}) {
  const r = await api(user, method, p, body, pwd);
  if (show) {
    let resp = typeof r.body === 'string' ? r.body : JSON.stringify(r.body, null, 2);
    const lines = (resp || '').split('\n');
    if (lines.length > 14) resp = lines.slice(0, 14).join('\n') + '\n  …';
    const good = r.status < 300;
    await card(
      `<span class="tag">API REST</span><h2>${esc(title)}</h2>
       <div class="req">${method} /api/v1${esc(p)} <span style="color:#94a3b8">· como ${esc(user)}</span></div>
       ${body ? `<pre>${esc(JSON.stringify(body, null, 2))}</pre>` : ''}
       <div class="${good ? 'ok' : 'ko'}">HTTP ${r.status}</div><pre>${esc(resp || '(sin cuerpo)')}</pre>`,
      ms);
  }
  return r;
}
const guard = async (name, fn) => {
  try { await fn(); } catch (e) { errs.push(`${name}: ${e.message.split('\n')[0]}`); console.log('  !! ' + name + ': ' + e.message.split('\n')[0]); await page.screenshot({ path: path.join(OUT, `error-${errs.length}.png`) }); }
};

const U = { rrhh: 'rrhh@empresa.com', dg: 'direccion@empresa.com', resp: 'responsable@empresa.com', emp: 'empleado@empresa.com' };

// ═════════════════════════════════════════ GUION ═════════════════════════════════════════
await page.goto(APP);
await sleep(1500);
await chapter('Inicio', 'Presentación del sistema', 'Sistema de Gestión de Recursos Humanos · Spec-Driven Development · 8 historias de usuario');
await card(
  `<span class="tag">SDD · IW</span><h1>Sistema de Gestión de Recursos Humanos</h1>
   <p>Demostración sobre la aplicación real: Angular 21 + Spring Boot 3.4 + PostgreSQL 16.</p>
   <ul><li>HU-01 Alta de empleados y trayectoria contractual · HU-02 Ficha y saldo de vacaciones</li>
   <li>HU-03 Solicitud de ausencias · HU-04 Seguimiento y cancelación</li>
   <li>HU-05 Aprobación y rechazo · HU-06 Calendario del equipo</li>
   <li>HU-07 Informes de absentismo · HU-08 Evaluación del desempeño</li></ul>
   <p>Cada historia se muestra con su flujo principal y sus escenarios de error y permisos.</p>`, 9000);

// ───────────── HU-01 ─────────────
await huCard('HU-01', 'Alta de empleados y trayectoria contractual', [
  'RRHH registra a una persona con su cuenta de acceso y su primer contrato.',
  'Validación de datos, duplicados y responsable obligatorio.',
  'Trayectoria contractual inmutable (append-only) y baja lógica.',
]);
await guard('login-ko', async () => {
  await page.goto(APP + '/login');
  await sleep(1000);
  await chapter('HU-01', 'Acceso con credenciales incorrectas', 'Escenario de error: el sistema rechaza una contraseña incorrecta');
  await type(page.locator('input[formcontrolname=username]'), U.rrhh, 50);
  await type(page.locator('input[formcontrolname=password]'), 'clave-erronea', 70);
  await click(page.locator('button[type=submit]'), 3200);
});
await guard('login-rrhh', async () => {
  await chapter('HU-01', 'Acceso como RRHH', 'RRHH inicia sesión con sus credenciales');
  await page.locator('input[formcontrolname=password]').fill('');
  await type(page.locator('input[formcontrolname=password]'), PWD, 70);
  await click(page.locator('button[type=submit]'), 2600);
});
await guard('alta-nav', async () => {
  await nav('Alta de Empleado');
});
await guard('alta-validacion', async () => {
  await chapter('HU-01', 'Validación del formulario de alta', 'Escenario de error: campos obligatorios y formato de NIF/NIE y correo');
  await type(page.locator('input[formcontrolname=firstName]'), 'Pedro');
  await type(page.locator('input[formcontrolname=nationalId]'), '123');
  await type(page.locator('input[formcontrolname=email]'), 'pedro');
  await click(page.locator('input[formcontrolname=position]'), 400);
  await click(page.locator('input[formcontrolname=lastName]'), 2800);
  await sleep(1500);
});
await guard('alta-ok', async () => {
  await chapter('HU-01', 'Alta correcta con responsable', 'Flujo principal: alta con puesto, departamento, convenio TIC y responsable Carlos Ruiz');
  await page.reload(); await sleep(1500);
  await type(page.locator('input[formcontrolname=firstName]'), 'Pedro');
  await type(page.locator('input[formcontrolname=lastName]'), 'Sánchez Vidal');
  await type(page.locator('input[formcontrolname=nationalId]'), '33333333P');
  await type(page.locator('input[formcontrolname=email]'), 'pedro.sanchez@empresa.com');
  await page.locator('input[formcontrolname=hireDate]').fill('2026-07-01');
  await sleep(700);
  await type(page.locator('input[formcontrolname=position]'), 'Analista de Datos');
  await type(page.locator('input[formcontrolname=department]'), 'Tecnología');
  await page.locator('input[formcontrolname=salary]').fill('32000'); await sleep(600);
  await pickMat(page.locator('mat-select[formcontrolname=collectiveAgreementId]'), /Convenio TIC/);
  await pickMat(page.locator('mat-select[formcontrolname=managerId]'), /Carlos Ruiz/);
  await sleep(800);
  await click(page.locator('button[type=submit]'), 2200);
  await cap('HU-01', 'El sistema crea la cuenta y muestra la clave provisional una sola vez');
  await sleep(4500);
  globalThis.pedroPwd = (await page.locator('.password-box code').innerText()).trim();
  console.log('   clave provisional capturada');
  await click(page.getByRole('button', { name: /Entendido/ }), 2000);
});
await guard('alta-dup', async () => {
  await chapter('HU-01', 'Alta duplicada', 'Escenario de error: el NIF y el correo ya existen, el alta se rechaza');
  await page.goto(APP + '/employees/new'); await sleep(1500);
  await type(page.locator('input[formcontrolname=firstName]'), 'Pedro');
  await type(page.locator('input[formcontrolname=lastName]'), 'Sánchez Vidal');
  await type(page.locator('input[formcontrolname=nationalId]'), '33333333P');
  await type(page.locator('input[formcontrolname=email]'), 'pedro.sanchez@empresa.com');
  await page.locator('input[formcontrolname=hireDate]').fill('2026-07-01'); await sleep(500);
  await type(page.locator('input[formcontrolname=position]'), 'Analista');
  await type(page.locator('input[formcontrolname=department]'), 'Tecnología');
  await page.locator('input[formcontrolname=salary]').fill('32000'); await sleep(500);
  await pickMat(page.locator('mat-select[formcontrolname=collectiveAgreementId]'), /Convenio TIC/);
  await pickMat(page.locator('mat-select[formcontrolname=managerId]'), /Carlos Ruiz/);
  await click(page.locator('button[type=submit]'), 1500);
  await snack(3800);
});
await guard('alta-sinresp', async () => {
  await chapter('HU-01', 'Responsable obligatorio', 'Regla de negocio: sin responsable solo se admite la excepción explícita «Dirección General»');
  const r = await api(U.rrhh, 'POST', '/employees', { firstName: 'Ana', lastName: 'Prueba', nationalId: '44444444A', email: 'ana.prueba@empresa.com', hireDate: '2026-07-01', position: 'Analista', department: 'Tecnología', salary: 30000, collectiveAgreementId: 1 });
  await card(`<span class="tag">API REST</span><h2>Alta sin responsable ni excepción explícita</h2><div class="req">POST /api/v1/employees <span style="color:#94a3b8">· como ${U.rrhh}</span></div><div class="${r.status < 300 ? 'ok' : 'ko'}">HTTP ${r.status}</div><pre>${esc(JSON.stringify(r.body, null, 2).split('\n').slice(0, 12).join('\n'))}</pre>`, 6500);
});
await guard('trayectoria', async () => {
  await chapter('HU-01', 'Trayectoria contractual inmutable', 'Variación retroactiva con fecha de efectos, historial append-only y rechazo de fechas duplicadas');
  const me = await api(U.emp, 'GET', '/employees/me');
  const ped = await api('pedro.sanchez@empresa.com', 'GET', '/employees/me', null, globalThis.pedroPwd);
  globalThis.pedroId = ped.body.id;
  const resp = await api(U.resp, 'GET', '/employees/me');
  globalThis.respId = resp.body.id; globalThis.lauraId = me.body.id;
  const base = { position: 'Analista Senior', department: 'Tecnología', salary: 36000, collectiveAgreementId: 2, managerId: resp.body.id };
  await apiCard('Variación contractual con fecha de efectos', U.rrhh, 'POST', `/employees/${globalThis.pedroId}/contracts`, { effectiveFrom: '2026-10-01', ...base }, { ms: 7000 });
  await apiCard('Trayectoria contractual (historial inmutable)', U.rrhh, 'GET', `/employees/${globalThis.pedroId}/contracts`, null, { ms: 8500 });
  await apiCard('Misma fecha de efectos: se rechaza', U.rrhh, 'POST', `/employees/${globalThis.pedroId}/contracts`, { effectiveFrom: '2026-10-01', ...base }, { ms: 6500 });
});

// ───────────── HU-02 ─────────────
await guard('hu02', async () => {
  await logout();
  await huCard('HU-02', 'Ficha personal y saldo de vacaciones', [
    'El saldo se calcula al consultarlo: correspondientes − disfrutados.',
    'Los días pendientes se muestran aparte; prorrateo por fecha de alta; años cerrados.',
    'Cada persona solo ve su propio saldo.',
  ]);
  await chapter('HU-02', 'Ficha y saldo de la empleada', 'Laura Martínez entra y consulta su ficha y su saldo de vacaciones del año');
  await login(U.emp);
  await nav('Mi Ficha y Saldo');
  await sleep(3500);
  await chapter('HU-02', 'Consulta de años anteriores', 'El selector permite consultar el saldo de ejercicios cerrados');
  await click(page.locator('mat-select').first(), 1200);
  const opts = page.getByRole('option');
  if (await opts.count() > 1) await click(opts.nth(1), 3500); else await page.keyboard.press('Escape');
  await chapter('HU-02', 'Saldo prorrateado por fecha de alta', 'Pedro se incorporó el 1 de julio: sus días correspondientes se prorratean');
  await apiCard('Saldo de Pedro (alta 01/07/2026)', 'pedro.sanchez@empresa.com', 'GET', '/leave/balances/me?year=2026', null, { pwd: globalThis.pedroPwd, ms: 6500 });
  await chapter('HU-02', 'Privacidad del saldo', 'Escenario de permisos: un empleado no puede consultar el saldo de otra persona');
  await apiCard('Laura intenta ver el saldo de Pedro', U.emp, 'GET', `/leave/balances/employee/${globalThis.pedroId}?year=2026`, null, { ms: 6000 });
  await apiCard('Su responsable sí puede ver el de su equipo', U.resp, 'GET', `/leave/balances/employee/${globalThis.lauraId}?year=2026`, null, { ms: 6000 });
});

// ───────────── HU-03 ─────────────
await guard('hu03', async () => {
  await huCard('HU-03', 'Solicitud de vacaciones o permisos', [
    'El servidor calcula los días laborables (festivos y fines de semana fuera).',
    'Validaciones: fechas pasadas, justificación en permisos, saldo y solapamientos.',
    'La solicitud nace en estado Pendiente, asignada a su responsable.',
  ]);
  await nav('Solicitar Ausencia');
  const range = async (a, b) => {
    await type(page.locator('input[aria-label="Fecha de inicio"]'), a, 70);
    await type(page.locator('input[aria-label="Fecha de finalización"]'), b, 70);
    await click(page.locator('h1.page-title'), 500);
  };
  const clearDates = async () => { await page.reload(); await sleep(1700); };
  const send = async () => { await click(page.getByRole('button', { name: /Enviar Solicitud/ }), 1200); await snap('send'); await sleep(2300); };

  await chapter('HU-03', 'Fecha pasada rechazada', 'Escenario de error: no se puede solicitar un periodo que ya ha comenzado');
  await pickMat(page.locator('mat-select[formcontrolname=leaveType]'), /Vacaciones/);
  await range('11/02/2026', '11/06/2026');
  await send();

  await clearDates();
  await chapter('HU-03', 'Permiso sin justificación', 'Escenario de error: un permiso exige justificación');
  await pickMat(page.locator('mat-select[formcontrolname=leaveType]'), /Permiso/);
  await range('12/22/2026', '12/22/2026');
  await send();

  await clearDates();
  await chapter('HU-03', 'Saldo insuficiente', 'Escenario de error: se pide más de lo que queda disponible (409)');
  await pickMat(page.locator('mat-select[formcontrolname=leaveType]'), /Vacaciones/);
  await range('01/04/2027', '12/31/2027');
  await sleep(2000);
  await send();

  await clearDates();
  await chapter('HU-03', 'Solicitud de vacaciones válida', 'Flujo principal: del 14 al 18 de diciembre; el servidor calcula los días laborables');
  await pickMat(page.locator('mat-select[formcontrolname=leaveType]'), /Vacaciones/);
  await range('12/14/2026', '12/18/2026');
  await sleep(2800);
  await send();

  await clearDates();
  await chapter('HU-03', 'Solapamiento con otra solicitud', 'Escenario de error: el periodo coincide con una solicitud ya registrada');
  await pickMat(page.locator('mat-select[formcontrolname=leaveType]'), /Vacaciones/);
  await range('12/16/2026', '12/17/2026');
  await sleep(1500);
  await send();

  await clearDates();
  await chapter('HU-03', 'Permiso con justificación', 'Flujo alternativo: un día de permiso retribuido con su justificación');
  await pickMat(page.locator('mat-select[formcontrolname=leaveType]'), /Permiso/);
  await range('12/22/2026', '12/22/2026');
  await type(page.locator('textarea[formcontrolname=justification]'), 'Cita médica programada', 45);
  await sleep(1200);
  await send();
});

// ───────────── HU-04 ─────────────
await guard('hu04', async () => {
  await huCard('HU-04', 'Seguimiento y cancelación de solicitudes', [
    'Historial paginado con filtro por estado.',
    'El motivo de un rechazo es visible para quien solicitó.',
    'Solo las solicitudes pendientes se pueden cancelar: liberan fechas y saldo.',
  ]);
  // solicitud auxiliar para mostrar la cancelación sin consumir las del resto de la demo
  await api(U.emp, 'POST', '/leave/requests', { leaveType: 'VACATION', startDate: '2027-01-11', endDate: '2027-01-13', justification: 'Puente de enero' });
  await api(U.emp, 'POST', '/leave/requests', { leaveType: 'PERMIT', startDate: '2026-12-29', endDate: '2026-12-29', justification: 'Gestión familiar' });
  await nav('Mis Solicitudes');
  await chapter('HU-04', 'Historial de solicitudes', 'Todas las solicitudes con su estado, periodo, días y justificación');
  await sleep(3000);
  await click(page.getByRole('button', { name: /Pendientes/ }), 2500);
  await chapter('HU-04', 'Cancelación de una solicitud pendiente', 'Flujo principal: la empleada cancela su solicitud del 11 al 13 de enero');
  await click(page.locator('tr', { hasText: 'Puente de enero' }).getByRole('button', { name: /Cancelar/ }), 2800);
  await cap('HU-04', 'El sistema pide confirmación antes de cancelar');
  await sleep(1500);
  await click(page.getByRole('button', { name: /Confirmar cancelación/ }), 2500);
  await snack(2500);
  await click(page.getByRole('button', { name: /Canceladas/ }), 3200);
  await cap('HU-04', 'La solicitud cancelada sigue en el historial con estado Cancelada');
  await click(page.getByRole('button', { name: /Todas/ }), 2500);
  const list = await api(U.emp, 'GET', '/leave/my-requests');
  globalThis.myReqs = list.body;
});

// Pedro solicita vacaciones coincidentes con Laura (para la demo del calendario)
await guard('pedro-req', async () => {
  const r = await api('pedro.sanchez@empresa.com', 'POST', '/leave/requests', { leaveType: 'VACATION', startDate: '2026-12-16', endDate: '2026-12-18', justification: 'Vacaciones de Navidad' }, globalThis.pedroPwd);
  console.log('   solicitud de Pedro:', r.status);
});

// ───────────── HU-05 ─────────────
await guard('hu05', async () => {
  await logout();
  await huCard('HU-05', 'Aprobación y rechazo de solicitudes', [
    'El responsable ve la bandeja de solicitudes pendientes de su equipo directo.',
    'Aprobar admite un comentario; rechazar exige un motivo de 5 a 500 caracteres.',
    'Estado, saldo y notificación se registran en una única transacción.',
  ]);
  await login(U.resp);
  await nav('Solicitudes del Equipo');
  await chapter('HU-05', 'Bandeja de solicitudes pendientes', 'Carlos Ruiz ve las solicitudes de Laura y de Pedro');
  await sleep(3500);
  const rows = page.locator('tr.mat-mdc-row, tr[mat-row]');
  const rowOf = (name) => rows.filter({ hasText: name });
  const permit29 = rowOf('Laura').filter({ hasText: '29/12/2026' });
  await chapter('HU-05', 'Rechazo con motivo demasiado corto', 'Escenario de error: el motivo de rechazo exige al menos 5 caracteres');
  await click(permit29.getByRole('button', { name: /Rechazar/ }), 1800);
  await type(page.locator('mat-dialog-container textarea'), 'no', 90);
  await click(page.locator('mat-dialog-container button').last(), 2600);
  await chapter('HU-05', 'Rechazo con motivo válido', 'Flujo alternativo: se rechaza el permiso indicando el motivo');
  await page.locator('mat-dialog-container textarea').fill('');
  await type(page.locator('mat-dialog-container textarea'), 'Cierre contable previsto ese día', 50);
  await click(page.locator('mat-dialog-container button').last(), 3200);
  await chapter('HU-05', 'Aprobación con comentario', 'Flujo principal: se aprueban las vacaciones de Laura con un comentario');
  await click(rowOf('Laura').filter({ hasText: 'Vacaciones' }).getByRole('button', { name: /Aprobar/ }), 1800);
  await type(page.locator('mat-dialog-container textarea'), 'Disfruta de las vacaciones', 50);
  await click(page.locator('mat-dialog-container button').last(), 3200);
  await chapter('HU-05', 'Aprobación de un permiso', 'Los permisos se aprueban igual; no descuentan días de vacaciones');
  await click(rowOf('Laura').filter({ hasText: '22/12/2026' }).getByRole('button', { name: /Aprobar/ }), 1800);
  await click(page.locator('mat-dialog-container button').last(), 3200);
  await chapter('HU-05', 'Aprobación de las vacaciones de Pedro', 'Pedro coincide con Laura del 16 al 18 de diciembre: se aprueba para ver la coincidencia en el calendario');
  await click(rowOf('Pedro').getByRole('button', { name: /Aprobar/ }), 1800);
  await click(page.locator('mat-dialog-container button').last(), 3200);
});
await guard('hu05-api', async () => {
  const fresh = await api(U.emp, 'GET', '/leave/my-requests?size=50');
  const reqs = (fresh.body && (fresh.body.content || fresh.body)) || [];
  const approved = reqs.find((x) => x.status === 'APPROVED' && x.leaveType === 'VACATION');
  if (approved) {
    await chapter('HU-05', 'Resolución doble y fuera de alcance', 'Escenarios de error: una solicitud ya resuelta devuelve 409; un empleado no puede resolver (403)');
    await apiCard('Aprobar una solicitud ya resuelta', U.resp, 'POST', `/leave/requests/${approved.id}/approve`, { comment: 'otra vez' }, { ms: 6500 });
    await chapter('HU-05', 'Cancelar una solicitud ya resuelta (regla de HU-04)', 'Regla de HU-04: una solicitud ya resuelta no se puede cancelar, solo las pendientes');
    await apiCard('Laura intenta cancelar una solicitud aprobada', U.emp, 'POST', `/leave/requests/${approved.id}/cancel`, null, { ms: 6500 });
    await apiCard('Un empleado intenta aprobar', U.emp, 'POST', `/leave/requests/${approved.id}/approve`, {}, { ms: 6000 });
  }
});
await guard('hu05-notif', async () => {
  await logout();
  await login(U.emp);
  await chapter('HU-05', 'Notificación al empleado', 'Laura recibe una notificación por cada resolución');
  await click(page.locator('app-notification-bell button').first(), 3800);
  await page.keyboard.press('Escape');
  await nav('Mis Solicitudes');
  await chapter('HU-05', 'Motivo del rechazo visible', 'La empleada consulta por qué se rechazó su permiso');
  await click(page.getByRole('button', { name: /Rechazadas/ }), 2200);
  await click(page.getByRole('button', { name: /Ver motivo del rechazo/ }).first(), 4200);
  await page.keyboard.press('Escape'); await sleep(800);
  await click(page.getByRole('button', { name: /Aprobadas/ }), 3000);
  await nav('Mi Ficha y Saldo');
  await cap('HU-05', 'Al aprobar, los 5 días pasan a «disfrutados» y el saldo disponible se actualiza');
  await sleep(4500);
});
await guard('hu05-rrhh-dg', async () => {
  await logout();
  // Carlos pide vacaciones: lo resuelve RRHH; RRHH pide: lo resuelve Dirección General
  await api(U.resp, 'POST', '/leave/requests', { leaveType: 'VACATION', startDate: '2027-02-01', endDate: '2027-02-05' });
  await api(U.rrhh, 'POST', '/leave/requests', { leaveType: 'VACATION', startDate: '2027-03-01', endDate: '2027-03-03' });
  await chapter('HU-05', 'Cadena de responsables', 'Un responsable no resuelve su propia solicitud: la resuelve su superior (RRHH), y la de RRHH, Dirección General');
  await login(U.rrhh);
  await nav('Solicitudes del Equipo');
  await sleep(2500);
  await click(page.getByRole('button', { name: /Aprobar solicitud de/ }).first(), 1800);
  await click(page.locator('mat-dialog-container button').last(), 3200);
  await logout();
  await login(U.dg);
  await cap('HU-05', 'Dirección General resuelve la solicitud de RRHH, su subordinado directo');
  await nav('Solicitudes del Equipo');
  await sleep(2500);
  await click(page.getByRole('button', { name: /Aprobar solicitud de/ }).first(), 1800);
  await click(page.locator('mat-dialog-container button').last(), 3200);
  await chapter('HU-05', 'Dirección General no solicita ausencias', 'Regla de negocio: sin superior que apruebe, Dirección General no puede solicitar ausencias ni tiene saldo propio');
  await page.goto(APP + '/leave/request'); await sleep(3500);
  await apiCard('Dirección General intenta solicitar', U.dg, 'POST', '/leave/requests', { leaveType: 'VACATION', startDate: '2027-08-02', endDate: '2027-08-06' }, { ms: 6000 });
  await logout();
});

// ───────────── HU-06 ─────────────
await guard('hu06', async () => {
  await huCard('HU-06', 'Calendario de ausencias del equipo', [
    'Vista mensual con las ausencias aprobadas y pendientes del equipo.',
    'Los días con dos o más personas ausentes se resaltan como coincidencias.',
    'Nunca se muestran justificaciones ni motivos de rechazo.',
  ]);
  await login(U.resp);
  await nav('Calendario del Equipo');
  await chapter('HU-06', 'Calendario del mes actual', 'Noviembre: sin ausencias en el equipo');
  await sleep(3200);
  await chapter('HU-06', 'Coincidencias en diciembre', 'Laura y Pedro coinciden del 16 al 18 de diciembre: el calendario resalta los días');
  await click(page.getByRole('button', { name: 'Mes siguiente' }), 3800);
  await cap('HU-06', 'Los permisos rechazados y las solicitudes retiradas no aparecen en el calendario');
  await sleep(3800);
  await chapter('HU-06', 'Filtro por empleado', 'El responsable filtra el calendario por una persona del equipo');
  await click(page.locator('mat-select').first(), 1000);
  await click(page.getByRole('option', { name: /Laura/ }), 3200);
  await click(page.locator('mat-select').first(), 900);
  await click(page.getByRole('option', { name: /Todo el equipo/ }), 1800);
  await chapter('HU-06', 'Detalle de una ausencia', 'Al abrir una ausencia se ve la persona, el tipo y las fechas, pero no su justificación');
  await click(page.locator('button.event').first(), 4200);
  await page.keyboard.press('Escape'); await sleep(900);
  await chapter('HU-06', 'Vista adaptada a móvil', 'Por debajo de 600 px el calendario se presenta como lista vertical de días');
  await page.setViewportSize({ width: 480, height: 720 });
  await sleep(3500);
  await page.mouse.wheel(0, 300); await sleep(2500);
  await page.setViewportSize({ width: 1280, height: 720 });
  await sleep(1500);
  await chapter('HU-06', 'Alcance por jerarquía', 'Escenario de permisos: un empleado sin personas a su cargo no ve el calendario del equipo');
  await apiCard('Laura intenta consultar el calendario del equipo', U.emp, 'GET', '/leave/team-calendar?month=2026-12', null, { ms: 6000 });
});

// ───────────── HU-07 ─────────────
await guard('hu07', async () => {
  await huCard('HU-07', 'Informes de absentismo y uso de vacaciones', [
    'Solo RRHH: agregados por departamento y por periodo, con desglose por empleado.',
    'Exportación a CSV (UTF-8) con los mismos datos que la pantalla.',
    'Si no hay datos en el periodo se informa expresamente.',
  ]);
  await logout();
  await login(U.rrhh);
  await nav('Informes');
  await chapter('HU-07', 'Informe de absentismo', 'Absentismo del año en curso por departamento');
  await click(page.getByRole('button', { name: /Generar informe/ }), 3500);
  await cap('HU-07', 'Tabla por departamento; cada fila se puede expandir para ver el desglose por empleado');
  const exp = page.locator('table button').first();
  if (await exp.count()) await click(exp, 3500); else await sleep(2500);
  await chapter('HU-07', 'Uso de vacaciones', 'Cambio de tipo de informe: días correspondientes, disfrutados y saldo remanente');
  await page.locator('#report-type').selectOption('VACATION_USAGE'); await sleep(1200);
  await click(page.getByRole('button', { name: /Generar informe/ }), 3800);
  await chapter('HU-07', 'Exportación a CSV', 'El CSV se genera con los mismos datos que se ven en pantalla');
  const dl = page.waitForEvent('download', { timeout: 15000 }).catch(() => null);
  await click(page.getByRole('button', { name: /CSV|Exportar/ }), 2500);
  const d = await dl;
  if (d) { await d.saveAs(path.join(OUT, 'informe-demo.csv')); await cap('HU-07', `Descarga completada: ${d.suggestedFilename()}`); }
  await sleep(2500);
  await chapter('HU-07', 'Periodo sin datos', 'Escenario límite: un rango sin ausencias muestra «No hay datos para el periodo seleccionado»');
  await page.locator('#report-type').selectOption('ABSENTEEISM'); await sleep(800);
  await page.locator('#report-period').selectOption('CUSTOM'); await sleep(900);
  await page.locator('#report-start-date').fill('2020-01-01'); await page.locator('#report-end-date').fill('2020-03-31'); await sleep(1000);
  await click(page.getByRole('button', { name: /Generar informe/ }), 3800);
  await chapter('HU-07', 'Acceso restringido a RRHH', 'Escenario de permisos: un empleado no ve la opción Informes y el servidor responde 403');
  await logout();
  await login(U.emp);
  await page.goto(APP + '/reports'); await sleep(2800);
  await apiCard('Un empleado solicita el informe', U.emp, 'GET', '/reports?type=ABSENTEEISM&startDate=2026-01-01&endDate=2026-12-31', null, { ms: 5800 });
  await logout();
});

// ───────────── HU-08 ─────────────
await guard('hu08', async () => {
  await huCard('HU-08', 'Evaluación del desempeño con conformidad', [
    'El responsable valora a su equipo (1 a 5) con objetivos y comentarios.',
    'El empleado da su conformidad y la evaluación queda finalizada.',
    'Una evaluación finalizada es inmutable, también en base de datos.',
  ]);
  await login(U.resp);
  await nav('Evaluaciones');
  await chapter('HU-08', 'Nueva evaluación', 'Carlos crea la evaluación anual de Laura con valoración 4 «Supera expectativas»');
  await page.locator('select[name=employee]').selectOption({ label: 'Laura Martínez Gómez' }).catch(async () => { await page.locator('select[name=employee]').selectOption({ index: 1 }); });
  await sleep(900);
  await type(page.locator('input[name=period]'), '2026-ANUAL');
  await type(page.locator('textarea[name=objectives]'), 'Entregar el módulo de informes y mejorar la cobertura de pruebas', 40);
  await page.locator('select[name=rating]').selectOption({ label: '4 — Supera expectativas' });
  await sleep(900);
  await type(page.locator('textarea[name=comments]'), 'Excelente año: cumple y supera los objetivos acordados', 40);
  await click(page.locator('.review-form-card button[type=submit]'), 3200);
  await cap('HU-08', 'La evaluación queda pendiente de la conformidad de la empleada');
  await sleep(2800);
  await chapter('HU-08', 'Valoración fuera de rango', 'Escenario de error: la valoración solo admite valores del 1 al 5');
  await apiCard('Valoración 6', U.resp, 'POST', `/performance-reviews/team/${globalThis.lauraId}`, { period: '2026-X', objectives: 'O', rating: 6, comments: 'C' }, { ms: 6500 });
  await logout();
  await login(U.emp);
  await nav('Evaluaciones');
  await chapter('HU-08', 'Conformidad del empleado', 'Laura revisa la evaluación, añade un comentario y da su conformidad');
  await sleep(3000);
  await type(page.locator('textarea[id^=employee-comments-]').first(), 'De acuerdo con la valoración, gracias', 40);
  await click(page.locator('.reviews-list button[type=button]').first(), 3500);
  await cap('HU-08', 'La evaluación pasa a Finalizada y ya no se puede modificar');
  await sleep(3200);
  const rev = await api(U.emp, 'GET', '/performance-reviews/me');
  const rid = (rev.body.content || rev.body)[0]?.id;
  if (rid) await apiCard('Segunda conformidad sobre una evaluación finalizada', U.emp, 'POST', `/performance-reviews/${rid}/acknowledge`, { employeeComments: 'otra vez' }, { ms: 6500 });
  await logout();
});

// ───────────── Epílogo HU-01: baja lógica ─────────────
await guard('baja', async () => {
  await chapter('HU-01', 'Baja lógica de un empleado', 'Epílogo: la baja desactiva la cuenta y conserva el historial');
  const m = await api(U.rrhh, 'POST', '/employees', { firstName: 'Marcos', lastName: 'Gil Ortega', nationalId: '55555555K', email: 'marcos.gil@empresa.com', managerId: globalThis.respId, hireDate: '2026-09-01', position: 'Soporte', department: 'Tecnología', salary: 24000, collectiveAgreementId: 1 });
  const mid = m.body.id || m.body.employeeId;
  await card(`<span class="tag">API REST</span><h2>Alta de un empleado de prueba</h2><div class="req">POST /api/v1/employees</div><div class="${m.status < 300 ? 'ok' : 'ko'}">HTTP ${m.status}</div><pre>${esc(JSON.stringify(m.body, null, 2).split('\n').slice(0, 12).join('\n'))}</pre>`, 5500);
  if (mid) {
    await apiCard('Baja lógica del empleado', U.rrhh, 'DELETE', `/employees/${mid}`, null, { ms: 5500 });
    await apiCard('La cuenta desactivada ya no puede acceder', 'marcos.gil@empresa.com', 'GET', '/employees/me', null, { pwd: m.body.temporaryPassword, ms: 5500 });
  }
});

await cap('', '');
await card(
  `<span class="tag">Resumen</span><h1>Ocho historias, un sistema completo</h1>
   <ul><li>224 pruebas de backend y 86 de frontend en verde</li>
   <li>Dos recorridos E2E que cruzan las ocho historias: 49 de 49 pasos conformes</li>
   <li>65 de 66 criterios verificados en la matriz de trazabilidad</li></ul>
   <p>Todo el material de la demo se reproduce con <code>demo/record-demo.mjs</code>.</p>`, 8000);

fs.writeFileSync(path.join(OUT, 'chapters.json'), JSON.stringify({ duration: (Date.now() - t0) / 1000, chapters, cards, errors: errs }, null, 2));
await ctx.close();
await browser.close();
console.log('FIN', ((Date.now() - t0) / 1000).toFixed(0) + 's', 'errores:', errs.length);
errs.forEach((e) => console.log(' -', e));

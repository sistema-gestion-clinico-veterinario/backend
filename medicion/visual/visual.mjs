import { chromium } from 'playwright';
import { execSync } from 'node:child_process';
import fs from 'node:fs';

const { BASE, SLUG, USUARIO_CUENTA, USUARIO_BASE, CLAVE, SALIDA, ESCENARIO, PUERTO_BACKEND, VERSION } = process.env;
fs.mkdirSync(SALIDA, { recursive: true });

const inicio = Date.now();
const seg = () => Number(((Date.now() - inicio) / 1000).toFixed(1));
const pausa = (ms) => new Promise((r) => setTimeout(r, ms));
const observaciones = [];
const api = [];
const errores = [];
let page;

const foto = async (nombre) => {
  try {
    await page.screenshot({ path: `${SALIDA}/${nombre}.png` });
  } catch (e) {
    errores.push(`captura ${nombre}: ${e.message}`);
  }
};

async function estado() {
  const formulario = await page.locator('#username').isVisible().catch(() => false);
  const textos = await page.locator('[role="alert"], .p-toast-message, .text-red-600, .text-rose-600').allInnerTexts().catch(() => []);
  const texto = await page.locator('body').innerText({ timeout: 2000 }).catch(() => '');
  return {
    url: page.url().replace(BASE, ''),
    formularioDeLogin: formulario,
    textoVisible: texto.replace(/\s+/g, ' ').trim().slice(0, 110),
    mensajes: textos.map((t) => t.replace(/\s+/g, ' ').trim()).filter(Boolean).slice(0, 3).map((t) => t.slice(0, 160)),
  };
}

async function observar(paso, extra = {}) {
  observaciones.push({ t: seg(), paso, ...(await estado()), ...extra });
}

const esperarSalud = async (maximo) => {
  for (let i = 0; i < maximo; i++) {
    try {
      const r = await fetch(`http://127.0.0.1:${PUERTO_BACKEND}/api/v1/actuator/health`, { signal: AbortSignal.timeout(2000) });
      if (r.ok) return seg();
    } catch { /* sigue esperando */ }
    await pausa(1000);
  }
  return null;
};

async function abrirLogin() {
  await page.goto(`${BASE}/${SLUG}/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').waitFor({ state: 'visible', timeout: 90000 });
  await page.fill('#username', USUARIO_CUENTA);
  await page.fill('#password', CLAVE);
}

const salioDelLogin = (u) => !u.pathname.endsWith('/login');

async function iniciarSesion() {
  await abrirLogin();
  await page.click('button[type="submit"]');
  await page.waitForURL(salioDelLogin, { timeout: 60000 });
  await pausa(3000);
  const aviso = page.getByRole('button', { name: /acept|entend|confirm|continuar/i }).first();
  if (/privacidad|legal|aviso/.test(page.url()) && (await aviso.count())) {
    await foto('00_aviso_previo');
    await aviso.click().catch(() => {});
    await pausa(3000);
  }
}

async function enlaceDeNavegacion() {
  for (const h of ['citas', 'mascotas', 'clientes', 'historias', 'pagos', 'empleados', 'reportes']) {
    const enlace = page.locator(`a[href*="/${h}"]`).first();
    if (await enlace.count()) return enlace;
  }
  return null;
}

async function caida() {
  await iniciarSesion();
  await foto('01_con_sesion');
  await observar('con sesión iniciada');
  execSync('docker stop sv-pg', { stdio: 'ignore' });
  const parada = seg();
  await pausa(8000);
  const enlace = await enlaceDeNavegacion();
  if (enlace) await enlace.click({ timeout: 5000 }).catch((e) => errores.push(`clic: ${e.message}`));
  await pausa(1500);
  await foto('02_clic_con_la_base_caida');
  await observar('clic con la base caída');
  await pausa(10500);
  await foto('03_12s_despues_del_clic');
  await observar('12 s después del clic');
  await pausa(18000);
  await foto('03b_30s_despues_del_clic');
  await observar('30 s después del clic');
  await page.reload({ waitUntil: 'domcontentloaded', timeout: 30000 }).catch((e) => errores.push(`recarga: ${e.message}`));
  await pausa(15000);
  await foto('04_recarga_con_la_base_caida');
  await observar('recarga con la base caída, 15 s después');
  execSync('docker start sv-pg', { stdio: 'ignore' });
  const salud = await esperarSalud(120);
  await pausa(6000);
  await foto('05_base_recuperada');
  await observar('base recuperada, sin acción del usuario');
  const enLogin = await page.locator('#username').isVisible().catch(() => false);
  if (enLogin) {
    await page.fill('#username', USUARIO_CUENTA).catch(() => {});
    await page.fill('#password', CLAVE).catch(() => {});
    await page.click('button[type="submit"]').catch(() => {});
  } else {
    const otro = await enlaceDeNavegacion();
    if (otro) await otro.click({ timeout: 5000 }).catch(() => {});
  }
  await pausa(6000);
  await foto('06_despues_de_recuperar');
  await observar(enLogin ? 'tuvo que iniciar sesión otra vez' : 'siguió con la misma sesión');
  const fallas = api.filter((r) => r.t >= parada);
  return {
    expulsadoAlLogin: observaciones.some((o) => ['clic con la base caída', '12 s después del clic', '30 s después del clic', 'recarga con la base caída, 15 s después'].includes(o.paso) && o.formularioDeLogin),
    tuvoQueIniciarSesionOtraVez: enLogin,
    saludRecuperadaTrasSegundos: salud === null ? null : Number((salud - parada).toFixed(1)),
    respuestasApiTrasLaParada: contar(fallas),
  };
}

const contar = (lista) => lista.reduce((acc, r) => ({ ...acc, [r.estado]: (acc[r.estado] ?? 0) + 1 }), {});

async function rafagaDeClavesIncorrectas(cantidad) {
  const lanzar = async (i) => {
    const t = Date.now();
    try {
      const r = await fetch(`http://127.0.0.1:${PUERTO_BACKEND}/api/v1/auth/login`, {
        method: 'POST',
        signal: AbortSignal.timeout(60000),
        headers: { 'Content-Type': 'application/json', Origin: 'http://localhost', 'X-Company-Slug': SLUG, 'X-Forwarded-For': `10.253.1.${i + 1}` },
        body: JSON.stringify({ slug: SLUG, username: `${USUARIO_BASE}${i + 2}`, password: 'incorrecta-1' }),
      });
      return { estado: r.status, segundos: (Date.now() - t) / 1000 };
    } catch {
      return { estado: 'sin respuesta', segundos: (Date.now() - t) / 1000 };
    }
  };
  return Promise.all(Array.from({ length: cantidad }, (_, i) => lanzar(i)));
}

async function loginBajoRafaga() {
  await abrirLogin();
  await foto('01_formulario_listo');
  const rafaga = rafagaDeClavesIncorrectas(40);
  const t = Date.now();
  await page.click('button[type="submit"]');
  await pausa(2500);
  await foto('02_durante_la_rafaga');
  await observar('2,5 s después de pulsar Ingresar');
  let resultado = 'sin respuesta en 45 s';
  try {
    await page.waitForURL(salioDelLogin, { timeout: 45000 });
    resultado = 'sesión iniciada';
  } catch {
    if (await page.locator('#username').isVisible().catch(() => false)) resultado = 'sigue en el login';
  }
  const segundos = Number(((Date.now() - t) / 1000).toFixed(1));
  await pausa(2500);
  await foto('03_resultado');
  await observar(`resultado: ${resultado}`);
  const respuestas = await rafaga;
  return {
    resultadoDelUsuario: resultado,
    segundosHastaElResultado: segundos,
    rafaga: {
      enviadas: respuestas.length,
      porEstado: contar(respuestas),
      segundosMaximo: Number(Math.max(...respuestas.map((r) => r.segundos)).toFixed(1)),
    },
  };
}

const mediana = (valores) => {
  const v = [...valores].sort((a, b) => a - b);
  return v.length ? v[Math.floor(v.length / 2)] : null;
};

async function usuarioDeFondo(i, control) {
  const cabeceras = { 'Content-Type': 'application/json', Origin: 'http://localhost', 'X-Company-Slug': SLUG, 'X-Forwarded-For': `10.252.0.${i + 1}` };
  const base = `http://127.0.0.1:${PUERTO_BACKEND}/api/v1`;
  try {
    await pausa(i * 600);
    let cookie = '';
    for (let intento = 0; intento < 5 && !cookie; intento++) {
      const login = await fetch(`${base}/auth/login`, { method: 'POST', headers: cabeceras, body: JSON.stringify({ slug: SLUG, username: `${USUARIO_BASE}${i + 2}`, password: CLAVE }), signal: AbortSignal.timeout(60000) });
      cookie = login.ok ? login.headers.getSetCookie().map((c) => c.split(';')[0]).join('; ') : '';
      if (!cookie) await pausa(2000);
    }
    if (!cookie) {
      control.sinSesion += 1;
      return;
    }
    while (!control.fin) {
      const t = Date.now();
      try {
        const r = await fetch(`${base}/dashboard/overview`, { headers: { ...cabeceras, Cookie: cookie }, signal: AbortSignal.timeout(60000) });
        control.respuestas.push({ estado: r.status, ms: Date.now() - t });
      } catch {
        control.respuestas.push({ estado: 'sin respuesta', ms: Date.now() - t });
      }
      await pausa(300);
    }
  } catch { /* el usuario de fondo no pudo iniciar sesión */ }
}

async function panel() {
  await iniciarSesion();
  const destino = page.url();
  const control = { fin: false, respuestas: [], sinSesion: 0 };
  const fondo = Array.from({ length: 30 }, (_, i) => usuarioDeFondo(i, control));
  await pausa(30000);
  control.respuestas.length = 0;
  const cargas = [];
  for (let i = 0; i < 5; i++) {
    api.length = 0;
    const t = Date.now();
    await page.goto(destino, { waitUntil: 'networkidle', timeout: 90000 }).catch((e) => errores.push(`carga ${i + 1}: ${e.message}`));
    const segundos = Number(((Date.now() - t) / 1000).toFixed(2));
    const delPanel = api.filter((r) => r.ruta.includes('/dashboard/')).map((r) => r.ms);
    cargas.push({ segundos, llamadasDelPanel: delPanel.length, msDelPanel: delPanel });
    if (i === 0) await foto('01_primera_carga');
    if (i === 4) await foto('02_ultima_carga');
    await observar(`carga ${i + 1}: ${segundos} s`, { llamadasDelPanel: delPanel.length });
  }
  control.fin = true;
  await Promise.allSettled(fondo);
  const duraciones = control.respuestas.filter((r) => typeof r.estado === 'number').map((r) => r.ms);
  return {
    destino: destino.replace(BASE, ''),
    usuariosDeFondo: 30,
    usuariosDeFondoSinSesion: control.sinSesion,
    peticionesDeFondoDurantePrueba: control.respuestas.length,
    medianaMsDeFondo: mediana(duraciones),
    estadosDeFondo: contar(control.respuestas),
    cargas,
    primeraCargaSegundos: cargas[0]?.segundos ?? null,
    medianaSegundosDeLasCargas2a5: mediana(cargas.slice(1).map((c) => c.segundos)),
    medianaMsDeLaLlamadaDelPanelEnLasCargas2a5: mediana(cargas.slice(1).map((c) => Math.max(0, ...c.msDelPanel))),
  };
}

const escenarios = { caida, login: loginBajoRafaga, panel };

const navegador = await chromium.launch();
const contexto = await navegador.newContext({ viewport: { width: 1366, height: 800 }, recordVideo: { dir: `${SALIDA}/video`, size: { width: 1366, height: 800 } } });
page = await contexto.newPage();
page.on('requestfinished', async (r) => {
  if (!r.url().includes('/api/v1/')) return;
  const respuesta = await r.response().catch(() => null);
  api.push({ t: seg(), estado: respuesta ? respuesta.status() : 0, metodo: r.method(), ruta: new URL(r.url()).pathname, ms: Math.max(0, Math.round(r.timing().responseEnd)) });
});
page.on('requestfailed', (r) => {
  if (r.url().includes('/api/v1/')) api.push({ t: seg(), estado: 0, metodo: r.method(), ruta: new URL(r.url()).pathname, ms: 0, falla: r.failure()?.errorText ?? null });
});

let resumen = {};
try {
  resumen = await escenarios[ESCENARIO]();
} catch (e) {
  errores.push(`escenario ${ESCENARIO}: ${e.message}`);
  await foto('99_error');
  await observar('error del guion');
}
await contexto.close();
await navegador.close();

fs.writeFileSync(`${SALIDA}/resultado.json`, JSON.stringify({ escenario: ESCENARIO, version: VERSION, resumen, observaciones, errores, api: api.slice(-400) }, null, 2));
console.log(JSON.stringify({ escenario: ESCENARIO, version: VERSION, resumen, errores }, null, 2));

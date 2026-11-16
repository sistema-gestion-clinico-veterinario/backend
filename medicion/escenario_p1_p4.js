import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = (__ENV.BASE_URL || '').replace(/\/$/, '');
const SLUG = __ENV.SLUG;
const USUARIO = __ENV.USUARIO;
const CLAVE = __ENV.CLAVE;
let EMPRESA = __ENV.COMPANY_ID;
const EMPRESA_FIJA = !!__ENV.COMPANY_ID;
const VUS = parseInt(__ENV.VUS || '2', 10);
const ITERACIONES = parseInt(__ENV.ITER || '10', 10);
const PAUSA_S = parseFloat(__ENV.PAUSA_S || '0.3');
const SALIDA = __ENV.SALIDA || 'resultado.json';
const CUENTAS = parseInt(__ENV.CUENTAS || '3', 10);
const PERFIL = __ENV.PERFIL || 'fijo';
const ETAPA = __ENV.ETAPA || '300s';
const NIVELES = (__ENV.NIVELES || '1,2,3,5').split(',').map((n) => parseInt(n, 10));
const NIVEL = parseInt(__ENV.NIVEL || '100', 10);
const RAMPA = __ENV.RAMPA || '120s';
const MANTENER = __ENV.MANTENER || '300s';
const JITTER = __ENV.JITTER === '1';

const escenarios = PERFIL === 'capacidad'
  ? {
      capacidad: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [{ duration: RAMPA, target: NIVEL }, { duration: MANTENER, target: NIVEL }, { duration: '30s', target: 0 }],
        gracefulRampDown: '60s',
        gracefulStop: '60s',
      },
    }
  : PERFIL === 'carga'
  ? {
      carga_escalonada: {
        executor: 'ramping-vus',
        startVUs: 1,
        stages: NIVELES.flatMap((n) => [{ duration: '30s', target: n }, { duration: ETAPA, target: n }]),
        gracefulRampDown: '120s',
        gracefulStop: '120s',
      },
    }
  : { trabajo_fijo: { executor: 'per-vu-iterations', vus: VUS, iterations: ITERACIONES, maxDuration: '30m' } };

export const options = {
  scenarios: escenarios,
  thresholds: {
    'group_duration{group:::P0_inicio_de_sesion}': ['max>=0'],
    'group_duration{group:::P1_acceso_y_panel}': ['max>=0'],
    'group_duration{group:::P2_agenda_mensual}': ['max>=0'],
    'group_duration{group:::P3_historia_clinica}': ['max>=0'],
    'group_duration{group:::P4_reportes}': ['max>=0'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'max'],
  cloud: { name: __ENV.NOMBRE_PRUEBA || 'Medición SoftVet' },
  noCookiesReset: true,
  insecureSkipTLSVerify: false,
};

const respuestasNo200 = new Counter('respuestas_no_200');
const estado2xx = new Counter('estado_2xx');
const estado401 = new Counter('estado_401');
const estado429 = new Counter('estado_429');
const estado4xx = new Counter('estado_otro_4xx');
const estado5xx = new Counter('estado_5xx');
const estado0 = new Counter('estado_sin_respuesta');
let conSesion = false;
let avisos = 0;

function contarEstado(res) {
  const c = res.status;
  if (c >= 200 && c < 300) estado2xx.add(1);
  else if (c === 401) { estado401.add(1); conSesion = false; }
  else if (c === 429) estado429.add(1);
  else if (c >= 400 && c < 500) estado4xx.add(1);
  else if (c >= 500) estado5xx.add(1);
  else estado0.add(1);
}

function pausa() {
  sleep(JITTER ? PAUSA_S * (0.5 + Math.random()) : PAUSA_S);
}

let enviadas = 0;
const cabeceras = () => {
  enviadas += 1;
  const cliente = `10.${__VU}.${Math.floor(enviadas / 40) % 250}.1`;
  return { headers: { 'Content-Type': 'application/json', 'X-Company-Slug': SLUG, Accept: 'application/json', Origin: 'http://localhost', 'X-Forwarded-For': `${cliente}${__ENV.XFF_RESTO !== undefined ? __ENV.XFF_RESTO : ', 10.0.0.1'}` } };
};

function galletas(url) {
  const jar = http.cookieJar().cookiesForURL(url);
  return Object.keys(jar).map((k) => `${k}(${String(jar[k][0] || '').length})`).join(',');
}

function pedir(ruta, etiqueta) {
  const res = http.get(`${BASE}/api/v1${ruta}`, { ...cabeceras(), timeout: __ENV.TIMEOUT || '60s', tags: { ruta: etiqueta } });
  const ok = check(res, { [`${etiqueta} 200`]: (r) => r.status === 200 });
  contarEstado(res);
  if (!ok) {
    respuestasNo200.add(1);
    if (avisos < 3) {
      avisos += 1;
      console.warn(`${etiqueta}: HTTP ${res.status} vu=${__VU} iter=${__ITER} ${String(res.body).slice(0, 120)}`);
    }
  }
  return res;
}

function datos(res) {
  try {
    return res.json('data');
  } catch (e) {
    return null;
  }
}

const dos = (n) => String(n).padStart(2, '0');
const fecha = (d) => `${d.getFullYear()}-${dos(d.getMonth() + 1)}-${dos(d.getDate())}`;

function iniciarSesion() {
  group('P0_inicio_de_sesion', () => {
    for (let intento = 1; intento <= 3 && !conSesion; intento++) {
      const res = http.post(`${BASE}/api/v1/auth/login`, JSON.stringify({ slug: SLUG, username: `${USUARIO}${((__VU - 1) % CUENTAS) + 1}`, password: CLAVE }), { ...cabeceras(), timeout: __ENV.TIMEOUT || '60s', tags: { ruta: 'login' } });
      const ok = check(res, { 'login 200': (r) => r.status === 200 });
      contarEstado(res);
      if (ok) {
        conSesion = true;
        if (!EMPRESA_FIJA) { try { EMPRESA = String(res.json('data.companyId')); } catch (e) { /* sin id */ } }
      } else {
        respuestasNo200.add(1);
        if (avisos < 3) {
          avisos += 1;
          console.warn(`login: HTTP ${res.status} vu=${__VU} intento=${intento} ${String(res.body).slice(0, 120)}`);
        }
        sleep(3 * intento);
      }
    }
  });
}

function p1() {
  group('P1_acceso_y_panel', () => {
    pedir('/me/navigation', 'navegacion');
    pedir('/privacidad/mi-estado', 'privacidad');
    pedir('/me/compliance', 'cumplimiento');
    pedir('/notifications/read', 'notificaciones');
    pedir('/me/compliance/request-notifications', 'avisos_de_cumplimiento');
    pedir(`/dashboard/overview?companyId=${EMPRESA}`, 'panel');
  });
}

function p2() {
  group('P2_agenda_mensual', () => {
    const hoy = new Date();
    const desde = fecha(new Date(hoy.getFullYear(), hoy.getMonth(), 1));
    const hasta = fecha(new Date(hoy.getFullYear(), hoy.getMonth() + 1, 0));
    const primera = pedir(`/appointments?page=0&size=100&companyId=${EMPRESA}&fechaDesde=${desde}&fechaHasta=${hasta}`, 'citas_del_mes');
    const cuerpo = datos(primera);
    const total = Math.min(((cuerpo && cuerpo.page && cuerpo.page.totalPages) || 1), 5);
    for (let p = 1; p < total; p++) {
      pedir(`/appointments?page=${p}&size=100&companyId=${EMPRESA}&fechaDesde=${desde}&fechaHasta=${hasta}`, 'citas_del_mes');
    }
    pedir(`/admin/employees?page=0&size=100&companyId=${EMPRESA}`, 'empleados');
    pedir(`/clients/guardians?page=0&size=100&companyId=${EMPRESA}`, 'propietarios');
  });
}

function p3() {
  group('P3_historia_clinica', () => {
    const lista = pedir(`/medical-records?page=0&size=10&companyId=${EMPRESA}`, 'lista_de_historias');
    const cuerpo = datos(lista);
    const items = (cuerpo && cuerpo.content) || [];
    if (!items.length) return;
    const elegida = items[(__VU + __ITER) % items.length];
    const detalle = datos(pedir(`/medical-records/numero/${encodeURIComponent(elegida.numeroHc)}`, 'historia'));
    const consultas = (detalle && detalle.consultas) || [];
    if (!consultas.length) return;
    const id = consultas[0].id;
    pedir(`/consultations/${id}`, 'consulta');
    pedir(`/prescriptions/consultation/${id}`, 'recetas');
    pedir(`/diagnoses/consultation/${id}`, 'diagnosticos');
  });
}

function p4() {
  group('P4_reportes', () => {
    const hoy = new Date();
    const hasta = fecha(hoy);
    const desde = fecha(new Date(hoy.getFullYear() - 1, hoy.getMonth(), hoy.getDate()));
    pedir(`/clinical-reports?fechaDesde=${desde}&fechaHasta=${hasta}&companyId=${EMPRESA}`, 'reporte_clinico');
    pedir(`/clinical-reports/sales-products?fechaDesde=${desde}&fechaHasta=${hasta}&companyId=${EMPRESA}`, 'reporte_de_ventas');
    pedir(`/clinical-reports/inactive-patients?page=0&size=20&companyId=${EMPRESA}`, 'pacientes_inactivos');
  });
}

export default function () {
  if (!conSesion) iniciarSesion();
  p1();
  pausa();
  p2();
  pausa();
  p3();
  pausa();
  p4();
  pausa();
}

function listar(valor) {
  return Array.isArray(valor) ? valor : Object.values(valor || {});
}

function chequeosFallidos(grupo, acumulado) {
  for (const c of listar(grupo.checks)) {
    if (c.fails > 0) acumulado.push(`${c.name}: ${c.fails} fallos de ${c.passes + c.fails}`);
  }
  for (const g of listar(grupo.groups)) chequeosFallidos(g, acumulado);
  return acumulado;
}

export function handleSummary(data) {
  const grupos = ['P0_inicio_de_sesion', 'P1_acceso_y_panel', 'P2_agenda_mensual', 'P3_historia_clinica', 'P4_reportes'];
  const resumen = { objetivo: BASE, vus: VUS, iteraciones_por_vu: ITERACIONES, recorridos: {} };
  for (const g of grupos) {
    const m = data.metrics[`group_duration{group:::${g}}`];
    if (m) resumen.recorridos[g] = { mediana_ms: m.values.med, p95_ms: m.values['p(95)'], promedio_ms: m.values.avg, maximo_ms: m.values.max, muestras: m.values.count };
  }
  const get = (nombre, campo) => (data.metrics[nombre] ? data.metrics[nombre].values[campo] : null);
  resumen.perfil = PERFIL;
  resumen.iteraciones_completas = get('iterations', 'count');
  resumen.vus_maximos = get('vus_max', 'max');
  resumen.estados = { '2xx': get('estado_2xx', 'count') || 0, '401': get('estado_401', 'count') || 0, '429': get('estado_429', 'count') || 0, otro_4xx: get('estado_otro_4xx', 'count') || 0, '5xx': get('estado_5xx', 'count') || 0, sin_respuesta: get('estado_sin_respuesta', 'count') || 0 };
  resumen.latencia_extra_ms = { p90: get('http_req_duration', 'p(90)'), max: get('http_req_duration', 'max') };
  resumen.peticiones = get('http_reqs', 'count');
  resumen.respuestas_no_200 = get('respuestas_no_200', 'count') || 0;
  resumen.tasa_de_fallos_http = get('http_req_failed', 'rate');
  resumen.latencia_ms = { mediana: get('http_req_duration', 'med'), p95: get('http_req_duration', 'p(95)') };
  resumen.bytes_recibidos = get('data_received', 'count');
  resumen.duracion_total_s = data.state.testRunDurationMs / 1000;
  resumen.chequeos_fallidos = chequeosFallidos(data.root_group || {}, []);
  return { [SALIDA]: JSON.stringify(resumen, null, 1), stdout: JSON.stringify(resumen, null, 1) + '\n' };
}

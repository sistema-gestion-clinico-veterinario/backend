import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = (__ENV.BASE_URL || '').replace(/\/$/, '');
const SLUG = __ENV.SLUG;
const USUARIO = __ENV.USUARIO;
const CLAVE = __ENV.CLAVE;
const EMPRESA = __ENV.COMPANY_ID;
const VUS = parseInt(__ENV.VUS || '2', 10);
const ITERACIONES = parseInt(__ENV.ITER || '10', 10);
const PAUSA_S = parseFloat(__ENV.PAUSA_S || '0.3');
const SALIDA = __ENV.SALIDA || 'resultado.json';

export const options = {
  scenarios: {
    trabajo_fijo: { executor: 'per-vu-iterations', vus: VUS, iterations: ITERACIONES, maxDuration: '30m' },
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'max'],
  insecureSkipTLSVerify: false,
};

const respuestasNo200 = new Counter('respuestas_no_200');

const cabeceras = () => ({ headers: { 'Content-Type': 'application/json', 'X-Company-Slug': SLUG, Accept: 'application/json' } });

function pedir(ruta, etiqueta) {
  const res = http.get(`${BASE}/api/v1${ruta}`, { ...cabeceras(), tags: { ruta: etiqueta } });
  const ok = check(res, { [`${etiqueta} 200`]: (r) => r.status === 200 });
  if (!ok) respuestasNo200.add(1);
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
    const res = http.post(`${BASE}/api/v1/auth/login`, JSON.stringify({ slug: SLUG, username: USUARIO, password: CLAVE }), { ...cabeceras(), tags: { ruta: 'login' } });
    const ok = check(res, { 'login 200': (r) => r.status === 200 });
    if (!ok) respuestasNo200.add(1);
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
  if (__ITER === 0) iniciarSesion();
  p1();
  sleep(PAUSA_S);
  p2();
  sleep(PAUSA_S);
  p3();
  sleep(PAUSA_S);
  p4();
  sleep(PAUSA_S);
}

export function handleSummary(data) {
  const grupos = ['P0_inicio_de_sesion', 'P1_acceso_y_panel', 'P2_agenda_mensual', 'P3_historia_clinica', 'P4_reportes'];
  const resumen = { objetivo: BASE, vus: VUS, iteraciones_por_vu: ITERACIONES, recorridos: {} };
  for (const g of grupos) {
    const m = data.metrics[`group_duration{group:::${g}}`];
    if (m) resumen.recorridos[g] = { mediana_ms: m.values.med, p95_ms: m.values['p(95)'], promedio_ms: m.values.avg, maximo_ms: m.values.max, muestras: m.values.count };
  }
  const get = (nombre, campo) => (data.metrics[nombre] ? data.metrics[nombre].values[campo] : null);
  resumen.peticiones = get('http_reqs', 'count');
  resumen.respuestas_no_200 = get('respuestas_no_200', 'count') || 0;
  resumen.tasa_de_fallos_http = get('http_req_failed', 'rate');
  resumen.latencia_ms = { mediana: get('http_req_duration', 'med'), p95: get('http_req_duration', 'p(95)') };
  resumen.bytes_recibidos = get('data_received', 'count');
  resumen.duracion_total_s = data.state.testRunDurationMs / 1000;
  return { [SALIDA]: JSON.stringify(resumen, null, 1), stdout: JSON.stringify(resumen, null, 1) + '\n' };
}

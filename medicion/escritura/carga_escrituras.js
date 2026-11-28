import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { crearGenerador } from './aleatorio.js';
import { clinicaDeCuenta } from './clinicas.js';

const BASE = (__ENV.BASE_URL || '').replace(/\/$/, '');
const SLUG = __ENV.SLUG;
const USUARIO = __ENV.USUARIO || 'medicion';
const CLAVE = __ENV.CLAVE;
const CUENTA_BASE = parseInt(__ENV.CUENTA_BASE || '0', 10);
const U_BASE = parseInt(__ENV.U_BASE || '0', 10);
const ETIQUETA = __ENV.ETIQUETA_NIVEL || 'n';
const NIVEL = parseInt(__ENV.NIVEL || '10', 10);
const RAMPA = __ENV.RAMPA || '60s';
const MANTENER = __ENV.MANTENER || '150s';
const PAUSA_S = parseFloat(__ENV.PAUSA_S || '15');
const TIMEOUT = __ENV.TIMEOUT || '30s';
const ITER_POR_VU = parseInt(__ENV.ITER_POR_VU || '19', 10);
const SALIDA = __ENV.SALIDA || 'resumen_carga.json';
const SEMILLA = __ENV.SEMILLA ? parseInt(__ENV.SEMILLA, 10) : null;
const IDS = JSON.parse(__ENV.IDS || '{}');

function clinica() {
  return clinicaDeCuenta(IDS, CUENTA_BASE + __VU);
}

function slugActual() {
  return clinica().slug || SLUG;
}

function empresa() {
  return clinica().companyId;
}

const NOMBRES_ACCIONES = ['panel', 'agenda', 'historia', 'reportes', 'recepcion', 'atencion', 'cobro', 'cancelar'];
const PESOS = (__ENV.PESOS_ACCIONES || '22,18,15,2,13,12,10,8').split(',').map((n) => parseFloat(n));

const PASOS_ESCRITURA = [
  'apoderado', 'mascota', 'cita', 'checkin', 'iniciar', 'consulta', 'diagnostico', 'receta', 'actualizar_mascota',
  'cerrar_consulta', 'finalizar_servicio', 'punto_cobro', 'vincular_equipo', 'abrir_caja', 'pago', 'cancelar_cita',
];
const ok = {};
const fallo = {};
for (const p of PASOS_ESCRITURA) {
  ok[p] = new Counter(`paso_${p}_ok`);
  fallo[p] = new Counter(`paso_${p}_fallo`);
}
const respuestasNo200 = new Counter('respuestas_no_200');
const estado2xx = new Counter('estado_2xx');
const estado401 = new Counter('estado_401');
const estado429 = new Counter('estado_429');
const estado4xx = new Counter('estado_otro_4xx');
const estado5xx = new Counter('estado_5xx');
const estado0 = new Counter('estado_sin_respuesta');
const acciones = {};
for (const a of NOMBRES_ACCIONES) acciones[a] = new Counter(`accion_${a}`);
const accionesSinTrabajo = new Counter('acciones_sin_trabajo');

export const options = {
  scenarios: {
    carga: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [{ duration: RAMPA, target: NIVEL }, { duration: MANTENER, target: NIVEL }, { duration: '30s', target: 0 }],
      gracefulRampDown: '60s',
      gracefulStop: '60s',
    },
  },
  noCookiesReset: true,
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

let conSesion = false;
let ultimoLogin = 0;
let cajaLista = false;
let usosDeHueco = 0;
let avisos = 0;
let enviadas = 0;
const pendientes = [];
const porCobrar = [];

function cabeceras() {
  enviadas += 1;
  const cliente = `10.${(__VU >> 8) & 255}.${__VU & 255}.${(Math.floor(enviadas / 40) % 250) + 1}`;
  return {
    'Content-Type': 'application/json',
    'X-Company-Slug': slugActual(),
    Accept: 'application/json',
    Origin: 'http://localhost',
    'X-Forwarded-For': `${cliente}`,
  };
}

function letras(n) {
  let t = '';
  let v = n;
  do {
    t = String.fromCharCode(97 + (v % 26)) + t;
    v = Math.floor(v / 26);
  } while (v > 0);
  return t;
}

function rellenar(n, largo) {
  return String(n).padStart(largo, '0');
}

function fechaLima(desplazamientoDias) {
  const d = new Date(Date.now() - 5 * 3600 * 1000 + desplazamientoDias * 86400 * 1000);
  return `${d.getUTCFullYear()}-${rellenar(d.getUTCMonth() + 1, 2)}-${rellenar(d.getUTCDate(), 2)}`;
}

function contarEstado(res) {
  const c = res.status;
  if (c >= 200 && c < 300) estado2xx.add(1);
  else if (c === 401) { estado401.add(1); conSesion = false; }
  else if (c === 429) estado429.add(1);
  else if (c >= 400 && c < 500) estado4xx.add(1);
  else if (c >= 500) estado5xx.add(1);
  else estado0.add(1);
}

function llamar(paso, metodo, ruta, cuerpo, esEscritura) {
  const t0 = Date.now();
  const res = http.request(metodo, `${BASE}/api/v1${ruta}`, cuerpo === undefined ? null : JSON.stringify(cuerpo), {
    headers: cabeceras(),
    timeout: TIMEOUT,
    tags: { ruta: paso },
  });
  const correcto = res.status >= 200 && res.status < 300;
  check(res, { [`${paso} 2xx`]: () => correcto });
  contarEstado(res);
  if (!correcto) respuestasNo200.add(1);
  if (esEscritura) {
    if (correcto) ok[paso].add(1);
    else fallo[paso].add(1);
  }
  if (!correcto && avisos < 5) {
    avisos += 1;
    const detalle = String(res.body).replace(/[\r\n|]+/g, ' ').slice(0, 300);
    console.log(`PASO|${__VU}|${paso}|${metodo}|${res.status}|${Date.now() - t0}|FALLO|${detalle}`);
  }
  let datos = null;
  try {
    datos = res.json('data');
  } catch (e) {
    datos = null;
  }
  return { correcto, datos, res };
}

function leer(paso, ruta) {
  return llamar(paso, 'GET', ruta, undefined, false);
}

let generadorAccion = null;
let generadorPausa = null;

function aleatorio(flujo) {
  if (SEMILLA === null) return Math.random();
  if (flujo === 'accion') {
    if (generadorAccion === null) generadorAccion = crearGenerador(SEMILLA * 7919 + __VU * 104729 + 1);
    return generadorAccion();
  }
  if (generadorPausa === null) generadorPausa = crearGenerador(SEMILLA * 7919 + __VU * 104729 + 2);
  return generadorPausa();
}

function pausa() {
  sleep(PAUSA_S * (0.5 + aleatorio('pausa')));
}

function iniciarSesion() {
  ultimoLogin = Date.now();
  const r = llamar('login', 'POST', '/auth/login', { slug: slugActual(), username: `${USUARIO}${CUENTA_BASE + __VU}`, password: CLAVE }, false);
  conSesion = r.correcto;
  return conSesion;
}

function hueco(u) {
  const c = clinica();
  const veterinarios = c.veterinarios;
  const dias = c.dias_con_turno || 120;
  const porDia = c.huecos_por_dia || 16;
  const v = veterinarios[u % veterinarios.length];
  const resto = Math.floor(u / veterinarios.length);
  const dia = 1 + (resto % dias);
  const franja = Math.floor(resto / dias) % porDia;
  const minutos = 8 * 60 + franja * 30;
  return { veterinarioId: v, fecha: fechaLima(dia), hora: `${rellenar(Math.floor(minutos / 60), 2)}:${rellenar(minutos % 60, 2)}:00` };
}

function panel() {
  leer('navegacion', '/me/navigation');
  leer('privacidad', '/privacidad/mi-estado');
  leer('cumplimiento', '/me/compliance');
  leer('notificaciones', '/notifications/read');
  leer('avisos_de_cumplimiento', '/me/compliance/request-notifications');
  leer('panel', `/dashboard/overview?companyId=${empresa()}`);
}

function agenda() {
  const hoy = new Date(Date.now() - 5 * 3600 * 1000);
  const desde = `${hoy.getUTCFullYear()}-${rellenar(hoy.getUTCMonth() + 1, 2)}-01`;
  const hasta = `${hoy.getUTCFullYear()}-${rellenar(hoy.getUTCMonth() + 1, 2)}-28`;
  leer('citas_del_mes', `/appointments?page=0&size=100&companyId=${empresa()}&fechaDesde=${desde}&fechaHasta=${hasta}`);
  leer('empleados', `/admin/employees?page=0&size=100&companyId=${empresa()}`);
  leer('propietarios', `/clients/guardians?page=0&size=100&companyId=${empresa()}`);
}

function historia() {
  const lista = leer('lista_de_historias', `/medical-records?page=0&size=10&companyId=${empresa()}`);
  const items = (lista.datos && lista.datos.content) || [];
  if (!items.length) return;
  const elegida = items[(__VU + usosDeHueco) % items.length];
  const detalle = leer('historia', `/medical-records/numero/${encodeURIComponent(elegida.numeroHc)}`);
  const consultas = (detalle.datos && detalle.datos.consultas) || [];
  if (!consultas.length) return;
  leer('consulta_lectura', `/consultations/${consultas[0].id}`);
}

function reportes() {
  const hasta = fechaLima(0);
  const desde = fechaLima(-365);
  leer('reporte_clinico', `/clinical-reports?fechaDesde=${desde}&fechaHasta=${hasta}&companyId=${empresa()}`);
  leer('reporte_de_ventas', `/clinical-reports/sales-products?fechaDesde=${desde}&fechaHasta=${hasta}&companyId=${empresa()}`);
  leer('pacientes_inactivos', `/clinical-reports/inactive-patients?page=0&size=20&companyId=${empresa()}`);
}

function recepcion() {
  if (usosDeHueco >= ITER_POR_VU) return false;
  const u = U_BASE + (__VU - 1) * ITER_POR_VU + usosDeHueco;
  usosDeHueco += 1;
  const sufijo = letras(u);
  const servicios = clinica().servicios;
  const servicio = servicios[u % servicios.length];
  const especie = u % 2 === 0 ? 'PERRO' : 'GATO';
  const h = hueco(u);

  const a = llamar('apoderado', 'POST', '/clients/guardians', {
    nombre: `Cliente${sufijo}`,
    apellido: 'Laboratorio',
    tipoDocumento: 'DNI',
    numeroDocumento: String(90000000 + u),
    email: `lab.${u}@softvet.test`,
    telefono: `9${rellenar(u, 8)}`,
    direccion: 'Avenida Los Olivos 123',
    genero: u % 2 === 0 ? 'MASCULINO' : 'FEMENINO',
    companyId: empresa(),
    roleIds: [clinica().rolCliente],
    avisoInformado: true,
    consentimientoRecordatorios: false,
    habilitarAccesoPortal: false,
  }, true);
  const apoderadoId = a.datos && (a.datos.apoderadoId || a.datos.id);
  if (!a.correcto || !apoderadoId) return true;

  const pedidoMascota = {
    nombreCompleto: `Mascota${sufijo}`,
    especie,
    razaId: clinica().razas[especie],
    sexo: 'MACHO',
    fechaNacimiento: '2022-03-15',
    peso: 12.5,
    color: 'Marron',
    esterilizado: false,
    apoderadoId,
  };
  const m = llamar('mascota', 'POST', '/pets', pedidoMascota, true);
  const mascotaId = m.datos && m.datos.id;
  if (!m.correcto || !mascotaId) return true;

  const c = llamar('cita', 'POST', '/appointments', {
    mascotaId,
    veterinarioId: h.veterinarioId,
    motivoCita: 'Control general de rutina',
    fechaHoraInicio: `${h.fecha}T${h.hora}`,
    servicioId: servicio.id,
    esEmergencia: false,
  }, true);
  const citaId = c.datos && c.datos.id;
  if (c.correcto && citaId) pendientes.push({ citaId, mascotaId, pedidoMascota });
  return true;
}

function atencion() {
  const trabajo = pendientes.shift();
  if (!trabajo) return false;
  const { citaId, mascotaId, pedidoMascota } = trabajo;
  llamar('checkin', 'PATCH', `/appointments/${citaId}/check-in`, undefined, true);
  const ini = llamar('iniciar', 'PATCH', `/appointments/${citaId}/start`, undefined, true);
  if (!ini.correcto) return true;
  const consultaId = ini.datos;
  if (consultaId) {
    const cons = llamar('consulta', 'GET', `/consultations/${consultaId}`, undefined, true);
    const version = cons.datos && cons.datos.version;
    llamar('diagnostico', 'POST', `/diagnoses/consultation/${consultaId}`, {
      nombre: 'Gastroenteritis leve',
      descripcion: 'Cuadro digestivo leve en observación',
      tipo: 'PRESUNTIVO',
      estado: 'ACTIVO',
    }, true);
    llamar('receta', 'POST', `/prescriptions/consultation/${consultaId}`, {
      medicamento: 'Amoxicilina',
      dosis: '250 mg',
      frecuencia: 'Cada 12 horas',
      duracionDias: 5,
      viaAdministracion: 'Oral',
      instrucciones: 'Administrar con alimento',
      fechaInicio: fechaLima(0),
    }, true);
    llamar('actualizar_mascota', 'PUT', `/pets/${mascotaId}`, Object.assign({}, pedidoMascota, { peso: 13.2, color: 'Marron claro' }), true);
    const cierre = llamar('cerrar_consulta', 'PATCH', `/consultations/${consultaId}/close`, {
      version,
      tipoConsulta: 'CONTROL_RUTINA',
      pesoEnConsulta: 12.5,
      temperatura: 38.4,
      frecuenciaCardiaca: 90,
      frecuenciaRespiratoria: 24,
      mucosas: 'Rosadas',
      turgenciaPiel: 'Normal',
      vacunacionAplicada: false,
      desparasitacionAplicada: false,
      motivoConsulta: 'Control general de rutina',
      anamnesis: 'Propietario refiere buen apetito y actividad normal',
      examenFisico: 'Sin hallazgos relevantes',
      observaciones: 'Control en 15 dias',
    }, true);
    if (cierre.correcto) porCobrar.push(citaId);
  } else {
    const fin = llamar('finalizar_servicio', 'PATCH', `/appointments/${citaId}/finish-service`, { notas: 'Servicio realizado' }, true);
    if (fin.correcto) porCobrar.push(citaId);
  }
  return true;
}

function prepararCaja() {
  const p = llamar('punto_cobro', 'POST', '/caja/puntos', { companyId: empresa(), nombre: `Caja ${ETIQUETA}-${__VU}`, activa: true }, true);
  const id = p.datos && p.datos.id;
  if (!p.correcto || !id) return false;
  const v = llamar('vincular_equipo', 'POST', `/caja/puntos/${id}/vincular?companyId=${empresa()}`, { nombreEquipo: `Equipo ${ETIQUETA}-${__VU}` }, true);
  if (!v.correcto) return false;
  return llamar('abrir_caja', 'POST', '/caja/sesion/abrir', { companyId: empresa(), montoApertura: 100 }, true).correcto;
}

function cobro() {
  const citaId = porCobrar.shift();
  if (!citaId) return false;
  if (!cajaLista) cajaLista = prepararCaja();
  if (!cajaLista) return true;
  llamar('pago', 'POST', '/payments', { citaId, metodoPago: 'EFECTIVO', montoRecibido: 1000 }, true);
  return true;
}

function cancelar() {
  const trabajo = pendientes.pop();
  if (!trabajo) return false;
  llamar('cancelar_cita', 'PATCH', `/appointments/${trabajo.citaId}/cancel?motivo=${encodeURIComponent('Cambio de planes del propietario')}`, undefined, true);
  return true;
}

function elegirAccion() {
  const total = PESOS.reduce((x, y) => x + y, 0);
  let r = aleatorio('accion') * total;
  for (let i = 0; i < PESOS.length; i++) {
    r -= PESOS[i];
    if (r < 0) return NOMBRES_ACCIONES[i];
  }
  return NOMBRES_ACCIONES[NOMBRES_ACCIONES.length - 1];
}

function ejecutar(nombre) {
  switch (nombre) {
    case 'panel': panel(); return true;
    case 'agenda': agenda(); return true;
    case 'historia': historia(); return true;
    case 'reportes': reportes(); return true;
    case 'recepcion': return recepcion();
    case 'atencion': return atencion();
    case 'cobro': return cobro();
    case 'cancelar': return cancelar();
    default: return false;
  }
}

export default function () {
  if (!conSesion) {
    if (ultimoLogin !== 0 && Date.now() - ultimoLogin < 20000) {
      pausa();
      return;
    }
    if (!iniciarSesion()) {
      pausa();
      return;
    }
    panel();
    pausa();
    return;
  }

  let accion = elegirAccion();
  let hecho = ejecutar(accion);
  if (!hecho) {
    accionesSinTrabajo.add(1);
    accion = porCobrar.length ? 'cobro' : (pendientes.length ? 'atencion' : 'recepcion');
    hecho = ejecutar(accion);
    if (!hecho) {
      accion = 'panel';
      panel();
    }
  }
  acciones[accion].add(1);
  pausa();
}

export function handleSummary(data) {
  const valor = (nombre, campo) => (data.metrics[nombre] ? data.metrics[nombre].values[campo] : 0);
  const pasos = {};
  for (const p of PASOS_ESCRITURA) {
    pasos[p] = { correctos: valor(`paso_${p}_ok`, 'count') || 0, fallidos: valor(`paso_${p}_fallo`, 'count') || 0 };
  }
  const resumen = {
    perfil: 'carga_escritura',
    nivel: NIVEL,
    iteraciones_completas: valor('iterations', 'count'),
    peticiones: valor('http_reqs', 'count'),
    duracion_total_s: data.state.testRunDurationMs / 1000,
    latencia_ms: { mediana: valor('http_req_duration', 'med'), p95: valor('http_req_duration', 'p(95)'), p99: valor('http_req_duration', 'p(99)'), max: valor('http_req_duration', 'max') },
    estados: {
      '2xx': valor('estado_2xx', 'count') || 0,
      '401': valor('estado_401', 'count') || 0,
      '429': valor('estado_429', 'count') || 0,
      otro_4xx: valor('estado_otro_4xx', 'count') || 0,
      '5xx': valor('estado_5xx', 'count') || 0,
      sin_respuesta: valor('estado_sin_respuesta', 'count') || 0,
    },
    pasos,
    acciones: Object.fromEntries(NOMBRES_ACCIONES.map((a) => [a, valor(`accion_${a}`, 'count') || 0])),
    acciones_sin_trabajo: valor('acciones_sin_trabajo', 'count') || 0,
    vus_maximos: valor('vus_max', 'max'),
    vus_activos_max: valor('vus', 'max'),
    iteraciones_descartadas: data.metrics.dropped_iterations ? data.metrics.dropped_iterations.values.count : null,
  };
  return { [SALIDA]: JSON.stringify(resumen, null, 1), stdout: JSON.stringify(resumen, null, 1) + '\n' };
}

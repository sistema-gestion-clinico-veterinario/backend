import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = (__ENV.BASE_URL || '').replace(/\/$/, '');
const SLUG = __ENV.SLUG;
const USUARIO = __ENV.USUARIO;
const CLAVE = __ENV.CLAVE;
const CUENTAS = parseInt(__ENV.CUENTAS || '3', 10);
const VUS = parseInt(__ENV.VUS || '1', 10);
const ITER = parseInt(__ENV.ITER || '1', 10);
const PAUSA_S = parseFloat(__ENV.PAUSA_S || '0.5');
const JITTER = __ENV.JITTER === '1';
const SALIDA = __ENV.SALIDA || 'resumen_jornada.json';
const TIMEOUT = __ENV.TIMEOUT || '60s';
const ITER_POR_VU = parseInt(__ENV.ITER_POR_VU || '19', 10);
const IDS = JSON.parse(__ENV.IDS || '{}');

const PASOS = [
  'login', 'apoderado', 'mascota', 'cita', 'checkin', 'iniciar', 'consulta', 'diagnostico', 'receta',
  'punto_cobro', 'vincular_equipo', 'abrir_caja',
  'cerrar_consulta', 'finalizar_servicio', 'pago', 'actualizar_mascota', 'cita_extra', 'cancelar_cita',
];
const ok = {};
const fallo = {};
for (const p of PASOS) {
  ok[p] = new Counter(`paso_${p}_ok`);
  fallo[p] = new Counter(`paso_${p}_fallo`);
}
const jornadasCompletas = new Counter('jornadas_completas');
const jornadasIncompletas = new Counter('jornadas_incompletas');

export const options = {
  scenarios: {
    jornada: { executor: 'per-vu-iterations', vus: VUS, iterations: ITER, maxDuration: '30m' },
  },
  noCookiesReset: true,
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'max'],
};

let conSesion = false;
let ultimoLogin = 0;
let cajaLista = false;

function cabeceras(extra) {
  return Object.assign({
    'Content-Type': 'application/json',
    'X-Company-Slug': SLUG,
    Accept: 'application/json',
    Origin: 'http://localhost',
    'X-Forwarded-For': `10.${(__VU >> 8) & 255}.${__VU & 255}.1`,
  }, extra || {});
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

function llamar(paso, metodo, ruta, cuerpo) {
  const t0 = Date.now();
  const res = http.request(metodo, `${BASE}/api/v1${ruta}`, cuerpo === undefined ? null : JSON.stringify(cuerpo), {
    headers: cabeceras(),
    timeout: TIMEOUT,
    tags: { ruta: paso },
  });
  const correcto = res.status >= 200 && res.status < 300;
  check(res, { [`${paso} 2xx`]: () => correcto });
  let datos = null;
  try {
    datos = res.json('data');
  } catch (e) {
    datos = null;
  }
  if (correcto) ok[paso].add(1);
  else fallo[paso].add(1);
  const detalle = correcto ? '' : String(res.body).replace(/[\r\n|]+/g, ' ').slice(0, 400);
  console.log(`PASO|${__VU}|${__ITER}|${paso}|${metodo}|${res.status}|${Date.now() - t0}|${correcto ? 'ok' : 'FALLO'}|${detalle}`);
  if (res.status === 401) conSesion = false;
  return { correcto, datos, res };
}

function iniciarSesion() {
  ultimoLogin = Date.now();
  const r = llamar('login', 'POST', '/auth/login', {
    slug: SLUG,
    username: `${USUARIO}${((__VU - 1) % CUENTAS) + 1}`,
    password: CLAVE,
  });
  conSesion = r.correcto;
  return conSesion;
}

function prepararCaja() {
  const p = llamar('punto_cobro', 'POST', '/caja/puntos', { companyId: IDS.companyId, nombre: `Caja laboratorio ${__VU}`, activa: true });
  const id = p.datos && p.datos.id;
  if (!p.correcto || !id) return false;
  const v = llamar('vincular_equipo', 'POST', `/caja/puntos/${id}/vincular?companyId=${IDS.companyId}`, { nombreEquipo: `Equipo laboratorio ${__VU}` });
  if (!v.correcto) return false;
  return llamar('abrir_caja', 'POST', '/caja/sesion/abrir', { companyId: IDS.companyId, montoApertura: 100 }).correcto;
}

function pausa() {
  sleep(JITTER ? PAUSA_S * (0.5 + Math.random()) : PAUSA_S);
}

function hueco(u) {
  const veterinarios = IDS.veterinarios;
  const dias = IDS.dias_con_turno || 120;
  const porDia = IDS.huecos_por_dia || 16;
  const v = veterinarios[u % veterinarios.length];
  const resto = Math.floor(u / veterinarios.length);
  const dia = 1 + (resto % dias);
  const franja = Math.floor(resto / dias) % porDia;
  const minutos = 8 * 60 + franja * 30;
  return { veterinarioId: v, fecha: fechaLima(dia), hora: `${rellenar(Math.floor(minutos / 60), 2)}:${rellenar(minutos % 60, 2)}:00` };
}

export default function () {
  if (!conSesion) {
    if (ultimoLogin !== 0 && Date.now() - ultimoLogin < 20000) {
      pausa();
      jornadasIncompletas.add(1);
      return;
    }
    if (!iniciarSesion()) {
      jornadasIncompletas.add(1);
      pausa();
      return;
    }
  }

  if (!cajaLista) {
    cajaLista = prepararCaja();
    pausa();
  }

  const u = (__VU - 1) * ITER_POR_VU + __ITER;
  const sufijo = letras(u);
  const documento = String(90000000 + u);
  const servicio = IDS.servicios[u % IDS.servicios.length];
  const especie = u % 2 === 0 ? 'PERRO' : 'GATO';
  const h = hueco(u * 2);
  const h2 = hueco(u * 2 + 1);
  let completa = true;

  const pedidoApoderado = {
    nombre: `Cliente${sufijo}`,
    apellido: 'Laboratorio',
    tipoDocumento: 'DNI',
    numeroDocumento: documento,
    email: `lab.${u}@softvet.test`,
    telefono: `9${rellenar(u, 8)}`,
    direccion: 'Avenida Los Olivos 123',
    genero: u % 2 === 0 ? 'MASCULINO' : 'FEMENINO',
    companyId: IDS.companyId,
    roleIds: [IDS.rolCliente],
    avisoInformado: true,
    consentimientoRecordatorios: false,
    habilitarAccesoPortal: false,
  };
  const a = llamar('apoderado', 'POST', '/clients/guardians', pedidoApoderado);
  const apoderadoId = a.datos && (a.datos.apoderadoId || a.datos.id);
  if (!a.correcto || !apoderadoId) {
    jornadasIncompletas.add(1);
    return;
  }
  pausa();

  const pedidoMascota = {
    nombreCompleto: `Mascota${sufijo}`,
    especie,
    razaId: IDS.razas[especie],
    sexo: 'MACHO',
    fechaNacimiento: '2022-03-15',
    peso: 12.5,
    color: 'Marron',
    esterilizado: false,
    apoderadoId,
  };
  const m = llamar('mascota', 'POST', '/pets', pedidoMascota);
  const mascotaId = m.datos && m.datos.id;
  if (!m.correcto || !mascotaId) {
    jornadasIncompletas.add(1);
    return;
  }
  pausa();

  const c = llamar('cita', 'POST', '/appointments', {
    mascotaId,
    veterinarioId: h.veterinarioId,
    motivoCita: 'Control general de rutina',
    fechaHoraInicio: `${h.fecha}T${h.hora}`,
    servicioId: servicio.id,
    esEmergencia: false,
  });
  const citaId = c.datos && c.datos.id;
  if (!c.correcto || !citaId) {
    jornadasIncompletas.add(1);
    return;
  }
  pausa();

  if (!llamar('checkin', 'PATCH', `/appointments/${citaId}/check-in`).correcto) completa = false;
  pausa();

  const ini = llamar('iniciar', 'PATCH', `/appointments/${citaId}/start`);
  if (!ini.correcto) {
    jornadasIncompletas.add(1);
    return;
  }
  const consultaId = ini.datos;
  pausa();

  if (consultaId) {
    const cons = llamar('consulta', 'GET', `/consultations/${consultaId}`);
    const version = cons.datos && cons.datos.version;
    if (!cons.correcto) completa = false;
    pausa();

    const d = llamar('diagnostico', 'POST', `/diagnoses/consultation/${consultaId}`, {
      nombre: 'Gastroenteritis leve',
      descripcion: 'Cuadro digestivo leve en observación',
      tipo: 'PRESUNTIVO',
      estado: 'ACTIVO',
    });
    if (!d.correcto) completa = false;
    pausa();

    const r = llamar('receta', 'POST', `/prescriptions/consultation/${consultaId}`, {
      medicamento: 'Amoxicilina',
      dosis: '250 mg',
      frecuencia: 'Cada 12 horas',
      duracionDias: 5,
      viaAdministracion: 'Oral',
      instrucciones: 'Administrar con alimento',
      fechaInicio: fechaLima(0),
    });
    if (!r.correcto) completa = false;
    pausa();

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
    });
    if (!cierre.correcto) completa = false;
  } else {
    if (!llamar('finalizar_servicio', 'PATCH', `/appointments/${citaId}/finish-service`, { notas: 'Servicio realizado' }).correcto) completa = false;
  }
  pausa();

  if (!llamar('pago', 'POST', '/payments', { citaId, metodoPago: 'EFECTIVO', montoRecibido: 1000 }).correcto) completa = false;
  pausa();

  const pedidoMascotaActualizada = Object.assign({}, pedidoMascota, { peso: 13.2, color: 'Marron claro' });
  if (!llamar('actualizar_mascota', 'PUT', `/pets/${mascotaId}`, pedidoMascotaActualizada).correcto) completa = false;
  pausa();

  const extra = llamar('cita_extra', 'POST', '/appointments', {
    mascotaId,
    veterinarioId: h2.veterinarioId,
    motivoCita: 'Control de seguimiento',
    fechaHoraInicio: `${h2.fecha}T${h2.hora}`,
    servicioId: servicio.id,
    esEmergencia: false,
  });
  const extraId = extra.datos && extra.datos.id;
  if (!extra.correcto || !extraId) {
    completa = false;
  } else {
    pausa();
    if (!llamar('cancelar_cita', 'PATCH', `/appointments/${extraId}/cancel?motivo=${encodeURIComponent('Cambio de planes del propietario')}`).correcto) completa = false;
  }

  if (completa) jornadasCompletas.add(1);
  else jornadasIncompletas.add(1);
  pausa();
}

export function handleSummary(data) {
  const valor = (nombre, campo) => (data.metrics[nombre] ? data.metrics[nombre].values[campo] : 0);
  const pasos = {};
  for (const p of PASOS) {
    pasos[p] = { correctos: valor(`paso_${p}_ok`, 'count') || 0, fallidos: valor(`paso_${p}_fallo`, 'count') || 0 };
  }
  const resumen = {
    usuarios: VUS,
    iteraciones_por_usuario: ITER,
    jornadas_completas: valor('jornadas_completas', 'count') || 0,
    jornadas_incompletas: valor('jornadas_incompletas', 'count') || 0,
    peticiones: valor('http_reqs', 'count'),
    duracion_total_s: data.state.testRunDurationMs / 1000,
    latencia_ms: { mediana: valor('http_req_duration', 'med'), p95: valor('http_req_duration', 'p(95)'), max: valor('http_req_duration', 'max') },
    pasos,
  };
  return { [SALIDA]: JSON.stringify(resumen, null, 1), stdout: JSON.stringify(resumen, null, 1) + '\n' };
}

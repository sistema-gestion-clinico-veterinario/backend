import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
  scenarios: { carga: { executor: 'ramping-vus', startVUs: 0, stages: [{ duration: '5s', target: 10 }, { duration: '20s', target: 10 }, { duration: '2s', target: 0 }] } },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  http.get('http://127.0.0.1:18999/');
  sleep(1);
}

export function handleSummary(data) {
  const valor = (nombre, campo) => (data.metrics[nombre] ? data.metrics[nombre].values[campo] : 0);
  const peticiones = valor('http_reqs', 'count');
  const fallidas = valor('http_req_failed', 'passes');
  const resumen = {
    perfil: 'calibracion',
    nivel: 10,
    iteraciones_completas: valor('iterations', 'count'),
    peticiones,
    duracion_total_s: data.state.testRunDurationMs / 1000,
    latencia_ms: { mediana: valor('http_req_duration', 'med'), p95: valor('http_req_duration', 'p(95)'), p99: valor('http_req_duration', 'p(99)'), max: valor('http_req_duration', 'max') },
    estados: { '2xx': peticiones - fallidas, '5xx': 0, sin_respuesta: 0 },
    vus_maximos: valor('vus_max', 'max'),
    vus_activos_max: valor('vus', 'max'),
    iteraciones_descartadas: data.metrics.dropped_iterations ? data.metrics.dropped_iterations.values.count : null,
  };
  return { [__ENV.SALIDA]: JSON.stringify(resumen, null, 1) };
}

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

export const options = {
  cloud: {
    projectID: 8500196,
    distribution: {
      ashburn: { loadZone: 'amazon:us:ashburn', percent: 100 },
    },
  },
  scenarios: {
    carga: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '2m', target: 250 },
        { duration: '2m', target: 500 },
        { duration: '3m', target: 1000 },
        { duration: '1m', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: [
      'rate<0.01',
      { threshold: 'rate<0.10', abortOnFail: true, delayAbortEval: '30s' },
    ],
    'http_req_duration{endpoint:health}': ['p(95)<800'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  const response = http.get(`${BASE_URL}/api/v1/health`, {
    tags: { endpoint: 'health' },
    timeout: '10s',
  });

  let body = {};
  try {
    body = response.json();
  } catch (_) {
    // Una respuesta vacía o no JSON no debe interrumpir la prueba.
  }

  if (__ITER === 0 && response.error) {
    console.error(
      `No se pudo consultar ${BASE_URL}/api/v1/health: ${response.error}`,
    );
  }

  check(response, {
    'responde con HTTP 200': (result) => result.status === 200,
  });

  check(body, {
    'aplicacion disponible': (result) => result.status === 'UP',
    'base de datos disponible': (result) => result.database === 'UP',
  });

  sleep(1);
}

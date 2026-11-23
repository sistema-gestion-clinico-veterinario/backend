import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';

const [distArg, puerto, puertoBackend] = process.argv.slice(2);
const dist = path.resolve(distArg);
const TIPOS = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript', '.mjs': 'text/javascript', '.css': 'text/css',
  '.json': 'application/json', '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg', '.ico': 'image/x-icon',
  '.woff': 'font/woff', '.woff2': 'font/woff2', '.ttf': 'font/ttf', '.webmanifest': 'application/manifest+json',
};

function enviarArchivo(res, archivo) {
  res.writeHead(200, { 'Content-Type': TIPOS[path.extname(archivo)] ?? 'application/octet-stream', 'Cache-Control': 'no-store' });
  fs.createReadStream(archivo).pipe(res);
}

const agente = new http.Agent({ keepAlive: false });

function reenviar(req, res) {
  const cabeceras = { ...req.headers, host: `127.0.0.1:${puertoBackend}`, 'x-forwarded-for': '10.254.0.1' };
  if (cabeceras.origin) cabeceras.origin = 'http://localhost';
  const salida = http.request({ host: '127.0.0.1', port: Number(puertoBackend), path: req.url, method: req.method, headers: cabeceras, agent: agente }, (respuesta) => {
    res.writeHead(respuesta.statusCode ?? 502, respuesta.headers);
    respuesta.pipe(res);
    respuesta.on('error', (e) => {
      console.error(`respuesta del backend interrumpida ${req.method} ${req.url}: ${e.code ?? e.message}`);
      res.destroy();
    });
  });
  salida.on('error', (e) => {
    console.error(`error al reenviar ${req.method} ${req.url}: ${e.code ?? e.message}`);
    if (res.headersSent) {
      res.destroy();
      return;
    }
    res.writeHead(502);
    res.end();
  });
  req.pipe(salida);
}

http.createServer((req, res) => {
  const ruta = decodeURIComponent((req.url ?? '/').split('?')[0]);
  if (ruta.startsWith('/api/v1')) return reenviar(req, res);
  const archivo = path.resolve(path.join(dist, path.normalize(ruta)));
  if (archivo.startsWith(dist) && fs.existsSync(archivo) && fs.statSync(archivo).isFile()) return enviarArchivo(res, archivo);
  return enviarArchivo(res, path.join(dist, 'index.html'));
}).listen(Number(puerto), '127.0.0.1', () => console.log(`servidor estático en ${puerto}`));

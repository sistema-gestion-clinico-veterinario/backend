import http from "node:http";

const port = Number(process.env.VARGASVET_MOCK_PORT ?? 8099);
const storedFiles = new Map();

function send(response, status, contentType, body) {
  const payload = Buffer.isBuffer(body) ? body : Buffer.from(String(body));
  response.writeHead(status, {
    "content-type": contentType,
    "content-length": payload.length,
    connection: "keep-alive",
  });
  response.end(payload);
}

function json(response, body, status = 200) {
  send(response, status, "application/json", JSON.stringify(body));
}

const server = http.createServer((request, response) => {
  const chunks = [];
  request.on("data", (chunk) => chunks.push(chunk));
  request.on("end", () => {
    const body = Buffer.concat(chunks);
    const pathname = new URL(request.url, `http://127.0.0.1:${port}`).pathname;

    if (pathname === "/health") return json(response, { status: "UP" });
    if (pathname === "/ia/laboratorio" && request.method === "POST") {
      return json(response, {
        fuente: "mock-load", tipo: "laboratorio", especie: "Perro", raza: "N/D",
        edad: "N/D", fecha: "2026-07-21", secciones_presentes: [],
        comentarios_clinicos: ["Respuesta simulada para carga"], alertas: [],
      });
    }
    if (pathname === "/predict/radiografia" && request.method === "POST") {
      return json(response, {
        model: "mock-load", file_type: "image/png", predictions: {},
        diagnoses: ["Respuesta simulada para carga"], inference_ms: 1.0,
      });
    }
    if (pathname.startsWith("/storage/v1/object/public/") && request.method === "GET") {
      const key = pathname.replace("/storage/v1/object/public/", "");
      return send(response, 200, "image/png", storedFiles.get(key) ?? Buffer.from("vargasvet-load-mock"));
    }
    if (pathname.startsWith("/storage/v1/object/") && request.method === "POST") {
      const key = pathname.replace("/storage/v1/object/", "");
      storedFiles.set(key, body);
      return json(response, { key });
    }
    if (pathname.startsWith("/storage/v1/object/") && request.method === "DELETE") {
      return json(response, { removed: true });
    }
    return json(response, { error: "Ruta simulada no encontrada", path: pathname }, 404);
  });
});

server.listen(port, "127.0.0.1", () => {
  console.log(`Mock de Supabase/IA listo en http://127.0.0.1:${port}`);
});

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => server.close(() => process.exit(0)));
}

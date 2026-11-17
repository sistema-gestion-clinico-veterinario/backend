# Control de la fase 2b: 10 usuarios con Metaspace de 128 MB (como el Dockerfile) y correo de pruebas

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

## CONTROL (DESPUÉS, Metaspace 128 MB): AGUANTA; datos CONSISTENTES

- Escrituras correctas: 167; fallidas: 2.
- Acciones hechas: panel 31, agenda 21, historia 18, reportes 2, recepcion 25, atencion 11, cobro 3, cancelar 5; sin trabajo disponible: 17.
- CPU del backend: 20 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 2 % del tiempo.
- CPU de PostgreSQL: 4 % de media y 34 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 651 MB (32 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **10 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.7 s.
- Correo: el servidor de pruebas recibió 25 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 126 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 126 a 125 MB (-0.1 %).
- GC: 187 pausas, 7.9 s en total (3 % del tiempo de vida de la JVM); 18 completas, 7 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 230 MB (tope -Xmx1g).
- Clases cargadas: 24806 en total, 458 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 12 | 13 | 3665 |
| 0.1 | 15 | 15 | 4052 |
| 0.1 | 22 | 22 | 5456 |
| 0.1 | 32 | 32 | 7225 |
| 0.2 | 50 | 50 | 10965 |
| 0.3 | 65 | 66 | 13627 |
| 0.3 | 80 | 81 | 16043 |
| 0.4 | 80 | 81 | 16074 |
| 0.4 | 80 | 81 | 16094 |
| 0.4 | 82 | 83 | 16394 |
| 0.5 | 86 | 87 | 17103 |
| 0.6 | 87 | 88 | 17206 |
| 0.7 | 100 | 102 | 19404 |
| 2.0 | 126 | 128 | 24177 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 343 |
| `veterinaria.vargasvet.service.reportes.ReporteVentasProductos$$Lambda/` | 17 |
| `java.lang.invoke.LambdaForm$DMH/` | 13 |
| `java.lang.invoke.LambdaForm$BMH/` | 9 |
| `veterinaria.vargasvet.service.reportes.ReportePreventivos$$Lambda/` | 7 |
| `veterinaria.vargasvet.service.PuntoCobroService$$Lambda/` | 7 |
| `veterinaria.vargasvet.service.reportes.ReportePacientesInactivos$$Lambda/` | 6 |
| `veterinaria.vargasvet.service.impl.CajaServiceImpl$$Lambda/` | 5 |
| `veterinaria.vargasvet.service.reportes.ReporteIngresos$$Lambda/` | 4 |
| `veterinaria.vargasvet.service.reportes.ReporteVentasProductos$PeriodoVentas$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 365 |
| `veterinaria.vargasvet.service.reportes` | 40 |
| `com.fasterxml.jackson.databind` | 11 |
| `veterinaria.vargasvet.service.impl` | 9 |
| `veterinaria.vargasvet.service.PuntoCobroService` | 7 |
| `veterinaria.vargasvet.dto.response` | 6 |
| `java.util.stream.SliceOps` | 3 |
| `jdk.proxy2.` | 3 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 25 | 25 | coincide |
| Mascotas | 25 | 25 | coincide |
| Citas agendadas | 23 | 23 | coincide |
| Diagnósticos | 11 | 11 | coincide |
| Recetas | 11 | 11 | coincide |
| Pagos | 3 | 3 | coincide |
| Citas completadas | 11 | 11 | coincide |
| Citas canceladas | 5 | 5 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 25 | 0 | 0.0 % | 65 | 242 |
| Registrar mascota | 25 | 0 | 0.0 % | 46 | 187 |
| Agendar cita | 23 | 2 | 8.0 % | 54 | 223 |
| Recepción (llegada) | 11 | 0 | 0.0 % | 27 | 53 |
| Iniciar atención | 11 | 0 | 0.0 % | 43 | 57 |
| Leer la consulta | 11 | 0 | 0.0 % | 24 | 34 |
| Registrar diagnóstico | 11 | 0 | 0.0 % | 26 | 148 |
| Registrar receta | 11 | 0 | 0.0 % | 31 | 64 |
| Actualizar mascota | 11 | 0 | 0.0 % | 30 | 39 |
| Cerrar la consulta | 11 | 0 | 0.0 % | 51 | 204 |
| Caja: crear punto de cobro | 3 | 0 | 0.0 % | 23 | 44 |
| Caja: vincular equipo | 3 | 0 | 0.0 % | 22 | 54 |
| Caja: abrir la caja | 3 | 0 | 0.0 % | 25 | 71 |
| Cobrar | 3 | 0 | 0.0 % | 37 | 585 |
| Cancelar cita | 5 | 0 | 0.0 % | 42 | 54 |

**Endpoints que más tiempo acumulan, CONTROL (DESPUÉS, Metaspace 128 MB):**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 41 | 0 (0 %) | 253 | 431 | 23 % |
| reporte_clinico | 2 | 0 (0 %) | 3 343 | 4 904 | 15 % |
| login | 10 | 0 (0 %) | 425 | 1 401 | 12 % |
| apoderado | 25 | 0 (0 %) | 65 | 242 | 5 % |
| cita | 25 | 2 (8 %) | 54 | 223 | 4 % |
| propietarios | 21 | 0 (0 %) | 71 | 136 | 4 % |
| mascota | 25 | 0 (0 %) | 46 | 187 | 4 % |
| citas_del_mes | 21 | 0 (0 %) | 51 | 93 | 3 % |
| navegacion | 41 | 0 (0 %) | 24 | 63 | 3 % |
| cumplimiento | 41 | 0 (0 %) | 23 | 72 | 3 % |
| avisos_de_cumplimiento | 41 | 0 (0 %) | 22 | 55 | 3 % |
| lista_de_historias | 18 | 0 (0 %) | 48 | 298 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 5 | 35 | 665 | 0 % |
| 0.3 | 10 | 56 | 412 | 0 % |
| 0.5 | 10 | 36 | 335 | 0 % |
| 0.8 | 10 | 48 | 129 | 0 % |
| 1.0 | 10 | 38 | 204 | 0 % |
| 1.3 | 10 | 37 | 590 | 0 % |
| 1.5 | 10 | 43 | 117 | 0 % |
| 1.8 | 10 | 52 | 289 | 0 % |
| 2.0 | 10 | 41 | 66 | 2 % |
| 2.3 | 10 | 34 | 250 | 0 % |
| 2.5 | 10 | 39 | 371 | 0 % |
| 2.8 | 10 | 35 | 350 | 0 % |
| 3.0 | 10 | 42 | 89 | 2 % |
| 3.3 | 7 | 12 | 253 | 0 % |

</details>

> Misma versión DESPUÉS y mismo escenario que el nivel de 10 usuarios de la escalera, pero con el tope de Metaspace del Dockerfile actual. Si aquí aparece `OutOfMemoryError: Metaspace` y en la escalera (256 MB) no, el tope de 128 MB es insuficiente.

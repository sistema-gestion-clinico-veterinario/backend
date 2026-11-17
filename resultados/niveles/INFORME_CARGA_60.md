# Fase 2b: carga con escrituras, 60 usuarios simultáneos (Metaspace 256 MB, correo de pruebas)

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | AGUANTA | AGUANTA |
| Usuarios sin degradación | 60 | 60 |
| Peticiones hechas | 3 496 | 3 408 |
| Peticiones correctas (2xx) | 3 485 | 3 400 |
| Errores (%) | 0.3 % | 0.2 % |
| Ritmo medio (peticiones/s) | 13.2 | 12.7 |
| Tiempo mediano (ms) | 24 | 26 |
| Tiempo p95 (ms) | 358 | 378 |
| CPU del backend, media (%) | 44 | 45 |
| Tiempo con la CPU al tope (%) | 15 | 12 |
| CPU de PostgreSQL, media (%) | 26 | 30 |
| Memoria pico (MB) | 890 | 1 007 |
| Sentencias SQL | 210 438 | 93 820 |
| Reinicios del contenedor | 0 | 0 |

## ANTES: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1238; fallidas: 11.
- Acciones hechas: panel 174, agenda 139, historia 110, reportes 12, recepcion 181, atencion 77, cobro 43, cancelar 24; sin trabajo disponible: 118.
- CPU del backend: 44 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 15 % del tiempo.
- CPU de PostgreSQL: 26 % de media y 95 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 890 MB (43 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **60 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.6 s.
- Correo: el servidor de pruebas recibió 181 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 134 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 134 MB (+1.6 %).
- GC: 239 pausas, 13.3 s en total (5 % del tiempo de vida de la JVM); 21 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 404 MB (tope -Xmx1g).
- Clases cargadas: 25507 en total, 454 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 13 | 3774 |
| 0.1 | 19 | 19 | 4719 |
| 0.1 | 30 | 30 | 6946 |
| 0.2 | 53 | 53 | 11309 |
| 0.3 | 71 | 72 | 14618 |
| 0.3 | 80 | 81 | 16091 |
| 0.4 | 80 | 81 | 16128 |
| 0.5 | 86 | 87 | 17093 |
| 0.5 | 89 | 90 | 17413 |
| 1.2 | 125 | 127 | 24123 |
| 1.8 | 130 | 132 | 24798 |
| 2.6 | 132 | 134 | 25089 |
| 3.9 | 133 | 136 | 25457 |
| 4.5 | 134 | 137 | 25494 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 439 |
| `java.lang.invoke.LambdaForm$DMH/` | 13 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 452 |
| `io.jsonwebtoken.impl.ParameterMap` | 1 |
| `java.util.concurrent.locks` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 181 | 181 | coincide |
| Mascotas | 181 | 181 | coincide |
| Citas agendadas | 171 | 171 | coincide |
| Diagnósticos | 76 | 76 | coincide |
| Recetas | 76 | 76 | coincide |
| Pagos | 43 | 43 | coincide |
| Citas completadas | 76 | 76 | coincide |
| Citas canceladas | 24 | 24 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 181 | 0 | 0.0 % | 45 | 184 |
| Registrar mascota | 181 | 0 | 0.0 % | 28 | 104 |
| Agendar cita | 171 | 10 | 5.5 % | 40 | 162 |
| Recepción (llegada) | 77 | 0 | 0.0 % | 16 | 52 |
| Iniciar atención | 76 | 1 | 1.3 % | 22 | 86 |
| Leer la consulta | 76 | 0 | 0.0 % | 14 | 49 |
| Registrar diagnóstico | 76 | 0 | 0.0 % | 13 | 48 |
| Registrar receta | 76 | 0 | 0.0 % | 15 | 60 |
| Actualizar mascota | 76 | 0 | 0.0 % | 15 | 52 |
| Cerrar la consulta | 76 | 0 | 0.0 % | 27 | 110 |
| Caja: crear punto de cobro | 35 | 0 | 0.0 % | 11 | 42 |
| Caja: vincular equipo | 35 | 0 | 0.0 % | 13 | 26 |
| Caja: abrir la caja | 35 | 0 | 0.0 % | 16 | 33 |
| Cobrar | 43 | 0 | 0.0 % | 18 | 38 |
| Cancelar cita | 24 | 0 | 0.0 % | 13 | 64 |

**Endpoints que más tiempo acumulan, ANTES:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 234 | 0 (0 %) | 317 | 720 | 27 % |
| login | 60 | 0 (0 %) | 894 | 1 931 | 18 % |
| reporte_clinico | 12 | 0 (0 %) | 3 778 | 8 099 | 17 % |
| citas_del_mes | 139 | 0 (0 %) | 112 | 336 | 6 % |
| propietarios | 139 | 0 (0 %) | 69 | 232 | 4 % |
| apoderado | 181 | 0 (0 %) | 45 | 184 | 4 % |
| cita | 181 | 10 (6 %) | 40 | 162 | 3 % |
| mascota | 181 | 0 (0 %) | 28 | 104 | 2 % |
| navegacion | 234 | 0 (0 %) | 16 | 87 | 2 % |
| avisos_de_cumplimiento | 234 | 0 (0 %) | 14 | 58 | 2 % |
| lista_de_historias | 110 | 0 (0 %) | 27 | 119 | 2 % |
| empleados | 139 | 0 (0 %) | 25 | 123 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 9 | 63 | 1 213 | 0 % |
| 0.2 | 22 | 113 | 437 | 0 % |
| 0.4 | 35 | 134 | 1 571 | 1 % |
| 0.7 | 47 | 230 | 519 | 1 % |
| 0.9 | 60 | 260 | 846 | 1 % |
| 1.2 | 60 | 246 | 294 | 0 % |
| 1.4 | 60 | 258 | 262 | 0 % |
| 1.7 | 60 | 267 | 217 | 0 % |
| 1.9 | 60 | 232 | 248 | 0 % |
| 2.2 | 60 | 245 | 242 | 0 % |
| 2.4 | 60 | 266 | 267 | 0 % |
| 2.7 | 60 | 243 | 221 | 1 % |
| 2.9 | 60 | 241 | 279 | 0 % |
| 3.2 | 60 | 241 | 280 | 0 % |
| 3.4 | 60 | 260 | 252 | 0 % |
| 3.7 | 60 | 151 | 501 | 1 % |
| 3.9 | 43 | 46 | 237 | 2 % |

</details>

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1234; fallidas: 8.
- Acciones hechas: panel 152, agenda 132, historia 126, reportes 20, recepcion 174, atencion 75, cobro 48, cancelar 33; sin trabajo disponible: 137.
- CPU del backend: 45 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 12 % del tiempo.
- CPU de PostgreSQL: 30 % de media y 96 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 007 MB (49 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **60 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.1 s.
- Correo: el servidor de pruebas recibió 174 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 133 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 131 a 133 MB (+1.8 %).
- GC: 233 pausas, 14.3 s en total (5 % del tiempo de vida de la JVM); 17 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 517 MB (tope -Xmx1g).
- Clases cargadas: 25317 en total, 421 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 13 | 13 | 3754 |
| 0.1 | 19 | 19 | 4660 |
| 0.1 | 26 | 27 | 6219 |
| 0.2 | 49 | 50 | 10917 |
| 0.2 | 66 | 67 | 13687 |
| 0.3 | 80 | 81 | 16048 |
| 0.4 | 80 | 81 | 16103 |
| 0.4 | 84 | 85 | 16791 |
| 0.5 | 87 | 88 | 17213 |
| 1.2 | 124 | 126 | 23919 |
| 2.0 | 129 | 132 | 24720 |
| 2.4 | 131 | 134 | 24904 |
| 3.6 | 133 | 135 | 25189 |
| 4.4 | 133 | 136 | 25303 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 407 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 418 |
| `io.micrometer.core.instrument` | 2 |
| `io.jsonwebtoken.impl.ParameterMap` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 174 | 174 | coincide |
| Mascotas | 174 | 174 | coincide |
| Citas agendadas | 166 | 166 | coincide |
| Diagnósticos | 75 | 75 | coincide |
| Recetas | 75 | 75 | coincide |
| Pagos | 48 | 48 | coincide |
| Citas completadas | 75 | 75 | coincide |
| Citas canceladas | 33 | 33 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 174 | 0 | 0.0 % | 48 | 199 |
| Registrar mascota | 174 | 0 | 0.0 % | 31 | 64 |
| Agendar cita | 166 | 8 | 4.6 % | 40 | 92 |
| Recepción (llegada) | 75 | 0 | 0.0 % | 20 | 83 |
| Iniciar atención | 75 | 0 | 0.0 % | 29 | 67 |
| Leer la consulta | 75 | 0 | 0.0 % | 20 | 48 |
| Registrar diagnóstico | 75 | 0 | 0.0 % | 19 | 39 |
| Registrar receta | 75 | 0 | 0.0 % | 20 | 52 |
| Actualizar mascota | 75 | 0 | 0.0 % | 17 | 44 |
| Cerrar la consulta | 75 | 0 | 0.0 % | 33 | 73 |
| Caja: crear punto de cobro | 38 | 0 | 0.0 % | 15 | 72 |
| Caja: vincular equipo | 38 | 0 | 0.0 % | 16 | 42 |
| Caja: abrir la caja | 38 | 0 | 0.0 % | 24 | 43 |
| Cobrar | 48 | 0 | 0.0 % | 19 | 56 |
| Cancelar cita | 33 | 0 | 0.0 % | 33 | 76 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 212 | 0 (0 %) | 256 | 1 089 | 31 % |
| reporte_clinico | 20 | 0 (0 %) | 2 090 | 5 782 | 17 % |
| login | 60 | 0 (0 %) | 494 | 1 423 | 13 % |
| apoderado | 174 | 0 (0 %) | 48 | 199 | 4 % |
| propietarios | 132 | 0 (0 %) | 76 | 171 | 4 % |
| citas_del_mes | 132 | 0 (0 %) | 45 | 177 | 3 % |
| cita | 174 | 8 (5 %) | 40 | 92 | 3 % |
| lista_de_historias | 126 | 0 (0 %) | 54 | 140 | 3 % |
| mascota | 174 | 0 (0 %) | 31 | 64 | 2 % |
| navegacion | 212 | 0 (0 %) | 17 | 44 | 2 % |
| historia | 126 | 0 (0 %) | 36 | 76 | 2 % |
| cumplimiento | 212 | 0 (0 %) | 17 | 45 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 11 | 73 | 746 | 0 % |
| 0.2 | 23 | 127 | 378 | 0 % |
| 0.5 | 36 | 173 | 385 | 0 % |
| 0.7 | 49 | 243 | 350 | 0 % |
| 1.0 | 60 | 259 | 354 | 0 % |
| 1.2 | 60 | 241 | 361 | 0 % |
| 1.5 | 60 | 222 | 468 | 0 % |
| 1.7 | 60 | 242 | 194 | 1 % |
| 2.0 | 60 | 242 | 152 | 0 % |
| 2.2 | 60 | 261 | 197 | 0 % |
| 2.5 | 60 | 219 | 388 | 0 % |
| 2.7 | 60 | 236 | 207 | 0 % |
| 3.0 | 60 | 241 | 253 | 1 % |
| 3.2 | 60 | 221 | 429 | 0 % |
| 3.5 | 60 | 247 | 156 | 0 % |
| 3.7 | 59 | 119 | 1 097 | 1 % |
| 4.0 | 39 | 42 | 141 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor, registros de la JVM y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.

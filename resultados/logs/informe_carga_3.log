# Fase 2: carga con escrituras, 3 usuarios simultáneos

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | AGUANTA | DEGRADADO |
| Último tramo sano (usuarios) | 3 | 3 |
| Peticiones hechas | 81 | 82 |
| Peticiones correctas (2xx) | 81 | 81 |
| Errores (%) | 0.0 % | 1.2 % |
| Ritmo medio (peticiones/s) | 0.7 | 0.8 |
| Tiempo mediano (ms) | 46 | 52 |
| Tiempo p95 (ms) | 335 | 346 |
| CPU del backend, media (%) | 8 | 6 |
| Tiempo con la CPU al tope (%) | 0 | 0 |
| CPU de PostgreSQL, media (%) | 1 | 0 |
| Memoria pico (MB) | 503 | 604 |
| Sentencias SQL | 4 047 | 2 279 |
| Reinicios del contenedor | 0 | 0 |

## ANTES: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 12; fallidas: 0.
- Acciones hechas: panel 6, agenda 2, historia 2, reportes 0, recepcion 4, atencion 0, cobro 0, cancelar 0; sin trabajo disponible: 1.
- CPU del backend: 8 % de media y 90 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 7 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 503 MB (25 % de 2048 MB).
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **3 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 4 | 4 | coincide |
| Mascotas | 4 | 4 | coincide |
| Citas agendadas | 4 | 4 | coincide |
| Diagnósticos | 0 | 0 | coincide |
| Recetas | 0 | 0 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 0 | 0 | coincide |
| Citas canceladas | 0 | 0 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 4 | 0 | 0.0 % | 85 | 211 |
| Registrar mascota | 4 | 0 | 0.0 % | 56 | 191 |
| Agendar cita | 4 | 0 | 0.0 % | 84 | 557 |

**Endpoints que más tiempo acumulan, ANTES:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 9 | 0 (0 %) | 199 | 483 | 26 % |
| login | 3 | 0 (0 %) | 370 | 867 | 20 % |
| cita | 4 | 0 (0 %) | 84 | 557 | 10 % |
| apoderado | 4 | 0 (0 %) | 85 | 211 | 6 % |
| navegacion | 9 | 0 (0 %) | 33 | 148 | 5 % |
| citas_del_mes | 2 | 0 (0 %) | 184 | 220 | 5 % |
| mascota | 4 | 0 (0 %) | 56 | 191 | 5 % |
| privacidad | 9 | 0 (0 %) | 29 | 57 | 3 % |
| avisos_de_cumplimiento | 9 | 0 (0 %) | 24 | 66 | 3 % |
| cumplimiento | 9 | 0 (0 %) | 26 | 46 | 3 % |
| propietarios | 2 | 0 (0 %) | 119 | 141 | 3 % |
| notificaciones | 9 | 0 (0 %) | 20 | 46 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 3 | 21 | 483 | 0 % |
| 0.2 | 3 | 12 | 557 | 0 % |
| 0.5 | 3 | 12 | 220 | 0 % |
| 0.7 | 3 | 10 | 177 | 0 % |
| 1.0 | 3 | 11 | 219 | 0 % |
| 1.2 | 3 | 9 | 167 | 0 % |
| 1.5 | 2 | 6 | 153 | 0 % |

</details>

## DESPUÉS: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 36; fallidas: 1.
- Acciones hechas: panel 3, agenda 2, historia 0, reportes 0, recepcion 6, atencion 2, cobro 1, cancelar 1; sin trabajo disponible: 4.
- CPU del backend: 6 % de media y 49 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 0 % de media y 2 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 604 MB (30 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 1.0 con unos 3 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **3 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.4 s.

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 6 | 6 | coincide |
| Mascotas | 6 | 6 | coincide |
| Citas agendadas | 5 | 5 | coincide |
| Diagnósticos | 2 | 2 | coincide |
| Recetas | 2 | 2 | coincide |
| Pagos | 1 | 1 | coincide |
| Citas completadas | 2 | 2 | coincide |
| Citas canceladas | 1 | 1 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 6 | 0 | 0.0 % | 96 | 313 |
| Registrar mascota | 6 | 0 | 0.0 % | 45 | 211 |
| Agendar cita | 5 | 1 | 16.7 % | 67 | 313 |
| Recepción (llegada) | 2 | 0 | 0.0 % | 45 | 56 |
| Iniciar atención | 2 | 0 | 0.0 % | 55 | 63 |
| Leer la consulta | 2 | 0 | 0.0 % | 42 | 54 |
| Registrar diagnóstico | 2 | 0 | 0.0 % | 41 | 49 |
| Registrar receta | 2 | 0 | 0.0 % | 47 | 58 |
| Actualizar mascota | 2 | 0 | 0.0 % | 45 | 48 |
| Cerrar la consulta | 2 | 0 | 0.0 % | 107 | 153 |
| Caja: crear punto de cobro | 1 | 0 | 0.0 % | 58 | 58 |
| Caja: vincular equipo | 1 | 0 | 0.0 % | 52 | 52 |
| Caja: abrir la caja | 1 | 0 | 0.0 % | 54 | 54 |
| Cobrar | 1 | 0 | 0.0 % | 66 | 66 |
| Cancelar cita | 1 | 0 | 0.0 % | 68 | 68 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 3 | 0 (0 %) | 399 | 895 | 22 % |
| panel | 6 | 0 (0 %) | 134 | 406 | 14 % |
| apoderado | 6 | 0 (0 %) | 96 | 313 | 12 % |
| cita | 6 | 1 (17 %) | 67 | 313 | 8 % |
| propietarios | 2 | 0 (0 %) | 241 | 396 | 6 % |
| mascota | 6 | 0 (0 %) | 45 | 211 | 6 % |
| navegacion | 6 | 0 (0 %) | 36 | 134 | 4 % |
| citas_del_mes | 2 | 0 (0 %) | 116 | 145 | 3 % |
| cerrar_consulta | 2 | 0 (0 %) | 107 | 153 | 3 % |
| privacidad | 6 | 0 (0 %) | 32 | 63 | 3 % |
| cumplimiento | 6 | 0 (0 %) | 29 | 57 | 2 % |
| avisos_de_cumplimiento | 6 | 0 (0 %) | 23 | 60 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 3 | 21 | 406 | 0 % |
| 0.2 | 3 | 6 | 396 | 0 % |
| 0.5 | 3 | 16 | 153 | 0 % |
| 0.7 | 3 | 13 | 126 | 0 % |
| 1.0 | 3 | 7 | 108 | 14 % |
| 1.2 | 3 | 16 | 117 | 0 % |
| 1.5 | 3 | 3 | 313 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.

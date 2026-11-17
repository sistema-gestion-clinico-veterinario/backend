# Fase 2: carga con escrituras, 250 usuarios simultáneos

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | FALLA | FALLA |
| Último tramo sano (usuarios) | 66 | 52 |
| Peticiones hechas | 2 721 | 2 830 |
| Peticiones correctas (2xx) | 871 | 987 |
| Errores (%) | 68.0 % | 65.1 % |
| Ritmo medio (peticiones/s) | 5.6 | 5.8 |
| Tiempo mediano (ms) | 30 000 | 30 000 |
| Tiempo p95 (ms) | 30 001 | 30 001 |
| CPU del backend, media (%) | 98 | 98 |
| Tiempo con la CPU al tope (%) | 91 | 93 |
| CPU de PostgreSQL, media (%) | 3 | 3 |
| Memoria pico (MB) | 728 | 882 |
| Sentencias SQL | 50 908 | 26 833 |
| Reinicios del contenedor | 0 | 0 |

## ANTES: FALLA; datos CONSISTENTES

- Escrituras correctas: 138; fallidas: 157.
- Acciones hechas: panel 70, agenda 73, historia 63, reportes 9, recepcion 149, atencion 24, cobro 0, cancelar 8; sin trabajo disponible: 90.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 585 errores `OutOfMemoryError` en el registro.
- CPU del backend: 98 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 91 % del tiempo.
- CPU de PostgreSQL: 3 % de media y 58 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 728 MB (36 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 1.1 con unos 81 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **66 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 43 | 43 | coincide |
| Mascotas | 42 | 42 | coincide |
| Citas agendadas | 41 | 41 | coincide |
| Diagnósticos | 1 | 1 | coincide |
| Recetas | 1 | 1 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 0 | 0 | coincide |
| Citas canceladas | 1 | 1 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 43 | 106 | 71.1 % | 30 000 | 30 001 |
| Registrar mascota | 42 | 1 | 2.3 % | 58 | 1 588 |
| Agendar cita | 41 | 1 | 2.4 % | 84 | 316 |
| Recepción (llegada) | 6 | 18 | 75.0 % | 30 000 | 30 001 |
| Iniciar atención | 1 | 23 | 95.8 % | 30 000 | 30 001 |
| Leer la consulta | 1 | 0 | 0.0 % | 550 | 550 |
| Registrar diagnóstico | 1 | 0 | 0.0 % | 131 | 131 |
| Registrar receta | 1 | 0 | 0.0 % | 115 | 115 |
| Actualizar mascota | 1 | 0 | 0.0 % | 72 | 72 |
| Cerrar la consulta | 0 | 1 | 100.0 % | 1 259 | 1 259 |
| Cancelar cita | 1 | 7 | 87.5 % | 30 000 | 30 001 |

**Endpoints que más tiempo acumulan, ANTES:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 1217 | 1154 (95 %) | 30 000 | 30 001 | 63 % |
| apoderado | 149 | 106 (71 %) | 30 000 | 30 001 | 6 % |
| privacidad | 153 | 61 (40 %) | 33 | 30 001 | 3 % |
| cumplimiento | 148 | 56 (38 %) | 34 | 30 001 | 3 % |
| navegacion | 153 | 61 (40 %) | 34 | 30 001 | 3 % |
| empleados | 77 | 52 (68 %) | 30 000 | 30 001 | 3 % |
| notificaciones | 143 | 51 (36 %) | 26 | 30 001 | 3 % |
| citas_del_mes | 77 | 51 (66 %) | 30 000 | 30 001 | 3 % |
| lista_de_historias | 63 | 49 (78 %) | 30 000 | 30 001 | 3 % |
| propietarios | 73 | 48 (66 %) | 30 000 | 30 001 | 3 % |
| panel | 133 | 41 (31 %) | 296 | 30 001 | 2 % |
| avisos_de_cumplimiento | 134 | 42 (31 %) | 28 | 30 001 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 6 | 42 | 741 | 0 % |
| 0.4 | 36 | 197 | 415 | 1 % |
| 0.9 | 66 | 252 | 497 | 0 % |
| 1.4 | 96 | 14 | 30 000 | 100 % |
| 1.9 | 126 | 34 | 30 001 | 100 % |
| 2.4 | 156 | 52 | 30 001 | 100 % |
| 2.9 | 186 | 55 | 30 001 | 100 % |
| 3.4 | 216 | 79 | 30 001 | 100 % |
| 3.9 | 246 | 70 | 30 001 | 100 % |
| 4.4 | 250 | 92 | 30 001 | 100 % |
| 4.9 | 250 | 95 | 30 001 | 100 % |
| 5.4 | 250 | 78 | 30 001 | 100 % |
| 5.9 | 250 | 99 | 30 001 | 100 % |
| 6.4 | 250 | 71 | 30 001 | 100 % |
| 6.9 | 236 | 77 | 30 001 | 100 % |
| 7.4 | 117 | 38 | 30 001 | 100 % |
| 7.9 | 36 | 12 | 30 001 | 100 % |

</details>

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 155; fallidas: 169.
- Acciones hechas: panel 81, agenda 83, historia 59, reportes 6, recepcion 165, atencion 24, cobro 0, cancelar 7; sin trabajo disponible: 112.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 735 errores `OutOfMemoryError` en el registro.
- CPU del backend: 98 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 93 % del tiempo.
- CPU de PostgreSQL: 3 % de media y 43 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 882 MB (43 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 1.6 con unos 112 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **52 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 45 | 45 | coincide |
| Mascotas | 44 | 45 | la base tiene más (ver nota) |
| Citas agendadas | 44 | 44 | coincide |
| Diagnósticos | 3 | 3 | coincide |
| Recetas | 3 | 3 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 0 | 0 | coincide |
| Citas canceladas | 1 | 1 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 45 | 120 | 72.7 % | 30 000 | 30 001 |
| Registrar mascota | 44 | 1 | 2.2 % | 50 | 656 |
| Agendar cita | 44 | 0 | 0.0 % | 72 | 229 |
| Recepción (llegada) | 6 | 18 | 75.0 % | 30 000 | 30 001 |
| Iniciar atención | 3 | 21 | 87.5 % | 30 000 | 30 001 |
| Leer la consulta | 3 | 0 | 0.0 % | 40 | 73 |
| Registrar diagnóstico | 3 | 0 | 0.0 % | 99 | 247 |
| Registrar receta | 3 | 0 | 0.0 % | 126 | 209 |
| Actualizar mascota | 3 | 0 | 0.0 % | 59 | 91 |
| Cerrar la consulta | 0 | 3 | 100.0 % | 1 459 | 5 586 |
| Cancelar cita | 1 | 6 | 85.7 % | 30 000 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 1168 | 1099 (94 %) | 30 000 | 30 001 | 60 % |
| apoderado | 165 | 120 (73 %) | 30 000 | 30 001 | 7 % |
| privacidad | 171 | 64 (37 %) | 33 | 30 001 | 3 % |
| citas_del_mes | 85 | 63 (74 %) | 30 000 | 30 001 | 3 % |
| empleados | 85 | 63 (74 %) | 30 000 | 30 001 | 3 % |
| navegacion | 171 | 63 (37 %) | 38 | 30 001 | 3 % |
| propietarios | 83 | 61 (73 %) | 30 000 | 30 001 | 3 % |
| cumplimiento | 167 | 60 (36 %) | 39 | 30 001 | 3 % |
| notificaciones | 160 | 53 (33 %) | 26 | 30 001 | 3 % |
| avisos_de_cumplimiento | 155 | 48 (31 %) | 27 | 30 001 | 3 % |
| panel | 150 | 43 (29 %) | 165 | 30 001 | 2 % |
| lista_de_historias | 59 | 43 (73 %) | 30 000 | 30 001 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 7 | 56 | 891 | 0 % |
| 0.4 | 37 | 196 | 669 | 0 % |
| 0.9 | 67 | 309 | 873 | 2 % |
| 1.4 | 97 | 2 | 30 001 | 100 % |
| 1.9 | 127 | 32 | 30 001 | 100 % |
| 2.4 | 157 | 60 | 30 001 | 100 % |
| 2.9 | 187 | 53 | 30 001 | 100 % |
| 3.4 | 217 | 83 | 30 001 | 100 % |
| 3.9 | 247 | 92 | 30 001 | 100 % |
| 4.4 | 250 | 92 | 30 001 | 100 % |
| 4.9 | 250 | 95 | 30 001 | 100 % |
| 5.4 | 250 | 89 | 30 001 | 100 % |
| 5.9 | 250 | 82 | 30 001 | 100 % |
| 6.4 | 250 | 75 | 30 001 | 100 % |
| 6.9 | 227 | 85 | 30 001 | 100 % |
| 7.4 | 106 | 35 | 30 001 | 100 % |
| 7.9 | 33 | 14 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.

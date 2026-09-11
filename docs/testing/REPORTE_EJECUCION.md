# Reporte de ejecución de pruebas ERS 1.2

## Estado actual

Las fases 1 y 2 prepararon la trazabilidad y el diseño técnico. La primera pasada de los cinco lotes quedó ejecutada y clasificada. Además, se completó una pasada E2E controlada contra los commits desplegados, utilizando exclusivamente datos ficticios de la empresa de pruebas con ID 3. No se corrigió código funcional durante la evaluación.

Las pruebas preexistentes del proyecto no se marcan automáticamente como evidencia. Primero deben vincularse con un CP y sus CA, ejecutarse sobre un commit identificado y conservar su salida.

## Resumen

| Métrica | Cantidad |
|---|---:|
| Casos definidos en ERS | 91 |
| Casos clasificados después de la primera pasada | 91 |
| Casos con carpeta de evidencia de lote | 91 |
| Casos completamente acreditados en todos sus niveles requeridos | 22 |
| Ejecuciones técnicas acumuladas | 297 |
| Ejecuciones técnicas aprobadas en su primera evaluación | 275 |
| Ejecuciones técnicas fallidas en su primera evaluación | 22 |
| Cumple | 22 |
| No cumple | 18 |
| Bloqueado | 6 |
| Pendiente | 45 |

Las 297 son ejecuciones o aserciones acumuladas, no casos únicos: algunas se repitieron deliberadamente porque aportan evidencia a más de un lote o agregan un nivel de comprobación. La pasada desplegada añadió 107 aserciones: 98 aprobaron inicialmente y 9 fallaron. Dos fallas se demostraron falsos negativos del oráculo y quedaron corregidas mediante una verificación independiente; las restantes corresponden a comportamientos del sistema. El cambio de correo produjo además una incidencia consolidada fuera de ese conteo. Una prueba técnica aprobada solo convierte un CP en `Cumple` cuando cubre todos los niveles y condiciones que exige su fila.

## Pasada E2E de seguridad y gestión de usuarios — despliegue controlado

- Identificador: `PRODUCCION-SEGURIDAD-E2E-7818730-20260908`.
- Frontend: commit `a95c1306f22b61dd1294096a41a80be1d621957a` desplegado en Vercel.
- Backend: commit `7818730744926d9cccfb07221eb8a03ce336a574` desplegado en Render.
- Datos: empresa ficticia ID 3; cuentas y roles temporales con prefijo QA.
- Evidencia: `docs/testing/evidencias/7818730/2026-09-08T18-35-16-05-00/PRODUCCION-SEGURIDAD-E2E/`.
- Higiene: las evidencias versionables no contienen correos, contraseñas, documentos, identificadores de personas, tokens ni cookies. El estado privado permanece ignorado por Git.
- Limpieza: la cuenta QA principal y la cuenta creada al demostrar la duplicidad documental quedaron desactivadas; los roles temporales fueron restaurados o desactivados.

### Comprobaciones aprobadas

- inicio de sesión válido, rechazo uniforme de credenciales inválidas y bloqueo de cuentas inactivas;
- cookies `HttpOnly`, `Secure` y `SameSite=None`, tokens omitidos del cuerpo y rotación del refresh token;
- recuperación y cambio de contraseña con consumo único del token y rechazo de la contraseña anterior;
- alta de empleado con activación por enlace temporal sin contraseña administrada por el creador;
- creación, actualización, asignación y desactivación de roles personalizados;
- permisos dinámicos por acción, rechazo HTTP 403 de acciones omitidas y control de concurrencia mediante HTTP 409;
- estructura mixta de menú agrupado y plano después de renovar la sesión;
- aislamiento por empresa en siete consultas con manipulación de `companyId`;
- desactivación con invalidación inmediata de la sesión vigente y bloqueo de nuevos ingresos;
- límite de login por IP: veinte rechazos HTTP 401 y HTTP 429 en el intento 21, con `Retry-After: 60`;
- HSTS, CSP, `nosniff`, política de referente, política de permisos, `no-store` y CORS restrictivo.

### No conformidades confirmadas en esta pasada

1. `CP-RF04-02`: el cambio de correo no completó la doble confirmación. Una ejecución no recibió ambos mensajes y otra respondió HTTP 500. El envío síncrono de dos correos dentro de la transacción acopla el caso de uso a la disponibilidad del proveedor.
2. `CP-RF10-02`: un segundo empleado con el mismo documento fue aceptado con HTTP 201. El correo duplicado sí fue rechazado con HTTP 400.
3. `CP-RF11-02`: la desactivación bloquea correctamente las sesiones, pero el endpoint no solicita ni persiste el motivo exigido por el criterio de aceptación.
4. `CP-RNF02-02`: un rol con alcance `OWN` y sin pacientes asignados obtuvo diez clientes, diez historias y acceso HTTP 200 a detalles ajenos. Agenda sí devolvió cero citas, por lo que el alcance está aplicado de manera inconsistente. Continúa además la falla previa de creación de recetas entre empresas.
5. `CP-RF08-03`: la API guardó el orden y la presentación `FLAT`, pero el menú de una sesión renovada no los aplicó. `MenuBuilderService` construye el menú con `presentacionDefault` y órdenes globales en lugar de consultar la configuración por rol.

### Incidencias y correcciones del procedimiento

- El segundo uso del enlace de activación respondió HTTP 404. El oráculo inicial esperaba otro 4xx concreto; se corrigió porque 404 representa adecuadamente un token consumido sin revelar información.
- El detalle de empleado no serializa `estado`, aunque el listado lo expone como `activo`. La conservación e inactividad se verificaron en una consulta independiente 3/3; no se contabilizó como defecto de desactivación.
- La primera prueba CORS apuntó a una ruta sin el prefijo `/api/v1` y recibió 404. Esa ejecución se descartó; la repetición sobre la ruta correcta aprobó 3/3.

### Cobertura que permanece pendiente o bloqueada

- `CP-RF06-02`: no se intentó eliminar ni alterar un rol técnico privilegiado en el despliegue, para evitar afectar la recuperación administrativa; requiere una cuenta `PLATFORM_ADMIN` y un entorno recuperable.
- `CP-RF08-02` dejó de estar pendiente: la comparación controlada obtuvo 0 citas con `OWN` y 10 con `COMPANY`.
- `CP-RNF02-01`: se probaron acciones seleccionadas, no todavía el 100 % de la matriz negativa de endpoints y acciones.
- `CP-RNF03-01`: las cookies no son accesibles a JavaScript, pero falta mantener una sesión realmente inactiva durante treinta minutos.
- No se creó una segunda empresa porque la cuenta proporcionada resultó ser `COMPANY_ADMIN`, no `PLATFORM_ADMIN`; el aislamiento se evaluó contra una empresa existente sin modificarla.

## Control de integridad de la matriz

La validación documental de la matriz técnica produjo lo siguiente:

- 91 filas y 91 códigos CP únicos;
- 77 casos funcionales y 14 no funcionales;
- 124 criterios de aceptación del ERS referenciados, sin códigos desconocidos ni CA omitidos;
- 75 casos con prioridad M y 16 con prioridad S;
- 27 casos con vínculo Directa, de los cuales 25 son prioridad M;
- 22 casos en el Lote 1, 19 en el Lote 2, 20 en el Lote 3, 20 en el Lote 4 y 10 en el Lote 5;
- 91 casos con resultado clasificado y referencia a la carpeta de su ejecución o evaluación de lote.

Estas cifras demuestran consistencia estructural, no cumplimiento funcional.

## Ejecución consolidada del Lote 1 — 8 de septiembre de 2026

- Identificador: `LOTE1-d05ec16-20260908T114445-0500`.
- Comando: `mvn -Dtest=RF30Test,RF17To21Test,RF29Test,RF31Test,CitaServiceIntegrationTest,CartillaServiceImplTest,RecordatorioPreventivoServiceImplTest test`.
- Base: commit `d05ec16df652d10a8f9380bd52b6e0ef815612d7` con árbol de trabajo modificado.
- Ambiente: Spring Boot 3.4.2, compilación Java 21, ejecución OpenJDK 22.0.2, Maven 3.9.9 y H2 2.3.232 en memoria.
- Alcance planificado: 22 CP de RF-17, RF-18, RF-20, RF-21, RF-28, RF-29, RF-30, RF-31 y RF-33.
- Hallazgo transversal: la prueba negativa de recetas también ejecutó parcialmente `CP-RNF02-02`, planificado originalmente para el Lote 2.
- Resultado técnico: 36 pruebas, 31 aprobadas, 5 fallidas, 0 errores y 0 omitidas.
- Resultado ERS del Lote 1: 3 CP `No cumple` y 19 CP `Pendiente` por niveles API, E2E, persistencia o concurrencia aún no ejecutados.
- Resultado transversal: 1 CP `No cumple` de RNF-02.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T11-44-45-05-00/LOTE1/`.

Las cinco fallas técnicas no equivalen a cinco CP: dos comprueban campos distintos del mismo `CP-RF31-02`. No se marca ningún caso como `Cumple` todavía porque aprobar una prueba unitaria o de servicio no cubre por sí sola todos los niveles exigidos en la matriz.

### No conformidades confirmadas

1. `CP-RF30-03`: se acepta el peso `10.123`, pese al máximo de dos decimales.
2. `CP-RF30-04`: se permite cerrar sin decisiones explícitas de vacunación y desparasitación.
3. `CP-RF31-02`: duración e indicaciones pueden omitirse en una receta sin producir violaciones de validación.
4. `CP-RNF02-02` — hallazgo transversal: la creación de una receta no rechaza una consulta perteneciente a otra empresa. `CP-RF29-02` permanece Pendiente porque su comprobación específica de lectura de historia ajena sí fue aprobada a nivel de servicio y aún requiere API/E2E.

### Comprobaciones backend aprobadas parcialmente

- consulta y registro transaccional de clientes y mascotas;
- reutilización de historia existente y creación de respaldo al iniciar una atención;
- cronología descendente de consultas y separación de preventivos;
- creación válida de receta;
- registro separado de vacunación y desparasitación, cálculo de próximos controles, preparación manual de WhatsApp y deduplicación inicial de recordatorios.

Estas aprobaciones permanecen `Pendiente` en la matriz hasta ejecutar el resto del nivel indicado para cada CP.

### Inconsistencia que requiere decisión funcional

RF-28 indica que la historia se crea al iniciar la primera atención, pero `MascotaServiceImpl.registerMascota` crea una historia inmediatamente al registrar la mascota. La prueba de RF-28 confirma que el servicio de citas sabe crearla si falta, no que el flujo real espere hasta la atención. Antes de modificar este comportamiento debe definirse cuál de los dos ciclos de vida es el autorizado y alinear ERS, código y pruebas.

## Ejecución del Lote 2 — seguridad e integridad

- Identificador: `LOTE2-d05ec16-20260908T115151-0500`.
- Alcance: 19 CP de autenticación, recuperación y cambio de credenciales, roles, permisos, sesiones e integridad.
- Resultado técnico: 48 pruebas, 48 aprobadas y ninguna falla.
- Resultado ERS: 18 `Pendiente` y 1 `No cumple` (`CP-RNF02-02`, confirmado por la prueba transversal del Lote 1).
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T11-51-51-05-00/LOTE2/`.

La cobertura existente comprueba política de contraseñas, doble confirmación de correo, invalidación de sesiones, permisos, menú y aislamientos parciales. Faltan los flujos API/E2E y la matriz negativa completa requerida para afirmar rechazo del 100 % de acciones no autorizadas.

## Ejecución del Lote 3 — agenda y operación

- Identificador: `LOTE3-d05ec16-20260908T115522-0500`.
- Alcance: 20 CP de empleados, horarios y agenda.
- Resultado técnico: 14 pruebas, 14 aprobadas y ninguna falla.
- Resultado ERS: 20 `Pendiente`.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T11-55-22-05-00/LOTE3/`.

No se confirmó una no conformidad nueva, pero faltan pruebas específicas de exportación, desactivación, reprogramación tardía, alcance OWN/COMPANY y flujos completos visibles.

## Ejecución del Lote 4 — administración y pagos

- Identificador: `LOTE4-d05ec16-20260908T115626-0500`.
- Alcance: 20 CP de empresa, clientes, mascotas, archivos, catálogos y pagos.
- Resultado técnico: 40 pruebas, 40 aprobadas y ninguna falla.
- Resultado ERS: 20 `Pendiente`.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T11-56-26-05-00/LOTE4/`.

Las pruebas candidatas no cubren de manera específica todos los casos. Permanecen especialmente pendientes la actualización y baja de clientes, el flujo completo de archivos clínicos, los ciclos de catálogos y los flujos API/E2E de pagos.

## Evaluación del Lote 5 — no funcionales restantes

- Identificador: `LOTE5-d05ec16-20260908T115701-0500`.
- Alcance: 10 CP no funcionales.
- Resultado técnico: 3 pruebas del mecanismo de medición, todas aprobadas.
- Resultado ERS: 4 `Pendiente` y 6 `Bloqueado`.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T11-57-01-05-00/LOTE5/`.

Los bloqueos son deliberados y evitan fabricar evidencia. Se necesitan participantes reales para usabilidad, navegadores y resoluciones concretas para compatibilidad, un período de monitoreo para disponibilidad, acceso autorizado a respaldos, comprobantes reales de costos y el equipo físico de la veterinaria. La prueba del filtro de rendimiento demuestra que el sistema registra mediciones, no que el despliegue cumpla por sí solo el límite de dos segundos.

## Segunda pasada — contratos API iniciales

- Identificador: `API2-d05ec16-20260908T123619-0500`.
- Alcance parcial: RF-01, RF-02, RF-03, RF-04, RF-06, RF-08, RF-30 y RNF-03.
- Resultado técnico: 11 pruebas, 9 aprobadas y 2 fallidas.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T12-36-19-05-00/SEGUNDA-PASADA-API/`.

Los contratos aprobados comprueban que los tokens no se serializan en el JSON de autenticación, las cookies se generan con `HttpOnly`, `Secure` y `SameSite`, la recuperación usa un mensaje neutro, el cambio de contraseña toma la identidad autenticada, el correo conserva dos confirmaciones, el administrador de empresa no puede forzar otro `companyId` al crear roles y la versión de permisos se transmite mediante `If-Match`.

En el cierre clínico, la API rechazó correctamente la ausencia de versión con HTTP 400. Sin embargo, confirmó los dos defectos ya conocidos: peso con tres decimales y decisiones preventivas ausentes obtienen HTTP 200. Los estados globales permanecen en 4 `No cumple`, 81 `Pendiente` y 6 `Bloqueado`.

## Segunda pasada — recetas por API

- Identificador: `RECETAS-API-d05ec16-20260908T124915-0500`.
- Alcance: `CP-RF31-01` y `CP-RF31-02` en el contrato HTTP de creación de recetas.
- Resultado técnico: 3 pruebas, 1 aprobada y 2 fallidas.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T12-49-15-05-00/RECETAS-API/`.

La API acepta una receta completa y devuelve HTTP 201. También acepta con HTTP 201 una receta sin duración y otra sin indicaciones, confirmando en el límite HTTP la no conformidad ya detectada para `CP-RF31-02`. No se modificó el código funcional.

## Segunda pasada — autorización estática de endpoints

- Identificador: `AUTHZ-ENDPOINTS-d05ec16-20260908T125147-0500`.
- Alcance: 24 acciones críticas de apoderados, mascotas, consultas, recetas y roles.
- Resultado técnico: 1 prueba aprobada con 24 aserciones de autorización.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T12-51-47-05-00/AUTORIZACION-ENDPOINTS/`.

La comprobación confirma que los métodos seleccionados declaran la vista, acción y alcance esperados mediante `@PreAuthorize`. Es evidencia de cableado estático, no una prueba de ejecución de Spring Method Security; `CP-RNF02-01` permanece `Pendiente` hasta completar la matriz negativa con identidades y respuestas HTTP reales.

## Segunda pasada — Agenda por integración

- Identificador: `AGENDA-INTEGRACION-d05ec16-20260908T125547-0500`.
- Alcance: `CP-RF24-01`, `CP-RF24-02`, `CP-RF24-03`, `CP-RF26-01`, `CP-RF26-02` y `CP-RF27-01`.
- Resultado técnico: 10 pruebas, 10 aprobadas y ninguna falla.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T12-55-47-05-00/AGENDA-INTEGRACION/`.

Quedaron aprobados a nivel de servicio la creación válida, el detector de cruces, el rechazo de una mascota inactiva, la reprogramación válida, el rechazo cuando falta una hora o menos y la cancelación conservando el antecedente. Los seis CP continúan `Pendiente`: faltan API/E2E, concurrencia real, variantes restantes de entidades inactivas y la notificación exigida por el criterio correspondiente.

## Segunda pasada — Pagos e historial

- Identificador de pagos: `PAGOS-INTEGRACION-d05ec16-20260908T130119-0500`.
- Resultado: 6 pruebas aprobadas, sin fallas.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T13-01-19-05-00/PAGOS-INTEGRACION/`.
- Identificador de historial: `HISTORIAL-PAGOS-CONTRATO-d05ec16-20260908T130215-0500`.
- Resultado: 1 prueba fallida.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T13-02-15-05-00/HISTORIAL-PAGOS-CONTRATO/`.

En RF-44 se aprobaron el pago completo por efectivo, Yape manual y los rechazos de cita cancelada, importe cero, importe superior al saldo y efectivo insuficiente. Los escenarios inválidos no persistieron una compra, no alteraron el saldo y no invocaron Caja. Permanecen pendientes el movimiento de caja persistido y los niveles API/E2E.

`CP-RF45-01` cambia de `Pendiente` a `No cumple`: el endpoint de historial únicamente admite `page`, `size` y `companyId`; no ofrece los filtros por cliente, mascota, período y estado exigidos por el ERS. Los estados globales quedan en 5 `No cumple`, 80 `Pendiente` y 6 `Bloqueado`.

## Segunda pasada — Archivos clínicos RF-32

- Identificador: `ARCHIVOS-CLINICOS-d05ec16-20260908T130805-0500`.
- Resultado: 3 pruebas aprobadas, sin fallas.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T13-08-05-05-00/ARCHIVOS-CLINICOS/`.

Se aprobó a nivel de servicio la carga y consulta posterior de un PDF de laboratorio válido. También se rechazaron una extensión no permitida y un archivo mayor a 20 MB antes de almacenar o persistir. `CP-RF32-01` y `CP-RF32-02` permanecen `Pendiente` hasta probar multipart por API, almacenamiento real y E2E.

## Control consolidado de la segunda pasada

- Identificador: `SEGUNDA-PASADA-CONSOLIDADA-d05ec16-20260908T131016-0500`.
- Resultado: 35 pruebas, 30 aprobadas, 5 fallidas, 0 errores y 0 omitidas.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T13-10-16-05-00/SEGUNDA-PASADA-CONSOLIDADA/`.
- Tratamiento estadístico: no se suma al acumulado porque reejecuta pruebas ya registradas individualmente.

La consolidación reproduce exactamente cinco fallas funcionales: dos de RF-30, dos aserciones del mismo caso RF-31 y una de RF-45. No aparecieron errores de compilación, contexto o infraestructura. La matriz mantiene 91 filas y 91 códigos únicos. Además, `git diff --name-only -- src/main` no devuelve archivos: durante la evaluación no se modificó código funcional.

## Segunda pasada — Personal: filtros y protección de turnos

- Identificador: `PERSONAL-FILTROS-TURNOS-d05ec16-20260908T131640-0500`.
- Resultado: 2 pruebas fallidas, sin errores.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T13-16-40-05-00/PERSONAL-FILTROS-TURNOS/`.

`CP-RF09-01` cambia a `No cumple`: la consulta de empleados ofrece empresa, nombre, apellido, correo, tipo laboral y especialidad, pero no los filtros por documento, rol y estado exigidos. `CP-RF15-01` también cambia a `No cumple`: la eliminación individual de un turno no consulta las citas relacionadas y procede con el borrado. Los estados globales quedan en 7 `No cumple`, 78 `Pendiente` y 6 `Bloqueado`.

## Segunda pasada — ciclo de empleados RF-10 y RF-11

- Identificador: `EMPLEADOS-CICLO-d05ec16-20260908T132111-0500`.
- Resultado: 4 pruebas aprobadas, sin fallas.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T13-21-11-05-00/EMPLEADOS-CICLO/`.

Se comprobó que el alta deja la cuenta inactiva y no verificada, no expone contraseña y envía un enlace cuyo token se conserva únicamente como hash. También se aprobaron el rechazo temprano de correo duplicado, la actualización sin reemplazar horarios ausentes en la solicitud y la desactivación con invalidación de sesiones. Los cuatro CP permanecen `Pendiente` hasta completar API, correo integrado real y E2E.

## Segunda pasada — Horarios RF-12 a RF-14

- Identificador: `HORARIOS-RF12-RF14-d05ec16-20260908T132547-0500`.
- Resultado: 5 pruebas, 4 aprobadas y 1 fallida.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T13-25-47-05-00/HORARIOS-RF12-RF14/`.

Se aprobaron la consulta ordenada de turnos, la creación válida, el rechazo de cruces y una modificación hacia un período libre. `CP-RF14-01` cambia a `No cumple`: la modificación individual no consulta ni identifica citas relacionadas antes de mover el turno. Los estados globales quedan en 8 `No cumple`, 77 `Pendiente` y 6 `Bloqueado`.

## Frontend — smoke test de Horarios

- Identificador: `FRONTEND-HORARIOS-SMOKE-d05ec16-20260908T132849-0500`.
- Resultado: 3 pruebas aprobadas en Chrome Headless 152.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T13-28-49-05-00/FRONTEND-HORARIOS-SMOKE/`.

Las pruebas existentes solo verifican que `ScheduleManagementComponent`, `RosterComponent` y `MyScheduleComponent` puedan instanciarse. Dos sustituyen la plantilla por contenido vacío. Por ello, no acreditan el modal, las vistas día/semana/mes ni la legibilidad de PDF/Excel; RF-12 y RF-16 continúan `Pendiente`.

## Primera ejecución aislada de RF-30 — 8 de septiembre de 2026

- Identificador: `RF30-d05ec16-20260908T111035-0500`.
- Comando: `mvn -Dtest=RF30Test test`.
- Base: commit `d05ec16df652d10a8f9380bd52b6e0ef815612d7` con árbol de trabajo modificado.
- Fuente de prueba: `RF30Test.java`, SHA-256 `10E7B0ABC24D4B7779B4ECC6349180B7D13FEC5D6651F0DBCA38A55BAEA07EE6`.
- Ambiente: Spring Boot 3.4.2, compilación Java 21, ejecución OpenJDK 22.0.2, Maven 3.9.9 y H2 2.3.232 en memoria.
- Resultado técnico: 10 pruebas, 8 aprobadas, 2 fallidas, 0 errores y 0 omitidas.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T11-10-35-05-00/RF30/`.

### Defectos confirmados

1. `CP-RF30-03` — No cumple: `pesoEnConsulta = 10.123` no produce una violación, pese a que CA-RF30-03 limita la precisión a dos decimales.
2. `CP-RF30-04` — No cumple: el servicio permite cerrar una consulta cuando `vacunacionAplicada` y `desparasitacionAplicada` están ausentes.

Los otros cuatro CP de RF-30 permanecen Pendiente porque aprobaron su comprobación backend inicial, pero todavía requieren el nivel API, E2E o de dos sesiones indicado en la matriz. Esta ejecución se conserva como antecedente y no fue reemplazada por la consolidada.

## Evidencia previa no homologada

Existe una medición real del Instrumento 1 relacionada con RNF-08. Se conserva como evidencia candidata y no cambia el estado de CP-RNF08-01 mientras no se resuelva la diferencia entre diez y 25 muestras y no se identifique el commit exacto desplegado.

## Registro de ejecuciones

Esta sección se completará de forma acumulativa. Cada registro debe incluir:

- fecha y hora;
- commit probado;
- ambiente;
- responsable;
- comando o procedimiento;
- cantidad de casos ejecutados;
- resultados;
- ruta de evidencias;
- incidencias.

No se reemplazarán resultados fallidos con una nueva ejecución. Cada intento conservará su identidad y la corrección se asociará con otro commit. Debido a que las pruebas nuevas todavía no están versionadas, debe repetirse el lote sobre un commit limpio antes de emplearlo como evidencia final de tesis.

## Producción controlada — consulta de Clientes y Mascotas

- Identificador: `CLIENTES-MASCOTAS-E2E-d05ec16-20260908T230313-0500`.
- Ambiente: frontend desplegado con la empresa ficticia autorizada `Veterinaria Vargas Vet` (`establishmentId = 3`).
- Cuenta: cuenta QA autorizada; la credencial se omitió de toda evidencia.
- Resultado: 2 casos aprobados, sin incidencias visibles ni errores de consola.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T23-03-13-05-00/CLIENTES-MASCOTAS-E2E/resultado.json`.

`CP-RF17-01` cambia a `Cumple`: el listado de Clientes mostró registros paginados y, al filtrar por nombre, devolvió únicamente el cliente esperado con contacto, documento y estado. `CP-RF20-01` cambia a `Cumple`: el listado de Mascotas mostró registros paginados y el filtro devolvió únicamente la mascota esperada, conservando especie, raza, propietario y estado. Estas ejecuciones completan el nivel E2E que faltaba a las pruebas de servicio previas. Los estados globales quedan en 14 `Cumple`, 12 `No cumple`, 59 `Pendiente` y 6 `Bloqueado`.

## Producción controlada — consulta y alcance de Agenda

- Identificador: `AGENDA-CONSULTA-E2E-d05ec16-20260908T230748-0500`.
- Resultado: 2 casos evaluados; 1 `Cumple` y 1 `No cumple`.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T23-07-48-05-00/AGENDA-CONSULTA-E2E/resultado.json`.

`CP-RF23-02` cambia a `Cumple`: en la prueba de alcance desplegada, `OWN` devolvió 0 citas para una cuenta sin asignaciones y `COMPANY` devolvió 10 en el mismo contexto. `CP-RF23-01` cambia a `No cumple`: las vistas Día, Semana y Mes funcionan y la vista tabular ofrece filtros por estado y veterinario, pero no existe el filtro por servicio exigido por `CA-RF23-01`. Los estados globales quedan en 15 `Cumple`, 13 `No cumple`, 57 `Pendiente` y 6 `Bloqueado`.

## Producción controlada — consulta de catálogos

- Identificador: `CATALOGOS-CONSULTA-E2E-d05ec16-20260908T231016-0500`.
- Resultado: 1 caso evaluado, `No cumple`.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T23-10-16-05-00/CATALOGOS-CONSULTA-E2E/resultado.json`.

`CP-RF34-01` cambia a `No cumple`. Las cinco pestañas abren; Especialidades muestra 1 registro, Tipos de empleado 1, Servicios 2, y Vacunas y Desparasitantes muestran estados vacíos. Sin embargo, no existen filtros visibles y el estado no aparece en todos los catálogos, aunque `CA-RF34-01` exige ambas condiciones para cada catálogo. Los estados globales quedan en 15 `Cumple`, 14 `No cumple`, 56 `Pendiente` y 6 `Bloqueado`.

## Producción controlada — consulta y aislamiento de Historia Clínica

- Identificador: `HISTORIA-CONSULTA-E2E-d05ec16-20260908T231219-0500`.
- Resultado: 2 casos evaluados; 1 `Cumple` y 1 `No cumple`.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T23-12-19-05-00/HISTORIA-CONSULTA-E2E/resultado.json`.

`CP-RF29-01` cambia a `Cumple`: la historia `HC-000001` presentó primero la consulta del 8 de septiembre de 2026 y después la del 27 de enero de 2024; Preventivos se encuentra separado de Consultas y Servicios, y contiene cartillas independientes de Vacunación y Desparasitación. `CP-RF29-02` cambia a `No cumple`: la evidencia de seguridad demuestra que una cuenta `OWN` sin historias asignadas recibió HTTP 200 al consultar una historia ajena, en vez de 403/404. Los estados globales quedan en 16 `Cumple`, 15 `No cumple`, 54 `Pendiente` y 6 `Bloqueado`.

## Producción controlada — inmutabilidad de consulta cerrada

- Identificador: `CONSULTA-CERRADA-E2E-d05ec16-20260908T231413-0500`.
- Resultado: 1 caso aprobado; no se realizaron mutaciones.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T23-14-13-05-00/CONSULTA-CERRADA-E2E/resultado.json`.

`CP-RF30-05` cambia a `Cumple`. La consulta 120 se presentó como `CERRADA` y `SOLO LECTURA V1`; el tipo, motivo, signos vitales y decisiones preventivas estaban deshabilitados. Junto con el rechazo backend ya acreditado, queda cubierto el criterio completo de interfaz y API. Los estados globales quedan en 17 `Cumple`, 15 `No cumple`, 53 `Pendiente` y 6 `Bloqueado`.

## Producción controlada — alta y activación temporal de cliente

- Identificador: `CLIENTE-ALTA-E2E-d05ec16-20260908T232836-0500`.
- Resultado: 1 caso aprobado.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T23-28-36-05-00/CLIENTE-ALTA-E2E/resultado.json`.

`CP-RF18-01` cambia a `Cumple`. Se creó un cliente ficticio con `ROLE_CLIENTE`; el sistema confirmó el registro, la consulta posterior lo devolvió como inactivo y el mensaje de activación llegó al buzón temporal. El enlace y su token no se conservaron. Se documenta una incidencia no bloqueante: la plantilla enviada dice `NUEVO EMPLEADO` y describe al cliente como parte del equipo. Los estados globales quedan en 18 `Cumple`, 15 `No cumple`, 52 `Pendiente` y 6 `Bloqueado`.

## Producción controlada — exportación de Mi Horario

- Identificador: `HORARIO-EXPORTACION-E2E-d05ec16-20260908T233653-0500`.
- Resultado: 1 caso evaluado, `No cumple`.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T23-36-53-05-00/HORARIO-EXPORTACION-E2E/resultado.json`.

`CP-RF16-01` cambia a `No cumple`. Con septiembre de 2026 seleccionado, el Excel exportó al colaborador como `Empleado`, mezcló semanas de junio y julio con las de septiembre, informó 324 horas y presentó caracteres dañados. La opción PDF invocó el diálogo de impresión mediante un iframe y no produjo un archivo descargable que pudiera verificarse. Los estados globales quedan en 18 `Cumple`, 16 `No cumple`, 51 `Pendiente` y 6 `Bloqueado`.

## Producción controlada — consulta administrativa de horarios

- Identificador: `HORARIO-CONSULTA-E2E-d05ec16-20260908T234045-0500`.
- Resultado: 1 caso aprobado; no se realizaron mutaciones.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T23-40-45-05-00/HORARIO-CONSULTA-E2E/resultado.json`.

`CP-RF12-01` cambia a `Cumple`. Se seleccionó un empleado con turnos, se comprobaron las vistas Día, Semana y Mes, y el detalle mostró fecha, horas, duración, empleado y cargo. Para el rol `ADMIN`, que dispone de modificación, apareció la acción `Editar turno`. Los estados globales quedan en 19 `Cumple`, 16 `No cumple`, 50 `Pendiente` y 6 `Bloqueado`.

## Producción controlada — rechazo de cliente duplicado

- Identificador: `CLIENTE-DUPLICADO-E2E-d05ec16-20260908T234336-0500`.
- Resultado: 1 caso aprobado; no se creó un registro adicional.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T23-43-36-05-00/CLIENTE-DUPLICADO-E2E/resultado.json`.

`CP-RF18-02` cambia a `Cumple`. El intento de registrar otro cliente con un correo QA existente fue rechazado con el mensaje `El email ya está registrado`; la búsqueda posterior no mostró un segundo cliente ni un registro parcial. Los estados globales quedan en 20 `Cumple`, 16 `No cumple`, 49 `Pendiente` y 6 `Bloqueado`.

## Producción controlada — actualización de cliente con relaciones

- Identificador: `CLIENTE-ACTUALIZACION-E2E-d05ec16-20260908T234629-0500`.
- Resultado: 1 caso evaluado, `No cumple`; no hubo actualización parcial.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T23-46-29-05-00/CLIENTE-ACTUALIZACION-E2E/resultado.json`.

`CP-RF19-01` cambia a `No cumple`. El cliente activo seleccionado conserva una mascota y la historia `HC-000030`, pero no fue posible actualizar únicamente su teléfono. El formulario lo cargó inicialmente sin rol y, al seleccionar el único `ROLE_CLIENTE` ofrecido, el backend respondió `Solo puede asignar roles de cliente activos de la misma empresa`. El teléfono original se conservó. Los estados globales quedan en 20 `Cumple`, 17 `No cumple`, 48 `Pendiente` y 6 `Bloqueado`.

## Producción controlada — desactivación de cliente con historial

- Identificador: `CLIENTE-DESACTIVACION-E2E-d05ec16-20260908T234843-0500`.
- Resultado: 1 caso evaluado, `No cumple`; el estado activo se restauró al finalizar.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T23-48-43-05-00/CLIENTE-DESACTIVACION-E2E/resultado.json`.

`CP-RF19-02` cambia a `No cumple`. La baja lógica funcionó: el cliente quedó inactivo, su mascota y la historia `HC-000030` permanecieron, y el cliente desapareció del selector para nuevas citas. Sin embargo, la operación no solicitó ni registró el motivo exigido por el criterio. Los estados globales quedan en 20 `Cumple`, 18 `No cumple`, 47 `Pendiente` y 6 `Bloqueado`.

## Producción controlada — alta de mascota y propietario inactivo

- Identificador: `MASCOTA-ALTA-E2E-d05ec16-20260908T235109-0500`.
- Resultado: 2 casos aprobados.
- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-08T23-51-09-05-00/MASCOTA-ALTA-E2E/resultado.json`.

`CP-RF21-02` cambia a `Cumple`: el servidor rechazó el alta asociada a un cliente inactivo y no creó relaciones incompletas. `CP-RF21-01` cambia a `Cumple`: con una propietaria activa, la mascota ficticia se guardó y apareció en la consulta posterior, aun dejando peso y fotografía vacíos. Se documenta que el frontend todavía muestra propietarios inactivos en el selector. Los estados globales quedan en 22 `Cumple`, 18 `No cumple`, 45 `Pendiente` y 6 `Bloqueado`.

## Producción E2E — ciclo de actualización y baja de mascota (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T00-10-32-05-00/MASCOTA-CICLO-E2E/resultado.json`.
- Entorno: frontend desplegado y empresa ficticia autorizada 3.
- Resultado: 2 casos evaluados; 1 `Cumple` y 1 permanece `Pendiente`.

En `CP-RF22-01` se modificó temporalmente el color de Aisha Cruz, se comprobó que conservó su propietaria y la historia `HC-000030`, y luego se restauró el valor original. El caso sigue `Pendiente` porque la interfaz no permitió demostrar directamente la conservación de todas las citas asociadas. `CP-RF22-02` cambia a `Cumple`: la baja exigió causal y detalle, mantuvo consultable `HC-000030` y excluyó a la mascota del selector de una nueva cita. Aisha fue reactivada al finalizar. Los estados globales quedan en 23 `Cumple`, 18 `No cumple`, 44 `Pendiente` y 6 `Bloqueado`.

## Producción E2E — actualización de contacto propio (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T00-13-05-05-00/PERFIL-CONTACTO-E2E/resultado.json`.
- Entorno: frontend desplegado y empresa ficticia autorizada 3.
- Resultado: 1 caso evaluado; 1 `Cumple`.

`CP-RF04-01` cambia a `Cumple`. Se actualizó únicamente el teléfono del usuario autenticado; la consulta posterior conservó los roles `ADMIN, ASISTENTE`, el estado `Activo` y la empresa seleccionada. El teléfono original fue restaurado al finalizar. Los estados globales quedan en 24 `Cumple`, 18 `No cumple`, 43 `Pendiente` y 6 `Bloqueado`.

## Producción E2E — actualización administrativa de empleado (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T00-17-09-05-00/EMPLEADO-ACTUALIZACION-E2E/resultado.json`.
- Entorno: frontend desplegado y empresa ficticia autorizada 3.
- Resultado: 1 caso evaluado; 1 `No cumple`.

`CP-RF11-01` cambia a `No cumple`. En el formulario administrativo se ingresó un teléfono válido distinto y se accionó `Actualizar Registro`; el formulario permaneció abierto, no informó validación ni error y el listado conservó el valor anterior. La prueba se repitió después de sacar el foco del campo y produjo el mismo resultado. No se modificaron roles, horarios ni otros datos. Los estados globales quedan en 24 `Cumple`, 19 `No cumple`, 42 `Pendiente` y 6 `Bloqueado`.

## Producción E2E — creación de horario de trabajo (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T00-20-46-05-00/HORARIO-ALTA-E2E/resultado.json`.
- Entorno: frontend desplegado y empresa ficticia autorizada 3.
- Resultado: 1 caso evaluado; 1 `No cumple`.

`CP-RF13-01` cambia a `No cumple`. Para un empleado activo se intentó registrar un turno futuro de 08:00 a 16:00 sobre el martes 15 de septiembre, mostrado como libre. Se repitió con un rango de un día y con el rango 15-16 de septiembre, seleccionando explícitamente el martes. `Confirmar Asignación` no persistió el turno ni comunicó validación o error. No fue necesaria limpieza porque no se creó ningún registro. Los estados globales quedan en 24 `Cumple`, 20 `No cumple`, 41 `Pendiente` y 6 `Bloqueado`.

## Producción E2E — ciclo integral de cita (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T00-27-20-05-00/CITA-CICLO-E2E/resultado.json`.
- Entorno: frontend desplegado y empresa ficticia autorizada 3.
- Resultado: 4 casos evaluados; 3 `Cumple` y 1 `No cumple`.

`CP-RF24-01`, `CP-RF25-01` y `CP-RF26-01` cambian a `Cumple`: la cita QA se creó una sola vez a las 10:00, su motivo se actualizó sin duplicación y luego se reprogramó a las 10:20 conservando paciente, propietaria, profesional y motivo. `CP-RF27-01` cambia a `No cumple`: la cancelación conservó el antecedente y liberó 10:20 —verificado en una nueva consulta de disponibilidad—, pero no solicitó ni registró el motivo requerido. La cita quedó `CANCELADA` y no se generó una segunda cita durante la comprobación. Los estados globales quedan en 27 `Cumple`, 21 `No cumple`, 37 `Pendiente` y 6 `Bloqueado`.

## Producción E2E — atención clínica, receta y preventivos (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T00-41-18-05-00/ATENCION-CLINICA-PREVENTIVOS-E2E/resultado.json`.
- Entorno: frontend desplegado y empresa ficticia autorizada 3.
- Resultado: 7 casos evaluados; 4 `Cumple` y 3 `No cumple`.

`CP-RF28-02` y `CP-RF28-01` cambian a `Cumple`: la primera atención creó únicamente `HC-000109` con la consulta 147 y la segunda atención reutilizó esa historia para la consulta 148; la vista final mostró una historia con dos consultas. `CP-RF30-02` cambia a `Cumple` porque el cierre fue rechazado primero por peso ausente y después por anamnesis ausente, manteniendo la consulta abierta. `CP-RF30-01` cambia a `Cumple`: con los datos obligatorios y decisiones preventivas presentes, la consulta quedó `CERRADA`, `SOLO LECTURA`, `V1` y la cita asociada pasó a `COMPLETADA`.

`CP-RF31-01` cambia a `No cumple`: aunque la receta se vinculó y apareció en la historia, las fechas 9-11 de septiembre ingresadas se mostraron como 8-10 de septiembre. `CP-RF33-01` y `CP-RF33-02` cambian a `No cumple`: marcar vacunación y desparasitación no presentó campos para vacuna/producto, fecha, intervalo o dosis y no produjo registros; la cartilla final continuó mostrando cero preventivos. Los estados globales quedan en 31 `Cumple`, 24 `No cumple`, 30 `Pendiente` y 6 `Bloqueado`.

## Producción E2E — altas de catálogos (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T09-43-52-05-00/CATALOGOS-ALTA-E2E/resultado.json`.
- Entorno: frontend desplegado y empresa ficticia autorizada 3.
- Resultado: 3 casos evaluados; 2 `Cumple` y 1 `No cumple`.

`CP-RF38-01` cambia a `Cumple`: `Tipo Laboral Pruebas Ers` fue creado con la opción de especialidades habilitada y apareció en los formularios de empleado y servicio. `CP-RF41-01` cambia a `Cumple`: `Servicio Pruebas Ers` se registró con precio, duración y disponibilidad, y apareció en el selector de una nueva cita. `CP-RF35-01` cambia a `No cumple`: la especialidad pudo crearse y actualizarse en el catálogo, pero el formulario de empleado no mostró el selector de especialidades al elegir el cargo Veterinario, por lo que no pudo asignarse. Esa especialidad temporal fue eliminada; el tipo y servicio QA permanecen identificados para limpieza posterior. Los estados globales quedan en 33 `Cumple`, 25 `No cumple`, 27 `Pendiente` y 6 `Bloqueado`.

## Producción E2E — comprensibilidad de mensajes (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T09-45-37-05-00/MENSAJES-E2E/resultado.json`.
- Resultado: 1 caso evaluado; 1 `No cumple`.

`CP-RNF06-01` cambia a `No cumple`. Las validaciones de peso/anamnesis y correo duplicado comunicaron la causa, pero la actualización administrativa de empleado y el alta de horario no persistieron y tampoco mostraron error, campo por corregir ni forma de continuar. Como el criterio exige mensajes comprensibles para cada error, las muestras silenciosas son suficientes para rechazarlo. Los estados globales quedan en 33 `Cumple`, 26 `No cumple`, 26 `Pendiente` y 6 `Bloqueado`.

## Homologación de instalación y rendimiento (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T10-15-00-05-00/RNF01-RNF08/resultado.json`.
- `CP-RNF01-01` cambia a `No cumple`: el Dockerfile y las variables de entorno proporcionan componentes técnicos, pero no sustituyen una guía. El README del backend conserva la plantilla con `TODO` y el frontend solo identifica el repositorio; por ello la instalación no puede repetirse sin pasos no documentados.
- `CP-RNF08-01` cambia a `Cumple`: se homologó la sesión `1b334fd5-c741-4589-9bcf-5fd7c495e0fc` ejecutada sobre producción controlada y el despliegue backend `7818730`. Contiene dos calentamientos y 25 muestras válidas por cada una de cuatro operaciones críticas. Las 100 muestras respondieron HTTP 200 y ninguna superó 2,000 ms.
- El recálculo independiente obtuvo: búsqueda de historias, mediana 140.654 ms y P95 308.701 ms; recuperación de historia, 88.380 ms y 401.031 ms; inicio de atención, 103.574 ms y 605.616 ms; cierre de atención, 156.669 ms y 504.476 ms. Los hashes SHA-256 del Excel, JSON del servidor y evidencia de red coinciden con el manifiesto.

Los estados globales quedan en 34 `Cumple`, 27 `No cumple`, 24 `Pendiente` y 6 `Bloqueado`.

## Producción E2E — accesibilidad básica (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T10-32-00-05-00/ACCESIBILIDAD-E2E/resultado.json`.
- Rutas inspeccionadas: `/citas/agenda`, `/clientes` y `/historias-clinicas`.
- `CP-RNF04-01` cambia a `No cumple`: el árbol de accesibilidad conserva títulos, navegación y varios campos con nombres adecuados, pero también expone controles principales únicamente mediante glifos. En Agenda fallan limpiar filtros, acciones por cita y notificación. En Historias fallan Buscar, Limpiar, notificación y los campos de rango de fechas aparecen sin nombre accesible asociado.

El criterio exige que los controles principales puedan identificarse y operarse. La ausencia de nombres comprensibles ya impide cumplirlo aunque otras etiquetas sí estén presentes. Los estados globales quedan en 34 `Cumple`, 28 `No cumple`, 23 `Pendiente` y 6 `Bloqueado`.

## Consolidación de integridad transaccional (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T10-45-00-05-00/INTEGRIDAD-CONSOLIDADA/resultado.json`.
- `CP-RNF10-01` cambia a `No cumple`: el criterio exige que ninguna operación inválida deje datos parciales o inconsistentes. En producción se aceptó con HTTP 201 un segundo empleado con el mismo documento y se creó una cuenta que luego tuvo que desactivarse durante la limpieza QA. Además, la prueba de pertenencia ya había demostrado que una receta podía asociarse a una consulta de otra empresa.

Las validaciones correctas de otros flujos no compensan estos contraejemplos porque el resultado esperado es absoluto. Los estados globales quedan en 34 `Cumple`, 29 `No cumple`, 22 `Pendiente` y 6 `Bloqueado`.

## Integración local — concurrencia de citas y consultas (2026-09-09)

- Evidencia de citas: `docs/testing/evidencias/d05ec16/2026-09-09T10-14-53-05-00/AGENDA-CONCURRENCIA/resultado.json`.
- Evidencia de consultas: `docs/testing/evidencias/d05ec16/2026-09-09T10-18-06-05-00/RF30-CONCURRENCIA/resultado.json`.
- `CP-RF24-02` cambia a `Cumple`: dos transacciones independientes iniciadas mediante una barrera intentaron registrar la misma franja. Una fue aceptada, la otra fue rechazada por cruce y la base terminó con exactamente una cita.
- `CP-RF24-03` cambia a `No cumple`: mascota, propietario y empleado inactivos se rechazaron sin persistencia, pero el servicio inactivo y no disponible fue aceptado y quedó vinculado a una nueva cita.
- `CP-RF26-02` cambia a `Cumple`: la reprogramación solicitada treinta minutos antes fue rechazada y no alteró fecha ni estado.
- `CP-RF30-06` cambia a `Cumple`: dos solicitudes usaron la misma versión; tras aceptar la primera, la segunda fue rechazada y no sobrescribió el dato persistido.

La suite de citas finalizó con 14 pruebas, 0 fallos y 0 errores. La suite RF-30 completa reveló además dos fallos que pertenecen a otros criterios; por ello `CP-RF30-06` se repitió aisladamente y finalizó con 1 prueba, 0 fallos y 0 errores. Los estados globales quedan en 37 `Cumple`, 30 `No cumple`, 18 `Pendiente` y 6 `Bloqueado`.

## Integración local — pagos y controles complementarios (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T10-20-31-05-00/PAGOS-REVALIDACION/resultado.json`.
- El lote ejecutó 14 pruebas con 0 fallos y 0 errores.
- `CP-RF44-01` cambia a `Cumple`: se verificaron en una base H2 la compra, el movimiento de ingreso, el monto pagado y el estado resultante de la cita para medios admitidos.
- `CP-RF44-02` cambia a `Cumple`: cero, monto superior al saldo y efectivo insuficiente fueron rechazados sin compra, movimiento de caja ni alteración financiera.
- `CP-RF33-03`, `CP-RF33-04` y `CP-RNF02-01` permanecen `Pendiente`: sus pruebas unitarias o de contrato pasaron, pero todavía no cubren respectivamente el proceso programado persistente ni la matriz negativa con seguridad de método activa.

Los estados globales quedan en 39 `Cumple`, 30 `No cumple`, 16 `Pendiente` y 6 `Bloqueado`.

## Revisión de seguridad — expiración por inactividad (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T10-24-00-05-00/RNF03-INACTIVIDAD/resultado.json`.
- `CP-RNF03-01` cambia a `No cumple`. El access token vence a los 1,800 segundos y las cookies están protegidas, pero eso no implementa por sí mismo inactividad. El interceptor renueva automáticamente ante un 401 y el backend admite la rotación del refresh token hasta siete días, limitado por una sesión absoluta de 24 horas, sin registrar ni validar la última actividad.

Por tanto, esperar treinta minutos no es necesario para decidir este caso: el flujo implementado permite que la siguiente solicitud renueve la sesión. Los estados globales quedan en 39 `Cumple`, 31 `No cumple`, 15 `Pendiente` y 6 `Bloqueado`.

## Producción E2E — actualización de mascota y conservación de relaciones (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T14-45-50-05-00/MASCOTA-RELACIONES-E2E/resultado.json`.
- `CP-RF22-01` cambia a `Cumple`. Se modificó de forma temporal el color de `QA Mascota Trazabilidad` y la ficha persistió el cambio. Después se comprobó que conservaba su propietaria, la historia `HC-000109` con dos consultas y tres citas asociadas —dos completadas y una cancelada—.
- El campo temporal fue restaurado al valor original y verificado nuevamente en el formulario.

Los estados globales quedan en 40 `Cumple`, 31 `No cumple`, 14 `Pendiente` y 6 `Bloqueado`.

## Producción E2E y unidad — actualización y protección de catálogos (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T15-13-59-05-00/CATALOGOS-ACTUALIZACION-PROTECCION/resultado.json`.
- La suite `CatalogoProteccionServiceTest` terminó con 4 pruebas, 0 fallos y 0 errores.
- `CP-RF36-01` cambia a `Cumple`: la especialidad se actualizó y restauró en el despliegue; la prueba de servicio confirmó que la modificación conserva la colección de empleados asignados.
- `CP-RF39-01` cambia a `Cumple`: el tipo laboral se actualizó, se ocultó del alta de personal durante su inactividad y se restauró.
- `CP-RF40-01` cambia a `Cumple`: con empleados asociados se rechazó la eliminación antes de invocar el repositorio.
- `CP-RF42-01` cambia a `No cumple`: la UI sí ocultó el servicio inactivo, pero la integración de citas ya demostró que el backend permite asociarlo directamente; el servidor debe ser la autoridad.
- `CP-RF43-01` cambia a `Cumple`: un servicio con citas se rechaza antes de guardar su baja lógica.
- `CP-RF37-01` cambia a `Cumple`: la integración H2 confirmó que una especialidad referenciada no puede borrarse y la transacción falla sin romper la asignación. La protección depende hoy de persistencia y devuelve una excepción técnica; conviene añadir una validación de dominio y un mensaje comprensible.

Los estados globales quedan en 45 `Cumple`, 32 `No cumple`, 8 `Pendiente` y 6 `Bloqueado`.

## Integración local — rechazo de horarios cruzados (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T15-29-13-05-00/HORARIO-CRUCE-INTEGRACION/resultado.json`.
- `CP-RF13-02` cambia a `Cumple`. La consulta real H2 detectó el solapamiento y conservó un único turno; la prueba aislada de servicio confirmó que el segundo registro se rechaza antes de persistir.
- El lote aislado terminó con 2 pruebas, 0 fallos y 0 errores. La ejecución conjunta inicial mostró además el fallo ya conocido de RF-14, que no se atribuyó a este criterio.

Los estados globales quedan en 46 `Cumple`, 32 `No cumple`, 7 `Pendiente` y 6 `Bloqueado`.

## Unidad de servicio — protección de roles técnicos (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T15-32-59-05-00/ROLES-TECNICOS/resultado.json`.
- `CP-RF06-02` cambia a `Cumple`. El rol protegido rechazó su eliminación sin invocar `delete`; al intentar modificar la identidad del administrador de plataforma conservó nombre, ámbito y propósito.
- El lote aislado terminó con 2 pruebas, 0 fallos y 0 errores.

Los estados globales quedan en 47 `Cumple`, 32 `No cumple`, 6 `Pendiente` y 6 `Bloqueado`.

## Producción E2E — información institucional y logotipo (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T15-35-00-05-00/EMPRESA-ACTUALIZACION-MARCA/resultado.json`.
- `CP-RF05-01` cambia a `Cumple`: la actualización válida persistió tras recargar y el logotipo del layout se abastece desde `companyLogoUrl` de la sesión, sin exigir permiso de lectura sobre `/admin/company`.
- Incidencia de limpieza: la descripción originalmente vacía no pudo restaurarse desde la vista de superadministración; permanece un texto QA identificado. Esto no invalida la actualización, pero sí requiere corrección/limpieza al terminar la fase de medición.

Los estados globales quedan en 48 `Cumple`, 32 `No cumple`, 5 `Pendiente` y 6 `Bloqueado`.

## Integración local — proceso y deduplicación de recordatorios (2026-09-09)

- Evidencia: `docs/testing/evidencias/d05ec16/2026-09-09T15-45-00-05-00/RECORDATORIOS-PROCESO-PERSISTENCIA/resultado.json`.
- `CP-RF33-03` cambia a `Cumple`: un control persistido y elegible fue recuperado por el proceso, produjo un único envío consolidado simulado y dejó el recordatorio persistido. La segunda ejecución no volvió a enviarlo. También se verificó la configuración programada parametrizable y el WhatsApp manual ya cubierto por la prueba de cartilla.
- `CP-RF33-04` cambia a `Cumple`: además de las reglas unitarias de intervalo y clave exacta, la base de datos rechazó físicamente un duplicado y la repetición normal del proceso conservó un único registro.
- La ejecución final reunió 6 pruebas, 0 fallos y 0 errores: 4 de reglas de servicio y 2 de persistencia. El proveedor SMTP se sustituyó por un doble controlado; la entrega externa no forma parte de esta conclusión funcional.

Los estados globales quedan en 50 `Cumple`, 32 `No cumple`, 3 `Pendiente` y 6 `Bloqueado`.

## Producción E2E — archivos clínicos y cierre de pendientes (2026-09-09)

- Evidencia de archivos: `docs/testing/evidencias/d05ec16/2026-09-09T20-53-45-05-00/ARCHIVOS-CLINICOS-PRODUCCION-E2E/resultado.json`.
- `CP-RF32-01` cambia a `No cumple`: el PDF de 603 B fue aceptado, quedó vinculado a la consulta 149 y persistió como único adjunto, pero el visor mostró contenido bloqueado y ninguno de los dos botones de descarga produjo una descarga verificable. El almacenamiento no basta porque el criterio también exige consulta posterior.
- `CP-RF32-02` cambia a `Cumple`: un `.exe` fue rechazado por el servidor y el contador permaneció en un adjunto, sin un segundo registro visible. El texto del error menciona “hemogramas” y debe corregirse sin cambiar la conclusión de seguridad.
- Evidencia de seguridad: `docs/testing/evidencias/d05ec16/2026-09-09T20-53-45-05-00/RNF02-CONCLUSION-SEGURIDAD/resultado.json`.
- `CP-RNF02-01` cambia a `No cumple`: el criterio universal del 100 % de rechazo queda invalidado por los contraejemplos desplegados ya documentados. Una cuenta `OWN` sin asignaciones recibió HTTP 200 y diez clientes, diez historias y el detalle de una historia ajena; también se obtuvo el mismo conjunto al consultar empresa propia y ajena. No hace falta fabricar más operaciones para decidir que el estado actual falla, aunque la matriz negativa CRUD completa será obligatoria para certificar la corrección futura.

Con esta consolidación ya no quedan casos en estado `Pendiente`. Los estados globales quedan en 51 `Cumple`, 34 `No cumple`, 0 `Pendiente` y 6 `Bloqueado`.

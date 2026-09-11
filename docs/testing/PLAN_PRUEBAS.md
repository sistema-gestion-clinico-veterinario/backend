# Plan técnico de pruebas del ERS 1.2

## 1. Objetivo

Convertir los 91 casos de prueba definidos en el ERS 1.2 en verificaciones reproducibles y trazables. Ningún caso se declarará Cumple sin una ejecución identificada por fecha, commit, ambiente, datos controlados y evidencia recuperable.

La línea de trazabilidad obligatoria es:

`RF o RNF -> criterio de aceptación -> caso de prueba -> prueba técnica -> evidencia -> resultado`

## 2. Línea base documental

- Fuente normativa: `ERS_Vargas_Vet_Validacion_Trazabilidad_v1.2 (1).docx`.
- SHA-256 de la fuente: `C1D44E2875CFB05027FC880C7031D70E1EF18470171BEACFC7EDF6F0C6892DF8`.
- Requisitos funcionales: 45.
- Requisitos no funcionales: 13.
- Casos de prueba: 91.
- Estados permitidos: Pendiente, Cumple, No cumple y Bloqueado.
- Regla de cierre: un requisito solo cumple cuando todos sus criterios obligatorios y casos aplicables cuentan con evidencia válida.

El nombre `(1)` pertenece al archivo descargado y no constituye una versión adicional. Para una ejecución formal se conservará también el hash SHA-256 del ERS.

## 3. Arquitectura verificada

### 3.1 Frontend

- Angular 19.2 y TypeScript 5.7.
- PrimeNG 19, Tailwind CSS 3 y RxJS 7.
- Pruebas unitarias con Jasmine y Karma.
- Existen 61 archivos `*.spec.ts`.
- Playwright está declarado como dependencia y existen comandos `test:e2e`, pero en la rama inspeccionada no existe todavía una configuración ni una suite E2E versionada.

### 3.2 Backend

- Java 21 y Spring Boot 3.4.2.
- API REST bajo `/api/v1`.
- Spring Security, JWT firmado, cookies HttpOnly y rotación de refresh token.
- JPA/Hibernate con PostgreSQL; el esquema se valida mediante `ddl-auto=validate`.
- Supabase se utiliza para persistencia PostgreSQL y almacenamiento de archivos según la configuración del proyecto.
- JUnit 5, Spring Boot Test, Spring Security Test y H2 para pruebas.
- Existen 38 clases de prueba Java.

### 3.3 Autorización y aislamiento

- La API aplica permisos dinámicos por vista y acción mediante `AccesoValidator`.
- Las acciones técnicas son LEER, ESCRIBIR, MODIFICAR y ELIMINAR; el ERS usa los nombres de negocio LEER, CREAR, EDITAR y ELIMINAR.
- El alcance de datos se modela como OWN o COMPANY.
- Las pruebas deben comprobar la respuesta del servidor. Ocultar una opción en Angular no constituye evidencia de autorización.

### 3.4 Persistencia y transacciones

- Los servicios críticos de citas, consultas, pagos y cartilla utilizan transacciones.
- Las entidades principales incluyen usuario, empresa, rol, empleado, apoderado, mascota, cita, historia clínica, consulta, prescripción, archivos y controles preventivos.
- La existencia de una anotación transaccional no demuestra atomicidad por sí sola; los escenarios negativos deben comprobar el estado de la base de datos después del error.

## 4. Estrategia de pruebas

| Nivel | Uso | Herramienta preferida |
|---|---|---|
| Unidad | Validadores, cálculos, reglas aisladas y transformaciones | JUnit 5 o Jasmine |
| Integración | Persistencia, transacciones, concurrencia y reglas entre servicios | Spring Boot Test con H2 o PostgreSQL de prueba cuando H2 no reproduzca el comportamiento |
| API y seguridad | Autenticación, permisos, alcance y validación del servidor | MockMvc o cliente HTTP contra ambiente controlado |
| E2E | Flujos completos y comportamiento visible desde la interfaz | Playwright, aprovechando la dependencia existente |
| Rendimiento | Operaciones críticas definidas en RNF-08 | Instrumentación del servidor y ejecutor controlado |
| Manual controlada | Usabilidad, compatibilidad, accesibilidad y procedimientos operativos | Protocolo documentado con evidencia real |
| Observación prolongada | Disponibilidad, respaldo y sostenibilidad | Registros acumulados del proveedor y ejercicios controlados |

No se introducirá otra librería E2E mientras Playwright cubra el flujo requerido. Playwright se justifica porque ya está declarado, permite aislar contextos de navegador, controlar datos y conservar trazas, capturas y respuestas de red.

## 5. Datos y ambientes

- Se usarán empresas, usuarios, mascotas, citas e historias exclusivamente ficticias.
- Cada ejecución creará o restaurará su propio conjunto de datos.
- Los identificadores variables se obtendrán durante la preparación y no se codificarán como IDs permanentes.
- Las contraseñas, cookies y tokens no se escribirán en evidencias ni archivos versionados.
- Las pruebas destructivas se ejecutarán únicamente en un ambiente de prueba autorizado.
- Las pruebas de tenant utilizarán como mínimo dos empresas y dos apoderados con datos diferenciables.
- Las pruebas de concurrencia utilizarán dos sesiones o dos transacciones sobre la misma versión de la consulta.

## 6. Convención de implementación

### Backend

```text
src/test/java/veterinaria/vargasvet/ers/
  auth/
  rbac/
  personal/
  clientes/
  mascotas/
  citas/
  historias/
  preventivos/
  pagos/
  nonfunctional/
```

Las pruebas nuevas conservarán el código CP en `@DisplayName`, por ejemplo:

```java
@DisplayName("[CP-RF30-05] Rechaza la edición de una consulta cerrada")
```

### Frontend

```text
e2e/ers/
  auth/
  agenda/
  historias/
  preventivos/
  rbac/
```

La configuración debe iniciar un backend de prueba o apuntar a un ambiente expresamente autorizado. No se ejecutarán pruebas mutables contra producción por defecto.

## 7. Evidencias

Cada ejecución creará una carpeta:

```text
docs/testing/evidencias/<commit>/<fecha-hora>/<caso>/
```

Contenido mínimo según el tipo de prueba:

- `resultado.json`: caso, CA, fecha, commit, ambiente, estado y resultado obtenido.
- `stdout.log`: salida depurada sin secretos.
- Respuesta HTTP sanitizada para pruebas API.
- Estado anterior y posterior para persistencia o atomicidad.
- Captura o traza Playwright solo cuando sea necesaria para demostrar comportamiento visible.
- Hash SHA-256 de los artefactos finales.

Una captura aislada no reemplaza una aserción automatizada. Un log sin código de caso, fecha y versión no se considera evidencia suficiente.

## 8. Orden de implementación

### Lote 1 Núcleo directo de la tesis

RF-17, RF-18, RF-20, RF-21, RF-28, RF-29, RF-30, RF-31 y RF-33.

Prioridades internas:

1. CP-RF30-01 a CP-RF30-06.
2. CP-RF28-01 y CP-RF28-02.
3. CP-RF29-01 y CP-RF29-02.
4. CP-RF33-01 a CP-RF33-04.
5. Clientes, mascotas y recetas.

### Lote 2 Seguridad e integridad

RF-01 a RF-04, RF-06 a RF-08, RNF-02, RNF-03 y RNF-10.

### Lote 3 Agenda y operación

RF-09 a RF-16 y RF-23 a RF-27.

### Lote 4 Administración y pagos

RF-05, RF-19, RF-22, RF-34 a RF-45.

### Lote 5 No funcionales restantes

RNF-01, RNF-04 a RNF-09 y RNF-11 a RNF-13.

## 9. Fases de trabajo

### Fase 1 Análisis y matriz

- Extraer requisitos, CA y CP del ERS 1.2.
- Inventariar controladores, servicios, validaciones, entidades y pruebas existentes.
- Identificar contradicciones y cobertura aparente.
- Mantener todos los resultados como Pendiente.

### Fase 2 Diseño técnico

- Asignar cada CP al nivel correcto.
- Proponer archivos y pruebas sin cambiar resultados esperados.
- Definir datos, precondiciones y evidencia.
- Revisar las brechas antes de tocar código funcional.

### Fase 3 Implementación

- Crear fixtures y pruebas automatizadas por lotes.
- No corregir defectos funcionales dentro del mismo cambio de pruebas.
- Registrar por separado cualquier propuesta de corrección.

### Fase 4 Ejecución

- Identificar commit, ambiente y responsable.
- Ejecutar primero unitarias e integración, después API y E2E.
- Ejecutar manuales y RNF solo con las condiciones exigidas.
- Conservar fallos y salidas originales.

### Fase 5 Informe

- Consolidar estados por CP, CA y requisito.
- Registrar defectos y bloqueos.
- Actualizar la matriz sin borrar ejecuciones anteriores.
- Emitir una recomendación de liberación del núcleo.

## 10. Criterios de salida

La validación del núcleo solo puede cerrarse cuando:

- todos los requisitos M con vínculo Directa tienen sus CA cubiertos;
- no existen pruebas críticas fallidas sin incidencia registrada;
- las pruebas de autorización confirman el rechazo en servidor;
- no se observan cambios parciales en operaciones inválidas;
- toda evidencia puede vincularse con un commit y una ejecución real.

Los RNF de disponibilidad mensual, usabilidad con usuarios, respaldo y recuperación o costo anual permanecerán Bloqueados o Pendientes hasta contar con el periodo, participantes, acceso al proveedor o ejercicio de restauración correspondiente.

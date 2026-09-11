# Brechas entre el ERS 1.2 y la implementación

## Estado del análisis

Este documento registra hallazgos obtenidos por inspección del ERS y del código. No sustituye la ejecución de los casos de prueba. Por ello, las observaciones se clasifican como brechas o riesgos y no como casos No cumple hasta que una prueba reproducible confirme el comportamiento.

## Brechas documentales y de trazabilidad

### BQ-001 Nombre físico de la línea base

- ERS: versión interna 1.2.
- Situación: el archivo se denomina `ERS_Vargas_Vet_Validacion_Trazabilidad_v1.2 (1).docx`.
- Riesgo: confundir el sufijo de descarga con una revisión documental.
- Acción: conservar hash SHA-256 y copiar la línea base con un nombre canónico antes de la ejecución formal.

### BQ-002 Matriz sin relación explícita CP a CA

- ERS: la sección 4 codifica criterios CA y la sección 6 indica que cada CP debe registrar los CA que verifica.
- Situación: la matriz del ERS relaciona requisito, CP y evidencia, pero no contiene una columna CA.
- Acción: la matriz técnica añade `Criterios de aceptación`. La asociación propuesta debe revisarse antes de ejecutar.

### BQ-003 Acción ESCRIBIR frente a CREAR

- ERS: usa LEER, CREAR, EDITAR y ELIMINAR.
- Código: usa LEER, ESCRIBIR, MODIFICAR y ELIMINAR.
- Riesgo: reportes o pruebas pueden considerar acciones distintas aunque sean equivalentes funcionalmente.
- Acción: documentar oficialmente `CREAR = ESCRIBIR` y `EDITAR = MODIFICAR`, sin renombrar permisos durante la validación.

## Brechas funcionales prioritarias

### BQ-004 Decisiones preventivas no obligatorias en la API

- Requisito: RF-30, CA-RF30-02 y CP-RF30-04.
- Esperado: vacunación y desparasitación deben contener una decisión explícita antes del cierre.
- Código observado: `ConsultaRequest` declara ambos campos como `Boolean` sin `@NotNull`; `validarCamposObligatorios` no comprueba su ausencia.
- Riesgo: una llamada directa podría cerrar una consulta sin ambas decisiones, aunque la interfaz las inicialice.
- Prueba requerida: enviar `null` u omitir cada propiedad mediante la API y comprobar rechazo y ausencia de cambios parciales.
- Ejecución inicial: confirmada en `RF30-d05ec16-20260908T111035-0500`; el cierre no lanzó la excepción esperada cuando ambas decisiones estaban ausentes.
- Causa probable: faltan restricciones `@NotNull` en el DTO y una comprobación defensiva en `validarCamposObligatorios`.
- Cambio propuesto: exigir ambas decisiones en la frontera HTTP y volver a verificarlas en el servicio antes de cualquier cambio de estado; añadir después una prueba API que omita cada propiedad por separado.

### BQ-005 Precisión del peso no aplicada en servidor

- Requisito: RF-30 y CA-RF30-03.
- Esperado: peso entre 0.01 y 120 kg con hasta dos decimales.
- Código observado: existen `@DecimalMin` y `@DecimalMax`, pero no una restricción equivalente a `@Digits(fraction = 2)`.
- Riesgo: la API podría aceptar valores como `10.123`.
- Prueba requerida: límites 0.01 y 120.00, valores 0, 120.01 y más de dos decimales.
- Ejecución inicial: confirmada en `RF30-d05ec16-20260908T111035-0500`; los límites y valores fuera de rango se validaron, pero `10.123` fue aceptado sin violación.
- Causa probable: el DTO solo controla mínimo y máximo, mientras la columna utiliza punto flotante y tampoco restringe la escala.
- Cambio propuesto: incorporar una validación de escala de dos decimales en el DTO; evaluar `BigDecimal` y una columna `numeric` si el peso debe conservar precisión decimal exacta. La decisión debe probar compatibilidad antes de migrar datos.

### BQ-006 Caso de rangos más amplio que su CA funcional

- Requisito: CP-RF30-03.
- Situación: el caso incluye peso, temperatura y frecuencias; CA-RF30-03 solo expresa el rango y precisión del peso.
- Complemento: las reglas de validación y RNF-10 sí abarcan validaciones clínicas del servidor.
- Acción: trazar el caso también a las reglas de validación correspondientes y a CA-RNF10-01/03, o dividirlo sin modificar el resultado esperado.

### BQ-007 Concurrencia no aparece como CA específico de RF-30

- Requisito: CP-RF30-06.
- Situación: el caso exige control de versión, pero los CA de RF-30 no contienen un criterio explícito de concurrencia.
- Complemento: CA-RNF10-01 sí menciona control de versión.
- Acción: mantener CP-RF30-06 y trazarlo adicionalmente a RNF-10. No inventar un nuevo CA de RF-30 sin revisión documental.

### BQ-008 Duración de receta no obligatoria en servidor

- Requisito: RF-31, CA-RF31-02 y CP-RF31-02.
- Esperado: medicamento, dosis, frecuencia, duración e indicaciones requeridas.
- Código observado: `duracionDias` tiene mínimo y máximo pero no `@NotNull`; `instrucciones` tampoco es obligatoria.
- Riesgo: una receta podría persistirse sin duración o indicaciones.
- Ejecución inicial: confirmada en `LOTE1-d05ec16-20260908T114445-0500`; no aparecen violaciones al omitir `duracionDias` ni `instrucciones`.
- Causa confirmada: `duracionDias` carece de `@NotNull` e `instrucciones` carece de `@NotBlank`; sus otras restricciones no rechazan valores nulos.
- Cambio propuesto: incorporar obligatoriedad declarativa en el DTO y una prueba API por cada campo, verificando además que no exista persistencia parcial.

### BQ-009 Riesgo de acceso transversal en prescripciones

- Requisitos: RF-29, RF-31 y RNF-02.
- Código observado: `PrescripcionServiceImpl` recupera consulta o prescripción por ID y no muestra una validación explícita de empresa o apoderado antes de crear, listar, actualizar o eliminar. El permiso general de vista no prueba aislamiento del objeto.
- Riesgo: un usuario con permiso sobre recetas podría intentar utilizar un ID perteneciente a otra empresa.
- Ejecución inicial: confirmada parcialmente como `CP-RNF02-02` en `LOTE1-d05ec16-20260908T114445-0500`; la creación de una receta con una consulta de otra empresa no produjo el rechazo esperado. No se atribuye a `CP-RF29-02`, cuya comprobación específica es la lectura de la historia.
- Causa probable: la creación recupera la consulta por identificador global sin imponer empresa o apoderado en la consulta de repositorio ni validar la pertenencia del objeto.
- Cambio propuesto: aplicar autorización a nivel de objeto en crear, listar, actualizar y eliminar, usando búsquedas acotadas por empresa o propietario; completar pruebas API con dos empresas y comprobar ausencia de cambios.

### BQ-010 Cobertura insuficiente del detalle cronológico en frontend

- Requisito: RF-29 y CP-RF29-01.
- Código observado: el backend ordena consultas por fecha descendente, pero la prueba Angular del detalle es principalmente de creación del componente.
- Riesgo: la presentación puede perder orden, secciones o cartilla sin ser detectada.
- Prueba requerida: prueba de integración del mapeo y E2E con consultas, diagnósticos, recetas, archivos y preventivos en fechas distintas.

### BQ-011 Alcance de agenda con CA incompleto

- Requisito: CP-RF23-02.
- Situación: el caso exige respetar OWN o COMPANY, mientras CA-RF23-01 describe vistas y filtros, no el alcance.
- Complemento: RF-08 contiene el CA de alcance.
- Acción: trazar CP-RF23-02 a CA-RF08-02/03 y RNF-02 además de RF-23.

### BQ-012 Organización y orden del menú

- Requisito: CP-RF08-03.
- Situación: CA-RF08-02 contempla GROUPED o FLAT, pero no expresa de forma explícita que el orden configurado de módulos y vistas deba persistir y reflejarse.
- Acción: revisar si se incorpora un CA específico en una futura versión. Mientras tanto, probar la conducta sin declarar que el CA cubre una condición no escrita.

## Brechas de automatización

### BQ-013 Infraestructura E2E incompleta

- Situación: Playwright figura en `package.json` y existen scripts, pero no se encontró `playwright.config.ts` ni una suite `e2e` en la rama inspeccionada.
- Impacto: no es posible ejecutar aún de forma reproducible los flujos completos definidos en el ERS.
- Acción: crear la configuración en Fase 3 y conectarla con fixtures de prueba, sin usar producción por defecto.

### BQ-014 Varias pruebas Angular son solo smoke tests

- Ejemplos: clientes, lista de mascotas y detalle de historia solo comprueban principalmente que el componente se crea.
- Impacto: existencia de un archivo `spec.ts` no equivale a cobertura del caso ERS.
- Acción: registrar cobertura únicamente cuando las aserciones demuestren el resultado esperado del CP.

### BQ-015 Cobertura parcial de preventivos

- Ejecución inicial: `LOTE1-d05ec16-20260908T114445-0500` aprobó pruebas unitarias de vacunación, desparasitación separada, próximo control, correo consolidado, preparación manual de WhatsApp e identidad de deduplicación.
- Cobertura restante: persistencia real, frontera API, ejecución programada, concurrencia y flujo E2E.
- Acción: conservar los cuatro CP como `Pendiente` hasta completar esos niveles; no inferir cumplimiento integral a partir de mocks unitarios.

### BQ-016 Cobertura de permisos todavía no exhaustiva

- Código existente: `AccesoValidatorTest` comprueba decisiones importantes del RBAC.
- ERS: RNF-02 exige rechazar el 100 % de las acciones no asignadas en lectura, creación, edición y eliminación.
- Impacto: pruebas unitarias del validador no demuestran que todos los endpoints estén protegidos correctamente.
- Acción: generar una matriz endpoint-vista-acción y ejecutar pruebas API negativas por cada combinación relevante.

## Brechas no funcionales

### BQ-017 Evidencia de RNF-08 pendiente de homologación

- Existe una ejecución real anterior con dos calentamientos y 25 muestras por operación.
- El ERS 1.2 establece diez muestras válidas por operación.
- La etiqueta de versión usada en esa ejecución fue descriptiva y no el hash exacto del commit desplegado.
- Acción: conservarla como evidencia candidata; antes de marcar CP-RNF08-01 como Cumple, homologar formalmente el tamaño muestral y vincular el despliegue con un commit verificable, o repetir exactamente el protocolo 1.2.

### BQ-018 RNF prolongados no demostrables con una ejecución aislada

- RNF-05 requiere participantes reales e inducción.
- RNF-07 requiere versiones concretas de tres navegadores.
- RNF-09 requiere observación de disponibilidad dentro del periodo definido.
- RNF-11 requiere acceso al respaldo y un ejercicio de restauración.
- RNF-12 requiere comprobantes de costos.
- RNF-13 requiere el equipo real de la veterinaria.
- Acción: mantener Pendiente o Bloqueado hasta que exista la precondición y no generar evidencia simulada.

### BQ-019 Maven Wrapper no inicia en el entorno Windows inspeccionado

- Situación observada: `mvnw.cmd -Dtest=RF30Test test` terminó antes de compilar con `No se puede indizar en una matriz nula` y `Cannot start maven from wrapper`.
- Impacto: el comando reproducible incluido en el repositorio no puede utilizarse actualmente en este entorno.
- Contención aplicada: la ejecución RF-30 utilizó Maven 3.9.9 instalado, manteniendo el `pom.xml` del proyecto.
- Acción: revisar por separado los scripts `.mvn/wrapper` y `mvnw.cmd`; no asociar este fallo de infraestructura con el resultado de los casos RF-30.

### BQ-020 Ciclo de vida contradictorio de la historia clínica

- Requisito: RF-28 y CP-RF28-02.
- ERS: la historia clínica debe crearse automáticamente al iniciar la primera atención cuando aún no existe.
- Código observado: `MascotaServiceImpl.registerMascota` crea y persiste la historia inmediatamente después de registrar la mascota.
- Ejecución inicial: `CitaServiceIntegrationTest` demuestra que el inicio de atención crea una historia cuando falta, pero ese fixture no atraviesa el registro real de mascota.
- Riesgo: las pruebas aisladas pueden aprobar dos comportamientos incompatibles y el sistema real nunca ejercer la condición prevista por RF-28.
- Acción: decidir con el responsable funcional si la historia nace con la mascota o con la primera atención; después actualizar ERS, implementación y pruebas como una sola decisión trazable.

## Decisión antes de corregir código

La primera ejecución del Lote 1 confirmó BQ-004, BQ-005, BQ-008 y BQ-009. Antes de corregir, debe resolverse BQ-020. Luego cada corrección funcional deberá realizarse en un cambio separado con requisito, comportamiento actual, causa, prueba de regresión y propuesta documentados.

# Inventario técnico para validación del ERS 1.2

## Propósito y alcance

Este inventario identifica los componentes que deberán participar en las pruebas del ERS. Es producto de inspección estática; no acredita por sí mismo que un requisito cumpla.

Todas las rutas indicadas se publican bajo el contexto `/api/v1`.

## Componentes por dominio

| Dominio ERS | API principal | Servicios o controles relevantes | Cobertura existente observada | Evaluación inicial |
|---|---|---|---|---|
| Autenticación y credenciales | `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/change-password`, `/auth/forgot-password`, `/auth/reset-password`, `/auth/email-change/*` | JWT, cookies, rotación de refresh, cambio y recuperación de credenciales | Pruebas de contraseña, cambio de correo, sesión y filtros JWT | Reutilizable, pero debe vincularse caso por caso |
| Roles, permisos y menú | `/admin/roles`, `/admin/roles/{id}/views`, `/admin/roles/{id}/menu-configuration`, `/admin/roles/{id}/menu-order`, `/me/navigation` | `AccesoValidator`, permisos dinámicos y alcances OWN/COMPANY | `AccesoValidatorTest`, pruebas de roles y construcción de menú | Falta matriz negativa completa por endpoint y acción |
| Personal y horarios | `/admin/employees`, `/admin/employees/{id}/schedule*`, `/profile/schedule` | altas, edición, estado, horarios y disponibilidad | Pruebas parciales de reglas y horario | Requiere integración y E2E por RF |
| Clientes | `/clients/guardians` | registro, consulta, modificación, estado y eliminación | Sin prueba backend específica suficiente identificada | Cobertura prioritaria pendiente |
| Mascotas | `/pets` | registro, asociación con apoderado, consulta y estado | Prueba de aislamiento tenant | Falta cubrir ciclo funcional y autorización por objeto |
| Agenda y citas | `/appointments`, `/appointments/availability`, `/appointments/{id}/reschedule`, `/appointments/{id}/start`, `/appointments/{id}/finish-service` | disponibilidad, solapamiento, reprogramación, cancelación e inicio de consulta | `CitaServiceIntegrationTest` | Buena base, no equivale aún a cobertura ERS formal |
| Historia clínica y consulta | `/medical-records`, `/medical-records/pet/{petId}`, `/consultations/{id}`, `/consultations/{id}/close` | detalle cronológico, edición clínica, cierre, bloqueo y concurrencia | `ConsultaServiceIntegrationTest` y `ConsultaServiceUnitTest` | Prioridad máxima por RF-29 y RF-30 |
| Prescripciones | `/prescriptions`, `/prescriptions/consultation/{consultationId}`, `/prescriptions/{id}` | crear, listar, actualizar y eliminar recetas | No se identificó prueba específica suficiente | Validar campos obligatorios y aislamiento entre empresas |
| Archivos clínicos | `/consultations/{consultationId}/files` | carga, listado, contenido y eliminación | Cobertura parcial | Requiere asociación, autorización y persistencia |
| Preventivos | `/cartilla/vaccinations`, `/cartilla/dewormings`, `/cartilla/pets/{petId}`, `/preventive-controls/*` | vacuna y desparasitación separadas, próximo control y recordatorios | `CartillaServiceImplTest`, `CartillaControllerTest`, `RecordatorioPreventivoServiceImplTest` | Cobertura desigual; faltan flujos integrales de desparasitación y WhatsApp |
| Pagos y caja | `/payments`, `/caja` | cobros, cuentas, movimientos, egresos, devoluciones y cierres | Pruebas de pago existentes | Debe verificarse atomicidad en escenarios negativos |
| Portal del apoderado | `/clients/portal/*`, `/payments/portal/my-payments` | perfil, mascotas, historias, citas, recetas, servicios y pagos propios | Cobertura parcial | Requiere pruebas OWN y rechazo de datos ajenos |
| Rendimiento de tesis | `/thesis/performance-measurements` | medición instrumentada de operaciones críticas | `ThesisPerformanceMeasurementFilterTest` y ejecución previa | Evidencia previa todavía no homologada al ERS 1.2 |

## Modelo de datos relevante

La inspección identificó entidades para usuario, empresa, rol, asignación usuario-rol, vistas y permisos, empleado, apoderado, mascota, cita, historia clínica, consulta, prescripción, archivos, vacunación, desparasitación, pago y caja. Las pruebas de integración deberán verificar tanto la respuesta HTTP como el estado persistido.

## Mecanismos de seguridad que deben probarse

- autenticación por JWT y renovación de sesión;
- cookies protegidas y cierre de sesión;
- permiso efectivo por vista y acción;
- alcance OWN o COMPANY;
- aislamiento entre empresas y entre apoderados;
- rechazo de acceso por identificador ajeno, no solo ocultamiento en la interfaz;
- invalidación o actualización efectiva de permisos y sesiones según el ERS;
- eliminación de secretos, cookies y tokens de las evidencias.

## Criterio para reutilizar pruebas existentes

Una prueba existente solo se convertirá en evidencia ERS cuando:

1. contenga aserciones que demuestren el resultado esperado del CP;
2. se vincule explícitamente al CP y sus CA;
3. se ejecute sobre un commit y ambiente identificados;
4. produzca salida conservable y sanitizada;
5. no dependa de datos personales ni identificadores permanentes.

Los archivos que solo comprueban que un componente Angular se crea se clasifican como pruebas de humo y no cubren por sí solos un caso funcional.


# Procedimiento de derechos sobre datos y consentimiento veterinario

## 1. Alcance y responsables

Este procedimiento se aplica por clínica (empresa) y mantiene separados los datos de cada organización.

- **Titular de los datos:** formula la solicitud y consulta su estado desde una sesión autenticada.
- **Responsable del banco de datos:** la clínica, que decide y sustenta la atención de la solicitud.
- **Administrador autorizado:** revisa la identidad, coordina la búsqueda o corrección y registra la respuesta.
- **Custodios técnicos:** conservan disponibilidad, integridad, respaldo y trazabilidad; no deciden por la clínica.
- **Personal veterinario:** redacta el consentimiento clínico con información específica del procedimiento.
- **Propietario o autorizado de la mascota:** acepta, rechaza o revoca el consentimiento de forma libre.

## 2. Derechos sobre datos personales

### 2.1 Canales implementados

- Portal autenticado del titular: `/privacidad/derechos`.
- Bandeja administrativa: `/admin/privacidad/derechos`.
- Cancelación de datos: conserva el flujo reforzado existente, con verificación adicional y evaluación de impedimentos.

### 2.2 Flujo operativo

1. El titular elige acceso, rectificación, oposición o portabilidad, describe el alcance y, para rectificación, indica el dato y el valor correcto.
2. El sistema verifica la sesión y empresa activa, evita duplicados abiertos y registra fecha, método de verificación, IP y tipo de dispositivo.
3. La solicitud queda en `RECIBIDA` y se calcula una fecha límite operativa.
4. Un administrador diferente del titular la mueve a `EN_REVISION`.
5. Si falta sustento, se registra `REQUIERE_INFORMACION` junto con una explicación visible para el titular.
6. La clínica evalúa sistemas, archivos y terceros pertinentes, aplicando minimización y acceso por necesidad.
7. La solicitud termina como `ATENDIDA`, con respuesta, o `RECHAZADA`, con fundamento obligatorio.
8. Toda actuación relevante genera auditoría y la resolución deja fecha y usuario responsable.

### 2.3 Plazos operativos iniciales

- Acceso: 20 días hábiles.
- Rectificación: 10 días hábiles.
- Oposición: 10 días hábiles.
- Portabilidad: 20 días hábiles como objetivo interno, cuando resulte aplicable.
- Cancelación: usa el plazo configurado en el flujo especializado.

El cálculo automático omite sábados y domingos. Antes del uso productivo, la clínica debe validar con asesoría legal los feriados, reglas de cómputo, prórrogas y la aplicabilidad de portabilidad según su jurisdicción y caso concreto.

### 2.4 Reglas de decisión

- Una solicitud resuelta no puede modificarse.
- Quien administra no puede resolver una solicitud sobre sus propios datos.
- Un rechazo requiere una razón concreta; no basta una etiqueta genérica.
- La cancelación no elimina a ciegas registros clínicos o contables: el flujo existente evalúa obligaciones de conservación y anonimiza cuando corresponde.
- La respuesta de acceso o portabilidad solo debe contener información del titular autenticado y debe entregarse por un canal seguro.

## 3. Consentimiento informado veterinario

### 3.1 Contenido mínimo implementado

El documento registra mascota, consulta opcional, persona autorizada, procedimiento, diagnóstico o indicación, descripción, beneficios, riesgos previsibles, alternativas, consecuencias de no realizarlo y autorizaciones específicas de anestesia y emergencia.

### 3.2 Flujo operativo

1. El personal con permiso de historias clínicas prepara el documento.
2. El sistema comprueba que la mascota pertenece a la clínica y que el destinatario mantiene una relación vigente con permiso para autorizar atención.
3. Se genera una versión inmutable y una huella SHA-256 del texto exacto.
4. El autorizado lo consulta en `/apoderado/consentimientos`, puede formular preguntas y confirma si fueron aclaradas.
5. Para decidir, debe estar autenticado y escribir el documento de identidad que consta en su ficha.
6. La aceptación, rechazo o revocación registra fecha, identidad declarada, relación con la mascota, IP, dispositivo y auditoría.
7. Un consentimiento aceptado puede revocarse antes del procedimiento; la revocación no borra la evidencia histórica.

### 3.3 Reglas clínicas

- La ausencia de consentimiento no se interpreta como aceptación.
- No se permite aceptar sin confirmar que existió oportunidad de preguntar y recibir aclaraciones.
- El rechazo exige motivo; no autoriza el procedimiento.
- Si cambia de forma material el procedimiento, sus riesgos o alternativas, se debe emitir una nueva versión.
- El consentimiento no reemplaza el juicio profesional, la historia clínica ni los protocolos de emergencia aplicables.

## 4. Configuración por clínica

La disponibilidad de los controles propios de cada clínica la define exclusivamente el administrador de plataforma. El administrador de la clínica puede operar los controles habilitados, pero no puede activarlos, desactivarlos ni cambiar su modalidad. Los términos y condiciones y la política de privacidad de la plataforma permanecen globales y no forman parte de esta configuración.

Cada control admite una de estas modalidades:

- **SOFTVET:** la funcionalidad y su evidencia documental se gestionan dentro de la plataforma.
- **EXTERNO:** la funcionalidad está habilitada, pero la clínica declara que la evidencia documental se conserva fuera de SoftVet. SoftVet no puede marcarla como bloqueante ni afirmar que verificó esa evidencia.
- **NO_APLICA:** la funcionalidad o control queda deshabilitado y no se muestran formularios, avisos ni acciones relacionados.

Los controles configurables son el aviso de privacidad de la clínica, los derechos sobre datos personales, el uso de inteligencia artificial, los recordatorios preventivos por correo y el consentimiento informado veterinario. Cada cambio crea una versión inmutable con justificación, referencia contractual opcional, fecha de vigencia, administrador responsable y confirmación de revisión legal.

Cuando IA o recordatorios se gestionan en SoftVet, el aviso de privacidad también debe gestionarse en SoftVet. La plataforma rechaza una configuración que deje autorizaciones internas sin aviso de referencia. Deshabilitar un control solo afecta a la función vinculada: el resto del sistema clínico y administrativo continúa funcionando.

## 5. Evidencias y conservación

Las solicitudes y consentimientos se conservan como registros de cumplimiento. No deben almacenarse secretos, contraseñas ni copias innecesarias de documentos de identidad en los campos libres. El acceso se limita por empresa, rol y permisos, y los respaldos siguen la política de cifrado y retención del proyecto.

## 6. Verificación antes de producción

1. Ejecutar las migraciones `V114`, `V115` y `V116` en un entorno de prueba.
2. Probar cada estado con un titular y un administrador diferentes.
3. Confirmar que otra empresa no pueda consultar los registros.
4. Aceptar, rechazar y revocar un consentimiento con un propietario autorizado; repetir con una persona no autorizada y comprobar el rechazo.
5. Comparar la huella SHA-256 antes y después de la decisión; el contenido debe permanecer igual.
6. Revisar los eventos en auditoría y realizar una restauración de respaldo de prueba.
7. Someter textos, plazos y criterios de conservación a revisión legal antes de habilitar el módulo a usuarios reales.
8. Probar cada combinación permitida de los controles por clínica y confirmar que las opciones deshabilitadas no aparezcan ni bloqueen funciones no relacionadas.

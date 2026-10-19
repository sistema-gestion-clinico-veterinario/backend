-- Publica una nueva versión porque el documento vigente describía planes,
-- suscripciones y cobros que todavía no forman parte de SoftVet.
-- Las versiones anteriores y sus constancias de aceptación se conservan.

UPDATE legal_document
SET activo = FALSE
WHERE tipo = 'TERMINOS_Y_CONDICIONES'
  AND activo = TRUE;

WITH documento(contenido) AS (
    VALUES ($terminos$
TÉRMINOS Y CONDICIONES DE USO DE SOFTVET
Versión 2.0
Vigente desde su publicación en la Plataforma

1. OBJETO
Los presentes Términos y Condiciones regulan el acceso y uso de SoftVet, plataforma web destinada a apoyar la gestión administrativa y clínico-veterinaria de las clínicas registradas.

Estos Términos se aplican a las clínicas que utilizan SoftVet, a sus administradores, médicos veterinarios, personal autorizado y a los responsables de mascotas que accedan a las funciones habilitadas para ellos.

La aceptación se realiza mediante el mecanismo dispuesto en la Plataforma. Si una persona no está de acuerdo con estos Términos, no debe utilizar SoftVet.

2. DEFINICIONES
Para este documento se entiende por:

Clínica: establecimiento veterinario registrado en SoftVet.
Administrador de la clínica: usuario autorizado para administrar la información, el personal, los roles y la configuración de una clínica.
Personal autorizado: médicos veterinarios, asistentes y demás trabajadores a quienes la clínica concede acceso.
Responsable de mascota: persona registrada como propietaria, apoderada o responsable de una mascota.
Usuario: toda persona que accede a SoftVet mediante una cuenta autorizada.
Datos de la clínica: información registrada por la clínica o por sus Usuarios durante el uso de la Plataforma.
Plataforma: aplicación web, servicios y componentes que conforman SoftVet.

3. CONDICIONES ACTUALES DE ACCESO
SoftVet no aplica actualmente planes de suscripción ni cobra a los Usuarios o a las clínicas por acceder a la Plataforma.

El acceso gratuito actual no obliga a mantener indefinidamente todas las funcionalidades bajo las mismas condiciones. Si en el futuro se incorporan planes, servicios de pago o funcionalidades comerciales, sus condiciones deberán informarse previamente y requerirán la aceptación expresa de la clínica. No se efectuarán cobros automáticos ni retroactivos.

4. REGISTRO Y CUENTAS
El acceso a SoftVet requiere una cuenta previamente registrada o autorizada. Las cuentas del personal son creadas o habilitadas por la clínica o por un administrador autorizado. Las cuentas de responsables de mascotas se habilitan de acuerdo con la relación registrada con la clínica.

El Usuario debe proporcionar información verdadera y mantenerla actualizada. Cada cuenta es personal e intransferible y solo puede utilizarse para las funciones autorizadas.

SoftVet podrá solicitar verificaciones adicionales cuando sean necesarias para confirmar la identidad del Usuario o proteger la seguridad de la cuenta.

5. CREDENCIALES Y SEGURIDAD DE LA CUENTA
El Usuario debe proteger sus credenciales, utilizar una contraseña segura y evitar compartirla con otras personas.

Ante la sospecha de un acceso no autorizado, deberá cambiar su contraseña y comunicar el incidente a la clínica o mediante el canal de soporte disponible.

SoftVet podrá cerrar sesiones, solicitar una nueva autenticación, revocar credenciales o bloquear temporalmente una cuenta cuando detecte actividad inusual o un riesgo de seguridad.

6. ROLES Y PERMISOS
El acceso a los módulos y operaciones de SoftVet depende del rol, los permisos y la clínica asociados al Usuario.

La clínica es responsable de asignar únicamente los accesos necesarios para las funciones de su personal, revisar periódicamente los privilegios concedidos y retirar los accesos que ya no correspondan.

Ningún Usuario está autorizado a consultar, modificar o eliminar información de otra clínica o información para la cual no tenga permiso.

7. FUNCIONALIDADES DE LA PLATAFORMA
SoftVet puede proporcionar funciones para gestionar citas, clientes, mascotas, historias clínicas, vacunación, desparasitación, servicios, personal, asistencia, caja, productos, inventario, reportes, comunicaciones, auditoría, privacidad, documentos legales, revisión de accesos y monitoreo de respaldos.

La disponibilidad de cada función depende del rol del Usuario, de la configuración de la clínica y de las funciones habilitadas en el sistema.

SoftVet podrá corregir, mejorar, agregar o retirar funcionalidades. Los cambios sustanciales serán comunicados previamente cuando resulte razonablemente posible. Los cambios urgentes relacionados con seguridad, continuidad o cumplimiento legal podrán aplicarse de inmediato.

8. USO PERMITIDO
La Plataforma debe utilizarse únicamente para fines legítimos relacionados con la atención veterinaria y la gestión de la clínica.

Está prohibido:
a) acceder o intentar acceder a cuentas, módulos o datos sin autorización;
b) compartir cuentas o suplantar la identidad de otra persona;
c) alterar, eliminar, extraer o divulgar información de manera indebida;
d) introducir código malicioso o afectar la disponibilidad de la Plataforma;
e) eludir los controles de autenticación, permisos, auditoría o aislamiento entre clínicas;
f) ejecutar pruebas automatizadas, escaneos de seguridad o ingeniería inversa sin autorización; y
g) utilizar SoftVet para actividades ilícitas, fraudulentas o contrarias a los derechos de terceros.

9. INFORMACIÓN CLÍNICA Y RESPONSABILIDAD PROFESIONAL
SoftVet es una herramienta de apoyo para registrar, organizar y consultar información. No sustituye el criterio profesional del médico veterinario.

Los diagnósticos, tratamientos, prescripciones, procedimientos y demás decisiones clínicas son responsabilidad del profesional que los realiza.

La clínica y su personal deben verificar que la información registrada sea correcta, completa y corresponda al paciente atendido. SoftVet no es responsable por consecuencias derivadas de información incorrecta, incompleta o registrada en un expediente equivocado por un Usuario.

10. FUNCIONES AUTOMATIZADAS
Cuando se habiliten funciones automatizadas o basadas en inteligencia artificial, sus resultados tendrán carácter auxiliar y referencial. No constituyen un diagnóstico ni reemplazan la evaluación de un médico veterinario.

El profesional debe revisar y validar el resultado antes de utilizarlo en una decisión clínica. La Plataforma identificará estas funciones cuando su naturaleza no sea evidente para el Usuario.

11. CAJA Y REGISTRO DE PAGOS
Los importes registrados en los módulos de caja o historial de pagos corresponden a operaciones realizadas directamente entre la clínica y sus clientes por consultas, procedimientos, productos u otros servicios veterinarios.

SoftVet permite registrar y consultar dichas operaciones, pero no recibe, custodia ni transfiere esos fondos. La clínica es responsable de la exactitud de los importes, medios de pago, devoluciones y comprobantes registrados por su personal.

Si posteriormente se integra un proveedor de pago electrónico, se informarán las condiciones aplicables antes de habilitar su uso.

12. DISPONIBILIDAD Y MANTENIMIENTO
SoftVet realizará esfuerzos razonables para mantener la Plataforma disponible y segura, pero no garantiza un funcionamiento ininterrumpido o libre de errores.

El acceso puede interrumpirse temporalmente por mantenimiento, actualizaciones, incidentes de seguridad, fallas de conectividad, problemas de proveedores tecnológicos o situaciones fuera del control razonable de SoftVet.

Los mantenimientos programados serán comunicados previamente cuando resulte posible. Actualmente no se ofrece un nivel contractual garantizado de disponibilidad ni un tiempo específico de atención de soporte.

13. RESPALDOS Y RECUPERACIÓN
SoftVet ejecuta procedimientos periódicos de respaldo destinados a reducir el riesgo de pérdida de información. El estado de estos procedimientos puede mostrarse mediante funciones de monitoreo de solo lectura.

Las restauraciones se realizan mediante un procedimiento controlado de recuperación y no directamente desde la pantalla de monitoreo. Toda recuperación debe validar la identidad, autorización y alcance de la solicitud.

La existencia de respaldos no constituye una garantía absoluta de recuperación frente a eventos fuera del control razonable de SoftVet.

14. DATOS REGISTRADOS EN LA PLATAFORMA
La clínica conserva el control sobre la información que registra en SoftVet, sin perjuicio de los derechos que corresponden a los titulares de los datos personales.

SoftVet no adquiere derechos de propiedad sobre las historias clínicas, datos de clientes, mascotas, citas, tratamientos, operaciones de caja ni demás información registrada por una clínica.

La clínica es responsable de que el registro y uso de esa información tenga una finalidad legítima y se realice por personal autorizado.

15. PRIVACIDAD Y DATOS PERSONALES
El tratamiento de datos personales se rige por la Ley N.° 29733, Ley de Protección de Datos Personales, su Reglamento aprobado por el Decreto Supremo N.° 016-2024-JUS y las demás disposiciones aplicables.

Cada clínica determina las finalidades del tratamiento de los datos de sus clientes, responsables de mascotas y personal. SoftVet trata dicha información para operar la Plataforma y prestar sus funciones tecnológicas.

La Política de Privacidad de SoftVet y los avisos de privacidad publicados por cada clínica son documentos independientes de estos Términos. La aceptación de estos Términos no sustituye los consentimientos específicos que resulten necesarios.

16. SEGURIDAD DE LA INFORMACIÓN
SoftVet aplica medidas técnicas y organizativas destinadas a proteger la confidencialidad, integridad y disponibilidad de la información. Estas medidas incluyen controles de autenticación, permisos basados en roles, aislamiento entre clínicas, protección de credenciales, gestión de sesiones, registros de auditoría y procedimientos de respaldo.

Ningún sistema es completamente inmune a incidentes. Cuando SoftVet tome conocimiento de un incidente que afecte datos tratados por cuenta de una clínica, lo comunicará de forma inmediata a la clínica afectada y colaborará en su investigación, mitigación y cumplimiento de las obligaciones legales correspondientes.

17. SERVICIOS DE TERCEROS
SoftVet puede utilizar proveedores de alojamiento, base de datos, almacenamiento, correo electrónico, autenticación, mensajería y otros servicios necesarios para operar la Plataforma.

Cuando el Usuario decida utilizar una función que implique una relación directa con un proveedor externo, también podrán resultar aplicables los términos y políticas de ese proveedor.

SoftVet procurará seleccionar proveedores adecuados a la naturaleza del servicio y limitará el acceso a la información a lo necesario para la función contratada.

18. SUSPENSIÓN Y DESACTIVACIÓN
SoftVet podrá suspender temporalmente una cuenta cuando detecte actividad inusual, riesgo de seguridad, uso fraudulento, incumplimiento de estos Términos o una orden de autoridad competente.

Cuando el riesgo pueda corregirse sin comprometer la seguridad, SoftVet procurará informar al Usuario o a la clínica antes de aplicar una suspensión definitiva.

La clínica o el Usuario podrán solicitar la desactivación de su acceso mediante los canales disponibles. La desactivación de una cuenta no autoriza la eliminación de registros que deban conservarse por razones clínicas, legales, de seguridad, auditoría o defensa frente a reclamaciones.

19. EXPORTACIÓN Y ELIMINACIÓN
Cuando una clínica deje de utilizar SoftVet, podrá solicitar la exportación de sus datos durante los treinta días calendario siguientes a la desactivación de su acceso.

La información será entregada en un formato razonablemente accesible, considerando su naturaleza y las capacidades técnicas disponibles.

Después de dicho plazo, SoftVet podrá eliminar la información de los sistemas activos. Los datos incluidos en respaldos se eliminarán progresivamente conforme al ciclo técnico de retención y no serán utilizados para finalidades distintas de recuperación, seguridad o cumplimiento legal.

Podrá conservarse la información necesaria para cumplir obligaciones legales, mantener constancias de consentimiento, atender incidentes, preservar la trazabilidad o resolver controversias.

20. PROPIEDAD INTELECTUAL
El software, código, diseño, estructura, documentación, logotipos y demás elementos tecnológicos de SoftVet pertenecen a sus respectivos titulares y están protegidos por la normativa aplicable.

El Usuario recibe únicamente autorización limitada, personal, revocable y no transferible para utilizar la Plataforma conforme a estos Términos.

No está permitido copiar, modificar, distribuir, vender, sublicenciar, realizar ingeniería inversa ni explotar la Plataforma sin autorización previa.

21. RESPONSABILIDAD
Cada persona es responsable de las acciones que realiza mediante su cuenta y de los daños directos que cause por incumplir estos Términos o la legislación aplicable.

SoftVet no será responsable por decisiones clínicas, información incorrectamente registrada, uso no autorizado causado por el incumplimiento de las obligaciones del Usuario, fallas de equipos o conexiones ajenas, ni interrupciones ocasionadas por fuerza mayor o proveedores fuera de su control razonable.

Estas disposiciones no excluyen las responsabilidades que no puedan limitarse conforme a la legislación peruana.

22. MODIFICACIÓN DE LOS TÉRMINOS
SoftVet podrá actualizar estos Términos por cambios legales, técnicos, operativos o de seguridad.

Cuando se publique una nueva versión, el sistema la identificará y solicitará al Usuario su aceptación. La Plataforma conservará la versión aceptada, su contenido y la constancia correspondiente.

Los cambios urgentes necesarios para proteger la seguridad o cumplir una obligación legal podrán aplicarse inmediatamente.

23. COMUNICACIONES
Las comunicaciones relacionadas con estos Términos, la seguridad o el funcionamiento de SoftVet podrán realizarse mediante la Plataforma o a través de los datos de contacto registrados.

El Usuario debe mantener actualizada su información de contacto y utilizar los canales oficiales disponibles para comunicar incidentes o presentar solicitudes.

24. INDEPENDENCIA DE LAS DISPOSICIONES
Si una disposición de estos Términos es declarada inválida o inaplicable, las demás conservarán su validez. La disposición afectada se interpretará de la manera que mejor preserve su finalidad dentro de los límites legales.

25. LEY APLICABLE Y CONTROVERSIAS
Estos Términos se rigen por la legislación de la República del Perú.

Las partes procurarán resolver cualquier controversia mediante comunicación directa y de buena fe. Si no fuera posible alcanzar una solución, la controversia será atendida por las autoridades o tribunales competentes conforme a la legislación aplicable.
$terminos$)
)
INSERT INTO legal_document (
    tipo,
    version,
    contenido,
    contenido_hash,
    vigente_desde,
    activo,
    creado_en
)
SELECT
    'TERMINOS_Y_CONDICIONES',
    '2.0',
    contenido,
    encode(sha256(convert_to(contenido, 'UTF8')), 'hex'),
    CURRENT_TIMESTAMP,
    TRUE,
    CURRENT_TIMESTAMP
FROM documento;


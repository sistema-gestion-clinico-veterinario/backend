-- Publica la política global de SoftVet separada de los avisos de privacidad
-- particulares de cada clínica. La versión anterior y sus constancias de
-- lectura se conservan para mantener la trazabilidad legal.

UPDATE legal_document
SET activo = FALSE
WHERE tipo = 'POLITICA_PRIVACIDAD'
  AND activo = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM legal_document
      WHERE tipo = 'POLITICA_PRIVACIDAD' AND version = '2.0'
  );

WITH documento(contenido) AS (
    VALUES ($politica$
POLÍTICA DE PRIVACIDAD DE SOFTVET
Versión 2.0
Vigente desde su publicación en la Plataforma

1. OBJETO Y ALCANCE
Esta Política explica cómo SoftVet trata los datos personales necesarios para operar su plataforma web de gestión clínico-veterinaria, proteger las cuentas y prestar las funciones habilitadas a clínicas, personal autorizado y responsables de mascotas.

La Política se aplica al tratamiento realizado por SoftVet como operador tecnológico. Cada clínica es responsable de las finalidades para las que registra y utiliza los datos de sus clientes, responsables de mascotas y trabajadores. Los detalles de ese tratamiento se informan en el aviso de privacidad publicado por cada clínica.

Esta Política se interpreta de acuerdo con la Ley N.° 29733, Ley de Protección de Datos Personales, su Reglamento aprobado por el Decreto Supremo N.° 016-2024-JUS y las demás disposiciones peruanas aplicables.

2. ROLES EN EL TRATAMIENTO
SoftVet determina el tratamiento necesario para administrar las cuentas de acceso, proteger la Plataforma, registrar evidencias de seguridad, atender solicitudes de soporte y mantener los documentos legales globales.

Respecto de la información que una clínica registra para atender a sus clientes y mascotas, la clínica determina las finalidades y SoftVet actúa como proveedor tecnológico y encargado del tratamiento, siguiendo las instrucciones legítimas de la clínica.

La Política de SoftVet no reemplaza el aviso de privacidad de cada clínica ni los consentimientos específicos que correspondan.

3. DATOS QUE PUEDEN SER TRATADOS
Según el tipo de Usuario y las funciones utilizadas, SoftVet puede tratar:

a) datos de identificación y contacto, como nombres, apellidos, documento de identidad, correo electrónico, teléfono y dirección;
b) datos de la relación con una clínica, como empresa asociada, cargo, profesión, colegiatura, especialidad, rol y permisos;
c) datos de autenticación y seguridad, como identificador de cuenta, huella no reversible de la contraseña, estado de activación, sesiones, dirección IP, navegador, dispositivo y eventos de auditoría;
d) información de clientes, responsables de mascotas y relaciones autorizadas con cada mascota;
e) datos de mascotas y registros veterinarios asociados, como citas, antecedentes, consultas, diagnósticos, tratamientos, prescripciones, vacunas, desparasitaciones, peso y archivos clínicos;
f) información administrativa, de inventario y de operaciones de caja registrada por la clínica, sin incluir datos completos de tarjetas bancarias;
g) comunicaciones, solicitudes de soporte, solicitudes de derechos y decisiones de privacidad;
h) constancias de lectura, aceptaciones legales y consentimientos específicos; e
i) preferencias técnicas y de accesibilidad almacenadas en el navegador.

4. ORIGEN DE LOS DATOS
Los datos pueden ser proporcionados directamente por el Usuario, registrados por una clínica con motivo de una relación laboral o de atención veterinaria, generados automáticamente durante el uso seguro de la Plataforma o recibidos de Google cuando el Usuario decide utilizar el acceso con Google.

Cuando se utiliza Google, SoftVet recibe únicamente la información necesaria para verificar la identidad asociada al correo electrónico. SoftVet no recibe ni almacena la contraseña de Google.

5. FINALIDADES NECESARIAS
SoftVet trata los datos necesarios para:

a) crear, activar y administrar cuentas y accesos por clínica;
b) autenticar Usuarios, gestionar sesiones y recuperar el acceso;
c) aplicar roles, permisos y separación de información entre clínicas;
d) operar las funciones administrativas y clínico-veterinarias solicitadas por cada clínica;
e) registrar citas, historias clínicas veterinarias, controles preventivos, inventario y operaciones de caja;
f) enviar comunicaciones operativas, como activación de cuenta, verificación de correo, recuperación de acceso y alertas de seguridad;
g) detectar y atender accesos indebidos, fraude, abuso o incidentes de seguridad;
h) mantener registros de auditoría, aceptaciones, constancias de lectura y evidencias de cumplimiento;
i) atender consultas, solicitudes de privacidad y soporte;
j) generar respaldos, verificar su integridad y ejecutar procedimientos controlados de recuperación; y
k) cumplir obligaciones legales y requerimientos de autoridades competentes.

6. FINALIDADES OPCIONALES
Los recordatorios preventivos por correo y el uso de inteligencia artificial con información clínica solo se habilitan cuando la clínica ha configurado el control correspondiente y el titular ha otorgado la autorización requerida.

El titular puede retirar una autorización opcional mediante las funciones de privacidad disponibles o a través de la clínica. El retiro no afecta la licitud del tratamiento realizado anteriormente y no impide los tratamientos necesarios que tengan otra justificación legal.

SoftVet no vende datos personales ni los utiliza para publicidad de terceros.

7. COOKIES Y ALMACENAMIENTO DEL NAVEGADOR
SoftVet utiliza cookies estrictamente necesarias para autenticar al Usuario, renovar de forma segura su sesión, proteger el flujo de acceso con Google y vincular de manera segura un equipo con un punto de caja cuando corresponda.

Los tokens de sesión se almacenan en cookies protegidas y no se guardan en el almacenamiento local del navegador. El almacenamiento local o de sesión se utiliza para preferencias de interfaz, accesibilidad, recorrido guiado y estados temporales que no contienen contraseñas.

SoftVet no utiliza actualmente cookies publicitarias ni herramientas de analítica de terceros. Si esta situación cambia, se actualizará la Política y se solicitará el consentimiento cuando corresponda.

8. PROVEEDORES Y TRANSFERENCIAS
Para operar la Plataforma, SoftVet utiliza servicios tecnológicos como Render para alojamiento de la aplicación, Supabase para base de datos y almacenamiento, Brevo para correo transaccional y Google cuando el Usuario elige la autenticación con Google. Los respaldos pueden almacenarse mediante el proveedor seguro configurado para esa finalidad.

Estos proveedores tratan únicamente la información necesaria para prestar sus servicios y están sujetos a sus propias medidas y compromisos de seguridad. Algunos tratamientos pueden realizarse fuera del Perú, por lo que SoftVet adopta medidas razonables para que las transferencias se efectúen conforme a la normativa aplicable.

SoftVet también podrá comunicar información a una autoridad competente cuando exista una obligación legal o requerimiento válido.

9. CONSERVACIÓN
Los datos se conservan durante el tiempo necesario para mantener la cuenta, prestar las funciones solicitadas, preservar la historia clínica veterinaria, proteger la seguridad y cumplir obligaciones legales.

El cierre o desactivación de una cuenta no produce la eliminación inmediata de información que deba conservarse por razones clínicas, legales, de auditoría, seguridad o defensa frente a reclamaciones.

Las constancias de aceptación, lectura y consentimiento se conservan con la versión y huella del documento correspondiente. Los datos incluidos en respaldos se eliminan progresivamente conforme al ciclo técnico de retención y no se utilizan para finalidades ordinarias.

10. SEGURIDAD
SoftVet aplica medidas técnicas y organizativas destinadas a proteger la confidencialidad, integridad y disponibilidad de la información. Estas medidas incluyen protección no reversible de contraseñas, cookies de sesión protegidas, control de acceso basado en roles, aislamiento entre clínicas, revisión de accesos privilegiados, registros de auditoría, límites frente a intentos abusivos y procedimientos de respaldo y recuperación.

Ningún sistema conectado a Internet puede garantizar seguridad absoluta. Cuando SoftVet tome conocimiento de un incidente que afecte datos tratados por cuenta de una clínica, lo comunicará de forma inmediata a la clínica afectada y colaborará con su investigación y mitigación. Las notificaciones a titulares y autoridades se realizarán cuando correspondan y dentro de los plazos legales aplicables.

11. DERECHOS DE LOS TITULARES
El titular puede solicitar acceso a sus datos, rectificación de información inexacta, oposición al tratamiento, portabilidad cuando corresponda y cancelación conforme a las condiciones legales aplicables.

SoftVet dispone de funciones para registrar y dar seguimiento a solicitudes de derechos. Cuando la solicitud se refiera a información tratada por una clínica para la atención veterinaria, la clínica será responsable de evaluarla y responderla, con el apoyo técnico de SoftVet cuando resulte necesario.

La identidad de la persona solicitante debe verificarse antes de entregar, modificar, transferir o cancelar información. La cancelación puede no proceder respecto de datos cuya conservación sea necesaria por obligación legal, atención de una relación vigente, seguridad, auditoría o defensa frente a reclamaciones.

Si el titular considera que su solicitud no fue atendida adecuadamente, puede acudir a la Autoridad Nacional de Protección de Datos Personales.

12. MENORES DE EDAD
SoftVet no está dirigido a menores de edad para la creación autónoma de cuentas. Cuando resulte necesario registrar información vinculada con una persona menor de edad, la clínica deberá actuar a través de su representante legal y aplicar las medidas exigidas por la normativa.

13. DOCUMENTOS DE CADA CLÍNICA
Cada clínica publica avisos de privacidad diferenciados para responsables de mascotas y para trabajadores o Usuarios internos. Dichos avisos identifican a la clínica, las finalidades aplicables y los canales disponibles para ejercer derechos.

La lectura de esta Política global no sustituye la información ni las decisiones que deben registrarse respecto del aviso particular de la clínica.

14. MODIFICACIONES Y CONSTANCIA DE LECTURA
SoftVet puede actualizar esta Política por cambios legales, técnicos, operativos o de seguridad. Cada nueva versión será identificada mediante su número y fecha de vigencia.

Cuando se publique una nueva versión, la Plataforma la mostrará al Usuario y registrará una constancia de lectura. La Política es un documento informativo; las autorizaciones para finalidades opcionales se solicitan y registran por separado.

15. CONTACTO
Las consultas, incidentes y solicitudes relacionadas con esta Política pueden presentarse mediante los canales de soporte disponibles en la Plataforma. Las solicitudes relacionadas con datos tratados por una clínica también pueden dirigirse directamente a esa clínica mediante los datos indicados en su aviso de privacidad.
$politica$)
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
    'POLITICA_PRIVACIDAD',
    '2.0',
    contenido,
    encode(sha256(convert_to(contenido, 'UTF8')), 'hex'),
    CURRENT_TIMESTAMP,
    TRUE,
    CURRENT_TIMESTAMP
FROM documento
WHERE NOT EXISTS (
    SELECT 1 FROM legal_document
    WHERE tipo = 'POLITICA_PRIVACIDAD' AND version = '2.0'
);

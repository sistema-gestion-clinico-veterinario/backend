-- Reemplaza el placeholder de la v1.0 de Términos y Condiciones / Política de
-- Privacidad por el borrador completo trabajado en legal-drafts/ (sigue siendo
-- un BORRADOR pendiente de validación legal, no un texto legalmente vinculante).
--
-- Se actualiza la misma versión (no se crea una v1.1) porque todavía no hay
-- usuarios que hayan aceptado el borrador anterior: no corresponde disparar
-- el ciclo de re-consentimiento por un ajuste de redacción inicial.
--
-- Pendiente antes de producción real: reemplazar "[Razón Social]" por la
-- razón social exacta de la clínica, y que un abogado revise el contenido
-- completo (Ley N.° 29733 de Protección de Datos Personales, Perú).

UPDATE legal_document
SET contenido = 'TÉRMINOS Y CONDICIONES DE USO — VARGAS VET
Versión 1.0 — BORRADOR PROVISIONAL, pendiente de validación legal.

Este documento es una plantilla técnica de referencia, no un documento legal vinculante. Debe ser revisado y validado por un abogado antes de publicarse en producción con usuarios reales, especialmente por tratarse de un servicio SaaS multi-tenant que procesa datos personales de clientes (apoderados), empleados y datos de salud de mascotas.

1. Aceptación de los términos
Al registrarte o utilizar Vargas Vet ("la Plataforma"), operada por [Razón Social], aceptas estos Términos y Condiciones y la Política de Privacidad asociada. Si no estás de acuerdo, no debes utilizar la Plataforma.

2. Descripción del servicio
La Plataforma es un sistema de gestión clínico-veterinaria ofrecido bajo modalidad SaaS (Software as a Service) a clínicas veterinarias ("Clientes Corporativos"), quienes a su vez la ponen a disposición de su personal y de los apoderados/dueños de mascotas registrados.

3. Cuentas de usuario
Las cuentas de personal son creadas y administradas por el Cliente Corporativo (clínica). El usuario es responsable de la confidencialidad de sus credenciales. La Plataforma puede suspender cuentas que incumplan estos Términos o representen un riesgo de seguridad para la Plataforma o para otros usuarios.

4. Uso permitido
El usuario se compromete a utilizar la Plataforma únicamente para fines lícitos relacionados con la gestión clínica veterinaria, y a no intentar vulnerar sus medidas de seguridad.

5. Propiedad de los datos
Los datos ingresados por cada Cliente Corporativo (historiales clínicos, información de mascotas, empleados y apoderados) son de propiedad del Cliente Corporativo. La Plataforma actúa como encargada del tratamiento de dichos datos conforme a la Política de Privacidad.

6. Disponibilidad del servicio
El servicio se presta "tal cual" y "según disponibilidad". La Plataforma realiza esfuerzos razonables para mantener la continuidad del servicio, pero no garantiza disponibilidad ininterrumpida.

7. Limitación de responsabilidad
[A definir con asesoría legal: alcance de responsabilidad por pérdida de datos, decisiones clínicas tomadas con base en la información registrada, disponibilidad del servicio, etc.]

8. Modificaciones a estos Términos
Estos Términos pueden actualizarse periódicamente. Cuando se publique una nueva versión, se solicitará al usuario aceptarla nuevamente antes de continuar usando la Plataforma.

9. Legislación aplicable
Ley N.° 29733 de Protección de Datos Personales (Perú) y su reglamento, dado que se procesan datos personales de clientes y empleados.

10. Contacto
Para consultas sobre estos Términos: soporte@patitasfelices.com'
WHERE tipo = 'TERMINOS_Y_CONDICIONES' AND version = '1.0';

UPDATE legal_document
SET contenido = 'POLÍTICA DE PRIVACIDAD — VARGAS VET
Versión 1.0 — BORRADOR PROVISIONAL, pendiente de validación legal.

Plantilla técnica de referencia, no un documento legal vinculante. Debe validarse con un abogado, considerando que la Plataforma procesa datos personales (clientes, empleados, apoderados) y datos de mascotas asociados a personas identificables.

1. Responsable del tratamiento
[Razón Social], en su calidad de responsable del tratamiento de los datos personales recogidos a través de la Plataforma.

2. Datos que se recopilan
Datos de identificación y contacto (nombre, apellido, DNI, teléfono, dirección, correo); credenciales de acceso (contraseña almacenada de forma cifrada); datos de la relación con la clínica (roles, empresa asociada); datos de mascotas y su historial clínico (cuando el usuario es un apoderado/cliente); y metadatos técnicos de sesión (dirección IP, user-agent) con fines de seguridad y auditoría.

3. Finalidad del tratamiento
Prestar el servicio de gestión clínico-veterinaria contratado por el Cliente Corporativo; gestionar la autenticación y seguridad de la cuenta; registrar auditoría de acciones críticas; y enviar comunicaciones operativas relacionadas con el servicio (verificación de cuenta, recuperación de contraseña, cambios de correo, etc.).

4. Base legal
[A definir con asesoría legal conforme a la Ley N.° 29733 de Protección de Datos Personales del Perú: consentimiento del titular, ejecución de una relación contractual con el Cliente Corporativo, u otra base aplicable.]

5. Conservación de los datos
Los datos se conservan mientras la cuenta permanezca activa y durante el plazo adicional que exija la normativa aplicable o los términos del contrato con el Cliente Corporativo.

6. Encargados y terceros
[Detallar: proveedor de hosting/base de datos, proveedor de correo transaccional, y cualquier servicio de IA (laboratorio/radiografía) que procese datos clínicos, si aplica.]

7. Derechos del titular
El titular de los datos puede ejercer sus derechos de acceso, rectificación, cancelación y oposición (derechos ARCO) escribiendo a soporte@patitasfelices.com.

8. Seguridad
La Plataforma implementa medidas técnicas (cifrado de contraseñas, control de acceso basado en roles, registro de auditoría, expiración y rotación de sesiones) para proteger los datos personales frente a accesos no autorizados.

9. Menores de edad
La Plataforma no está dirigida a menores de edad para la creación de cuentas propias.

10. Cambios a esta Política
Cuando se publique una nueva versión de esta Política, se solicitará al usuario aceptarla nuevamente antes de continuar usando la Plataforma.

11. Contacto
soporte@patitasfelices.com'
WHERE tipo = 'POLITICA_PRIVACIDAD' AND version = '1.0';

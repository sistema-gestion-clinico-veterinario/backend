-- Reemplaza el contenido de la v1.0 de Términos y Condiciones con la versión
-- surgida de una revisión jurídico-contractual exhaustiva (roles diferenciados,
-- separación T&C / Contrato de Servicio / Política de Privacidad, cláusulas de
-- exportación y eliminación de datos, incidentes de seguridad, responsabilidad
-- clínica y herramientas de IA como apoyo referencial, subencargados del
-- tratamiento, fuerza mayor autónoma, propiedad de los Datos del Cliente).
--
-- Sigue siendo la VERSIÓN 1.0: todavía no hay usuarios que hayan aceptado
-- ninguna versión anterior de este documento, por lo que no corresponde
-- numerarla como 2.0 ni disparar un nuevo ciclo de re-consentimiento.
--
-- Sigue siendo un BORRADOR pendiente de validación legal profesional y de
-- completar los marcadores [Razón Social] / [POR COMPLETAR] (ver Parte V y VI
-- del análisis jurídico entregado junto con esta migración).

UPDATE legal_document
SET contenido = 'TÉRMINOS Y CONDICIONES GENERALES — VETSOFT
Versión 1.0 — BORRADOR PROVISIONAL, pendiente de validación legal y de completar datos comerciales.

1. Disposiciones generales
Los presentes Términos y Condiciones Generales (en adelante, los "Términos") regulan el acceso y uso de la plataforma de gestión clínico-veterinaria VetSoft (en adelante, la "Plataforma"), ofrecida por [Razón Social] (en adelante, "VETSOFT"), bajo modalidad de Software as a Service (SaaS).
Estos Términos se aplican a toda clínica veterinaria que contrate el Servicio (en adelante, el "Cliente Corporativo"), así como a su personal y a los apoderados/dueños de mascotas que accedan a la Plataforma (en conjunto, los "Usuarios"). El uso de la Plataforma implica la aceptación íntegra de estos Términos y de la Política de Privacidad vigente.
Las condiciones comerciales específicas de cada Cliente Corporativo (plan contratado, tarifa, periodicidad, fecha de inicio) se formalizan en un Contrato de Servicio u Orden de Compra independiente, el cual prevalece sobre estos Términos en lo relativo a dichas condiciones comerciales.

2. Definiciones
"Administrador": Usuario del Cliente Corporativo con permisos de configuración y gestión de la cuenta de la clínica.
"Veterinario" / "Personal de atención": Usuarios del Cliente Corporativo que registran y consultan información clínica dentro de la Plataforma.
"Apoderado": Usuario dueño/responsable de una mascota registrada por el Cliente Corporativo, con acceso al portal correspondiente.
"Cliente Corporativo": la clínica veterinaria que contrata el Servicio.
"Datos del Cliente": toda la información ingresada en la Plataforma por o para el Cliente Corporativo (historias clínicas, mascotas, citas, pagos de servicios veterinarios, información administrativa).
"Plataforma": la aplicación web y sus componentes a través de los cuales se presta el Servicio.
"Servicio": la suscripción SaaS de gestión clínico-veterinaria ofrecida por VETSOFT.

3. Capacidad y representación
El Usuario declara contar con capacidad legal para aceptar estos Términos. Quien registre a un Cliente Corporativo declara contar con representación suficiente para contratar en su nombre.

4. Descripción del Servicio
VETSOFT ofrece una herramienta tecnológica de apoyo a la gestión administrativa y clínica de la clínica veterinaria (citas, historias clínicas, control de vacunación y desparasitación, gestión de personal, caja, portal de apoderados, y funcionalidades de apoyo mediante inteligencia artificial cuando estén disponibles). VetSoft es una herramienta de gestión y no sustituye el criterio profesional del médico veterinario (ver cláusula 14).
VETSOFT podrá modificar, agregar o retirar funcionalidades de la Plataforma. Cuando un cambio afecte de forma sustancial una funcionalidad activamente usada por el Cliente Corporativo, VETSOFT lo comunicará con una anticipación razonable [POR COMPLETAR: plazo exacto de aviso].

5. Registro y cuentas de usuario
Las cuentas de personal son creadas por el propio Cliente Corporativo desde su panel administrativo; el nuevo Usuario recibe un enlace de activación para configurar su contraseña. Las cuentas de apoderados son registradas por el Cliente Corporativo. El uso de la cuenta es responsabilidad exclusiva de su titular.

6. Uso lícito
El Usuario declara que la información que proporciona es verdadera y se compromete a mantenerla actualizada, y es responsable de las actividades realizadas mediante su cuenta. Ante el uso indebido de la Plataforma, VETSOFT podrá amonestar, suspender o cancelar la cuenta correspondiente, sin perjuicio de las acciones legales que correspondan.

7. Responsabilidad por credenciales y accesos
El Usuario es responsable de resguardar su contraseña. VETSOFT no será responsable por accesos realizados con credenciales válidas que hayan sido compartidas o expuestas por negligencia del propio Usuario. Ante la sospecha de un acceso no autorizado, el Usuario deberá cambiar su contraseña de inmediato y notificar a VETSOFT o a su Cliente Corporativo.

8. Relación comercial: planes, renovación, suspensión y cancelación
El Cliente Corporativo contrata el Servicio bajo el plan, periodicidad y tarifa acordados en el Contrato de Servicio correspondiente. [POR COMPLETAR: condiciones definitivas de renovación, período de gracia por falta de pago, plazo de aviso para cancelación, efectos de la cancelación sobre el acceso a la Plataforma.]
VETSOFT podrá suspender el acceso del Cliente Corporativo en caso de falta de pago conforme al plazo pactado, incumplimiento de estos Términos, o uso fraudulento de la Plataforma, previa notificación cuando sea razonablemente posible.

9. Pagos registrados dentro de la Plataforma
Los pagos que se registran dentro de la Plataforma corresponden a los servicios veterinarios que el apoderado abona directamente al Cliente Corporativo (consultas, vacunas, procedimientos, productos), y son independientes de la suscripción del Servicio. VETSOFT únicamente provee el módulo de registro de dichos pagos; la relación de cobro es exclusivamente entre el Cliente Corporativo y el apoderado. Si en el futuro se integra una pasarela de pago electrónico, dicha integración se regirá adicionalmente por los términos propios de su proveedor.

10. Disponibilidad, mantenimiento y soporte técnico
VETSOFT procurará mantener la Plataforma disponible y realizará labores de mantenimiento procurando avisar con anticipación razonable. Los niveles específicos de disponibilidad garantizada y tiempos de atención de soporte, de existir, se detallan en un Anexo de Nivel de Servicio (SLA) [POR COMPLETAR: si existe o se proyecta un SLA formal].

11. Copias de seguridad, exportación y eliminación de datos
VETSOFT realiza copias de seguridad periódicas de los Datos del Cliente con el fin de minimizar el riesgo de pérdida de información, sin garantizar la integridad absoluta ante eventos fuera de su control.
Al terminar la relación contractual, el Cliente Corporativo podrá solicitar la exportación de sus Datos del Cliente en un formato razonablemente accesible, dentro del plazo de [POR COMPLETAR] días desde la terminación. Transcurrido dicho plazo sin que medie solicitud de exportación, VETSOFT podrá eliminar los Datos del Cliente de sus sistemas activos, conservando únicamente lo que resulte exigible por ley.

12. Propiedad y control de los Datos del Cliente
Los Datos del Cliente (historias clínicas, mascotas, citas, tratamientos, pagos de servicios veterinarios e información administrativa) son de propiedad del Cliente Corporativo que los generó. VETSOFT no reclama derechos de propiedad sobre dicha información y la trata únicamente para prestar el Servicio y en calidad de encargado del tratamiento respecto de los datos personales que dicha información contenga, conforme a la Política de Privacidad.

13. Seguridad de la información e incidentes
VETSOFT implementa medidas técnicas orientadas a proteger la Plataforma y los Datos del Cliente (cifrado de contraseñas, control de acceso basado en roles, registro de auditoría, rotación de sesiones). En caso de tomar conocimiento de un incidente de seguridad que afecte datos personales tratados en la Plataforma, VETSOFT notificará al Cliente Corporativo afectado dentro de un plazo razonable [POR COMPLETAR: plazo exacto], a fin de que este último pueda cumplir con sus propias obligaciones frente a los titulares de los datos y la autoridad competente.

14. Responsabilidad clínica y herramientas automatizadas
VetSoft es una herramienta tecnológica de apoyo a la gestión clínica y administrativa; no sustituye el criterio profesional del médico veterinario. Toda decisión de diagnóstico o tratamiento es responsabilidad exclusiva del profesional veterinario que la adopta, con independencia de la información registrada en la Plataforma.
Cuando la Plataforma incorpore funcionalidades basadas en inteligencia artificial (por ejemplo, apoyo en la interpretación de estudios de laboratorio o imágenes), dichos resultados constituyen una herramienta de apoyo referencial y no un diagnóstico, y en ningún caso reemplazan el juicio clínico del profesional veterinario responsable del caso.
VETSOFT no será responsable por errores derivados de información incompleta, inexacta o mal ingresada por el personal del Cliente Corporativo.

15. Servicios de terceros e integraciones
La Plataforma puede apoyarse en proveedores de infraestructura (hosting, base de datos), correo electrónico transaccional, mensajería (por ejemplo, WhatsApp), servicios de inteligencia artificial, facturación electrónica y/o pasarelas de pago. Cuando el Usuario o Cliente Corporativo interactúe directamente con un servicio de un tercero integrado en la Plataforma, dicha interacción quedará adicionalmente sujeta a los términos y condiciones propios de ese tercero, los cuales serán comunicados oportunamente cuando corresponda.

16. Exención y limitación de responsabilidad
VETSOFT no será responsable por: (i) decisiones clínicas adoptadas por el personal del Cliente Corporativo; (ii) interrupciones del Servicio derivadas de causas fuera de su control razonable (fallas de conectividad del Usuario, fallas de terceros proveedores, fuerza mayor); (iii) daños derivados del uso de la Plataforma en contravención a estos Términos. Esta limitación no exime a VETSOFT de su responsabilidad por dolo o culpa inexcusable en la prestación directa del Servicio.
El Cliente Corporativo mantendrá indemne a VETSOFT frente a reclamos de terceros que se originen en el uso indebido o ilícito de la Plataforma por parte de su propio personal o apoderados registrados.

17. Fuerza mayor
Ninguna de las partes será responsable por el incumplimiento de sus obligaciones cuando este se deba a un evento de fuerza mayor o caso fortuito, ajeno a su control razonable, siempre que se comunique dicha circunstancia a la brevedad posible.

18. Propiedad intelectual
El software, código fuente, interfaz, diseño, logotipos, marca "VetSoft" y demás elementos de la Plataforma son de propiedad de VETSOFT o de sus licenciantes, y están protegidos por la normativa de propiedad intelectual aplicable. Queda prohibida su reproducción, modificación o explotación sin autorización previa y por escrito. Las sugerencias o comentarios que el Cliente Corporativo o sus Usuarios brinden voluntariamente a VETSOFT sobre la Plataforma podrán ser utilizados por VETSOFT para mejorar el Servicio, sin que ello genere contraprestación alguna a favor de quien los formuló.

19. Política de Privacidad
El tratamiento de datos personales realizado a través de la Plataforma se rige por la Política de Privacidad vigente, publicada de forma independiente a estos Términos, la cual el Usuario debe leer y aceptar de forma separada.

20. Cesión de posición contractual
VETSOFT podrá ceder los derechos y obligaciones derivados de estos Términos, comunicándolo oportunamente. El Cliente Corporativo no podrá ceder su posición contractual sin autorización previa y por escrito de VETSOFT.

21. Terminación
Cualquiera de las partes podrá terminar la relación contractual conforme a lo pactado en el Contrato de Servicio correspondiente. Terminada la relación, aplican las disposiciones de la cláusula 11 sobre exportación y eliminación de datos.

22. Modificaciones a estos Términos
VETSOFT podrá actualizar estos Términos. Cuando se publique una nueva versión, se solicitará al Usuario aceptarla nuevamente antes de continuar usando la Plataforma.

23. Validez de las disposiciones
Si alguna disposición de estos Términos resultara inválida o inexigible, dicha invalidez no afectará la validez de las disposiciones restantes.

24. Ley aplicable y jurisdicción
Estos Términos se rigen por la ley peruana. Toda controversia que no pueda resolverse mediante negociación directa entre las partes se someterá a los mecanismos de solución de controversias y a los jueces y tribunales que correspondan conforme a ley.'
WHERE tipo = 'TERMINOS_Y_CONDICIONES' AND version = '1.0';

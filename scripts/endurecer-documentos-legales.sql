-- Paso de despliegue (no es una migración): limita lo que la cuenta de la aplicación puede hacer con el
-- historial legal. Solo tiene efecto si la aplicación NO es la propietaria de las tablas, es decir, si las
-- migraciones se ejecutan con otro usuario. Si son el mismo, la propietaria puede devolverse los permisos.
--
-- Uso:  psql -d <base> -v app_role=<usuario_de_la_aplicacion> -f endurecer-documentos-legales.sql
--
-- La aplicación solo necesita: leer, insertar versiones y constancias, y retirar la versión vigente.

REVOKE ALL ON legal_document, user_consent FROM :"app_role";
GRANT SELECT, INSERT ON legal_document, user_consent TO :"app_role";
GRANT UPDATE (activo) ON legal_document TO :"app_role";
GRANT USAGE, SELECT ON SEQUENCE legal_document_id_seq, user_consent_id_seq TO :"app_role";

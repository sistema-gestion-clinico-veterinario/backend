-- Aislamiento total entre empresas: dos empresas NUNCA deben poder detectar ni cruzar
-- datos entre si (ni el personal que registra ni el sistema), aunque sea la misma
-- persona real registrada en ambas con el mismo DNI/correo/username. Antes, el registro
-- buscaba coincidencias de DNI/correo EN TODA LA BASE y reutilizaba esa identidad, lo
-- que filtraba a la empresa B que esa persona ya existia en la empresa A - y username
-- era unico globalmente, lo cual tambien filtraba informacion (rechazar un username
-- "ya usado" revela que existe en algun lado del sistema). De ahora en adelante, cada
-- registro nuevo es siempre una cuenta propia de esa empresa, sin buscar ni cruzar
-- contra las demas - la validacion de duplicados (DNI, correo, username) pasa a ser
-- SOLO dentro de la misma empresa.

-- El unique global de username (creado en V66) ya no aplica.
DROP INDEX IF EXISTS uq_usuario_username;

CREATE UNIQUE INDEX uq_usuario_username_empresa
    ON usuario (LOWER(username), company_id)
    WHERE company_id IS NOT NULL;

CREATE UNIQUE INDEX uq_usuario_username_global
    ON usuario (LOWER(username))
    WHERE company_id IS NULL;

CREATE UNIQUE INDEX uq_usuario_email_empresa
    ON usuario (LOWER(email), company_id)
    WHERE company_id IS NOT NULL;

CREATE UNIQUE INDEX uq_usuario_email_global
    ON usuario (LOWER(email))
    WHERE company_id IS NULL;

CREATE UNIQUE INDEX uq_usuario_dni_empresa
    ON usuario (dni, company_id)
    WHERE company_id IS NOT NULL AND dni IS NOT NULL;

CREATE UNIQUE INDEX uq_usuario_dni_global
    ON usuario (dni)
    WHERE company_id IS NULL AND dni IS NOT NULL;

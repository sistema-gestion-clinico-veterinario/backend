-- El enlace de confirmacion de cambio de correo se armaba con Usuario.company (una
-- cache legacy que puede estar vacia/desactualizada para cuentas con mas de una
-- relacion), asi que a veces salia sin el slug de la empresa - dejando a la persona en
-- la pantalla de login sin marca despues de confirmar, sin poder iniciar sesion. Se
-- guarda la empresa exacta (la de la sesion activa al pedir el cambio) en la propia
-- solicitud, mismo patron que password_reset_tokens.company_id.
ALTER TABLE email_change_requests ADD COLUMN company_id INTEGER REFERENCES company(id);

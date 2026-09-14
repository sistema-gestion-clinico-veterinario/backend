-- Sin esto, refrescar la sesion de alguien con relaciones activas en mas de
-- una empresa (ej. empleado en A y cliente en B) perdia la empresa de la
-- sesion (quedaba en null, porque usuario.company solo cachea "la unica
-- empresa activa si hay exactamente una") y podia terminar resolviendo un
-- rol de OTRA empresa al refrescar el token. El refresh token ahora guarda
-- con que empresa se abrio esa sesion en particular, para restaurarla tal
-- cual en cada refresh sin ambiguedad.
ALTER TABLE refresh_tokens
    ADD COLUMN company_id INTEGER REFERENCES company(id);

COMMENT ON COLUMN refresh_tokens.company_id IS 'Empresa con la que se establecio esta sesion (nulo para SuperAdmin) - fuente de verdad al refrescar el token, no usuario.company (ambiguo si la persona tiene mas de una empresa activa).';

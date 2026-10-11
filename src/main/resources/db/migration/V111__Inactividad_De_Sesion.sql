-- Última petición autenticada de cada sesión, para cerrarla tras 30 minutos sin actividad (RNF-03).
-- Las sesiones abiertas antes de esta migración quedan sin fecha: no se cierran por inactividad hasta su
-- próxima petición, que la registra.
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS last_activity_at TIMESTAMPTZ;

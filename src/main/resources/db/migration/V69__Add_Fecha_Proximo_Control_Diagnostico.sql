-- Permite marcar una fecha de proximo control para un diagnostico en seguimiento
-- o cronico. Sin esto, no hay forma de saber si el proximo control es en 3 dias
-- o en 3 meses, y por lo tanto no se puede sugerir agendar una cita de control -
-- a diferencia de Tratamiento, que ya tiene fecha_fin para ese mismo proposito.
ALTER TABLE diagnosticos
    ADD COLUMN fecha_proximo_control DATE;

COMMENT ON COLUMN diagnosticos.fecha_proximo_control IS 'Fecha opcional de proximo control, usada para sugerir una cita de seguimiento. No se completa automaticamente - el veterinario la define solo si el diagnostico requiere seguimiento con fecha.';

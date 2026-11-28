CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

COMMENT ON TABLE shedlock IS
    'Bloqueo de las tareas programadas para que solo una instancia del backend ejecute cada una (ShedLock). Las fechas son UTC sin zona, como exige la biblioteca con el reloj de la base.';
COMMENT ON COLUMN shedlock.locked_by IS
    'Nombre de la instancia que tomo el bloqueo; solo informativo.';

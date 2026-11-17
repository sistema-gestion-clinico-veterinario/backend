# Fase 1: jornada de clínica con escrituras (validación con 1 usuario)

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

## ANTES: FLUJO COMPLETO Y CONSISTENTE

- Jornadas completas: 2; incompletas: 0.

| Paso | Veces | Correctos | Fallidos | Códigos HTTP | Mediana (ms) |
|---|---|---|---|---|---|
| Iniciar sesión | 1 | 1 | 0 | 200×1 | 370 |
| Registrar propietario | 2 | 2 | 0 | 201×2 | 198 |
| Registrar mascota | 2 | 2 | 0 | 201×2 | 115 |
| Agendar cita | 2 | 2 | 0 | 201×2 | 214 |
| Recepción (llegada) | 2 | 2 | 0 | 200×2 | 72 |
| Iniciar atención | 2 | 2 | 0 | 200×2 | 80 |
| Leer la consulta | 2 | 2 | 0 | 200×2 | 64 |
| Registrar diagnóstico | 2 | 2 | 0 | 201×2 | 62 |
| Registrar receta | 2 | 2 | 0 | 201×2 | 82 |
| Caja: crear punto de cobro | 1 | 1 | 0 | 200×1 | 90 |
| Caja: vincular este equipo | 1 | 1 | 0 | 200×1 | 94 |
| Caja: abrir la caja | 1 | 1 | 0 | 200×1 | 114 |
| Cerrar la consulta | 2 | 2 | 0 | 200×2 | 146 |
| Cobrar | 2 | 2 | 0 | 201×2 | 82 |
| Actualizar mascota | 2 | 2 | 0 | 200×2 | 66 |
| Agendar segunda cita | 2 | 2 | 0 | 201×2 | 113 |
| Cancelar la segunda cita | 2 | 2 | 0 | 200×2 | 76 |

**Comprobación contra la base de datos** (lo que k6 dice que creó frente a lo que hay en la base):

| Comprobación | Esperado | Real | ¿Coincide? |
|---|---|---|---|
| Propietarios nuevos en la base | 2 | 2 | sí |
| Mascotas nuevas en la base | 2 | 2 | sí |
| Citas nuevas en la base | 4 | 4 | sí |
| Diagnósticos nuevos en la base | 2 | 2 | sí |
| Recetas nuevas en la base | 2 | 2 | sí |
| Pagos nuevos en la base | 2 | 2 | sí |
| Citas completadas nuevas | 2 | 2 | sí |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | sí |
| Citas sin mascota (debe ser 0) | 0 | 0 | sí |
| Consultas sin cita (debe ser 0) | 0 | 0 | sí |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | sí |

## DESPUÉS: FLUJO COMPLETO Y CONSISTENTE

- Jornadas completas: 2; incompletas: 0.

| Paso | Veces | Correctos | Fallidos | Códigos HTTP | Mediana (ms) |
|---|---|---|---|---|---|
| Iniciar sesión | 1 | 1 | 0 | 200×1 | 404 |
| Registrar propietario | 2 | 2 | 0 | 201×2 | 188 |
| Registrar mascota | 2 | 2 | 0 | 201×2 | 122 |
| Agendar cita | 2 | 2 | 0 | 201×2 | 201 |
| Recepción (llegada) | 2 | 2 | 0 | 200×2 | 60 |
| Iniciar atención | 2 | 2 | 0 | 200×2 | 92 |
| Leer la consulta | 2 | 2 | 0 | 200×2 | 65 |
| Registrar diagnóstico | 2 | 2 | 0 | 201×2 | 62 |
| Registrar receta | 2 | 2 | 0 | 201×2 | 72 |
| Caja: crear punto de cobro | 1 | 1 | 0 | 200×1 | 79 |
| Caja: vincular este equipo | 1 | 1 | 0 | 200×1 | 107 |
| Caja: abrir la caja | 1 | 1 | 0 | 200×1 | 140 |
| Cerrar la consulta | 2 | 2 | 0 | 200×2 | 142 |
| Cobrar | 2 | 2 | 0 | 201×2 | 68 |
| Actualizar mascota | 2 | 2 | 0 | 200×2 | 62 |
| Agendar segunda cita | 2 | 2 | 0 | 201×2 | 252 |
| Cancelar la segunda cita | 2 | 2 | 0 | 200×2 | 68 |

**Comprobación contra la base de datos** (lo que k6 dice que creó frente a lo que hay en la base):

| Comprobación | Esperado | Real | ¿Coincide? |
|---|---|---|---|
| Propietarios nuevos en la base | 2 | 2 | sí |
| Mascotas nuevas en la base | 2 | 2 | sí |
| Citas nuevas en la base | 4 | 4 | sí |
| Diagnósticos nuevos en la base | 2 | 2 | sí |
| Recetas nuevas en la base | 2 | 2 | sí |
| Pagos nuevos en la base | 2 | 2 | sí |
| Citas completadas nuevas | 2 | 2 | sí |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | sí |
| Citas sin mascota (debe ser 0) | 0 | 0 | sí |
| Consultas sin cita (debe ser 0) | 0 | 0 | sí |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | sí |

> Todos los datos de este informe salen de los archivos que genera la prueba (registro de k6 y consultas a la base). Nada está escrito a mano.

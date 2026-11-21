# Fase 1: jornada de clínica con escrituras (validación con 1 usuario)

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

## ANTES: FLUJO COMPLETO Y CONSISTENTE

- Jornadas completas: 2; incompletas: 0.

| Paso | Veces | Correctos | Fallidos | Códigos HTTP | Mediana (ms) |
|---|---|---|---|---|---|
| Iniciar sesión | 1 | 1 | 0 | 200×1 | 462 |
| Registrar propietario | 2 | 2 | 0 | 201×2 | 159 |
| Registrar mascota | 2 | 2 | 0 | 201×2 | 116 |
| Agendar cita | 2 | 2 | 0 | 201×2 | 191 |
| Recepción (llegada) | 2 | 2 | 0 | 200×2 | 62 |
| Iniciar atención | 2 | 2 | 0 | 200×2 | 82 |
| Leer la consulta | 2 | 2 | 0 | 200×2 | 64 |
| Registrar diagnóstico | 2 | 2 | 0 | 201×2 | 54 |
| Registrar receta | 2 | 2 | 0 | 201×2 | 74 |
| Caja: crear punto de cobro | 1 | 1 | 0 | 200×1 | 83 |
| Caja: vincular este equipo | 1 | 1 | 0 | 200×1 | 82 |
| Caja: abrir la caja | 1 | 1 | 0 | 200×1 | 107 |
| Cerrar la consulta | 2 | 2 | 0 | 200×2 | 155 |
| Cobrar | 2 | 2 | 0 | 201×2 | 76 |
| Actualizar mascota | 2 | 2 | 0 | 200×2 | 168 |
| Agendar segunda cita | 2 | 2 | 0 | 201×2 | 108 |
| Cancelar la segunda cita | 2 | 2 | 0 | 200×2 | 64 |

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
| Iniciar sesión | 1 | 1 | 0 | 200×1 | 495 |
| Registrar propietario | 2 | 2 | 0 | 201×2 | 167 |
| Registrar mascota | 2 | 2 | 0 | 201×2 | 118 |
| Agendar cita | 2 | 2 | 0 | 201×2 | 184 |
| Recepción (llegada) | 2 | 2 | 0 | 200×2 | 66 |
| Iniciar atención | 2 | 2 | 0 | 200×2 | 82 |
| Leer la consulta | 2 | 2 | 0 | 200×2 | 56 |
| Registrar diagnóstico | 2 | 2 | 0 | 201×2 | 62 |
| Registrar receta | 2 | 2 | 0 | 201×2 | 76 |
| Caja: crear punto de cobro | 1 | 1 | 0 | 200×1 | 75 |
| Caja: vincular este equipo | 1 | 1 | 0 | 200×1 | 83 |
| Caja: abrir la caja | 1 | 1 | 0 | 200×1 | 106 |
| Cerrar la consulta | 2 | 2 | 0 | 200×2 | 138 |
| Cobrar | 2 | 2 | 0 | 201×2 | 196 |
| Actualizar mascota | 2 | 2 | 0 | 200×2 | 54 |
| Agendar segunda cita | 2 | 2 | 0 | 201×2 | 104 |
| Cancelar la segunda cita | 2 | 2 | 0 | 200×2 | 64 |

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

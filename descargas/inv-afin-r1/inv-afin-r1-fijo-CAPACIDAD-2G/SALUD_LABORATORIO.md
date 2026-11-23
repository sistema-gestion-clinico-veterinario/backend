# Salud del laboratorio: generador de carga, topología y memoria de la JVM

- perfil del laboratorio: CAPACIDAD-2G (memoria 2g, CPU 1, heap máximo 1g; configuración de la JVM en los niveles: D, en el control: E)
- umbral del generador: 80 % de la CPU de k6 (criterio experimental, configurable)
- modo de afinidad: fijo (aplicado: no); k6 en CPU 0, backend '--cpuset-cpus 1', PostgreSQL '--cpuset-cpus 2', muestreadores 'taskset -c 3'
- mezcla de operaciones: semilla 20261123; pesos 22,18,15,2,13,12,10,8 (panel, agenda, historia, reportes, recepción, atención, cobro, cancelar)
- citas de la clínica grande: 20000; propietarios: 1200
- pool de conexiones: 10; muestreo de PostgreSQL: no; volcados de hilos en los segundos: ninguno; registro de Hikari en DEBUG: no; NMT: no
- caída del backend: k6 se detiene 20 s después de que el contenedor se detenga
- heap en vivo: sí, cada 3 s (contadores hsperfdata y cgroup)
- runner: ubuntu24 20261116.376.1
- kernel: 7.0.0-1017-azure
- pool de conexiones: 10
- docker: 28.0.4
- k6: k6 v2.4.0 (commit/dd9395906d, go1.26.11, linux/amd64)
- fijado de CPU: PG='--cpuset-cpus 2' backend='--cpuset-cpus 1' k6='taskset -c 0'
- imágenes fijadas por digest (referencia; id local):
- imagen: postgres:16@sha256:3950ad7c3148ca2175ef1b6c36c9efbf78957ae8fbe8b3a5d7c35af50500382f (id sha256:2fd2c185c3dc19e21db2174dce9677cbe6c78dfa6b50d7a3751cea87d95d16b3; digest verificado por Docker al descargar)
- imagen: python:3.12-alpine@sha256:c5ba3deef00c7758561a0f03c26f44f10589263d40e732b8748e0d1f38f6dd89 (id sha256:4076016aea5420e5c87eb52280edfc527ace1ce31dc1c9288b1cc4ef9a0e8905; digest verificado por Docker al descargar)
- imagen: maven:3.9.6-eclipse-temurin-21-alpine@sha256:9fca6934e64eff26860889804faecb71a522d04b4ebf05574140c71f8e9bc700 (id sha256:b112e8e8ae6e4208e073641bc966e6c9f49fdd32f17000b499b1f35cb1ab75b9; digest verificado por Docker al descargar)
- imagen: eclipse-temurin:21-jre-alpine@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70 (id sha256:5649cf3d873b6600b777ddff8a1a89cffbfa19755e5334bfb61136e8ec7a59a9; digest verificado por Docker al descargar)
- bases del Dockerfile.measurement fijadas por digest: sí
- Dockerfile.measurement usado: /home/runner/work/backend/backend/resultados-inv/inv-afin-r1-fijo-CAPACIDAD-2G/Dockerfile.fijado (sha256 11a2a24a9dfa62a56dcbeb51a2f67632ebcc2f4c60b57b587feaf8e64b020b1c)
- k6 fijado: 2.4.0
- imagen de ejecución (JRE): eclipse-temurin:21-jre-alpine@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70
- openjdk version "21.0.13.1" 2026-11-17 LTS
- java: sí
- jcmd: no
- jstat: no
- jfr: sí
- jps: no
- el heap en vivo se lee de los contadores de la JVM (/tmp/hsperfdata_*/1) con cat dentro del contenedor, sin jstat ni jcmd

### Topología de CPU del runner

| CPU lógica | Núcleo físico (hermanas SMT) | core_id | Paquete |
|---|---|---|---|
| 0 | 0,1 | 0 | 0 |
| 1 | 0,1 | 0 | 0 |
| 2 | 2,3 | 1 | 0 |
| 3 | 2,3 | 1 | 0 |

- Núcleos físicos distintos: 2; SMT activo: sí.
- k6: CPU lógica(s) 0
- backend: CPU lógica(s) 1
- postgres: CPU lógica(s) 2
- muestreadores: CPU lógica(s) 3
- **k6 y backend comparten núcleo físico** (hilos hermanos): el `cpuset` los separa por CPU lógica, no por núcleo físico; pueden interferirse.
- **postgres y muestreadores comparten núcleo físico** (hilos hermanos): el `cpuset` los separa por CPU lógica, no por núcleo físico; pueden interferirse.
- **Aislamiento físico no demostrado:** el fijado de CPU solo restringe dónde pueden ejecutarse estos procesos; el demonio de Docker, el agente del runner y el núcleo del sistema no están fijados, la caché L3 y el ancho de banda de memoria son compartidos, y el runner es una máquina virtual cuyo mapeo a CPU físicas del anfitrión no es visible desde dentro.

### Modo de afinidad: fijo
- fijado anterior (k6 en CPU 0, backend '--cpuset-cpus 1', PostgreSQL '--cpuset-cpus 2'), sin considerar qué CPU lógicas son hermanas SMT


## Generador de carga (k6)

**Criterio experimental, no un estándar:** el generador se marca como saturado si su CPU, medida como porcentaje de los núcleos a los que está fijado, supera 80 % en 3 muestras consecutivas (≈ 6 s). El umbral se cambia con `UMBRAL_GENERADOR_CPU_PCT`. Un nivel marcado «potencialmente no» no debe interpretarse como capacidad del backend sin repetirlo con un generador con más recursos.

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 299 | 20.5 | no aplica (ramping-vus) | 0.1 % | 0 | 2 / 5 / 8 | 0 % | 131 | 0 | **sí** |

Utilización media por CPU lógica del runner durante cada nivel (todas las CPU, estén o no fijadas; sirve para ver si una CPU hermana SMT de una fijada estaba ocupada):

- `despues_100`: CPU 0: 4 %, CPU 1: 53 %, CPU 2: 22 %, CPU 3: 4 %

Ningún nivel superó el criterio: no se detectó saturación del generador ni carga inferior a la prevista (esto no prueba que el generador no influya, solo que no alcanzó el umbral).

> `dropped_iterations` solo lo emiten los ejecutores de tasa de llegada; este escenario usa `ramping-vus`, por lo que no aplica. La carga efectivamente generada se verifica con los usuarios activos máximos (`vus`), las peticiones totales y por segundo, y los errores.

## Memoria de la JVM y del contenedor

- **Heap usado / comprometido** (contadores de la JVM, los mismos que lee `jstat -gc`, en vivo cada pocos segundos): memoria de objetos dentro del heap de la JVM, y la reservada por la JVM para ese heap.
- **Contenedor** (`memory.current` y pico del cgroup, `docker stats`): memoria total que ve el límite de Docker (heap + Metaspace + código + hilos + memoria directa + caché de archivos).
- **GC en vivo**: contadores leídos mientras corre el nivel; la primera cifra es lo ocurrido desde que empezó el muestreo y entre paréntesis el acumulado de la JVM. **GC registro**: pausas de toda la vida de la JVM leídas del archivo `-Xlog:gc` (incluye el arranque), por eso no coinciden con la ventana del muestreo.

| Etiqueta | Heap usado máx / media (MB) | Heap comprometido máx (MB) | Metaspace usado máx (MB) | Contenedor: memory.current máx (MB) | Contenedor: pico cgroup (MB) | Contenedor: docker stats pico (MB) | GC en vivo, durante el muestreo: young / full / tiempo (s) (acumulado de la JVM) | GC registro: pausas / total ms / máx ms / full |
|---|---|---|---|---|---|---|---|---|
| despues_100 | 621 / 301 | 651 | 134 | 957 | 959 | 930 | 107 / 16 / 15.27 (274 / 29 / 21.0) | 303 / 21 013 / 608.2 / 29 |

Costo del muestreo en vivo: cada muestra tardó 101 ms de reloj de pared de media (máximo 355 ms). Es tiempo de pared, no CPU: el cálculo de la interferencia está en el flujo de calibración.

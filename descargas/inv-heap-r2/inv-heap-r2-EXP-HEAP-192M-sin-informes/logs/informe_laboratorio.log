# Salud del laboratorio: generador de carga, topología y memoria de la JVM

- perfil del laboratorio: EXP-HEAP-192M (experimental: -Xmx192m en un contenedor de 512m; el resto de las opciones es el de producción) (memoria 512m, CPU 1, heap máximo 192m; configuración de la JVM en los niveles: H, en el control: H)
- umbral del generador: 80 % de la CPU de k6 (criterio experimental, configurable)
- modo de afinidad: topologia (aplicado: si); k6 en CPU 2, backend '--cpuset-cpus 1', PostgreSQL '--cpuset-cpus 0', muestreadores 'taskset -c 2'
- mezcla de operaciones: semilla 20261123; pesos 22,18,15,0,13,12,10,8 (panel, agenda, historia, reportes, recepción, atención, cobro, cancelar)
- citas de la clínica grande: 20000; propietarios: 1200
- pool de conexiones: 10; muestreo de PostgreSQL: no; volcados de hilos en los segundos: ninguno; registro de Hikari en DEBUG: no; NMT: no
- caída del backend: k6 se detiene 20 s después de que el contenedor se detenga
- heap en vivo: sí, cada 3 s (contadores hsperfdata y cgroup)
- runner: ubuntu24 20261116.133.1
- kernel: 7.0.0-1014-aws
- pool de conexiones: 10
- docker: 28.0.4
- k6: k6 v2.4.0 (commit/dd9395906d, go1.26.11, linux/amd64)
- fijado de CPU: PG='--cpuset-cpus 0' backend='--cpuset-cpus 1' k6='taskset -c 2'
- imágenes fijadas por digest (referencia; id local):
- imagen: postgres:16@sha256:3950ad7c3148ca2175ef1b6c36c9efbf78957ae8fbe8b3a5d7c35af50500382f (id sha256:2fd2c185c3dc19e21db2174dce9677cbe6c78dfa6b50d7a3751cea87d95d16b3; digest verificado por Docker al descargar)
- imagen: python:3.12-alpine@sha256:c5ba3deef00c7758561a0f03c26f44f10589263d40e732b8748e0d1f38f6dd89 (id sha256:4076016aea5420e5c87eb52280edfc527ace1ce31dc1c9288b1cc4ef9a0e8905; digest verificado por Docker al descargar)
- imagen: maven:3.9.6-eclipse-temurin-21-alpine@sha256:9fca6934e64eff26860889804faecb71a522d04b4ebf05574140c71f8e9bc700 (id sha256:b112e8e8ae6e4208e073641bc966e6c9f49fdd32f17000b499b1f35cb1ab75b9; digest verificado por Docker al descargar)
- imagen: eclipse-temurin:21-jre-alpine@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70 (id sha256:5649cf3d873b6600b777ddff8a1a89cffbfa19755e5334bfb61136e8ec7a59a9; digest verificado por Docker al descargar)
- bases del Dockerfile.measurement fijadas por digest: sí
- Dockerfile.measurement usado: /home/runner/work/backend/backend/resultados-inv/inv-heap-r2-EXP-HEAP-192M-sin-informes/Dockerfile.fijado (sha256 11a2a24a9dfa62a56dcbeb51a2f67632ebcc2f4c60b57b587feaf8e64b020b1c)
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
| 0 | 0,2 | 0 | 0 |
| 1 | 1,3 | 1 | 0 |
| 2 | 0,2 | 0 | 0 |
| 3 | 1,3 | 1 | 0 |

- Núcleos físicos distintos: 2; SMT activo: sí.
- k6: CPU lógica(s) 2
- backend: CPU lógica(s) 1
- postgres: CPU lógica(s) 0
- muestreadores: CPU lógica(s) 2
- **k6 y postgres comparten núcleo físico** (hilos hermanos): el `cpuset` los separa por CPU lógica, no por núcleo físico; pueden interferirse.
- **k6 y muestreadores comparten núcleo físico** (hilos hermanos): el `cpuset` los separa por CPU lógica, no por núcleo físico; pueden interferirse.
- **postgres y muestreadores comparten núcleo físico** (hilos hermanos): el `cpuset` los separa por CPU lógica, no por núcleo físico; pueden interferirse.
- **Aislamiento físico no demostrado:** el fijado de CPU solo restringe dónde pueden ejecutarse estos procesos; el demonio de Docker, el agente del runner y el núcleo del sistema no están fijados, la caché L3 y el ancho de banda de memoria son compartidos, y el runner es una máquina virtual cuyo mapeo a CPU físicas del anfitrión no es visible desde dentro.

### Modo de afinidad: topologia
- El backend queda en la CPU lógica 1, y su hilo hermano 3 no recibe trabajo fijado: ningún proceso del experimento comparte con él su núcleo físico.
- Esa CPU hermana no está bloqueada: el demonio de Docker, el agente del runner y el núcleo del sistema pueden usarla.
- PostgreSQL, k6 y los muestreadores comparten el otro núcleo físico (hilos hermanos): la interferencia entre ellos no se elimina, solo se saca al backend de ese núcleo.
- El aislamiento físico completo sigue sin demostrarse: el runner es una máquina virtual y la caché L3 y la memoria se comparten.


## Generador de carga (k6)

**Criterio experimental, no un estándar:** el generador se marca como saturado si su CPU, medida como porcentaje de los núcleos a los que está fijado, supera 80 % en 3 muestras consecutivas (≈ 6 s). El umbral se cambia con `UMBRAL_GENERADOR_CPU_PCT`. Un nivel marcado «potencialmente no» no debe interpretarse como capacidad del backend sin repetirlo con un generador con más recursos.

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 817 | 11.3 | no aplica (ramping-vus) | 0.1 % | 0 | 1 / 2 / 3 | 0 % | 86 | 0 | **sí** |
| despues_100 | 100 / 100 | 3 976 | 13.1 | no aplica (ramping-vus) | 9.0 % | 0 | 1 / 2 / 3 | 0 % | 120 | 0 | **sí** |

Utilización media por CPU lógica del runner durante cada nivel (todas las CPU, estén o no fijadas; sirve para ver si una CPU hermana SMT de una fijada estaba ocupada):

- `despues_50`: CPU 0: 6 %, CPU 1: 19 %, CPU 2: 2 %, CPU 3: 1 %
- `despues_100`: CPU 0: 7 %, CPU 1: 42 %, CPU 2: 2 %, CPU 3: 1 %

Ningún nivel superó el criterio: no se detectó saturación del generador ni carga inferior a la prevista (esto no prueba que el generador no influya, solo que no alcanzó el umbral).

> `dropped_iterations` solo lo emiten los ejecutores de tasa de llegada; este escenario usa `ramping-vus`, por lo que no aplica. La carga efectivamente generada se verifica con los usuarios activos máximos (`vus`), las peticiones totales y por segundo, y los errores.

## Memoria de la JVM y del contenedor

- **Heap usado / comprometido** (contadores de la JVM, los mismos que lee `jstat -gc`, en vivo cada pocos segundos): memoria de objetos dentro del heap de la JVM, y la reservada por la JVM para ese heap.
- **Contenedor** (`memory.current` y pico del cgroup, `docker stats`): memoria total que ve el límite de Docker (heap + Metaspace + código + hilos + memoria directa + caché de archivos).
- **GC en vivo**: contadores leídos mientras corre el nivel; la primera cifra es lo ocurrido desde que empezó el muestreo y entre paréntesis el acumulado de la JVM. **GC registro**: pausas de toda la vida de la JVM leídas del archivo `-Xlog:gc` (incluye el arranque), por eso no coinciden con la ventana del muestreo.

| Etiqueta | Heap usado máx / media (MB) | Heap comprometido máx (MB) | Metaspace usado máx (MB) | Contenedor: memory.current máx (MB) | Contenedor: pico cgroup (MB) | Contenedor: docker stats pico (MB) | GC en vivo, durante el muestreo: young / full / tiempo (s) (acumulado de la JVM) | GC registro: pausas / total ms / máx ms / full |
|---|---|---|---|---|---|---|---|---|
| despues_50 | 166 / 139 | 192 | 112 | 430 | 436 | 425 | 66 / 5 / 1.30 (280 / 27 / 5.6) | 307 / 5 612 / 234.6 / 27 |
| despues_100 | 166 / 141 | 192 | 111 | 433 | 436 | 424 | 121 / 5 / 1.49 (337 / 26 / 5.7) | 363 / 5 752 / 266.7 / 26 |

Costo del muestreo en vivo: cada muestra tardó 53 ms de reloj de pared de media (máximo 236 ms). Es tiempo de pared, no CPU: el cálculo de la interferencia está en el flujo de calibración.

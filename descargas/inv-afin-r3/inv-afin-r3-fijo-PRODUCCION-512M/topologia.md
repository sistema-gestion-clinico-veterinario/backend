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

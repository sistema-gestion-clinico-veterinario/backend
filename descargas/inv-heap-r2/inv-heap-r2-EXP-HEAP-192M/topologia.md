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

# Investigaciones adicionales: heap, pool y afinidad

Cada celda resume las repeticiones como **mediana [mínimo–máximo]**. Las condiciones son las de la batería (misma semilla, mismo plan por nivel, base reiniciada por nivel).

## 1. Corridas incluidas

| Tipo | Configuración | Usuarios | Rep. | Pool | Máquina | Veredicto |
|---|---|---|---|---|---|---|
| afin | fijo-CAPACIDAD-2G | 100 | 1 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| afin | fijo-CAPACIDAD-2G | 100 | 2 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | AGUANTA |
| afin | fijo-CAPACIDAD-2G | 100 | 3 | 10 | AMD EPYC 7763 64-Core Processor; hilos p | AGUANTA |
| afin | fijo-PRODUCCION-512M | 100 | 1 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| afin | fijo-PRODUCCION-512M | 100 | 2 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | FALLA |
| afin | fijo-PRODUCCION-512M | 100 | 3 | 10 | AMD EPYC 7763 64-Core Processor; hilos p | FALLA |
| afin | topologia-CAPACIDAD-2G | 100 | 1 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| afin | topologia-CAPACIDAD-2G | 100 | 2 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | AGUANTA |
| afin | topologia-CAPACIDAD-2G | 100 | 3 | 10 | AMD EPYC 7763 64-Core Processor; hilos p | AGUANTA |
| afin | topologia-PRODUCCION-512M | 100 | 1 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| afin | topologia-PRODUCCION-512M | 100 | 2 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | FALLA |
| afin | topologia-PRODUCCION-512M | 100 | 3 | 10 | AMD EPYC 7763 64-Core Processor; hilos p | FALLA |
| heap | EXP-HEAP-192M | 50 | 1 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | FALLA |
| heap | EXP-HEAP-192M | 50 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-192M | 50 | 3 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-192M | 100 | 1 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | FALLA |
| heap | EXP-HEAP-192M | 100 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-192M | 100 | 3 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-192M-citas6000 | 50 | 1 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | AGUANTA |
| heap | EXP-HEAP-192M-citas6000 | 50 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| heap | EXP-HEAP-192M-citas6000 | 50 | 3 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| heap | EXP-HEAP-192M-citas6000 | 100 | 1 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | AGUANTA |
| heap | EXP-HEAP-192M-citas6000 | 100 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| heap | EXP-HEAP-192M-citas6000 | 100 | 3 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| heap | EXP-HEAP-192M-nmt | 50 | 4 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-192M-nmt | 100 | 4 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-192M-sin-informes | 50 | 1 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | AGUANTA |
| heap | EXP-HEAP-192M-sin-informes | 50 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| heap | EXP-HEAP-192M-sin-informes | 50 | 3 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| heap | EXP-HEAP-192M-sin-informes | 100 | 1 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | AGUANTA |
| heap | EXP-HEAP-192M-sin-informes | 100 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| heap | EXP-HEAP-192M-sin-informes | 100 | 3 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| heap | EXP-HEAP-256M | 50 | 1 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | AGUANTA |
| heap | EXP-HEAP-256M | 50 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| heap | EXP-HEAP-256M | 50 | 3 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| heap | EXP-HEAP-256M | 100 | 1 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | FALLA |
| heap | EXP-HEAP-256M | 100 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-256M | 100 | 3 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-256M-nmt | 50 | 4 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| heap | EXP-HEAP-256M-nmt | 100 | 4 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-288M | 50 | 1 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | FALLA |
| heap | EXP-HEAP-288M | 50 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-288M | 50 | 3 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-288M | 100 | 1 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | FALLA |
| heap | EXP-HEAP-288M | 100 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-288M | 100 | 3 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-288M-nmt | 50 | 4 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-288M-nmt | 100 | 4 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| pool | POOL-10 | 250 | 1 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-10 | 250 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-10 | 250 | 3 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-20 | 250 | 1 | 20 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-20 | 250 | 2 | 20 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-20 | 250 | 3 | 20 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-40 | 250 | 1 | 40 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-40 | 250 | 2 | 40 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-40 | 250 | 3 | 40 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-80 | 250 | 1 | 80 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-80 | 250 | 2 | 80 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-80 | 250 | 3 | 80 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |

## 2. Investigación del heap de producción (contenedor de 512 MB)

### Resultado y estabilidad

| Configuración | Usuarios | Reps. | AGUANTA / DEGRADADO / FALLA | Peticiones/s | Errores (%) | p95 (ms) | p99 (ms) | OOMKilled / reinicios / OutOfMemoryError |
|---|---|---|---|---|---|---|---|---|
| EXP-HEAP-192M | 100 | 3 | 0 / 0 / 3 | 2.0 [2.0–2.0] | 79.7 [79.5–81.1] | 30 001 [30 001–30 001] | 30 001 [30 001–30 001] | 0 / 0 / 3 |
| EXP-HEAP-192M | 50 | 3 | 0 / 0 / 3 | 1.4 [1.3–3.6] | 61.9 [20.1–67.6] | 30 001 [30 001–30 001] | 30 001 [30 001–30 001] | 0 / 0 / 3 |
| EXP-HEAP-192M-citas6000 | 100 | 3 | 3 / 0 / 0 | 20.5 [20.4–20.6] | 0.1 [0.1–0.3] | 200 [120–252] | 388 [375–444] | 0 / 0 / 0 |
| EXP-HEAP-192M-citas6000 | 50 | 3 | 3 / 0 / 0 | 11.2 [11.2–11.2] | 0.1 [0.1–0.1] | 65 [53–92] | 358 [345–400] | 0 / 0 / 0 |
| EXP-HEAP-192M-nmt | 100 | 1 | 0 / 0 / 1 | 2.0 | 81.4 | 30 001 | 30 001 | 0 / 0 / 1 |
| EXP-HEAP-192M-nmt | 50 | 1 | 0 / 0 / 1 | 2.4 | 31.6 | 30 001 | 30 001 | 0 / 0 / 1 |
| EXP-HEAP-192M-sin-informes | 100 | 3 | 3 / 0 / 0 | 20.7 [20.7–20.8] | 0.2 [0.2–0.2] | 59 [56–81] | 345 [342–365] | 0 / 0 / 0 |
| EXP-HEAP-192M-sin-informes | 50 | 3 | 3 / 0 / 0 | 11.3 [11.3–11.3] | 0.2 [0.2–0.3] | 55 [55–64] | 343 [341–366] | 0 / 0 / 0 |
| EXP-HEAP-256M | 100 | 3 | 0 / 0 / 3 | 10.5 [9.9–17.6] | 12.1 [9.9–12.6] | 30 001 [1 674–30 001] | 30 001 [16 279–30 001] | 1 / 0 / 3 |
| EXP-HEAP-256M | 50 | 3 | 3 / 0 / 0 | 11.1 [11.0–11.2] | 0.3 [0.2–0.3] | 113 [84–118] | 413 [361–426] | 0 / 0 / 0 |
| EXP-HEAP-256M-nmt | 100 | 1 | 0 / 0 / 1 | 18.4 | 13.1 | 546 | 13 947 | 1 / 0 / 1 |
| EXP-HEAP-256M-nmt | 50 | 1 | 1 / 0 / 0 | 11.1 | 0.3 | 101 | 437 | 0 / 0 / 0 |
| EXP-HEAP-288M | 100 | 3 | 0 / 0 / 3 | 5.1 [4.8–5.1] | 40.9 [39.9–41.3] | 353 [329–373] | 1 199 [418–1 315] | 3 / 0 / 0 |
| EXP-HEAP-288M | 50 | 3 | 0 / 0 / 3 | 5.1 [4.8–5.2] | 38.7 [38.6–41.8] | 349 [331–358] | 1 196 [503–1 310] | 3 / 0 / 0 |
| EXP-HEAP-288M-nmt | 100 | 1 | 0 / 0 / 1 | 4.8 | 38.7 | 337 | 484 | 1 / 0 / 0 |
| EXP-HEAP-288M-nmt | 50 | 1 | 0 / 0 / 1 | 4.8 | 39.2 | 339 | 1 037 | 1 / 0 / 0 |

### Memoria: heap, Metaspace, memoria fuera del heap y contenedor

| Configuración | Usuarios | Heap usado máx. (MB) | Heap tras GC en el nivel (MB) | Metaspace máx. (MB) | RssAnon máx. (MB) | Fuera del heap = RssAnon − heap comprometido, máx. (MB) | Hilos del sistema máx. | Pilas del núcleo (MB) | Contenedor: pico cgroup (MB) |
|---|---|---|---|---|---|---|---|---|---|
| EXP-HEAP-192M | 100 | 186 [186–186] | 185 [185–185] | 109 [109–109] | 416 [414–418] | 224 [222–226] | 64 [61–71] | 1.3 [1.2–1.5] | 432 [429–432] de 512 |
| EXP-HEAP-192M | 50 | 186 [186–186] | 185 [185–185] | 109 [109–110] | 419 [413–426] | 227 [221–234] | 71 [64–80] | 1.4 [1.3–1.4] | 432 [432–438] de 512 |
| EXP-HEAP-192M-citas6000 | 100 | 185 [185–185] | 184 [180–185] | 112 [112–112] | 421 [421–424] | 229 [229–232] | 40 [40–41] | 0.7 [0.7–0.7] | 433 [432–435] de 512 |
| EXP-HEAP-192M-citas6000 | 50 | 184 [183–184] | 184 [184–185] | 112 [111–112] | 420 [417–420] | 228 [225–228] | 40 [39–40] | 0.7 [0.6–0.7] | 430 [428–431] de 512 |
| EXP-HEAP-192M-nmt | 100 | 186 | 185 | 109 | 424 | 232 | 67 | 1.5 | 444 de 512 |
| EXP-HEAP-192M-nmt | 50 | 186 | 185 | 110 | 430 | 238 | 70 | 1.3 | 447 de 512 |
| EXP-HEAP-192M-sin-informes | 100 | 165 [162–167] | 116 [116–117] | 111 [111–111] | 425 [424–425] | 233 [232–233] | 40 [40–41] | 0.7 [0.7–0.7] | 436 [435–436] de 512 |
| EXP-HEAP-192M-sin-informes | 50 | 166 [166–167] | 117 [117–121] | 111 [111–112] | 422 [421–423] | 230 [229–231] | 40 [40–40] | 0.7 [0.7–0.7] | 435 [434–435] de 512 |
| EXP-HEAP-256M | 100 | 248 [248–248] | 247 [247–247] | 112 [112–112] | 493 [490–493] | 237 [234–237] | 118 [102–127] | 2.0 [1.9–2.2] | 512 [508–512] de 512 |
| EXP-HEAP-256M | 50 | 245 [244–247] | 242 [240–247] | 112 [112–112] | 484 [484–484] | 228 [228–228] | 40 [40–40] | 0.7 [0.7–0.7] | 495 [495–495] de 512 |
| EXP-HEAP-256M-nmt | 100 | 247 | 247 | 112 | 501 | 245 | 127 | 2.0 | 512 de 512 |
| EXP-HEAP-256M-nmt | 50 | 246 | 241 | 112 | 494 | 238 | 40 | 0.7 | 505 de 512 |
| EXP-HEAP-288M | 100 | 242 [184–256] | 265 [260–278] | 110 [106–110] | 494 [489–499] | 206 [201–211] | 37 [34–37] | 0.6 [0.6–0.7] | 512 [499–512] de 512 |
| EXP-HEAP-288M | 50 | 241 [206–263] | 276 [263–278] | 110 [109–110] | 489 [489–495] | 201 [201–207] | 37 [34–37] | 0.6 [0.6–0.7] | 505 [500–512] de 512 |
| EXP-HEAP-288M-nmt | 100 | 173 | 171 | 106 | 491 | 203 | 35 | 0.6 | 501 de 512 |
| EXP-HEAP-288M-nmt | 50 | 217 | 193 | 109 | 469 | 181 | 34 | 0.6 | 512 de 512 |

### GC y primer fallo

| Configuración | Usuarios | Pausas de GC por minuto | Pausas completas | Tiempo en GC (%) | Pausa máxima (ms) | Primer tramo de 15 s con más de 5 % de errores (s) | Informes clínicos: p95 / p99 (ms) y con error |
|---|---|---|---|---|---|---|---|
| EXP-HEAP-192M | 100 | 101.0 [47.2–163.0] | 585 [270–945] | 46.1 [23.1–83.9] | 404 [348–542] | 45 [45–45] | 30 001 [30 001–30 001] / 30 001 [30 001–30 001]; 3 de 3 |
| EXP-HEAP-192M | 50 | 96.8 [43.3–224.8] | 452 [206–1 104] | 39.6 [21.7–94.2] | 378 [316–408] | 45 [37–45] | 30 001 [30 000–30 001] / 30 001 [30 000–30 001]; 6 de 6 |
| EXP-HEAP-192M-citas6000 | 100 | 56.7 [49.8–56.9] | 133 [94–135] | 9.7 [7.5–10.3] | 308 [300–322] | 99 [99–100] | 2 057 [1 942–2 185] / 2 363 [2 077–2 829]; 0 de 66 |
| EXP-HEAP-192M-citas6000 | 50 | 24.8 [24.8–25.1] | 30 [29–30] | 3.0 [2.8–3.0] | 285 [284–288] | 99 [98–99] | 2 369 [2 112–2 872] / 2 369 [2 112–2 872]; 0 de 24 |
| EXP-HEAP-192M-nmt | 100 | 186.9 | 1 085 | 95.2 | 407 | 45 | 30 001 / 30 001; 1 de 1 |
| EXP-HEAP-192M-nmt | 50 | 163.6 | 793 | 82.7 | 373 | 45 | 30 001 / 30 001; 4 de 4 |
| EXP-HEAP-192M-sin-informes | 100 | 29.3 [29.3–29.5] | 6 [6–6] | 0.6 [0.6–0.6] | 249 [249–261] | 52 [33–52] | sin datos / sin datos; 0 de 0 |
| EXP-HEAP-192M-sin-informes | 50 | 16.7 [16.6–16.9] | 5 [5–5] | 0.6 [0.5–0.6] | 269 [244–290] | 52 [33–52] | sin datos / sin datos; 0 de 0 |
| EXP-HEAP-256M | 100 | 56.7 [54.3–92.4] | 252 [119–472] | 26.9 [21.3–49.0] | 478 [468–492] | 33 [23–52] | 30 001 [30 001–30 001] / 30 001 [30 001–30 001]; 15 de 36 |
| EXP-HEAP-256M | 50 | 28.0 [27.5–28.1] | 48 [47–50] | 6.5 [5.8–6.5] | 412 [382–457] | 34 [24–52] | 5 361 [4 964–5 679] / 5 361 [4 964–5 679]; 0 de 24 |
| EXP-HEAP-256M-nmt | 100 | 54.0 | 104 | 19.4 | 473 | 33 | 22 910 / 22 910; 5 de 12 |
| EXP-HEAP-256M-nmt | 50 | 27.4 | 48 | 6.6 | 455 | 33 | 5 350 / 5 350; 0 de 8 |
| EXP-HEAP-288M | 100 | 17.1 [10.8–17.1] | 4 [1–4] | 3.6 [1.0–3.7] | 389 [220–403] | 20 [17–20] | 0 [0–4 539] / 0 [0–4 539]; 2 de 3 |
| EXP-HEAP-288M | 50 | 14.3 [12.6–17.1] | 3 [1–4] | 2.7 [1.2–3.6] | 371 [340–393] | 19 [17–20] | 1 [0–4 341] / 1 [0–4 341]; 2 de 3 |
| EXP-HEAP-288M-nmt | 100 | 9.2 | 1 | 1.0 | 274 | 17 | 2 / 2; 1 de 1 |
| EXP-HEAP-288M-nmt | 50 | 9.5 | 1 | 0.9 | 204 | 17 | 1 873 / 1 873; 1 de 1 |

### Memoria nativa según NMT (una corrida por configuración, con el sobrecosto del propio NMT)

| Configuración | Usuarios | Total comprometido (MB) | Java Heap | Metaspace | Symbol | Code | Class | Native Memory Tracking | Thread | Arena Chunk | GC |
|---|---|---|---|---|---|---|---|---|---|---|---|
| EXP-HEAP-256M-nmt | 50 | 465 | 256 | 98 | 41 | 27 | 21 | 7 | 1 | 1 | 1 |

## 3. Investigación del pool de conexiones (CAPACIDAD-2G, 250 usuarios)

| Pool | Reps. | AGUANTA / FALLA | Peticiones/s | Errores (%) | p95 (ms) | p99 (ms) | Usuarios cuando los errores superan 5 % | CPU backend (%) | CPU PostgreSQL (%) |
|---|---|---|---|---|---|---|---|---|---|
| 10 | 3 | 3 / 0 | 44.4 [44.4–44.6] | 0.1 [0.1–0.4] | 263 [230–264] | 776 [735–825] | 151 | 62 [62–63] | 39 [35–40] |
| 20 | 3 | 3 / 0 | 44.4 [44.4–44.8] | 0.1 [0.1–0.5] | 224 [193–228] | 783 [706–821] | sin datos | 62 [58–65] | 37 [36–40] |
| 40 | 3 | 3 / 0 | 44.6 [44.6–44.6] | 0.1 [0.1–0.4] | 220 [195–271] | 799 [743–1 184] | sin datos | 60 [59–67] | 38 [36–40] |
| 80 | 3 | 3 / 0 | 44.4 [44.4–44.7] | 0.1 [0.1–0.1] | 252 [212–258] | 855 [777–866] | sin datos | 62 [59–63] | 40 [36–40] |

### Conexiones (PostgreSQL y Hikari)

| Pool | Conexiones máx. en PostgreSQL | Activas: media / máx. | Inactivas: media | Inactivas en transacción: máx. | Esperan un lock: máx. | Transacción más antigua: máx. (s) | Solicitudes esperando (Hikari): máx. | Esperas agotadas (30 s) |
|---|---|---|---|---|---|---|---|---|
| 10 | 12 [11–12] | 0.6 [0.5–0.6] / 6 [5–8] | 7.1 [6.8–7.1] | 9 [9–10] | 0 [0–0] | 5.3 [4.7–6.6] | 28 [19–31] | 0 [0–0] |
| 20 | 22 [21–22] | 0.6 [0.6–0.6] / 8 [6–10] | 12.4 [12.2–12.9] | 13 [10–13] | 0 [0–0] | 6.7 [5.9–7.1] | 8 [7–17] | 0 [0–0] |
| 40 | 32 [25–36] | 0.8 [0.6–0.8] / 9 [6–14] | 15.6 [13.5–17.1] | 14 [11–22] | 0 [0–0] | 7.5 [6.4–14.7] | 3 [2–3] | 0 [0–0] |
| 80 | 30 [29–35] | 0.8 [0.6–0.9] / 8 [7–15] | 15.6 [14.9–15.8] | 13 [11–22] | 0 [0–1] | 7.6 [5.5–10.3] | 3 [2–6] | 0 [0–0] |

Volcados de hilos disponibles (se analizan con `analizar_hilos.py`): pool 10, repetición 1: 4; pool 10, repetición 2: 4; pool 10, repetición 3: 4; pool 20, repetición 1: 4; pool 20, repetición 2: 4; pool 20, repetición 3: 4; pool 40, repetición 1: 4; pool 40, repetición 2: 4; pool 40, repetición 3: 4; pool 80, repetición 1: 4; pool 80, repetición 2: 4; pool 80, repetición 3: 4.

## 4. Comparación de afinidad de CPU (100 usuarios)

| Perfil | Modo | Reps. | AGUANTA / FALLA | Peticiones/s | Errores (%) | p95 (ms) | p99 (ms) | CPU backend (%) | CPU PostgreSQL (%) | Heap tras GC (MB) | Primer tramo con más de 5 % de errores (s) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| CAPACIDAD-2G | fijo | 3 | 3 / 0 | 20.5 [20.4–20.6] | 0.2 [0.1–0.3] | 129 [81–160] | 719 [417–801] | 55 [44–55] | 20 [12–20] | 433 [432–473] | 24 [24–33] |
| CAPACIDAD-2G | topologia | 3 | 3 / 0 | 20.7 [20.5–20.7] | 0.1 [0.1–0.2] | 127 [81–147] | 747 [423–771] | 54 [42–58] | 20 [12–21] | 424 [423–515] | 24 [24–66] |
| PRODUCCION-512M | fijo | 3 | 0 / 3 | 2.2 [2.1–4.5] | 74.0 [35.2–76.6] | 30 001 [30 001–30 001] | 30 001 [30 001–30 001] | 58 [23–97] | 1 [1–3] | 185 [185–185] | 45 [28–45] |
| PRODUCCION-512M | topologia | 3 | 0 / 3 | 2.1 [2.0–2.2] | 76.4 [75.4–81.9] | 30 001 [30 001–30 001] | 30 001 [30 001–30 001] | 32 [26–57] | 1 [1–1] | 185 [185–185] | 45 [29–45] |

### Diferencia por repetición (topología menos fijo, misma máquina y misma repetición)

| Perfil | Rep. | Δ peticiones/s | Δ errores (puntos %) | Δ p95 (ms) | Δ p99 (ms) | Δ CPU backend (puntos) | Δ CPU PostgreSQL (puntos) | Máquina |
|---|---|---|---|---|---|---|---|---|
| CAPACIDAD-2G | 1 | 0.1 | 0.0 | 1 | 5 | -2 | 0 | Intel(R) Xeon(R) 6975P-C; hilos  |
| CAPACIDAD-2G | 2 | -0.0 | -0.1 | -2 | 52 | -1 | 1 | AMD EPYC 9V74 80-Core Processor; |
| CAPACIDAD-2G | 3 | 0.3 | -0.0 | -14 | -55 | 3 | -0 | AMD EPYC 7763 64-Core Processor; |
| PRODUCCION-512M | 1 | -2.5 | 46.7 | 0 | -0 | -26 | -2 | Intel(R) Xeon(R) 6975P-C; hilos  |
| PRODUCCION-512M | 2 | 0.0 | -0.2 | 0 | -0 | 33 | 1 | AMD EPYC 9V74 80-Core Processor; |
| PRODUCCION-512M | 3 | -0.0 | 1.4 | 0 | -0 | -71 | -0 | AMD EPYC 7763 64-Core Processor; |

## 5. Límites

- Un laboratorio: PostgreSQL local con una sola CPU lógica, generador en el mismo equipo, máquinas de GitHub que pueden cambiar entre repeticiones y tres repeticiones.
- Los perfiles `EXP-HEAP-*` son experimentales: no se modificó el Dockerfile ni la configuración de producción.
- El monitoreo en vivo cada 3 s consume una fracción de CPU del contenedor (estimada en cerca de 1 % de una CPU) y puede no captar picos más cortos.
- La memoria fuera del heap se calcula como RssAnon menos el heap comprometido: es una aproximación (incluye Metaspace, código, pilas, memoria directa y arenas de malloc).


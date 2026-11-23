# Investigaciones adicionales: heap, pool y afinidad

Cada celda resume las repeticiones como **mediana [mínimo–máximo]**. Las condiciones son las de la batería (misma semilla, mismo plan por nivel, base reiniciada por nivel).

## 1. Corridas incluidas

| Tipo | Configuración | Usuarios | Rep. | Pool | Máquina | Veredicto |
|---|---|---|---|---|---|---|
| afin | fijo-CAPACIDAD-2G | 100 | 1 | 10 | AMD EPYC 7763 64-Core Processor; hilos p | AGUANTA |
| afin | fijo-CAPACIDAD-2G | 100 | 2 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | AGUANTA |
| afin | fijo-CAPACIDAD-2G | 100 | 3 | 10 | INTEL(R) XEON(R) PLATINUM 8573C; hilos p | AGUANTA |
| afin | fijo-PRODUCCION-512M | 100 | 1 | 10 | AMD EPYC 7763 64-Core Processor; hilos p | FALLA |
| afin | fijo-PRODUCCION-512M | 100 | 2 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | FALLA |
| afin | fijo-PRODUCCION-512M | 100 | 3 | 10 | INTEL(R) XEON(R) PLATINUM 8573C; hilos p | FALLA |
| afin | topologia-CAPACIDAD-2G | 100 | 1 | 10 | AMD EPYC 7763 64-Core Processor; hilos p | AGUANTA |
| afin | topologia-CAPACIDAD-2G | 100 | 2 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | AGUANTA |
| afin | topologia-CAPACIDAD-2G | 100 | 3 | 10 | INTEL(R) XEON(R) PLATINUM 8573C; hilos p | AGUANTA |
| afin | topologia-PRODUCCION-512M | 100 | 1 | 10 | AMD EPYC 7763 64-Core Processor; hilos p | FALLA |
| afin | topologia-PRODUCCION-512M | 100 | 2 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | FALLA |
| afin | topologia-PRODUCCION-512M | 100 | 3 | 10 | INTEL(R) XEON(R) PLATINUM 8573C; hilos p | FALLA |
| heap | EXP-HEAP-192M | 50 | 1 | 10 | AMD EPYC 9V45 96-Core Processor; hilos p | FALLA |
| heap | EXP-HEAP-192M | 50 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-192M | 50 | 3 | 10 | INTEL(R) XEON(R) PLATINUM 8573C; hilos p | FALLA |
| heap | EXP-HEAP-192M | 100 | 1 | 10 | AMD EPYC 9V45 96-Core Processor; hilos p | FALLA |
| heap | EXP-HEAP-192M | 100 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-192M | 100 | 3 | 10 | INTEL(R) XEON(R) PLATINUM 8573C; hilos p | FALLA |
| heap | EXP-HEAP-192M-citas6000 | 50 | 1 | 10 | AMD EPYC 9V45 96-Core Processor; hilos p | AGUANTA |
| heap | EXP-HEAP-192M-citas6000 | 50 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| heap | EXP-HEAP-192M-citas6000 | 50 | 3 | 10 | INTEL(R) XEON(R) PLATINUM 8573C; hilos p | AGUANTA |
| heap | EXP-HEAP-192M-citas6000 | 100 | 1 | 10 | AMD EPYC 9V45 96-Core Processor; hilos p | FALLA |
| heap | EXP-HEAP-192M-citas6000 | 100 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-192M-citas6000 | 100 | 3 | 10 | INTEL(R) XEON(R) PLATINUM 8573C; hilos p | FALLA |
| heap | EXP-HEAP-192M-nmt | 50 | 4 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-192M-nmt | 100 | 4 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-192M-sin-informes | 50 | 1 | 10 | AMD EPYC 9V45 96-Core Processor; hilos p | AGUANTA |
| heap | EXP-HEAP-192M-sin-informes | 50 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| heap | EXP-HEAP-192M-sin-informes | 50 | 3 | 10 | INTEL(R) XEON(R) PLATINUM 8573C; hilos p | AGUANTA |
| heap | EXP-HEAP-192M-sin-informes | 100 | 1 | 10 | AMD EPYC 9V45 96-Core Processor; hilos p | FALLA |
| heap | EXP-HEAP-192M-sin-informes | 100 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-192M-sin-informes | 100 | 3 | 10 | INTEL(R) XEON(R) PLATINUM 8573C; hilos p | FALLA |
| heap | EXP-HEAP-256M | 50 | 1 | 10 | AMD EPYC 9V45 96-Core Processor; hilos p | AGUANTA |
| heap | EXP-HEAP-256M | 50 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| heap | EXP-HEAP-256M | 50 | 3 | 10 | INTEL(R) XEON(R) PLATINUM 8573C; hilos p | AGUANTA |
| heap | EXP-HEAP-256M | 100 | 1 | 10 | AMD EPYC 9V45 96-Core Processor; hilos p | FALLA |
| heap | EXP-HEAP-256M | 100 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-256M | 100 | 3 | 10 | INTEL(R) XEON(R) PLATINUM 8573C; hilos p | FALLA |
| heap | EXP-HEAP-256M-nmt | 50 | 4 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| heap | EXP-HEAP-256M-nmt | 100 | 4 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-288M | 50 | 1 | 10 | AMD EPYC 9V45 96-Core Processor; hilos p | FALLA |
| heap | EXP-HEAP-288M | 50 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-288M | 50 | 3 | 10 | INTEL(R) XEON(R) PLATINUM 8573C; hilos p | FALLA |
| heap | EXP-HEAP-288M | 100 | 1 | 10 | AMD EPYC 9V45 96-Core Processor; hilos p | FALLA |
| heap | EXP-HEAP-288M | 100 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-288M | 100 | 3 | 10 | INTEL(R) XEON(R) PLATINUM 8573C; hilos p | FALLA |
| heap | EXP-HEAP-288M-nmt | 50 | 4 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| heap | EXP-HEAP-288M-nmt | 100 | 4 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | FALLA |
| pool | POOL-10 | 250 | 1 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-10 | 250 | 2 | 10 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-10 | 250 | 3 | 10 | AMD EPYC 9V74 80-Core Processor; hilos p | AGUANTA |
| pool | POOL-20 | 250 | 1 | 20 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-20 | 250 | 2 | 20 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-20 | 250 | 3 | 20 | AMD EPYC 9V74 80-Core Processor; hilos p | AGUANTA |
| pool | POOL-40 | 250 | 1 | 40 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-40 | 250 | 2 | 40 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-40 | 250 | 3 | 40 | AMD EPYC 9V74 80-Core Processor; hilos p | AGUANTA |
| pool | POOL-80 | 250 | 1 | 80 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-80 | 250 | 2 | 80 | Intel(R) Xeon(R) 6975P-C; hilos por núcl | AGUANTA |
| pool | POOL-80 | 250 | 3 | 80 | AMD EPYC 9V74 80-Core Processor; hilos p | AGUANTA |

## 2. Investigación del heap de producción (contenedor de 512 MB)

### Resultado y estabilidad

| Configuración | Usuarios | Reps. | AGUANTA / DEGRADADO / FALLA | Peticiones/s | Errores (%) | p95 (ms) | p99 (ms) | OOMKilled / reinicios / OutOfMemoryError |
|---|---|---|---|---|---|---|---|---|
| EXP-HEAP-192M | 100 | 3 | 0 / 0 / 3 | 2.0 [2.0–2.1] | 80.6 [76.6–81.0] | 30 001 [30 001–30 001] | 30 001 [30 001–30 001] | 0 / 0 / 3 |
| EXP-HEAP-192M | 50 | 3 | 0 / 0 / 3 | 1.4 [1.3–1.4] | 64.2 [63.1–67.2] | 30 001 [30 001–30 001] | 30 001 [30 001–30 001] | 0 / 0 / 3 |
| EXP-HEAP-192M-citas6000 | 100 | 3 | 0 / 0 / 3 | 12.8 [12.8–12.9] | 9.1 [9.1–9.1] | 392 [299–785] | 1 243 [874–3 075] | 0 / 0 / 0 |
| EXP-HEAP-192M-citas6000 | 50 | 3 | 3 / 0 / 0 | 11.2 [11.1–11.2] | 0.0 [0.0–0.1] | 59 [55–73] | 364 [306–475] | 0 / 0 / 0 |
| EXP-HEAP-192M-nmt | 100 | 1 | 0 / 0 / 1 | 2.0 | 80.5 | 30 001 | 30 001 | 0 / 0 / 1 |
| EXP-HEAP-192M-nmt | 50 | 1 | 0 / 0 / 1 | 1.3 | 67.5 | 30 001 | 30 001 | 0 / 0 / 1 |
| EXP-HEAP-192M-sin-informes | 100 | 3 | 0 / 0 / 3 | 13.0 [13.0–13.1] | 8.9 [8.9–9.0] | 350 [274–491] | 1 077 [694–1 891] | 0 / 0 / 0 |
| EXP-HEAP-192M-sin-informes | 50 | 3 | 3 / 0 / 0 | 11.3 [11.3–11.4] | 0.1 [0.1–0.1] | 53 [49–70] | 331 [279–437] | 0 / 0 / 0 |
| EXP-HEAP-256M | 100 | 3 | 0 / 0 / 3 | 11.2 [7.3–12.9] | 9.6 [9.1–19.1] | 608 [453–30 001] | 3 202 [1 798–30 001] | 0 / 0 / 2 |
| EXP-HEAP-256M | 50 | 3 | 3 / 0 / 0 | 11.1 [11.0–11.1] | 0.2 [0.2–0.4] | 100 [96–148] | 434 [395–495] | 0 / 0 / 0 |
| EXP-HEAP-256M-nmt | 100 | 1 | 0 / 0 / 1 | 12.5 | 9.1 | 500 | 2 702 | 0 / 0 / 1 |
| EXP-HEAP-256M-nmt | 50 | 1 | 1 / 0 / 0 | 11.1 | 0.2 | 83 | 367 | 0 / 0 / 0 |
| EXP-HEAP-288M | 100 | 3 | 0 / 0 / 3 | 5.1 [4.9–5.2] | 39.4 [38.6–41.8] | 345 [282–444] | 1 179 [1 175–1 325] | 3 / 0 / 0 |
| EXP-HEAP-288M | 50 | 3 | 0 / 0 / 3 | 5.2 [5.1–5.2] | 39.3 [38.6–40.5] | 348 [297–468] | 1 252 [1 127–1 886] | 3 / 0 / 0 |
| EXP-HEAP-288M-nmt | 100 | 1 | 0 / 0 / 1 | 4.7 | 40.6 | 335 | 591 | 1 / 0 / 0 |
| EXP-HEAP-288M-nmt | 50 | 1 | 0 / 0 / 1 | 4.6 | 40.8 | 335 | 973 | 1 / 0 / 0 |

### Memoria: heap, Metaspace, memoria fuera del heap y contenedor

| Configuración | Usuarios | Heap usado máx. (MB) | Heap tras GC en el nivel (MB) | Metaspace máx. (MB) | RssAnon máx. (MB) | Fuera del heap = RssAnon − heap comprometido, máx. (MB) | Hilos del sistema máx. | Pilas del núcleo (MB) | Contenedor: pico cgroup (MB) |
|---|---|---|---|---|---|---|---|---|---|
| EXP-HEAP-192M | 100 | 186 [186–186] | 185 [185–185] | 109 [109–109] | 419 [419–419] | 227 [227–227] | 67 [64–71] | 1.3 [1.2–1.4] | 433 [432–434] de 512 |
| EXP-HEAP-192M | 50 | 186 [186–186] | 185 [185–185] | 109 [109–109] | 414 [413–418] | 222 [221–226] | 67 [60–67] | 1.3 [1.2–1.3] | 431 [427–432] de 512 |
| EXP-HEAP-192M-citas6000 | 100 | 185 [182–185] | 185 [185–185] | 112 [112–112] | 420 [420–421] | 228 [228–229] | 40 [40–45] | 0.7 [0.7–0.8] | 432 [431–437] de 512 |
| EXP-HEAP-192M-citas6000 | 50 | 185 [180–185] | 185 [182–185] | 112 [112–112] | 421 [417–421] | 229 [225–229] | 40 [39–40] | 0.7 [0.6–0.7] | 432 [430–432] de 512 |
| EXP-HEAP-192M-nmt | 100 | 186 | 185 | 109 | 427 | 235 | 63 | 1.2 | 440 de 512 |
| EXP-HEAP-192M-nmt | 50 | 186 | 185 | 109 | 426 | 234 | 53 | 1.0 | 440 de 512 |
| EXP-HEAP-192M-sin-informes | 100 | 167 [166–168] | 121 [116–121] | 111 [111–111] | 425 [423–425] | 233 [231–233] | 40 [40–41] | 0.7 [0.7–0.7] | 436 [435–436] de 512 |
| EXP-HEAP-192M-sin-informes | 50 | 166 [164–171] | 121 [120–121] | 111 [111–112] | 422 [422–425] | 230 [230–233] | 40 [39–40] | 0.7 [0.6–0.7] | 436 [433–436] de 512 |
| EXP-HEAP-256M | 100 | 247 [247–248] | 247 [236–247] | 112 [112–112] | 484 [484–497] | 228 [228–241] | 47 [45–136] | 0.8 [0.7–2.4] | 496 [495–512] de 512 |
| EXP-HEAP-256M | 50 | 246 [237–247] | 242 [239–244] | 112 [112–112] | 484 [484–484] | 228 [228–228] | 40 [40–40] | 0.7 [0.7–0.7] | 495 [495–495] de 512 |
| EXP-HEAP-256M-nmt | 100 | 247 | 247 | 112 | 494 | 238 | 45 | 0.8 | 505 de 512 |
| EXP-HEAP-256M-nmt | 50 | 246 | 239 | 112 | 494 | 238 | 40 | 0.7 | 505 de 512 |
| EXP-HEAP-288M | 100 | 267 [253–275] | 260 [258–260] | 110 [109–110] | 491 [489–491] | 203 [203–204] | 37 [37–37] | 0.6 [0.6–0.6] | 512 [500–512] de 512 |
| EXP-HEAP-288M | 50 | 238 [189–267] | 263 [251–264] | 110 [110–110] | 492 [488–499] | 204 [200–211] | 37 [37–37] | 0.6 [0.6–0.7] | 512 [510–512] de 512 |
| EXP-HEAP-288M-nmt | 100 | 244 | 252 | 109 | 499 | 220 | 37 | 0.6 | 510 de 512 |
| EXP-HEAP-288M-nmt | 50 | 191 | 170 | 106 | 494 | 206 | 34 | 0.6 | 505 de 512 |

### GC y primer fallo

| Configuración | Usuarios | Pausas de GC por minuto | Pausas completas | Tiempo en GC (%) | Pausa máxima (ms) | Primer tramo de 15 s con más de 5 % de errores (s) | Informes clínicos: p95 / p99 (ms) y con error |
|---|---|---|---|---|---|---|---|
| EXP-HEAP-192M | 100 | 60.9 [35.8–79.7] | 347 [203–460] | 31.7 [19.0–36.5] | 397 [352–410] | 45 [45–45] | 30 001 [30 001–30 001] / 30 001 [30 001–30 001]; 3 de 3 |
| EXP-HEAP-192M | 50 | 56.9 [33.1–175.5] | 274 [159–857] | 27.9 [17.1–84.9] | 383 [377–414] | 16 [16–45] | 30 001 [30 001–30 001] / 30 001 [30 001–30 001]; 3 de 3 |
| EXP-HEAP-192M-citas6000 | 100 | 34.4 [33.7–35.2] | 46 [44–47] | 3.9 [3.6–4.1] | 319 [298–329] | 55 [55–56] | 2 169 [2 148–5 773] / 2 169 [2 148–5 773]; 0 de 36 |
| EXP-HEAP-192M-citas6000 | 50 | 24.7 [24.5–26.7] | 29 [27–35] | 2.9 [2.9–3.3] | 333 [320–346] | 16 [15–194] | 2 245 [2 173–2 906] / 2 245 [2 173–2 906]; 0 de 24 |
| EXP-HEAP-192M-nmt | 100 | 48.3 | 275 | 22.1 | 361 | 45 | 30 001 / 30 001; 1 de 1 |
| EXP-HEAP-192M-nmt | 50 | 32.2 | 153 | 12.9 | 301 | 16 | 30 001 / 30 001; 1 de 1 |
| EXP-HEAP-192M-sin-informes | 100 | 24.6 [24.5–24.8] | 5 [5–6] | 0.5 [0.5–0.6] | 267 [231–282] | 56 [56–56] | sin datos / sin datos; 0 de 0 |
| EXP-HEAP-192M-sin-informes | 50 | 17.1 [16.9–17.2] | 5 [5–5] | 0.6 [0.5–0.6] | 244 [227–266] | 39 [15–39] | sin datos / sin datos; 0 de 0 |
| EXP-HEAP-256M | 100 | 57.3 [36.7–90.3] | 228 [78–475] | 24.9 [8.7–54.9] | 506 [480–509] | 56 [55–56] | 30 001 [6 742–30 001] / 30 001 [6 742–30 001]; 5 de 32 |
| EXP-HEAP-256M | 50 | 27.7 [27.4–28.0] | 48 [47–49] | 6.5 [6.4–6.6] | 428 [407–504] | 16 [15–16] | 4 732 [4 643–6 007] / 4 732 [4 643–6 007]; 0 de 24 |
| EXP-HEAP-256M-nmt | 100 | 40.0 | 105 | 11.4 | 482 | 56 | 17 322 / 17 322; 1 de 12 |
| EXP-HEAP-256M-nmt | 50 | 27.8 | 48 | 5.7 | 369 | 15 | 4 553 / 4 553; 0 de 8 |
| EXP-HEAP-288M | 100 | 14.0 [13.2–16.7] | 3 [3–4] | 2.8 [2.7–3.4] | 399 [384–411] | 20 [19–20] | 3 628 [1–4 028] / 3 628 [1–4 028]; 1 de 3 |
| EXP-HEAP-288M | 50 | 17.1 [16.7–17.1] | 4 [4–5] | 3.8 [3.7–3.9] | 421 [364–455] | 16 [15–21] | 4 229 [4 026–5 783] / 4 229 [4 026–5 783]; 1 de 3 |
| EXP-HEAP-288M-nmt | 100 | 13.2 | 2 | 1.7 | 372 | 17 | 1 / 1; 1 de 1 |
| EXP-HEAP-288M-nmt | 50 | 7.7 | 1 | 0.7 | 202 | 16 | 1 454 / 1 454; 1 de 1 |

### Memoria nativa según NMT (una corrida por configuración, con el sobrecosto del propio NMT)

| Configuración | Usuarios | Total comprometido (MB) |
|---|---|---|
| EXP-HEAP-192M-nmt | 50 | sin datos |
| EXP-HEAP-192M-nmt | 100 | sin datos |
| EXP-HEAP-256M-nmt | 50 | sin datos |
| EXP-HEAP-256M-nmt | 100 | sin datos |

## 3. Investigación del pool de conexiones (CAPACIDAD-2G, 250 usuarios)

| Pool | Reps. | AGUANTA / FALLA | Peticiones/s | Errores (%) | p95 (ms) | p99 (ms) | Usuarios cuando los errores superan 5 % | CPU backend (%) | CPU PostgreSQL (%) |
|---|---|---|---|---|---|---|---|---|---|
| 10 | 3 | 3 / 0 | 44.4 [44.3–44.5] | 0.1 [0.1–0.2] | 299 [255–320] | 942 [778–1 050] | sin datos | 66 [57–66] | 38 [38–38] |
| 20 | 3 | 3 / 0 | 44.4 [43.9–44.8] | 0.1 [0.1–0.2] | 231 [183–407] | 800 [670–1 158] | sin datos | 65 [57–66] | 40 [36–47] |
| 40 | 3 | 3 / 0 | 44.4 [44.2–44.9] | 0.1 [0.1–0.1] | 249 [168–336] | 907 [601–1 024] | sin datos | 64 [59–68] | 38 [34–42] |
| 80 | 3 | 3 / 0 | 44.4 [44.4–44.4] | 0.1 [0.1–0.2] | 257 [221–310] | 854 [828–931] | sin datos | 65 [61–66] | 39 [37–42] |

### Conexiones (PostgreSQL y Hikari)

| Pool | Conexiones máx. en PostgreSQL | Activas: media / máx. | Inactivas: media | Inactivas en transacción: máx. | Esperan un lock: máx. | Transacción más antigua: máx. (s) | Solicitudes esperando (Hikari): máx. | Esperas agotadas (30 s) |
|---|---|---|---|---|---|---|---|---|
| 10 | 11 [11–12] | 0.6 [0.6–0.8] / 8 [7–8] | 6.6 [6.2–7.0] | 9 [8–9] | 0 [0–0] | 6.0 [5.3–7.2] | 1 [0–2] | 36 [33–37] |
| 20 | 22 [22–22] | 0.7 [0.6–1.9] / 9 [6–18] | 12.7 [11.7–12.7] | 14 [12–16] | 0 [0–0] | 5.7 [5.4–7.2] | 0 [0–1] | 35 [34–35] |
| 40 | 30 [20–31] | 0.7 [0.5–1.0] / 7 [6–10] | 15.8 [11.4–18.3] | 13 [9–15] | 0 [0–0] | 6.7 [4.6–8.8] | 0 [0–0] | 35 [33–35] |
| 80 | 29 [29–33] | 0.7 [0.6–1.3] / 8 [8–15] | 15.0 [14.7–16.2] | 13 [12–18] | 0 [0–0] | 7.1 [5.5–7.4] | 0 [0–0] | 35 [35–36] |

Volcados de hilos disponibles (se analizan con `analizar_hilos.py`): pool 10, repetición 1: 4; pool 10, repetición 2: 4; pool 10, repetición 3: 4; pool 20, repetición 1: 4; pool 20, repetición 2: 4; pool 20, repetición 3: 4; pool 40, repetición 1: 4; pool 40, repetición 2: 4; pool 40, repetición 3: 4; pool 80, repetición 1: 4; pool 80, repetición 2: 4; pool 80, repetición 3: 4.

## 4. Comparación de afinidad de CPU (100 usuarios)

| Perfil | Modo | Reps. | AGUANTA / FALLA | Peticiones/s | Errores (%) | p95 (ms) | p99 (ms) | CPU backend (%) | CPU PostgreSQL (%) | Heap tras GC (MB) | Primer tramo con más de 5 % de errores (s) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| CAPACIDAD-2G | fijo | 3 | 3 / 0 | 20.5 [20.5–20.5] | 0.1 [0.1–0.2] | 136 [124–152] | 663 [632–794] | 54 [53–55] | 18 [17–21] | 426 [394–462] | 17 [16–17] |
| CAPACIDAD-2G | topologia | 3 | 3 / 0 | 20.5 [20.5–20.6] | 0.1 [0.1–0.2] | 131 [128–155] | 694 [605–950] | 57 [54–58] | 20 [17–22] | 426 [411–474] | 17 [16–17] |
| PRODUCCION-512M | fijo | 3 | 0 / 3 | 2.2 [2.1–2.2] | 75.2 [73.8–78.4] | 30 001 [30 001–30 001] | 30 001 [30 001–30 001] | 66 [34–80] | 1 [1–1] | 185 [185–185] | 17 [16–17] |
| PRODUCCION-512M | topologia | 3 | 0 / 3 | 2.1 [2.1–2.1] | 76.2 [75.4–78.3] | 30 001 [30 001–30 001] | 30 001 [30 001–30 001] | 97 [72–97] | 0 [0–1] | 185 [185–185] | 17 [16–17] |

### Diferencia por repetición (topología menos fijo, misma máquina y misma repetición)

| Perfil | Rep. | Δ peticiones/s | Δ errores (puntos %) | Δ p95 (ms) | Δ p99 (ms) | Δ CPU backend (puntos) | Δ CPU PostgreSQL (puntos) | Máquina |
|---|---|---|---|---|---|---|---|---|
| CAPACIDAD-2G | 1 | 0.0 | 0.1 | -20 | 31 | 3 | -1 | AMD EPYC 7763 64-Core Processor; |
| CAPACIDAD-2G | 2 | 0.1 | 0.0 | 18 | 156 | 4 | 4 | AMD EPYC 9V74 80-Core Processor; |
| CAPACIDAD-2G | 3 | 0.0 | -0.0 | 4 | -27 | 1 | 0 | INTEL(R) XEON(R) PLATINUM 8573C; |
| PRODUCCION-512M | 1 | -0.0 | 1.6 | 0 | -0 | 38 | 0 | AMD EPYC 7763 64-Core Processor; |
| PRODUCCION-512M | 2 | -0.0 | 1.1 | 0 | 0 | 17 | -0 | AMD EPYC 9V74 80-Core Processor; |
| PRODUCCION-512M | 3 | 0.0 | -0.0 | 0 | 0 | 31 | -1 | INTEL(R) XEON(R) PLATINUM 8573C; |

## 5. Límites

- Un laboratorio: PostgreSQL local con una sola CPU lógica, generador en el mismo equipo, máquinas de GitHub que pueden cambiar entre repeticiones y tres repeticiones.
- Los perfiles `EXP-HEAP-*` son experimentales: no se modificó el Dockerfile ni la configuración de producción.
- El monitoreo en vivo cada 3 s consume una fracción de CPU del contenedor (estimada en cerca de 1 % de una CPU) y puede no captar picos más cortos.
- La memoria fuera del heap se calcula como RssAnon menos el heap comprometido: es una aproximación (incluye Metaspace, código, pilas, memoria directa y arenas de malloc).


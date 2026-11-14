# Medición A/B del servidor

- Rondas medidas por versión: antes=5, después=5 (la ronda 0 de calentamiento se descarta).
- Cada celda: **mediana (mínimo–máximo)**. «Cambio» compara las medianas: negativo = el después consume o tarda menos.

## Recursos del servidor (mismo trabajo en cada corrida)

| Métrica | ANTES | DESPUÉS | Cambio |
|---|---|---|---|
| Segundos de CPU durante el trabajo | 82.87 (81.66–85.14) | 69.29 (62.81–76.26) | -16.4 % |
| Segundos de CPU de arranque | 21.52 (20.81–22.04) | 21.82 (20.52–22.35) | +1.4 % |
| Memoria al terminar de arrancar (MB) | 476 (430–543) | 462 (435–510) | -2.9 % |
| Memoria máxima durante el trabajo (MB) | 745 (741–754) | 739 (735–742) | -0.9 % |
| Tiempo de arranque (s) | 20.0 (18.0–21.0) | 20.0 (18.0–21.0) | +0.0 % |
| Sentencias SQL durante el trabajo | 104113 (104112–104117) | 12345 (12338–12395) | -88.1 % |
| Tiempo total de SQL en la base (s) | 33.58 (33.06–36.29) | 31.91 (30.61–33.28) | -5.0 % |
| Peticiones HTTP | 507 (507–507) | 507 (507–507) | +0.0 % |
| CPU por cada 100 peticiones (s) | 16.344 (16.106–16.793) | 13.666 (12.388–15.041) | -16.4 % |
| Respuestas con error | 0 (0–0) | 0 (0–0) |  |
| Duración total de la prueba (s) | 75.0 (72.4–76.2) | 65.2 (56.7–68.9) | -13.1 % |

## Latencia por recorrido (ms)

| Recorrido | Estadístico | ANTES | DESPUÉS | Cambio |
|---|---|---|---|---|
| P0_inicio_de_sesion | mediana | 1122 (945–1184) | 1176 (1130–1188) | +4.8 % |
| P0_inicio_de_sesion | p95 | 1123 (945–1185) | 1177 (1131–1188) | +4.9 % |
| P1_acceso_y_panel | mediana | 409 (345–426) | 269 (220–341) | -34.3 % |
| P1_acceso_y_panel | p95 | 874 (639–1156) | 595 (578–1127) | -31.9 % |
| P2_agenda_mensual | mediana | 592 (574–819) | 372 (322–419) | -37.2 % |
| P2_agenda_mensual | p95 | 1089 (950–1824) | 780 (753–1257) | -28.4 % |
| P3_historia_clinica | mediana | 135 (94–153) | 138 (119–147) | +2.2 % |
| P3_historia_clinica | p95 | 329 (172–414) | 381 (257–405) | +15.8 % |
| P4_reportes | mediana | 6280 (6206–6821) | 5728 (4680–6257) | -8.8 % |
| P4_reportes | p95 | 8544 (8119–9121) | 7019 (6924–7176) | -17.9 % |

## Versiones medidas

- **antes**: 5b27bba
- **despues**: cb7f6bd

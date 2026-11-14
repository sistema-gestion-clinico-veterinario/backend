import json
import statistics
import sys
from pathlib import Path

RAIZ = Path(sys.argv[1])
corridas = sorted((RAIZ / 'corridas').glob('*.json'))
datos = {'antes': [], 'despues': []}
for ruta in corridas:
    c = json.loads(ruta.read_text(encoding='utf-8'))
    if c.get('ronda', 0) >= 1 and c.get('version') in datos:
        datos[c['version']].append(c)

RECORRIDOS = ['P0_inicio_de_sesion', 'P1_acceso_y_panel', 'P2_agenda_mensual', 'P3_historia_clinica', 'P4_reportes']


def serie(version, extractor):
    valores = []
    for c in datos[version]:
        try:
            v = extractor(c)
        except (KeyError, TypeError):
            v = None
        if v is not None:
            valores.append(float(v))
    return valores


def celda(valores, decimales=1):
    if not valores:
        return 'sin datos'
    med = statistics.median(valores)
    return f"{med:.{decimales}f} ({min(valores):.{decimales}f}–{max(valores):.{decimales}f})"


def cambio(a, d):
    if not a or not d:
        return ''
    ma, md = statistics.median(a), statistics.median(d)
    if ma == 0:
        return ''
    return f"{(md - ma) / ma * 100:+.1f} %"


def fila(titulo, extractor, decimales=1):
    a, d = serie('antes', extractor), serie('despues', extractor)
    return f"| {titulo} | {celda(a, decimales)} | {celda(d, decimales)} | {cambio(a, d)} |"


lineas = []
lineas.append('# Medición A/B del servidor\n')
lineas.append(f"- Rondas medidas por versión: antes={len(datos['antes'])}, después={len(datos['despues'])} (la ronda 0 de calentamiento se descarta).")
lineas.append('- Cada celda: **mediana (mínimo–máximo)**. «Cambio» compara las medianas: negativo = el después consume o tarda menos.\n')

lineas.append('## Recursos del servidor (mismo trabajo en cada corrida)\n')
lineas.append('| Métrica | ANTES | DESPUÉS | Cambio |')
lineas.append('|---|---|---|---|')
lineas.append(fila('Segundos de CPU durante el trabajo', lambda c: c['cpu_trabajo_s'], 2))
lineas.append(fila('Segundos de CPU de arranque', lambda c: c['cpu_arranque_s'], 2))
lineas.append(fila('Memoria al terminar de arrancar (MB)', lambda c: c['mem_tras_arranque_mb'], 0))
lineas.append(fila('Memoria máxima durante el trabajo (MB)', lambda c: c['mem_pico_mb'], 0))
lineas.append(fila('Tiempo de arranque (s)', lambda c: c['arranque_s'], 1))
lineas.append(fila('Sentencias SQL durante el trabajo', lambda c: c['sql_sentencias'], 0))
lineas.append(fila('Tiempo total de SQL en la base (s)', lambda c: c['sql_tiempo_s'], 2))
lineas.append(fila('Peticiones HTTP', lambda c: c['k6']['peticiones'], 0))
lineas.append(fila('CPU por cada 100 peticiones (s)', lambda c: c['cpu_trabajo_s'] / c['k6']['peticiones'] * 100, 3))
lineas.append(fila('Respuestas con error', lambda c: c['k6']['respuestas_no_200'], 0))
lineas.append(fila('Duración total de la prueba (s)', lambda c: c['k6']['duracion_total_s'], 1))

lineas.append('\n## Latencia por recorrido (ms)\n')
lineas.append('| Recorrido | Estadístico | ANTES | DESPUÉS | Cambio |')
lineas.append('|---|---|---|---|---|')
for rec in RECORRIDOS:
    for nombre, clave in (('mediana', 'mediana_ms'), ('p95', 'p95_ms')):
        a = serie('antes', lambda c, r=rec, k=clave: c['k6']['recorridos'][r][k])
        d = serie('despues', lambda c, r=rec, k=clave: c['k6']['recorridos'][r][k])
        lineas.append(f"| {rec} | {nombre} | {celda(a, 0)} | {celda(d, 0)} | {cambio(a, d)} |")

lineas.append('\n## Versiones medidas\n')
for v in ('antes', 'despues'):
    if datos[v]:
        lineas.append(f"- **{v}**: {datos[v][0].get('commit', '?')}")
lineas.append('')

(RAIZ / 'RESUMEN.md').write_text('\n'.join(lineas), encoding='utf-8')
print('\n'.join(lineas))

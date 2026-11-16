import json
import re
import sys
from collections import defaultdict
from pathlib import Path

import informe_nivel as n
import informe_recuperacion as r

RAIZ = Path(sys.argv[1])
NIV = RAIZ / 'niveles'


def celda_capacidad(version, nivel, cfg):
    n.configurar(RAIZ, nivel, cfg)
    datos = n.analizar(version)
    m = n.metricas(datos)
    if not datos['k6']:
        return 'sin datos'
    estado = datos['estado'] or {}
    marcas = []
    if estado.get('oom_killed') or datos['log']['sin_memoria_java']:
        marcas.append('sin memoria')
    texto = n.veredicto(m).split(' (')[0]
    sano = f"~{m['sano']:.0f} usuarios sanos" if m['sano'] is not None else 'sin tramo sano'
    err = n.fmt(m['errores_pct'], 1, ' %')
    p95 = n.fmt(m['p95_ms'])
    extra = (' · ' + ', '.join(marcas)) if marcas else ''
    return f'**{texto}** · {sano} · errores {err} · p95 {p95} ms{extra}'


def celda_recuperacion(version, pico, cfg):
    r.configurar(RAIZ, pico, cfg)
    ev = r.evaluar(version)
    return f"**{ev['veredicto']}** · p95 en el pico {n.fmt(ev['pico']['p95_ms'])} ms · salud final {ev['estado'].get('salud_final_http', '?')}"


grupos = defaultdict(set)
for ruta in NIV.glob('estado_*.json'):
    try:
        e = json.loads(ruta.read_text(encoding='utf-8'))
    except (OSError, ValueError):
        continue
    grupos[(e.get('perfil', 'capacidad'), int(e.get('nivel', 0)), e.get('configuracion', 'C'))].add(e.get('version'))

lineas = ['# Resumen de capacidad por configuración de memoria', '']
lineas.append('Configuraciones: **A** = heap 192 MB, compilador reducido (como el Dockerfile de hoy); **B** = heap 1 GB, compilador reducido; **C** = heap 1 GB, compilador completo. Todas con 1 CPU y 2 GB.')
lineas.append('')
lineas += ['| Prueba | Configuración | ANTES | DESPUÉS |', '|---|---|---|---|']
for (perfil, nivel, cfg) in sorted(grupos, key=lambda k: (k[0] != 'capacidad', k[1], k[2])):
    if perfil == 'recuperacion':
        a = celda_recuperacion('antes', nivel, cfg)
        d = celda_recuperacion('despues', nivel, cfg)
        titulo = f'Recuperación (pico de {nivel})'
    else:
        a = celda_capacidad('antes', nivel, cfg)
        d = celda_capacidad('despues', nivel, cfg)
        titulo = f'{nivel} usuarios'
    lineas.append(f'| {titulo} | {cfg} | {a} | {d} |')
lineas.append('')
lineas.append('> Cada fila sale de una corrida con el backend reiniciado desde cero. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.')

(RAIZ / 'RESUMEN_CONFIGURACIONES.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
print('\n'.join(lineas))

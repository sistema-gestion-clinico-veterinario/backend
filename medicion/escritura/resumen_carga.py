import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import informe_carga as c  # noqa: E402

RAIZ = Path(sys.argv[1])
NIV = RAIZ / 'niveles'


def nivel_de(ruta, prefijo):
    return int(ruta.name[len(f'estado_{prefijo}_'):].split('.')[0])


def fila(nivel, titulo, d):
    m = c.n.metricas(d)
    ok, fallo = c.tasa_exito_escrituras(d)
    sano = f"~{m['sano']:.0f}" if m['sano'] is not None else '-'
    jvm = d['jvm']
    pico = max((p[1] for p in jvm['metaspace']), default=None)
    tend = c.tendencia_metaspace(jvm['metaspace'])
    if d['log']['oom_metaspace']:
        estado_ms = f"AGOTADO ({d['log']['oom_metaspace']} errores)"
    elif tend is None:
        estado_ms = 'sin datos'
    elif tend['pct'] < 3:
        estado_ms = 'estable'
    else:
        estado_ms = f"crece {tend['pct']:+.0f} %"
    correos = (d['estado'] or {}).get('correos_recibidos')
    return (
        f"| {nivel} | {titulo} | {c.n.veredicto(m).split(' (')[0]} | {sano} | {c.n.fmt(m['errores_pct'], 1, ' %')} | {c.n.fmt(m['p95_ms'])} | {c.n.fmt(m['p99_ms'])} | "
        f"{ok} / {fallo} | {c.estado_datos(d)} | {c.n.fmt(pico)} | {estado_ms} | {c.n.fmt(correos)} |"
    )


CABECERA = [
    '| Usuarios | Versión | Veredicto | Sin degradación hasta (usuarios) | Errores | p95 (ms) | p99 (ms) | Escrituras correctas / fallidas | Datos | Metaspace pico (MB) | Metaspace | Correos recibidos |',
    '|---|---|---|---|---|---|---|---|---|---|---|---|',
]

lineas = ['# Fase 2b: resumen de la escalera de carga con escrituras', '']

controles = sorted(NIV.glob('estado_ctrl128_*.json'))
if controles:
    nivel = nivel_de(controles[0], 'ctrl128')
    c.configurar(RAIZ, nivel)
    lineas += ['## Control: Metaspace de 128 MB (igual que el Dockerfile actual)', ''] + CABECERA
    lineas.append(fila(nivel, 'DESPUÉS, 128 MB', c.analizar('ctrl128', 'E')))
    lineas.append('')

niveles = sorted({nivel_de(p, 'antes') for p in NIV.glob('estado_antes_*.json')} | {nivel_de(p, 'despues') for p in NIV.glob('estado_despues_*.json')})
lineas += ['## Escalera con Metaspace de 256 MB y correo de pruebas', ''] + CABECERA
for nivel in niveles:
    c.configurar(RAIZ, nivel)
    for version, titulo in (('antes', 'ANTES'), ('despues', 'DESPUÉS')):
        lineas.append(fila(nivel, titulo, c.analizar(version)))
lineas.append('')
lineas.append('> Datos CONSISTENTES = lo que k6 contó como creado coincide con la base y no hay registros huérfanos. "Sin degradación hasta" = usuarios del último tramo de 15 s antes del primero con 1 % de errores o p95 de 2 s. Metaspace: lectura de los registros de GC de la propia JVM. Cada nivel reinicia el backend; la base conserva lo escrito en niveles anteriores.')
(RAIZ / 'RESUMEN_FASE2B.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
print('\n'.join(lineas))

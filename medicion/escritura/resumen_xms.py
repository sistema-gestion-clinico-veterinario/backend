import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import informe_carga as c  # noqa: E402

RAIZ = Path(sys.argv[1])
MODO = sys.argv[2] if len(sys.argv) > 2 else 'xms'
NIV = RAIZ / 'niveles'

if MODO == 'indice':
    VARIANTES = (('despues', 'sin índice'), ('indice', 'con índice'))
    TITULO = 'Fase 3c: índice en consentimiento_datos, cada corrida desde la misma base'
    SALIDA = 'RESUMEN_FASE3C'
    PIE = 'Cada corrida parte de la misma base copiada de una plantilla; el orden alterna por nivel. Ambas variantes usan -Xms = -Xmx. Una corrida por variante y nivel. Las consultas más costosas y los planes de cada nivel están en INFORME_CARGA_<nivel>.md.'
else:
    VARIANTES = (('despues', 'sin -Xms'), ('xms', 'con -Xms'))
    TITULO = 'Fase 3b: efecto de -Xms = -Xmx (1 CPU, 2 GB) y consultas más costosas'
    SALIDA = 'RESUMEN_FASE3B'
    PIE = 'Las dos variantes corren en la misma máquina, una detrás de otra, alternando cuál va primero en cada nivel. Una corrida por variante y nivel. Las consultas más costosas de cada nivel están en INFORME_CARGA_<nivel>.md.'

niveles = sorted({int(p.name[len('estado_despues_'):].split('.')[0]) for p in NIV.glob('estado_despues_*.json')})

filas = []
datos = []
for nivel in niveles:
    c.configurar(RAIZ, nivel)
    for etiqueta, titulo in VARIANTES:
        if not (NIV / f'estado_{etiqueta}_{nivel}.json').exists():
            continue
        d = c.analizar(etiqueta)
        m = c.n.metricas(d)
        j = d['jvm']
        dur = j['duracion_jvm_s'] or 1
        gc_pct = j['pausa_total_s'] / dur * 100 if j['pausas'] else None
        ok, fallo = c.tasa_exito_escrituras(d)
        consent = d.get('sql_consent') or {}
        tabla = d.get('consent_despues') or {}
        datos.append({
            'nivel': nivel, 'variante': titulo, 'veredicto': c.n.veredicto(m).split(' (')[0], 'errores_pct': m['errores_pct'], 'rps': m['rps'],
            'mediana_ms': m['mediana_ms'], 'p95_ms': m['p95_ms'], 'sin_degradacion': m['sano'], 'pausas': j['pausas'], 'completas': j['completos'],
            'completas_allocation_failure': j['completas_por_heap'], 'completas_s': j['completas_s'], 'gc_pct': gc_pct,
            'heap_inicial_mb': j['heap_inicial_mb'], 'heap_tras_gc_max_mb': j['heap_tras_gc_max_mb'], 'pg_cpu_media': d['cpu_pg']['media'],
            'cpu_media': d['cpu']['media'], 'pool_agotado': d['log']['pool_conexiones'], 'datos': c.estado_datos(d),
            'consent_llamadas': consent.get('llamadas'), 'consent_total_ms': consent.get('total_ms'), 'consent_media_ms': consent.get('media_ms'),
            'consent_filas': tabla.get('filas'), 'consent_tabla_mb': tabla.get('tabla_mb'),
        })
        if MODO == 'indice':
            extra = f"{c.n.fmt((consent.get('total_ms') or 0) / 1000, 1)} | {c.n.fmt(consent.get('media_ms'), 1)} | {c.n.fmt(tabla.get('filas'))} | "
        else:
            extra = ''
        filas.append(
            f"| {nivel} | {titulo} | {c.n.veredicto(m).split(' (')[0]} | {c.n.fmt(m['errores_pct'], 1, ' %')} | {c.n.fmt(m['rps'], 1)} | {c.n.fmt(m['p95_ms'])} | "
            f"{j['completos']} | {c.n.fmt(gc_pct, 1, ' %')} | {c.n.fmt(d['cpu_pg']['media'], 0)} | {extra}{d['log']['pool_conexiones']} | {ok} / {fallo} | {c.estado_datos(d)} |"
        )

cabecera = '| Usuarios | Variante | Veredicto | Errores | Peticiones/s | p95 (ms) | Pausas completas de GC | Tiempo en GC | CPU PostgreSQL media (%) | '
sep = '|---|---|---|---|---|---|---|---|---|'
if MODO == 'indice':
    cabecera += 'Tiempo SQL en consentimiento_datos (s) | Media por llamada (ms) | Filas de la tabla | '
    sep += '---|---|---|'
cabecera += 'Avisos de pool agotado | Escrituras correctas / fallidas | Datos |'
sep += '---|---|---|'

lineas = [f'# {TITULO}', '']
lineas += c.lineas_entorno() + ['']
lineas += [cabecera, sep] + filas + ['']
lineas.append(f'> {PIE}')
(RAIZ / f'{SALIDA}.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
(RAIZ / f'{SALIDA}.json').write_text(json.dumps(datos, indent=1), encoding='utf-8')
print('\n'.join(lineas))

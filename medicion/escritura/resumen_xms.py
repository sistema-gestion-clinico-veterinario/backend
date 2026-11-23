import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import informe_carga as c  # noqa: E402

RAIZ = Path(sys.argv[1])
MODO = sys.argv[2] if len(sys.argv) > 2 else 'xms'
NIV = RAIZ / 'niveles'

if MODO == 'panel':
    VARIANTES = (('legado', 'código anterior'), ('optimizado', 'código nuevo'))
    TITULO = 'Fase 3d: panel optimizado frente al código anterior'
    SALIDA = 'RESUMEN_FASE3D'
    PIE = 'Mismo servidor, misma imagen y misma base de partida en cada corrida; solo cambia la bandera app.dashboard.consultas-optimizadas. El orden alterna por nivel. Una corrida por variante y nivel. Detalle, consultas más costosas y comparación de respuestas en INFORME_CARGA_<nivel>.md.'
elif MODO == 'indice':
    VARIANTES = (('despues', 'sin índice'), ('indice', 'con índice'))
    TITULO = 'Fase 3c: índice en consentimiento_datos, cada corrida desde la misma base'
    SALIDA = 'RESUMEN_FASE3C'
    PIE = 'Cada corrida parte de la misma base copiada de una plantilla; el orden alterna por nivel. Ambas variantes usan -Xms = -Xmx. Una corrida por variante y nivel. Las consultas más costosas y los planes de cada nivel están en INFORME_CARGA_<nivel>.md.'
else:
    VARIANTES = (('despues', 'sin -Xms'), ('xms', 'con -Xms'))
    TITULO = 'Fase 3b: efecto de -Xms = -Xmx (1 CPU, 2 GB) y consultas más costosas'
    SALIDA = 'RESUMEN_FASE3B'
    PIE = 'Las dos variantes corren en la misma máquina, una detrás de otra, alternando cuál va primero en cada nivel. Una corrida por variante y nivel. Las consultas más costosas de cada nivel están en INFORME_CARGA_<nivel>.md.'

PRIMERA = VARIANTES[0][0]
niveles = sorted({int(p.name[len(f'estado_{PRIMERA}_'):].split('.')[0]) for p in NIV.glob(f'estado_{PRIMERA}_*.json')})

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
        sonda = d.get('sonda') or {}
        panel = d['endpoints'].get('panel')
        panel_p95 = c.n.percentil(panel['duraciones'], 0.95) if panel and panel['duraciones'] else None
        sentencias_panel = (sonda['sentencias_sql'] / sonda['llamadas']) if sonda.get('llamadas') and sonda.get('sentencias_sql') is not None else None
        sql_total = (d['estado'] or {}).get('sql_tiempo_s')
        datos.append({
            'nivel': nivel, 'variante': titulo, 'veredicto': c.n.veredicto(m).split(' (')[0], 'errores_pct': m['errores_pct'], 'rps': m['rps'],
            'mediana_ms': m['mediana_ms'], 'p95_ms': m['p95_ms'], 'sin_degradacion': m['sano'], 'pausas': j['pausas'], 'completas': j['completos'],
            'completas_allocation_failure': j['completas_por_heap'], 'completas_s': j['completas_s'], 'gc_pct': gc_pct,
            'heap_inicial_mb': j['heap_inicial_mb'], 'heap_tras_gc_max_mb': j['heap_tras_gc_max_mb'], 'pg_cpu_media': d['cpu_pg']['media'],
            'cpu_media': d['cpu']['media'], 'pool_agotado': d['log']['pool_conexiones'], 'datos': c.estado_datos(d),
            'consent_llamadas': consent.get('llamadas'), 'consent_total_ms': consent.get('total_ms'), 'consent_media_ms': consent.get('media_ms'),
            'consent_filas': tabla.get('filas'), 'consent_tabla_mb': tabla.get('tabla_mb'),
            'sentencias_por_panel': sentencias_panel, 'panel_p95_ms': panel_p95, 'sql_tiempo_total_s': sql_total,
            'sentencias_sql_total': (d['estado'] or {}).get('sql_sentencias'), 'peticiones': m['peticiones'],
        })
        if MODO == 'indice':
            extra = f"{c.n.fmt((consent.get('total_ms') or 0) / 1000, 1)} | {c.n.fmt(consent.get('media_ms'), 1)} | {c.n.fmt(tabla.get('filas'))} | "
        elif MODO == 'panel':
            extra = f"{c.n.fmt(sentencias_panel, 1)} | {c.n.fmt(panel_p95)} | {c.n.fmt(sql_total, 0)} | "
        else:
            extra = ''
        filas.append(
            f"| {nivel} | {titulo} | {c.n.veredicto(m).split(' (')[0]} | {c.n.fmt(m['errores_pct'], 1, ' %')} | {c.n.fmt(m['rps'], 1)} | {c.n.fmt(m['p95_ms'])} | {c.n.fmt(m['p99_ms'])} | "
            f"{j['completos']} | {c.n.fmt(gc_pct, 1, ' %')} | {c.n.fmt(d['cpu_pg']['media'], 0)} | {extra}{d['log']['pool_conexiones']} | {ok} / {fallo} | {c.estado_datos(d)} |"
        )

cabecera = '| Usuarios | Variante | Veredicto | Errores | Peticiones/s | p95 (ms) | p99 (ms) | Pausas completas de GC | Tiempo en GC | CPU PostgreSQL media (%) | '
sep = '|---|---|---|---|---|---|---|---|---|---|'
if MODO == 'indice':
    cabecera += 'Tiempo SQL en consentimiento_datos (s) | Media por llamada (ms) | Filas de la tabla | '
    sep += '---|---|---|'
elif MODO == 'panel':
    cabecera += 'Sentencias SQL por carga del panel | Panel p95 bajo carga (ms) | Tiempo total en PostgreSQL (s) | '
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

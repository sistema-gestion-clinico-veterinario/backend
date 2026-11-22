import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import informe_carga as c  # noqa: E402

RAIZ = Path(sys.argv[1])
NIV = RAIZ / 'niveles'

niveles = sorted({int(p.name[len('estado_despues_'):].split('.')[0]) for p in NIV.glob('estado_despues_*.json')})

filas = []
datos = []
for nivel in niveles:
    c.configurar(RAIZ, nivel)
    for etiqueta, titulo in (('despues', 'sin -Xms'), ('xms', 'con -Xms')):
        if not (NIV / f'estado_{etiqueta}_{nivel}.json').exists():
            continue
        d = c.analizar(etiqueta)
        m = c.n.metricas(d)
        j = d['jvm']
        dur = j['duracion_jvm_s'] or 1
        gc_pct = j['pausa_total_s'] / dur * 100 if j['pausas'] else None
        ok, fallo = c.tasa_exito_escrituras(d)
        top = (d.get('sql_top') or [{}])[0].get('consulta', '')
        datos.append({
            'nivel': nivel, 'variante': titulo, 'veredicto': c.n.veredicto(m).split(' (')[0], 'errores_pct': m['errores_pct'], 'rps': m['rps'],
            'mediana_ms': m['mediana_ms'], 'p95_ms': m['p95_ms'], 'sin_degradacion': m['sano'], 'pausas': j['pausas'], 'completas': j['completos'],
            'completas_allocation_failure': j['completas_por_heap'], 'completas_s': j['completas_s'], 'gc_pct': gc_pct,
            'heap_inicial_mb': j['heap_inicial_mb'], 'heap_tras_gc_max_mb': j['heap_tras_gc_max_mb'], 'pg_cpu_media': d['cpu_pg']['media'],
            'cpu_media': d['cpu']['media'], 'pool_agotado': d['log']['pool_conexiones'], 'datos': c.estado_datos(d), 'sql_top1': top,
        })
        filas.append(
            f"| {nivel} | {titulo} | {c.n.veredicto(m).split(' (')[0]} | {c.n.fmt(m['errores_pct'], 1, ' %')} | {c.n.fmt(m['rps'], 1)} | {c.n.fmt(m['p95_ms'])} | "
            f"{c.n.fmt(j['heap_inicial_mb'])} | {j['pausas']} | {j['completos']} | {c.n.fmt(j['completas_s'], 1)} | {c.n.fmt(gc_pct, 1, ' %')} | "
            f"{c.n.fmt(j['heap_tras_gc_max_mb'])} | {c.n.fmt(d['cpu_pg']['media'], 0)} | {d['log']['pool_conexiones']} | {ok} / {fallo} | {c.estado_datos(d)} |"
        )

lineas = ['# Fase 3b: efecto de -Xms = -Xmx (1 CPU, 2 GB) y consultas más costosas', '']
lineas += c.lineas_entorno() + ['']
lineas += [
    '| Usuarios | Variante | Veredicto | Errores | Peticiones/s | p95 (ms) | Heap en la 1ª pausa (MB) | Pausas de GC | Pausas completas | Tiempo en pausas completas (s) | Tiempo en GC | Heap tras GC máx (MB) | CPU PostgreSQL media (%) | Avisos de pool agotado | Escrituras correctas / fallidas | Datos |',
    '|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|',
] + filas + ['']
lineas.append('> Las dos variantes corren en la misma máquina, una detrás de otra, alternando cuál va primero en cada nivel. Una corrida por variante y nivel. Las consultas más costosas de cada nivel están en su informe INFORME_CARGA_<nivel>.md.')
(RAIZ / 'RESUMEN_FASE3B.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
(RAIZ / 'RESUMEN_FASE3B.json').write_text(json.dumps(datos, indent=1), encoding='utf-8')
print('\n'.join(lineas))

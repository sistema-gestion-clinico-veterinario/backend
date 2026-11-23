import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import informe_carga as c  # noqa: E402

RAIZ = Path(sys.argv[1])
NIV = RAIZ / 'niveles'

niveles = sorted(int(p.name[len('estado_despues_'):].split('.')[0]) for p in NIV.glob('estado_despues_*.json'))

filas = []
datos = []
for nivel in niveles:
    c.configurar(RAIZ, nivel)
    d = c.analizar('despues')
    m = c.n.metricas(d)
    jvm = d['jvm']
    est = m['estados']
    pico_ms = max((p[1] for p in jvm['metaspace']), default=None)
    gc_pct = (jvm['pausa_total_s'] / jvm['duracion_jvm_s'] * 100) if jvm['duracion_jvm_s'] else None
    ok, fallo = c.tasa_exito_escrituras(d)
    fallos = f"{est.get('sin_respuesta', 0)} / {est.get('5xx', 0)} / {est.get('otro_4xx', 0)}"
    registro = {
        'nivel': nivel, 'veredicto': c.n.veredicto(m).split(' (')[0], 'sin_degradacion': m['sano'], 'errores_pct': m['errores_pct'],
        'rps': m['rps'], 'p95_ms': m['p95_ms'], 'p99_ms': m['p99_ms'], 'mediana_ms': m['mediana_ms'], 'peticiones': m['peticiones'],
        'cpu_media_pct_limite': d['cpu']['media'], 'cpu_tope_pct': d['cpu']['tope_pct'], 'cpu_pg_media': d['cpu_pg']['media'],
        'mem_pico_mb': d['mem_pico_mb'], 'heap_tras_gc_max_mb': jvm['heap_tras_gc_max_mb'], 'gc_pct': gc_pct, 'metaspace_pico_mb': pico_ms,
        'pool_agotado': d['log']['pool_conexiones'], 'oom_heap': d['log']['sin_memoria_java'], 'oom_metaspace': d['log']['oom_metaspace'],
        'sin_respuesta': est.get('sin_respuesta', 0), 'err_5xx': est.get('5xx', 0), 'err_4xx': est.get('otro_4xx', 0),
        'escrituras_ok': ok, 'escrituras_fallidas': fallo, 'datos': c.estado_datos(d), 'limite_cpu': d['limite_cpu'], 'limite_mem_mb': d['limite_mem_mb'],
        'heap_max': d['heap_max'],
    }
    datos.append(registro)
    filas.append(
        f"| {nivel} | {registro['veredicto']} | {c.n.fmt(m['sano'])} | {c.n.fmt(m['errores_pct'], 1, ' %')} | {c.n.fmt(m['rps'], 1)} | {c.n.fmt(m['p95_ms'])} | {c.n.fmt(m['p99_ms'])} | "
        f"{c.n.fmt(d['cpu']['media'], 0)} / {c.n.fmt(d['cpu']['tope_pct'], 0)} | {c.n.fmt(d['cpu_pg']['media'], 0)} | {c.n.fmt(d['mem_pico_mb'])} | "
        f"{c.n.fmt(jvm['heap_tras_gc_max_mb'])} | {c.n.fmt(gc_pct, 0, ' %')} | {c.n.fmt(pico_ms)} | {d['log']['pool_conexiones']} | {fallos} | {ok} / {fallo} | {registro['datos']} |"
    )

limite_cpu = datos[0]['limite_cpu'] if datos else '?'
limite_mem = c.memoria_legible(datos[0]['limite_mem_mb']) if datos else '?'
heap = datos[0]['heap_max'] if datos else '?'
lineas = [f'# Fase 3: escalado vertical, {limite_cpu} CPU, {limite_mem}, heap {heap}', '']
lineas += c.lineas_entorno() + ['']
lineas += [
    '| Usuarios | Veredicto | Sin degradación hasta (usuarios) | Errores | Peticiones/s | p95 (ms) | p99 (ms) | CPU backend: media / al tope (% del límite) | CPU PostgreSQL media (%) | Memoria pico (MB) | Heap tras GC máx (MB) | Tiempo en GC | Metaspace pico (MB) | Avisos de pool agotado | Fallos: sin respuesta / 5xx / 4xx | Escrituras correctas / fallidas | Datos |',
    '|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|',
] + filas + ['']
lineas.append('> "Sin degradación hasta" = usuarios del último tramo de 15 s antes del primero con 1 % de errores o p95 de 2 s. Los 4xx del laboratorio son reglas de negocio (solo hay 10 veterinarios). Una corrida por nivel y por configuración.')
(RAIZ / 'RESUMEN_FASE3.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
(RAIZ / 'RESUMEN_FASE3.json').write_text(json.dumps(datos, indent=1), encoding='utf-8')
print('\n'.join(lineas))

import csv
import json
import re
import statistics
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import comparar_bateria as cb  # noqa: E402
import informe_laboratorio as lab  # noqa: E402
import series_temporales as st  # noqa: E402

NOMBRE_RE = re.compile(r'inv-(heap|pool|afin)-r(\d+)-(.+)$')
RE_NMT = re.compile(r'^-\s+(.+?)\s+\(reserved=(\d+)(KB)?, committed=(\d+)(KB)?\)', re.M)
RE_NMT_TOTAL = re.compile(r'Total: reserved=(\d+)(KB)?, committed=(\d+)(KB)?')


def descubrir(raiz):
    corridas = []
    for entorno in sorted(Path(raiz).rglob('entorno.txt')):
        d = entorno.parent
        m = NOMBRE_RE.search(d.name)
        if not m or not (d / 'niveles').is_dir():
            continue
        for estado in sorted((d / 'niveles').glob('estado_despues_*.json')):
            corridas.append((d, m.group(1), int(m.group(2)), m.group(3), int(estado.stem.split('_')[-1])))
    return corridas


def leer_nmt(ruta):
    texto = cb.leer_texto(ruta)
    if not texto:
        return None
    def a_mb(valor, unidad):
        return int(valor) / (1024 if unidad == 'KB' else 1048576)
    categorias = {nombre.strip(): a_mb(comprometido, unidad) for nombre, _, _, comprometido, unidad in RE_NMT.findall(texto)}
    total = RE_NMT_TOTAL.search(texto)
    return {'categorias_mb': categorias, 'total_comprometido_mb': a_mb(total.group(3), total.group(4)) if total else None}


def actividad_pg(ruta):
    filas = lab.leer_csv(ruta)
    if not filas:
        return None

    def col(nombre):
        return [lab.numero(f.get(nombre)) for f in filas if lab.numero(f.get(nombre)) is not None]
    act, ina, itx, tot, lock = col('activas'), col('inactivas'), col('inactivas_en_transaccion'), col('total'), col('esperan_lock')
    return {'muestras': len(filas), 'conexiones_max': max(tot, default=None), 'activas_max': max(act, default=None), 'activas_media': statistics.mean(act) if act else None,
            'inactivas_media': statistics.mean(ina) if ina else None, 'inactivas_en_transaccion_max': max(itx, default=None), 'esperan_lock_max': max(lock, default=None),
            'tx_mas_vieja_max_s': max(col('tx_mas_vieja_s'), default=None)}


def hikari_resumen(a):
    puntos = [h for _, h in a['hikari']]
    if not puntos:
        return {'esperando_max': None, 'activas_max': None, 'avisos': 0, 'esperas_agotadas': a.get('sin_conexion_lineas', 0)}
    return {'esperando_max': max(h['esperando'] for h in puntos), 'activas_max': max(h['activas'] for h in puntos), 'avisos': len(puntos), 'total': puntos[0]['total'],
            'esperas_agotadas': a.get('sin_conexion_lineas', 0)}


def pool_de_entorno(d):
    m = re.search(r'^pool de conexiones: (\d+)', cb.leer_texto(d / 'entorno.txt'), re.M)
    return int(m.group(1)) if m else None


def analizar(d, tipo, rep, etiqueta, nivel):
    c = cb.analizar_corrida(d, etiqueta, rep, nivel)
    jvm = lab.evaluar_jvm(d / 'niveles', f'despues_{nivel}')
    serie = st.analizar(d, nivel)
    c.update({
        'tipo': tipo, 'etiqueta': etiqueta, 'pool': pool_de_entorno(d),
        'rss_anon_max_mb': jvm.get('rss_anon_max_mb'), 'fuera_del_heap_max_mb': jvm.get('fuera_del_heap_max_mb'), 'hilos_so_max': jvm.get('hilos_so_max'),
        'hilos_java_max': jvm.get('hilos_java_max'), 'kstack_max_mb': jvm.get('kstack_max_mb'),
        'pg_actividad': actividad_pg(d / 'niveles' / f'pg_actividad_despues_{nivel}.csv'), 'hikari': hikari_resumen(serie) if serie else None,
        'nmt': leer_nmt(d / 'niveles' / f'nmt_despues_{nivel}.txt'), 'primer_fallo_s': serie['primer_fallo_s'] if serie else None,
        'usuarios_al_5pct': serie['usuarios_al_5pct_de_errores'] if serie else None, 'orden_de_eventos': serie['orden_de_eventos'] if serie else [],
        'volcados_de_hilos': sorted(p.name for p in (d / 'niveles').glob(f'hilos_despues_{nivel}_t*.txt.gz')),
    })
    return c


def grupos(corridas, claves):
    g = {}
    for c in corridas:
        g.setdefault(tuple(c[k] for k in claves), []).append(c)
    return dict(sorted(g.items(), key=lambda kv: tuple(str(x) for x in kv[0])))


def fila_rendimiento(cs):
    return [cb.rango([c['rps'] for c in cs], 1), cb.rango([c['errores_pct'] for c in cs], 1), cb.rango([c['p95_ms'] for c in cs]), cb.rango([c['p99_ms'] for c in cs])]


def seccion_heap(corridas):
    L = ['## 2. Investigación del heap de producción (contenedor de 512 MB)', '']
    cs = [c for c in corridas if c['tipo'] == 'heap']
    if not cs:
        return L + ['Sin corridas de este tipo.', '']
    g = grupos(cs, ('etiqueta', 'nivel'))
    L += ['### Resultado y estabilidad', '']
    L += cb.tabla(['Configuración', 'Usuarios', 'Reps.', 'AGUANTA / DEGRADADO / FALLA', 'Peticiones/s', 'Errores (%)', 'p95 (ms)', 'p99 (ms)', 'OOMKilled / reinicios / OutOfMemoryError'],
                  [(e, nv, len(x), f"{sum(c['veredicto'] == 'AGUANTA' for c in x)} / {sum(c['veredicto'] == 'DEGRADADO' for c in x)} / {sum(c['veredicto'] not in ('AGUANTA', 'DEGRADADO') for c in x)}",
                    *fila_rendimiento(x), f"{sum(bool(c['oom_killed']) for c in x)} / {sum((c['reinicios'] or 0) > 0 for c in x)} / {sum((c['oom_java_en_log'] or 0) > 0 for c in x)}")
                   for (e, nv), x in g.items()]) + ['']
    L += ['### Memoria: heap, Metaspace, memoria fuera del heap y contenedor', '']
    L += cb.tabla(['Configuración', 'Usuarios', 'Heap usado máx. (MB)', 'Heap tras GC en el nivel (MB)', 'Metaspace máx. (MB)', 'RssAnon máx. (MB)', 'Fuera del heap = RssAnon − heap comprometido, máx. (MB)', 'Hilos del sistema máx.', 'Pilas del núcleo (MB)', 'Contenedor: pico cgroup (MB)'],
                  [(e, nv, cb.rango([c['heap_usado_max_mb'] for c in x]), cb.rango([c['heap_tras_gc_max_mb'] for c in x]), cb.rango([c['metaspace_max_mb'] for c in x]),
                    cb.rango([c['rss_anon_max_mb'] for c in x]), cb.rango([c['fuera_del_heap_max_mb'] for c in x]), cb.rango([c['hilos_so_max'] for c in x]), cb.rango([c['kstack_max_mb'] for c in x], 1),
                    cb.rango([c['cont_pico_cgroup_mb'] or c['mem_docker_pico_mb'] for c in x]) + ' de 512') for (e, nv), x in g.items()]) + ['']
    L += ['### GC y primer fallo', '']
    L += cb.tabla(['Configuración', 'Usuarios', 'Pausas de GC por minuto', 'Pausas completas', 'Tiempo en GC (%)', 'Pausa máxima (ms)', 'Primer tramo de 15 s con más de 5 % de errores (s)', 'Informes clínicos: p95 / p99 (ms) y con error'],
                  [(e, nv, cb.rango([cb.val(c, 'gc', 'por_minuto') for c in x], 1), cb.rango([cb.val(c, 'gc', 'completas') for c in x]), cb.rango([cb.val(c, 'gc', 'pct_tiempo') for c in x], 1),
                    cb.rango([cb.val(c, 'gc', 'max_ms') for c in x]), cb.rango([c.get('primer_fallo_s') for c in x]),
                    f"{cb.rango([cb.val(c, 'informe_clinico', 'p95') for c in x])} / {cb.rango([cb.val(c, 'informe_clinico', 'p99') for c in x])}; {sum((cb.val(c, 'informe_clinico', 'errores') or 0) for c in x)} de {sum((cb.val(c, 'informe_clinico', 'n') or 0) for c in x)}")
                   for (e, nv), x in g.items()]) + ['']
    nmt = [c for c in cs if c.get('nmt')]
    if nmt:
        categorias = sorted({k for c in nmt for k in c['nmt']['categorias_mb']}, key=lambda k: -max(c['nmt']['categorias_mb'].get(k, 0) for c in nmt))
        L += ['### Memoria nativa según NMT (una corrida por configuración, con el sobrecosto del propio NMT)', '']
        L += cb.tabla(['Configuración', 'Usuarios', 'Total comprometido (MB)'] + categorias[:9],
                      [(c['etiqueta'], c['nivel'], cb.fmt0(c['nmt']['total_comprometido_mb'])) + tuple(cb.fmt0(c['nmt']['categorias_mb'].get(k)) for k in categorias[:9]) for c in nmt]) + ['']
    return L


def seccion_pool(corridas):
    L = ['## 3. Investigación del pool de conexiones (CAPACIDAD-2G, 250 usuarios)', '']
    cs = [c for c in corridas if c['tipo'] == 'pool']
    if not cs:
        return L + ['Sin corridas de este tipo.', '']
    g = grupos(cs, ('pool',))
    L += cb.tabla(['Pool', 'Reps.', 'AGUANTA / FALLA', 'Peticiones/s', 'Errores (%)', 'p95 (ms)', 'p99 (ms)', 'Usuarios cuando los errores superan 5 %', 'CPU backend (%)', 'CPU PostgreSQL (%)'],
                  [(p[0], len(x), f"{sum(c['veredicto'] == 'AGUANTA' for c in x)} / {sum(c['veredicto'] != 'AGUANTA' for c in x)}", *fila_rendimiento(x), cb.rango([c.get('usuarios_al_5pct') for c in x]),
                    cb.rango([c['cpu_backend_media'] for c in x]), cb.rango([c['cpu_pg_media'] for c in x])) for p, x in g.items()]) + ['']
    L += ['### Conexiones (PostgreSQL y Hikari)', '']
    L += cb.tabla(['Pool', 'Conexiones máx. en PostgreSQL', 'Activas: media / máx.', 'Inactivas: media', 'Inactivas en transacción: máx.', 'Esperan un lock: máx.', 'Transacción más antigua: máx. (s)', 'Solicitudes esperando (Hikari): máx.', 'Esperas agotadas (30 s)'],
                  [(p[0], cb.rango([cb.val(c, 'pg_actividad', 'conexiones_max') for c in x]), f"{cb.rango([cb.val(c, 'pg_actividad', 'activas_media') for c in x], 1)} / {cb.rango([cb.val(c, 'pg_actividad', 'activas_max') for c in x])}",
                    cb.rango([cb.val(c, 'pg_actividad', 'inactivas_media') for c in x], 1), cb.rango([cb.val(c, 'pg_actividad', 'inactivas_en_transaccion_max') for c in x]),
                    cb.rango([cb.val(c, 'pg_actividad', 'esperan_lock_max') for c in x]), cb.rango([cb.val(c, 'pg_actividad', 'tx_mas_vieja_max_s') for c in x], 1),
                    cb.rango([cb.val(c, 'hikari', 'esperando_max') for c in x]), cb.rango([cb.val(c, 'hikari', 'esperas_agotadas') for c in x])) for p, x in g.items()]) + ['']
    hilos = [(c['pool'], c['repeticion'], c['volcados_de_hilos']) for c in cs if c['volcados_de_hilos']]
    if hilos:
        L += ['Volcados de hilos disponibles (se analizan con `analizar_hilos.py`): ' + '; '.join(f'pool {p}, repetición {r}: {len(v)}' for p, r, v in hilos) + '.', '']
    return L


def seccion_afinidad(corridas):
    L = ['## 4. Comparación de afinidad de CPU (100 usuarios)', '']
    cs = [c for c in corridas if c['tipo'] == 'afin']
    if not cs:
        return L + ['Sin corridas de este tipo.', '']
    for c in cs:
        modo, _, perfil = c['etiqueta'].partition('-')
        c['modo'], c['perfil_afin'] = modo, perfil
    g = grupos(cs, ('perfil_afin', 'modo'))
    L += cb.tabla(['Perfil', 'Modo', 'Reps.', 'AGUANTA / FALLA', 'Peticiones/s', 'Errores (%)', 'p95 (ms)', 'p99 (ms)', 'CPU backend (%)', 'CPU PostgreSQL (%)', 'Heap tras GC (MB)', 'Primer tramo con más de 5 % de errores (s)'],
                  [(p, m, len(x), f"{sum(c['veredicto'] == 'AGUANTA' for c in x)} / {sum(c['veredicto'] != 'AGUANTA' for c in x)}", *fila_rendimiento(x), cb.rango([c['cpu_backend_media'] for c in x]),
                    cb.rango([c['cpu_pg_media'] for c in x]), cb.rango([c['heap_tras_gc_max_mb'] for c in x]), cb.rango([c.get('primer_fallo_s') for c in x])) for (p, m), x in g.items()]) + ['']
    L += ['### Diferencia por repetición (topología menos fijo, misma máquina y misma repetición)', '']
    filas = []
    for perfil in sorted({c['perfil_afin'] for c in cs}):
        for rep in sorted({c['repeticion'] for c in cs}):
            t = next((c for c in cs if c['perfil_afin'] == perfil and c['repeticion'] == rep and c['modo'] == 'topologia'), None)
            f = next((c for c in cs if c['perfil_afin'] == perfil and c['repeticion'] == rep and c['modo'] == 'fijo'), None)
            if t and f:
                d = lambda k: None if t[k] is None or f[k] is None else t[k] - f[k]
                filas.append((perfil, rep, cb.fmt1(d('rps')), cb.fmt1(d('errores_pct')), cb.fmt0(d('p95_ms')), cb.fmt0(d('p99_ms')), cb.fmt0(d('cpu_backend_media')), cb.fmt0(d('cpu_pg_media')), t['maquina'][:32]))
    L += cb.tabla(['Perfil', 'Rep.', 'Δ peticiones/s', 'Δ errores (puntos %)', 'Δ p95 (ms)', 'Δ p99 (ms)', 'Δ CPU backend (puntos)', 'Δ CPU PostgreSQL (puntos)', 'Máquina'], filas) + ['']
    return L


def generar(raiz, salida=None):
    raiz = Path(raiz)
    salida = Path(salida) if salida else raiz
    corridas = [analizar(*x) for x in descubrir(raiz)]
    corridas.sort(key=lambda c: (c['tipo'], c['etiqueta'], c['nivel'], c['repeticion']))
    L = ['# Investigaciones adicionales: heap, pool y afinidad', '', 'Cada celda resume las repeticiones como **mediana [mínimo–máximo]**. Las condiciones son las de la batería (misma semilla, mismo plan por nivel, base reiniciada por nivel).', '']
    L += ['## 1. Corridas incluidas', '']
    L += cb.tabla(['Tipo', 'Configuración', 'Usuarios', 'Rep.', 'Pool', 'Máquina', 'Veredicto'], [(c['tipo'], c['etiqueta'], c['nivel'], c['repeticion'], c['pool'], c['maquina'][:40], c['veredicto']) for c in corridas]) + ['']
    L += seccion_heap(corridas) + seccion_pool(corridas) + seccion_afinidad(corridas)
    L += ['## 5. Límites', '', '- Un laboratorio: PostgreSQL local con una sola CPU lógica, generador en el mismo equipo, máquinas de GitHub que pueden cambiar entre repeticiones y tres repeticiones.',
          '- Los perfiles `EXP-HEAP-*` son experimentales: no se modificó el Dockerfile ni la configuración de producción.',
          '- El monitoreo en vivo cada 3 s consume una fracción de CPU del contenedor (estimada en cerca de 1 % de una CPU) y puede no captar picos más cortos.',
          '- La memoria fuera del heap se calcula como RssAnon menos el heap comprometido: es una aproximación (incluye Metaspace, código, pilas, memoria directa y arenas de malloc).', '']
    texto = '\n'.join(L) + '\n'
    salida.mkdir(parents=True, exist_ok=True)
    (salida / 'COMPARACION_INVESTIGACION.md').write_text(texto, encoding='utf-8')
    (salida / 'comparacion_investigacion.json').write_text(json.dumps(corridas, indent=1, ensure_ascii=False, default=str), encoding='utf-8')
    planas = [{k: v for k, v in c.items() if not isinstance(v, (dict, list))} for c in corridas]
    columnas = sorted({k for p in planas for k in p})
    with open(salida / 'comparacion_investigacion.csv', 'w', encoding='utf-8', newline='') as f:
        w = csv.DictWriter(f, fieldnames=columnas)
        w.writeheader()
        w.writerows(planas)
    return texto


def main():
    raiz = sys.argv[1]
    salida = sys.argv[2] if len(sys.argv) > 2 else raiz
    sys.stdout.write(generar(raiz, salida))
    return 0


if __name__ == '__main__':
    sys.exit(main())

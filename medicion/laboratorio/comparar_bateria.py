import csv
import gzip
import json
import math
import re
import statistics
import sys
from pathlib import Path
from urllib.parse import parse_qs

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'capacidad'))
sys.path.insert(0, str(Path(__file__).resolve().parent))
import informe_nivel as n  # noqa: E402
import informe_laboratorio as lab  # noqa: E402

NOMBRE_RE = re.compile(r'bateria-r(\d+)-(CAPACIDAD-2G|PRODUCCION-512M)$')
RE_PAUSA_UPTIME = re.compile(r'\[(\d+(?:\.\d+)?)s\].*?Pause (\w+).*?(\d+)([KMG])->(\d+)([KMG])\((\d+)([KMG])\)\s+([\d.]+)ms\s*$')
A_MB = {'K': 1 / 1024, 'M': 1, 'G': 1024}
RE_ID_GC = re.compile(r'GC\((\d+)\)')
OPERACION_INFORME = 'reporte_clinico'
UMBRAL_ERRORES_TRAMO = 5.0


def leer_json(ruta):
    try:
        return json.loads(Path(ruta).read_text(encoding='utf-8'))
    except (OSError, ValueError):
        return None


def leer_texto(ruta):
    try:
        return Path(ruta).read_text(encoding='utf-8', errors='replace')
    except OSError:
        return ''


def mediana(valores):
    v = [x for x in valores if x is not None]
    return statistics.median(v) if v else None


def rango(valores, decimales=0, sufijo=''):
    v = [x for x in valores if x is not None]
    if not v:
        return 'sin datos'
    f = lambda x: f'{x:,.{decimales}f}'.replace(',', ' ')
    if len(v) == 1:
        return f'{f(v[0])}{sufijo}'
    return f'{f(statistics.median(v))}{sufijo} [{f(min(v))}–{f(max(v))}]'


def percentil(valores, p):
    return n.percentil(valores, p) if valores else None


def descubrir(raiz):
    raiz = Path(raiz)
    candidatos = [d.parent for d in raiz.rglob('entorno.txt') if (d.parent / 'niveles').is_dir()]
    corridas = []
    for d in sorted(set(candidatos)):
        m = NOMBRE_RE.search(d.name)
        entorno = leer_texto(d / 'entorno.txt')
        perfil = m.group(2) if m else None
        rep = int(m.group(1)) if m else 0
        if perfil is None:
            pm = re.search(r'^perfil del laboratorio: (CAPACIDAD-2G|PRODUCCION-512M)', entorno, re.M)
            perfil = pm.group(1) if pm else 'desconocido'
        for estado in sorted((d / 'niveles').glob('estado_despues_*.json')):
            nivel = int(estado.stem.split('_')[-1])
            corridas.append((d, perfil, rep, nivel))
    return corridas


def linea_entorno(entorno, prefijo):
    m = re.search(rf'^{re.escape(prefijo)}(.*)$', entorno, re.M)
    return m.group(1).strip() if m else None


def leer_stats(ruta, limite_cpu=1.0):
    cpus, mems = n.leer_estadisticas(ruta)
    return [c / (limite_cpu or 1) for c in cpus], mems


def sumar_io(texto):
    r = {'rbytes': 0, 'wbytes': 0, 'rios': 0, 'wios': 0}
    for par in re.findall(r'(rbytes|wbytes|rios|wios)=(\d+)', texto or ''):
        r[par[0]] += int(par[1])
    return r


def leer_seccion_io(ruta):
    secciones = {}
    actual = None
    for linea in leer_texto(ruta).splitlines():
        m = re.fullmatch(r'\[(\w+)\]', linea.strip())
        if m:
            actual = m.group(1)
            secciones[actual] = {}
        elif actual and ': ' in linea:
            k, v = linea.split(': ', 1)
            secciones[actual][k] = v
    return secciones


def campo(texto, nombre):
    m = re.search(rf'(?:^|;|\s){re.escape(nombre)}[ =](\d+)', texto or '')
    return int(m.group(1)) if m else None


def delta_io(niv, etiqueta):
    antes = leer_seccion_io(niv / f'io_{etiqueta}_antes.txt')
    despues = leer_seccion_io(niv / f'io_{etiqueta}_despues.txt')
    salida = {}
    for rol in ('backend', 'postgres'):
        a, d = antes.get(rol), despues.get(rol)
        if not a or not d or not d.get('io.stat') and not d.get('cpu.stat'):
            salida[rol] = None
            continue
        ia, id_ = sumar_io(a.get('io.stat')), sumar_io(d.get('io.stat'))
        r = {k: id_[k] - ia[k] for k in ia}
        for nombre in ('nr_throttled', 'throttled_usec', 'nr_periods'):
            va, vd = campo(a.get('cpu.stat'), nombre), campo(d.get('cpu.stat'), nombre)
            r[nombre] = None if va is None or vd is None else vd - va
        for nombre in ('oom', 'oom_kill'):
            va, vd = campo(a.get('memory.events'), nombre), campo(d.get('memory.events'), nombre)
            r[nombre] = None if va is None or vd is None else vd - va
        for sentido in ('rx_bytes', 'tx_bytes'):
            va, vd = campo(a.get('red'), sentido), campo(d.get('red'), sentido)
            r[sentido] = None if va is None or vd is None else vd - va
        salida[rol] = r
    return salida


def leer_peticiones(ruta_gz):
    filas = []
    try:
        with gzip.open(ruta_gz, 'rt', encoding='utf-8', errors='replace', newline='') as f:
            for fila in csv.DictReader(f):
                if fila.get('metric_name') != 'http_req_duration':
                    continue
                try:
                    ts = float(fila['timestamp'])
                    dur = float(fila['metric_value'])
                except (KeyError, ValueError):
                    continue
                etiquetas = parse_qs(fila.get('extra_tags', ''))
                ruta = (etiquetas.get('ruta') or ['?'])[0]
                fallo = n.es_fallo(fila.get('status', ''), fila.get('error', ''))
                filas.append((ts, dur, ruta, fallo))
    except OSError:
        return None
    return filas


def por_operacion(peticiones):
    ops = {}
    for _, dur, ruta, fallo in peticiones:
        o = ops.setdefault(ruta, {'n': 0, 'errores': 0, 'dur': []})
        o['n'] += 1
        o['errores'] += 1 if fallo else 0
        o['dur'].append(dur)
    total = sum(sum(o['dur']) for o in ops.values()) or 1
    salida = {}
    for ruta, o in ops.items():
        salida[ruta] = {'n': o['n'], 'errores': o['errores'], 'p50': statistics.median(o['dur']), 'p95': percentil(o['dur'], 0.95),
                        'p99': percentil(o['dur'], 0.99), 'max': max(o['dur']), 'tiempo_total_s': sum(o['dur']) / 1000, 'pct_tiempo': sum(o['dur']) / total * 100}
    return salida


def tramos(peticiones):
    if not peticiones:
        return []
    inicio = min(p[0] for p in peticiones)
    cubos = {}
    for ts, dur, _, fallo in peticiones:
        c = int((ts - inicio) // 15) * 15
        b = cubos.setdefault(c, {'dur': [], 'fallos': 0})
        b['dur'].append(dur)
        b['fallos'] += 1 if fallo else 0
    return [{'desde_s': c, 'n': len(b['dur']), 'errores_pct': b['fallos'] / len(b['dur']) * 100, 'p95': percentil(b['dur'], 0.95)} for c, b in sorted(cubos.items())]


def heap_en_informes(peticiones, vivo, periodo_s):
    resultado = {'informes': 0, 'heap_en_informes_mediana_mb': None, 'heap_en_informes_max_mb': None, 'heap_fuera_mediana_mb': None}
    if not peticiones or not vivo:
        return resultado
    muestras = []
    for fila in vivo:
        t, h = lab.numero(fila.get('ts')), lab.numero(fila.get('heap_usado_kb'))
        if t is not None and h is not None:
            muestras.append((t, h / 1024))
    if not muestras:
        return resultado
    ventanas = []
    for ts, dur, ruta, _ in peticiones:
        if ruta == OPERACION_INFORME:
            ventanas.append((ts - dur / 1000 - 1, ts + periodo_s))
    dentro, fuera, maximos = [], [], []
    for t, h in muestras:
        if any(a <= t <= b for a, b in ventanas):
            dentro.append(h)
        else:
            fuera.append(h)
    for a, b in ventanas:
        en = [h for t, h in muestras if a <= t <= b]
        if en:
            maximos.append(max(en))
    resultado.update({'informes': len(ventanas), 'heap_en_informes_mediana_mb': mediana(maximos), 'heap_en_informes_max_mb': max(maximos) if maximos else None,
                      'heap_fuera_mediana_mb': mediana(fuera)})
    return resultado


def pausas_en_ventana(ruta_gz, duracion_s, vivo=None):
    pausas = []
    try:
        with gzip.open(ruta_gz, 'rt', encoding='utf-8', errors='replace') as f:
            for linea in f:
                m = RE_PAUSA_UPTIME.search(linea)
                if m:
                    ident = RE_ID_GC.search(linea)
                    pausas.append((float(m.group(1)), m.group(2), float(m.group(9)), float(m.group(5)) * A_MB[m.group(6)], int(ident.group(1)) if ident else None))
    except OSError:
        return None
    if not pausas:
        return {'pausas': 0, 'total_ms': 0.0, 'max_ms': None, 'completas': 0, 'por_minuto': None, 'pct_tiempo': None, 'despues_max_mb': None, 'criterio': 'sin pausas'}
    inicio_id = None
    for fila in vivo or []:
        y, fg = lab.numero(fila.get('ygc')), lab.numero(fila.get('fgc'))
        if y is not None and fg is not None:
            inicio_id = int(y + fg)
            break
    if inicio_id is not None and all(p[4] is not None for p in pausas):
        ventana = [p for p in pausas if p[4] >= inicio_id]
        criterio = 'recolecciones posteriores al primer muestreo en vivo'
    else:
        fin = max(p[0] for p in pausas)
        ventana = [p for p in pausas if p[0] >= fin - duracion_s]
        criterio = 'aproximación por tiempo (últimos segundos de la JVM): incluye parte del arranque'
    total = sum(p[2] for p in ventana)
    return {'despues_max_mb': max((p[3] for p in ventana), default=None), 'pausas': len(ventana), 'total_ms': total, 'max_ms': max((p[2] for p in ventana), default=None),
            'completas': sum(1 for p in ventana if p[1] == 'Full'), 'por_minuto': len(ventana) / (duracion_s / 60) if duracion_s else None,
            'pct_tiempo': total / 1000 / duracion_s * 100 if duracion_s else None, 'criterio': criterio}


def contar_oom_en_log(ruta):
    total = 0
    for linea in leer_texto(ruta).splitlines():
        if 'OutOfMemoryError' in linea:
            total += 1
    return total


def analizar_corrida(d, perfil, rep, nivel, umbral_generador=80.0):
    niv = d / 'niveles'
    etiqueta = f'despues_{nivel}'
    entorno = leer_texto(d / 'entorno.txt')
    estado = leer_json(niv / f'estado_{etiqueta}.json') or {}
    k6 = leer_json(niv / f'k6_{etiqueta}.json') or {}
    limite_cpu = estado.get('cpus_limite') or 1
    cpus_b, mems_b = leer_stats(niv / f'stats_{etiqueta}.csv', limite_cpu)
    cpus_pg, _ = leer_stats(niv / f'stats_pg_{etiqueta}.csv', 1)
    jvm = lab.evaluar_jvm(niv, etiqueta)
    gen = lab.evaluar_generador(niv, etiqueta, umbral_generador)
    io = delta_io(niv, etiqueta)
    peticiones = leer_peticiones(niv / f'k6_{etiqueta}.csv.gz')
    vivo = lab.leer_csv(niv / f'jvm_vivo_{etiqueta}.csv') or []
    ops = por_operacion(peticiones) if peticiones else {}
    trs = tramos(peticiones or [])
    lat = k6.get('latencia_ms') or {}
    estados = k6.get('estados') or {}
    total = k6.get('peticiones') or 0
    duracion_k6 = k6.get('duracion_total_s') or 0
    m = {'peticiones': total, 'errores_pct': ((total - (estados.get('2xx') or 0)) / total * 100) if total else None, 'p95_ms': lat.get('p95')}
    informe = ops.get(OPERACION_INFORME) or {}
    gc = pausas_en_ventana(niv / f'jvm_gc_{etiqueta}.log.gz', estado.get('duracion_s') or duracion_k6, vivo)
    primer_error = next((t['desde_s'] for t in trs if t['n'] >= 3 and t['errores_pct'] > UMBRAL_ERRORES_TRAMO), None)
    oom_log = contar_oom_en_log(d / 'logs' / f'backend_carga_{etiqueta}.log')
    caida = (estado.get('caida_backend') or '').strip() or None
    r = {
        'perfil': perfil, 'repeticion': rep, 'nivel': nivel, 'directorio': str(d),
        'maquina': linea_entorno(entorno, 'procesador:') or 'desconocida',
        'modo_afinidad': linea_entorno(entorno, 'modo de afinidad:') or 'no registrado (anterior a la afinidad por topología)',
        'mezcla': linea_entorno(entorno, 'mezcla de operaciones:') or 'no registrada',
        'veredicto': n.veredicto(m).split(' (')[0], 'peticiones': total, 'rps': (total / duracion_k6) if duracion_k6 else None,
        'errores_pct': m['errores_pct'], 'sin_respuesta': estados.get('sin_respuesta'), 'err_5xx': estados.get('5xx'),
        'p50_ms': lat.get('mediana'), 'p95_ms': lat.get('p95'), 'p99_ms': lat.get('p99'), 'max_ms': lat.get('max'),
        'vus_activos_max': k6.get('vus_activos_max'), 'acciones': k6.get('acciones') or {},
        'cpu_backend_media': statistics.mean(cpus_b) if cpus_b else None, 'cpu_backend_max': max(cpus_b) if cpus_b else None,
        'cpu_backend_tope_pct': (sum(1 for c in cpus_b if c >= 95) / len(cpus_b) * 100) if cpus_b else None,
        'cpu_pg_media': statistics.mean(cpus_pg) if cpus_pg else None,
        'throttled_pct': None, 'mem_docker_pico_mb': max(mems_b) if mems_b else None,
        'cont_pico_cgroup_mb': jvm.get('cont_pico_cgroup_mb'), 'cont_actual_max_mb': jvm.get('cont_actual_max_mb'), 'limite_mem_mb': estado.get('memoria_limite_mb'),
        'heap_max': estado.get('heap_max'), 'heap_usado_max_mb': jvm.get('heap_usado_max_mb'), 'heap_comprometido_max_mb': jvm.get('heap_comprometido_max_mb'),
        'heap_tras_gc_max_mb': (gc or {}).get('despues_max_mb') or (jvm.get('gc_registro') or {}).get('usado_despues_max_mb'), 'heap_tras_gc_vida_max_mb': (jvm.get('gc_registro') or {}).get('usado_despues_max_mb'), 'metaspace_max_mb': jvm.get('metaspace_usado_max_mb'),
        'gc': gc, 'oom_killed': estado.get('oom_killed'), 'reinicios': estado.get('reinicios'), 'sigue_corriendo': estado.get('sigue_corriendo'),
        'codigo_salida_contenedor': estado.get('codigo_salida'), 'salud_final': estado.get('salud_final_http'), 'k6_codigo': estado.get('k6_codigo_salida'),
        'caida_backend': caida, 'oom_java_en_log': oom_log, 'primer_tramo_con_errores_s': primer_error,
        'peor_p95_tramo_ms': max((t['p95'] for t in trs if t['p95'] is not None), default=None),
        'generador_cpu_media': gen.get('cpu_media_pct'), 'generador_cpu_max': gen.get('cpu_max_pct'), 'generador_interpretable': gen.get('interpretable'),
        'generador_motivos': gen.get('motivos'), 'io': io, 'informe_clinico': informe,
        'operaciones': ops, 'pg_sentencias': estado.get('sql_sentencias'), 'pg_tiempo_sql_s': estado.get('sql_tiempo_s'),
    }
    r.update({'heap_informes': heap_en_informes(peticiones, vivo, 5.0)})
    b = io.get('backend') or {}
    if b.get('nr_periods'):
        r['throttled_pct'] = (b.get('nr_throttled') or 0) / b['nr_periods'] * 100
    r['carga_alcanzada'] = None if r['vus_activos_max'] is None else r['vus_activos_max'] >= nivel
    r['estable'] = (not r['oom_killed'] and (r['reinicios'] or 0) == 0 and r['sigue_corriendo'] is not False and str(r['salud_final']) == '200'
                    and not r['caida_backend'] and (r['oom_java_en_log'] or 0) == 0 and not (b.get('oom_kill') or 0))
    return r


def agrupar(corridas):
    grupos = {}
    for c in corridas:
        grupos.setdefault((c['perfil'], c['nivel']), []).append(c)
    return dict(sorted(grupos.items(), key=lambda kv: (kv[0][0], kv[0][1])))


def mb(bytes_):
    return None if bytes_ is None else bytes_ / 1048576


def val(c, *ruta):
    x = c
    for k in ruta:
        if x is None:
            return None
        x = x.get(k) if isinstance(x, dict) else None
    return x


def conclusion(perfil, nivel, cs, minimo_reps=3):
    razones = []
    if len(cs) < minimo_reps:
        razones.append(f'hay {len(cs)} repetición(es) de las {minimo_reps} previstas')
    for c in cs:
        pref = f"repetición {c['repeticion']}"
        if c['veredicto'] != 'AGUANTA':
            razones.append(f"{pref}: veredicto {c['veredicto']} (errores {fmt1(c['errores_pct'])} %, p95 {fmt0(c['p95_ms'])} ms)")
        if c['oom_killed']:
            razones.append(f'{pref}: el contenedor fue terminado por falta de memoria (OOMKilled)')
        if c['oom_java_en_log']:
            razones.append(f"{pref}: {c['oom_java_en_log']} líneas de OutOfMemoryError en el registro del backend")
        if (c['reinicios'] or 0) > 0:
            razones.append(f"{pref}: {c['reinicios']} reinicio(s) del contenedor")
        if c['caida_backend']:
            razones.append(f"{pref}: el backend se detuvo durante la carga ({c['caida_backend']})")
        elif c['sigue_corriendo'] is False or (c['salud_final'] is not None and str(c['salud_final']) != '200'):
            razones.append(f"{pref}: el backend no estaba sano al final (salud {c['salud_final']})")
        if c['generador_interpretable'] != 'sí':
            razones.append(f"{pref}: generador k6 {c['generador_interpretable']} ({'; '.join(c['generador_motivos'] or [])})")
        if c['carga_alcanzada'] is False:
            razones.append(f"{pref}: k6 alcanzó {c['vus_activos_max']} de {nivel} usuarios")
        if c['informe_clinico'] and c['informe_clinico'].get('errores'):
            razones.append(f"{pref}: {c['informe_clinico']['errores']} de {c['informe_clinico']['n']} informes clínicos con error")
        if not c['informe_clinico']:
            razones.append(f'{pref}: no hay informes clínicos en los datos (no se puede afirmar nada sobre ellos)')
        ht, hm = c['heap_tras_gc_max_mb'], c['heap_max']
        if ht and hm and hm.endswith('m') and ht >= 0.9 * float(hm[:-1]):
            razones.append(f"{pref}: el heap tras GC llegó a {ht:.0f} MB de {hm[:-1]} MB ({ht / float(hm[:-1]) * 100:.0f} %)")
    if not razones:
        return (f'**{perfil} con {nivel} usuarios:** en las {len(cs)} repeticiones cumplió los criterios experimentales de este laboratorio (errores menores a 1 %, p95 menor a 2 s, sin OOM, reinicios ni caídas, generador k6 sin saturar y con los usuarios previstos). '
                'Eso no equivale a una garantía para producción: el laboratorio usa una base local, un generador en el mismo equipo y 3 repeticiones.')
    return f'**{perfil} con {nivel} usuarios: no queda demostrado que lo soporte.** ' + '; '.join(razones) + '.'


def fmt0(x):
    return 'sin datos' if x is None else f'{x:,.0f}'.replace(',', ' ')


def fmt1(x):
    return 'sin datos' if x is None else f'{x:,.1f}'.replace(',', ' ')


def tabla(cabecera, filas):
    return ['| ' + ' | '.join(cabecera) + ' |', '|' + '---|' * len(cabecera)] + ['| ' + ' | '.join(str(x) for x in f) + ' |' for f in filas]


def generar(raiz, salida=None):
    raiz = Path(raiz)
    salida = Path(salida) if salida else raiz
    corridas = [analizar_corrida(d, p, r, nv) for d, p, r, nv in descubrir(raiz)]
    corridas.sort(key=lambda c: (c['perfil'], c['nivel'], c['repeticion']))
    grupos = agrupar(corridas)
    L = ['# Comparación de la batería: perfiles, niveles y repeticiones', '']
    if not corridas:
        L.append('No se encontraron corridas (carpetas `bateria-rN-PERFIL` con `entorno.txt` y `niveles/`).')
        (salida / 'COMPARACION_BATERIA.md').write_text('\n'.join(L) + '\n', encoding='utf-8')
        return '\n'.join(L)
    L += ['Cada celda resume las repeticiones disponibles como **mediana [mínimo–máximo]**. Todos los números salen de los archivos de cada corrida (k6, cgroup, contadores de la JVM, registro de GC, `docker stats`); los umbrales de veredicto son los del laboratorio (AGUANTA: menos de 1 % de errores y p95 menor a 2 s).', '']
    L += ['## 1. Corridas incluidas', '']
    L += tabla(['Perfil', 'Nivel', 'Rep.', 'Máquina', 'Afinidad', 'Mezcla de operaciones'],
               [(c['perfil'], c['nivel'], c['repeticion'], c['maquina'], c['modo_afinidad'][:90], c['mezcla'][:90]) for c in corridas]) + ['']
    L += ['Las máquinas pueden diferir entre repeticiones; la comparación entre perfiles es más fiable dentro de una misma repetición.', '']

    L += ['## 2. Veredicto y estabilidad', '']
    L += tabla(['Perfil', 'Nivel', 'Reps.', 'AGUANTA / DEGRADADO / FALLA', 'Usuarios activos', 'Generador k6 interpretable', 'OOM / reinicios / caídas / OutOfMemoryError', 'Primer tramo con errores > 5 % (s)'],
               [(p, nv, len(cs),
                 f"{sum(c['veredicto'] == 'AGUANTA' for c in cs)} / {sum(c['veredicto'] == 'DEGRADADO' for c in cs)} / {sum(c['veredicto'] not in ('AGUANTA', 'DEGRADADO') for c in cs)}",
                 rango([c['vus_activos_max'] for c in cs]), f"{sum(c['generador_interpretable'] == 'sí' for c in cs)} de {len(cs)}",
                 f"{sum(bool(c['oom_killed']) for c in cs)} / {sum((c['reinicios'] or 0) > 0 for c in cs)} / {sum(bool(c['caida_backend']) for c in cs)} / {sum((c['oom_java_en_log'] or 0) > 0 for c in cs)}",
                 rango([c['primer_tramo_con_errores_s'] for c in cs]) if any(c['primer_tramo_con_errores_s'] is not None for c in cs) else 'ninguno')
                for (p, nv), cs in grupos.items()]) + ['']

    L += ['## 3. Rendimiento: throughput, errores y latencia', '']
    L += tabla(['Perfil', 'Nivel', 'Peticiones/s', 'Errores (%)', 'Sin respuesta', 'p50 (ms)', 'p95 (ms)', 'p99 (ms)', 'Peor p95 en 15 s (ms)'],
               [(p, nv, rango([c['rps'] for c in cs], 1), rango([c['errores_pct'] for c in cs], 1), rango([c['sin_respuesta'] for c in cs]),
                 rango([c['p50_ms'] for c in cs]), rango([c['p95_ms'] for c in cs]), rango([c['p99_ms'] for c in cs]), rango([c['peor_p95_tramo_ms'] for c in cs]))
                for (p, nv), cs in grupos.items()]) + ['']

    L += ['## 4. CPU', '']
    L += tabla(['Perfil', 'Nivel', 'Backend: media (% de 1 CPU)', 'Backend: tiempo al tope (%)', 'Backend: periodos limitados por el cgroup (%)', 'PostgreSQL: media (% de 1 núcleo)', 'k6: media / máx (% de su CPU)'],
               [(p, nv, rango([c['cpu_backend_media'] for c in cs]), rango([c['cpu_backend_tope_pct'] for c in cs]), rango([c['throttled_pct'] for c in cs], 1),
                 rango([c['cpu_pg_media'] for c in cs]), f"{rango([c['generador_cpu_media'] for c in cs])} / {rango([c['generador_cpu_max'] for c in cs])}")
                for (p, nv), cs in grupos.items()]) + ['']

    L += ['## 5. Memoria, heap, Metaspace y GC', '', 'El heap usado y comprometido vienen de los contadores de la JVM en vivo; el heap tras GC y el GC, del registro `-Xlog:gc`, **limitado a las recolecciones posteriores al primer muestreo en vivo** (es decir, durante el nivel; el registro completo incluye el arranque y la preparación, que también recolectan mucho y no son carga).', '']
    L += tabla(['Perfil', 'Nivel', 'Heap máx.', 'Heap usado máx. (MB)', 'Heap comprometido máx. (MB)', 'Heap tras GC máx. en el nivel (MB)', 'Metaspace máx. (MB)', 'Contenedor: pico cgroup (MB) de límite', 'Pausas de GC por minuto', 'Pausas completas', 'Tiempo en GC (%)', 'Pausa máxima (ms)'],
               [(p, nv, cs[0]['heap_max'], rango([c['heap_usado_max_mb'] for c in cs]), rango([c['heap_comprometido_max_mb'] for c in cs]), rango([c['heap_tras_gc_max_mb'] for c in cs]),
                 rango([c['metaspace_max_mb'] for c in cs]), f"{rango([c['cont_pico_cgroup_mb'] or c['mem_docker_pico_mb'] for c in cs])} de {cs[0]['limite_mem_mb']}",
                 rango([val(c, 'gc', 'por_minuto') for c in cs], 1), rango([val(c, 'gc', 'completas') for c in cs]), rango([val(c, 'gc', 'pct_tiempo') for c in cs], 1),
                 rango([val(c, 'gc', 'max_ms') for c in cs]))
                for (p, nv), cs in grupos.items()]) + ['']

    L += ['## 6. Entrada y salida (cgroup y red, durante el nivel)', '']
    filas_io = []
    for (p, nv), cs in grupos.items():
        def io(rol, k, esc=1048576):
            return rango([(None if val(c, 'io', rol, k) is None else val(c, 'io', rol, k) / esc) for c in cs], 1)
        filas_io.append((p, nv, io('postgres', 'wbytes'), io('postgres', 'rbytes'), io('postgres', 'wios', 1), io('backend', 'wbytes'), io('backend', 'rbytes'), io('backend', 'tx_bytes'), io('postgres', 'tx_bytes')))
    L += tabla(['Perfil', 'Nivel', 'PostgreSQL: escrito (MB)', 'PostgreSQL: leído (MB)', 'PostgreSQL: escrituras (operaciones)', 'Backend: escrito (MB)', 'Backend: leído (MB)', 'Backend: red enviada (MB)', 'PostgreSQL: red enviada (MB)'], filas_io) + ['']
    L += ['Si una celda dice «sin datos», el runner no expuso `io.stat` o la red del contenedor para ese nivel; no se sustituye por una estimación.', '']

    L += ['## 7. Informes clínicos (`reporte_clinico`, 365 días, unas 9.000 a 10.000 filas)', '']
    L += tabla(['Perfil', 'Nivel', 'Rep.', 'Informes', 'Con error', 'p50 (ms)', 'p95 (ms)', 'p99 (ms)', 'Máx. (ms)', 'Heap usado en ventanas de informe: mediana de picos / máx. (MB)', 'Heap usado fuera de ellas: mediana (MB)'],
               [(c['perfil'], c['nivel'], c['repeticion'], val(c, 'informe_clinico', 'n') or 0, val(c, 'informe_clinico', 'errores') or 0, fmt0(val(c, 'informe_clinico', 'p50')),
                 fmt0(val(c, 'informe_clinico', 'p95')), fmt0(val(c, 'informe_clinico', 'p99')), fmt0(val(c, 'informe_clinico', 'max')),
                 f"{fmt0(val(c, 'heap_informes', 'heap_en_informes_mediana_mb'))} / {fmt0(val(c, 'heap_informes', 'heap_en_informes_max_mb'))}", fmt0(val(c, 'heap_informes', 'heap_fuera_mediana_mb')))
                for c in corridas]) + ['']
    L += ['La relación entre un informe y el heap es solo temporal (una muestra cada pocos segundos, con otras peticiones en paralelo): indica asociación, no causa.', '']

    L += ['## 8. Qué operaciones consumen más (por perfil y nivel, mediana de las repeticiones)', '']
    for (p, nv), cs in grupos.items():
        rutas = sorted({r for c in cs for r in c['operaciones']})
        filas = []
        for r in rutas:
            datos = [c['operaciones'].get(r) for c in cs if r in c['operaciones']]
            filas.append((r, mediana([d['n'] for d in datos]), mediana([d['errores'] for d in datos]), mediana([d['p50'] for d in datos]), mediana([d['p95'] for d in datos]),
                          mediana([d['p99'] for d in datos]), mediana([d['tiempo_total_s'] for d in datos]), mediana([d['pct_tiempo'] for d in datos])))
        filas.sort(key=lambda f: -(f[7] or 0))
        L += [f'### {p}, {nv} usuarios', '']
        L += tabla(['Operación', 'Peticiones', 'Errores', 'p50 (ms)', 'p95 (ms)', 'p99 (ms)', 'Tiempo total en el servidor (s)', '% del tiempo total'],
                   [(f[0], fmt0(f[1]), fmt0(f[2]), fmt0(f[3]), fmt0(f[4]), fmt0(f[5]), fmt1(f[6]), fmt1(f[7])) for f in filas[:15]]) + ['']
        acciones = {}
        for c in cs:
            for k, v in (c['acciones'] or {}).items():
                acciones.setdefault(k, []).append(v)
        if acciones:
            L += ['Acciones elegidas por k6 (mediana): ' + ', '.join(f'{k} {fmt0(mediana(v))}' for k, v in acciones.items()), '']
    L += ['El «tiempo total en el servidor» suma las duraciones de las peticiones de cada operación y sirve para ver cuál ocupa más tiempo; no es CPU por operación (eso no se mide).', '']

    L += ['## 9. Por repetición', '']
    L += tabla(['Perfil', 'Nivel', 'Rep.', 'Veredicto', 'Pet./s', 'Errores (%)', 'p95 (ms)', 'p99 (ms)', 'CPU backend (%)', 'Heap tras GC (MB)', 'Contenedor pico (MB)', 'Pausas completas', 'OOM / reinicios', 'Generador k6', 'Estable'],
               [(c['perfil'], c['nivel'], c['repeticion'], c['veredicto'], fmt1(c['rps']), fmt1(c['errores_pct']), fmt0(c['p95_ms']), fmt0(c['p99_ms']), fmt0(c['cpu_backend_media']),
                 fmt0(c['heap_tras_gc_max_mb']), fmt0(c['cont_pico_cgroup_mb'] or c['mem_docker_pico_mb']), fmt0(val(c, 'gc', 'completas')), f"{'sí' if c['oom_killed'] else 'no'} / {c['reinicios']}",
                 c['generador_interpretable'], 'sí' if c['estable'] else '**NO**') for c in corridas]) + ['']

    L += ['## 10. Conclusiones', '']
    for (p, nv), cs in grupos.items():
        L += ['- ' + conclusion(p, nv, cs), '']
    L += ['## 11. Límites', '',
          '- Es un laboratorio: PostgreSQL local, un generador en el mismo equipo, máquinas de GitHub que cambian entre ejecuciones y tres repeticiones. Las cifras no se trasladan a Render ni a Supabase.',
          '- Los veredictos usan umbrales experimentales (1 % y 2 s para AGUANTA; 80 % de CPU de k6 para el generador).',
          '- El GC del nivel se obtiene de las recolecciones posteriores al primer muestreo en vivo (si faltara, se aproxima por tiempo y la columna de criterio lo indica en el JSON); el heap en vivo se muestrea cada pocos segundos y puede no captar picos más cortos.',
          '- La atribución de memoria a un tipo de operación es temporal, no causal.', '']
    texto = '\n'.join(L) + '\n'
    (salida / 'COMPARACION_BATERIA.md').write_text(texto, encoding='utf-8')
    plano = []
    for c in corridas:
        fila = {k: v for k, v in c.items() if not isinstance(v, (dict, list))}
        fila.update({f'gc_{k}': v for k, v in (c['gc'] or {}).items()})
        fila.update({f'informe_{k}': v for k, v in (c['informe_clinico'] or {}).items()})
        fila.update({f'heap_inf_{k}': v for k, v in c['heap_informes'].items()})
        for rol, d in c['io'].items():
            for k, v in (d or {}).items():
                fila[f'io_{rol}_{k}'] = v
        plano.append(fila)
    columnas = sorted({k for f in plano for k in f})
    with open(salida / 'comparacion_bateria.csv', 'w', encoding='utf-8', newline='') as f:
        w = csv.DictWriter(f, fieldnames=columnas)
        w.writeheader()
        w.writerows(plano)
    (salida / 'comparacion_bateria.json').write_text(json.dumps(corridas, indent=1, ensure_ascii=False, default=str), encoding='utf-8')
    with open(salida / 'operaciones_bateria.csv', 'w', encoding='utf-8', newline='') as f:
        w = csv.writer(f)
        w.writerow(['perfil', 'nivel', 'repeticion', 'operacion', 'peticiones', 'errores', 'p50_ms', 'p95_ms', 'p99_ms', 'max_ms', 'tiempo_total_s', 'pct_tiempo'])
        for c in corridas:
            for r, o in sorted(c['operaciones'].items()):
                w.writerow([c['perfil'], c['nivel'], c['repeticion'], r, o['n'], o['errores'], round(o['p50'], 1), o['p95'], o['p99'], o['max'], round(o['tiempo_total_s'], 2), round(o['pct_tiempo'], 2)])
    return texto


def main():
    raiz = sys.argv[1]
    salida = sys.argv[2] if len(sys.argv) > 2 else raiz
    Path(salida).mkdir(parents=True, exist_ok=True)
    sys.stdout.write(generar(raiz, salida))
    return 0


if __name__ == '__main__':
    sys.exit(main())

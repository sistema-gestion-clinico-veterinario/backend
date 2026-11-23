import csv
import gzip
import json
import math
import re
import statistics
import sys
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import parse_qs

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'capacidad'))
sys.path.insert(0, str(Path(__file__).resolve().parent))
import informe_nivel as n  # noqa: E402
import informe_laboratorio as lab  # noqa: E402

RE_FECHA = re.compile(r'^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2}:\d{2})(?:\.(\d+))?Z?')
RE_HIKARI_STATS = re.compile(r'(Before cleanup|After cleanup|After adding|Timeout failure) +stats \(total=(\d+), active=(\d+), idle=(\d+), waiting=(\d+)\)')
RE_SIN_CONEXION = re.compile(r'Connection is not available|request timed out after')
RE_HIKARI_ERROR = re.compile(r'request timed out after \d+ms \(total=(\d+), active=(\d+), idle=(\d+), waiting=(\d+)\)')
CUBO_S = 5


def epoch_de_linea(linea):
    m = RE_FECHA.match(linea)
    if not m:
        return None
    base = datetime.strptime(f'{m.group(1)} {m.group(2)}', '%Y-%m-%d %H:%M:%S').replace(tzinfo=timezone.utc).timestamp()
    return base + (float('0.' + m.group(3)) if m.group(3) else 0.0)


def leer_k6(ruta_gz):
    peticiones, vus = [], []
    try:
        with gzip.open(ruta_gz, 'rt', encoding='utf-8', errors='replace', newline='') as f:
            for fila in csv.DictReader(f):
                metrica = fila.get('metric_name')
                try:
                    ts = float(fila['timestamp'])
                    valor = float(fila['metric_value'])
                except (KeyError, ValueError):
                    continue
                if metrica == 'vus':
                    vus.append((ts, valor))
                elif metrica == 'http_req_duration':
                    etiquetas = parse_qs(fila.get('extra_tags', ''))
                    peticiones.append({'ts': ts, 'dur': valor, 'ruta': (etiquetas.get('ruta') or ['?'])[0], 'status': fila.get('status', ''),
                                       'error': fila.get('error', ''), 'fallo': n.es_fallo(fila.get('status', ''), fila.get('error', ''))})
    except OSError:
        return None, None
    peticiones.sort(key=lambda p: p['ts'])
    return peticiones, sorted(vus)


def leer_vivo(ruta):
    filas = lab.leer_csv(ruta) or []
    salida = []
    for f in filas:
        t = lab.numero(f.get('ts'))
        if t is None:
            continue
        salida.append({k: lab.numero(v) for k, v in f.items() if k not in ('fuente',)} | {'ts': t})
    return salida


def leer_stats(ruta, limite_cpu=1.0):
    filas = []
    try:
        for linea in Path(ruta).read_text(encoding='utf-8').splitlines():
            partes = linea.split(',', 2)
            if len(partes) < 3:
                continue
            cpu = re.match(r'([\d.]+)%', partes[1])
            mem = re.match(r'([\d.]+)\s*([KMG]i?B)', partes[2])
            if cpu and mem:
                factor = {'KIB': 1 / 1024, 'KB': 1 / 1024, 'MIB': 1, 'MB': 1, 'GIB': 1024, 'GB': 1024}.get(mem.group(2).upper(), 1)
                filas.append({'ts': float(partes[0]), 'cpu': float(cpu.group(1)) / (limite_cpu or 1), 'mem_mb': float(mem.group(1)) * factor})
    except (OSError, ValueError):
        return []
    return filas


def leer_pg(ruta):
    filas = lab.leer_csv(ruta) or []
    return [{k: lab.numero(v) for k, v in f.items()} for f in filas if lab.numero(f.get('ts')) is not None]


def leer_log(ruta):
    ev = {'oom': [], 'sin_conexion': [], 'hikari': []}
    try:
        with open(ruta, encoding='utf-8', errors='replace') as f:
            for linea in f:
                t = epoch_de_linea(linea)
                if 'OutOfMemoryError' in linea:
                    ev['oom'].append((t, linea.strip()[:160]))
                if RE_SIN_CONEXION.search(linea):
                    ev['sin_conexion'].append(t)
                m = RE_HIKARI_STATS.search(linea)
                if m and t is not None:
                    ev['hikari'].append({'ts': t, 'tipo': m.group(1), 'total': int(m.group(2)), 'activas': int(m.group(3)), 'inactivas': int(m.group(4)), 'esperando': int(m.group(5))})
                else:
                    m = RE_HIKARI_ERROR.search(linea)
                    if m and t is not None:
                        ev['hikari'].append({'ts': t, 'tipo': 'Tiempo agotado', 'total': int(m.group(1)), 'activas': int(m.group(2)), 'inactivas': int(m.group(3)), 'esperando': int(m.group(4))})
    except OSError:
        return None
    return ev


def mb_a_numero(texto):
    m = re.match(r'(\d+(?:\.\d+)?)([mMgG])', str(texto or ''))
    if not m:
        return None
    return float(m.group(1)) * (1024 if m.group(2) in 'gG' else 1)


def cubos(peticiones, t0, fin):
    salida = []
    for i in range(int(math.ceil((fin - t0) / CUBO_S)) + 1):
        salida.append({'t': i * CUBO_S, 'n': 0, 'fallos': 0, 'dur': []})
    for p in peticiones:
        k = int((p['ts'] - t0) // CUBO_S)
        if 0 <= k < len(salida):
            salida[k]['n'] += 1
            salida[k]['fallos'] += 1 if p['fallo'] else 0
            salida[k]['dur'].append(p['dur'])
    for c in salida:
        c['errores_pct'] = (c['fallos'] / c['n'] * 100) if c['n'] else None
        c['p95'] = n.percentil(c['dur'], 0.95) if c['dur'] else None
        del c['dur']
    return salida


def mediana(v):
    v = [x for x in v if x is not None]
    return statistics.median(v) if v else None


def analizar(d, nivel, limite_cpu=None):
    d = Path(d)
    niv = d / 'niveles'
    et = f'despues_{nivel}'
    estado = lab.leer_json(niv / f'estado_{et}.json') or {}
    limite_cpu = limite_cpu or estado.get('cpus_limite') or 1
    peticiones, vus = leer_k6(niv / f'k6_{et}.csv.gz')
    if peticiones is None:
        return None
    vivo = leer_vivo(niv / f'jvm_vivo_{et}.csv')
    stats = leer_stats(niv / f'stats_{et}.csv', limite_cpu)
    stats_pg = leer_stats(niv / f'stats_pg_{et}.csv', 1)
    pg = leer_pg(niv / f'pg_actividad_{et}.csv')
    log = leer_log(d / 'logs' / f'backend_carga_{et}.log') or {'oom': [], 'sin_conexion': [], 'hikari': []}
    t0 = vus[0][0] if vus else (peticiones[0]['ts'] if peticiones else 0)
    fin = max([p['ts'] for p in peticiones] + [t0]) if peticiones else t0
    xmx = mb_a_numero(estado.get('heap_max'))
    limite_mem = estado.get('memoria_limite_mb')
    rel = lambda t: None if t is None else t - t0

    fallos = [p for p in peticiones if p['fallo']]
    primer_fallo = fallos[0]['ts'] if fallos else None
    vus_en = lambda t: next((v for ts, v in reversed(vus) if ts <= t), None) if t is not None else None
    primeros = [{'t_s': round(p['ts'] - t0, 1), 'ruta': p['ruta'], 'status': p['status'] or 'sin respuesta', 'error': p['error'], 'dur_ms': round(p['dur'])} for p in fallos[:12]]
    ventana = [p for p in fallos if primer_fallo is not None and p['ts'] <= primer_fallo + 30]
    rutas = {}
    for p in ventana:
        rutas[p['ruta']] = rutas.get(p['ruta'], 0) + 1

    eventos = {}
    if primer_fallo is not None:
        eventos['primer_fallo_de_k6'] = primer_fallo
    for fila in vivo:
        if xmx and fila.get('heap_usado_kb') is not None and fila['heap_usado_kb'] / 1024 >= 0.9 * xmx:
            eventos['heap_usado_sobre_90pct_del_tope'] = fila['ts']
            break
    for a, b in zip(vivo, vivo[1:]):
        if a.get('fgc') is not None and b.get('fgc') is not None and b['fgc'] - a['fgc'] >= 2:
            eventos['primera_rafaga_de_gc_completo'] = b['ts']
            break
    for fila in vivo:
        if limite_mem and fila.get('cont_actual_kb') is not None and fila['cont_actual_kb'] / 1024 >= 0.8 * limite_mem:
            eventos['contenedor_sobre_80pct_de_memoria'] = fila['ts']
            break
    for fila in stats:
        if fila['cpu'] >= 95:
            eventos['cpu_del_backend_al_tope'] = fila['ts']
            break
    ooms = [t for t, _ in log['oom'] if t is not None]
    if ooms:
        eventos['primer_OutOfMemoryError'] = min(ooms)
    sc = [t for t in log['sin_conexion'] if t is not None]
    if sc:
        eventos['primer_tiempo_agotado_del_pool'] = min(sc)
    orden = sorted(((round(t - t0, 1), nombre) for nombre, t in eventos.items()), key=lambda x: x[0])
    cub = cubos(peticiones, t0, fin)
    primer_5 = next((cb for cb in cub if cb['n'] >= 3 and cb['errores_pct'] is not None and cb['errores_pct'] > 5), None)
    usuarios_5 = vus_en(t0 + primer_5['t']) if primer_5 else None

    def en_ventana(filas, desde, hasta, clave):
        return [f[clave] for f in filas if f.get(clave) is not None and desde <= f['ts'] <= hasta]

    antes = None
    if primer_fallo is not None:
        desde, hasta = primer_fallo - 30, primer_fallo
        base = [f for f in vivo if t0 <= f['ts'] <= t0 + 20]
        antes = {
            'heap_usado_max_mb': max(en_ventana(vivo, desde, hasta, 'heap_usado_kb'), default=None),
            'heap_comprometido_max_mb': max(en_ventana(vivo, desde, hasta, 'heap_comprometido_kb'), default=None),
            'rss_anon_max_mb': max(en_ventana(vivo, desde, hasta, 'rss_anon_kb'), default=None),
            'cont_actual_max_mb': max(en_ventana(vivo, desde, hasta, 'cont_actual_kb'), default=None),
            'cpu_backend_media': mediana([f['cpu'] for f in stats if desde <= f['ts'] <= hasta]),
            'cpu_pg_media': mediana([f['cpu'] for f in stats_pg if desde <= f['ts'] <= hasta]),
            'heap_usado_inicio_mb': mediana([f.get('heap_usado_kb') for f in base]),
        }
        for k in ('heap_usado_max_mb', 'heap_comprometido_max_mb', 'rss_anon_max_mb', 'cont_actual_max_mb', 'heap_usado_inicio_mb'):
            if antes[k] is not None:
                antes[k] = antes[k] / 1024
        fg = [(a, b) for a, b in zip(vivo, vivo[1:]) if desde <= b['ts'] <= hasta and a.get('gct_s') is not None and b.get('gct_s') is not None]
        if fg:
            t_gc = sum(b['gct_s'] - a['gct_s'] for a, b in fg)
            t_tot = sum(b['ts'] - a['ts'] for a, b in fg)
            antes['tiempo_en_gc_pct_30s_previos'] = t_gc / t_tot * 100 if t_tot else None
            antes['gc_completos_30s_previos'] = sum((b.get('fgc') or 0) - (a.get('fgc') or 0) for a, b in fg)
    return {
        'directorio': str(d), 'nivel': nivel, 't0': t0, 'duracion_s': fin - t0, 'xmx_mb': xmx, 'limite_mem_mb': limite_mem, 'peticiones': len(peticiones), 'fallos': len(fallos),
        'primer_fallo_s': rel(primer_fallo), 'usuarios_al_primer_fallo': vus_en(primer_fallo), 'primeros_fallos': primeros, 'rutas_en_30s_tras_el_primer_fallo': dict(sorted(rutas.items(), key=lambda kv: -kv[1])),
        'orden_de_eventos': orden, 'antes_del_colapso': antes, 'cubos': cub, 'primer_tramo_con_5pct_de_errores_s': primer_5['t'] if primer_5 else None, 'usuarios_al_5pct_de_errores': usuarios_5, 'vus': [(ts - t0, v) for ts, v in vus],
        'vivo': [(f['ts'] - t0, f) for f in vivo], 'stats': [(f['ts'] - t0, f) for f in stats], 'stats_pg': [(f['ts'] - t0, f) for f in stats_pg],
        'pg': [(f['ts'] - t0, f) for f in pg], 'hikari': [(h['ts'] - t0, h) for h in log['hikari']], 'oom_lineas': len(log['oom']), 'sin_conexion_lineas': len(sc),
        'oom_ejemplo': log['oom'][0][1] if log['oom'] else None,
    }


def graficar(a, ruta_png, titulo):
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt

    fig, ejes = plt.subplots(4, 1, figsize=(11, 10), sharex=True)
    t_cub = [c['t'] for c in a['cubos']]
    ax = ejes[0]
    ax.plot([t for t, _ in a['vus']], [v for _, v in a['vus']], color='#4c78a8', label='Usuarios activos')
    ax.set_ylabel('Usuarios')
    ax2 = ax.twinx()
    ax2.bar(t_cub, [c['errores_pct'] if c['errores_pct'] is not None else 0 for c in a['cubos']], width=CUBO_S * 0.9, color='#e45756', alpha=0.45, label='Errores (%)')
    ax2.set_ylabel('Errores (%)')
    ax2.set_ylim(0, 105)
    ax.set_title(titulo)
    ejes[1].plot([t for t, _ in a['vivo']], [f.get('heap_usado_kb', 0) / 1024 for _, f in a['vivo'] if f.get('heap_usado_kb') is not None], color='#54a24b', label='Heap usado')
    ejes[1].plot([t for t, f in a['vivo'] if f.get('heap_comprometido_kb') is not None], [f['heap_comprometido_kb'] / 1024 for _, f in a['vivo'] if f.get('heap_comprometido_kb') is not None], color='#b279a2', label='Heap comprometido')
    if a['xmx_mb']:
        ejes[1].axhline(a['xmx_mb'], color='k', linestyle='--', linewidth=0.8, label='-Xmx')
    ejes[1].set_ylabel('Heap (MB)')
    ejes[1].legend(loc='upper left', fontsize=8)
    pares = [(t, f) for t, f in a['vivo'] if f.get('fgc') is not None]
    xs, completos, gc_pct = [], [], []
    for (t0_, f0), (t1_, f1) in zip(pares, pares[1:]):
        xs.append(t1_)
        completos.append(f1['fgc'] - f0['fgc'])
        dt = t1_ - t0_
        gc_pct.append(((f1.get('gct_s') or 0) - (f0.get('gct_s') or 0)) / dt * 100 if dt else 0)
    ejes[2].bar(xs, completos, width=2.5, color='#f58518', label='GC completos por intervalo')
    ejes[2].set_ylabel('GC completos')
    e3 = ejes[2].twinx()
    e3.plot(xs, gc_pct, color='#333333', linewidth=0.9, label='Tiempo en GC (%)')
    e3.set_ylabel('Tiempo en GC (%)')
    e3.set_ylim(0, 105)
    ejes[3].plot([t for t, _ in a['stats']], [f['cpu'] for _, f in a['stats']], color='#4c78a8', label='CPU backend (% de su límite)')
    if a['stats_pg']:
        ejes[3].plot([t for t, _ in a['stats_pg']], [f['cpu'] for _, f in a['stats_pg']], color='#72b7b2', label='CPU PostgreSQL (% de 1 núcleo)')
    ejes[3].set_ylabel('CPU (%)')
    ejes[3].set_xlabel('Segundos desde el inicio de k6')
    e4 = ejes[3].twinx()
    e4.plot([t for t, _ in a['stats']], [f['mem_mb'] for _, f in a['stats']], color='#9d755d', linestyle=':', label='Memoria del contenedor (MB)')
    e4.set_ylabel('Memoria (MB)')
    h1, l1 = ejes[3].get_legend_handles_labels()
    h2, l2 = e4.get_legend_handles_labels()
    ejes[3].legend(h1 + h2, l1 + l2, loc='upper left', fontsize=8)
    colores = {'primer_fallo_de_k6': '#e45756', 'primer_OutOfMemoryError': '#000000', 'heap_usado_sobre_90pct_del_tope': '#54a24b', 'primera_rafaga_de_gc_completo': '#f58518', 'primer_tiempo_agotado_del_pool': '#b279a2'}
    for t, nombre in a['orden_de_eventos']:
        if nombre in colores:
            for ax_ in ejes:
                ax_.axvline(t, color=colores[nombre], linestyle='-.', linewidth=0.9, alpha=0.8)
            ejes[0].text(t, 1.0, nombre.replace('_', ' ')[:28], rotation=90, va='bottom', fontsize=6, color=colores[nombre], transform=ejes[0].get_xaxis_transform())
    fig.tight_layout()
    Path(ruta_png).parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(ruta_png, dpi=110)
    plt.close(fig)


def graficar_pool(a, ruta_png, titulo):
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt

    fig, ejes = plt.subplots(3, 1, figsize=(11, 7.5), sharex=True)
    pg = a['pg']
    ejes[0].plot([t for t, _ in pg], [f.get('activas') or 0 for _, f in pg], label='activas', color='#e45756')
    ejes[0].plot([t for t, _ in pg], [f.get('inactivas') or 0 for _, f in pg], label='inactivas', color='#54a24b')
    ejes[0].plot([t for t, _ in pg], [f.get('inactivas_en_transaccion') or 0 for _, f in pg], label='inactivas en transacción', color='#f58518')
    ejes[0].set_ylabel('Conexiones en PostgreSQL')
    ejes[0].legend(fontsize=8)
    ejes[0].set_title(titulo)
    hk = a['hikari']
    if hk:
        ejes[1].plot([t for t, _ in hk], [h['esperando'] for _, h in hk], '.', color='#b279a2', label='solicitudes esperando (Hikari)')
        ejes[1].plot([t for t, _ in hk], [h['activas'] for _, h in hk], '.', color='#e45756', label='activas (Hikari)')
        ejes[1].legend(fontsize=8)
    ejes[1].set_ylabel('Pool (registro de Hikari)')
    ejes[2].bar([c['t'] for c in a['cubos']], [c['errores_pct'] or 0 for c in a['cubos']], width=CUBO_S * 0.9, color='#e45756', alpha=0.5)
    ejes[2].set_ylabel('Errores (%)')
    ejes[2].set_xlabel('Segundos desde el inicio de k6')
    fig.tight_layout()
    fig.savefig(ruta_png, dpi=110)
    plt.close(fig)


def resumen_md(a, nombre):
    L = [f'### {nombre}', '']
    L.append(f"- Duración observada: {a['duracion_s']:.0f} s; {a['peticiones']} peticiones, {a['fallos']} con fallo.")
    if a['primer_fallo_s'] is None:
        L.append('- **No hubo ninguna petición fallida.**')
        return L + ['']
    L.append(f"- **Primer fallo a los {a['primer_fallo_s']:.0f} s, con {a['usuarios_al_primer_fallo']:.0f} usuarios activos.**")
    if a['primeros_fallos']:
        L += ['', '| t (s) | Operación | Estado | Duración (ms) | Error de k6 |', '|---|---|---|---|---|']
        L += [f"| {p['t_s']} | {p['ruta']} | {p['status']} | {p['dur_ms']} | {p['error'][:40]} |" for p in a['primeros_fallos']]
        L.append('')
    if a['rutas_en_30s_tras_el_primer_fallo']:
        L.append('- Operaciones con fallos en los 30 s siguientes al primero: ' + ', '.join(f'{k} ({v})' for k, v in list(a['rutas_en_30s_tras_el_primer_fallo'].items())[:8]) + '.')
    if a['orden_de_eventos']:
        L.append('- **Qué ocurre primero** (segundos desde el inicio de k6): ' + '; '.join(f'{t:.0f} s: {nombre_.replace("_", " ")}' for t, nombre_ in a['orden_de_eventos']) + '.')
    b = a['antes_del_colapso']
    if b:
        partes = []
        if b.get('heap_usado_inicio_mb') is not None:
            partes.append(f"heap usado al inicio {b['heap_usado_inicio_mb']:.0f} MB")
        if b.get('heap_usado_max_mb') is not None:
            partes.append(f"heap usado máx. {b['heap_usado_max_mb']:.0f} MB" + (f" de {a['xmx_mb']:.0f}" if a['xmx_mb'] else ''))
        if b.get('rss_anon_max_mb') is not None:
            partes.append(f"RssAnon máx. {b['rss_anon_max_mb']:.0f} MB")
        if b.get('cont_actual_max_mb') is not None:
            partes.append(f"contenedor {b['cont_actual_max_mb']:.0f} MB" + (f" de {a['limite_mem_mb']}" if a['limite_mem_mb'] else ''))
        if b.get('cpu_backend_media') is not None:
            partes.append(f"CPU del backend {b['cpu_backend_media']:.0f} %")
        if b.get('cpu_pg_media') is not None:
            partes.append(f"CPU de PostgreSQL {b['cpu_pg_media']:.0f} %")
        if b.get('tiempo_en_gc_pct_30s_previos') is not None:
            partes.append(f"tiempo en GC {b['tiempo_en_gc_pct_30s_previos']:.1f} % con {b.get('gc_completos_30s_previos', 0):.0f} GC completos")
        L.append('- **En los 30 s previos al primer fallo:** ' + ', '.join(partes) + '.')
    if a['oom_lineas']:
        L.append(f"- OutOfMemoryError en el registro: {a['oom_lineas']} líneas (ejemplo: `{a['oom_ejemplo']}`).")
    if a['sin_conexion_lineas']:
        L.append(f"- Esperas de conexión agotadas en el registro: {a['sin_conexion_lineas']}.")
    return L + ['']


def main():
    raiz = Path(sys.argv[1])
    salida = Path(sys.argv[2]) if len(sys.argv) > 2 else raiz / 'series'
    salida.mkdir(parents=True, exist_ok=True)
    import comparar_bateria as cb
    L = ['# Series temporales de la batería', '', 'Cada gráfica muestra, con el mismo eje de tiempo: usuarios y errores; heap usado y comprometido; recolecciones completas y tiempo en GC; y CPU y memoria. Las líneas verticales marcan el primer fallo y los demás eventos detectados.', '']
    resultados = []
    for d, perfil, rep, nivel in cb.descubrir(raiz):
        a = analizar(d, nivel)
        if a is None:
            continue
        nombre = f'{perfil}, {nivel} usuarios, repetición {rep}'
        png = salida / f'{perfil}_{nivel}_r{rep}.png'
        graficar(a, png, nombre)
        L += resumen_md(a, nombre) + [f'![{nombre}]({png.name})', '']
        liviano = {k: v for k, v in a.items() if k not in ('cubos', 'vus', 'vivo', 'stats', 'stats_pg', 'pg', 'hikari')}
        liviano.update({'perfil': perfil, 'repeticion': rep})
        resultados.append(liviano)
    import comparar_investigacion as ci
    for d, tipo, rep, etiqueta, nivel in ci.descubrir(raiz):
        a = analizar(d, nivel)
        if a is None:
            continue
        nombre = f'{tipo}: {etiqueta}, {nivel} usuarios, repetición {rep}'
        png = salida / f'inv-{tipo}-r{rep}-{etiqueta}_{nivel}.png'
        graficar(a, png, nombre)
        L += resumen_md(a, nombre) + [f'![{nombre}]({png.name})', '']
        if tipo == 'pool' and a['pg']:
            png_pool = salida / f'inv-{tipo}-r{rep}-{etiqueta}_{nivel}_pool.png'
            graficar_pool(a, png_pool, nombre + ': conexiones')
            L += [f'![{nombre}: conexiones]({png_pool.name})', '']
        liviano = {k: v for k, v in a.items() if k not in ('cubos', 'vus', 'vivo', 'stats', 'stats_pg', 'pg', 'hikari')}
        liviano.update({'perfil': etiqueta, 'tipo': tipo, 'repeticion': rep})
        resultados.append(liviano)
    (salida / 'SERIES_TEMPORALES.md').write_text('\n'.join(L) + '\n', encoding='utf-8')
    (salida / 'series_temporales.json').write_text(json.dumps(resultados, indent=1, ensure_ascii=False, default=str), encoding='utf-8')
    sys.stdout.write('\n'.join(L) + '\n')
    return 0


if __name__ == '__main__':
    sys.exit(main())

import csv
import gzip
import json
import os
import re
import statistics
import sys
from pathlib import Path

UMBRAL_DEFECTO = 80.0
VENTANA = 3
RE_PAUSA = re.compile(r'\[(\d+(?:\.\d+)?)s\].*?Pause (\w+).*?(\d+)([KMG])->(\d+)([KMG])\((\d+)([KMG])\)\s+([\d.]+)ms')
A_MB = {'K': 1 / 1024, 'M': 1, 'G': 1024}


def percentil(valores, p):
    if not valores:
        return None
    ordenados = sorted(valores)
    return ordenados[min(len(ordenados) - 1, int(len(ordenados) * p))]


def leer_json(ruta):
    try:
        return json.loads(Path(ruta).read_text(encoding='utf-8'))
    except (OSError, ValueError):
        return None


def numero(texto):
    try:
        return float(texto)
    except (TypeError, ValueError):
        return None


def leer_csv(ruta):
    try:
        with open(ruta, encoding='utf-8', newline='') as f:
            contenido = f.read()
    except OSError:
        return None
    if contenido.startswith('sin_proceso'):
        return []
    return list(csv.DictReader(contenido.splitlines()))


def cpus_de_lista(texto):
    cpus = set()
    for trozo in (texto or '').replace(' ', '').split(','):
        if not trozo:
            continue
        if '-' in trozo:
            a, b = trozo.split('-', 1)
            cpus.update(range(int(a), int(b) + 1))
        else:
            cpus.add(int(trozo))
    return cpus


def leer_afinidad(ruta):
    campos = {}
    try:
        for linea in Path(ruta).read_text(encoding='utf-8').splitlines():
            if '=' in linea:
                k, v = linea.split('=', 1)
                campos[k] = v
    except OSError:
        pass
    return campos


def fmt(valor, decimales=0, sufijo=''):
    if valor is None:
        return 'sin datos'
    return f'{valor:,.{decimales}f}{sufijo}'.replace(',', ' ')


def ventana_saturada(serie, umbral, ventana=VENTANA):
    if len(serie) < ventana:
        return bool(serie) and statistics.mean(serie) >= umbral
    return any(statistics.mean(serie[i:i + ventana]) >= umbral for i in range(len(serie) - ventana + 1))


def evaluar_generador(niv, etiqueta, umbral=UMBRAL_DEFECTO):
    niv = Path(niv)
    estado = leer_json(niv / f'estado_{etiqueta}.json') or {}
    k6 = leer_json(niv / f'k6_{etiqueta}.json') or {}
    filas = leer_csv(niv / f'generador_{etiqueta}.csv')
    afinidad = leer_afinidad(niv / f'generador_{etiqueta}.csv.afinidad')
    nucleos = len(cpus_de_lista(afinidad.get('Cpus_allowed_list', ''))) or 1
    r = {'etiqueta': etiqueta, 'nivel': estado.get('nivel'), 'umbral_pct': umbral, 'nucleos_k6': nucleos,
         'cpus_permitidas_k6': afinidad.get('Cpus_allowed_list') or None, 'motivos': []}
    cpu = [numero(f.get('cpu_k6_pct')) for f in (filas or [])]
    cpu = [c / nucleos for c in cpu if c is not None]
    rss = [numero(f.get('rss_kb')) for f in (filas or [])]
    rss = [x / 1024 for x in rss if x is not None]
    hwm = [numero(f.get('hwm_kb')) for f in (filas or [])]
    hwm = [x / 1024 for x in hwm if x is not None]
    r['muestras'] = len(cpu)
    r['cpu_media_pct'] = statistics.mean(cpu) if cpu else None
    r['cpu_p95_pct'] = percentil(cpu, 0.95)
    r['cpu_max_pct'] = max(cpu) if cpu else None
    r['muestras_sobre_umbral_pct'] = sum(1 for c in cpu if c >= umbral) / len(cpu) * 100 if cpu else None
    r['rss_pico_mb'] = max(max(rss, default=0), max(hwm, default=0)) or None
    por_cpu = {}
    for f in filas or []:
        for par in (f.get('cpus_logicas') or '').split('|'):
            if ':' in par:
                k, v = par.split(':', 1)
                x = numero(v)
                if x is not None and k.isdigit():
                    por_cpu.setdefault(int(k), []).append(x)
    r['cpu_logica_media_pct'] = {k: statistics.mean(v) for k, v in sorted(por_cpu.items())}
    peticiones = k6.get('peticiones') or 0
    estados = k6.get('estados') or {}
    duracion = k6.get('duracion_total_s') or 0
    r['peticiones'] = peticiones
    r['iteraciones'] = k6.get('iteraciones_completas')
    r['peticiones_por_s'] = peticiones / duracion if duracion else None
    r['vus_activos_max'] = k6.get('vus_activos_max')
    r['vus_asignados_max'] = k6.get('vus_maximos')
    r['iteraciones_descartadas'] = k6.get('iteraciones_descartadas')
    r['no_2xx_pct'] = (peticiones - (estados.get('2xx') or 0)) / peticiones * 100 if peticiones else None
    r['sin_respuesta'] = estados.get('sin_respuesta')
    r['k6_codigo_salida'] = estado.get('k6_codigo_salida')
    if not k6:
        r['motivos'].append('no hay resumen de k6 (k6_*.json): no se puede verificar la carga generada')
    if filas is None or not cpu:
        r['motivos'].append('no hay muestras de CPU del generador: no se pudo evaluar su saturación')
        r['saturado'] = None
    else:
        r['saturado'] = ventana_saturada(cpu, umbral)
        if r['saturado']:
            r['motivos'].append(f'CPU de k6 por encima de {umbral:g} % de su capacidad en {VENTANA} muestras seguidas (máximo {r["cpu_max_pct"]:.0f} %)')
    previstos = r['nivel']
    activos = r['vus_activos_max']
    r['alcanzo_vus'] = None if previstos is None or activos is None else activos >= previstos
    if r['alcanzo_vus'] is False:
        r['motivos'].append(f'k6 alcanzó {activos:g} usuarios activos de {previstos} previstos')
    if r['k6_codigo_salida'] not in (None, 0):
        r['motivos'].append(f'k6 terminó con código {r["k6_codigo_salida"]}')
    if r['iteraciones_descartadas']:
        r['motivos'].append(f'k6 descartó {r["iteraciones_descartadas"]:g} iteraciones')
    if not r['motivos']:
        r['interpretable'] = 'sí'
    elif r['saturado'] or r['alcanzo_vus'] is False or r['k6_codigo_salida'] not in (None, 0) or r['iteraciones_descartadas']:
        r['interpretable'] = 'potencialmente no'
    else:
        r['interpretable'] = 'no evaluado'
    return r


def leer_pausas_gc(ruta):
    pausas = []
    try:
        with gzip.open(ruta, 'rt', encoding='utf-8', errors='replace') as f:
            for linea in f:
                m = RE_PAUSA.search(linea)
                if m:
                    pausas.append({'tipo': m.group(2), 'ms': float(m.group(9)), 'antes_mb': float(m.group(3)) * A_MB[m.group(4)],
                                   'despues_mb': float(m.group(5)) * A_MB[m.group(6)], 'comprometido_mb': float(m.group(7)) * A_MB[m.group(8)]})
    except OSError:
        return None
    return pausas


def evaluar_jvm(niv, etiqueta):
    niv = Path(niv)
    filas = leer_csv(niv / f'jvm_vivo_{etiqueta}.csv')
    r = {'etiqueta': etiqueta, 'muestras': len(filas or []), 'heap_en_vivo': False}
    if filas:
        def col(nombre, escala=1.0):
            vals = [numero(f.get(nombre)) for f in filas]
            return [v / escala for v in vals if v is not None]
        usado, comp = col('heap_usado_kb', 1024), col('heap_comprometido_kb', 1024)
        r['heap_en_vivo'] = bool(usado)
        r['heap_usado_max_mb'] = max(usado) if usado else None
        r['heap_usado_media_mb'] = statistics.mean(usado) if usado else None
        r['heap_comprometido_max_mb'] = max(comp) if comp else None
        r['metaspace_usado_max_mb'] = max(col('metaspace_usado_kb', 1024), default=None)
        r['cont_actual_max_mb'] = max(col('cont_actual_kb', 1024), default=None)
        r['cont_pico_cgroup_mb'] = max(col('cont_pico_kb', 1024), default=None)
        r['cont_anon_max_mb'] = max(col('cont_anon_kb', 1024), default=None)
        r['cont_file_max_mb'] = max(col('cont_file_kb', 1024), default=None)
        r['rss_max_mb'] = max(col('rss_kb', 1024), default=None)
        r['rss_anon_max_mb'] = max(col('rss_anon_kb', 1024), default=None)
        r['hilos_so_max'] = max(col('hilos_so'), default=None)
        r['hilos_java_max'] = max(col('hilos_java'), default=None)
        r['kstack_max_mb'] = max(col('kstack_kb', 1024), default=None)
        r['slab_max_mb'] = max(col('slab_kb', 1024), default=None)
        no_heap = []
        for f in filas:
            anon, comp = numero(f.get('rss_anon_kb')), numero(f.get('heap_comprometido_kb'))
            if anon is not None and comp is not None:
                no_heap.append((anon - comp) / 1024)
        r['fuera_del_heap_max_mb'] = max(no_heap, default=None)
        ygc, fgc, gct = col('ygc'), col('fgc'), col('gct_s')
        r['jstat_ygc'] = (ygc[-1] - ygc[0]) if len(ygc) > 1 else None
        r['jstat_total'] = (ygc[-1] if ygc else None, fgc[-1] if fgc else None, gct[-1] if gct else None)
        r['jstat_fgc'] = (fgc[-1] - fgc[0]) if len(fgc) > 1 else None
        r['jstat_gct_s'] = (gct[-1] - gct[0]) if len(gct) > 1 else None
        dur = col('duracion_ms')
        r['muestreo_ms_media'] = statistics.mean(dur) if dur else None
        r['muestreo_ms_max'] = max(dur) if dur else None
        r['sin_jstat'] = sum(1 for f in filas if f.get('fuente') != 'hsperfdata')
    pausas = leer_pausas_gc(niv / f'jvm_gc_{etiqueta}.log.gz')
    r['gc_registro'] = None
    if pausas is not None:
        r['gc_registro'] = {
            'pausas': len(pausas), 'total_ms': sum(p['ms'] for p in pausas), 'max_ms': max((p['ms'] for p in pausas), default=None),
            'completas': sum(1 for p in pausas if p['tipo'] == 'Full'), 'comprometido_max_mb': max((p['comprometido_mb'] for p in pausas), default=None),
            'usado_antes_max_mb': max((p['antes_mb'] for p in pausas), default=None), 'usado_despues_max_mb': max((p['despues_mb'] for p in pausas), default=None),
        }
    sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'capacidad'))
    import informe_nivel as n
    _, mems = n.leer_estadisticas(niv / f'stats_{etiqueta}.csv')
    r['docker_stats_pico_mb'] = max(mems) if mems else None
    return r


def etiquetas(niv):
    return sorted(p.name[len('estado_'):-len('.json')] for p in Path(niv).glob('estado_*.json'))


def orden(etiqueta, niv):
    est = leer_json(Path(niv) / f'estado_{etiqueta}.json') or {}
    return (est.get('nivel') or 0, etiqueta)


def tabla_generador(evals, umbral):
    filas = ['| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |',
             '|---|---|---|---|---|---|---|---|---|---|---|---|']
    for e in evals:
        descartadas = 'no aplica (ramping-vus)' if e['iteraciones_descartadas'] is None else fmt(e['iteraciones_descartadas'])
        filas.append(
            f"| {e['etiqueta']} | {fmt(e['vus_activos_max'])} / {fmt(e['nivel'])} | {fmt(e['peticiones'])} | {fmt(e['peticiones_por_s'], 1)} | {descartadas} | "
            f"{fmt(e['no_2xx_pct'], 1, ' %')} | {fmt(e['sin_respuesta'])} | {fmt(e['cpu_media_pct'], 0)} / {fmt(e['cpu_p95_pct'], 0)} / {fmt(e['cpu_max_pct'], 0)} | "
            f"{fmt(e['muestras_sobre_umbral_pct'], 0, ' %')} | {fmt(e['rss_pico_mb'])} | {fmt(e['k6_codigo_salida'])} | **{e['interpretable']}** |")
    return filas


def tabla_jvm(evals):
    filas = ['| Etiqueta | Heap usado máx / media (MB) | Heap comprometido máx (MB) | Metaspace usado máx (MB) | Contenedor: memory.current máx (MB) | Contenedor: pico cgroup (MB) | Contenedor: docker stats pico (MB) | GC en vivo, durante el muestreo: young / full / tiempo (s) (acumulado de la JVM) | GC registro: pausas / total ms / máx ms / full |',
             '|---|---|---|---|---|---|---|---|---|']
    for e in evals:
        g = e.get('gc_registro')
        gc = f"{g['pausas']} / {fmt(g['total_ms'])} / {fmt(g['max_ms'], 1)} / {g['completas']}" if g else 'sin registro'
        if e['heap_en_vivo']:
            vivo = (f"{fmt(e['heap_usado_max_mb'])} / {fmt(e['heap_usado_media_mb'])}", fmt(e['heap_comprometido_max_mb']), fmt(e['metaspace_usado_max_mb']),
                    f"{fmt(e['jstat_ygc'])} / {fmt(e['jstat_fgc'])} / {fmt(e['jstat_gct_s'], 2)} ({fmt(e['jstat_total'][0])} / {fmt(e['jstat_total'][1])} / {fmt(e['jstat_total'][2], 1)})")
        else:
            vivo = ('no medido', 'no medido', 'no medido', 'no medido')
        filas.append(f"| {e['etiqueta']} | {vivo[0]} | {vivo[1]} | {vivo[2]} | {fmt(e.get('cont_actual_max_mb'))} | {fmt(e.get('cont_pico_cgroup_mb'))} | {fmt(e['docker_stats_pico_mb'])} | {vivo[3]} | {gc} |")
    return filas


def lineas_entorno(raiz):
    salida = []
    try:
        for linea in (Path(raiz) / 'entorno.txt').read_text(encoding='utf-8').splitlines():
            if linea.startswith(('perfil del laboratorio', 'citas de la clínica grande', 'pool de conexiones', 'modo de afinidad', 'mezcla de operaciones', 'caída del backend', 'imagen de ejecución', 'imágenes', '  imagen:', '  bases del', '  Dockerfile', '  k6 fijado', '  openjdk', '  java', '  jcmd', '  jstat', '  jfr', '  jps', '  el heap en vivo', 'k6:', 'docker:', 'runner:', 'kernel:', 'fijado de CPU', 'núcleos de k6', 'umbral del generador', 'heap en vivo')):
                salida.append(f'- {linea.strip()}')
    except OSError:
        pass
    return salida


def generar(raiz, umbral=UMBRAL_DEFECTO):
    raiz = Path(raiz)
    niv = raiz / 'niveles'
    ets = sorted(etiquetas(niv), key=lambda e: orden(e, niv))
    gens = [evaluar_generador(niv, e, umbral) for e in ets]
    jvms = [evaluar_jvm(niv, e) for e in ets]
    for g in gens:
        (niv / f"generador_{g['etiqueta']}.json").write_text(json.dumps(g, indent=1, ensure_ascii=False), encoding='utf-8')
    for j in jvms:
        (niv / f"jvm_resumen_{j['etiqueta']}.json").write_text(json.dumps(j, indent=1, ensure_ascii=False), encoding='utf-8')
    lineas = ['# Salud del laboratorio: generador de carga, topología y memoria de la JVM', '']
    lineas += lineas_entorno(raiz) + ['']
    topologia = raiz / 'topologia.md'
    if topologia.exists():
        lineas += [topologia.read_text(encoding='utf-8'), '']
    lineas += ['## Generador de carga (k6)', '',
               f'**Criterio experimental, no un estándar:** el generador se marca como saturado si su CPU, medida como porcentaje de los núcleos a los que está fijado, supera {umbral:g} % en {VENTANA} muestras consecutivas (≈ 6 s). El umbral se cambia con `UMBRAL_GENERADOR_CPU_PCT`. Un nivel marcado «potencialmente no» no debe interpretarse como capacidad del backend sin repetirlo con un generador con más recursos.', '']
    lineas += tabla_generador(gens, umbral) + ['']
    con_cpus = [g for g in gens if g['cpu_logica_media_pct']]
    if con_cpus:
        lineas += ['Utilización media por CPU lógica del runner durante cada nivel (todas las CPU, estén o no fijadas; sirve para ver si una CPU hermana SMT de una fijada estaba ocupada):', '']
        for g in con_cpus:
            lineas.append(f"- `{g['etiqueta']}`: " + ', '.join(f'CPU {k}: {v:.0f} %' for k, v in g['cpu_logica_media_pct'].items()))
        lineas.append('')
    marcados = [g for g in gens if g['interpretable'] != 'sí']
    if marcados:
        lineas += ['**Niveles que requieren cautela:**', '']
        for g in marcados:
            lineas.append(f"- `{g['etiqueta']}` ({g['interpretable']}): " + '; '.join(g['motivos']))
        lineas.append('')
    else:
        lineas += ['Ningún nivel superó el criterio: no se detectó saturación del generador ni carga inferior a la prevista (esto no prueba que el generador no influya, solo que no alcanzó el umbral).', '']
    lineas += ['> `dropped_iterations` solo lo emiten los ejecutores de tasa de llegada; este escenario usa `ramping-vus`, por lo que no aplica. La carga efectivamente generada se verifica con los usuarios activos máximos (`vus`), las peticiones totales y por segundo, y los errores.', '']
    lineas += ['## Memoria de la JVM y del contenedor', '',
               '- **Heap usado / comprometido** (contadores de la JVM, los mismos que lee `jstat -gc`, en vivo cada pocos segundos): memoria de objetos dentro del heap de la JVM, y la reservada por la JVM para ese heap.',
               '- **Contenedor** (`memory.current` y pico del cgroup, `docker stats`): memoria total que ve el límite de Docker (heap + Metaspace + código + hilos + memoria directa + caché de archivos).',
               '- **GC en vivo**: contadores leídos mientras corre el nivel; la primera cifra es lo ocurrido desde que empezó el muestreo y entre paréntesis el acumulado de la JVM. **GC registro**: pausas de toda la vida de la JVM leídas del archivo `-Xlog:gc` (incluye el arranque), por eso no coinciden con la ventana del muestreo.',
               '']
    lineas += tabla_jvm(jvms) + ['']
    medidos = [j for j in jvms if j.get('muestreo_ms_media') is not None]
    if medidos:
        lineas.append(f"Costo del muestreo en vivo: cada muestra tardó {statistics.mean(j['muestreo_ms_media'] for j in medidos):.0f} ms de reloj de pared de media (máximo {max(j['muestreo_ms_max'] for j in medidos):.0f} ms). Es tiempo de pared, no CPU: el cálculo de la interferencia está en el flujo de calibración.")
    sin = [j['etiqueta'] for j in jvms if not j['heap_en_vivo']]
    if sin:
        lineas.append(f"Sin heap en vivo en: {', '.join(sin)} (no se leyeron los contadores de la JVM (hsperfdata) o el muestreo estaba apagado).")
    texto = '\n'.join(lineas) + '\n'
    (raiz / 'SALUD_LABORATORIO.md').write_text(texto, encoding='utf-8')
    return texto


def lineas_generador_nivel(raiz, etiquetas_nivel, umbral=UMBRAL_DEFECTO):
    niv = Path(raiz) / 'niveles'
    evals = [evaluar_generador(niv, e, umbral) for e in etiquetas_nivel if (niv / f'estado_{e}.json').exists()]
    if not evals:
        return []
    salida = ['## Salud del generador de carga (criterio experimental)', '']
    salida += tabla_generador(evals, umbral) + ['']
    for g in evals:
        if g['interpretable'] == 'potencialmente no':
            salida.append(f"> **Resultado potencialmente no interpretable en `{g['etiqueta']}`:** " + '; '.join(g['motivos']))
        elif g['interpretable'] == 'no evaluado':
            salida.append(f"> Generador no evaluado en `{g['etiqueta']}` (no se puede afirmar que sea interpretable): " + '; '.join(g['motivos']))
    return salida + ['']


def main():
    raiz = sys.argv[1]
    umbral = float(os.environ.get('UMBRAL_GENERADOR_CPU_PCT') or UMBRAL_DEFECTO)
    sys.stdout.write(generar(raiz, umbral))
    return 0


if __name__ == '__main__':
    sys.exit(main())

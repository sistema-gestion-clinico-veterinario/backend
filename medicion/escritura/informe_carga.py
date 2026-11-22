import gzip
import json
import re
import statistics
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'capacidad'))
import informe_nivel as n  # noqa: E402

RAIZ = NIV = LOGS = None
NIVEL = 0

TOPES_METASPACE_MB = {'D': 256, 'E': 128, 'F': 256, 'A': 128, 'B': 128, 'C': 128}

ORDEN_PASOS = ['apoderado', 'mascota', 'cita', 'checkin', 'iniciar', 'consulta', 'diagnostico', 'receta', 'actualizar_mascota',
               'cerrar_consulta', 'finalizar_servicio', 'punto_cobro', 'vincular_equipo', 'abrir_caja', 'pago', 'cancelar_cita']
ETIQUETAS = {
    'apoderado': 'Registrar propietario', 'mascota': 'Registrar mascota', 'cita': 'Agendar cita', 'checkin': 'Recepción (llegada)',
    'iniciar': 'Iniciar atención', 'consulta': 'Leer la consulta', 'diagnostico': 'Registrar diagnóstico', 'receta': 'Registrar receta',
    'actualizar_mascota': 'Actualizar mascota', 'cerrar_consulta': 'Cerrar la consulta', 'finalizar_servicio': 'Finalizar servicio',
    'punto_cobro': 'Caja: crear punto de cobro', 'vincular_equipo': 'Caja: vincular equipo', 'abrir_caja': 'Caja: abrir la caja',
    'pago': 'Cobrar', 'cancelar_cita': 'Cancelar cita',
}

RE_METASPACE = re.compile(r'\[(\d+(?:\.\d+)?)s\].*?Metaspace: (\d+)K\((\d+)K\)->(\d+)K\((\d+)K\)')
RE_PAUSA = re.compile(r'\[(\d+(?:\.\d+)?)s\].*?(Pause \w+).*?(\d+)([KMG])->(\d+)([KMG])\((\d+)([KMG])\)\s+([\d.]+)ms')
RE_CLASE = re.compile(r'\[(\d+(?:\.\d+)?)s\](?:\[[^\]]*\])*\s+(\S+) source: ')
A_MB = {'K': 1 / 1024, 'M': 1, 'G': 1024}


def usuarios_sin_degradacion(tramos):
    sano = None
    for t in tramos:
        if t['peticiones'] < 3 or t['usuarios'] is None:
            continue
        if t['errores_pct'] >= 1 or (t['p95_ms'] or 0) >= 2000:
            break
        sano = max(sano or 0, t['usuarios'])
    return sano


n.usuarios_sanos = usuarios_sin_degradacion


def configurar(raiz, nivel):
    global RAIZ, NIV, LOGS, NIVEL
    RAIZ = Path(raiz)
    NIV = RAIZ / 'niveles'
    LOGS = RAIZ / 'logs'
    NIVEL = int(nivel)


def leer_lineas_gz(ruta):
    try:
        with gzip.open(ruta, 'rt', encoding='utf-8', errors='replace') as f:
            return f.read().splitlines()
    except OSError:
        return None


def grupo_de_clase(nombre):
    return re.sub(r'\d+', '#', re.sub(r'0x[0-9a-fA-F]+', '', nombre))


def paquete_de_clase(nombre):
    return '.'.join(nombre.split('$')[0].split('.')[:4])


def analizar_jvm(et, config):
    gc = leer_lineas_gz(NIV / f'jvm_gc_{et}.log.gz')
    clases = leer_lineas_gz(NIV / f'jvm_clases_{et}.log.gz')
    r = {
        'hay_gc': gc is not None, 'hay_clases': clases is not None, 'tope_mb': TOPES_METASPACE_MB.get(config),
        'metaspace': [], 'pausa_total_s': 0.0, 'pausas': 0, 'completos': 0, 'por_metadatos': 0, 'heap_tras_gc_max_mb': None,
        'clases': [], 'duracion_jvm_s': 0.0, 'completas_s': 0.0, 'completas_por_heap': 0, 'heap_inicial_mb': None,
    }
    capacidades = []
    heap = []
    for linea in gc or []:
        m = RE_METASPACE.search(linea)
        if m:
            r['metaspace'].append((float(m.group(1)), int(m.group(4)) / 1024, int(m.group(5)) / 1024))
            continue
        p = RE_PAUSA.search(linea)
        if p:
            r['pausas'] += 1
            r['pausa_total_s'] += float(p.group(9)) / 1000
            capacidades.append(int(p.group(7)) * A_MB[p.group(8)])
            if p.group(2) == 'Pause Full':
                r['completos'] += 1
                r['completas_s'] += float(p.group(9)) / 1000
                if 'Allocation Failure' in linea:
                    r['completas_por_heap'] += 1
            if 'Metadata GC' in linea:
                r['por_metadatos'] += 1
            heap.append(int(p.group(5)) * A_MB[p.group(6)])
            r['duracion_jvm_s'] = max(r['duracion_jvm_s'], float(p.group(1)))
    r['heap_tras_gc_max_mb'] = max(heap) if heap else None
    r['heap_inicial_mb'] = capacidades[0] if capacidades else None
    for linea in clases or []:
        m = RE_CLASE.search(linea)
        if m:
            r['clases'].append((float(m.group(1)), m.group(2)))
    if r['clases']:
        r['duracion_jvm_s'] = max(r['duracion_jvm_s'], r['clases'][-1][0])
    if r['metaspace']:
        r['duracion_jvm_s'] = max(r['duracion_jvm_s'], r['metaspace'][-1][0])
    return r


def tendencia_metaspace(serie):
    if len(serie) < 6:
        return None
    t_fin = serie[-1][0]
    mitad = [p for p in serie if p[0] >= t_fin / 2]
    if len(mitad) < 3:
        return None
    ini, fin = mitad[0][1], mitad[-1][1]
    minutos = (mitad[-1][0] - mitad[0][0]) / 60
    return {'inicio_mb': ini, 'fin_mb': fin, 'pct': (fin - ini) / ini * 100 if ini else 0.0, 'mb_min': (fin - ini) / minutos if minutos else None}


def notas_jvm(d, jvm):
    notas = []
    log = d['log']
    if not jvm['hay_gc']:
        return ['No se pudo copiar el registro de GC de la JVM: no hay serie de Metaspace.']
    tope = jvm['tope_mb']
    serie = jvm['metaspace']
    if serie:
        pico = max(p[1] for p in serie)
        notas.append(f"Metaspace usado: pico de {pico:.0f} MB tras un GC" + (f" (tope configurado: {tope} MB)." if tope else '.'))
        tend = tendencia_metaspace(serie)
        if log['oom_metaspace']:
            notas.append(f"**Se agotó el Metaspace: {log['oom_metaspace']} errores `OutOfMemoryError: Metaspace`.**")
        elif tend is None:
            notas.append('Pocos GC registrados: no alcanza para saber si el Metaspace se estabiliza.')
        elif tend['pct'] < 3:
            notas.append(f"**Metaspace estable**: en la segunda mitad pasó de {tend['inicio_mb']:.0f} a {tend['fin_mb']:.0f} MB ({tend['pct']:+.1f} %).")
        else:
            ritmo = f", unos {tend['mb_min']:+.1f} MB por minuto" if tend['mb_min'] is not None else ''
            notas.append(f"**Metaspace sigue creciendo** en la segunda mitad: de {tend['inicio_mb']:.0f} a {tend['fin_mb']:.0f} MB ({tend['pct']:+.1f} %{ritmo}).")
    else:
        notas.append('El registro de GC no trae líneas de Metaspace (¿la JVM no hizo ningún GC?).')
    if jvm['pausas']:
        dur = jvm['duracion_jvm_s'] or 1
        notas.append(
            f"GC: {jvm['pausas']} pausas, {jvm['pausa_total_s']:.1f} s en total ({jvm['pausa_total_s'] / dur * 100:.0f} % del tiempo de vida de la JVM); "
            f"{jvm['completos']} completas, {jvm['por_metadatos']} provocadas por el umbral de metadatos de clases."
        )
    if jvm['heap_tras_gc_max_mb'] is not None:
        notas.append(f"Heap tras GC: máximo de {jvm['heap_tras_gc_max_mb']:.0f} MB (tope -Xmx{d['heap_max']}).")
    if jvm['clases']:
        t_fin = jvm['clases'][-1][0]
        tardias = [c for c in jvm['clases'] if c[0] >= t_fin / 2]
        notas.append(f"Clases cargadas: {len(jvm['clases'])} en total, {len(tardias)} en la segunda mitad de la vida de la JVM.")
    return notas


def tabla_jvm(jvm):
    serie = jvm['metaspace']
    if len(serie) < 2:
        return []
    filas = ['| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |', '|---|---|---|---|']
    paso = max(1, len(serie) // 14)
    tiempos_clases = [c[0] for c in jvm['clases']]
    for t, usado, reservado in serie[::paso]:
        cargadas = sum(1 for x in tiempos_clases if x <= t)
        filas.append(f'| {t / 60:.1f} | {usado:.0f} | {reservado:.0f} | {cargadas if tiempos_clases else "-"} |')
    return filas


def tabla_clases_tardias(jvm):
    if not jvm['clases']:
        return []
    t_fin = jvm['clases'][-1][0]
    tardias = [c[1] for c in jvm['clases'] if c[0] >= t_fin / 2]
    if not tardias:
        return []
    salida = []
    generados = [(g, c) for g, c in Counter(grupo_de_clase(x) for x in tardias).most_common(40) if c >= 3][:10]
    if generados:
        salida += ['**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**', '',
                   '| Patrón de clase | Veces |', '|---|---|']
        salida += [f'| `{g}` | {c} |' for g, c in generados]
        salida.append('')
    paquetes = Counter(paquete_de_clase(x) for x in tardias).most_common(8)
    salida += ['**Paquetes con más clases cargadas en la segunda mitad:**', '', '| Paquete | Clases |', '|---|---|']
    salida += [f'| `{p}` | {c} |' for p, c in paquetes]
    return salida


def analizar(version, config=None):
    et = f'{version}_{NIVEL}'
    estado = n.leer_json(NIV / f'estado_{et}.json')
    config = config or (estado or {}).get('config_jvm') or 'D'
    limite_cpu = (estado or {}).get('cpus_limite') or 1
    n.LIMITE_MEM_MB = (estado or {}).get('memoria_limite_mb') or 2048
    cpus, mems = n.leer_estadisticas(NIV / f'stats_{et}.csv')
    cpus = [c / limite_cpu for c in cpus]
    cpus_pg, _ = n.leer_estadisticas(NIV / f'stats_pg_{et}.csv')
    tramos, endpoints = n.leer_k6_csv(NIV / f'k6_{et}.csv.gz')
    return {
        'version': version,
        'config': config,
        'limite_cpu': limite_cpu,
        'limite_mem_mb': n.LIMITE_MEM_MB,
        'heap_max': '1g' if config == 'E' else ((estado or {}).get('heap_max') or '1g'),
        'k6': n.leer_json(NIV / f'k6_{et}.json'),
        'estado': estado,
        'cpu': n.resumen_cpu(cpus),
        'cpu_pg': n.resumen_cpu(cpus_pg),
        'mem_pico_mb': max(mems) if mems else None,
        'log': n.contar_en_log(LOGS / f'backend_carga_{et}.log', {
            'sin_memoria_java': r'OutOfMemoryError: Java heap',
            'oom_metaspace': r'OutOfMemoryError: Metaspace',
            'oom_total': r'OutOfMemoryError',
            'pool_conexiones': r'Connection is not available|HikariPool.*(timeout|Timeout)',
            'hilos_agotados': r'RejectedExecutionException|Thread pool',
            'correo_fallido': r'Error al enviar correo',
        }),
        'tramos': tramos,
        'endpoints': endpoints,
        'db_antes': n.leer_json(NIV / f'db_antes_{et}.json'),
        'db_despues': n.leer_json(NIV / f'db_despues_{et}.json'),
        'integridad': n.leer_json(NIV / f'integridad_{et}.json'),
        'jvm': analizar_jvm(et, config),
        'sql_top': n.leer_json(NIV / f'sql_top_{et}.json'),
        'sql_consent': n.leer_json(NIV / f'sql_consentimiento_{et}.json'),
        'consent_antes': n.leer_json(NIV / f'consentimiento_antes_{et}.json'),
        'consent_despues': n.leer_json(NIV / f'consentimiento_despues_{et}.json'),
        'et': et,
        'sonda': n.leer_json(NIV / f'sonda_panel_{et}.json'),
        'ruta_respuesta': NIV / f'panel_respuesta_{et}.json',
    }


def correctos(d, paso):
    return ((d['k6'] or {}).get('pasos', {}).get(paso, {}) or {}).get('correctos', 0)


def fallidos(d, paso):
    return ((d['k6'] or {}).get('pasos', {}).get(paso, {}) or {}).get('fallidos', 0)


def conciliacion(d):
    a, b = d['db_antes'], d['db_despues']
    if not a or not b or not d['k6']:
        return []
    pares = [
        ('Propietarios', 'apoderados', correctos(d, 'apoderado')),
        ('Mascotas', 'mascotas', correctos(d, 'mascota')),
        ('Citas agendadas', 'citas', correctos(d, 'cita')),
        ('Diagnósticos', 'diagnosticos', correctos(d, 'diagnostico')),
        ('Recetas', 'recetas', correctos(d, 'receta')),
        ('Pagos', 'pagos', correctos(d, 'pago')),
        ('Citas completadas', 'citas_completadas', correctos(d, 'cerrar_consulta') + correctos(d, 'finalizar_servicio')),
        ('Citas canceladas', 'citas_canceladas', correctos(d, 'cancelar_cita')),
    ]
    filas = []
    for nombre, clave, esperado in pares:
        real = b.get(clave, 0) - a.get(clave, 0)
        if real == esperado:
            estado = 'coincide'
        elif real > esperado:
            estado = 'la base tiene más (ver nota)'
        else:
            estado = 'FALTAN REGISTROS'
        filas.append((nombre, esperado, real, estado))
    return filas


def estado_datos(d):
    filas = conciliacion(d)
    if not filas:
        return 'SIN DATOS'
    faltan = any(f[3] == 'FALTAN REGISTROS' for f in filas)
    integ = d['integridad'] or {}
    roto = any((integ.get(k) or 0) != 0 for k in ('mascotas_sin_apoderado', 'citas_sin_mascota', 'consultas_sin_cita', 'documentos_repetidos'))
    if faltan or roto:
        return 'INCONSISTENTES'
    return 'CONSISTENTES'


def tabla_conciliacion(d):
    filas = conciliacion(d)
    if not filas:
        return []
    salida = ['| Qué | k6 dice que creó | Nuevos en la base | Resultado |', '|---|---|---|---|']
    salida += [f'| {a} | {b} | {c} | {e} |' for a, b, c, e in filas]
    integ = d['integridad'] or {}
    for clave, texto in (('mascotas_sin_apoderado', 'Mascotas sin propietario'), ('citas_sin_mascota', 'Citas sin mascota'),
                         ('consultas_sin_cita', 'Consultas sin cita'), ('documentos_repetidos', 'Documentos de propietarios repetidos')):
        valor = integ.get(clave)
        salida.append(f"| {texto} (debe ser 0) | 0 | {valor} | {'coincide' if valor == 0 else 'ROTO'} |")
    return salida


def tabla_escrituras(d):
    k = (d['k6'] or {}).get('pasos', {})
    if not k:
        return []
    filas = ['| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |', '|---|---|---|---|---|---|']
    for p in ORDEN_PASOS:
        c, f = correctos(d, p), fallidos(d, p)
        if c + f == 0:
            continue
        ep = d['endpoints'].get(p)
        mediana = n.fmt(statistics.median(ep['duraciones'])) if ep and ep['duraciones'] else '-'
        p95 = n.fmt(n.percentil(ep['duraciones'], 0.95)) if ep and ep['duraciones'] else '-'
        filas.append(f"| {ETIQUETAS.get(p, p)} | {c} | {f} | {f / (c + f) * 100:.1f} % | {mediana} | {p95} |")
    return filas


def tasa_exito_escrituras(d):
    k = (d['k6'] or {}).get('pasos', {})
    c = sum(v.get('correctos', 0) for v in k.values())
    f = sum(v.get('fallidos', 0) for v in k.values())
    return (c, f)


def notas_generales(d, m):
    salida = []
    for nota in n.que_paso(d, m):
        if nota.startswith('Último tramo sano'):
            nota = nota.replace('Último tramo sano (menos de 1 % de errores y p95 menor a 2 s)', 'Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s)')
        salida.append(nota.replace('100 % = el límite de 1 CPU', f"100 % = el límite de {d['limite_cpu']} CPU"))
    log = d['log']
    otros = log['oom_total'] - log['sin_memoria_java'] - log['oom_metaspace']
    if log['sin_memoria_java']:
        salida.append(f"Java agotó el heap: {log['sin_memoria_java']} errores `OutOfMemoryError: Java heap space`.")
    if otros > 0:
        salida.append(f"Otros `OutOfMemoryError` que no son de heap ni de Metaspace: {otros}.")
    est = d['estado'] or {}
    if est.get('correos_recibidos') is not None:
        salida.append(f"Correo: el servidor de pruebas recibió {est['correos_recibidos']} mensajes; {log['correo_fallido']} envíos fallidos en el registro del backend.")
    return salida


def tabla_sql_top(d):
    filas = d.get('sql_top') or []
    if not filas:
        return []
    total_s = (d['estado'] or {}).get('sql_tiempo_s') or 0
    salida = ['| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |', '|---|---|---|---|---|']
    for f in filas:
        consulta = str(f.get('consulta', '')).replace('|', '/')
        pct = f"{f['total_ms'] / 1000 / total_s * 100:.0f} %" if total_s else '-'
        salida.append(f"| `{consulta}` | {f.get('calls')} | {f['total_ms'] / 1000:.1f} | {f.get('media_ms')} | {pct} |")
    return salida


def bloque_version(titulo, d, m, con_sql=True):
    c, f = tasa_exito_escrituras(d)
    lineas = [f'## {titulo}: {n.veredicto(m)}; datos {estado_datos(d)}', '', f'- Escrituras correctas: {c}; fallidas: {f}.']
    acc = (d['k6'] or {}).get('acciones', {})
    if acc:
        lineas.append('- Acciones hechas: ' + ', '.join(f'{k} {v}' for k, v in acc.items()) + f"; sin trabajo disponible: {(d['k6'] or {}).get('acciones_sin_trabajo', 0)}.")
    for nota in notas_generales(d, m):
        lineas.append(f'- {nota}')
    for nota in notas_jvm(d, d['jvm']):
        lineas.append(f'- {nota}')
    lineas.append('')
    tj = tabla_jvm(d['jvm'])
    if tj:
        lineas += ['**Metaspace y clases cargadas (registro de la propia JVM):**', ''] + tj + ['']
    tc = tabla_clases_tardias(d['jvm'])
    if tc:
        lineas += tc + ['']
    tsql = tabla_sql_top(d) if con_sql else []
    if tsql:
        lineas += ['**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**', ''] + tsql + ['']
    tcon = tabla_conciliacion(d)
    if tcon:
        lineas += ['**Comprobación contra la base de datos:**', ''] + tcon + ['']
        lineas.append('_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._')
        lineas.append('')
    te = tabla_escrituras(d)
    if te:
        lineas += ['**Escrituras por paso:**', ''] + te + ['']
    tab = n.tabla_endpoints(d)
    if tab:
        lineas += [f'**Endpoints que más tiempo acumulan, {titulo}:**', ''] + tab + ['']
    tl = n.linea_tiempo_md(d)
    if tl:
        lineas += ['<details><summary>Línea de tiempo cada 15 s</summary>', ''] + tl + ['', '</details>', '']
    return lineas


def lineas_entorno():
    salida = []
    entorno = RAIZ / 'entorno.txt'
    if entorno.exists():
        for linea in entorno.read_text(encoding='utf-8').splitlines():
            if linea.startswith(('recursos del backend', 'pausa entre acciones', 'antes', 'despues', 'usadas en esta corrida', 'correo:', 'procesador', 'versiones medidas')):
                salida.append(f'- {linea}')
    return salida


def etiquetar_tabla(lineas):
    return [x.replace('Último tramo sano (usuarios)', 'Usuarios sin degradación') for x in lineas]


def generar():
    antes, despues = analizar('antes'), analizar('despues')
    ma, md = n.metricas(antes), n.metricas(despues)
    lineas = [f'# Fase 2b: carga con escrituras, {NIVEL} usuarios simultáneos (Metaspace {antes["jvm"]["tope_mb"]} MB, correo de pruebas)', '']
    lineas += lineas_entorno() + ['']
    lineas += etiquetar_tabla(n.tabla_resumen(ma, md, antes, despues)) + ['']
    lineas += bloque_version('ANTES', antes, ma)
    lineas += bloque_version('DESPUÉS', despues, md)
    lineas.append('> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor, registros de la JVM y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.')
    (NIV / f'INFORME_CARGA_{NIVEL}.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
    print('\n'.join(lineas))


def tabla_una(d, m):
    e = d['estado'] or {}
    filas = [
        ('Veredicto', n.veredicto(m)),
        ('Usuarios sin degradación', n.fmt(m['sano'])),
        ('Peticiones hechas', n.fmt(m['peticiones'])),
        ('Peticiones correctas (2xx)', n.fmt(m['ok'])),
        ('Errores (%)', n.fmt(m['errores_pct'], 1, ' %')),
        ('Ritmo medio (peticiones/s)', n.fmt(m['rps'], 1)),
        ('Tiempo mediano (ms)', n.fmt(m['mediana_ms'])),
        ('Tiempo p95 (ms)', n.fmt(m['p95_ms'])),
        (f"CPU del backend, media (% del límite de {d['limite_cpu']} CPU)", n.fmt(d['cpu']['media'], 0)),
        ('Tiempo con la CPU al tope (%)', n.fmt(d['cpu']['tope_pct'], 0)),
        ('CPU de PostgreSQL, media (% de 1 núcleo)', n.fmt(d['cpu_pg']['media'], 0)),
        (f"Memoria pico (MB, de {d['limite_mem_mb']})", n.fmt(d['mem_pico_mb'])),
        ('Sentencias SQL', n.fmt(e.get('sql_sentencias'))),
        ('Reinicios del contenedor', n.fmt(e.get('reinicios'))),
    ]
    return ['| | DESPUÉS |', '|---|---|'] + [f'| {a} | {b} |' for a, b in filas]


def generar_una():
    d = analizar('despues')
    m = n.metricas(d)
    lineas = [f'# Fase 3: {NIVEL} usuarios simultáneos, {d["limite_cpu"]} CPU, {d["limite_mem_mb"] // 1024} GB, heap {d["heap_max"]}', '']
    lineas += lineas_entorno() + [''] + tabla_una(d, m) + ['']
    lineas += bloque_version('DESPUÉS', d, m)
    lineas.append('> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.')
    (NIV / f'INFORME_CARGA_{NIVEL}.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
    print('\n'.join(lineas))


def plan_consentimiento(d):
    ruta = NIV / f"plan_pg_{d['et']}.log.gz"
    lineas = leer_lineas_gz(ruta)
    if not lineas:
        return []
    bloques, actual = [], None
    for linea in lineas:
        if 'duration:' in linea and 'plan:' in linea:
            if actual:
                bloques.append(actual)
            actual = [linea]
        elif actual is not None:
            if linea.startswith(('LOG:', '2026-', '20')) and 'duration:' not in linea and 'LOG:' in linea:
                bloques.append(actual)
                actual = None
            else:
                actual.append(linea)
    if actual:
        bloques.append(actual)
    candidatos = []
    for b in bloques:
        texto = '\n'.join(b)
        if 'consentimiento_datos' not in texto:
            continue
        m = re.search(r'duration: ([\d.]+) ms', b[0])
        candidatos.append((float(m.group(1)) if m else 0.0, b))
    if not candidatos:
        return ['_Sin planes lentos de consentimiento_datos en el registro de PostgreSQL (auto_explain, muestreo del 10 %, umbral 300 ms)._']
    candidatos.sort(key=lambda x: x[0], reverse=True)
    duracion, bloque = candidatos[0]
    salida = [f'**Plan más lento observado de la consulta sobre consentimiento_datos ({duracion:.0f} ms; {len(candidatos)} planes lentos muestreados):**', '', '```']
    salida += [x.rstrip() for x in bloque[:70]]
    salida += ['```']
    return salida


def filas_consentimiento(a, b):
    def val(d, clave):
        x = d.get('sql_consent') or {}
        return x.get(clave)

    def tab(d, clave):
        x = d.get('consent_despues') or {}
        return x.get(clave)

    def indices(d):
        x = (d.get('consent_despues') or {}).get('indices') or []
        return str(len(x))

    return [
        ('Consultas sobre consentimiento_datos (llamadas)', n.fmt(val(a, 'llamadas')), n.fmt(val(b, 'llamadas'))),
        ('Tiempo total en PostgreSQL (s)', n.fmt((val(a, 'total_ms') or 0) / 1000, 1) if val(a, 'total_ms') is not None else 'sin datos', n.fmt((val(b, 'total_ms') or 0) / 1000, 1) if val(b, 'total_ms') is not None else 'sin datos'),
        ('Tiempo medio por llamada (ms)', n.fmt(val(a, 'media_ms'), 1), n.fmt(val(b, 'media_ms'), 1)),
        ('Filas de consentimiento_datos al terminar', n.fmt(tab(a, 'filas')), n.fmt(tab(b, 'filas'))),
        ('Tamaño de la tabla con índices (MB)', n.fmt(tab(a, 'tabla_mb'), 1), n.fmt(tab(b, 'tabla_mb'), 1)),
        ('Índices de la tabla', indices(a), indices(b)),
    ]


def respuesta_panel(d):
    try:
        datos = json.loads(Path(d['ruta_respuesta']).read_text(encoding='utf-8'))
    except (OSError, ValueError):
        return None
    cuerpo = datos.get('data', datos) if isinstance(datos, dict) else datos
    if isinstance(cuerpo, dict):
        cuerpo = {k: v for k, v in cuerpo.items() if k != 'recentLogs'}
    return cuerpo


def filas_panel(a, b):
    def por_carga(d, clave):
        x = d.get('sonda') or {}
        if x.get('llamadas') and x.get(clave) is not None:
            return x[clave] / x['llamadas']
        return None

    def mediana_sonda(d):
        t = (d.get('sonda') or {}).get('tiempos_ms') or []
        return statistics.median(t) if t else None

    def primera(d):
        return (d.get('sonda') or {}).get('primera_llamada_ms')

    def bajo_carga(d, mediana):
        ep = d['endpoints'].get('panel')
        if not ep or not ep['duraciones']:
            return None
        return statistics.median(ep['duraciones']) if mediana else n.percentil(ep['duraciones'], 0.95)

    ra, rb = respuesta_panel(a), respuesta_panel(b)
    igual = 'sin datos' if ra is None or rb is None else ('sí' if ra == rb else 'NO')
    return [
        ('Sentencias SQL por carga del panel (sonda, sin carga)', n.fmt(por_carga(a, 'sentencias_sql'), 1), n.fmt(por_carga(b, 'sentencias_sql'), 1)),
        ('Tiempo SQL por carga del panel (ms, sin carga)', n.fmt(por_carga(a, 'tiempo_sql_ms'), 1), n.fmt(por_carga(b, 'tiempo_sql_ms'), 1)),
        ('Panel sin carga: mediana de 19 llamadas (ms)', n.fmt(mediana_sonda(a)), n.fmt(mediana_sonda(b))),
        ('Panel sin carga: primera llamada (ms)', n.fmt(primera(a)), n.fmt(primera(b))),
        ('Panel bajo carga: mediana (ms)', n.fmt(bajo_carga(a, True)), n.fmt(bajo_carga(b, True))),
        ('Panel bajo carga: p95 (ms)', n.fmt(bajo_carga(a, False)), n.fmt(bajo_carga(b, False))),
        ('Respuesta del panel idéntica a la del código anterior (sin el registro reciente)', '-', igual),
    ]


def tabla_xms(a, b, ma, mb, nombre_a='SIN -Xms', nombre_b='CON -Xms = -Xmx', con_consent=False, extra=None):
    ja, jb = a['jvm'], b['jvm']
    dur_a, dur_b = ja['duracion_jvm_s'] or 1, jb['duracion_jvm_s'] or 1

    def pct(j, dur):
        return n.fmt(j['pausa_total_s'] / dur * 100, 1, ' %') if j['pausas'] else 'sin datos'

    filas = [
        ('Veredicto', n.veredicto(ma), n.veredicto(mb)),
        ('Errores (%)', n.fmt(ma['errores_pct'], 1, ' %'), n.fmt(mb['errores_pct'], 1, ' %')),
        ('Ritmo medio (peticiones/s)', n.fmt(ma['rps'], 1), n.fmt(mb['rps'], 1)),
        ('Tiempo mediano (ms)', n.fmt(ma['mediana_ms']), n.fmt(mb['mediana_ms'])),
        ('Tiempo p95 (ms)', n.fmt(ma['p95_ms']), n.fmt(mb['p95_ms'])),
        ('Usuarios sin degradación', n.fmt(ma['sano']), n.fmt(mb['sano'])),
        ('CPU del backend, media (% del límite)', n.fmt(a['cpu']['media'], 0), n.fmt(b['cpu']['media'], 0)),
        ('CPU de PostgreSQL, media (% de 1 núcleo)', n.fmt(a['cpu_pg']['media'], 0), n.fmt(b['cpu_pg']['media'], 0)),
        ('Memoria pico (MB)', n.fmt(a['mem_pico_mb']), n.fmt(b['mem_pico_mb'])),
        ('Capacidad del heap en la primera pausa de GC (MB)', n.fmt(ja['heap_inicial_mb']), n.fmt(jb['heap_inicial_mb'])),
        ('Pausas de GC en total', n.fmt(ja['pausas']), n.fmt(jb['pausas'])),
        ('Pausas completas', n.fmt(ja['completos']), n.fmt(jb['completos'])),
        ('Pausas completas por "Allocation Failure"', n.fmt(ja['completas_por_heap']), n.fmt(jb['completas_por_heap'])),
        ('Tiempo en pausas completas (s)', n.fmt(ja['completas_s'], 1), n.fmt(jb['completas_s'], 1)),
        ('Tiempo en GC / vida de la JVM', pct(ja, dur_a), pct(jb, dur_b)),
        ('Heap tras GC, máximo (MB)', n.fmt(ja['heap_tras_gc_max_mb']), n.fmt(jb['heap_tras_gc_max_mb'])),
        ('Avisos de pool agotado', n.fmt(a['log']['pool_conexiones']), n.fmt(b['log']['pool_conexiones'])),
    ]
    if con_consent:
        filas = filas[:9] + filas_consentimiento(a, b) + filas[9:]
    if extra:
        filas = filas[:9] + extra + filas[9:]
    return [f'| | {nombre_a} | {nombre_b} |', '|---|---|---|'] + [f'| {x} | {y} | {z} |' for x, y, z in filas]


def generar_variantes(pref_a, nombre_a, pref_b, nombre_b, titulo, con_consent=False, nota='', panel=False):
    a, b = analizar(pref_a), analizar(pref_b)
    ma, mb = n.metricas(a), n.metricas(b)
    lineas = [f'# {titulo}: {NIVEL} usuarios (1 CPU, 2 GB)', '']
    lineas += lineas_entorno() + ['']
    lineas += tabla_xms(a, b, ma, mb, nombre_a, nombre_b, con_consent, filas_panel(a, b) if panel else None) + ['']
    if con_consent:
        for nombre, d in ((nombre_a, a), (nombre_b, b)):
            plan = plan_consentimiento(d)
            if plan:
                lineas += [f'### {nombre}', ''] + plan + ['']
    lineas += bloque_version(nombre_a, a, ma, con_sql=True)
    lineas += bloque_version(nombre_b, b, mb, con_sql=True)
    if nota:
        lineas.append(nota)
    (NIV / f'INFORME_CARGA_{NIVEL}.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
    print('\n'.join(lineas))


def generar_panel():
    generar_variantes(
        'legado', 'Código anterior', 'optimizado', 'Código nuevo', 'Fase 3d: panel optimizado', panel=True,
        nota='> Mismo servidor, misma imagen y misma base de partida; solo cambia la bandera app.dashboard.consultas-optimizadas. Una corrida por variante y nivel.',
    )


def generar_indice():
    generar_variantes(
        'despues', 'SIN índice', 'indice', 'CON índice', 'Fase 3c: índice en consentimiento_datos', con_consent=True,
        nota='> Cada corrida parte de la misma base (copiada de una plantilla), así que el orden y los datos acumulados no influyen. Las dos variantes usan -Xms = -Xmx. Una corrida por variante y nivel.',
    )


def generar_xms():
    a, b = analizar('despues'), analizar('xms')
    ma, mb = n.metricas(a), n.metricas(b)
    lineas = [f'# Fase 3b: {NIVEL} usuarios, con y sin -Xms (1 CPU, 2 GB)', '']
    lineas += lineas_entorno() + ['']
    lineas += tabla_xms(a, b, ma, mb) + ['']
    lineas += bloque_version('SIN -Xms', a, ma, con_sql=True)
    lineas += bloque_version('CON -Xms = -Xmx', b, mb, con_sql=False)
    lineas.append('> Las dos variantes corrieron en la misma máquina, una detrás de otra, y el orden alterna por nivel. La base conserva lo escrito antes, así que la segunda variante parte con algo más de datos. Una corrida por variante y nivel.')
    (NIV / f'INFORME_CARGA_{NIVEL}.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
    print('\n'.join(lineas))


def generar_control():
    d = analizar('ctrl128', 'E')
    m = n.metricas(d)
    lineas = [f'# Control de la fase 2b: {NIVEL} usuarios con Metaspace de 128 MB (como el Dockerfile) y correo de pruebas', '']
    lineas += lineas_entorno() + ['']
    lineas += bloque_version('CONTROL (DESPUÉS, Metaspace 128 MB)', d, m)
    lineas.append('> Misma versión DESPUÉS y mismo escenario que el nivel de 10 usuarios de la escalera, pero con el tope de Metaspace del Dockerfile actual. Si aquí aparece `OutOfMemoryError: Metaspace` y en la escalera (256 MB) no, el tope de 128 MB es insuficiente.')
    (NIV / 'INFORME_CONTROL_128.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
    print('\n'.join(lineas))


if __name__ == '__main__':
    configurar(sys.argv[1], sys.argv[2])
    if len(sys.argv) > 3 and sys.argv[3] == 'control':
        generar_control()
    elif len(sys.argv) > 3 and sys.argv[3] == 'una':
        generar_una()
    elif len(sys.argv) > 3 and sys.argv[3] == 'xms':
        generar_xms()
    elif len(sys.argv) > 3 and sys.argv[3] == 'indice':
        generar_indice()
    elif len(sys.argv) > 3 and sys.argv[3] == 'panel':
        generar_panel()
    else:
        generar()

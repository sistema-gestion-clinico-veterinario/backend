import csv
import gzip
import json
import os
import re
import statistics
import sys
from collections import defaultdict
from pathlib import Path
from urllib.parse import parse_qs

RAIZ = NIV = LOGS = None
NIVEL = 0
CFG = 'C'


def configurar(raiz, nivel, cfg):
    global RAIZ, NIV, LOGS, NIVEL, CFG
    RAIZ = Path(raiz)
    NIV = RAIZ / 'niveles'
    LOGS = RAIZ / 'logs'
    NIVEL = int(nivel)
    CFG = cfg

LIMITE_MEM_MB = 2048
UMBRAL_TOPE_CPU = 95
UMBRAL_P99_MS = float(os.environ.get('UMBRAL_P99_MS') or 5000)


def leer_json(ruta):
    try:
        return json.loads(Path(ruta).read_text(encoding='utf-8'))
    except (OSError, ValueError):
        return None


def fmt(valor, decimales=0, sufijo=''):
    if valor is None:
        return 'sin datos'
    return f'{valor:,.{decimales}f}{sufijo}'.replace(',', ' ')


def p99_dentro(m):
    if m.get('p99_ms') is None:
        return 'sin datos'
    return 'sí' if m['p99_ms'] < UMBRAL_P99_MS else 'no'


def etiqueta_p99():
    return f'p99 menor a {UMBRAL_P99_MS:.0f} ms (criterio experimental)'


def percentil(valores, p):
    if not valores:
        return None
    ordenados = sorted(valores)
    return ordenados[min(len(ordenados) - 1, int(len(ordenados) * p))]


def leer_estadisticas(ruta):
    cpus, mems = [], []
    try:
        with open(ruta, encoding='utf-8') as f:
            for linea in f:
                partes = linea.strip().split(',', 2)
                if len(partes) < 3:
                    continue
                cpu = re.match(r'([\d.]+)%', partes[1])
                mem = re.match(r'([\d.]+)\s*([KMG]i?B)', partes[2])
                if cpu:
                    cpus.append(float(cpu.group(1)))
                if mem:
                    valor = float(mem.group(1))
                    unidad = mem.group(2).upper()
                    mems.append(valor * {'KIB': 1 / 1024, 'KB': 1 / 1024, 'MIB': 1, 'MB': 1, 'GIB': 1024, 'GB': 1024}.get(unidad, 1))
    except OSError:
        pass
    return cpus, mems


def resumen_cpu(cpus):
    if not cpus:
        return {'media': None, 'pico': None, 'tope_pct': None}
    return {
        'media': statistics.mean(cpus),
        'pico': max(cpus),
        'tope_pct': sum(1 for c in cpus if c >= UMBRAL_TOPE_CPU) / len(cpus) * 100,
    }


def contar_en_log(ruta, patrones):
    cuentas = {k: 0 for k in patrones}
    try:
        with open(ruta, encoding='utf-8', errors='replace') as f:
            for linea in f:
                for nombre, patron in patrones.items():
                    if re.search(patron, linea):
                        cuentas[nombre] += 1
    except OSError:
        pass
    return cuentas


def es_fallo(status, error):
    if error:
        return True
    if status in ('', '0'):
        return True
    return status.isdigit() and int(status) >= 400


def leer_k6_csv(ruta):
    cubos = defaultdict(lambda: {'duraciones': [], 'total': 0, 'fallos': 0})
    usuarios = {}
    endpoints = defaultdict(lambda: {'n': 0, 'errores': 0, 'duraciones': []})
    inicio = None
    try:
        with gzip.open(ruta, 'rt', encoding='utf-8', errors='replace', newline='') as f:
            for fila in csv.DictReader(f):
                try:
                    t = int(float(fila['timestamp']))
                    valor = float(fila['metric_value'])
                except (KeyError, ValueError):
                    continue
                if inicio is None or t < inicio:
                    inicio = t
                cubo = (t // 15) * 15
                metrica = fila.get('metric_name')
                if metrica == 'http_req_duration':
                    cubos[cubo]['duraciones'].append(valor)
                    cubos[cubo]['total'] += 1
                    fallo = es_fallo(fila.get('status', ''), fila.get('error', ''))
                    if fallo:
                        cubos[cubo]['fallos'] += 1
                    etiquetas = parse_qs(fila.get('extra_tags', ''))
                    ruta_ep = (etiquetas.get('ruta') or ['?'])[0]
                    ep = endpoints[ruta_ep]
                    ep['n'] += 1
                    ep['duraciones'].append(valor)
                    if fallo:
                        ep['errores'] += 1
                elif metrica == 'vus':
                    usuarios[cubo] = max(usuarios.get(cubo, 0), valor)
    except OSError:
        return [], {}
    tramos = []
    for cubo in sorted(cubos):
        c = cubos[cubo]
        total = c['total']
        tramos.append({
            'desde_s': max(0, cubo - (inicio or cubo)),
            'peticiones': total,
            'p95_ms': percentil(c['duraciones'], 0.95),
            'p99_ms': percentil(c['duraciones'], 0.99),
            'errores_pct': (c['fallos'] / total * 100) if total else 0,
            'usuarios': usuarios.get(cubo),
        })
    return tramos, endpoints


def analizar(version):
    etiqueta = f'{version}_{NIVEL}_{CFG}'
    cpus, mems = leer_estadisticas(NIV / f'stats_{etiqueta}.csv')
    cpus_pg, _ = leer_estadisticas(NIV / f'stats_pg_{etiqueta}.csv')
    tramos, endpoints = leer_k6_csv(NIV / f'k6_{etiqueta}.csv.gz')
    return {
        'version': version,
        'k6': leer_json(NIV / f'k6_{etiqueta}.json'),
        'estado': leer_json(NIV / f'estado_{etiqueta}.json'),
        'cpu': resumen_cpu(cpus),
        'cpu_pg': resumen_cpu(cpus_pg),
        'mem_pico_mb': max(mems) if mems else None,
        'log': contar_en_log(LOGS / f'backend_{etiqueta}.log', {
            'sin_memoria_java': r'OutOfMemoryError',
            'pool_conexiones': r'Connection is not available|HikariPool.*(timeout|Timeout)',
            'hilos_agotados': r'RejectedExecutionException|Thread pool',
        }),
        'tramos': tramos,
        'endpoints': endpoints,
    }


def usuarios_sanos(tramos):
    sano = None
    for t in tramos:
        if t['peticiones'] < 3 or t['usuarios'] is None:
            continue
        if t['errores_pct'] > 5:
            break
        if t['errores_pct'] < 1 and (t['p95_ms'] or 0) < 2000:
            sano = max(sano or 0, t['usuarios'])
    return sano


def metricas(datos):
    k6 = datos['k6'] or {}
    est = k6.get('estados', {})
    peticiones = k6.get('peticiones') or 0
    ok = est.get('2xx', 0)
    errores = max(peticiones - ok, 0)
    duracion = k6.get('duracion_total_s') or 0
    return {
        'peticiones': peticiones,
        'ok': ok,
        'errores': errores,
        'errores_pct': (errores / peticiones * 100) if peticiones else None,
        'rps': (peticiones / duracion) if duracion else None,
        'p95_ms': (k6.get('latencia_ms') or {}).get('p95'),
        'p99_ms': (k6.get('latencia_ms') or {}).get('p99'),
        'mediana_ms': (k6.get('latencia_ms') or {}).get('mediana'),
        'estados': est,
        'sano': usuarios_sanos(datos['tramos']),
    }


def veredicto(m):
    if m['peticiones'] == 0 or m['errores_pct'] is None:
        return 'SIN DATOS'
    if m['errores_pct'] < 1 and (m['p95_ms'] or 0) < 2000:
        return 'AGUANTA'
    if m['errores_pct'] < 5 and (m['p95_ms'] or 0) < 5000:
        return 'DEGRADADO'
    if m['errores_pct'] < 5:
        return 'SATURADO (muy lento, casi sin errores)'
    return 'FALLA'


def que_paso(datos, m):
    notas = []
    est = datos['estado'] or {}
    if not datos['k6']:
        return ['No se obtuvo el resumen de k6: la prueba no terminó o k6 falló (ver logs).']
    if est.get('oom_killed'):
        notas.append(f"**El contenedor se quedó sin memoria y el sistema lo mató (OOMKilled).** Reinicios: {est.get('reinicios', 0)}.")
    elif est.get('reinicios'):
        notas.append(f"El contenedor se reinició {est['reinicios']} veces.")
    if est.get('sigue_corriendo') is False:
        notas.append(f"Al terminar el backend **ya no estaba corriendo** (código de salida {est.get('codigo_salida')}).")
    elif est.get('salud_final_http') not in (None, '200'):
        notas.append(f"Al terminar, el chequeo de salud respondió {est.get('salud_final_http')} (no 200).")
    log = datos['log']
    if log['sin_memoria_java']:
        notas.append(f"Java agotó su memoria de trabajo (heap): {log['sin_memoria_java']} errores `OutOfMemoryError` en el registro.")
    if log['pool_conexiones']:
        notas.append(f"Se agotó el pool de conexiones a la base de datos: {log['pool_conexiones']} avisos en el registro.")
    if log['hilos_agotados']:
        notas.append(f"El servidor rechazó trabajo por falta de hilos: {log['hilos_agotados']} avisos.")

    cpu, pg = datos['cpu'], datos['cpu_pg']
    if cpu['media'] is not None:
        notas.append(
            f"CPU del backend: {fmt(cpu['media'], 0, ' %')} de media y {fmt(cpu['pico'], 0, ' %')} de pico "
            f"(100 % = el límite de 1 CPU); estuvo al tope el {fmt(cpu['tope_pct'], 0, ' %')} del tiempo."
        )
    if pg['media'] is not None:
        notas.append(f"CPU de PostgreSQL: {fmt(pg['media'], 0, ' %')} de media y {fmt(pg['pico'], 0, ' %')} de pico (de 1 núcleo).")
    if cpu['tope_pct'] is not None:
        if cpu['tope_pct'] >= 40:
            notas.append('**Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).')
        elif pg['media'] is not None and pg['media'] >= 80:
            notas.append('**Diagnóstico: el límite parece ser la base de datos** (PostgreSQL casi al tope y el backend con CPU de sobra).')
        elif m['errores_pct'] and m['errores_pct'] >= 5:
            notas.append('**Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.')
    if datos['mem_pico_mb'] is not None:
        pct = datos['mem_pico_mb'] / LIMITE_MEM_MB * 100
        notas.append(f"Memoria del contenedor: pico de {fmt(datos['mem_pico_mb'], 0, ' MB')} ({fmt(pct, 0, ' %')} de {LIMITE_MEM_MB} MB).")

    est_k6 = m['estados']
    fallos = m['errores']
    if fallos:
        partes = []
        for clave, texto in (('sin_respuesta', 'sin respuesta (tiempo agotado)'), ('5xx', 'errores 5xx del servidor'), ('401', '401 (sesión perdida)'), ('429', '429 (límite de peticiones)'), ('otro_4xx', 'otros 4xx')):
            cuenta = est_k6.get(clave, 0)
            if cuenta:
                partes.append(f'{cuenta / fallos * 100:.0f} % {texto}')
        if partes:
            notas.append('Los fallos fueron: ' + ', '.join(partes) + '.')

    corte = next((t for t in datos['tramos'] if t['peticiones'] >= 5 and t['errores_pct'] > 5), None)
    if corte:
        usuarios = f" con unos {corte['usuarios']:.0f} usuarios" if corte['usuarios'] else ''
        notas.append(f"**Los errores superaron el 5 % por primera vez al minuto {corte['desde_s'] / 60:.1f}{usuarios}.**")
    elif datos['tramos']:
        notas.append('Los errores no superaron el 5 % en ningún tramo de 15 s.')
    if m['sano'] is not None:
        notas.append(f"Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **{m['sano']:.0f} usuarios**.")
    peor = max((t['p95_ms'] or 0 for t in datos['tramos']), default=0)
    if peor:
        notas.append(f"El peor tiempo p95 en un tramo de 15 s fue de {peor / 1000:.1f} s.")
    return notas


def tabla_resumen(m1, m2, d1, d2):
    e1, e2 = d1['estado'] or {}, d2['estado'] or {}
    filas = [
        ('Veredicto', veredicto(m1), veredicto(m2)),
        ('Último tramo sano (usuarios)', fmt(m1['sano']), fmt(m2['sano'])),
        ('Peticiones hechas', fmt(m1['peticiones']), fmt(m2['peticiones'])),
        ('Peticiones correctas (2xx)', fmt(m1['ok']), fmt(m2['ok'])),
        ('Errores (%)', fmt(m1['errores_pct'], 1, ' %'), fmt(m2['errores_pct'], 1, ' %')),
        ('Ritmo medio (peticiones/s)', fmt(m1['rps'], 1), fmt(m2['rps'], 1)),
        ('Tiempo mediano (ms)', fmt(m1['mediana_ms']), fmt(m2['mediana_ms'])),
        ('Tiempo p95 (ms)', fmt(m1['p95_ms']), fmt(m2['p95_ms'])),
        ('Tiempo p99 (ms)', fmt(m1['p99_ms']), fmt(m2['p99_ms'])),
        (etiqueta_p99(), p99_dentro(m1), p99_dentro(m2)),
        ('CPU del backend, media (%)', fmt(d1['cpu']['media'], 0), fmt(d2['cpu']['media'], 0)),
        ('Tiempo con la CPU al tope (%)', fmt(d1['cpu']['tope_pct'], 0), fmt(d2['cpu']['tope_pct'], 0)),
        ('CPU de PostgreSQL, media (%)', fmt(d1['cpu_pg']['media'], 0), fmt(d2['cpu_pg']['media'], 0)),
        ('Memoria pico (MB)', fmt(d1['mem_pico_mb']), fmt(d2['mem_pico_mb'])),
        ('Sentencias SQL', fmt(e1.get('sql_sentencias')), fmt(e2.get('sql_sentencias'))),
        ('Reinicios del contenedor', fmt(e1.get('reinicios')), fmt(e2.get('reinicios'))),
    ]
    salida = ['| | ANTES | DESPUÉS |', '|---|---|---|']
    salida += [f'| {a} | {b} | {c} |' for a, b, c in filas]
    return salida


def tabla_endpoints(datos):
    eps = datos['endpoints']
    if not eps:
        return []
    total = sum(sum(e['duraciones']) for e in eps.values()) or 1
    filas = ['| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |', '|---|---|---|---|---|---|---|']
    ordenados = sorted(eps.items(), key=lambda kv: sum(kv[1]['duraciones']), reverse=True)[:12]
    for nombre, e in ordenados:
        errores = f"{e['errores']} ({e['errores'] / e['n'] * 100:.0f} %)" if e['n'] else '0'
        filas.append(
            f"| {nombre} | {e['n']} | {errores} | {fmt(statistics.median(e['duraciones']))} | "
            f"{fmt(percentil(e['duraciones'], 0.95))} | {fmt(percentil(e['duraciones'], 0.99))} | {sum(e['duraciones']) / total * 100:.0f} % |"
        )
    return filas


def linea_tiempo_md(datos):
    if not datos['tramos']:
        return []
    filas = ['| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |', '|---|---|---|---|---|---|']
    pasos = max(1, len(datos['tramos']) // 14)
    for tramo in datos['tramos'][::pasos]:
        usuarios = fmt(tramo['usuarios']) if tramo['usuarios'] is not None else '-'
        filas.append(f"| {tramo['desde_s'] / 60:.1f} | {usuarios} | {tramo['peticiones']} | {fmt(tramo['p95_ms'])} | {fmt(tramo['p99_ms'])} | {tramo['errores_pct']:.0f} % |")
    return filas


def generar():
    antes = analizar('antes')
    despues = analizar('despues')
    ma, md = metricas(antes), metricas(despues)

    lineas = [f'# Capacidad: {NIVEL} usuarios simultáneos, configuración {CFG}', '']
    entorno = RAIZ / 'entorno.txt'
    if entorno.exists():
        for linea in entorno.read_text(encoding='utf-8').splitlines():
            if linea.startswith(('recursos del backend', 'configuración de la JVM', 'pausa entre acciones', 'mezcla de acciones')):
                lineas.append(f'- {linea}')
    lineas.append('')
    lineas += tabla_resumen(ma, md, antes, despues)
    lineas.append('')
    for datos, m, titulo in ((antes, ma, 'ANTES'), (despues, md, 'DESPUÉS')):
        lineas.append(f'## Qué pasó en el {titulo}: {veredicto(m)}')
        lineas.append('')
        for nota in que_paso(datos, m):
            lineas.append(f'- {nota}')
        lineas.append('')
        te = tabla_endpoints(datos)
        if te:
            lineas.append(f'**Por endpoint, {titulo}** (los que más tiempo acumulan):')
            lineas.append('')
            lineas += te
            lineas.append('')
        tl = linea_tiempo_md(datos)
        if tl:
            lineas.append('<details><summary>Línea de tiempo cada 15 s</summary>')
            lineas.append('')
            lineas += tl
            lineas.append('')
            lineas.append('</details>')
            lineas.append('')
    lineas.append('> Veredicto: AGUANTA = menos de 1 % de errores y p95 menor a 2 s; DEGRADADO = menos de 5 % y p95 menor a 5 s; SATURADO = menos de 5 % de errores pero p95 de 5 s o más; FALLA = 5 % o más de errores. Los usuarios virtuales hacen una acción cada ~15 s (con variación), no peticiones sin pausa. Es una medición de laboratorio, no una promesa de producción.')

    salida = NIV / f'INFORME_{NIVEL}_{CFG}.md'
    salida.write_text('\n'.join(lineas) + '\n', encoding='utf-8')
    print('\n'.join(lineas))


if __name__ == '__main__':
    configurar(sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else 'C')
    generar()

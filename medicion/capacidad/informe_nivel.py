import csv
import gzip
import json
import re
import statistics
import sys
from collections import defaultdict
from pathlib import Path

RAIZ = Path(sys.argv[1])
NIVEL = int(sys.argv[2])
NIV = RAIZ / 'niveles'
LOGS = RAIZ / 'logs'
VERSIONES = ('antes', 'despues')
LIMITE_MEM_MB = 2048


def leer_json(ruta):
    try:
        return json.loads(Path(ruta).read_text(encoding='utf-8'))
    except (OSError, ValueError):
        return None


def mediana(valores):
    return statistics.median(valores) if valores else None


def fmt(valor, decimales=0, sufijo=''):
    if valor is None:
        return 'sin datos'
    return f'{valor:,.{decimales}f}{sufijo}'.replace(',', ' ')


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


def percentil(valores, p):
    if not valores:
        return None
    ordenados = sorted(valores)
    return ordenados[min(len(ordenados) - 1, int(len(ordenados) * p))]


def linea_de_tiempo(ruta):
    cubos = defaultdict(lambda: {'duraciones': [], 'total': 0, 'fallos': 0})
    usuarios = {}
    inicio = None
    try:
        with gzip.open(ruta, 'rt', encoding='utf-8', errors='replace', newline='') as f:
            lector = csv.DictReader(f)
            for fila in lector:
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
                elif metrica == 'http_reqs':
                    cubos[cubo]['total'] += 1
                elif metrica == 'http_req_failed':
                    cubos[cubo]['fallos'] += 1 if valor >= 1 else 0
                elif metrica == 'vus':
                    usuarios[cubo] = max(usuarios.get(cubo, 0), valor)
    except OSError:
        return [], None
    filas = []
    for cubo in sorted(cubos):
        c = cubos[cubo]
        total = c['total'] or len(c['duraciones'])
        filas.append({
            'desde_s': max(0, cubo - (inicio or cubo)),
            'peticiones': total,
            'p95_ms': percentil(c['duraciones'], 0.95),
            'errores_pct': (c['fallos'] / total * 100) if total else 0,
            'usuarios': usuarios.get(cubo),
        })
    return filas, inicio


def analizar(version):
    etiqueta = f'{version}_{NIVEL}'
    k6 = leer_json(NIV / f'k6_{etiqueta}.json')
    estado = leer_json(NIV / f'estado_{etiqueta}.json')
    cpus, mems = leer_estadisticas(NIV / f'stats_{etiqueta}.csv')
    log_backend = LOGS / f'backend_{etiqueta}.log'
    errores_log = contar_en_log(log_backend, {
        'sin_memoria_java': r'OutOfMemoryError',
        'pool_conexiones': r'Connection is not available|HikariPool.*(timeout|Timeout)',
        'hilos_agotados': r'RejectedExecutionException|Thread pool',
        'peticion_abortada': r'ClientAbortException|Broken pipe',
    })
    tramos, _ = linea_de_tiempo(NIV / f'k6_{etiqueta}.csv.gz')
    return {'version': version, 'k6': k6, 'estado': estado, 'cpus': cpus, 'mems': mems, 'log': errores_log, 'tramos': tramos}


def metricas(datos):
    k6 = datos['k6'] or {}
    est = k6.get('estados', {})
    peticiones = k6.get('peticiones') or 0
    ok = est.get('2xx', 0)
    errores = max(peticiones - ok, 0)
    duracion = k6.get('duracion_total_s') or 0
    p95 = (k6.get('latencia_ms') or {}).get('p95')
    return {
        'peticiones': peticiones,
        'ok': ok,
        'errores': errores,
        'errores_pct': (errores / peticiones * 100) if peticiones else None,
        'rps': (peticiones / duracion) if duracion else None,
        'p95_ms': p95,
        'mediana_ms': (k6.get('latencia_ms') or {}).get('mediana'),
        'iteraciones': k6.get('iteraciones_completas'),
        'estados': est,
        'cpu_prom': mediana(datos['cpus']),
        'cpu_pico': max(datos['cpus']) if datos['cpus'] else None,
        'mem_pico_mb': max(datos['mems']) if datos['mems'] else None,
    }


def veredicto(m):
    if m['peticiones'] == 0 or m['errores_pct'] is None:
        return 'SIN DATOS'
    if m['errores_pct'] < 1 and (m['p95_ms'] or 0) < 2000:
        return 'AGUANTA'
    if m['errores_pct'] < 5 and (m['p95_ms'] or 0) < 5000:
        return 'DEGRADADO'
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
    if m['cpu_prom'] is not None and m['cpu_prom'] >= 90:
        notas.append(f"La CPU estuvo al tope: {fmt(m['cpu_prom'], 0, ' %')} de promedio y {fmt(m['cpu_pico'], 0, ' %')} de pico (el límite es 100 % de 1 CPU). **El servidor está limitado por CPU.**")
    elif m['cpu_prom'] is not None:
        notas.append(f"CPU: {fmt(m['cpu_prom'], 0, ' %')} de promedio, {fmt(m['cpu_pico'], 0, ' %')} de pico (de 100 % posible).")
    if m['mem_pico_mb'] is not None:
        pct = m['mem_pico_mb'] / LIMITE_MEM_MB * 100
        notas.append(f"Memoria del contenedor: pico de {fmt(m['mem_pico_mb'], 0, ' MB')} ({fmt(pct, 0, ' %')} de {LIMITE_MEM_MB} MB).")
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
    for tramo in datos['tramos']:
        if tramo['peticiones'] >= 5 and tramo['errores_pct'] > 5:
            usuarios = f" con unos {tramo['usuarios']:.0f} usuarios" if tramo['usuarios'] else ''
            notas.append(f"**Los errores superaron el 5 % por primera vez al minuto {tramo['desde_s'] / 60:.1f}{usuarios}.**")
            break
    else:
        if datos['tramos']:
            notas.append('Los errores no superaron el 5 % en ningún tramo de 15 s.')
    peor = max((t['p95_ms'] or 0 for t in datos['tramos']), default=0)
    if peor:
        notas.append(f"El peor tiempo p95 en un tramo de 15 s fue de {peor / 1000:.1f} s.")
    return notas


def tabla(m1, m2, d1, d2):
    filas = [
        ('Veredicto', veredicto(m1), veredicto(m2)),
        ('Peticiones hechas', fmt(m1['peticiones']), fmt(m2['peticiones'])),
        ('Peticiones correctas (2xx)', fmt(m1['ok']), fmt(m2['ok'])),
        ('Errores (%)', fmt(m1['errores_pct'], 1, ' %'), fmt(m2['errores_pct'], 1, ' %')),
        ('Ritmo medio (peticiones/s)', fmt(m1['rps'], 1), fmt(m2['rps'], 1)),
        ('Tiempo mediano (ms)', fmt(m1['mediana_ms']), fmt(m2['mediana_ms'])),
        ('Tiempo p95 (ms)', fmt(m1['p95_ms']), fmt(m2['p95_ms'])),
        ('CPU promedio del contenedor', fmt(m1['cpu_prom'], 0, ' %'), fmt(m2['cpu_prom'], 0, ' %')),
        ('Memoria pico (MB)', fmt(m1['mem_pico_mb']), fmt(m2['mem_pico_mb'])),
        ('Sentencias SQL', fmt((d1['estado'] or {}).get('sql_sentencias')), fmt((d2['estado'] or {}).get('sql_sentencias'))),
        ('Reinicios del contenedor', fmt((d1['estado'] or {}).get('reinicios')), fmt((d2['estado'] or {}).get('reinicios'))),
    ]
    salida = ['| | ANTES | DESPUÉS |', '|---|---|---|']
    salida += [f'| {a} | {b} | {c} |' for a, b, c in filas]
    return salida


def linea_tiempo_md(datos):
    if not datos['tramos']:
        return []
    filas = ['| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |', '|---|---|---|---|---|']
    pasos = max(1, len(datos['tramos']) // 14)
    for tramo in datos['tramos'][::pasos]:
        usuarios = fmt(tramo['usuarios']) if tramo['usuarios'] is not None else '-'
        filas.append(f"| {tramo['desde_s'] / 60:.1f} | {usuarios} | {tramo['peticiones']} | {fmt(tramo['p95_ms'])} | {tramo['errores_pct']:.0f} % |")
    return filas


antes = analizar('antes')
despues = analizar('despues')
ma, md = metricas(antes), metricas(despues)

lineas = [f'# Capacidad: {NIVEL} usuarios simultáneos', '']
entorno = RAIZ / 'entorno.txt'
if entorno.exists():
    for linea in entorno.read_text(encoding='utf-8').splitlines():
        if linea.startswith(('recursos del backend', 'configuración de la JVM', 'pausa entre acciones')):
            lineas.append(f'- {linea}')
lineas.append('')
lineas += tabla(ma, md, antes, despues)
lineas.append('')
for datos, m, titulo in ((antes, ma, 'ANTES'), (despues, md, 'DESPUÉS')):
    lineas.append(f'## Qué pasó en el {titulo}: {veredicto(m)}')
    lineas.append('')
    for nota in que_paso(datos, m):
        lineas.append(f'- {nota}')
    lineas.append('')
    tl = linea_tiempo_md(datos)
    if tl:
        lineas.append('<details><summary>Línea de tiempo cada 15 s</summary>')
        lineas.append('')
        lineas += tl
        lineas.append('')
        lineas.append('</details>')
        lineas.append('')
lineas.append('> Veredicto: AGUANTA = menos de 1 % de errores y p95 menor a 2 s; DEGRADADO = menos de 5 % y p95 menor a 5 s; FALLA = peor que eso. Los usuarios virtuales hacen una acción cada ~15 s (con variación), no peticiones sin pausa. Es una medición de laboratorio, no una promesa de producción.')

salida = NIV / f'INFORME_{NIVEL}.md'
salida.write_text('\n'.join(lineas) + '\n', encoding='utf-8')
print('\n'.join(lineas))

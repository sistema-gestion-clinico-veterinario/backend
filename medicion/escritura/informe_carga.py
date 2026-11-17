import statistics
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'capacidad'))
import informe_nivel as n  # noqa: E402

RAIZ = NIV = LOGS = None
NIVEL = 0

ORDEN_PASOS = ['apoderado', 'mascota', 'cita', 'checkin', 'iniciar', 'consulta', 'diagnostico', 'receta', 'actualizar_mascota',
               'cerrar_consulta', 'finalizar_servicio', 'punto_cobro', 'vincular_equipo', 'abrir_caja', 'pago', 'cancelar_cita']
ETIQUETAS = {
    'apoderado': 'Registrar propietario', 'mascota': 'Registrar mascota', 'cita': 'Agendar cita', 'checkin': 'Recepción (llegada)',
    'iniciar': 'Iniciar atención', 'consulta': 'Leer la consulta', 'diagnostico': 'Registrar diagnóstico', 'receta': 'Registrar receta',
    'actualizar_mascota': 'Actualizar mascota', 'cerrar_consulta': 'Cerrar la consulta', 'finalizar_servicio': 'Finalizar servicio',
    'punto_cobro': 'Caja: crear punto de cobro', 'vincular_equipo': 'Caja: vincular equipo', 'abrir_caja': 'Caja: abrir la caja',
    'pago': 'Cobrar', 'cancelar_cita': 'Cancelar cita',
}


def configurar(raiz, nivel):
    global RAIZ, NIV, LOGS, NIVEL
    RAIZ = Path(raiz)
    NIV = RAIZ / 'niveles'
    LOGS = RAIZ / 'logs'
    NIVEL = int(nivel)


def analizar(version):
    et = f'{version}_{NIVEL}'
    cpus, mems = n.leer_estadisticas(NIV / f'stats_{et}.csv')
    cpus_pg, _ = n.leer_estadisticas(NIV / f'stats_pg_{et}.csv')
    tramos, endpoints = n.leer_k6_csv(NIV / f'k6_{et}.csv.gz')
    return {
        'version': version,
        'k6': n.leer_json(NIV / f'k6_{et}.json'),
        'estado': n.leer_json(NIV / f'estado_{et}.json'),
        'cpu': n.resumen_cpu(cpus),
        'cpu_pg': n.resumen_cpu(cpus_pg),
        'mem_pico_mb': max(mems) if mems else None,
        'log': n.contar_en_log(LOGS / f'backend_carga_{et}.log', {
            'sin_memoria_java': r'OutOfMemoryError',
            'pool_conexiones': r'Connection is not available|HikariPool.*(timeout|Timeout)',
            'hilos_agotados': r'RejectedExecutionException|Thread pool',
        }),
        'tramos': tramos,
        'endpoints': endpoints,
        'db_antes': n.leer_json(NIV / f'db_antes_{et}.json'),
        'db_despues': n.leer_json(NIV / f'db_despues_{et}.json'),
        'integridad': n.leer_json(NIV / f'integridad_{et}.json'),
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


def generar():
    antes, despues = analizar('antes'), analizar('despues')
    ma, md = n.metricas(antes), n.metricas(despues)
    lineas = [f'# Fase 2: carga con escrituras, {NIVEL} usuarios simultáneos', '']
    entorno = RAIZ / 'entorno.txt'
    if entorno.exists():
        for linea in entorno.read_text(encoding='utf-8').splitlines():
            if linea.startswith(('recursos del backend', 'pausa entre acciones', 'antes', 'despues')):
                lineas.append(f'- {linea}')
    lineas.append('')
    lineas += n.tabla_resumen(ma, md, antes, despues)
    lineas.append('')
    for d, m, titulo in ((antes, ma, 'ANTES'), (despues, md, 'DESPUÉS')):
        c, f = tasa_exito_escrituras(d)
        lineas.append(f'## {titulo}: {n.veredicto(m)}; datos {estado_datos(d)}')
        lineas.append('')
        lineas.append(f'- Escrituras correctas: {c}; fallidas: {f}.')
        acc = (d['k6'] or {}).get('acciones', {})
        if acc:
            lineas.append('- Acciones hechas: ' + ', '.join(f'{k} {v}' for k, v in acc.items()) + f"; sin trabajo disponible: {(d['k6'] or {}).get('acciones_sin_trabajo', 0)}.")
        for nota in n.que_paso(d, m):
            lineas.append(f'- {nota}')
        lineas.append('')
        tc = tabla_conciliacion(d)
        if tc:
            lineas.append('**Comprobación contra la base de datos:**')
            lineas.append('')
            lineas += tc
            lineas.append('')
            lineas.append('_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._')
            lineas.append('')
        te = tabla_escrituras(d)
        if te:
            lineas.append('**Escrituras por paso:**')
            lineas.append('')
            lineas += te
            lineas.append('')
        tab = n.tabla_endpoints(d)
        if tab:
            lineas.append(f'**Endpoints que más tiempo acumulan, {titulo}:**')
            lineas.append('')
            lineas += tab
            lineas.append('')
        tl = n.linea_tiempo_md(d)
        if tl:
            lineas.append('<details><summary>Línea de tiempo cada 15 s</summary>')
            lineas.append('')
            lineas += tl
            lineas.append('')
            lineas.append('</details>')
            lineas.append('')
    lineas.append('> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.')
    salida = NIV / f'INFORME_CARGA_{NIVEL}.md'
    salida.write_text('\n'.join(lineas) + '\n', encoding='utf-8')
    print('\n'.join(lineas))


if __name__ == '__main__':
    configurar(sys.argv[1], sys.argv[2])
    generar()

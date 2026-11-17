import json
import statistics
import sys
from collections import defaultdict
from pathlib import Path

RAIZ = Path(sys.argv[1])
NIV = RAIZ / 'niveles'
LOGS = RAIZ / 'logs'

ORDEN = ['login', 'apoderado', 'mascota', 'cita', 'checkin', 'iniciar', 'consulta', 'diagnostico', 'receta',
         'punto_cobro', 'vincular_equipo', 'abrir_caja',
         'cerrar_consulta', 'finalizar_servicio', 'pago', 'actualizar_mascota', 'cita_extra', 'cancelar_cita']
ETIQUETAS = {
    'login': 'Iniciar sesión', 'apoderado': 'Registrar propietario', 'mascota': 'Registrar mascota',
    'cita': 'Agendar cita', 'checkin': 'Recepción (llegada)', 'iniciar': 'Iniciar atención',
    'consulta': 'Leer la consulta', 'diagnostico': 'Registrar diagnóstico', 'receta': 'Registrar receta',
    'cerrar_consulta': 'Cerrar la consulta', 'finalizar_servicio': 'Finalizar servicio (sin consulta)',
    'pago': 'Cobrar', 'actualizar_mascota': 'Actualizar mascota', 'cita_extra': 'Agendar segunda cita', 'cancelar_cita': 'Cancelar la segunda cita',
    'punto_cobro': 'Caja: crear punto de cobro', 'vincular_equipo': 'Caja: vincular este equipo', 'abrir_caja': 'Caja: abrir la caja',
}


def leer_json(ruta):
    try:
        return json.loads(Path(ruta).read_text(encoding='utf-8'))
    except (OSError, ValueError):
        return None


def leer_pasos(version):
    pasos = defaultdict(list)
    ruta = LOGS / f'k6_escritura_{version}.log'
    try:
        with open(ruta, encoding='utf-8', errors='replace') as f:
            for linea in f:
                if 'PASO|' not in linea:
                    continue
                trozo = linea[linea.index('PASO|'):].split('" source=console')[0].rstrip()
                partes = trozo.split('|', 8)
                if len(partes) < 8:
                    continue
                try:
                    pasos[partes[3]].append({
                        'vu': partes[1], 'iter': partes[2], 'metodo': partes[4], 'http': partes[5],
                        'ms': int(partes[6]), 'ok': partes[7] == 'ok', 'detalle': partes[8] if len(partes) > 8 else '',
                    })
                except ValueError:
                    continue
    except OSError:
        pass
    return pasos


def delta(antes, despues, clave):
    if not antes or not despues:
        return None
    return despues.get(clave, 0) - antes.get(clave, 0)


def analizar(version):
    pasos = leer_pasos(version)
    resumen = leer_json(NIV / f'k6_escritura_{version}.json')
    antes = leer_json(NIV / f'db_antes_{version}.json')
    despues = leer_json(NIV / f'db_despues_{version}.json')
    integridad = leer_json(NIV / f'integridad_{version}.json')
    return {'version': version, 'pasos': pasos, 'resumen': resumen, 'antes': antes, 'despues': despues, 'integridad': integridad}


def correctos(d, paso):
    return sum(1 for p in d['pasos'].get(paso, []) if p['ok'])


def comprobaciones(d):
    filas = []
    if not d['antes'] or not d['despues']:
        return [('Instantáneas de la base de datos', 'no disponibles', '-', False)]
    pares = [
        ('Propietarios nuevos en la base', 'apoderados', correctos(d, 'apoderado')),
        ('Mascotas nuevas en la base', 'mascotas', correctos(d, 'mascota')),
        ('Citas nuevas en la base', 'citas', correctos(d, 'cita') + correctos(d, 'cita_extra')),
        ('Diagnósticos nuevos en la base', 'diagnosticos', correctos(d, 'diagnostico')),
        ('Recetas nuevas en la base', 'recetas', correctos(d, 'receta')),
        ('Pagos nuevos en la base', 'pagos', correctos(d, 'pago')),
        ('Citas completadas nuevas', 'citas_completadas', correctos(d, 'cerrar_consulta') + correctos(d, 'finalizar_servicio')),
    ]
    for nombre, clave, esperado in pares:
        real = delta(d['antes'], d['despues'], clave)
        filas.append((nombre, str(esperado), str(real), real == esperado))
    if d['integridad']:
        for clave, texto in (('mascotas_sin_apoderado', 'Mascotas sin propietario'), ('citas_sin_mascota', 'Citas sin mascota'),
                             ('consultas_sin_cita', 'Consultas sin cita'), ('documentos_repetidos', 'Documentos de propietarios repetidos')):
            valor = d['integridad'].get(clave)
            filas.append((texto + ' (debe ser 0)', '0', str(valor), valor == 0))
    return filas


def tabla_pasos(d):
    filas = ['| Paso | Veces | Correctos | Fallidos | Códigos HTTP | Mediana (ms) |', '|---|---|---|---|---|---|']
    for clave in ORDEN:
        lista = d['pasos'].get(clave, [])
        if not lista:
            continue
        codigos = defaultdict(int)
        for p in lista:
            codigos[p['http']] += 1
        cod = ', '.join(f'{k}×{v}' for k, v in sorted(codigos.items()))
        filas.append(f"| {ETIQUETAS.get(clave, clave)} | {len(lista)} | {sum(1 for p in lista if p['ok'])} | {sum(1 for p in lista if not p['ok'])} | {cod} | {statistics.median(p['ms'] for p in lista):.0f} |")
    return filas


def fallos(d):
    salida = []
    vistos = set()
    for clave in ORDEN:
        for p in d['pasos'].get(clave, []):
            if p['ok']:
                continue
            marca = (clave, p['detalle'][:80])
            if marca in vistos:
                continue
            vistos.add(marca)
            salida.append(f"- **{ETIQUETAS.get(clave, clave)}** (HTTP {p['http']}): `{p['detalle'][:300]}`")
    return salida


def veredicto(d):
    r = d['resumen']
    if not r:
        return 'SIN DATOS (k6 no produjo resumen)'
    cuadra = all(f[3] for f in comprobaciones(d))
    completas = r.get('jornadas_completas', 0)
    incompletas = r.get('jornadas_incompletas', 0)
    if completas > 0 and incompletas == 0 and cuadra:
        return 'FLUJO COMPLETO Y CONSISTENTE'
    if completas > 0:
        return 'FLUJO PARCIAL'
    return 'EL FLUJO NO SE COMPLETA'


antes = analizar('antes')
despues = analizar('despues')
lineas = ['# Fase 1: jornada de clínica con escrituras (validación con 1 usuario)', '']
entorno = RAIZ / 'entorno.txt'
if entorno.exists():
    for linea in entorno.read_text(encoding='utf-8').splitlines():
        if linea.startswith(('antes', 'despues', 'recursos del backend')):
            lineas.append(f'- {linea}')
lineas.append('')
for d, titulo in ((antes, 'ANTES'), (despues, 'DESPUÉS')):
    r = d['resumen'] or {}
    lineas.append(f'## {titulo}: {veredicto(d)}')
    lineas.append('')
    lineas.append(f"- Jornadas completas: {r.get('jornadas_completas', 0)}; incompletas: {r.get('jornadas_incompletas', 0)}.")
    lineas.append('')
    lineas += tabla_pasos(d)
    lineas.append('')
    f = fallos(d)
    if f:
        lineas.append('**Fallos observados (mensajes reales del sistema):**')
        lineas.append('')
        lineas += f
        lineas.append('')
    lineas.append('**Comprobación contra la base de datos** (lo que k6 dice que creó frente a lo que hay en la base):')
    lineas.append('')
    lineas.append('| Comprobación | Esperado | Real | ¿Coincide? |')
    lineas.append('|---|---|---|---|')
    for nombre, esperado, real, coincide in comprobaciones(d):
        lineas.append(f"| {nombre} | {esperado} | {real} | {'sí' if coincide else '**NO**'} |")
    lineas.append('')
lineas.append('> Todos los datos de este informe salen de los archivos que genera la prueba (registro de k6 y consultas a la base). Nada está escrito a mano.')
(RAIZ / 'INFORME_FASE1.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
print('\n'.join(lineas))

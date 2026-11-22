import csv
import gzip
import json
import re
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import informe_carga as c  # noqa: E402

RAIZ = Path(sys.argv[1])
NIV = RAIZ / 'niveles'
NOMBRES = {
    'caos_bd_reinicio': 'Reinicio ordenado de PostgreSQL',
    'caos_bd_kill': 'Caída abrupta de PostgreSQL (SIGKILL) y arranque',
    'caos_bd_parada': 'PostgreSQL detenido 30 s',
    'caos_bd_kill_checkpoint': 'Caída abrupta de PostgreSQL tras un CHECKPOINT',
    'caos_backend_kill': 'Caída abrupta del backend (SIGKILL) y arranque',
}
CUBO_S = 5


def fallo_de_disponibilidad(status, error):
    if error:
        return True
    if status in ('', '0'):
        return True
    return status.isdigit() and int(status) >= 500


def leer_peticiones(ruta):
    filas = []
    try:
        with gzip.open(ruta, 'rt', encoding='utf-8', errors='replace', newline='') as f:
            for fila in csv.DictReader(f):
                if fila.get('metric_name') != 'http_req_duration':
                    continue
                try:
                    t = float(fila['timestamp'])
                except (KeyError, ValueError):
                    continue
                filas.append((t, fila.get('status', ''), fila.get('error', '')))
    except OSError:
        return []
    filas.sort()
    return filas


def f1(x, d=1):
    return 'sin datos' if x is None else f'{x:.{d}f}'.replace('.', ',')


def analizar_caos(pref, nivel):
    et = f'{pref}_{nivel}'
    caos = c.n.leer_json(NIV / f'caos_{et}.json')
    filas = leer_peticiones(NIV / f'k6_{et}.csv.gz')
    c.configurar(RAIZ, nivel)
    d = c.analizar(pref)
    ok, fallidas = c.tasa_exito_escrituras(d)
    r = {'pref': pref, 'datos': c.estado_datos(d), 'escrituras_ok': ok, 'escrituras_fallidas': fallidas, 'caos': caos}
    if not caos or not filas:
        return r
    t_ev = caos['t_evento']
    antes = [x for x in filas if x[0] < t_ev]
    despues = [x for x in filas if x[0] >= t_ev]
    r['pet_antes'] = len(antes)
    r['error_antes_pct'] = sum(1 for x in antes if fallo_de_disponibilidad(x[1], x[2])) / len(antes) * 100 if antes else None
    base = r['error_antes_pct'] or 0.0

    cubos = {}
    for t, st, er in despues:
        i = int((t - t_ev) // CUBO_S)
        n, f = cubos.get(i, (0, 0))
        cubos[i] = (n + 1, f + (1 if fallo_de_disponibilidad(st, er) else 0))
    maximo = max(cubos) if cubos else 0
    recuperado = None
    for i in range(maximo):
        a, b = cubos.get(i, (0, 0)), cubos.get(i + 1, (0, 0))
        if a[0] >= 5 and b[0] >= 5 and a[1] / a[0] * 100 <= base + 2 and b[1] / b[0] * 100 <= base + 2:
            recuperado = i * CUBO_S
            break
    r['recuperacion_s'] = recuperado
    fin = t_ev + (recuperado if recuperado is not None else (maximo + 1) * CUBO_S)
    en_ventana = [x for x in despues if x[0] < fin]
    fallos = [x for x in en_ventana if fallo_de_disponibilidad(x[1], x[2])]
    r['fallidas_en_ventana'] = len(fallos)
    r['peticiones_en_ventana'] = len(en_ventana)
    r['tipos'] = Counter(('sin respuesta' if (x[1] in ('', '0') or x[2]) else x[1]) for x in fallos)
    r['sesiones_perdidas'] = sum(1 for x in despues if x[1] == '401')
    siguientes = [x for x in despues if x[0] < t_ev + 90]
    fallos90 = [x for x in siguientes if fallo_de_disponibilidad(x[1], x[2])]
    r['fallos_90'] = len(fallos90)
    r['peticiones_90'] = len(siguientes)
    r['ultimo_fallo_s'] = (max(x[0] for x in fallos90) - t_ev) if fallos90 else None
    r['tipos_90'] = Counter(('sin respuesta' if (x[1] in ('', '0') or x[2]) else x[1]) for x in fallos90)
    r['401_90'] = sum(1 for x in siguientes if x[1] == '401')
    pre = [x for x in filas if t_ev - 60 <= x[0] < t_ev]
    post_ini = fin + 30
    post = [x for x in filas if post_ini <= x[0] < post_ini + 60]
    r['rps_antes'] = len(pre) / 60 if pre else None
    r['rps_despues'] = len(post) / 60 if post else None
    r['bd_s'] = (caos['t_bd_listo'] - t_ev) if caos.get('t_bd_listo') else None
    r['backend_s'] = (caos['t_backend_ok'] - t_ev) if caos.get('t_backend_ok') else None
    return r


niveles = sorted({int(m.group(1)) for p in NIV.glob('estado_*.json') if (m := re.search(r'_(\d+)\.json$', p.name))})
resultados = []
for nivel in niveles:
    c.configurar(RAIZ, nivel)
    for pref in sorted(set(p.name[len('estado_'):-len(f'_{nivel}.json')] for p in NIV.glob(f'estado_caos_*_{nivel}.json'))):
        resultados.append((nivel, analizar_caos(pref, nivel)))

lineas = ['# Fase 3g: caída y recuperación del servicio', '']
lineas += c.lineas_entorno() + ['']
lineas += [
    '| Usuarios | Escenario | PostgreSQL de nuevo disponible tras (s) | Backend con salud 200 tras (s) | Hasta recuperar el servicio (s) | Peticiones fallidas en esa ventana | Fallos de disponibilidad en los 90 s siguientes | Último fallo tras (s) | Fallos antes del evento (%) | Respuestas 401 tras el evento | Peticiones/s antes → después | Escrituras correctas / fallidas | Datos |',
    '|---|---|---|---|---|---|---|---|---|---|---|---|---|',
]
notas = []
for nivel, r in resultados:
    if 'recuperacion_s' not in r:
        lineas.append(f"| {nivel} | {NOMBRES.get(r['pref'], r['pref'])} | sin datos | sin datos | sin datos | sin datos | sin datos | sin datos | sin datos | sin datos | sin datos | {r['escrituras_ok']} / {r['escrituras_fallidas']} | {r['datos']} |")
        continue
    ventana = f"{r['fallidas_en_ventana']} de {r['peticiones_en_ventana']}"
    lineas.append(
        f"| {nivel} | {NOMBRES.get(r['pref'], r['pref'])} | {f1(r['bd_s'])} | {f1(r['backend_s'])} | "
        f"{f1(r['recuperacion_s'], 0) if r['recuperacion_s'] is not None else 'no se recuperó'} | {ventana} | {r['fallos_90']} de {r['peticiones_90']} | {f1(r['ultimo_fallo_s'])} | {f1(r['error_antes_pct'], 2)} | "
        f"{r['sesiones_perdidas']} | {f1(r['rps_antes'])} → {f1(r['rps_despues'])} | {r['escrituras_ok']} / {r['escrituras_fallidas']} | {r['datos']} |")
    tipos = ', '.join(f'{k}: {v}' for k, v in r['tipos_90'].most_common()) or 'ninguno'
    if r['caos'].get('secuencia_antes'):
        notas.append(f"- {NOMBRES.get(r['pref'], r['pref'])}: secuencia de purchases (último valor | id máximo) antes del evento {r['caos']['secuencia_antes']} y tras recuperar la base {r['caos'].get('secuencia_despues') or 'sin datos'}.")
    notas.append(f"- {NOMBRES.get(r['pref'], r['pref'])}: tipos de fallo en los 90 s siguientes: {tipos}; respuestas 401 en ese lapso: {r['401_90']}.")
lineas += [''] + notas + [
    '',
    '> El evento ocurre a los 150 s del inicio de la carga, con 100 usuarios simulados (muy por debajo de la capacidad). "Hasta recuperar" es el tiempo desde el evento hasta el primer tramo de 5 s, seguido de otro, con tasa de fallos de disponibilidad (sin respuesta o error 5xx) no mayor que la de antes del evento más 2 puntos. Una corrida por escenario; el resultado vale para el laboratorio, no para Render ni Supabase.',
]
(RAIZ / 'RESUMEN_FASE3G_CAOS.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
(RAIZ / 'RESUMEN_FASE3G_CAOS.json').write_text(json.dumps([{**{k: v for k, v in r.items() if k != 'tipos'}, 'nivel': n, 'tipos': dict(r.get('tipos', {}))} for n, r in resultados], indent=1, ensure_ascii=False, default=str), encoding='utf-8')
print('\n'.join(lineas))

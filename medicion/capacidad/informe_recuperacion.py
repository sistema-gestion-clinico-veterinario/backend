import statistics
import sys
from pathlib import Path

import informe_nivel as n

BASE_INI, BASE_FIN = 45, 90
PICO_INI, PICO_FIN = 165, 330
RECUP_INI = 360
FIN_PICO = 330

PICO = 0
CFG = 'C'


def configurar(raiz, pico, cfg):
    global PICO, CFG
    PICO = int(pico)
    CFG = cfg
    n.configurar(raiz, pico, cfg)


def fase(tramos, ini, fin):
    ts = [t for t in tramos if ini <= t['desde_s'] < fin and t['peticiones'] >= 3]
    peticiones = sum(t['peticiones'] for t in ts)
    fallos = sum(t['peticiones'] * t['errores_pct'] / 100 for t in ts)
    p95s = [t['p95_ms'] for t in ts if t['p95_ms'] is not None]
    return {
        'peticiones': peticiones,
        'errores_pct': (fallos / peticiones * 100) if peticiones else None,
        'p95_ms': statistics.median(p95s) if p95s else None,
        'p95_peor_ms': max(p95s) if p95s else None,
        'usuarios': max((t['usuarios'] or 0 for t in ts), default=None),
    }


def evaluar(version):
    datos = n.analizar(version)
    tramos = datos['tramos']
    base = fase(tramos, BASE_INI, BASE_FIN)
    pico = fase(tramos, PICO_INI, PICO_FIN)
    umbral = max(2000, 2 * (base['p95_ms'] or 0))
    degradado = bool(pico['peticiones']) and ((pico['errores_pct'] or 0) >= 5 or (pico['p95_ms'] or 0) >= umbral)
    recuperado_en = None
    posteriores = [t for t in tramos if t['desde_s'] >= FIN_PICO]
    for i in range(len(posteriores) - 1):
        a, b = posteriores[i], posteriores[i + 1]
        if all(t['peticiones'] >= 3 and t['errores_pct'] < 1 and (t['p95_ms'] or 0) < umbral for t in (a, b)):
            recuperado_en = a['desde_s'] - FIN_PICO
            break
    final = fase(tramos, RECUP_INI + 180, RECUP_INI + 300)
    estado = datos['estado'] or {}
    if not datos['k6']:
        veredicto = 'SIN DATOS'
    elif not degradado:
        veredicto = 'NO SE DEGRADÓ'
    elif recuperado_en is not None:
        veredicto = f'SE RECUPERÓ en {max(recuperado_en, 0):.0f} s'
    else:
        veredicto = 'NO SE RECUPERÓ'
    return {
        'datos': datos, 'base': base, 'pico': pico, 'final': final, 'umbral': umbral,
        'degradado': degradado, 'recuperado_en': recuperado_en, 'veredicto': veredicto,
        'estado': estado, 'tramos': tramos,
    }


def etiqueta_fase(t):
    if t < 90:
        return 'base'
    if t < 150:
        return 'subida'
    if t < 330:
        return 'pico'
    if t < 360:
        return 'bajada'
    return 'recuperación'


def linea_tiempo(ev):
    filas = ['| Minuto | Fase | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |', '|---|---|---|---|---|---|']
    for i, t in enumerate(ev['tramos']):
        if i % 2:
            continue
        usuarios = n.fmt(t['usuarios']) if t['usuarios'] is not None else '-'
        filas.append(f"| {t['desde_s'] / 60:.1f} | {etiqueta_fase(t['desde_s'])} | {usuarios} | {t['peticiones']} | {n.fmt(t['p95_ms'])} | {t['errores_pct']:.0f} % |")
    return filas


def generar():
    a, d = evaluar('antes'), evaluar('despues')
    lineas = [f'# Recuperación: pico de {PICO} usuarios durante 3 minutos, configuración {CFG}', '']
    lineas.append(f'- Base de 50 usuarios, subida a {PICO} usuarios en 60 s, pico de 3 minutos, bajada a 50 y 5 minutos de recuperación.')
    lineas.append(f'- Se considera "normal" cuando p95 < {max(a["umbral"], d["umbral"]):.0f} ms y menos de 1 % de errores durante 30 s seguidos.')
    lineas.append('')
    lineas += ['| | ANTES | DESPUÉS |', '|---|---|---|']

    def fila(nombre, fa, fd):
        lineas.append(f'| {nombre} | {fa} | {fd} |')

    fila('Veredicto', a['veredicto'], d['veredicto'])
    fila('Base: p95 (ms) / errores', f"{n.fmt(a['base']['p95_ms'])} / {n.fmt(a['base']['errores_pct'], 1, ' %')}", f"{n.fmt(d['base']['p95_ms'])} / {n.fmt(d['base']['errores_pct'], 1, ' %')}")
    fila('Pico: p95 (ms) / errores', f"{n.fmt(a['pico']['p95_ms'])} / {n.fmt(a['pico']['errores_pct'], 1, ' %')}", f"{n.fmt(d['pico']['p95_ms'])} / {n.fmt(d['pico']['errores_pct'], 1, ' %')}")
    fila('Tras el pico (últimos 2 min): p95 (ms) / errores', f"{n.fmt(a['final']['p95_ms'])} / {n.fmt(a['final']['errores_pct'], 1, ' %')}", f"{n.fmt(d['final']['p95_ms'])} / {n.fmt(d['final']['errores_pct'], 1, ' %')}")
    fila('Salud al terminar (HTTP)', str(a['estado'].get('salud_final_http', 'sin datos')), str(d['estado'].get('salud_final_http', 'sin datos')))
    fila('Contenedor muerto por memoria (OOMKilled)', 'sí' if a['estado'].get('oom_killed') else 'no', 'sí' if d['estado'].get('oom_killed') else 'no')
    fila('Reinicios del contenedor', n.fmt(a['estado'].get('reinicios')), n.fmt(d['estado'].get('reinicios')))
    fila('Errores OutOfMemoryError en el registro', n.fmt(a['datos']['log']['sin_memoria_java']), n.fmt(d['datos']['log']['sin_memoria_java']))
    fila('Avisos de pool de conexiones agotado', n.fmt(a['datos']['log']['pool_conexiones']), n.fmt(d['datos']['log']['pool_conexiones']))
    lineas.append('')
    for titulo, ev in (('ANTES', a), ('DESPUÉS', d)):
        lineas.append(f'## Qué pasó en el {titulo}: {ev["veredicto"]}')
        lineas.append('')
        if ev['veredicto'] == 'SIN DATOS':
            lineas.append('- No se obtuvo el resumen de k6.')
        else:
            if not ev['degradado']:
                lineas.append('- El pico no logró degradar el servicio con esta carga.')
            else:
                lineas.append(f"- Durante el pico el p95 típico fue {n.fmt(ev['pico']['p95_ms'])} ms (peor tramo {n.fmt(ev['pico']['p95_peor_ms'])} ms) y {n.fmt(ev['pico']['errores_pct'], 1, ' %')} de errores.")
                if ev['recuperado_en'] is not None:
                    lineas.append(f"- **Volvió a la normalidad {max(ev['recuperado_en'], 0):.0f} s después de bajar la carga.**")
                else:
                    lineas.append('- **No volvió a la normalidad en los 5 minutos posteriores a bajar la carga.**')
            if ev['estado'].get('oom_killed') or ev['datos']['log']['sin_memoria_java']:
                lineas.append('- Hubo agotamiento de memoria durante la prueba.')
            if str(ev['estado'].get('salud_final_http')) != '200':
                lineas.append(f"- Al terminar el chequeo de salud respondió {ev['estado'].get('salud_final_http')}: el servicio **no quedó funcionando**.")
        lineas.append('')
        lineas.append('<details><summary>Línea de tiempo cada 30 s</summary>')
        lineas.append('')
        lineas += linea_tiempo(ev)
        lineas.append('')
        lineas.append('</details>')
        lineas.append('')
    lineas.append('> Laboratorio con la base de datos en el mismo equipo: sirve para comparar versiones, no es una promesa para producción.')
    salida = n.NIV / f'INFORME_recuperacion_{PICO}_{CFG}.md'
    salida.write_text('\n'.join(lineas) + '\n', encoding='utf-8')
    print('\n'.join(lineas))


if __name__ == '__main__':
    configurar(sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else 'C')
    generar()

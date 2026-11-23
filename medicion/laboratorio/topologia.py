import argparse
import json
import re
import sys
from pathlib import Path


def lista_cpus(texto):
    cpus = set()
    for trozo in (texto or '').replace(' ', '').split(','):
        if not trozo:
            continue
        if '-' in trozo:
            a, b = trozo.split('-', 1)
            cpus.update(range(int(a), int(b) + 1))
        else:
            cpus.add(int(trozo))
    return sorted(cpus)


def leer(ruta):
    try:
        return Path(ruta).read_text(encoding='utf-8').strip()
    except OSError:
        return None


def leer_topologia(raiz):
    cpus = {}
    for d in Path(raiz).iterdir():
        m = re.fullmatch(r'cpu(\d+)', d.name)
        if not m:
            continue
        hermanos = leer(d / 'topology' / 'thread_siblings_list')
        cpus[int(m.group(1))] = {
            'hermanos': lista_cpus(hermanos) if hermanos else None,
            'core_id': leer(d / 'topology' / 'core_id'),
            'package': leer(d / 'topology' / 'physical_package_id'),
        }
    return dict(sorted(cpus.items()))


def analizar(cpus, roles):
    grupos = {}
    for n, c in cpus.items():
        clave = tuple(c['hermanos']) if c['hermanos'] else (n,)
        grupos.setdefault(clave, []).append(n)
    smt = any(len(g) > 1 for g in grupos)
    desconocida = any(c['hermanos'] is None for c in cpus.values())
    por_cpu = {n: i for i, g in enumerate(sorted(grupos)) for n in g}
    asignacion = {}
    for rol, lista in roles.items():
        asignacion[rol] = sorted({por_cpu.get(n) for n in lista if n in por_cpu})
    conflictos = []
    nombres = list(roles)
    for i in range(len(nombres)):
        for j in range(i + 1, len(nombres)):
            comunes = set(asignacion[nombres[i]]) & set(asignacion[nombres[j]])
            if comunes:
                conflictos.append((nombres[i], nombres[j], sorted(comunes)))
    return {'nucleos_fisicos': [list(g) for g in sorted(grupos)], 'smt': smt, 'topologia_desconocida': desconocida,
            'roles': {r: roles[r] for r in roles}, 'nucleo_fisico_de_cada_rol': asignacion,
            'roles_que_comparten_nucleo_fisico': [{'a': a, 'b': b, 'grupos': g} for a, b, g in conflictos]}


def a_markdown(analisis, cpus):
    lineas = ['### Topología de CPU del runner', '']
    lineas.append('| CPU lógica | Núcleo físico (hermanas SMT) | core_id | Paquete |')
    lineas.append('|---|---|---|---|')
    for n, c in cpus.items():
        h = ','.join(str(x) for x in c['hermanos']) if c['hermanos'] else 'no disponible'
        lineas.append(f"| {n} | {h} | {c['core_id'] or '-'} | {c['package'] or '-'} |")
    lineas.append('')
    lineas.append(f"- Núcleos físicos distintos: {len(analisis['nucleos_fisicos'])}; SMT activo: {'sí' if analisis['smt'] else 'no'}.")
    for rol, lista in analisis['roles'].items():
        lineas.append(f"- {rol}: CPU lógica(s) {','.join(str(x) for x in lista) or 'sin fijar'}")
    if analisis['topologia_desconocida']:
        lineas.append('- **No se pudo leer thread_siblings_list de todas las CPU: no se puede decir si comparten núcleo físico.**')
    elif analisis['roles_que_comparten_nucleo_fisico']:
        for c in analisis['roles_que_comparten_nucleo_fisico']:
            lineas.append(f"- **{c['a']} y {c['b']} comparten núcleo físico** (hilos hermanos): el `cpuset` los separa por CPU lógica, no por núcleo físico; pueden interferirse.")
    else:
        lineas.append('- Backend, PostgreSQL y k6 están en núcleos físicos distintos según `thread_siblings_list`.')
    lineas.append('- **Aislamiento físico no demostrado:** el fijado de CPU solo restringe dónde pueden ejecutarse estos procesos; el demonio de Docker, el agente del runner y el núcleo del sistema no están fijados, la caché L3 y el ancho de banda de memoria son compartidos, y el runner es una máquina virtual cuyo mapeo a CPU físicas del anfitrión no es visible desde dentro.')
    return '\n'.join(lineas) + '\n'


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--sys', default='/sys/devices/system/cpu')
    ap.add_argument('--pg', default='')
    ap.add_argument('--backend', default='')
    ap.add_argument('--k6', default='')
    ap.add_argument('--mon', default='')
    ap.add_argument('--json', default='')
    a = ap.parse_args()
    cpus = leer_topologia(a.sys)
    roles = {'k6': lista_cpus(a.k6), 'backend': lista_cpus(a.backend), 'postgres': lista_cpus(a.pg), 'muestreadores': lista_cpus(a.mon)}
    roles = {r: v for r, v in roles.items() if v}
    analisis = analizar(cpus, roles)
    if a.json:
        Path(a.json).write_text(json.dumps(analisis, indent=1), encoding='utf-8')
    sys.stdout.write(a_markdown(analisis, cpus))
    return 0


if __name__ == '__main__':
    sys.exit(main())

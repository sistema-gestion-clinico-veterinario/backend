import argparse
import json
import math
import shlex
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import topologia as tp  # noqa: E402


def nucleos_fisicos(cpus):
    grupos = {}
    for n, c in cpus.items():
        if not c['hermanos']:
            return None
        grupos.setdefault(tuple(c['hermanos']), []).append(n)
    return [sorted(g) for g in sorted(grupos.values(), key=lambda g: min(g))]


def lista(cpus):
    return ','.join(str(x) for x in cpus)


def planificar(cpus, cpus_backend=1.0):
    plan = {'modo': 'topologia', 'aplicado': False, 'motivo': '', 'notas': []}
    grupos = nucleos_fisicos(cpus)
    if grupos is None:
        plan['motivo'] = 'no se pudo leer thread_siblings_list de todas las CPU: se conserva el fijado anterior'
        return plan
    if len(cpus) < 4 or len(grupos) < 2:
        plan['motivo'] = f'hay {len(cpus)} CPU lógicas en {len(grupos)} núcleos físicos: no alcanzan para separar backend, PostgreSQL y k6; se conserva el fijado anterior'
        return plan
    necesarias = math.ceil(cpus_backend)
    ultimo = grupos[-1]
    if necesarias > len(ultimo):
        plan['motivo'] = f'el backend necesita {necesarias} CPU lógicas y un núcleo físico solo tiene {len(ultimo)}: se conserva el fijado anterior'
        return plan

    backend = ultimo[:necesarias]
    reservada = [x for x in ultimo if x not in backend]
    if len(grupos) >= 3:
        pg = [grupos[-2][0]]
        k6 = [grupos[0][0]]
        mon = [grupos[0][-1]] if len(grupos[0]) > 1 else k6
        compartidos = []
    else:
        resto = grupos[0]
        pg = [resto[0]]
        k6 = [resto[-1]]
        mon = k6
        compartidos = [('PostgreSQL', 'k6')] if len(resto) > 1 else [('PostgreSQL', 'k6', 'los muestreadores en la misma CPU lógica')]
    plan.update({'aplicado': True, 'backend': backend, 'postgres': pg, 'k6': k6, 'muestreadores': mon,
                 'cpu_reservada_sin_trabajo_fijado': reservada, 'nucleos_fisicos': grupos})
    plan['notas'] = [
        f'El backend queda en la CPU lógica {lista(backend)}, y su hilo hermano {lista(reservada) or "(ninguno)"} no recibe trabajo fijado: ningún proceso del experimento comparte con él su núcleo físico.',
        'Esa CPU hermana no está bloqueada: el demonio de Docker, el agente del runner y el núcleo del sistema pueden usarla.',
    ]
    if compartidos:
        plan['notas'].append('PostgreSQL, k6 y los muestreadores comparten el otro núcleo físico (hilos hermanos): la interferencia entre ellos no se elimina, solo se saca al backend de ese núcleo.')
    plan['notas'].append('El aislamiento físico completo sigue sin demostrarse: el runner es una máquina virtual y la caché L3 y la memoria se comparten.')
    return plan


def a_shell(plan):
    if not plan['aplicado']:
        return f"AFINIDAD_APLICADA=no\nAFINIDAD_MOTIVO={shlex.quote(plan['motivo'])}\n"
    return (
        'AFINIDAD_APLICADA=si\n'
        f"PIN_BACK={shlex.quote('--cpuset-cpus ' + lista(plan['backend']))}\n"
        f"PIN_PG={shlex.quote('--cpuset-cpus ' + lista(plan['postgres']))}\n"
        f"K6_NUCLEOS={shlex.quote(lista(plan['k6']))}\n"
        f"PIN_MON={shlex.quote('taskset -c ' + lista(plan['muestreadores']))}\n"
    )


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--sys', default='/sys/devices/system/cpu')
    ap.add_argument('--cpus', type=float, default=1.0)
    ap.add_argument('--json', default='')
    a = ap.parse_args()
    plan = planificar(tp.leer_topologia(a.sys), a.cpus)
    if a.json:
        Path(a.json).write_text(json.dumps(plan, indent=1, ensure_ascii=False), encoding='utf-8')
    sys.stdout.write(a_shell(plan))
    return 0


if __name__ == '__main__':
    sys.exit(main())

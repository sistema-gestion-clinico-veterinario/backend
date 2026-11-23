import argparse
import subprocess
import sys
import time
from pathlib import Path

ORDEN = ['ts', 'duracion_ms', 'total', 'activas', 'inactivas', 'inactivas_en_transaccion', 'esperan_lock', 'esperan_io', 'esperan_cliente', 'tx_mas_vieja_s', 'xact_commit', 'xact_rollback']


def consulta(base):
    return (
        "SELECT count(*), count(*) FILTER (WHERE state = 'active'), count(*) FILTER (WHERE state = 'idle'), "
        "count(*) FILTER (WHERE state = 'idle in transaction'), count(*) FILTER (WHERE wait_event_type = 'Lock'), "
        "count(*) FILTER (WHERE wait_event_type = 'IO'), count(*) FILTER (WHERE wait_event_type = 'Client'), "
        "coalesce(round(extract(epoch FROM max(now() - xact_start))::numeric, 1), 0), "
        f"(SELECT xact_commit FROM pg_stat_database WHERE datname = '{base}'), (SELECT xact_rollback FROM pg_stat_database WHERE datname = '{base}') "
        f"FROM pg_stat_activity WHERE datname = '{base}' AND application_name NOT LIKE 'psql%'"
    )


def parsear(salida):
    for linea in salida.splitlines():
        partes = linea.strip().split(',')
        if len(partes) == 10:
            try:
                return [float(x) if '.' in x else int(x) for x in partes]
            except ValueError:
                return None
    return None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('salida')
    ap.add_argument('--base', required=True)
    ap.add_argument('--contenedor', default='sv-pg')
    ap.add_argument('--periodo', type=float, default=2.0)
    ap.add_argument('--docker', default='docker')
    ap.add_argument('--max-muestras', type=int, default=0)
    a = ap.parse_args()
    salida = Path(a.salida)
    salida.parent.mkdir(parents=True, exist_ok=True)
    muestras = 0
    with open(salida, 'w', encoding='utf-8', buffering=1) as f:
        f.write(','.join(ORDEN) + '\n')
        while True:
            inicio = time.perf_counter()
            try:
                p = subprocess.run([a.docker, 'exec', a.contenedor, 'psql', '-U', 'postgres', '-d', a.base, '-tAF,', '-c', consulta(a.base)],
                                   capture_output=True, text=True, timeout=15)
                valores = parsear(p.stdout)
            except (subprocess.TimeoutExpired, OSError):
                valores = None
            duracion = round((time.perf_counter() - inicio) * 1000)
            if valores:
                f.write(','.join(str(x) for x in [int(time.time()), duracion] + valores) + '\n')
            muestras += 1
            if a.max_muestras and muestras >= a.max_muestras:
                break
            time.sleep(max(0.0, a.periodo - (time.perf_counter() - inicio)))
    return 0


if __name__ == '__main__':
    sys.exit(main())

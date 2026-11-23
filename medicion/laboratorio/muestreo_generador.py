import argparse
import os
import sys
import time
from pathlib import Path


def leer(ruta):
    try:
        return Path(ruta).read_text(encoding='utf-8', errors='replace')
    except OSError:
        return None


def buscar_pid(proc, nombre):
    try:
        entradas = sorted(Path(proc).iterdir(), key=lambda p: p.name)
    except OSError:
        return None
    for d in entradas:
        if not d.name.isdigit():
            continue
        comm = leer(d / 'comm')
        if comm is not None and comm.strip() == nombre:
            return int(d.name)
    return None


def ticks_proceso(proc, pid):
    texto = leer(Path(proc) / str(pid) / 'stat')
    if not texto:
        return None
    resto = texto[texto.rfind(')') + 2:].split()
    try:
        return int(resto[11]) + int(resto[12]), int(resto[17])
    except (IndexError, ValueError):
        return None


def estado_proceso(proc, pid):
    campos = {}
    for linea in (leer(Path(proc) / str(pid) / 'status') or '').splitlines():
        if ':' in linea:
            clave, valor = linea.split(':', 1)
            campos[clave.strip()] = valor.strip()
    return campos


def kb(valor):
    try:
        return int(str(valor).split()[0])
    except (IndexError, ValueError):
        return ''


def cpus_sistema(proc):
    salida = {}
    for linea in (leer(Path(proc) / 'stat') or '').splitlines():
        partes = linea.split()
        if not partes or not partes[0].startswith('cpu'):
            continue
        try:
            numeros = [int(x) for x in partes[1:9]]
        except ValueError:
            continue
        total = sum(numeros)
        ocioso = numeros[3] + numeros[4]
        salida[partes[0]] = (total, total - ocioso)
    return salida


def porcentaje(ahora, antes):
    if not antes or ahora[0] <= antes[0]:
        return None
    return (ahora[1] - antes[1]) / (ahora[0] - antes[0]) * 100


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('salida')
    ap.add_argument('--nombre', default='k6')
    ap.add_argument('--periodo', type=float, default=2.0)
    ap.add_argument('--proc', default='/proc')
    ap.add_argument('--hz', type=int, default=0)
    ap.add_argument('--max-muestras', type=int, default=0)
    ap.add_argument('--espera-proceso-s', type=float, default=120.0)
    a = ap.parse_args()
    hz = a.hz or os.sysconf('SC_CLK_TCK')
    salida = Path(a.salida)
    salida.parent.mkdir(parents=True, exist_ok=True)

    pid = None
    limite = time.monotonic() + a.espera_proceso_s
    while pid is None and time.monotonic() < limite:
        pid = buscar_pid(a.proc, a.nombre)
        if pid is None:
            time.sleep(0.2)
    if pid is None:
        salida.write_text('sin_proceso\n', encoding='utf-8')
        return 0

    estado = estado_proceso(a.proc, pid)
    Path(str(salida) + '.afinidad').write_text(
        f"pid={pid}\nCpus_allowed_list={estado.get('Cpus_allowed_list', '')}\nMems_allowed_list={estado.get('Mems_allowed_list', '')}\nhz={hz}\n",
        encoding='utf-8')

    antes_t = ticks_proceso(a.proc, pid)
    antes_sis = cpus_sistema(a.proc)
    antes_reloj = time.monotonic()
    muestras = 0
    with open(salida, 'w', encoding='utf-8', buffering=1) as f:
        f.write('ts,cpu_k6_pct,rss_kb,hwm_kb,hilos,cpu_sistema_pct,cpus_logicas\n')
        while True:
            time.sleep(a.periodo)
            t = ticks_proceso(a.proc, pid)
            if t is None:
                break
            ahora_reloj = time.monotonic()
            sis = cpus_sistema(a.proc)
            if antes_t is not None and ahora_reloj > antes_reloj:
                cpu = (t[0] - antes_t[0]) / hz / (ahora_reloj - antes_reloj) * 100
            else:
                cpu = ''
            estado = estado_proceso(a.proc, pid)
            por_cpu = []
            for nombre, valor in sorted(sis.items(), key=lambda kv: (kv[0] != 'cpu', len(kv[0]), kv[0])):
                p = porcentaje(valor, antes_sis.get(nombre))
                if nombre != 'cpu' and p is not None:
                    por_cpu.append(f"{nombre[3:]}:{p:.1f}")
            total = porcentaje(sis['cpu'], antes_sis.get('cpu')) if 'cpu' in sis else None
            f.write(f"{int(time.time())},{'' if cpu == '' else f'{cpu:.1f}'},{kb(estado.get('VmRSS'))},{kb(estado.get('VmHWM'))},{t[1]},"
                    f"{'' if total is None else f'{total:.1f}'},{'|'.join(por_cpu)}\n")
            antes_t, antes_sis, antes_reloj = t, sis, ahora_reloj
            muestras += 1
            if a.max_muestras and muestras >= a.max_muestras:
                break
    return 0


if __name__ == '__main__':
    sys.exit(main())

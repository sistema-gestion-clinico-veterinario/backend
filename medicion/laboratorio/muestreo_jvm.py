import argparse
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import hsperfdata  # noqa: E402

ORDEN = ['ts', 'duracion_ms', 'fuente', 'heap_usado_kb', 'heap_comprometido_kb', 'eden_usado_kb', 'viejo_usado_kb',
         'metaspace_usado_kb', 'metaspace_comprometido_kb', 'ygc', 'fgc', 'gct_s', 'cont_actual_kb', 'cont_anon_kb',
         'cont_file_kb', 'cont_pico_kb', 'rss_kb', 'rss_anon_kb', 'rss_file_kb', 'hilos_so', 'kstack_kb', 'slab_kb', 'sock_kb', 'hilos_java', 'clases_comprimidas_usado_kb']

MARCA = b'\n=====SVLAB=====\n'

SCRIPT_CONTENEDOR = (
    "for f in /tmp/hsperfdata_*/1; do cat \"$f\" 2>/dev/null; done; printf '\n=====SVLAB=====\n'; "
    "echo '--actual'; cat /sys/fs/cgroup/memory.current 2>/dev/null; "
    "echo '--stat'; grep -E '^(anon|file|kernel_stack|slab|sock) ' /sys/fs/cgroup/memory.stat 2>/dev/null; "
    "echo '--status'; grep -E '^(VmRSS|RssAnon|RssFile|Threads):' /proc/1/status 2>/dev/null; "
    "echo '--pico'; cat /sys/fs/cgroup/memory.peak 2>/dev/null; true"
)


def a_numero(texto):
    try:
        return float(texto)
    except (TypeError, ValueError):
        return None


def a_kb(valor):
    return '' if valor is None else round(valor / 1024)


def parsear_muestra(bruto):
    datos, marca, texto = bruto.rpartition(MARCA)
    if not marca:
        datos, texto = b'', bruto.decode('utf-8', errors='replace')
    else:
        texto = texto.decode('utf-8', errors='replace')
    secciones = {}
    actual = None
    for linea in texto.splitlines():
        if linea.startswith('--'):
            actual = linea[2:].strip()
            secciones[actual] = []
        elif linea.strip() and actual:
            secciones[actual].append(linea.strip())
    r = {k: '' for k in ORDEN}
    heap = hsperfdata.resumen_heap(hsperfdata.leer_contadores(datos))
    if heap and heap['heap_usado_b'] is not None:
        r['fuente'] = 'hsperfdata'
        r['heap_usado_kb'] = a_kb(heap['heap_usado_b'])
        r['heap_comprometido_kb'] = a_kb(heap['heap_comprometido_b'])
        r['eden_usado_kb'] = a_kb(heap['eden_usado_b'])
        r['viejo_usado_kb'] = a_kb(heap['viejo_usado_b'])
        r['metaspace_usado_kb'] = a_kb(heap['metaspace_usado_b'])
        r['metaspace_comprometido_kb'] = a_kb(heap['metaspace_comprometido_b'])
        r['ygc'] = '' if heap['ygc'] is None else heap['ygc']
        r['fgc'] = '' if heap['fgc'] is None else heap['fgc']
        r['gct_s'] = '' if heap['gct_s'] is None else round(heap['gct_s'], 4)
        r['hilos_java'] = '' if heap['hilos_java'] is None else heap['hilos_java']
        r['clases_comprimidas_usado_kb'] = a_kb(heap['clases_comprimidas_usado_b'])
    else:
        r['fuente'] = 'ninguna'
    actual_b = a_numero((secciones.get('actual') or [''])[0])
    r['cont_actual_kb'] = a_kb(actual_b)
    for linea in secciones.get('stat', []):
        partes = linea.split()
        if len(partes) == 2 and a_numero(partes[1]) is not None:
            clave = {'anon': 'cont_anon_kb', 'file': 'cont_file_kb', 'kernel_stack': 'kstack_kb', 'slab': 'slab_kb', 'sock': 'sock_kb'}.get(partes[0])
            if clave:
                r[clave] = a_kb(float(partes[1]))
    for linea in secciones.get('status', []):
        partes = linea.replace(':', '').split()
        if len(partes) >= 2 and a_numero(partes[1]) is not None:
            if partes[0] == 'Threads':
                r['hilos_so'] = int(float(partes[1]))
            else:
                clave = {'VmRSS': 'rss_kb', 'RssAnon': 'rss_anon_kb', 'RssFile': 'rss_file_kb'}.get(partes[0])
                if clave:
                    r[clave] = int(float(partes[1]))
    r['cont_pico_kb'] = a_kb(a_numero((secciones.get('pico') or [''])[0]))
    return r


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('salida')
    ap.add_argument('--contenedor', required=True)
    ap.add_argument('--periodo', type=float, default=5.0)
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
                p = subprocess.run([a.docker, 'exec', a.contenedor, 'sh', '-c', SCRIPT_CONTENEDOR], capture_output=True, timeout=20)
                bruto = p.stdout
            except (subprocess.TimeoutExpired, OSError):
                bruto = b''
            duracion = round((time.perf_counter() - inicio) * 1000)
            if bruto.strip():
                r = parsear_muestra(bruto)
                r['ts'] = int(time.time())
                r['duracion_ms'] = duracion
                f.write(','.join(str(r[k]) for k in ORDEN) + '\n')
            muestras += 1
            if a.max_muestras and muestras >= a.max_muestras:
                break
            time.sleep(max(0.0, a.periodo - (time.perf_counter() - inicio)))
    return 0


if __name__ == '__main__':
    sys.exit(main())

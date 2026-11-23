import csv
import sys
from pathlib import Path


def leer_jstat(texto):
    lineas = [x for x in texto.splitlines() if x.strip()]
    for i, linea in enumerate(lineas):
        if linea.split()[0] == 'S0C' and i + 1 < len(lineas):
            salida = {}
            for nombre, valor in zip(linea.split(), lineas[i + 1].split()):
                try:
                    salida[nombre] = float(valor.replace(',', '.'))
                except ValueError:
                    salida[nombre] = None
            return salida
    return None


def numero(texto):
    try:
        return float(texto)
    except (TypeError, ValueError):
        return None


def comparar(j, fila):
    def suma(*nombres):
        valores = [j.get(n) for n in nombres]
        return None if any(v is None for v in valores) else sum(valores)

    pares = [
        ('Heap comprometido (KB)', suma('S0C', 'S1C', 'EC', 'OC'), numero(fila.get('heap_comprometido_kb')), 2),
        ('Heap usado (KB)', suma('S0U', 'S1U', 'EU', 'OU'), numero(fila.get('heap_usado_kb')), 2),
        ('Metaspace usado (KB)', j.get('MU'), numero(fila.get('metaspace_usado_kb')), 2),
        ('Metaspace comprometido (KB)', j.get('MC'), numero(fila.get('metaspace_comprometido_kb')), 2),
        ('Recolecciones young', j.get('YGC'), numero(fila.get('ygc')), 0),
        ('Recolecciones full', j.get('FGC'), numero(fila.get('fgc')), 0),
        ('Tiempo de GC (s)', j.get('GCT'), numero(fila.get('gct_s')), 0.01),
    ]
    return [(nombre, a, b, a is not None and b is not None and abs(a - b) <= tol) for nombre, a, b, tol in pares]


def main():
    jstat = leer_jstat(Path(sys.argv[1]).read_text(encoding='utf-8'))
    with open(sys.argv[2], encoding='utf-8', newline='') as f:
        filas = list(csv.DictReader(f))
    if not jstat or not filas:
        print('sin datos para comparar')
        return 1
    resultado = comparar(jstat, filas[-1])
    print('| Magnitud | jstat -gc | Lector de contadores | ¿Coincide? |')
    print('|---|---|---|---|')
    for nombre, a, b, ok in resultado:
        print(f"| {nombre} | {a} | {b} | {'sí' if ok else 'NO'} |")
    return 0 if all(r[3] for r in resultado) else 1


if __name__ == '__main__':
    sys.exit(main())

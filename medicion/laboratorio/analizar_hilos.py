import gzip
import re
import sys
from pathlib import Path

RE_HILO = re.compile(r'^"([^"]+)"')
RE_ESTADO = re.compile(r'java\.lang\.Thread\.State: (\w+)')
CATEGORIAS = [
    ('esperando_pool', ('HikariPool.getConnection', 'HikariDataSource.getConnection', 'ConcurrentBag.borrow')),
    ('contrasena_bcrypt', ('BCrypt', 'BCryptPasswordEncoder')),
    ('correo_smtp', ('SMTPTransport', 'jakarta.mail', 'angus.mail')),
    ('leyendo_de_la_base', ('PGStream.receive', 'PgConnection', 'QueryExecutorImpl', 'socketRead0')),
    ('esperando_monitor', ('waiting to lock',)),
]


def bloques(texto):
    actual = []
    for linea in texto.splitlines():
        if linea.startswith('"'):
            if actual:
                yield actual
            actual = [linea]
        elif actual:
            if not linea.strip():
                yield actual
                actual = []
            else:
                actual.append(linea)
    if actual:
        yield actual


def clasificar(bloque):
    texto = '\n'.join(bloque)
    for nombre, marcas in CATEGORIAS:
        if any(m in texto for m in marcas):
            return nombre
    return 'otros'


def analizar_volcado(texto):
    r = {'hilos': 0, 'hilos_de_peticiones': 0, 'estados': {}, 'categorias': {}, 'esperan_pool_con_transaccion_abierta': 0, 'esperan_pool_sin_transaccion_previa': 0,
         'con_vista_abierta_osiv': 0, 'interbloqueo_java': 'Found one Java-level deadlock' in texto, 'ejemplos': {}}
    for b in bloques(texto):
        m = RE_HILO.match(b[0])
        if not m:
            continue
        nombre = m.group(1)
        r['hilos'] += 1
        estado = RE_ESTADO.search('\n'.join(b))
        r['estados'][estado.group(1) if estado else '?'] = r['estados'].get(estado.group(1) if estado else '?', 0) + 1
        if 'exec' not in nombre:
            continue
        r['hilos_de_peticiones'] += 1
        categoria = clasificar(b)
        r['categorias'][categoria] = r['categorias'].get(categoria, 0) + 1
        cuerpo = '\n'.join(b)
        if categoria == 'esperando_pool':
            transacciones = cuerpo.count('TransactionAspectSupport.invokeWithinTransaction')
            if transacciones >= 2:
                r['esperan_pool_con_transaccion_abierta'] += 1
            else:
                r['esperan_pool_sin_transaccion_previa'] += 1
        if 'OpenEntityManagerInView' in cuerpo:
            r['con_vista_abierta_osiv'] += 1
        if categoria not in r['ejemplos']:
            r['ejemplos'][categoria] = [x.strip() for x in b[2:14]]
    return r


def leer(ruta):
    ruta = Path(ruta)
    if ruta.suffix == '.gz':
        return gzip.open(ruta, 'rt', encoding='utf-8', errors='replace').read()
    return ruta.read_text(encoding='utf-8', errors='replace')


def a_markdown(analisis_por_archivo):
    L = ['| Volcado | Hilos | Hilos de peticiones | Esperando una conexión del pool | …de ellos ya con una transacción abierta | Leyendo de la base | Bcrypt | Correo | Otros | Interbloqueo de monitores |', '|---|---|---|---|---|---|---|---|---|---|']
    for nombre, r in analisis_por_archivo:
        c = r['categorias']
        L.append(f"| {nombre} | {r['hilos']} | {r['hilos_de_peticiones']} | {c.get('esperando_pool', 0)} | {r['esperan_pool_con_transaccion_abierta']} | {c.get('leyendo_de_la_base', 0)} | "
                 f"{c.get('contrasena_bcrypt', 0)} | {c.get('correo_smtp', 0)} | {c.get('otros', 0)} | {'sí' if r['interbloqueo_java'] else 'no'} |")
    return L


def main():
    archivos = sorted(Path(sys.argv[1]).glob('hilos_*'))
    res = [(a.name, analizar_volcado(leer(a))) for a in archivos]
    sys.stdout.write('\n'.join(a_markdown(res)) + '\n')
    return 0


if __name__ == '__main__':
    sys.exit(main())

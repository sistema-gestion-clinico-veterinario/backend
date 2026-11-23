import struct

MAGIC = 0xCAFEC0C0
TIPOS = {ord('J'): 'q', ord('I'): 'i', ord('S'): 'h', ord('D'): 'd', ord('F'): 'f'}


def leer_contadores(datos):
    if not datos or len(datos) < 32:
        return None
    orden = '<' if datos[4] == 1 else '>'
    magia = struct.unpack_from(orden + 'I', datos, 0)[0]
    if magia != MAGIC and struct.unpack_from('>I', datos, 0)[0] != MAGIC:
        return None
    entrada, cantidad = struct.unpack_from(orden + 'ii', datos, 24)
    contadores = {}
    for _ in range(cantidad):
        if entrada + 20 > len(datos):
            break
        largo, nombre_pos, vector, tipo, _flags, _unidades, _variab, dato_pos = struct.unpack_from(orden + 'iiibbbbi', datos, entrada)
        if largo <= 0:
            break
        ini = entrada + nombre_pos
        fin = datos.find(b'\0', ini, entrada + largo)
        nombre = datos[ini:fin if fin >= 0 else entrada + largo].decode('ascii', errors='replace')
        if vector == 0 and tipo in TIPOS:
            fmt = TIPOS[tipo]
            if entrada + dato_pos + struct.calcsize(fmt) <= len(datos):
                contadores[nombre] = struct.unpack_from(orden + fmt, datos, entrada + dato_pos)[0]
        elif tipo == ord('B') and vector > 0 and entrada + dato_pos + vector <= len(datos):
            crudo = datos[entrada + dato_pos:entrada + dato_pos + vector]
            contadores[nombre] = crudo.split(b'\0', 1)[0].decode('utf-8', errors='replace')
        entrada += largo
    return contadores


def espacios_del_heap(c):
    usado = comprometido = 0
    encontrados = False
    for generacion in (0, 1):
        for espacio in range(3):
            u = c.get(f'sun.gc.generation.{generacion}.space.{espacio}.used')
            cap = c.get(f'sun.gc.generation.{generacion}.space.{espacio}.capacity')
            if u is not None and cap is not None:
                usado += u
                comprometido += cap
                encontrados = True
    return (usado, comprometido) if encontrados else (None, None)


def resumen_heap(c):
    if not c:
        return None
    usado, comprometido = espacios_del_heap(c)
    frecuencia = c.get('sun.os.hrt.frequency') or 0
    tiempo_gc = sum(c.get(f'sun.gc.collector.{i}.time') or 0 for i in (0, 1))
    return {
        'heap_usado_b': usado,
        'heap_comprometido_b': comprometido,
        'eden_usado_b': c.get('sun.gc.generation.0.space.0.used'),
        'viejo_usado_b': c.get('sun.gc.generation.1.space.0.used'),
        'metaspace_usado_b': c.get('sun.gc.metaspace.used'),
        'metaspace_comprometido_b': c.get('sun.gc.metaspace.capacity'),
        'ygc': c.get('sun.gc.collector.0.invocations'),
        'fgc': c.get('sun.gc.collector.1.invocations'),
        'gct_s': (tiempo_gc / frecuencia) if frecuencia else None,
        'hilos_java': c.get('java.threads.live'),
        'clases_comprimidas_usado_b': c.get('sun.gc.compressedclassspace.used'),
    }

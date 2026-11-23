import sys
import unittest
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import analizar_hilos as ah  # noqa: E402
import muestreo_pg as mp  # noqa: E402

TRANSACCION = 'at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:374)'


def hilo(nombre, estado, marcas, transacciones=1, osiv=False):
    lineas = [f'"{nombre}" #45 [123] daemon prio=5 os_prio=0 cpu=10ms elapsed=100s tid=0x1 nid=123 waiting on condition  [0x1]', f'   java.lang.Thread.State: {estado}',
              '\tat jdk.internal.misc.Unsafe.park(Native Method)']
    lineas += [f'\tat {m}' for m in marcas]
    lineas += ['\t' + TRANSACCION] * transacciones
    if osiv:
        lineas.append('\tat org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor.preHandle(X.java:1)')
    return '\n'.join(lineas) + '\n'


VOLCADO = (
    'Full thread dump OpenJDK 64-Bit Server VM (21.0.13.1+1 mixed mode):\n\n'
    + hilo('http-nio-8080-exec-1', 'TIMED_WAITING', ['com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:181)'], transacciones=2) + '\n'
    + hilo('http-nio-8080-exec-2', 'TIMED_WAITING', ['com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:181)'], transacciones=1) + '\n'
    + hilo('http-nio-8080-exec-3', 'RUNNABLE', ['org.postgresql.core.PGStream.receive(PGStream.java:10)']) + '\n'
    + hilo('http-nio-8080-exec-4', 'RUNNABLE', ['org.springframework.security.crypto.bcrypt.BCrypt.hashpw(BCrypt.java:5)']) + '\n'
    + hilo('http-nio-8080-exec-5', 'RUNNABLE', ['veterinaria.Otro.hacer(X.java:1)'], osiv=True) + '\n'
    + hilo('GC Thread#0', 'RUNNABLE', ['x.y.Z.run(Z.java:1)']) + '\n'
    + 'JNI global refs: 20\n'
)


class Hilos(unittest.TestCase):
    def test_clasifica_hilos_de_peticiones(self):
        r = ah.analizar_volcado(VOLCADO)
        self.assertEqual(r['hilos'], 6)
        self.assertEqual(r['hilos_de_peticiones'], 5)
        self.assertEqual(r['categorias'], {'esperando_pool': 2, 'leyendo_de_la_base': 1, 'contrasena_bcrypt': 1, 'otros': 1})

    def test_huella_de_espera_de_segunda_conexion(self):
        r = ah.analizar_volcado(VOLCADO)
        self.assertEqual(r['esperan_pool_con_transaccion_abierta'], 1)
        self.assertEqual(r['esperan_pool_sin_transaccion_previa'], 1)
        self.assertEqual(r['con_vista_abierta_osiv'], 1)
        self.assertFalse(r['interbloqueo_java'])
        self.assertTrue(ah.analizar_volcado(VOLCADO + 'Found one Java-level deadlock:\n')['interbloqueo_java'])

    def test_tabla(self):
        md = '\n'.join(ah.a_markdown([('hilos_t200.txt', ah.analizar_volcado(VOLCADO))]))
        self.assertIn('| hilos_t200.txt | 6 | 5 | 2 | 1 |', md)


class ActividadDePostgres(unittest.TestCase):
    def test_parsea_una_fila(self):
        self.assertEqual(mp.parsear('10,4,5,1,0,0,0,12.5,1000,3\n'), [10, 4, 5, 1, 0, 0, 0, 12.5, 1000, 3])

    def test_rechaza_filas_incompletas_o_vacias(self):
        self.assertIsNone(mp.parsear('10,4,5\n'))
        self.assertIsNone(mp.parsear('10,4,5,1,0,0,0,12.5,,3\n'))
        self.assertIsNone(mp.parsear(''))

    def test_consulta_excluye_al_propio_muestreador(self):
        q = mp.consulta('despues')
        self.assertIn("application_name NOT LIKE 'psql%'", q)
        self.assertIn("datname = 'despues'", q)
        self.assertEqual(len(mp.ORDEN), 12)


if __name__ == '__main__':
    unittest.main()

import sys
import unittest
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import analizar_hilos as ah  # noqa: E402
import series_temporales as st  # noqa: E402
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


class RegistroDeHikari(unittest.TestCase):
    def test_lee_las_estadisticas_reales_de_hikari(self):
        import tempfile
        pie = 'com.zaxxer.hikari.pool.HikariPool : HikariPool-1 - '
        lineas = '\n'.join([
            '2026-11-23T19:56:00.100Z DEBUG 1 --- [vargasvet] [housekeeper] ' + pie + 'Before cleanup stats (total=20, active=3, idle=17, waiting=0)',
            '2026-11-23T19:56:00.101Z DEBUG 1 --- [vargasvet] [housekeeper] ' + pie + 'After cleanup  stats (total=20, active=3, idle=17, waiting=0)',
            '2026-11-23T19:57:12.930Z ERROR 1 --- [vargasvet] [exec-8] o.h.e.j.s.SqlExceptionHelper : HikariPool-1 - Connection is not available, request timed out after 30000ms (total=10, active=10, idle=0, waiting=199)',
            '2026-11-23T19:57:13.000Z DEBUG 1 --- [vargasvet] [exec-9] ' + pie + 'Timeout failure stats (total=10, active=10, idle=0, waiting=198)',
            '2026-11-23T19:57:14.000Z ERROR 1 --- [vargasvet] [exec-9] x : java.lang.OutOfMemoryError: Java heap space',
        ]) + '\n'
        with tempfile.TemporaryDirectory() as t:
            ruta = Path(t) / 'backend.log'
            ruta.write_text(lineas, encoding='utf-8')
            ev = st.leer_log(ruta)
        self.assertEqual([h['tipo'] for h in ev['hikari']], ['Before cleanup', 'After cleanup', 'Tiempo agotado', 'Timeout failure'])
        self.assertEqual([h['esperando'] for h in ev['hikari']], [0, 0, 199, 198])
        self.assertEqual(ev['hikari'][0]['activas'], 3)
        self.assertEqual(len(ev['sin_conexion']), 1)
        self.assertEqual(len(ev['oom']), 1)


if __name__ == '__main__':
    unittest.main()

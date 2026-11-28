package veterinaria.vargasvet.config;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import veterinaria.vargasvet.BaseDesechableDePruebas;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "FLYWAY_TEST_URL", matches = ".+")
class BloqueoDeTareasPostgresTest {

    private static final String BASE_PROPIA = "prueba_bloqueo_tareas";
    private static final String MIGRACION = "db/migration/V141__Bloqueo_Tareas_Programadas.sql";

    private static String urlPropia;
    private static String usuario;
    private static String clave;

    @BeforeAll
    static void prepararBase() throws Exception {
        String urlIndicada = System.getenv("FLYWAY_TEST_URL");
        usuario = System.getenv().getOrDefault("FLYWAY_TEST_USER", "postgres");
        clave = System.getenv().getOrDefault("FLYWAY_TEST_PASSWORD", "");
        BaseDesechableDePruebas.exigir(urlIndicada);
        urlPropia = urlIndicada.replaceFirst("/[^/?]+(\\?|$)", "/" + BASE_PROPIA + "$1");
        BaseDesechableDePruebas.exigir(urlPropia);
        try (Connection conexion = DriverManager.getConnection(urlIndicada, usuario, clave);
             Statement sentencia = conexion.createStatement();
             ResultSet existente = sentencia.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + BASE_PROPIA + "'")) {
            if (!existente.next()) {
                try (Statement crear = conexion.createStatement()) {
                    crear.execute("CREATE DATABASE " + BASE_PROPIA);
                }
            }
        }
        String sql;
        try (var entrada = BloqueoDeTareasPostgresTest.class.getClassLoader().getResourceAsStream(MIGRACION)) {
            sql = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Connection conexion = DriverManager.getConnection(urlPropia, usuario, clave);
             Statement sentencia = conexion.createStatement()) {
            sentencia.execute("DROP TABLE IF EXISTS shedlock");
            sentencia.execute(sql);
        }
    }

    @BeforeEach
    void limpiar() {
        new JdbcTemplate(new DriverManagerDataSource(urlPropia, usuario, clave)).update("DELETE FROM shedlock");
    }

    private static LockProvider instancia(String zonaDeLaSesion) {
        DriverManagerDataSource fuente = new DriverManagerDataSource(urlPropia, usuario, clave);
        if (zonaDeLaSesion != null) {
            fuente.setConnectionProperties(propiedades("options", "-c TimeZone=" + zonaDeLaSesion));
        }
        return new BloqueoDeTareasConfig().proveedorDeBloqueos(fuente);
    }

    private static java.util.Properties propiedades(String clave, String valor) {
        java.util.Properties propiedades = new java.util.Properties();
        propiedades.setProperty(clave, valor);
        return propiedades;
    }

    private static Optional<SimpleLock> pedir(LockProvider proveedor, String nombre, Duration maximo, Duration minimo) {
        return proveedor.lock(new LockConfiguration(Instant.now(), nombre, maximo, minimo));
    }

    @Test
    void conVariasInstanciasCompitiendoSoloUnaObtieneElBloqueo() throws Exception {
        int instancias = 8;
        ExecutorService pool = Executors.newFixedThreadPool(instancias);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> intentos = new ArrayList<>();
        for (int i = 0; i < instancias; i++) {
            LockProvider proveedor = instancia(null);
            intentos.add(pool.submit(() -> {
                salida.await();
                return pedir(proveedor, "recordatorios-preventivos", Duration.ofMinutes(1), Duration.ofSeconds(5)).isPresent();
            }));
        }
        salida.countDown();
        int ganadoras = 0;
        for (Future<Boolean> intento : intentos) {
            if (intento.get(20, TimeUnit.SECONDS)) {
                ganadoras++;
            }
        }
        pool.shutdownNow();

        assertThat(ganadoras).isEqualTo(1);
    }

    @Test
    void despuesDeTerminarNadieRepiteLaTareaAntesDelMinimoYLuegoSiPuede() throws Exception {
        LockProvider unaInstancia = instancia(null);
        LockProvider otraInstancia = instancia(null);

        SimpleLock bloqueo = pedir(unaInstancia, "purga-cuentas-cerradas", Duration.ofMinutes(1), Duration.ofSeconds(3)).orElseThrow();
        bloqueo.unlock();

        assertThat(pedir(otraInstancia, "purga-cuentas-cerradas", Duration.ofMinutes(1), Duration.ofSeconds(3))).isEmpty();
        Thread.sleep(3500);
        assertThat(pedir(otraInstancia, "purga-cuentas-cerradas", Duration.ofMinutes(1), Duration.ofSeconds(3))).isPresent();
    }

    @Test
    void siLaInstanciaSeCaeSinSoltarElBloqueoOtraLoRetomaAlVencerElMaximo() throws Exception {
        LockProvider quienSeCae = instancia(null);
        LockProvider quienRetoma = instancia(null);

        pedir(quienSeCae, "cierre-vinculos-vencidos", Duration.ofSeconds(2), Duration.ZERO).orElseThrow();

        assertThat(pedir(quienRetoma, "cierre-vinculos-vencidos", Duration.ofSeconds(2), Duration.ZERO)).isEmpty();
        Thread.sleep(2500);
        assertThat(pedir(quienRetoma, "cierre-vinculos-vencidos", Duration.ofSeconds(2), Duration.ZERO)).isPresent();
    }

    @Test
    void lasTareasDistintasNoSeBloqueanEntreSi() {
        LockProvider proveedor = instancia(null);

        assertThat(pedir(proveedor, "tarea-a", Duration.ofMinutes(1), Duration.ZERO)).isPresent();
        assertThat(pedir(proveedor, "tarea-b", Duration.ofMinutes(1), Duration.ZERO)).isPresent();
    }

    @Test
    void elBloqueoNoDependeDeLaZonaHorariaDeLaSesionDeCadaInstancia() throws Exception {
        LockProvider enLima = instancia("America/Lima");
        LockProvider enTokio = instancia("Asia/Tokyo");
        LockProvider enUtc = instancia("UTC");

        SimpleLock bloqueo = pedir(enLima, "revision-autorizacion-mascotas", Duration.ofMinutes(10), Duration.ofMinutes(1)).orElseThrow();

        assertThat(pedir(enTokio, "revision-autorizacion-mascotas", Duration.ofMinutes(10), Duration.ofMinutes(1))).isEmpty();
        assertThat(pedir(enUtc, "revision-autorizacion-mascotas", Duration.ofMinutes(10), Duration.ofMinutes(1))).isEmpty();
        bloqueo.unlock();
        assertThat(pedir(enTokio, "revision-autorizacion-mascotas", Duration.ofMinutes(10), Duration.ofMinutes(1))).isEmpty();
    }
}

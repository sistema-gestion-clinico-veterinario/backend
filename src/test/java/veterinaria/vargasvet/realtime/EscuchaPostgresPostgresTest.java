package veterinaria.vargasvet.realtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import veterinaria.vargasvet.BaseDesechableDePruebas;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@EnabledIfEnvironmentVariable(named = "FLYWAY_TEST_URL", matches = ".+")
class EscuchaPostgresPostgresTest {

    private static String url;
    private static String usuario;
    private static String clave;
    private static JdbcTemplate administrador;

    private final List<Instancia> instancias = new ArrayList<>();

    @BeforeAll
    static void prepararBase() {
        url = System.getenv("FLYWAY_TEST_URL");
        usuario = System.getenv().getOrDefault("FLYWAY_TEST_USER", "postgres");
        clave = System.getenv().getOrDefault("FLYWAY_TEST_PASSWORD", "");
        BaseDesechableDePruebas.exigir(url);
        administrador = new JdbcTemplate(new DriverManagerDataSource(url, usuario, clave));
    }

    @AfterEach
    void cerrar() {
        instancias.forEach(Instancia::detener);
    }

    private final class Instancia {

        final String id = UUID.randomUUID().toString();
        final List<SobreTiempoReal> recibidos = new CopyOnWriteArrayList<>();
        final EscuchaPostgres escucha;
        final DifusionPostgres difusion;

        Instancia(String canal) {
            escucha = new EscuchaPostgres(url, usuario, clave, null, canal, id, recibidos::add);
            JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(url, usuario, clave));
            difusion = new DifusionPostgres(TiempoRealPruebas.conversor(),
                    (c, carga) -> jdbc.query("SELECT pg_notify(?, ?)", resultado -> { }, c, carga),
                    canal, id, 100);
            escucha.iniciar();
            instancias.add(this);
        }

        void esperarEscucha() {
            await().atMost(Duration.ofSeconds(20)).until(() -> escucha.estado() == EscuchaPostgres.Estado.ESCUCHANDO);
        }

        void detener() {
            escucha.detener();
            difusion.detener();
        }

        List<String> cuerpos() {
            return recibidos.stream().map(s -> new String(s.cuerpo(), StandardCharsets.UTF_8)).toList();
        }
    }

    private static String canalNuevo() {
        return "prueba_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @Test
    void unEventoDeUnaInstanciaLlegaATodasLasDemasYNoVuelveAlOrigen() throws Exception {
        String canal = canalNuevo();
        Instancia a = new Instancia(canal);
        Instancia b = new Instancia(canal);
        Instancia c = new Instancia(canal);
        a.esperarEscucha();
        b.esperarEscucha();
        c.esperarEscucha();

        a.difusion.difundir("/topic/citas/7", Map.of("nota", "Niño"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(b.recibidos).hasSize(1);
            assertThat(c.recibidos).hasSize(1);
        });
        Thread.sleep(1500);
        assertThat(a.recibidos).isEmpty();
        assertThat(b.recibidos).hasSize(1);
        assertThat(c.recibidos).hasSize(1);
        assertThat(b.recibidos.get(0).destino()).isEqualTo("/topic/citas/7");
        assertThat(b.recibidos.get(0).origen()).isEqualTo(a.id);
        assertThat(b.cuerpos()).containsExactly("{\"nota\":\"Niño\"}");
        assertThat(c.cuerpos()).containsExactly("{\"nota\":\"Niño\"}");
    }

    @Test
    void cadaInstanciaPuedePublicarYLasDemasLaOyen() {
        String canal = canalNuevo();
        Instancia a = new Instancia(canal);
        Instancia b = new Instancia(canal);
        Instancia c = new Instancia(canal);
        a.esperarEscucha();
        b.esperarEscucha();
        c.esperarEscucha();

        a.difusion.difundir("/topic/caja/1", Map.of("de", "a"));
        b.difusion.difundir("/topic/caja/1", Map.of("de", "b"));
        c.difusion.difundir("/topic/caja/1", Map.of("de", "c"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(a.cuerpos()).containsExactlyInAnyOrder("{\"de\":\"b\"}", "{\"de\":\"c\"}");
            assertThat(b.cuerpos()).containsExactlyInAnyOrder("{\"de\":\"a\"}", "{\"de\":\"c\"}");
            assertThat(c.cuerpos()).containsExactlyInAnyOrder("{\"de\":\"a\"}", "{\"de\":\"b\"}");
        });
    }

    @Test
    void losCanalesDistintosNoSeMezclan() throws Exception {
        String canalCompartido = canalNuevo();
        Instancia emisora = new Instancia(canalCompartido);
        Instancia delMismoCanal = new Instancia(canalCompartido);
        Instancia deOtroCanal = new Instancia(canalNuevo());
        emisora.esperarEscucha();
        delMismoCanal.esperarEscucha();
        deOtroCanal.esperarEscucha();

        emisora.difusion.difundir("/topic/citas/1", Map.of("n", 1));

        await().atMost(Duration.ofSeconds(10)).until(() -> delMismoCanal.recibidos.size() == 1);
        Thread.sleep(1500);
        assertThat(deOtroCanal.recibidos).isEmpty();
    }

    @Test
    void siSeCortaLaConexionDeEscuchaLaInstanciaSeReconectaYSigueRecibiendo() {
        String canal = canalNuevo();
        Instancia emisora = new Instancia(canal);
        Instancia receptora = new Instancia(canal);
        emisora.esperarEscucha();
        receptora.esperarEscucha();

        administrador.query("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE application_name = ?",
                resultado -> { }, receptora.escucha.nombreDeAplicacion());

        await().atMost(Duration.ofSeconds(40)).pollInterval(Duration.ofMillis(500)).until(() -> {
            emisora.difusion.difundir("/topic/citas/1", Map.of("sondeo", UUID.randomUUID().toString()));
            return !receptora.recibidos.isEmpty();
        });
    }

    @Test
    void alDetenerSeLiberaLaConexionDeEscucha() {
        Instancia instancia = new Instancia(canalNuevo());
        instancia.esperarEscucha();
        String aplicacion = instancia.escucha.nombreDeAplicacion();
        assertThat(conexionesDe(aplicacion)).isEqualTo(1);

        instancia.escucha.detener();

        assertThat(instancia.escucha.estado()).isEqualTo(EscuchaPostgres.Estado.DETENIDA);
        await().atMost(Duration.ofSeconds(10)).until(() -> conexionesDe(aplicacion) == 0);
    }

    private int conexionesDe(String aplicacion) {
        Integer total = administrador.queryForObject(
                "SELECT count(*) FROM pg_stat_activity WHERE application_name = ?", Integer.class, aplicacion);
        return total == null ? 0 : total;
    }
}

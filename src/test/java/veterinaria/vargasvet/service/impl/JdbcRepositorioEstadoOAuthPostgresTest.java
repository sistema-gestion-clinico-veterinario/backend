package veterinaria.vargasvet.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import veterinaria.vargasvet.BaseDesechableDePruebas;
import veterinaria.vargasvet.dto.response.AuthResponse;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "FLYWAY_TEST_URL", matches = ".+")
class JdbcRepositorioEstadoOAuthPostgresTest {

    private static final String BASE_PROPIA = "prueba_oauth_estado";
    private static final String MIGRACION = "db/migration/V140__Estado_OAuth_Google_Compartido.sql";

    private static String urlPropia;
    private static String usuario;
    private static String clave;

    private JdbcRepositorioEstadoOAuth repositorio;

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
        try (var entrada = JdbcRepositorioEstadoOAuthPostgresTest.class.getClassLoader().getResourceAsStream(MIGRACION)) {
            sql = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Connection conexion = DriverManager.getConnection(urlPropia, usuario, clave);
             Statement sentencia = conexion.createStatement()) {
            sentencia.execute("DROP TABLE IF EXISTS oauth_google_estado");
            sentencia.execute(sql);
        }
    }

    private static JdbcTemplate nuevaConexion() {
        return new JdbcTemplate(new DriverManagerDataSource(urlPropia, usuario, clave));
    }

    @BeforeEach
    void limpiar() {
        JdbcTemplate jdbc = nuevaConexion();
        jdbc.update("DELETE FROM oauth_google_estado");
        repositorio = new JdbcRepositorioEstadoOAuth(jdbc);
    }

    @Test
    void unaFilaSeRetiraUnaSolaVezYVieneVigente() {
        repositorio.insertar("INTENT", "h1", "contenido", 60);

        var primera = repositorio.retirar("INTENT", "h1");
        var segunda = repositorio.retirar("INTENT", "h1");

        assertThat(primera).isPresent();
        assertThat(primera.get().contenido()).isEqualTo("contenido");
        assertThat(primera.get().vigente()).isTrue();
        assertThat(segunda).isEmpty();
    }

    @Test
    void unaFilaVencidaSeRetiraPeroNoVieneVigente() throws Exception {
        repositorio.insertar("INTENT", "h2", "contenido", 1);
        Thread.sleep(1500);

        var fila = repositorio.retirar("INTENT", "h2");

        assertThat(fila).isPresent();
        assertThat(fila.get().vigente()).isFalse();
        assertThat(repositorio.retirar("INTENT", "h2")).isEmpty();
    }

    @Test
    void elTipoFormaParteDeLaClave() {
        repositorio.insertar("INTENT", "h3", "a", 60);

        assertThat(repositorio.retirar("SESION", "h3")).isEmpty();
        assertThat(repositorio.retirar("INTENT", "h3")).isPresent();
    }

    @Test
    void purgarBorraSoloLoVencidoYContarSeparaPorTipo() throws Exception {
        repositorio.insertar("INTENT", "vencida", "a", 1);
        repositorio.insertar("INTENT", "vigente", "b", 60);
        repositorio.insertar("SESION", "otra", "c", 60);
        Thread.sleep(1500);

        repositorio.purgarVencidos();

        assertThat(repositorio.contar("INTENT")).isEqualTo(1);
        assertThat(repositorio.contar("SESION")).isEqualTo(1);
    }

    @Test
    void variasConexionesQueCompitenPorLaMismaFilaSoloUnaLaObtiene() throws Exception {
        repositorio.insertar("INTENT", "disputada", "contenido", 60);
        int competidores = 16;
        ExecutorService pool = Executors.newFixedThreadPool(competidores);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> intentos = new ArrayList<>();
        for (int i = 0; i < competidores; i++) {
            JdbcRepositorioEstadoOAuth instancia = new JdbcRepositorioEstadoOAuth(nuevaConexion());
            intentos.add(pool.submit(() -> {
                salida.await();
                return instancia.retirar("INTENT", "disputada").isPresent();
            }));
        }
        salida.countDown();
        int ganadores = 0;
        for (Future<Boolean> intento : intentos) {
            if (intento.get(20, TimeUnit.SECONDS)) {
                ganadores++;
            }
        }
        pool.shutdownNow();

        assertThat(ganadores).isEqualTo(1);
    }

    @Test
    void elEstadoGuardadoPorUnaInstanciaSeCanjeaEnOtraConLosTokens() {
        CifradoEstadoOAuth cifrado = new CifradoEstadoOAuth(new byte[32]);
        GoogleLoginExchangeStore instanciaA = new GoogleLoginExchangeStore(
                new EstadoOAuthCompartido(new JdbcRepositorioEstadoOAuth(nuevaConexion()), cifrado, new ObjectMapper()));
        GoogleLoginExchangeStore instanciaB = new GoogleLoginExchangeStore(
                new EstadoOAuthCompartido(new JdbcRepositorioEstadoOAuth(nuevaConexion()), cifrado, new ObjectMapper()));
        AuthResponse sesion = new AuthResponse();
        sesion.setToken("token-de-acceso");
        sesion.setRefreshToken("token-de-refresco");
        sesion.setCompanySlug("vargas-vet");

        String codigo = instanciaA.store(sesion);
        AuthResponse canjeada = instanciaB.consume(codigo);

        assertThat(canjeada.getToken()).isEqualTo("token-de-acceso");
        assertThat(canjeada.getRefreshToken()).isEqualTo("token-de-refresco");
        assertThat(canjeada.getCompanySlug()).isEqualTo("vargas-vet");
        assertThat(instanciaA.consume(codigo)).isNull();
        String contenidoGuardado = nuevaConexion().queryForList("SELECT contenido FROM oauth_google_estado", String.class)
                .stream().findFirst().orElse("");
        assertThat(contenidoGuardado).doesNotContain("token-de-acceso");
    }

    @Test
    void laVidaSeMideConElRelojDeLaBaseYNoDeLaInstancia() {
        repositorio.insertar("INTENT", "h9", "contenido", Duration.ofMinutes(5).toSeconds());

        Long restante = nuevaConexion().queryForObject(
                "SELECT extract(epoch FROM (expira_en - CURRENT_TIMESTAMP))::bigint FROM oauth_google_estado WHERE clave_hash = 'h9'", Long.class);

        assertThat(restante).isBetween(290L, 300L);
    }
}

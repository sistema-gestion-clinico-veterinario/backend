package veterinaria.vargasvet;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba con una base PostgreSQL real que ya tiene tablas y ningún historial de Flyway, como producción.
 * Se omite si no se indica FLYWAY_TEST_URL (por ejemplo jdbc:postgresql://localhost:5432/prueba_flyway,
 * FLYWAY_TEST_USER y FLYWAY_TEST_PASSWORD); la base debe poder vaciarse.
 */
class FlywayConEsquemaExistenteTest {

    private static final String URL = System.getenv("FLYWAY_TEST_URL");
    private static final String USER = System.getenv().getOrDefault("FLYWAY_TEST_USER", "postgres");
    private static final String PASSWORD = System.getenv().getOrDefault("FLYWAY_TEST_PASSWORD", "");
    private static final String NUEVAS = "filesystem:src/main/resources/db/migration";

    @TempDir Path carpetaTemporal;

    @BeforeEach
    void baseComoProduccion() throws Exception {
        Assumptions.assumeTrue(URL != null, "FLYWAY_TEST_URL no está definida");
        try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD); Statement s = c.createStatement()) {
            s.execute("DROP SCHEMA public CASCADE");
            s.execute("CREATE SCHEMA public");
            s.execute("CREATE TABLE usuario (id serial PRIMARY KEY, email text)");
            s.execute("CREATE TABLE aviso_privacidad (id serial PRIMARY KEY)");
        }
    }

    private Flyway flyway(String... ubicaciones) {
        return Flyway.configure().dataSource(URL, USER, PASSWORD).locations(ubicaciones)
                .baselineOnMigrate(true).baselineVersion("103").validateOnMigrate(true).load();
    }

    private String uno(String sql) throws Exception {
        try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD); Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            return r.next() ? r.getString(1) : null;
        }
    }

    @Test
    void sobreUnEsquemaExistenteTomaLaV103ComoBaseYNoEjecutaNadaDelHistorico() throws Exception {
        var resultado = flyway(NUEVAS).migrate();

        assertThat(resultado.migrationsExecuted).isZero();
        assertThat(uno("SELECT version FROM flyway_schema_history WHERE type = 'BASELINE'")).isEqualTo("103");
        assertThat(uno("SELECT count(*) FROM flyway_schema_history")).isEqualTo("1");
        assertThat(uno("SELECT to_regclass('public.usuario')::text")).isEqualTo("usuario");
    }

    @Test
    void aplicaSoloLaMigracionNuevaYNoLaRepiteLaSegundaVez() throws Exception {
        Files.writeString(carpetaTemporal.resolve("V104__Tabla_De_Prueba.sql"),
                "CREATE TABLE prueba_v104 (id serial PRIMARY KEY);\nALTER TABLE prueba_v104 ENABLE ROW LEVEL SECURITY;\n");
        String carpeta = "filesystem:" + carpetaTemporal;

        var primera = flyway(NUEVAS, carpeta).migrate();
        var segunda = flyway(NUEVAS, carpeta).migrate();

        assertThat(primera.migrationsExecuted).isEqualTo(1);
        assertThat(segunda.migrationsExecuted).isZero();
        assertThat(uno("SELECT relrowsecurity::text FROM pg_class WHERE relname = 'prueba_v104'")).isEqualTo("true");
        assertThat(uno("SELECT string_agg(version, ',' ORDER BY installed_rank) FROM flyway_schema_history"))
                .isEqualTo("103,104");
        MigrationInfo[] hechas = flyway(NUEVAS, carpeta).info().applied();
        assertThat(hechas).hasSize(2);
    }

    @Test
    void unaMigracionNuevaConVersionYaCubiertaPorLaBaseSeIgnoraYNoSeAplica() throws Exception {
        Files.writeString(carpetaTemporal.resolve("V90__Vieja_Que_No_Debe_Correr.sql"),
                "CREATE TABLE no_debe_existir (id int);\n");

        var resultado = flyway(NUEVAS, "filesystem:" + carpetaTemporal).migrate();

        assertThat(resultado.migrationsExecuted).isZero();
        assertThat(uno("SELECT to_regclass('public.no_debe_existir')::text")).isNull();
    }

    @Test
    void laCarpetaDelHistoricoNoSePuedeEscanearPorTenerDosV52() {
        assertThatThrownBy(() -> flyway("filesystem:src/main/resources/db/historico").migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("52");
    }
}

package veterinaria.vargasvet;

import jakarta.persistence.Entity;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Prueba las migraciones con una base PostgreSQL real que ya tiene su esquema y ningún historial de Flyway,
 * como producción. El esquema inicial se genera desde las entidades (producción corre con ddl-auto=validate).
 *
 * Se necesita FLYWAY_TEST_URL (por ejemplo jdbc:postgresql://localhost:5432/prueba_flyway), más
 * FLYWAY_TEST_USER y FLYWAY_TEST_PASSWORD. La base se vacía en cada prueba, por eso solo se acepta un servidor
 * local con nombre de prueba. Sin la variable se omite, salvo en integración continua (CI=true), donde falla
 * para que estas pruebas no puedan quedar sin ejecutarse sin que nadie se entere.
 */
class FlywayConEsquemaExistenteTest {

    private static final String URL = System.getenv("FLYWAY_TEST_URL");
    private static final String USER = System.getenv().getOrDefault("FLYWAY_TEST_USER", "postgres");
    private static final String PASSWORD = System.getenv().getOrDefault("FLYWAY_TEST_PASSWORD", "");
    private static final String CARPETA_MIGRACIONES = "src/main/resources/db/migration";
    private static final String NUEVAS = "filesystem:" + CARPETA_MIGRACIONES;
    private static final Pattern ARCHIVO = Pattern.compile("^V(\\d+)__.+\\.sql$");

    @TempDir Path carpetaTemporal;

    @BeforeEach
    void baseComoProduccion() throws Exception {
        if (URL == null) {
            if ("true".equalsIgnoreCase(System.getenv("CI"))) {
                fail("FLYWAY_TEST_URL no está definida en integración continua: las pruebas de migraciones no pueden omitirse");
            }
            Assumptions.assumeTrue(false, "FLYWAY_TEST_URL no está definida");
        }
        BaseDesechableDePruebas.exigir(URL);
        try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD); Statement s = c.createStatement()) {
            s.execute("DROP SCHEMA public CASCADE");
            s.execute("CREATE SCHEMA public");
        }
        crearEsquemaDesdeLasEntidades();
        aplicarElHistoricoSobreElEsquema();
    }

    private void crearEsquemaDesdeLasEntidades() throws IOException {
        StandardServiceRegistry registro = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.url", URL)
                .applySetting("hibernate.connection.username", USER)
                .applySetting("hibernate.connection.password", PASSWORD)
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.hbm2ddl.auto", "create")
                .applySetting("hibernate.physical_naming_strategy",
                        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .applySetting("hibernate.implicit_naming_strategy",
                        "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy")
                .build();
        try {
            MetadataSources fuentes = new MetadataSources(registro);
            ClassPathScanningCandidateComponentProvider buscador = new ClassPathScanningCandidateComponentProvider(false);
            buscador.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            buscador.findCandidateComponents("veterinaria.vargasvet.domain.entity").forEach(definicion -> {
                try {
                    fuentes.addAnnotatedClass(Class.forName(definicion.getBeanClassName()));
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            });
            Metadata metadatos = fuentes.buildMetadata();
            try (SessionFactory fabrica = metadatos.buildSessionFactory()) {
                assertThat(fabrica.isOpen()).isTrue();
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registro);
        }
    }

    /**
     * Producción nació de las entidades y recibió encima los parches de db/historico (hasta la V103). Se reproduce
     * en el mismo orden; un parche que ya no aplica sobre el esquema de las entidades se omite, porque aquí solo
     * importa dejar los objetos que las migraciones nuevas esperan encontrar (disparadores, funciones, índices).
     */
    private void aplicarElHistoricoSobreElEsquema() throws Exception {
        Pattern version = Pattern.compile("^V(\\d+)__.+\\.sql$");
        List<Path> parches;
        try (Stream<Path> archivos = Files.list(Path.of("src/main/resources/db/historico"))) {
            parches = archivos.filter(ruta -> version.matcher(ruta.getFileName().toString()).matches())
                    .sorted(Comparator.comparingInt((Path ruta) -> {
                        Matcher m = version.matcher(ruta.getFileName().toString());
                        m.matches();
                        return Integer.parseInt(m.group(1));
                    }).thenComparing(ruta -> ruta.getFileName().toString()))
                    .toList();
        }
        try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD)) {
            for (Path parche : parches) {
                for (String sentencia : separarSentencias(Files.readString(parche))) {
                    try (Statement s = c.createStatement()) {
                        s.execute(sentencia);
                    } catch (java.sql.SQLException omitida) {
                        // Sentencia ya cubierta por las entidades o que dependía de datos antiguos.
                    }
                }
            }
        }
    }

    /** Separa un script en sentencias respetando cadenas, comentarios y bloques $$ ... $$. */
    static List<String> separarSentencias(String script) {
        List<String> sentencias = new java.util.ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean enCadena = false;
        boolean enBloque = false;
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            if (!enCadena && script.startsWith("$$", i)) {
                enBloque = !enBloque;
                actual.append("$$");
                i++;
                continue;
            }
            if (!enBloque && c == '\'') {
                enCadena = !enCadena;
            }
            if (!enBloque && !enCadena && script.startsWith("--", i)) {
                int fin = script.indexOf('\n', i);
                i = fin < 0 ? script.length() : fin;
                actual.append('\n');
                continue;
            }
            if (!enBloque && !enCadena && c == ';') {
                if (!actual.toString().isBlank()) {
                    sentencias.add(actual.toString());
                }
                actual.setLength(0);
                continue;
            }
            actual.append(c);
        }
        if (!actual.toString().isBlank()) {
            sentencias.add(actual.toString());
        }
        return sentencias;
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

    private void ejecutar(String sql) throws Exception {
        try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private int migracionesNuevasEnLaCarpeta() throws IOException {
        try (Stream<Path> archivos = Files.list(Path.of(CARPETA_MIGRACIONES))) {
            return (int) archivos.map(ruta -> ARCHIVO.matcher(ruta.getFileName().toString()))
                    .filter(Matcher::matches)
                    .filter(m -> Integer.parseInt(m.group(1)) > 103)
                    .count();
        }
    }

    @Test
    void sobreUnEsquemaExistenteTomaLaV103ComoBaseYAplicaSoloLasMigracionesNuevas() throws Exception {
        int nuevas = migracionesNuevasEnLaCarpeta();
        assertThat(nuevas).as("debe haber migraciones posteriores a la base").isPositive();

        var resultado = flyway(NUEVAS).migrate();

        assertThat(resultado.migrationsExecuted).isEqualTo(nuevas);
        assertThat(uno("SELECT version FROM flyway_schema_history WHERE type = 'BASELINE'")).isEqualTo("103");
        assertThat(uno("SELECT count(*) FROM flyway_schema_history")).isEqualTo(String.valueOf(nuevas + 1));
        assertThat(uno("SELECT to_regclass('public.usuario')::text")).isEqualTo("usuario");
        assertThat(uno("SELECT count(*) FROM flyway_schema_history WHERE success = false")).isEqualTo("0");
    }

    @Test
    void unaSegundaEjecucionNoRepiteNingunaMigracion() throws Exception {
        flyway(NUEVAS).migrate();

        var segunda = flyway(NUEVAS).migrate();

        assertThat(segunda.migrationsExecuted).isZero();
    }

    @Test
    void lasMigracionesAplicadasAManoSePuedenVolverAEjecutarSinError() throws Exception {
        int nuevas = migracionesNuevasEnLaCarpeta();
        flyway(NUEVAS).migrate();
        // Producción tuvo migraciones aplicadas a mano que Flyway no conoce: se simula olvidando su historial.
        ejecutar("DELETE FROM flyway_schema_history WHERE type <> 'BASELINE'");

        var repetida = flyway(NUEVAS).migrate();

        assertThat(repetida.migrationsExecuted).isEqualTo(nuevas);
        assertThat(uno("SELECT count(*) FROM flyway_schema_history WHERE success = false")).isEqualTo("0");
    }

    @Test
    void unaMigracionPosteriorSeAplicaEnOrdenYNoSeRepiteLaSegundaVez() throws Exception {
        int nuevas = migracionesNuevasEnLaCarpeta();
        Files.writeString(carpetaTemporal.resolve("V9999__Tabla_De_Prueba.sql"),
                "CREATE TABLE prueba_v9999 (id serial PRIMARY KEY);\nALTER TABLE prueba_v9999 ENABLE ROW LEVEL SECURITY;\n");
        String carpeta = "filesystem:" + carpetaTemporal;

        var primera = flyway(NUEVAS, carpeta).migrate();
        var segunda = flyway(NUEVAS, carpeta).migrate();

        assertThat(primera.migrationsExecuted).isEqualTo(nuevas + 1);
        assertThat(segunda.migrationsExecuted).isZero();
        assertThat(uno("SELECT relrowsecurity::text FROM pg_class WHERE relname = 'prueba_v9999'")).isEqualTo("true");
        assertThat(uno("SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1")).isEqualTo("9999");
        MigrationInfo[] hechas = flyway(NUEVAS, carpeta).info().applied();
        assertThat(hechas).hasSize(nuevas + 2);
    }

    @Test
    void unaMigracionConVersionYaCubiertaPorLaBaseSeIgnoraYNoSeAplica() throws Exception {
        Files.writeString(carpetaTemporal.resolve("V90__Vieja_Que_No_Debe_Correr.sql"),
                "CREATE TABLE no_debe_existir (id int);\n");

        flyway(NUEVAS, "filesystem:" + carpetaTemporal).migrate();

        assertThat(uno("SELECT to_regclass('public.no_debe_existir')::text")).isNull();
        assertThat(uno("SELECT count(*) FROM flyway_schema_history WHERE version = '90'")).isEqualTo("0");
    }

    @Test
    void laCarpetaDelHistoricoNoSePuedeEscanearPorTenerDosV52() {
        assertThatThrownBy(() -> flyway("filesystem:src/main/resources/db/historico").migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("52");
    }
}

package veterinaria.vargasvet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizacionDeMigracionesTest {

    private static final int LINEA_BASE = 103;
    private static final Pattern NOMBRE = Pattern.compile("^V(\\d+)__[A-Za-z0-9_]+\\.sql$");
    private static final Path NUEVAS = Path.of("src/main/resources/db/migration");
    private static final Path HISTORICO = Path.of("src/main/resources/db/historico");

    private List<String> sql(Path carpeta) throws IOException {
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".sql")).sorted().toList();
        }
    }

    @Test
    void lasMigracionesNuevasSoloTienenVersionesPosterioresALaLineaBaseYNingunaSeRepite() throws IOException {
        Set<Integer> vistas = new HashSet<>();
        for (String nombre : sql(NUEVAS)) {
            Matcher m = NOMBRE.matcher(nombre);
            assertThat(m.matches()).as("nombre con el formato V<numero>__Descripcion.sql: " + nombre).isTrue();
            int version = Integer.parseInt(m.group(1));
            assertThat(version).as("una migración nueva debe ser posterior a la V" + LINEA_BASE + ": " + nombre)
                    .isGreaterThan(LINEA_BASE);
            assertThat(vistas.add(version)).as("versión repetida: " + nombre).isTrue();
        }
    }

    @Test
    void elHistoricoConservaLasMigracionesAplicadasAManoYFlywayNoLasLee() throws IOException {
        List<String> historico = sql(HISTORICO);

        assertThat(historico).hasSizeGreaterThanOrEqualTo(73);
        assertThat(historico).anyMatch(n -> n.startsWith("V103__"));
        assertThat(historico).noneMatch(n -> {
            Matcher m = NOMBRE.matcher(n);
            return m.matches() && Integer.parseInt(m.group(1)) > LINEA_BASE;
        });
    }

    @Test
    void laConfiguracionDeFlywayCoincideConLaLineaBaseYArrancaApagada() throws IOException {
        String propiedades = Files.readString(Path.of("src/main/resources/application.properties"));

        assertThat(propiedades).contains("spring.flyway.baseline-version=" + LINEA_BASE);
        assertThat(propiedades).contains("spring.flyway.enabled=${FLYWAY_ENABLED:false}");
        assertThat(propiedades).contains("spring.flyway.locations=classpath:db/migration");
    }
}

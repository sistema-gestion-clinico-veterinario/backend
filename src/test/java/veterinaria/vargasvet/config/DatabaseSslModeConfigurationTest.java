package veterinaria.vargasvet.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DatabaseSslModeConfigurationTest {

    @Test
    void produccionUsaTlsObligatorioPorDefecto() throws Exception {
        Path propsFile = Path.of("src/main/resources/application.properties");
        String content = Files.readString(propsFile);

        String sslmodeLine = content.lines()
                .filter(line -> line.trim().startsWith("spring.datasource.hikari.data-source-properties.sslmode="))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No se encontro la property de sslmode en application.properties"));

        assertThat(sslmodeLine).contains("DB_SSL_MODE:require");
        assertThat(sslmodeLine).doesNotContain(":disable");
        assertThat(sslmodeLine).doesNotContain(":allow");
    }

    @Test
    void perfilLocalPermiteConexionSinTls() throws Exception {
        Path propsFile = Path.of("src/main/resources/application-local.properties");
        assumeTrue(Files.exists(propsFile), "application-local.properties no existe en este entorno (gitignored) - se omite");

        String content = Files.readString(propsFile);
        assertThat(content).contains("sslmode=prefer");
    }
}

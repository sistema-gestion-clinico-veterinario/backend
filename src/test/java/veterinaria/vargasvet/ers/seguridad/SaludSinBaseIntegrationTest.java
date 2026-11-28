package veterinaria.vargasvet.ers.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Render reinicia una instancia cuando su ruta de salud falla varias veces seguidas. Si esa ruta incluye la base de
 * datos, una caída de la base provocaría reinicios que no la arreglan. La de vida no depende de la base.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SaludSinBaseIntegrationTest.Configuracion.class)
@TestPropertySource(locations = {"file:src/main/resources/application.properties", "classpath:aislamiento.properties"},
        properties = "spring.datasource.url=jdbc:h2:mem:vargasvet_salud;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class SaludSinBaseIntegrationTest {

    @Autowired private TestRestTemplate http;

    @Test
    void conUnaDependenciaCaidaLaSaludGeneralFallaPeroLaDeVidaResponde() {
        ResponseEntity<String> general = http.getForEntity("/actuator/health", String.class);
        ResponseEntity<String> vida = http.getForEntity("/actuator/health/liveness", String.class);

        assertThat(general.getStatusCode().value()).isEqualTo(503);
        assertThat(vida.getStatusCode().value()).isEqualTo(200);
        assertThat(vida.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void laDeLecturaIncluyeLaBaseYTambienEstaDisponibleSinSesionSinExponerDetalles() {
        ResponseEntity<String> lectura = http.getForEntity("/actuator/health/readiness", String.class);

        assertThat(lectura.getStatusCode().value()).isEqualTo(200);
        assertThat(lectura.getBody()).doesNotContain("components").doesNotContain("motivo");
    }

    @TestConfiguration
    static class Configuracion {

        @Bean
        HealthIndicator dependenciaCaida() {
            return () -> Health.down().withDetail("motivo", "prueba").build();
        }
    }
}

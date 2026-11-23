package veterinaria.vargasvet.ers.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.CannotCreateTransactionException;
import veterinaria.vargasvet.security.SharedRateLimitService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Con un servidor web real: si el límite de peticiones, que consulta la base, falla como lo haría una base caída o
 * un pool agotado, las rutas de autenticación responden 503 con reintento, sin tocar las cookies de sesión.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(BaseCaidaEnFiltrosIntegrationTest.Configuracion.class)
@TestPropertySource(locations = {"file:src/main/resources/application.properties", "classpath:aislamiento.properties"},
        properties = "spring.datasource.url=jdbc:h2:mem:vargasvet_base_caida;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class BaseCaidaEnFiltrosIntegrationTest {

    @Autowired private TestRestTemplate http;

    private ResponseEntity<String> enviar(String ruta, String cuerpo) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        cabeceras.add("Origin", "http://localhost:4200");
        return http.exchange(ruta, HttpMethod.POST, new HttpEntity<>(cuerpo, cabeceras), String.class);
    }

    @Test
    void lasRutasDeAutenticacionRespondenConReintentoYSinBorrarLaSesion() {
        String[][] rutas = {
                {"/auth/refresh", null},
                {"/auth/login", "{\"slug\":\"x\",\"username\":\"a\",\"password\":\"b\"}"},
                {"/auth/forgot-password", "{\"email\":\"a@b.test\",\"slug\":\"x\"}"},
        };
        for (String[] r : rutas) {
            ResponseEntity<String> respuesta = enviar(r[0], r[1]);

            assertThat(respuesta.getStatusCode().value()).as(r[0]).isEqualTo(503);
            assertThat(respuesta.getHeaders().getFirst("Retry-After")).as(r[0]).isEqualTo("5");
            assertThat(respuesta.getHeaders().getFirst("Access-Control-Allow-Origin")).as(r[0]).isEqualTo("http://localhost:4200");
            assertThat(respuesta.getHeaders().get("Set-Cookie")).as(r[0]).isNull();
            assertThat(respuesta.getBody()).as(r[0]).contains("temporalmente");
        }
    }

    @TestConfiguration
    static class Configuracion {

        @Bean
        @Primary
        SharedRateLimitService limiteQueFallaPorBaseCaida(JdbcTemplate jdbc) {
            return new SharedRateLimitService(jdbc) {
                @Override
                public boolean tryConsume(String namespace, String identifier, int capacity, Duration period) {
                    throw new CannotCreateTransactionException("Could not open JPA EntityManager for transaction",
                            new RuntimeException("HikariPool-1 - Connection is not available"));
                }

                @Override
                public void enforce(String namespace, String identifier, int capacity, Duration period) {
                    throw new CannotCreateTransactionException("Could not open JPA EntityManager for transaction",
                            new RuntimeException("HikariPool-1 - Connection is not available"));
                }
            };
        }
    }
}

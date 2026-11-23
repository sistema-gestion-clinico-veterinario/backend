package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.CannotCreateTransactionException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El límite de peticiones consulta la base: si ella cae, la respuesta es 503 y no un 500 del servidor. */
class RateLimitFilterBaseCaidaTest {

    private SharedRateLimitService limites;
    private RateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        limites = mock(SharedRateLimitService.class);
        chain = mock(FilterChain.class);
        filter = new RateLimitFilter(limites, new ClientIpResolver(1));
    }

    private MockHttpServletResponse enviar(String metodo, String ruta) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(metodo, ruta), response, chain);
        return response;
    }

    @Test
    void lasRutasDeAutenticacionRespondenConReintentoCuandoLaBaseNoResponde() throws Exception {
        when(limites.tryConsume(anyString(), anyString(), anyInt(), any(Duration.class)))
                .thenThrow(new CannotCreateTransactionException("Could not open JPA EntityManager for transaction"));

        for (String ruta : new String[]{"/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/forgot-password"}) {
            MockHttpServletResponse response = enviar("POST", ruta);

            assertThat(response.getStatus()).as(ruta).isEqualTo(503);
            assertThat(response.getHeader("Retry-After")).as(ruta).isEqualTo("5");
            assertThat(response.getHeader("Set-Cookie")).as(ruta).isNull();
        }
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void unErrorQueNoEsDeLaBaseSigueSiendoUnErrorYNoUna503() {
        when(limites.tryConsume(anyString(), anyString(), anyInt(), any(Duration.class)))
                .thenThrow(new IllegalStateException("fallo de programación"));

        assertThatThrownBy(() -> enviar("POST", "/api/v1/auth/login")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void lasRutasQueNoUsanLaBaseNoSeVenAfectadas() throws Exception {
        MockHttpServletResponse response = enviar("GET", "/api/v1/mascotas");

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(any(), any());
    }
}

package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitFilterAuthenticationTest {

    private SharedRateLimitService rateLimits;
    private FilterChain chain;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        rateLimits = mock(SharedRateLimitService.class);
        ClientIpResolver ipResolver = mock(ClientIpResolver.class);
        when(ipResolver.resolve(any())).thenReturn("203.0.113.10");
        filter = new RateLimitFilter(rateLimits, ipResolver);
        ReflectionTestUtils.setField(filter, "loginPerMinute", 20);
        ReflectionTestUtils.setField(filter, "passwordResetPerWindow", 5);
        chain = mock(FilterChain.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/auth/login",
            "/api/v1/auth/admin-login",
            "/api/v1/auth/google/intent",
            "/api/v1/auth/google/exchange",
            "/api/v1/auth/google/reactivate"
    })
    void todaEntradaDeAutenticacionSeLimitaPorIpYNoContinuaAlExcederse(String path) throws Exception {
        when(rateLimits.tryConsume("login-ip", "203.0.113.10", 20, Duration.ofMinutes(1)))
                .thenReturn(false);

        MockHttpServletResponse response = execute("POST", path);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentAsString()).contains("Demasiadas solicitudes");
        verify(chain, never()).doFilter(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/resend-verification",
            "/api/v1/auth/resend-verification-by-token",
            "/api/v1/auth/account/reactivate",
            "/api/v1/auth/setup-account"
    })
    void todaRecuperacionSeLimitaYExponeLaVentanaCorrecta(String path) throws Exception {
        when(rateLimits.tryConsume("account-recovery-ip", "203.0.113.10", 5, Duration.ofMinutes(15)))
                .thenReturn(false);

        MockHttpServletResponse response = execute("POST", path);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("900");
        verify(chain, never()).doFilter(any(), any());
    }

    private MockHttpServletResponse execute(String method, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }
}

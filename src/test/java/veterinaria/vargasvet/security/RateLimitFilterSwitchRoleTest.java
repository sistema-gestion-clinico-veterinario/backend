package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitFilterSwitchRoleTest {

    private RateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        ClientIpResolver ipResolver = mock(ClientIpResolver.class);
        when(ipResolver.resolve(any())).thenReturn("10.0.0.1");
        filter = new RateLimitFilter(mock(SharedRateLimitService.class), ipResolver);
        ReflectionTestUtils.setField(filter, "switchRolePerMinute", 2);
        chain = mock(FilterChain.class);
    }

    private MockHttpServletResponse switchRoleAs(String usuario) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/switch-role");
        Principal principal = () -> usuario;
        request.setUserPrincipal(principal);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void limitaLosCambiosDeRolPorUsuario() throws Exception {
        assertThat(switchRoleAs("ana@example.test").getStatus()).isEqualTo(200);
        assertThat(switchRoleAs("ana@example.test").getStatus()).isEqualTo(200);
        assertThat(switchRoleAs("ana@example.test").getStatus()).isEqualTo(429);

        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    void elLimiteDeUnUsuarioNoAfectaAOtro() throws Exception {
        switchRoleAs("ana@example.test");
        switchRoleAs("ana@example.test");
        switchRoleAs("ana@example.test");

        assertThat(switchRoleAs("luis@example.test").getStatus()).isEqualTo(200);
    }
}

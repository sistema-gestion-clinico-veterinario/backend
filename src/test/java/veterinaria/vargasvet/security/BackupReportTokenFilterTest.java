package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class BackupReportTokenFilterTest {

    private static final String TOKEN = "0123456789abcdef0123456789abcdef";
    private final BackupReportTokenFilter filter =
            new BackupReportTokenFilter(new BackupReportTokenVerifier(TOKEN));

    @Test
    void rechazaAntesDelControladorCuandoFaltaElToken() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    @Test
    void permiteContinuarCuandoElTokenCoincide() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader(BackupReportTokenFilter.TOKEN_HEADER, TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/backups/events");
        request.setServletPath("/internal/backups/events");
        return request;
    }
}

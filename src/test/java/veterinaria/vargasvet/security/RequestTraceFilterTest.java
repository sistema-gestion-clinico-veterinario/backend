package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import veterinaria.vargasvet.service.SystemLogService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RequestTraceFilterTest {
    private final SystemLogService systemLogService = mock(SystemLogService.class);
    private final RequestTraceFilter filter = new RequestTraceFilter(systemLogService);

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void conservaIdentificadorSeguroYRegistraLaSolicitud() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/citas");
        request.addHeader(RequestTraceFilter.HEADER, "caso-cita-12345");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(201);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestTraceFilter.HEADER)).isEqualTo("caso-cita-12345");
        verify(systemLogService).recordHttpRequest(eq(request), eq("caso-cita-12345"),
                eq(201), longThat(value -> value >= 0), isNull());
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void reemplazaIdentificadorManipulado() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/clientes");
        request.addHeader(RequestTraceFilter.HEADER, "valor\ninyectado");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String generated = response.getHeader(RequestTraceFilter.HEADER);
        assertThat(generated).matches("[0-9a-f-]{36}");
        verify(systemLogService).recordHttpRequest(eq(request), eq(generated),
                eq(200), anyLong(), isNull());
    }

    @Test
    void registraElTipoDeFalloSinOcultarlo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/recurso");
        MockHttpServletResponse response = new MockHttpServletResponse();
        IllegalStateException expected = new IllegalStateException("fallo controlado");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> filter.doFilter(request, response, (req, res) -> { throw expected; }));

        ArgumentCaptor<String> id = ArgumentCaptor.forClass(String.class);
        verify(systemLogService).recordHttpRequest(eq(request), id.capture(),
                eq(200), anyLong(), same(expected));
        assertThat(id.getValue()).isNotBlank();
        assertThat(MDC.get("correlationId")).isNull();
    }
}

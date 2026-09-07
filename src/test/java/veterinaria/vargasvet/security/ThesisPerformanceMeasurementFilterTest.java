package veterinaria.vargasvet.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerMapping;
import veterinaria.vargasvet.domain.enums.ThesisMeasurementPhase;
import veterinaria.vargasvet.domain.enums.ThesisPerformanceOperation;
import veterinaria.vargasvet.service.ThesisPerformanceMeasurementService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ThesisPerformanceMeasurementFilterTest {

    private ThesisPerformanceMeasurementService measurementService;
    private ThesisPerformanceMeasurementFilter filter;

    @BeforeEach
    void setUp() {
        measurementService = mock(ThesisPerformanceMeasurementService.class);
        filter = new ThesisPerformanceMeasurementFilter(measurementService);
        ReflectionTestUtils.setField(filter, "enabled", true);

        UsuarioPrincipal principal = new UsuarioPrincipal(7, "vet@example.com", "", List.of(), 3);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsNormalizedCriticalRouteForAnExplicitSession() throws Exception {
        UUID sessionId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/medical-records");
        request.addHeader(ThesisPerformanceMeasurementFilter.SESSION_HEADER, sessionId.toString());
        request.addHeader(ThesisPerformanceMeasurementFilter.PHASE_HEADER, "SAMPLE");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                servletRequest.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/medical-records"));

        verify(measurementService).record(
                eq(sessionId),
                eq(ThesisMeasurementPhase.SAMPLE),
                any(),
                eq(ThesisPerformanceOperation.SEARCH_MEDICAL_RECORDS),
                eq("/medical-records"),
                eq("GET"),
                any(BigDecimal.class),
                eq(200),
                eq(7),
                eq(3));
    }

    @Test
    void ignoresRequestsWithoutMeasurementHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/medical-records");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                servletRequest.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/medical-records"));

        verifyNoInteractions(measurementService);
    }

    @Test
    void ignoresRoutesOutsideTheDefinedInstrument() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/appointments");
        request.addHeader(ThesisPerformanceMeasurementFilter.SESSION_HEADER, UUID.randomUUID().toString());
        request.addHeader(ThesisPerformanceMeasurementFilter.PHASE_HEADER, "WARMUP");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                servletRequest.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/appointments"));

        verifyNoInteractions(measurementService);
    }
}

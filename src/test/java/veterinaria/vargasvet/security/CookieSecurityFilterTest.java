package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CookieSecurityFilterTest {

    private final CookieSecurityFilter filter =
            new CookieSecurityFilter("https://systemvetfrontend.vercel.app,http://localhost:4200");

    @Test
    void rechazaLoginSinOrigenConfiable() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setServletPath("/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void aceptaOperacionConCookieDesdeOrigenPermitido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/pets");
        request.setServletPath("/pets");
        request.setCookies(new Cookie("access_token", "token"));
        request.addHeader("Origin", "https://systemvetfrontend.vercel.app");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void lecturaNoRequiereCabeceraOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/pets");
        request.setServletPath("/pets");
        request.setCookies(new Cookie("access_token", "token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}

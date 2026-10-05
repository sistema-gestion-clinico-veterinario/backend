package veterinaria.vargasvet.ers.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.controller.AuthController;
import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.security.SessionCookies;
import veterinaria.vargasvet.service.AccountClosureService;
import veterinaria.vargasvet.service.EmailChangeService;
import veterinaria.vargasvet.service.UsuarioService;
import veterinaria.vargasvet.service.impl.GoogleLoginExchangeStore;
import veterinaria.vargasvet.service.impl.GoogleOAuthFlowStore;
import veterinaria.vargasvet.service.impl.GoogleOAuthService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Un mismo navegador mantiene una sesión por clínica: el slug es parte del nombre de las cookies. */
class AuthControllerSessionCookiesTest {

    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final AuthController controller = new AuthController(
            usuarioService, mock(EmailChangeService.class), mock(GoogleOAuthService.class),
            mock(GoogleLoginExchangeStore.class), new GoogleOAuthFlowStore(), mock(AccountClosureService.class),
                org.mockito.Mockito.mock(veterinaria.vargasvet.security.ClientIpResolver.class));

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "cookieSecure", true);
        ReflectionTestUtils.setField(controller, "cookieSameSite", "Lax");
        ReflectionTestUtils.setField(controller, "accessTokenMaxAge", 1800L);
        ReflectionTestUtils.setField(controller, "refreshTokenMaxAge", 604800L);
    }

    private AuthResponse sesion(String slug) {
        AuthResponse auth = new AuthResponse();
        auth.setToken("acceso-" + slug);
        auth.setRefreshToken("refresco-" + slug);
        auth.setCompanySlug(slug);
        auth.setRoles(List.of("ROLE_X"));
        return auth;
    }

    private List<String> nombresDeCookies(MockHttpServletResponse response) {
        return response.getHeaders(HttpHeaders.SET_COOKIE).stream().map(c -> c.substring(0, c.indexOf('='))).toList();
    }

    private LoginDTO login(String slug) {
        LoginDTO dto = new LoginDTO();
        dto.setSlug(slug);
        dto.setUsername("persona");
        dto.setPassword("Frase extensa de prueba 2026");
        return dto;
    }

    @Test
    void elLoginDeUnaClinicaGuardaSusCookiesConElSlugEnElNombre() {
        LoginDTO dto = login("clinica-a");
        when(usuarioService.login(dto)).thenReturn(sesion("clinica-a"));
        MockHttpServletResponse http = new MockHttpServletResponse();

        controller.login(dto, http);

        assertThat(nombresDeCookies(http)).containsExactlyInAnyOrder("access_token__clinica-a", "refresh_token__clinica-a");
        assertThat(http.getHeaders(HttpHeaders.SET_COOKIE)).allSatisfy(c -> assertThat(c).contains("HttpOnly").contains("Secure"));
    }

    @Test
    void elLoginDeUnaSegundaClinicaNoPisaLasCookiesDeLaPrimera() {
        LoginDTO dtoA = login("clinica-a");
        LoginDTO dtoB = login("clinica-b");
        when(usuarioService.login(dtoA)).thenReturn(sesion("clinica-a"));
        when(usuarioService.login(dtoB)).thenReturn(sesion("clinica-b"));
        MockHttpServletResponse primera = new MockHttpServletResponse();
        MockHttpServletResponse segunda = new MockHttpServletResponse();

        controller.login(dtoA, primera);
        controller.login(dtoB, segunda);

        assertThat(nombresDeCookies(primera)).doesNotContainAnyElementsOf(nombresDeCookies(segunda));
        assertThat(nombresDeCookies(segunda)).noneMatch(nombre -> nombre.endsWith("clinica-a"));
    }

    @Test
    void laCuentaDePlataformaSigueConLosNombresDeSiempre() {
        var dto = new veterinaria.vargasvet.dto.request.AdminLoginDTO();
        dto.setUsername("plataforma");
        dto.setPassword("Frase extensa de prueba 2026");
        when(usuarioService.adminLogin(dto)).thenReturn(sesion(null));
        MockHttpServletResponse http = new MockHttpServletResponse();

        controller.adminLogin(dto, http);

        assertThat(nombresDeCookies(http)).containsExactlyInAnyOrder("access_token", "refresh_token");
    }

    @Test
    void unSlugRaroNuncaLlegaAUnNombreDeCookie() {
        LoginDTO dto = login("x");
        AuthResponse rara = sesion("clinica-a");
        rara.setCompanySlug("malo;path=/");
        when(usuarioService.login(dto)).thenReturn(rara);
        MockHttpServletResponse http = new MockHttpServletResponse();

        controller.login(dto, http);

        assertThat(nombresDeCookies(http)).containsExactlyInAnyOrder("access_token", "refresh_token");
    }

    @Test
    void renovarUsaLaCookieDeLaClinicaDeLaPestanaYNoLaDeOtra() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SessionCookies.SLUG_HEADER, "clinica-b");
        request.setCookies(new Cookie("refresh_token__clinica-a", "refresco-a"), new Cookie("refresh_token__clinica-b", "refresco-b"));
        when(usuarioService.refreshToken("refresco-b")).thenReturn(sesion("clinica-b"));
        MockHttpServletResponse http = new MockHttpServletResponse();

        controller.refresh(request, http);

        verify(usuarioService).refreshToken("refresco-b");
        verify(usuarioService, never()).refreshToken("refresco-a");
        assertThat(nombresDeCookies(http)).containsExactlyInAnyOrder("access_token__clinica-b", "refresh_token__clinica-b");
    }

    @Test
    void renovarEnUnaClinicaSinSuCookieNoConsumeLaSesionDePlataforma() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SessionCookies.SLUG_HEADER, "clinica-a");
        request.setCookies(new Cookie("refresh_token", "refresco-plataforma"));
        when(usuarioService.refreshToken(null))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("sin sesión"));
        MockHttpServletResponse http = new MockHttpServletResponse();

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.security.authentication.BadCredentialsException.class,
                () -> controller.refresh(request, http));

        verify(usuarioService, never()).refreshToken("refresco-plataforma");
        assertThat(http.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    @Test
    void cerrarSesionEnUnaClinicaSinSuCookieNoRevocaLaSesionDePlataforma() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SessionCookies.SLUG_HEADER, "clinica-a");
        request.setCookies(new Cookie("refresh_token", "refresco-plataforma"));
        MockHttpServletResponse http = new MockHttpServletResponse();

        controller.logout(request, http);

        verify(usuarioService, never()).revokeRefreshToken("refresco-plataforma");
        assertThat(nombresDeCookies(http)).containsExactlyInAnyOrder("access_token__clinica-a", "refresh_token__clinica-a");
    }

    @Test
    void cerrarSesionRevocaYBorraSoloLasCookiesDeEsaClinica() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SessionCookies.SLUG_HEADER, "clinica-a");
        request.setCookies(new Cookie("refresh_token__clinica-a", "refresco-a"), new Cookie("refresh_token__clinica-b", "refresco-b"));
        MockHttpServletResponse http = new MockHttpServletResponse();

        controller.logout(request, http);

        verify(usuarioService).revokeRefreshToken("refresco-a");
        assertThat(nombresDeCookies(http)).containsExactlyInAnyOrder("access_token__clinica-a", "refresh_token__clinica-a");
        assertThat(http.getHeaders(HttpHeaders.SET_COOKIE)).allSatisfy(c -> assertThat(c).contains("Max-Age=0"));
    }

    @Test
    void cerrarSesionSinClinicaDeclaradaBorraLasCookiesDeSiempre() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", "refresco"));
        MockHttpServletResponse http = new MockHttpServletResponse();

        controller.logout(request, http);

        verify(usuarioService).revokeRefreshToken("refresco");
        assertThat(nombresDeCookies(http)).containsExactlyInAnyOrder("access_token", "refresh_token");
    }

    @Test
    void sinCookieNoSeIntentaRenovarConNada() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SessionCookies.SLUG_HEADER, "clinica-a");
        when(usuarioService.refreshToken(any())).thenReturn(sesion("clinica-a"));

        controller.refresh(request, new MockHttpServletResponse());

        verify(usuarioService).refreshToken(null);
    }
}

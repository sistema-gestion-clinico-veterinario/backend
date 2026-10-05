package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.LegalDocumentService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** El filtro lee la sesión de la clínica que declara la pestaña y nunca la de otra clínica del mismo navegador. */
@ExtendWith(MockitoExtension.class)
class JWTFilterSessionCookieTest {

    @Mock TokenProvider tokenProvider;
    @Mock UsuarioRepository usuarioRepository;
    @Mock UsuarioPorRolRepository usuarioPorRolRepository;
    @Mock UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock LegalDocumentService legalDocumentService;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock FilterChain chain;

    private JWTFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JWTFilter(tokenProvider, usuarioRepository, usuarioPorRolRepository, credencialRepository,
                legalDocumentService, refreshTokenRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest solicitud(String slug) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mascotas");
        request.setServletPath("/mascotas");
        request.setCookies(new Cookie("access_token__clinica-a", "token-a"), new Cookie("access_token__clinica-b", "token-b"));
        if (slug != null) {
            request.addHeader(SessionCookies.SLUG_HEADER, slug);
        }
        return request;
    }

    @Test
    void laPestanaDeUnaClinicaAutenticaConLaCookieDeEsaClinica() throws Exception {
        filter.doFilter(solicitud("clinica-b"), new MockHttpServletResponse(), chain);

        verify(tokenProvider).getAuthentication("token-b");
        verify(tokenProvider, never()).getAuthentication("token-a");
    }

    @Test
    void laOtraPestanaUsaLaSuya() throws Exception {
        filter.doFilter(solicitud("clinica-a"), new MockHttpServletResponse(), chain);

        verify(tokenProvider).getAuthentication("token-a");
        verify(tokenProvider, never()).getAuthentication("token-b");
    }

    @Test
    void conVariasSesionesYSinClinicaDeclaradaNoAdivinaCualUsar() throws Exception {
        filter.doFilter(solicitud(null), new MockHttpServletResponse(), chain);

        verify(tokenProvider, never()).getAuthentication(anyString());
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}

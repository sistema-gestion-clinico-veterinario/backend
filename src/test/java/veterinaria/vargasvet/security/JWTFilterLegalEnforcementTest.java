package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.LegalDocumentService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica que el bloqueo por Términos y Condiciones / Política de Privacidad vencidos
 * (fuera del período de gracia) se aplique a nivel de backend, no solo en el frontend,
 * y que las rutas exentas (/legal/**, logout, refresh) sigan pasando aunque el usuario
 * tenga consentimiento pendiente.
 */
@ExtendWith(MockitoExtension.class)
class JWTFilterLegalEnforcementTest {

    private static final Integer USUARIO_ID = 42;
    private static final String EMAIL = "cliente@vargasvet.com";
    private static final String ACCESS_TOKEN = "token-valido";

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioPorRolRepository usuarioPorRolRepository;

    @Mock
    private LegalDocumentService legalDocumentService;

    @Mock
    private FilterChain filterChain;

    private JWTFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JWTFilter(tokenProvider, usuarioRepository, usuarioPorRolRepository, legalDocumentService);
        SecurityContextHolder.clearContext();
    }

    private HttpServletRequest requestFor(String servletPath) {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn(servletPath);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("access_token", ACCESS_TOKEN) });
        return request;
    }

    private void stubValidSession() {
        Role rol = new Role();
        rol.setId(1);
        rol.setPurpose(RolePurpose.CLIENT_PORTAL);
        rol.setScope(RoleScope.CLIENT);
        rol.setPermissionVersion(1L);

        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setRol(rol);

        UsuarioPrincipal principal = new UsuarioPrincipal(
                USUARIO_ID, EMAIL, "hash", java.util.List.of(), null,
                1, RoleScope.CLIENT, RolePurpose.CLIENT_PORTAL, 1L);
        principal.setCredentialsVersion(0L);

        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, java.util.List.of());

        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setEmail(EMAIL);
        usuario.setActivo(true);
        usuario.setCredentialsVersion(0L);
        Company company = new Company();
        company.setActivo(true);
        usuario.setCompany(company);

        when(tokenProvider.getAuthentication(ACCESS_TOKEN)).thenReturn(authentication);
        when(usuarioPorRolRepository.findActiveAssignmentByUsuarioIdAndRoleId(USUARIO_ID, 1))
                .thenReturn(Optional.of(asignacion));
        when(usuarioRepository.findByEmailWithCompany(EMAIL)).thenReturn(Optional.of(usuario));
    }

    @Test
    void requestNegocio_conConsentimientoVencido_esBloqueadaCon403YCodigoEspecifico() throws Exception {
        stubValidSession();
        when(legalDocumentService.isPastGracePeriod(USUARIO_ID)).thenReturn(true);
        HttpServletRequest request = requestFor("/mascotas");
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertTrue(body.toString().contains("TERMS_NOT_ACCEPTED"));
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void requestNegocio_conConsentimientoDentroDelPeriodoDeGracia_pasaSinBloqueo() throws Exception {
        stubValidSession();
        when(legalDocumentService.isPastGracePeriod(USUARIO_ID)).thenReturn(false);
        HttpServletRequest request = requestFor("/mascotas");
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);

        filter.doFilter(request, response, filterChain);

        verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, times(1)).doFilter(request, response);
        assertFalse(SecurityContextHolder.getContext().getAuthentication() == null);
    }

    @Test
    void endpointLegalAccept_noSeBloqueaAunqueElConsentimientoEsteVencido() throws Exception {
        // La ruta está exenta (isLegalExemptEndpoint), por lo que ni siquiera debe
        // consultarse el servicio legal.
        stubValidSession();
        HttpServletRequest request = requestFor("/legal/accept");
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);

        filter.doFilter(request, response, filterChain);

        verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, times(1)).doFilter(request, response);
        verify(legalDocumentService, never()).isPastGracePeriod(anyInt());
    }

    @Test
    void logout_esUnEndpointPublicoYNuncaLlegaAEvaluarElConsentimientoLegal() throws Exception {
        // /auth/logout ya está en la lista de endpoints públicos del filtro, así que ni
        // siquiera se procesa el token: debe dejar pasar la petición sin invocar al
        // servicio legal (evita bloquear el cierre de sesión de una cuenta ya vencida).
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getServletPath()).thenReturn("/auth/logout");
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(legalDocumentService, never()).isPastGracePeriod(anyInt());
    }
}

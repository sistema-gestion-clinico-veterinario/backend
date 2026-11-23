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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.CannotCreateTransactionException;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.LegalDocumentService;
import veterinaria.vargasvet.service.SessionActivityService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Una base caída no es una sesión inválida: el filtro responde 503 en vez de 401. */
@ExtendWith(MockitoExtension.class)
class JWTFilterBaseCaidaTest {

    @Mock TokenProvider tokenProvider;
    @Mock UsuarioRepository usuarioRepository;
    @Mock UsuarioPorRolRepository usuarioPorRolRepository;
    @Mock UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock LegalDocumentService legalDocumentService;
    @Mock SessionActivityService sessionActivityService;
    @Mock FilterChain chain;

    private JWTFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JWTFilter(tokenProvider, usuarioRepository, usuarioPorRolRepository, credencialRepository,
                org.mockito.Mockito.mock(veterinaria.vargasvet.repository.ApoderadoRepository.class),
                org.mockito.Mockito.mock(veterinaria.vargasvet.service.ConsentimientoDatosService.class),
                legalDocumentService, sessionActivityService);
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest("GET", "/api/v1/mascotas");
        request.setServletPath("/mascotas");
        request.setCookies(new Cookie("access_token", "token"));
        response = new MockHttpServletResponse();

        UsuarioPrincipal principal = new UsuarioPrincipal(
                42, "ana@example.test", "", List.of(), 7, 1, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 1L);
        principal.setCredentialsVersion(0L);
        when(tokenProvider.getAuthentication("token"))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void conLaBaseCaidaResponde503ConReintentoYNoDejaPasarLaPeticion() throws Exception {
        when(usuarioPorRolRepository.findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(any(), any(), any()))
                .thenThrow(new CannotCreateTransactionException("Could not open JPA EntityManager for transaction"));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Retry-After")).isEqualTo("5");
        assertThat(response.getContentAsString()).contains("temporalmente");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void unaConexionDelPoolMatadaPorElApagadoDelServidorResponde503() throws Exception {
        when(usuarioPorRolRepository.findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(any(), any(), any()))
                .thenThrow(IndisponibilidadPorCodigoSqlTest.comoLlegaAlFiltro("57P01"));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Retry-After")).isEqualTo("5");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void unaSesionRealmenteInvalidaSigueSinAutenticarYPasaAlResponsable401() throws Exception {
        when(usuarioPorRolRepository.findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(any(), any(), any()))
                .thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}

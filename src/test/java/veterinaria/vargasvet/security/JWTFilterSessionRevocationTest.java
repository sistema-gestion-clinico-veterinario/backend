package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.LegalDocumentService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Cerrar sesión (o que se revoque) deja sin efecto también el access token que ya circula. */
@ExtendWith(MockitoExtension.class)
class JWTFilterSessionRevocationTest {

    private static final String ACCESS_TOKEN = "token";

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

    private HttpServletRequest request() {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/mascotas");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", ACCESS_TOKEN)});
        return request;
    }

    private void stubSession(String sessionId) {
        Role role = new Role();
        role.setId(1);
        role.setPurpose(RolePurpose.COMPANY_ADMIN);
        role.setScope(RoleScope.STAFF);
        role.setPermissionVersion(1L);
        UsuarioPorRol assignment = new UsuarioPorRol();
        assignment.setRol(role);

        UsuarioPrincipal principal = new UsuarioPrincipal(
                42, "ana@example.test", "", List.of(), 7, 1, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 1L);
        principal.setCredentialsVersion(0L);
        principal.setSessionId(sessionId);

        Company company = new Company();
        company.setActivo(true);
        Usuario usuario = new Usuario();
        usuario.setId(42);
        usuario.setActivo(true);
        usuario.setCompany(company);
        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setCredentialsVersion(0L);

        when(tokenProvider.getAuthentication(ACCESS_TOKEN))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(usuarioPorRolRepository.findActiveAssignmentByUsuarioIdAndRoleId(42, 1)).thenReturn(Optional.of(assignment));
        when(usuarioRepository.findByIdWithCompany(42)).thenReturn(Optional.of(usuario));
        when(credencialRepository.findByUsuarioIdAndCompanyId(42, 7)).thenReturn(Optional.of(credencial));
    }

    @Test
    void unAccessTokenDeUnaSesionCerradaYaNoAutentica() throws Exception {
        stubSession("sesion-cerrada");
        when(refreshTokenRepository.existsByFamilyIdAndRevokedAtIsNull("sesion-cerrada")).thenReturn(false);
        HttpServletRequest request = request();

        filter.doFilter(request, org.mockito.Mockito.mock(HttpServletResponse.class), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void unAccessTokenDeUnaSesionAbiertaAutentica() throws Exception {
        stubSession("sesion-abierta");
        when(refreshTokenRepository.existsByFamilyIdAndRevokedAtIsNull("sesion-abierta")).thenReturn(true);
        when(legalDocumentService.isPastGracePeriod(42)).thenReturn(false);
        HttpServletRequest request = request();

        filter.doFilter(request, org.mockito.Mockito.mock(HttpServletResponse.class), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void unTokenEmitidoAntesDelCampoDeSesionSigueValidoHastaVencer() throws Exception {
        stubSession(null);
        when(legalDocumentService.isPastGracePeriod(42)).thenReturn(false);
        HttpServletRequest request = request();

        filter.doFilter(request, org.mockito.Mockito.mock(HttpServletResponse.class), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verifyNoInteractions(refreshTokenRepository);
        verify(chain, never()).doFilter(any(), org.mockito.ArgumentMatchers.isNull());
    }
}

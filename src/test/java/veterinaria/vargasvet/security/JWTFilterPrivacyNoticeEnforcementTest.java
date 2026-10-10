package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.AudienciaAvisoPrivacidad;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.ConsentimientoDatosService;
import veterinaria.vargasvet.service.LegalDocumentService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JWTFilterPrivacyNoticeEnforcementTest {

    private static final int USUARIO_ID = 42;
    private static final int COMPANY_ID = 7;
    private static final String TOKEN = "token-valido";

    @Mock private TokenProvider tokenProvider;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioPorRolRepository usuarioPorRolRepository;
    @Mock private UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock private ConsentimientoDatosService consentimientoDatosService;
    @Mock private LegalDocumentService legalDocumentService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private FilterChain chain;

    private JWTFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JWTFilter(tokenProvider, usuarioRepository, usuarioPorRolRepository, credencialRepository,
                consentimientoDatosService, legalDocumentService, refreshTokenRepository);
        SecurityContextHolder.clearContext();
        sesionValida();
    }

    @Test
    void unaOperacionDeNegocioSeBloqueaEnBackendMientrasElAvisoEstePendiente() throws Exception {
        when(consentimientoDatosService.requiereLecturaPersonal(USUARIO_ID, COMPANY_ID,
                AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS)).thenReturn(true);
        MockHttpServletRequest request = request("GET", "/mascotas");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(428);
        assertThat(response.getContentAsString()).contains("PRIVACY_NOTICE_REQUIRED");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void sePuedeConsultarYConfirmarElAvisoAunqueEstePendiente() throws Exception {
        filter.doFilter(request("GET", "/privacidad/aviso-vigente"), new MockHttpServletResponse(), chain);
        filter.doFilter(request("GET", "/privacidad/mi-estado"), new MockHttpServletResponse(), chain);
        filter.doFilter(request("POST", "/privacidad/enterado"), new MockHttpServletResponse(), chain);

        verify(chain, org.mockito.Mockito.times(3)).doFilter(any(), any());
    }

    @Test
    void losDocumentosDeSoftVetNoSeAnteponenAlAvisoDeLaClinica() throws Exception {
        when(consentimientoDatosService.requiereLecturaPersonal(USUARIO_ID, COMPANY_ID,
                AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS)).thenReturn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/legal/status"), response, chain);

        assertThat(response.getStatus()).isEqualTo(428);
        assertThat(response.getContentAsString()).contains("PRIVACY_NOTICE_REQUIRED");
        verify(legalDocumentService, never()).isPastGracePeriod(USUARIO_ID);
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.setCookies(new Cookie("access_token", TOKEN));
        return request;
    }

    private void sesionValida() {
        Company company = new Company();
        company.setId(COMPANY_ID);
        company.setSlug("clinica-a");
        company.setActivo(true);

        Role role = new Role();
        role.setId(1);
        role.setCompany(company);
        role.setPurpose(RolePurpose.CLIENT_PORTAL);
        role.setScope(RoleScope.CLIENT);
        role.setPermissionVersion(1L);

        UsuarioPorRol assignment = new UsuarioPorRol();
        assignment.setRol(role);
        assignment.setCompany(company);

        UsuarioPrincipal principal = new UsuarioPrincipal(USUARIO_ID, "cliente@example.test", null, List.of(),
                COMPANY_ID, 1, RoleScope.CLIENT, RolePurpose.CLIENT_PORTAL, 1L);
        principal.setCredentialsVersion(0L);

        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setActivo(true);
        usuario.setCompany(company);

        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setCredentialsVersion(0L);

        when(tokenProvider.getAuthentication(TOKEN)).thenReturn(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(usuarioPorRolRepository.findActiveAssignmentByUsuarioIdAndRoleId(USUARIO_ID, 1))
                .thenReturn(Optional.of(assignment));
        when(usuarioRepository.findByIdWithCompany(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(credencialRepository.findByUsuarioIdAndCompanyId(USUARIO_ID, COMPANY_ID))
                .thenReturn(Optional.of(credencial));
    }
}

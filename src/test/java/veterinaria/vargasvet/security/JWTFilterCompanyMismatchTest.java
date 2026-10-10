package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
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
import static org.mockito.Mockito.when;

/**
 * Un navegador solo guarda una sesión a la vez. Una pestaña que sigue mostrando la clínica A mientras la sesión
 * del navegador pasó a la clínica B no debe poder operar sobre B creyendo que está en A.
 */
@ExtendWith(MockitoExtension.class)
class JWTFilterCompanyMismatchTest {

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
                org.mockito.Mockito.mock(veterinaria.vargasvet.service.ConsentimientoDatosService.class),
                legalDocumentService, refreshTokenRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private HttpServletRequest request(String declaredCompany) {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/mascotas");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", ACCESS_TOKEN)});
        when(request.getHeader(JWTFilter.COMPANY_HEADER)).thenReturn(declaredCompany);
        org.mockito.Mockito.lenient().when(request.getHeader(SessionCookies.SLUG_HEADER)).thenReturn(null);
        return request;
    }

    private void sesion(Integer companyId, RolePurpose purpose) {
        sesion(companyId, purpose, true, true);
    }

    private void sesion(Integer companyId, RolePurpose purpose, boolean rolConClinica, boolean asignacionConClinica) {
        Role role = new Role();
        role.setId(1);
        role.setPurpose(purpose);
        role.setScope(purpose == RolePurpose.PLATFORM_ADMIN ? RoleScope.PLATFORM : RoleScope.STAFF);
        role.setPermissionVersion(1L);
        UsuarioPorRol assignment = new UsuarioPorRol();
        assignment.setRol(role);
        UsuarioPrincipal principal = new UsuarioPrincipal(
                42, "ana@example.test", "", List.of(), companyId, 1, role.getScope(), purpose, 1L);
        principal.setCredentialsVersion(0L);
        Company company = new Company();
        company.setId(companyId);
        company.setSlug(companyId == null ? null : "clinica-a");
        company.setActivo(true);
        role.setCompany(companyId == null || !rolConClinica ? null : company);
        assignment.setCompany(companyId == null || !asignacionConClinica ? null : company);
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
        if (companyId == null) {
            when(credencialRepository.findByUsuarioIdAndCompanyIsNull(42)).thenReturn(Optional.of(credencial));
        } else {
            when(credencialRepository.findByUsuarioIdAndCompanyId(42, companyId)).thenReturn(Optional.of(credencial));
        }
        org.mockito.Mockito.lenient().when(legalDocumentService.isPastGracePeriod(42)).thenReturn(false);
    }

    @Test
    void siLaPestanaDiceQueEstaEnOtraClinicaSeRechazaConUnAvisoClaro() throws Exception {
        sesion(7, RolePurpose.COMPANY_ADMIN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("3"), response, chain);

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("SESSION_COMPANY_MISMATCH").contains("otra clínica");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void siLaClinicaCoincideAutenticaNormalmente() throws Exception {
        sesion(7, RolePurpose.COMPANY_ADMIN);

        filter.doFilter(request(" 7 "), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void sinLaCabeceraLosOtrosClientesSiguenFuncionando() throws Exception {
        sesion(7, RolePurpose.COMPANY_ADMIN);

        filter.doFilter(request(null), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void laCuentaDePlataformaSinClinicaDeclaradaAutenticaNormalmente() throws Exception {
        sesion(null, RolePurpose.PLATFORM_ADMIN);

        filter.doFilter(request(null), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void unaSesionDePlataformaConservaLaEmpresaActivaEnLaSolicitud() throws Exception {
        sesion(null, RolePurpose.PLATFORM_ADMIN);
        HttpServletRequest request = request("3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(request).setAttribute(ActiveCompanyContext.REQUEST_ATTRIBUTE, 3);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void rechazaUnaEmpresaActivaConFormatoInvalido() throws Exception {
        sesion(null, RolePurpose.PLATFORM_ADMIN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("abc"), response, chain);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("INVALID_ACTIVE_COMPANY");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void unaSesionDePlataformaNoAutenticaUnaSolicitudQueDeclaraSoloElSlugDeUnaClinica() throws Exception {
        sesion(null, RolePurpose.PLATFORM_ADMIN);
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/mascotas");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + ACCESS_TOKEN);
        when(request.getHeader(SessionCookies.SLUG_HEADER)).thenReturn("clinica-a");
        org.mockito.Mockito.lenient().when(request.getHeader(JWTFilter.COMPANY_HEADER)).thenReturn(null);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private HttpServletRequest requestConSlug(String slug) {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/me/navigation");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token__" + slug, ACCESS_TOKEN)});
        when(request.getHeader(SessionCookies.SLUG_HEADER)).thenReturn(slug);
        org.mockito.Mockito.lenient().when(request.getHeader(JWTFilter.COMPANY_HEADER)).thenReturn("7");
        return request;
    }

    @Test
    void unRolGeneralSinClinicaAutenticaPorLaClinicaDeSuAsignacion() throws Exception {
        sesion(7, RolePurpose.COMPANY_ADMIN, false, true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestConSlug("clinica-a"), response, chain);

        assertThat(response.getStatus()).isNotEqualTo(409);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void unRolGeneralTampocoDejaUsarLaSesionBajoElNombreDeOtraClinica() throws Exception {
        sesion(7, RolePurpose.COMPANY_ADMIN, false, true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestConSlug("clinica-b"), response, chain);

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("SESSION_COMPANY_MISMATCH");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void sinClinicaEnLaAsignacionNiEnElRolSeUsaLaDelUsuario() throws Exception {
        sesion(7, RolePurpose.COMPANY_ADMIN, false, false);

        filter.doFilter(requestConSlug("clinica-a"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void siLaUrlDeclaraOtroSlugAunqueNoEnvieCompanyIdSeRechaza() throws Exception {
        sesion(7, RolePurpose.COMPANY_ADMIN);
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/profile");
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("access_token__clinica-b", ACCESS_TOKEN)
        });
        when(request.getHeader(SessionCookies.SLUG_HEADER)).thenReturn("clinica-b");
        org.mockito.Mockito.lenient().when(request.getHeader(JWTFilter.COMPANY_HEADER)).thenReturn(null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("SESSION_COMPANY_MISMATCH");
        verify(chain, never()).doFilter(any(), any());
    }
}

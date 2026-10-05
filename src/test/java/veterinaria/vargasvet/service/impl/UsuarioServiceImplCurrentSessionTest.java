package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.RealtimeSubscriptionGuard;
import veterinaria.vargasvet.security.TokenProvider;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.LegalDocumentService;
import veterinaria.vargasvet.service.MenuBuilderService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Estado de la sesión vigente para otra pestaña: se arma con lo que el servidor sabe del
 * access token, sin emitir ni rotar tokens ni tocar ninguna sesión.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplCurrentSessionTest {

    private static final int COMPANY_ID = 3;

    @Mock UsuarioRepository usuarioRepository;
    @Mock CompanyRepository companyRepository;
    @Mock UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock RealtimeSubscriptionGuard realtimeSubscriptionGuard;
    @Mock TokenProvider tokenProvider;
    @Mock MenuBuilderService menuBuilderService;
    @Mock LegalDocumentService legalDocumentService;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock ApoderadoRepository apoderadoRepository;

    @InjectMocks UsuarioServiceImpl service;

    private Usuario usuario;
    private Role veterinario;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setId(COMPANY_ID);
        company.setName("Vargas Vet");
        company.setSlug("vargas-vet");

        usuario = new Usuario();
        usuario.setId(10);
        usuario.setEmail("ana@example.test");
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        usuario.setActivo(true);

        Role admin = rol(1, "ROLE_ADMIN", RolePurpose.COMPANY_ADMIN);
        veterinario = rol(2, "ROLE_VETERINARIO", RolePurpose.CUSTOM);
        asignar(admin, company);
        asignar(veterinario, company);

        UsuarioPrincipal principal = new UsuarioPrincipal(10, "ana@example.test", "",
                List.of(new SimpleGrantedAuthority("ROLE_VETERINARIO")), COMPANY_ID,
                2, RoleScope.STAFF, RolePurpose.CUSTOM, 0L);
        principal.setSessionId("sesion-actual");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setPasswordChanged(true);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, COMPANY_ID)).thenReturn(Optional.of(credencial));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Role rol(int id, String nombre, RolePurpose purpose) {
        Role role = new Role();
        role.setId(id);
        role.setName(nombre);
        role.setActivo(true);
        role.setScope(RoleScope.STAFF);
        role.setPurpose(purpose);
        return role;
    }

    private void asignar(Role role, Company empresa) {
        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setUsuario(usuario);
        asignacion.setRol(role);
        asignacion.setCompany(empresa);
        usuario.getUsuariosPorRol().add(asignacion);
    }

    @Test
    void devuelveElRolActivoDelTokenConSusRolesDisponiblesYEmpresa() {
        when(menuBuilderService.construirPermissions(10, 2)).thenReturn(List.of("VER_CITAS"));

        AuthResponse response = service.currentSession(10);

        assertThat(response.getActiveRoleId()).isEqualTo(2);
        assertThat(response.getRoles()).containsExactly("ROLE_VETERINARIO");
        assertThat(response.getAssignedRoles()).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_VETERINARIO");
        assertThat(response.getAvailableRoles()).hasSize(2);
        assertThat(response.getCompanySlug()).isEqualTo("vargas-vet");
        assertThat(response.getPermissions()).containsExactly("VER_CITAS");
        assertThat(response.getNombreCompleto()).isEqualTo("Ana Pérez");
    }

    @Test
    void noEmiteNiRotaTokensNiTocaLaSesion() {
        service.currentSession(10);

        verifyNoInteractions(tokenProvider, refreshTokenRepository, realtimeSubscriptionGuard);
    }

    @Test
    void noMarcaTokensEnLaRespuesta() {
        AuthResponse response = service.currentSession(10);

        assertThat(response.getToken()).isNull();
        assertThat(response.getRefreshToken()).isNull();
    }

    @Test
    void siElRolActivoDelTokenFueDesactivadoRechazaEnVezDeSustituirloPorOtro() {
        veterinario.setActivo(false);

        assertThatThrownBy(() -> service.currentSession(10))
                .isInstanceOf(DisabledException.class)
                .hasMessage("El rol de la sesión ya no está disponible");
    }

    @Test
    void siElRolDelTokenYaNoEstaAsignadoALaCuentaRechazaLaSesion() {
        usuario.getUsuariosPorRol().removeIf(upr -> upr.getRol().getId() == 2);

        assertThatThrownBy(() -> service.currentSession(10))
                .isInstanceOf(DisabledException.class);
    }
}

package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.RefreshToken;
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
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.LegalDocumentService;
import veterinaria.vargasvet.service.MenuBuilderService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Cambio de rol activo: solo entre los roles de la cuenta en la empresa de la sesión, sin
 * sustituir en silencio un rol ajeno por otro, y reemplazando únicamente la sesión actual
 * (sin cerrar las de otros dispositivos ni renovar el límite absoluto de la sesión).
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplSwitchRoleTest {

    private static final int COMPANY_ID = 3;
    private static final String SESION_ACTUAL = "sesion-actual";

    @Mock UsuarioRepository usuarioRepository;
    @Mock CompanyRepository companyRepository;
    @Mock UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock RealtimeSubscriptionGuard realtimeSubscriptionGuard;
    @Mock TokenProvider tokenProvider;
    @Mock MenuBuilderService menuBuilderService;
    @Mock LegalDocumentService legalDocumentService;
    @Mock AuditLogService auditLogService;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock ApoderadoRepository apoderadoRepository;

    @InjectMocks UsuarioServiceImpl service;

    private Usuario usuario;
    private Company company;
    private Role admin;
    private Role veterinario;
    private RefreshToken sesionActual;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(COMPANY_ID);
        company.setName("Vargas Vet");
        company.setSlug("vargas-vet");

        usuario = new Usuario();
        usuario.setId(10);
        usuario.setEmail("ana@example.test");
        usuario.setActivo(true);

        admin = rol(1, "ROLE_ADMIN", RolePurpose.COMPANY_ADMIN);
        veterinario = rol(2, "ROLE_VETERINARIO", RolePurpose.CUSTOM);
        asignar(admin, company);
        asignar(veterinario, company);

        sesionActual = RefreshToken.builder()
                .usuario(usuario)
                .familyId(SESION_ACTUAL)
                .sessionStartedAt(Instant.now().minus(5, ChronoUnit.HOURS))
                .expiryDate(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();

        ReflectionTestUtils.setField(service, "absoluteTimeoutSeconds", 86400L);
        iniciarSesionComo("ROLE_ADMIN", SESION_ACTUAL);

        lenient().when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
        lenient().when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        lenient().when(refreshTokenRepository.findActiveByFamilyIdForUpdate(SESION_ACTUAL))
                .thenReturn(List.of(sesionActual));
        lenient().when(refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull(SESION_ACTUAL))
                .thenReturn(List.of(sesionActual));
        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setPasswordChanged(true);
        credencial.setCredentialsVersion(0L);
        lenient().when(credencialRepository.findByUsuarioIdAndCompanyId(10, COMPANY_ID))
                .thenReturn(Optional.of(credencial));
        lenient().when(tokenProvider.createToken(any(), any(), any(), any(), any(), any(), any(),
                anyLong(), anyLong(), anyString())).thenReturn("access-token");
        lenient().when(tokenProvider.createRefreshToken(anyString(), any(), any(), anyString(), anyLong()))
                .thenReturn("refresh-token");
        lenient().when(tokenProvider.getRefreshTokenDetails("refresh-token"))
                .thenReturn(new TokenProvider.RefreshTokenDetails("ana@example.test", "jti-nuevo", "familia", null, null, 0L));
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

    private UsuarioPorRol asignar(Role role, Company empresa) {
        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setUsuario(usuario);
        asignacion.setRol(role);
        asignacion.setCompany(empresa);
        usuario.getUsuariosPorRol().add(asignacion);
        return asignacion;
    }

    private void iniciarSesionComo(String rolActual, String sessionId) {
        UsuarioPrincipal principal = new UsuarioPrincipal(10, "ana@example.test", "",
                List.of(new SimpleGrantedAuthority(rolActual)), COMPANY_ID,
                1, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 0L);
        principal.setSessionId(sessionId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void cambiaAlRolPedidoYEntregaUnaSesionNueva() {
        AuthResponse response = service.switchRole(10, 2);

        assertThat(response.getActiveRoleId()).isEqualTo(2);
        assertThat(response.getRoles()).containsExactly("ROLE_VETERINARIO");
        assertThat(response.getCompanyId()).isEqualTo(COMPANY_ID);
    }

    @Test
    void rechazaUnRolQueNoPerteneceALaCuentaSinSustituirloPorOtro() {
        assertThatThrownBy(() -> service.switchRole(10, 999))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("El rol seleccionado no pertenece a tu cuenta");

        verifyNoInteractions(refreshTokenRepository, realtimeSubscriptionGuard);
        verify(tokenProvider, never()).createToken(any(), any(), any(), any(), any(), any(), any(),
                anyLong(), anyLong(), anyString());
    }

    @Test
    void rechazaUnRolAsignadoEnOtraEmpresa() {
        Company otraEmpresa = new Company();
        otraEmpresa.setId(99);
        Role roleDeOtraEmpresa = rol(50, "ROLE_RECEPCION", RolePurpose.CUSTOM);
        asignar(roleDeOtraEmpresa, otraEmpresa);

        assertThatThrownBy(() -> service.switchRole(10, 50))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void unaSesionSinEmpresaSoloPuedeUsarRolesSinEmpresa() {
        Role plataforma = rol(100, "ROLE_SUPERADMIN", RolePurpose.PLATFORM_ADMIN);
        asignar(plataforma, null);
        UsuarioPrincipal principal = new UsuarioPrincipal(10, "ana@example.test", "",
                List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN")), null,
                100, RoleScope.PLATFORM, RolePurpose.PLATFORM_ADMIN, 0L);
        principal.setSessionId(SESION_ACTUAL);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        assertThatThrownBy(() -> service.switchRole(10, 2))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(realtimeSubscriptionGuard);
    }

    @Test
    void rechazaUnRolDeLaCuentaQueFueDesactivado() {
        veterinario.setActivo(false);

        assertThatThrownBy(() -> service.switchRole(10, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El rol seleccionado se encuentra desactivado");

        verifyNoInteractions(refreshTokenRepository);
    }

    private Role crearRolCliente() {
        Role cliente = rol(3, "ROLE_CLIENTE", RolePurpose.CLIENT_PORTAL);
        cliente.setScope(RoleScope.CLIENT);
        asignar(cliente, company);
        return cliente;
    }

    @Test
    void unEmpleadoSuspendidoQueSigueSiendoClienteNoRecuperaSusRolesDePersonal() {
        crearRolCliente();
        when(empleadoRepository.existsByUserIdAndCompanyId(10, COMPANY_ID)).thenReturn(true);
        when(apoderadoRepository.existsByUserIdAndCompanyId(10, COMPANY_ID)).thenReturn(true);
        when(empleadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(10, COMPANY_ID)).thenReturn(false);
        when(apoderadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(10, COMPANY_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.switchRole(10, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El rol seleccionado se encuentra desactivado");
        assertThatThrownBy(() -> service.switchRole(10, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El rol seleccionado se encuentra desactivado");
    }

    @Test
    void unEmpleadoSuspendidoQueSigueSiendoClientePuedeCambiarASuRolDeCliente() {
        crearRolCliente();
        when(empleadoRepository.existsByUserIdAndCompanyId(10, COMPANY_ID)).thenReturn(true);
        when(apoderadoRepository.existsByUserIdAndCompanyId(10, COMPANY_ID)).thenReturn(true);
        when(empleadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(10, COMPANY_ID)).thenReturn(false);
        when(apoderadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(10, COMPANY_ID)).thenReturn(true);

        AuthResponse response = service.switchRole(10, 3);

        assertThat(response.getRoles()).containsExactly("ROLE_CLIENTE");
        assertThat(response.getAssignedRoles()).containsExactly("ROLE_CLIENTE");
    }

    @Test
    void unClienteSuspendidoQueSigueSiendoEmpleadoNoPuedeUsarElRolDeCliente() {
        crearRolCliente();
        when(empleadoRepository.existsByUserIdAndCompanyId(10, COMPANY_ID)).thenReturn(true);
        when(apoderadoRepository.existsByUserIdAndCompanyId(10, COMPANY_ID)).thenReturn(true);
        when(empleadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(10, COMPANY_ID)).thenReturn(true);
        when(apoderadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(10, COMPANY_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.switchRole(10, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El rol seleccionado se encuentra desactivado");
        assertThat(service.switchRole(10, 2).getRoles()).containsExactly("ROLE_VETERINARIO");
    }

    @Test
    void rechazaElRolSiLaPersonaYaNoEsEmpleadaActivaDeLaEmpresa() {
        when(empleadoRepository.existsByUserIdAndCompanyId(10, COMPANY_ID)).thenReturn(true);
        when(empleadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(10, COMPANY_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.switchRole(10, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reemplazaSoloLaSesionActualYCierraSusConexionesEnTiempoReal() {
        service.switchRole(10, 2);

        assertThat(sesionActual.getRevokedAt()).isNotNull();
        verify(realtimeSubscriptionGuard).closeConnectionsOfSession(SESION_ACTUAL);
        verify(refreshTokenRepository, never()).findAllByUsuarioAndCompanyAndRevokedAtIsNull(any(), any());
        verify(refreshTokenRepository, never()).findAllByUsuarioAndRevokedAtIsNull(any());
    }

    @Test
    void laSesionNuevaFirmaAccessYRefreshConUnIdentificadorPropioDistintoAlAnterior() {
        ArgumentCaptor<String> sesionDelAccess = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sesionDelRefresh = ArgumentCaptor.forClass(String.class);
        when(tokenProvider.createToken(any(), any(), any(), any(), any(), any(), any(),
                anyLong(), anyLong(), sesionDelAccess.capture())).thenReturn("access-token");
        when(tokenProvider.createRefreshToken(anyString(), any(), any(), sesionDelRefresh.capture(), anyLong()))
                .thenReturn("refresh-token");

        service.switchRole(10, 2);

        assertThat(sesionDelAccess.getValue()).isNotBlank().isNotEqualTo(SESION_ACTUAL)
                .isEqualTo(sesionDelRefresh.getValue());
    }

    @Test
    void cambiarDeRolNoRenuevaElInicioDeLaSesion() {
        Instant inicioReal = sesionActual.getSessionStartedAt();
        // Otra sesión más reciente (otro dispositivo): no debe influir en la heredada.
        lenient().when(refreshTokenRepository.findAllByUsuarioAndRevokedAtIsNull(usuario)).thenReturn(List.of(
                RefreshToken.builder().usuario(usuario).familyId("otro-dispositivo")
                        .sessionStartedAt(Instant.now()).expiryDate(Instant.now().plus(2, ChronoUnit.DAYS)).build()));

        service.switchRole(10, 2);

        ArgumentCaptor<RefreshToken> guardado = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(guardado.capture());
        assertThat(guardado.getValue().getSessionStartedAt()).isEqualTo(inicioReal);
        assertThat(guardado.getValue().getFamilyId()).isNotEqualTo(SESION_ACTUAL);
    }

    @Test
    void rechazaElCambioCuandoLaSesionYaSuperoElLimiteAbsoluto() {
        sesionActual.setSessionStartedAt(Instant.now().minus(25, ChronoUnit.HOURS));

        assertThatThrownBy(() -> service.switchRole(10, 2))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("expirado");

        assertThat(sesionActual.getRevokedAt()).isNotNull();
    }

    @Test
    void rechazaElCambioSiLaSesionYaNoEstaActiva() {
        when(refreshTokenRepository.findActiveByFamilyIdForUpdate(SESION_ACTUAL)).thenReturn(List.of());

        assertThatThrownBy(() -> service.switchRole(10, 2))
                .isInstanceOf(BadCredentialsException.class);

        verify(realtimeSubscriptionGuard, never()).closeConnectionsOfSession(anyString());
    }

    @Test
    void rechazaUnaSesionQueEsDeOtraPersona() {
        Usuario otra = new Usuario();
        otra.setId(77);
        sesionActual.setUsuario(otra);

        assertThatThrownBy(() -> service.switchRole(10, 2))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(sesionActual.getRevokedAt()).isNull();
    }

    @Test
    void conUnAccessTokenSinIdentificadorDeSesionPideRenovarYNoTocaNada() {
        iniciarSesionComo("ROLE_ADMIN", null);

        assertThatThrownBy(() -> service.switchRole(10, 2))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("renovarse");

        verifyNoInteractions(refreshTokenRepository, realtimeSubscriptionGuard);
    }

    @Test
    void laAuditoriaRegistraElRolAnteriorYElNuevo() {
        service.switchRole(10, 2);

        verify(auditLogService).log(eq("ana@example.test"), eq("ROLE_VETERINARIO"), eq(COMPANY_ID), eq("Vargas Vet"),
                eq("CAMBIO_ROL"), eq("Seguridad"),
                eq("Cambio de rol activo del usuario de ROLE_ADMIN a ROLE_VETERINARIO"), any());
    }
}

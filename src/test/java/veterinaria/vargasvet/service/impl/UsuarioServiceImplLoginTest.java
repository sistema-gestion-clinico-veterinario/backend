package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.AccountLockoutService;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.security.TokenProvider;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.AuthenticationAuditService;
import veterinaria.vargasvet.service.CompanyMembershipService;
import veterinaria.vargasvet.service.LegalDocumentService;
import veterinaria.vargasvet.service.MenuBuilderService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Cubre login(): aislamiento total entre empresas - ya no existe login "global" sin
 * slug (se rechaza siempre), y el username/correo puede repetirse entre empresas sin
 * relacion entre si, asi que el login desambigua quedandose con el candidato que tiene
 * membresia activa en la empresa del slug.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplLoginTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock veterinaria.vargasvet.service.AccountClosureGuard accountClosureGuard;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CompanyRepository companyRepository;
    @Mock CompanyMembershipService companyMembershipService;
    @Mock TokenProvider tokenProvider;
    @Mock SharedRateLimitService sharedRateLimitService;
    @Mock AccountLockoutService accountLockoutService;
    @Mock AuthenticationAuditService authenticationAuditService;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock veterinaria.vargasvet.security.RealtimeSubscriptionGuard realtimeSubscriptionGuard;
    @Mock MenuBuilderService menuBuilderService;
    @Mock LegalDocumentService legalDocumentService;
    @Mock AuditLogService auditLogService;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock ApoderadoRepository apoderadoRepository;
    @Mock veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository credencialRepository;

    @InjectMocks UsuarioServiceImpl service;

    private Usuario usuarioValido() {
        Usuario usuario = new Usuario();
        usuario.setId(10);
        usuario.setUsername("ana.qa");
        usuario.setEmail("ana@example.test");
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        return usuario;
    }

    private veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencialValida() {
        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                new veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial();
        credencial.setPassword("hash-almacenado");
        credencial.setPasswordChanged(true);
        credencial.setCredentialsVersion(0L);
        return credencial;
    }

    private Usuario conRolEnLaEmpresa(Usuario usuario, Company company) {
        veterinaria.vargasvet.domain.entity.Role rol = new veterinaria.vargasvet.domain.entity.Role();
        rol.setId(3);
        rol.setName("ROLE_VETERINARIO");
        rol.setActivo(true);
        rol.setScope(veterinaria.vargasvet.domain.enums.RoleScope.STAFF);
        rol.setPurpose(veterinaria.vargasvet.domain.enums.RolePurpose.CUSTOM);
        veterinaria.vargasvet.domain.entity.UsuarioPorRol asignacion = new veterinaria.vargasvet.domain.entity.UsuarioPorRol();
        asignacion.setUsuario(usuario);
        asignacion.setRol(rol);
        asignacion.setCompany(company);
        usuario.getUsuariosPorRol().add(asignacion);
        return usuario;
    }

    private Company vargasVet() {
        Company company = new Company();
        company.setId(7);
        company.setName("Vargas Vet");
        company.setSlug("vargas-vet");
        company.setActivo(true);
        return company;
    }

    private void tokensDeSesion() {
        when(tokenProvider.createToken(any(), any(), any(), any(), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn("access-token");
        when(tokenProvider.createRefreshToken(anyString(), any(), any(), anyString(), anyLong()))
                .thenReturn("refresh-token");
        when(tokenProvider.getRefreshTokenDetails("refresh-token"))
                .thenReturn(new TokenProvider.RefreshTokenDetails("ana@example.test", "jti-1", "family-1", null, null, 0L));
    }

    private LoginDTO loginConSlug(String slug) {
        LoginDTO dto = new LoginDTO();
        dto.setSlug(slug);
        dto.setUsername("ana.qa");
        dto.setPassword("Password-123");
        return dto;
    }

    @Test
    void rechazaLoginSinSlug() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("ana.qa");
        dto.setPassword("Password-123");

        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verifyNoInteractions(companyRepository, usuarioRepository);
    }

    @Test
    void rechazaLoginCuandoElSlugNoCorrespondeAUnaEmpresa() {
        when(companyRepository.findBySlug("empresa-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(loginConSlug("empresa-inexistente")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void rechazaLoginCuandoNingunCandidatoTieneMembresiaActivaEnEsaEmpresa() {
        Company company = new Company();
        company.setId(7);
        company.setSlug("vargas-vet");
        Usuario usuario = usuarioValido();

        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(usuarioRepository.findAllByUsernameIgnoreCase("ana.qa")).thenReturn(List.of(usuario));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(false);
        when(usuarioRepository.findAllByEmailIgnoreCase("ana.qa")).thenReturn(List.of());

        assertThatThrownBy(() -> service.login(loginConSlug("vargas-vet")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verify(credencialRepository, never()).findByUsuarioIdAndCompanyId(any(), any());
    }

    @Test
    void desambiguaEntreDosCandidatosConElMismoUsernameEnEmpresasDistintasPorMembresiaActiva() {
        // El username ya no es unico globalmente - puede existir "ana.qa" en dos
        // empresas sin relacion entre si. El login se queda con el que SI tiene
        // membresia activa en la empresa del slug, nunca con el primero que aparezca.
        Company company = new Company();
        company.setId(7);
        company.setName("Vargas Vet");
        company.setSlug("vargas-vet");
        company.setActivo(true);

        Usuario deOtraEmpresa = usuarioValido();
        deOtraEmpresa.setId(11);
        Usuario elCorrecto = conRolEnLaEmpresa(usuarioValido(), company);
        elCorrecto.setId(10);

        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial = credencialValida();

        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(usuarioRepository.findAllByUsernameIgnoreCase("ana.qa")).thenReturn(List.of(deOtraEmpresa, elCorrecto));
        when(companyMembershipService.hasActiveMembership(11, 7)).thenReturn(false);
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("Password-123", "hash-almacenado")).thenReturn(true);
        when(tokenProvider.createToken(any(), any(), any(), any(), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn("access-token");
        when(tokenProvider.createRefreshToken(anyString(), any(), any(), anyString(), anyLong()))
                .thenReturn("refresh-token");
        when(tokenProvider.getRefreshTokenDetails("refresh-token"))
                .thenReturn(new TokenProvider.RefreshTokenDetails("ana@example.test", "jti-1", "family-1", null, null, 0L));

        AuthResponse response = service.login(loginConSlug("vargas-vet"));

        assertThat(response.getCompanyId()).isEqualTo(7);
        assertThat(response.getCompanyName()).isEqualTo("Vargas Vet");
    }

    @Test
    void elAccessTokenYElRefreshTokenNacenDeLaMismaSesion() {
        Company company = new Company();
        company.setId(7);
        company.setName("Vargas Vet");
        company.setSlug("vargas-vet");
        company.setActivo(true);
        Usuario usuario = conRolEnLaEmpresa(usuarioValido(), company);
        usuario.setId(10);

        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(usuarioRepository.findAllByUsernameIgnoreCase("ana.qa")).thenReturn(List.of(usuario));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(credencialValida()));
        when(passwordEncoder.matches("Password-123", "hash-almacenado")).thenReturn(true);
        org.mockito.ArgumentCaptor<String> sessionOfAccessToken = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<String> sessionOfRefreshToken = org.mockito.ArgumentCaptor.forClass(String.class);
        when(tokenProvider.createToken(any(), any(), any(), any(), any(), any(), any(), anyLong(), anyLong(),
                sessionOfAccessToken.capture())).thenReturn("access-token");
        when(tokenProvider.createRefreshToken(anyString(), any(), any(), sessionOfRefreshToken.capture(), anyLong()))
                .thenReturn("refresh-token");
        when(tokenProvider.getRefreshTokenDetails("refresh-token"))
                .thenReturn(new TokenProvider.RefreshTokenDetails("ana@example.test", "jti-1", "family-1", null, null, 0L));

        service.login(loginConSlug("vargas-vet"));

        assertThat(sessionOfAccessToken.getValue()).isNotBlank().isEqualTo(sessionOfRefreshToken.getValue());
    }

    @Test
    void elLoginConContrasenaSeRechazaSiLaPersonaNoTieneNingunRolEnLaEmpresa() {
        Company company = vargasVet();
        Usuario sinRol = usuarioValido();
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(usuarioRepository.findAllByUsernameIgnoreCase("ana.qa")).thenReturn(List.of(sinRol));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(credencialValida()));
        when(passwordEncoder.matches("Password-123", "hash-almacenado")).thenReturn(true);

        assertThatThrownBy(() -> service.login(loginConSlug("vargas-vet")))
                .isInstanceOf(org.springframework.security.authentication.DisabledException.class)
                .hasMessageContaining("Todavía no tienes un rol asignado");

        verify(authenticationAuditService).recordLoginFailure(sinRol, "ana.qa", "sin rol asignado");
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void elRolDeOtraEmpresaNoCuentaComoRolAsignadoEnEstaEmpresa() {
        Company company = vargasVet();
        Company otra = new Company();
        otra.setId(8);
        Usuario soloEnOtra = conRolEnLaEmpresa(usuarioValido(), otra);
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(usuarioRepository.findAllByUsernameIgnoreCase("ana.qa")).thenReturn(List.of(soloEnOtra));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(credencialValida()));
        when(passwordEncoder.matches("Password-123", "hash-almacenado")).thenReturn(true);

        assertThatThrownBy(() -> service.login(loginConSlug("vargas-vet")))
                .isInstanceOf(org.springframework.security.authentication.DisabledException.class);

        verifyNoInteractions(tokenProvider);
    }

    @Test
    void elLoginConGoogleSeRechazaSiLaPersonaNoTieneNingunRolEnLaEmpresa() {
        Company company = vargasVet();
        Usuario sinRol = usuarioValido();
        when(usuarioRepository.findAllByEmailIgnoreCase("ana@example.test")).thenReturn(List.of(sinRol));
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(companyMembershipService.hasAnyMembership(10, 7)).thenReturn(true);
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(credencialValida()));

        assertThatThrownBy(() -> service.loginWithGoogle("Ana@Example.test", "vargas-vet"))
                .isInstanceOf(org.springframework.security.authentication.DisabledException.class)
                .hasMessageContaining("Todavía no tienes un rol asignado");

        verify(authenticationAuditService).recordLoginFailure(sinRol, "ana@example.test", "sin rol asignado (con Google)");
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void elLoginConGoogleDeQuienTieneRolEnLaEmpresaEntraConEseRol() {
        Company company = vargasVet();
        Usuario conRol = conRolEnLaEmpresa(usuarioValido(), company);
        when(usuarioRepository.findAllByEmailIgnoreCase("ana@example.test")).thenReturn(List.of(conRol));
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(companyMembershipService.hasAnyMembership(10, 7)).thenReturn(true);
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(credencialValida()));
        tokensDeSesion();

        AuthResponse response = service.loginWithGoogle("ana@example.test", "vargas-vet");

        assertThat(response.getRoles()).containsExactly("ROLE_VETERINARIO");
        assertThat(response.getAssignedRoles()).containsExactly("ROLE_VETERINARIO");
        assertThat(response.getCompanyId()).isEqualTo(7);
    }

    private void googleConRol(Usuario usuario, Company company, veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial) {
        when(usuarioRepository.findAllByEmailIgnoreCase("ana@example.test")).thenReturn(List.of(usuario));
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(companyMembershipService.hasAnyMembership(10, 7)).thenReturn(true);
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(credencial));
        tokensDeSesion();
    }

    private String detalleDeLaAuditoriaDelLogin() {
        org.mockito.ArgumentCaptor<String> detalle = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(anyString(), any(), any(), anyString(), org.mockito.ArgumentMatchers.eq("LOGIN_EXITOSO"),
                anyString(), detalle.capture(), any());
        return detalle.getValue();
    }

    @Test
    void elLoginDeGoogleSinClinicaSeRechazaIgualQueElDeContrasena() {
        for (String sinClinica : new String[]{null, "", "   "}) {
            assertThatThrownBy(() -> service.loginWithGoogle("ana@example.test", sinClinica))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Credenciales inválidas");
        }

        verify(authenticationAuditService, times(3)).recordLoginFailure(null, "ana@example.test", "inicio de sesión de Google sin clínica");
        verifyNoInteractions(usuarioRepository, companyRepository, sharedRateLimitService, tokenProvider);
    }

    @Test
    void laAuditoriaDelLoginConGoogleDiceQueFueConGoogle() {
        Company company = vargasVet();
        googleConRol(conRolEnLaEmpresa(usuarioValido(), company), company, credencialValida());

        service.loginWithGoogle("ana@example.test", "vargas-vet");

        assertThat(detalleDeLaAuditoriaDelLogin()).endsWith("mediante Google");
    }

    @Test
    void laAuditoriaDelLoginConContrasenaDiceQueFueConContrasena() {
        Company company = vargasVet();
        Usuario usuario = conRolEnLaEmpresa(usuarioValido(), company);
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(usuarioRepository.findAllByUsernameIgnoreCase("ana.qa")).thenReturn(List.of(usuario));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(credencialValida()));
        when(passwordEncoder.matches("Password-123", "hash-almacenado")).thenReturn(true);
        tokensDeSesion();

        service.login(loginConSlug("vargas-vet"));

        assertThat(detalleDeLaAuditoriaDelLogin()).endsWith("mediante contraseña");
    }

    @Test
    void unaCuentaDeGoogleSuspendidaEnLaClinicaRecibeUnMotivoPropio() {
        Company company = vargasVet();
        Usuario suspendida = usuarioValido();
        when(usuarioRepository.findAllByEmailIgnoreCase("ana@example.test")).thenReturn(List.of(suspendida));
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(companyMembershipService.hasAnyMembership(10, 7)).thenReturn(true);
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(false);
        when(companyMembershipService.isSuspendedIn(10, 7)).thenReturn(true);

        assertThatThrownBy(() -> service.loginWithGoogle("ana@example.test", "vargas-vet"))
                .isInstanceOf(veterinaria.vargasvet.exception.GoogleAccountSuspendedException.class);

        verify(authenticationAuditService).recordLoginFailure(suspendida, "ana@example.test", "cuenta suspendida (con Google)");
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void quienCerroSuPropiaCuentaRecibeElAvisoDeQueLaPuedeReactivar() {
        Company company = vargasVet();
        Usuario cerrada = usuarioValido();
        when(usuarioRepository.findAllByEmailIgnoreCase("ana@example.test")).thenReturn(List.of(cerrada));
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(companyMembershipService.hasAnyMembership(10, 7)).thenReturn(true);
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(false);
        when(accountClosureGuard.findReactivable(10, 7))
                .thenReturn(Optional.of(new veterinaria.vargasvet.domain.entity.CierreCuenta()));

        assertThatThrownBy(() -> service.loginWithGoogle("ana@example.test", "vargas-vet"))
                .isInstanceOfSatisfying(veterinaria.vargasvet.exception.GoogleAccountClosedException.class, ex -> {
                    assertThat(ex.getUsuarioId()).isEqualTo(10);
                    assertThat(ex.getCompanyId()).isEqualTo(7);
                    assertThat(ex.getEmail()).isEqualTo("ana@example.test");
                    assertThat(ex.getSlug()).isEqualTo("vargas-vet");
                });

        verify(authenticationAuditService).recordLoginFailure(cerrada, "ana@example.test", "cuenta cerrada por la persona (con Google)");
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void unaCuentaDeGoogleDadaDeBajaEnLaClinicaRecibeUnMotivoPropio() {
        Company company = vargasVet();
        Usuario deBaja = usuarioValido();
        when(usuarioRepository.findAllByEmailIgnoreCase("ana@example.test")).thenReturn(List.of(deBaja));
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(companyMembershipService.hasAnyMembership(10, 7)).thenReturn(true);
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(false);
        when(companyMembershipService.isDeactivatedIn(10, 7)).thenReturn(true);

        assertThatThrownBy(() -> service.loginWithGoogle("ana@example.test", "vargas-vet"))
                .isInstanceOf(veterinaria.vargasvet.exception.GoogleAccountDeactivatedException.class);

        verify(authenticationAuditService).recordLoginFailure(deBaja, "ana@example.test", "cuenta dada de baja (con Google)");
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void quienNuncaEstuvoRegistradoEnLaClinicaSigueViendoElMensajeGeneral() {
        Company company = vargasVet();
        Usuario deBaja = usuarioValido();
        when(usuarioRepository.findAllByEmailIgnoreCase("ana@example.test")).thenReturn(List.of(deBaja));
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(companyMembershipService.hasAnyMembership(10, 7)).thenReturn(false);

        assertThatThrownBy(() -> service.loginWithGoogle("ana@example.test", "vargas-vet"))
                .isInstanceOf(veterinaria.vargasvet.exception.GoogleClinicAccessException.class);

        verify(authenticationAuditService).recordLoginFailure(deBaja, "ana@example.test", "cuenta de Google sin acceso a la empresa");
    }

    @Test
    void unCorreoSinCuentaSeRegistraConMotivoPropioYNoCuentaComoContrasenaIncorrecta() {
        when(usuarioRepository.findAllByEmailIgnoreCase("nadie@example.test")).thenReturn(List.of());

        assertThatThrownBy(() -> service.loginWithGoogle("nadie@example.test", "vargas-vet"))
                .isInstanceOf(veterinaria.vargasvet.exception.GoogleClinicAccessException.class);

        verify(authenticationAuditService).recordLoginFailure(null, "nadie@example.test", "correo de Google sin cuenta");
        verify(authenticationAuditService, never()).recordLoginFailure(any(), anyString(), org.mockito.ArgumentMatchers.eq("credenciales inválidas"));
    }

    @Test
    void elPanelNoPideCambiarContrasenaAQuienActivoConGoogle() {
        Company company = vargasVet();
        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial = credencialValida();
        credencial.setPasswordChanged(false);
        credencial.setActivatedWithGoogle(true);
        googleConRol(conRolEnLaEmpresa(usuarioValido(), company), company, credencial);

        assertThat(service.loginWithGoogle("ana@example.test", "vargas-vet").isPasswordChanged()).isTrue();
        assertThat(credencial.isPasswordChanged()).isFalse();
    }

    @Test
    void quienTieneUnaContrasenaTemporalSigueViendoElAvisoDeCambiarla() {
        Company company = vargasVet();
        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial = credencialValida();
        credencial.setPasswordChanged(false);
        googleConRol(conRolEnLaEmpresa(usuarioValido(), company), company, credencial);

        assertThat(service.loginWithGoogle("ana@example.test", "vargas-vet").isPasswordChanged()).isFalse();
    }

    private Usuario invitacionPendiente() {
        Usuario usuario = usuarioValido();
        usuario.setActivo(false);
        usuario.setEmailVerified(false);
        usuario.setVerificationToken("hash");
        usuario.setVerificationTokenExpiresAt(veterinaria.vargasvet.util.AppClock.now().plusHours(1));
        return usuario;
    }

    @Test
    void activarConGoogleMarcaLaCredencialSinInventarleUnaContrasena() {
        Company company = vargasVet();
        Usuario invitado = conRolEnLaEmpresa(invitacionPendiente(), company);
        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial = credencialValida();
        credencial.setPasswordChanged(false);
        credencial.setCompany(company);
        when(usuarioRepository.findByVerificationTokenForUpdate(anyString())).thenReturn(Optional.of(invitado));
        when(credencialRepository.findAllByUsuarioId(10)).thenReturn(List.of(credencial));
        tokensDeSesion();

        AuthResponse respuesta = service.activateAccountWithGoogle("token", "Ana@Example.test");

        assertThat(invitado.isActivo()).isTrue();
        assertThat(invitado.isEmailVerified()).isTrue();
        assertThat(invitado.getVerificationToken()).isNull();
        assertThat(credencial.isActivatedWithGoogle()).isTrue();
        assertThat(credencial.isPasswordChanged()).isFalse();
        assertThat(respuesta.isPasswordChanged()).isTrue();
        assertThat(respuesta.getRoles()).containsExactly("ROLE_VETERINARIO");
        verify(authenticationAuditService).record(org.mockito.ArgumentMatchers.eq(invitado),
                org.mockito.ArgumentMatchers.eq("ACTIVAR_CUENTA_GOOGLE"), anyString());
        assertThat(detalleDeLaAuditoriaDelLogin()).endsWith("mediante Google");
    }

    @Test
    void activarConUnCorreoDeGoogleDistintoAlInvitadoNoActivaNada() {
        Company company = vargasVet();
        Usuario invitado = conRolEnLaEmpresa(invitacionPendiente(), company);
        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial = credencialValida();
        credencial.setPasswordChanged(false);
        credencial.setCompany(company);
        when(usuarioRepository.findByVerificationTokenForUpdate(anyString())).thenReturn(Optional.of(invitado));
        when(credencialRepository.findAllByUsuarioId(10)).thenReturn(List.of(credencial));

        assertThatThrownBy(() -> service.activateAccountWithGoogle("token", "otra.persona@example.test"))
                .isInstanceOf(veterinaria.vargasvet.exception.GoogleEmailMismatchException.class);

        assertThat(invitado.isActivo()).isFalse();
        assertThat(credencial.isActivatedWithGoogle()).isFalse();
        assertThat(invitado.getVerificationToken()).isEqualTo("hash");
        verifyNoInteractions(authenticationAuditService);
    }

    @Test
    void activarConUnEnlaceVencidoNoActivaNada() {
        Usuario vencido = invitacionPendiente();
        vencido.setVerificationTokenExpiresAt(veterinaria.vargasvet.util.AppClock.now().minusMinutes(1));
        when(usuarioRepository.findByVerificationTokenForUpdate(anyString())).thenReturn(Optional.of(vencido));

        assertThatThrownBy(() -> service.activateAccountWithGoogle("token", "ana@example.test"))
                .isInstanceOf(veterinaria.vargasvet.exception.ResourceNotFoundException.class);

        assertThat(vencido.isActivo()).isFalse();
    }

    @Test
    void alCerrarSesionSeRevocaLaFamiliaYSeCierranSusConexionesEnTiempoReal() {
        Usuario usuario = usuarioValido();
        veterinaria.vargasvet.domain.entity.RefreshToken presentado = veterinaria.vargasvet.domain.entity.RefreshToken.builder()
                .familyId("sesion-1").usuario(usuario).build();
        veterinaria.vargasvet.domain.entity.RefreshToken vigente = veterinaria.vargasvet.domain.entity.RefreshToken.builder()
                .familyId("sesion-1").usuario(usuario).build();
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(presentado));
        when(refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull("sesion-1")).thenReturn(List.of(vigente));

        service.revokeRefreshToken("cookie-refresh");

        assertThat(vigente.getRevokedAt()).isNotNull();
        verify(realtimeSubscriptionGuard).closeConnectionsOfSession("sesion-1");
    }

    @Test
    void cerrarSesionSinCookieNoHaceNada() {
        service.revokeRefreshToken(null);
        service.revokeRefreshToken("  ");

        verifyNoInteractions(refreshTokenRepository, realtimeSubscriptionGuard);
    }

    private void cuentaCerradaPorSuDueno(String claveEscrita, boolean claveCorrecta) {
        Company company = vargasVet();
        Usuario cerrada = usuarioValido();
        veterinaria.vargasvet.domain.entity.CierreCuenta cierre = new veterinaria.vargasvet.domain.entity.CierreCuenta();
        cierre.setVenceAt(java.time.LocalDateTime.now().plusDays(5));
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(usuarioRepository.findAllByUsernameIgnoreCase("ana.qa")).thenReturn(List.of(cerrada));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(false);
        when(accountClosureGuard.findReactivable(10, 7)).thenReturn(Optional.of(cierre));
        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial = credencialValida();
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches(claveEscrita, "hash-almacenado")).thenReturn(claveCorrecta);
    }

    @Test
    void conLaClaveCorrectaUnaCuentaCerradaPorSuDuenoAvisaQueSePuedeReactivar() {
        cuentaCerradaPorSuDueno("Password-123", true);

        assertThatThrownBy(() -> service.login(loginConSlug("vargas-vet")))
                .isInstanceOfSatisfying(veterinaria.vargasvet.exception.AccountClosedException.class, ex -> {
                    assertThat(ex.getUsuarioId()).isEqualTo(10);
                    assertThat(ex.getCompanyId()).isEqualTo(7);
                    assertThat(ex.getReactivableHasta()).isNotNull();
                });

        verifyNoInteractions(tokenProvider);
    }

    @Test
    void conUnaClaveIncorrectaNoSeRevelaQueLaCuentaEstaCerrada() {
        cuentaCerradaPorSuDueno("Password-123", false);

        assertThatThrownBy(() -> service.login(loginConSlug("vargas-vet")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verifyNoInteractions(tokenProvider);
    }

    @Test
    void unaCuentaSinCierreDelDuenoNoOfreceReactivacionAunqueLaClaveSeaCorrecta() {
        Company company = vargasVet();
        Usuario inactiva = usuarioValido();
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(usuarioRepository.findAllByUsernameIgnoreCase("ana.qa")).thenReturn(List.of(inactiva));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(false);
        when(accountClosureGuard.findReactivable(10, 7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(loginConSlug("vargas-vet")))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(tokenProvider);
    }
}

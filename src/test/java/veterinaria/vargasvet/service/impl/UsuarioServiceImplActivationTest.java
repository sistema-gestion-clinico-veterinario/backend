package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.service.AuthenticationAuditService;
import veterinaria.vargasvet.service.CompanyMembershipService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.security.PasswordPolicyService;
import veterinaria.vargasvet.util.AppClock;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Cubre el reenvio del enlace de activacion y los puntos de activacion por token:
 * el correo se resuelve dentro de la empresa del slug (puede repetirse entre empresas)
 * y quien fue dado de baja antes de activar su cuenta no puede reactivarse solo.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplActivationTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CompanyRepository companyRepository;
    @Mock CompanyMembershipService companyMembershipService;
    @Mock SharedRateLimitService sharedRateLimitService;
    @Mock AuthenticationAuditService authenticationAuditService;
    @Mock EmailService emailService;
    @Mock PasswordPolicyService passwordPolicyService;
    @Mock UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock veterinaria.vargasvet.service.ConsentimientoDatosService consentimientoDatosService;

    @InjectMocks UsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "verificationTokenValidityHours", 24L);
    }

    private Company empresa(int id, String slug) {
        Company company = new Company();
        company.setId(id);
        company.setSlug(slug);
        return company;
    }

    private Usuario pendiente(int id, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setActivo(false);
        usuario.setEmailVerified(false);
        return usuario;
    }

    private Usuario conTokenVigente(Usuario usuario) {
        usuario.setVerificationToken("hash");
        usuario.setVerificationTokenExpiresAt(AppClock.now().plusHours(1));
        return usuario;
    }

    @Test
    void reenvioConSlugEligeAlUsuarioConMembresiaActivaCuandoElCorreoSeRepiteEntreEmpresas() {
        Company vargas = empresa(7, "vargas-vet");
        Usuario deVargas = pendiente(10, "ana@example.test");
        Usuario deOtraEmpresa = pendiente(20, "ana@example.test");
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(vargas));
        when(usuarioRepository.findAllByEmailIgnoreCase("ana@example.test"))
                .thenReturn(List.of(deOtraEmpresa, deVargas));
        when(companyMembershipService.hasActiveMembership(20, 7)).thenReturn(false);
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(credencialRepository.findAllByUsuarioId(10)).thenReturn(List.of());

        service.resendVerificationToken("Ana@Example.test", "Vargas-Vet");

        assertThat(deVargas.getVerificationToken()).isNotNull();
        assertThat(deOtraEmpresa.getVerificationToken()).isNull();
        verify(usuarioRepository).save(deVargas);
        verify(emailService).sendEmailWithRetry(any(), eq("email/welcome-template"));
    }

    @Test
    void reenvioNoEmiteEnlaceAQuienFueDadoDeBajaAntesDeActivar() {
        Company vargas = empresa(7, "vargas-vet");
        Usuario desactivado = pendiente(10, "ana@example.test");
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(vargas));
        when(usuarioRepository.findAllByEmailIgnoreCase("ana@example.test")).thenReturn(List.of(desactivado));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(false);

        service.resendVerificationToken("ana@example.test", "vargas-vet");

        assertThat(desactivado.getVerificationToken()).isNull();
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void reenvioConSlugInexistenteNoBuscaUsuariosNiEnviaCorreo() {
        when(companyRepository.findBySlug("no-existe")).thenReturn(Optional.empty());

        service.resendVerificationToken("ana@example.test", "no-existe");

        verify(usuarioRepository, never()).findAllByEmailIgnoreCase(anyString());
        verifyNoInteractions(emailService);
    }

    @Test
    void reenvioSinSlugSoloAtiendeCuentasSinEmpresa() {
        Usuario adminPlataforma = pendiente(1, "admin@example.test");
        when(usuarioRepository.findByEmailAndCompanyIsNull("admin@example.test")).thenReturn(Optional.of(adminPlataforma));
        when(companyMembershipService.hasOnlyInactiveMemberships(1)).thenReturn(false);
        when(credencialRepository.findAllByUsuarioId(1)).thenReturn(List.of());

        service.resendVerificationToken("admin@example.test", null);

        assertThat(adminPlataforma.getVerificationToken()).isNotNull();
        verify(emailService).sendEmailWithRetry(any(), eq("email/welcome-template"));
        verify(usuarioRepository, never()).findAllByEmailIgnoreCase(anyString());
    }

    @Test
    void reenvioSinSlugNoEmiteEnlaceACuentaDesactivadaEnTodasSusEmpresas() {
        Usuario desactivado = pendiente(2, "ex@example.test");
        when(usuarioRepository.findByEmailAndCompanyIsNull("ex@example.test")).thenReturn(Optional.of(desactivado));
        when(companyMembershipService.hasOnlyInactiveMemberships(2)).thenReturn(true);

        service.resendVerificationToken("ex@example.test", null);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void setupAccountRechazaElEnlaceDeQuienFueDadoDeBajaYNoLoActiva() {
        Usuario desactivado = conTokenVigente(pendiente(10, "ana@example.test"));
        when(usuarioRepository.findByVerificationTokenForUpdate(anyString())).thenReturn(Optional.of(desactivado));
        when(companyMembershipService.hasOnlyInactiveMemberships(10)).thenReturn(true);

        assertThatThrownBy(() -> service.setupAccount("token-previo-a-la-baja", "Contrasena-Segura-123", null, null, null))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(desactivado.isActivo()).isFalse();
        assertThat(desactivado.isEmailVerified()).isFalse();
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordPolicyService);
    }

    private veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencialPendiente(Usuario usuario, int companyId) {
        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                new veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial();
        credencial.setUsuario(usuario);
        credencial.setCompany(empresa(companyId, "clinica-" + companyId));
        credencial.setPassword("hash-temporal");
        credencial.setPasswordChanged(false);
        return credencial;
    }

    @Test
    void setupAccountExigeHaberLeidoElAvisoCuandoLaClinicaYaLoPublico() {
        Usuario pendiente = conTokenVigente(pendiente(10, "ana@example.test"));
        when(usuarioRepository.findByVerificationTokenForUpdate(anyString())).thenReturn(Optional.of(pendiente));
        when(companyMembershipService.hasOnlyInactiveMemberships(10)).thenReturn(false);
        when(credencialRepository.findAllByUsuarioId(10)).thenReturn(List.of(credencialPendiente(pendiente, 7)));
        when(consentimientoDatosService.hayAvisoPublicado(7)).thenReturn(true);

        assertThatThrownBy(() -> service.setupAccount("token", "Contrasena-Segura-123", null, "10.0.0.1", "navegador"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("aviso de privacidad");
        assertThatThrownBy(() -> service.setupAccount("token", "Contrasena-Segura-123", false, "10.0.0.1", "navegador"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(pendiente.isActivo()).isFalse();
        verifyNoInteractions(passwordPolicyService);
        verify(consentimientoDatosService, never()).registrarEnterado(any(), any(), any(), any(), any(), any());
    }

    @Test
    void setupAccountConElAvisoLeidoDejaConstanciaDeLaActivacion() {
        Usuario pendiente = conTokenVigente(pendiente(10, "ana@example.test"));
        when(usuarioRepository.findByVerificationTokenForUpdate(anyString())).thenReturn(Optional.of(pendiente));
        when(companyMembershipService.hasOnlyInactiveMemberships(10)).thenReturn(false);
        when(credencialRepository.findAllByUsuarioId(10)).thenReturn(List.of(credencialPendiente(pendiente, 7)));
        when(consentimientoDatosService.hayAvisoPublicado(7)).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hash-nuevo");

        service.setupAccount("token", "Contrasena-Segura-123", true, "10.0.0.1", "navegador");

        assertThat(pendiente.isActivo()).isTrue();
        verify(consentimientoDatosService).registrarEnterado(10, 7,
                veterinaria.vargasvet.domain.enums.CanalConsentimiento.ACTIVACION, null, "10.0.0.1", "navegador");
    }

    @Test
    void setupAccountSigueComoAntesSiLaClinicaNoPublicoAviso() {
        Usuario pendiente = conTokenVigente(pendiente(10, "ana@example.test"));
        when(usuarioRepository.findByVerificationTokenForUpdate(anyString())).thenReturn(Optional.of(pendiente));
        when(companyMembershipService.hasOnlyInactiveMemberships(10)).thenReturn(false);
        when(credencialRepository.findAllByUsuarioId(10)).thenReturn(List.of(credencialPendiente(pendiente, 7)));
        when(consentimientoDatosService.hayAvisoPublicado(7)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash-nuevo");

        service.setupAccount("token", "Contrasena-Segura-123", null, null, null);

        assertThat(pendiente.isActivo()).isTrue();
    }

    @Test
    void setupAccountResponde404ConEnlaceVencidoParaQueLaPantallaOfrezcaElReenvio() {
        Usuario pendiente = pendiente(10, "ana@example.test");
        pendiente.setVerificationToken("hash");
        pendiente.setVerificationTokenExpiresAt(AppClock.now().minusHours(1));
        when(usuarioRepository.findByVerificationTokenForUpdate(anyString())).thenReturn(Optional.of(pendiente));

        assertThatThrownBy(() -> service.setupAccount("token-vencido", "Contrasena-Segura-123", null, null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El enlace de activación es inválido o expiró");

        assertThat(pendiente.isActivo()).isFalse();
        verifyNoInteractions(passwordPolicyService);
    }

    @Test
    void activacionConGoogleRechazaElEnlaceDeQuienFueDadoDeBajaYNoLoActiva() {
        Usuario desactivado = conTokenVigente(pendiente(10, "ana@example.test"));
        when(usuarioRepository.findByVerificationTokenForUpdate(anyString())).thenReturn(Optional.of(desactivado));
        when(companyMembershipService.hasOnlyInactiveMemberships(10)).thenReturn(true);

        assertThatThrownBy(() -> service.activateAccountWithGoogle("token-previo-a-la-baja", "ana@example.test"))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(desactivado.isActivo()).isFalse();
        assertThat(desactivado.isEmailVerified()).isFalse();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void activacionConGoogleSinRolAsignadoNoActivaLaCuentaNiConsumeElEnlace() {
        Usuario invitado = conTokenVigente(pendiente(10, "ana@example.test"));
        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                new veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial();
        credencial.setCompany(empresa(7, "vargas-vet"));
        when(usuarioRepository.findByVerificationTokenForUpdate(anyString())).thenReturn(Optional.of(invitado));
        when(credencialRepository.findAllByUsuarioId(10)).thenReturn(List.of(credencial));

        assertThatThrownBy(() -> service.activateAccountWithGoogle("token", "Ana@Example.test"))
                .isInstanceOf(org.springframework.security.authentication.DisabledException.class)
                .hasMessageContaining("Todavía no tienes un rol asignado");

        assertThat(invitado.isActivo()).isFalse();
        assertThat(invitado.isEmailVerified()).isFalse();
        assertThat(invitado.getVerificationToken()).isEqualTo("hash");
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(authenticationAuditService);
    }

    private Usuario conEnlaceVencido(Usuario usuario) {
        usuario.setVerificationToken("hash-vencido");
        usuario.setVerificationTokenExpiresAt(AppClock.now().minusHours(2));
        return usuario;
    }

    @Test
    void reenvioPorEnlaceVencidoEmiteUnoNuevoYDevuelveElCorreoEnmascarado() {
        Company vargas = empresa(7, "vargas-vet");
        Usuario pendiente = conEnlaceVencido(pendiente(10, "ana@example.test"));
        when(usuarioRepository.findByVerificationTokenForUpdate(anyString())).thenReturn(Optional.of(pendiente));
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(vargas));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(credencialRepository.findAllByUsuarioId(10)).thenReturn(List.of());

        String enmascarado = service.resendVerificationByToken("token-vencido", "Vargas-Vet");

        assertThat(enmascarado).isEqualTo("a***@example.test");
        assertThat(pendiente.getVerificationToken()).isNotEqualTo("hash-vencido").isNotNull();
        assertThat(pendiente.getVerificationTokenExpiresAt()).isAfter(AppClock.now());
        verify(usuarioRepository).save(pendiente);
        verify(emailService).sendEmailWithRetry(any(), eq("email/welcome-template"));
    }

    @Test
    void reenvioPorEnlaceDesconocidoResponde404SinEnviarCorreo() {
        when(usuarioRepository.findByVerificationTokenForUpdate(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resendVerificationByToken("token-inexistente", "vargas-vet"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(emailService);
    }

    @Test
    void reenvioPorEnlaceNoAplicaAQuienFueDadoDeBajaEnEsaClinica() {
        Company vargas = empresa(7, "vargas-vet");
        Usuario desactivado = conEnlaceVencido(pendiente(10, "ana@example.test"));
        when(usuarioRepository.findByVerificationTokenForUpdate(anyString())).thenReturn(Optional.of(desactivado));
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(vargas));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(false);

        assertThatThrownBy(() -> service.resendVerificationByToken("token-vencido", "vargas-vet"))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(desactivado.getVerificationToken()).isEqualTo("hash-vencido");
        verifyNoInteractions(emailService);
    }

    @Test
    void reenvioPorEnlaceRechazaUnaCuentaYaActivada() {
        Company vargas = empresa(7, "vargas-vet");
        Usuario activa = conEnlaceVencido(pendiente(10, "ana@example.test"));
        activa.setActivo(true);
        activa.setEmailVerified(true);
        when(usuarioRepository.findByVerificationTokenForUpdate(anyString())).thenReturn(Optional.of(activa));
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(vargas));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);

        assertThatThrownBy(() -> service.resendVerificationByToken("token-vencido", "vargas-vet"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya fue activada");

        verifyNoInteractions(emailService);
    }
}

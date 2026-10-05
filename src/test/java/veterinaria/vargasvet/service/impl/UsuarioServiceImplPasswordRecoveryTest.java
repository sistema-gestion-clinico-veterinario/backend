package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.PasswordResetToken;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.dto.request.ForgotPasswordRequest;
import veterinaria.vargasvet.dto.request.ResetPasswordRequest;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.PasswordResetTokenRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.AccountLockoutService;
import veterinaria.vargasvet.security.PasswordPolicyService;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CompanyMembershipService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.SessionSecurityService;
import veterinaria.vargasvet.util.AppClock;

import java.util.List;
import java.util.Map;
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
 * Recuperación de contraseña: pedido de enlace (respuesta neutra, empresa del slug, token
 * guardado solo como hash) y restablecimiento (token vigente de un solo uso, contraseña
 * distinta a la actual, sesiones cerradas y bloqueo de cuenta levantado).
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplPasswordRecoveryTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock CompanyRepository companyRepository;
    @Mock CompanyMembershipService companyMembershipService;
    @Mock SharedRateLimitService sharedRateLimitService;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock EmailService emailService;
    @Mock AuditLogService auditLogService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock PasswordPolicyService passwordPolicyService;
    @Mock SessionSecurityService sessionSecurityService;
    @Mock AccountLockoutService accountLockoutService;

    @InjectMocks UsuarioServiceImpl service;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(7);
        company.setName("Vargas Vet");
        company.setSlug("vargas-vet");
        ReflectionTestUtils.setField(service, "appUrl", "https://frontend.test");
        ReflectionTestUtils.setField(service, "passwordResetValidityMinutes", 60L);
    }

    private Usuario usuario(boolean activo, boolean verificado) {
        Usuario usuario = new Usuario();
        usuario.setId(10);
        usuario.setUsername("ana.perez");
        usuario.setEmail("ana@example.test");
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        usuario.setActivo(activo);
        usuario.setEmailVerified(verificado);
        return usuario;
    }

    private UsuarioEmpresaCredencial credencial() {
        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setPassword("HASH_ACTUAL");
        credencial.setPasswordChanged(true);
        credencial.setCompany(company);
        return credencial;
    }

    private ForgotPasswordRequest pedido(String slug) {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("ana@example.test");
        request.setSlug(slug);
        return request;
    }

    private ResetPasswordRequest restablecimiento(String token, String nueva) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        request.setNewPassword(nueva);
        return request;
    }

    private PasswordResetToken tokenGuardado(Usuario usuario, boolean vencido) {
        return PasswordResetToken.builder()
                .token(SecurityTokenUtils.hash("token-valido"))
                .usuario(usuario)
                .company(company)
                .expiryDate(vencido ? AppClock.now().minusMinutes(1) : AppClock.now().plusMinutes(30))
                .build();
    }

    @Test
    void pedidoDeCuentaVigenteGuardaSoloElHashDelTokenYEnviaElEnlaceDeSuClinica() {
        Usuario usuario = usuario(true, true);
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(usuarioRepository.findAllByEmailIgnoreCase("ana@example.test")).thenReturn(List.of(usuario));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(credencial()));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> model = ArgumentCaptor.forClass(Map.class);
        when(emailService.createMail(eq("ana@example.test"), anyString(), model.capture()))
                .thenAnswer(i -> new Mail(null, i.getArgument(0), i.getArgument(1), i.getArgument(2)));
        ArgumentCaptor<PasswordResetToken> guardado = ArgumentCaptor.forClass(PasswordResetToken.class);

        service.forgotPassword(pedido("Vargas-Vet"));

        InOrder orden = Mockito.inOrder(passwordResetTokenRepository);
        orden.verify(passwordResetTokenRepository).deleteByUsuarioAndCompany(usuario, company);
        orden.verify(passwordResetTokenRepository).save(guardado.capture());
        String enlace = (String) model.getValue().get("resetUrl");
        String tokenEnClaro = enlace.substring(enlace.indexOf("#token=") + "#token=".length());
        assertThat(enlace).startsWith("https://frontend.test").contains("vargas-vet");
        assertThat(guardado.getValue().getToken()).isEqualTo(SecurityTokenUtils.hash(tokenEnClaro)).isNotEqualTo(tokenEnClaro);
        assertThat(guardado.getValue().getExpiryDate()).isAfter(AppClock.now().plusMinutes(59));
        verify(emailService).sendEmailWithRetry(any(), eq("email/forgot-password-template"));
    }

    @Test
    void pedidoDeCuentaSinVerificarODesactivadaNoGeneraTokenNiCorreo() {
        Usuario sinVerificar = usuario(false, false);
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(usuarioRepository.findAllByEmailIgnoreCase("ana@example.test")).thenReturn(List.of(sinVerificar));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);

        service.forgotPassword(pedido("vargas-vet"));

        verify(passwordResetTokenRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void pedidoConClinicaInexistenteONoVinculadaNoHaceNada() {
        when(companyRepository.findBySlug("no-existe")).thenReturn(Optional.empty());
        service.forgotPassword(pedido("no-existe"));

        Usuario deOtraClinica = usuario(true, true);
        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(usuarioRepository.findAllByEmailIgnoreCase("ana@example.test")).thenReturn(List.of(deOtraClinica));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(false);
        service.forgotPassword(pedido("vargas-vet"));

        verify(passwordResetTokenRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void restablecerConTokenVigenteCambiaLaContrasenaCierraSesionesYLevantaElBloqueo() {
        Usuario usuario = usuario(true, true);
        UsuarioEmpresaCredencial credencial = credencial();
        PasswordResetToken token = tokenGuardado(usuario, false);
        when(passwordResetTokenRepository.findByTokenForUpdate(SecurityTokenUtils.hash("token-valido")))
                .thenReturn(Optional.of(token));
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("Nueva-Contrasena-123", "HASH_ACTUAL")).thenReturn(false);
        when(passwordEncoder.encode("Nueva-Contrasena-123")).thenReturn("HASH_NUEVO");

        service.resetPasswordWithToken(restablecimiento("token-valido", "Nueva-Contrasena-123"));

        assertThat(credencial.getPassword()).isEqualTo("HASH_NUEVO");
        assertThat(credencial.isPasswordChanged()).isTrue();
        verify(credencialRepository).save(credencial);
        verify(sessionSecurityService).invalidateSessionsForCredential(credencial);
        verify(accountLockoutService).clearLockout("ana.perez");
        verify(accountLockoutService).clearLockout("ana@example.test");
        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    void restablecerConTokenVencidoResponde404YNoCambiaNada() {
        Usuario usuario = usuario(true, true);
        when(passwordResetTokenRepository.findByTokenForUpdate(SecurityTokenUtils.hash("token-valido")))
                .thenReturn(Optional.of(tokenGuardado(usuario, true)));

        assertThatThrownBy(() -> service.resetPasswordWithToken(restablecimiento("token-valido", "Nueva-Contrasena-123")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("expirado");

        verify(credencialRepository, never()).save(any());
        verifyNoInteractions(accountLockoutService, sessionSecurityService);
    }

    @Test
    void restablecerConTokenInexistenteResponde404() {
        when(passwordResetTokenRepository.findByTokenForUpdate(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPasswordWithToken(restablecimiento("otro", "Nueva-Contrasena-123")))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(credencialRepository, accountLockoutService);
    }

    @Test
    void restablecerConLaMismaContrasenaActualSeRechazaYConservaElToken() {
        Usuario usuario = usuario(true, true);
        PasswordResetToken token = tokenGuardado(usuario, false);
        when(passwordResetTokenRepository.findByTokenForUpdate(SecurityTokenUtils.hash("token-valido")))
                .thenReturn(Optional.of(token));
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(credencial()));
        when(passwordEncoder.matches("Misma-Contrasena-123", "HASH_ACTUAL")).thenReturn(true);

        assertThatThrownBy(() -> service.resetPasswordWithToken(restablecimiento("token-valido", "Misma-Contrasena-123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diferente");

        verify(credencialRepository, never()).save(any());
        verify(passwordResetTokenRepository, never()).delete(any(PasswordResetToken.class));
        verifyNoInteractions(accountLockoutService, sessionSecurityService);
    }
}

package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.request.ChangePasswordDTO;
import veterinaria.vargasvet.exception.RateLimitExceededException;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.PasswordPolicyService;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.SessionSecurityService;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Cambio de contraseña desde una sesión autenticada: exige la contraseña actual, aplica la
 * política, invalida la credencial reemplazada y limita los intentos para que una sesión
 * robada no pueda adivinar la contraseña actual.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplChangePasswordTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock PasswordPolicyService passwordPolicyService;
    @Mock SessionSecurityService sessionSecurityService;
    @Mock SharedRateLimitService sharedRateLimitService;
    @Mock AuditLogService auditLogService;
    @Mock EmailService emailService;

    @InjectMocks UsuarioServiceImpl service;

    private Usuario usuario;
    private UsuarioEmpresaCredencial credencial;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setId(3);

        usuario = new Usuario();
        usuario.setId(10);
        usuario.setEmail("ana@example.test");
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");

        credencial = new UsuarioEmpresaCredencial();
        credencial.setPassword("HASH_ACTUAL");
        credencial.setPasswordChanged(true);
        credencial.setCompany(company);

        UsuarioPrincipal principal = new UsuarioPrincipal(
                10, "ana@example.test", "", List.of(), 3,
                2, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        ReflectionTestUtils.setField(service, "appUrl", "https://frontend.test");

        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ChangePasswordDTO dto(String actual, String nueva) {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(actual);
        dto.setNewPassword(nueva);
        return dto;
    }

    @Test
    void cambioValidoGuardaElHashNuevoInvalidaSesionesYRegistraAuditoria() {
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 3)).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("Actual-Contrasena-1", "HASH_ACTUAL")).thenReturn(true);
        when(passwordEncoder.matches("Nueva-Contrasena-123", "HASH_ACTUAL")).thenReturn(false);
        when(passwordEncoder.encode("Nueva-Contrasena-123")).thenReturn("HASH_NUEVO");

        service.changePassword(10, dto("Actual-Contrasena-1", "Nueva-Contrasena-123"));

        assertThat(credencial.getPassword()).isEqualTo("HASH_NUEVO");
        assertThat(credencial.isPasswordChanged()).isTrue();
        verify(credencialRepository).save(credencial);
        verify(sessionSecurityService).invalidateSessionsForCredential(credencial);
        verify(auditLogService).log("CAMBIO_CONTRASENA", "Seguridad", "El usuario cambió su contraseña");
        verify(emailService).sendEmail(any(), org.mockito.ArgumentMatchers.eq("email/password-change-template"));
    }

    @Test
    void contrasenaActualIncorrectaSeRechazaSinCambiarNada() {
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 3)).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("Equivocada-123", "HASH_ACTUAL")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(10, dto("Equivocada-123", "Nueva-Contrasena-123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La contraseña actual es incorrecta");

        assertThat(credencial.getPassword()).isEqualTo("HASH_ACTUAL");
        verify(credencialRepository, never()).save(any());
        verifyNoInteractions(sessionSecurityService, passwordPolicyService, emailService);
    }

    @Test
    void laNuevaContrasenaDebeSerDistintaDeLaActual() {
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 3)).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("Misma-Contrasena-123", "HASH_ACTUAL")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(10, dto("Misma-Contrasena-123", "Misma-Contrasena-123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diferente");

        verify(credencialRepository, never()).save(any());
        verifyNoInteractions(sessionSecurityService);
    }

    @Test
    void elTopeDeIntentosSeAplicaPorUsuarioYClinicaAntesDeVerificarLaContrasena() {
        doThrow(new RateLimitExceededException()).when(sharedRateLimitService)
                .enforce(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt(), any(Duration.class));

        assertThatThrownBy(() -> service.changePassword(10, dto("Cualquiera-123", "Nueva-Contrasena-123")))
                .isInstanceOf(RateLimitExceededException.class);

        verify(sharedRateLimitService).enforce("change-password-account", "10:3", 5, Duration.ofMinutes(15));
        verifyNoInteractions(passwordEncoder, credencialRepository, sessionSecurityService);
    }
}

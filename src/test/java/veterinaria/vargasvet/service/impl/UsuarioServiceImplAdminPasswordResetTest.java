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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.PasswordResetToken;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.dto.request.AdminPasswordResetRequest;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.PasswordResetTokenRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AdministratorProtection;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CompanyMembershipService;
import veterinaria.vargasvet.service.EmailService;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Restablecimiento administrativo (RF-AUT-11): la contraseña la elige la persona desde su correo,
 * la acción queda a nombre de quien la pidió, no cruza empresas y respeta la jerarquía.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplAdminPasswordResetTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock CompanyRepository companyRepository;
    @Mock SharedRateLimitService sharedRateLimitService;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock EmailService emailService;
    @Mock AuditLogService auditLogService;
    @Mock AdministratorProtection administratorProtection;
    @Mock CompanyMembershipService companyMembershipService;

    @InjectMocks UsuarioServiceImpl service;

    private Company company;
    private Usuario colaborador;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(7);
        company.setName("Vargas Vet");
        company.setSlug("vargas-vet");
        colaborador = new Usuario();
        colaborador.setId(10);
        colaborador.setEmail("ana@example.test");
        colaborador.setNombre("Ana");
        colaborador.setApellido("Pérez");
        colaborador.setActivo(true);
        colaborador.setEmailVerified(true);
        ReflectionTestUtils.setField(service, "appUrl", "https://frontend.test");
        ReflectionTestUtils.setField(service, "passwordResetValidityMinutes", 60L);
        ReflectionTestUtils.setField(service, "recoveryPerAccountPerHour", 3);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void actuarComo(RolePurpose purpose, Integer companyId) {
        UsuarioPrincipal principal = new UsuarioPrincipal(1, "quien.pide@example.test", "", List.of(), companyId,
                2, purpose == RolePurpose.PLATFORM_ADMIN ? RoleScope.PLATFORM : RoleScope.STAFF, purpose, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private AdminPasswordResetRequest pedido(Integer userId, Integer companyId) {
        AdminPasswordResetRequest request = new AdminPasswordResetRequest();
        request.setUserId(userId);
        request.setCompanyId(companyId);
        return request;
    }

    private void colaboradorDeLaClinica7() {
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(colaborador));
        when(companyMembershipService.hasAnyMembership(10, 7)).thenReturn(true);
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(companyRepository.findById(7)).thenReturn(Optional.of(company));
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(new UsuarioEmpresaCredencial()));
        when(emailService.createMail(anyString(), anyString(), anyMap())).thenReturn(new Mail());
    }

    @Test
    void elEnlaceSeEnviaAlCorreoDelColaboradorConElTokenGuardadoSoloComoHash() {
        actuarComo(RolePurpose.COMPANY_ADMIN, 7);
        colaboradorDeLaClinica7();

        service.requestPasswordReset(pedido(10, null));

        ArgumentCaptor<PasswordResetToken> guardado = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(guardado.capture());
        assertThat(guardado.getValue().getCompany()).isSameAs(company);
        assertThat(guardado.getValue().getUsuario()).isSameAs(colaborador);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> modelo = ArgumentCaptor.forClass(Map.class);
        verify(emailService).createMail(eq("ana@example.test"), anyString(), modelo.capture());
        String enlace = (String) modelo.getValue().get("resetUrl");
        String token = enlace.substring(enlace.indexOf("#token=") + "#token=".length()).split("[&?]")[0];
        assertThat(enlace).startsWith("https://frontend.test/").contains("#token=");
        assertThat(guardado.getValue().getToken()).isNotEqualTo(token).hasSize(64);
        assertThat(modelo.getValue().get("initiatedByAdmin")).isEqualTo(true);
    }

    @Test
    void laAuditoriaQuedaARegistroDeQuienPidioYNoDeLaPersonaAfectada() {
        actuarComo(RolePurpose.COMPANY_ADMIN, 7);
        colaboradorDeLaClinica7();

        service.requestPasswordReset(pedido(10, null));

        ArgumentCaptor<String> detalle = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq(7), eq("SOLICITAR_RESET_ADMINISTRATIVO"), eq("Seguridad"), detalle.capture());
        assertThat(detalle.getValue()).contains("Ana Pérez").contains("ana@example.test");
        verify(auditLogService, never()).log(anyString(), anyString(), any(), anyString(), anyString(), anyString(),
                anyString(), any());
    }

    @Test
    void elLimiteDeSolicitudesEsPorClinicaParaQueUnaNoBloqueeALaOtra() {
        actuarComo(RolePurpose.COMPANY_ADMIN, 7);
        colaboradorDeLaClinica7();

        service.requestPasswordReset(pedido(10, null));

        verify(sharedRateLimitService).enforce("admin-reset-account", "7:ana@example.test", 3, Duration.ofHours(1));
    }

    @Test
    void unAdministradorDeClinicaNoPuedeElegirOtraEmpresaConElCuerpoDelPedido() {
        actuarComo(RolePurpose.COMPANY_ADMIN, 7);
        colaboradorDeLaClinica7();

        service.requestPasswordReset(pedido(10, 99));

        verify(companyMembershipService).hasAnyMembership(10, 7);
        verify(credencialRepository).findByUsuarioIdAndCompanyId(10, 7);
        verify(auditLogService).log(eq(7), anyString(), anyString(), anyString());
    }

    @Test
    void siElColaboradorNoEsDeLaClinicaNoSeCreaNadaNiSeEnviaNada() {
        actuarComo(RolePurpose.COMPANY_ADMIN, 7);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(colaborador));
        when(companyMembershipService.hasAnyMembership(10, 7)).thenReturn(false);

        assertThatThrownBy(() -> service.requestPasswordReset(pedido(10, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(passwordResetTokenRepository, emailService, auditLogService, sharedRateLimitService);
    }

    @Test
    void quienNoEsAdministradorNoPuedeRestablecerElAccesoDeUnAdministrador() {
        actuarComo(RolePurpose.CUSTOM, 7);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(colaborador));
        when(companyMembershipService.hasAnyMembership(10, 7)).thenReturn(true);
        when(companyRepository.findById(7)).thenReturn(Optional.of(company));
        doThrow(new AccessDeniedException("Solo un administrador puede gestionar la cuenta de otro administrador"))
                .when(administratorProtection).assertCanManage(colaborador, 7);

        assertThatThrownBy(() -> service.requestPasswordReset(pedido(10, null)))
                .isInstanceOf(AccessDeniedException.class);

        verify(passwordResetTokenRepository, never()).save(any());
        verifyNoInteractions(emailService, auditLogService, sharedRateLimitService);
    }

    @Test
    void unaCuentaSinActivarSeRechazaSinCrearToken() {
        actuarComo(RolePurpose.COMPANY_ADMIN, 7);
        colaborador.setEmailVerified(false);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(colaborador));
        when(companyMembershipService.hasAnyMembership(10, 7)).thenReturn(true);

        assertThatThrownBy(() -> service.requestPasswordReset(pedido(10, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("todavía no está habilitada");

        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void siNoSePuedeArmarElCorreoElAdministradorRecibeElErrorYNoUnFalsoExito() {
        actuarComo(RolePurpose.COMPANY_ADMIN, 7);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(colaborador));
        when(companyMembershipService.hasAnyMembership(10, 7)).thenReturn(true);
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(companyRepository.findById(7)).thenReturn(Optional.of(company));
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(new UsuarioEmpresaCredencial()));
        when(emailService.createMail(anyString(), anyString(), anyMap())).thenThrow(new IllegalStateException("plantilla"));

        assertThatThrownBy(() -> service.requestPasswordReset(pedido(10, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void laPlataformaRestableceAlColaboradorDeLaClinicaIndicada() {
        actuarComo(RolePurpose.PLATFORM_ADMIN, null);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(colaborador));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(companyRepository.findById(7)).thenReturn(Optional.of(company));
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(new UsuarioEmpresaCredencial()));
        when(emailService.createMail(anyString(), anyString(), anyMap())).thenReturn(new Mail());

        service.requestPasswordReset(pedido(10, 7));

        ArgumentCaptor<PasswordResetToken> guardado = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(guardado.capture());
        assertThat(guardado.getValue().getCompany()).isSameAs(company);
        verify(auditLogService).log(eq(7), eq("SOLICITAR_RESET_ADMINISTRATIVO"), eq("Seguridad"), anyString());
        verify(sharedRateLimitService).enforce("admin-reset-account", "7:ana@example.test", 3, Duration.ofHours(1));
    }

    @Test
    void laPlataformaPuedeBuscarAlColaboradorPorCorreoDentroDeLaClinicaIndicada() {
        actuarComo(RolePurpose.PLATFORM_ADMIN, null);
        AdminPasswordResetRequest pedido = new AdminPasswordResetRequest();
        pedido.setEmail("Ana@Example.test");
        pedido.setCompanyId(7);
        when(usuarioRepository.findAllByEmailIgnoreCase("ana@example.test")).thenReturn(java.util.List.of(colaborador));
        when(companyMembershipService.hasAnyMembership(10, 7)).thenReturn(true);
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(companyRepository.findById(7)).thenReturn(Optional.of(company));
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(new UsuarioEmpresaCredencial()));
        when(emailService.createMail(anyString(), anyString(), anyMap())).thenReturn(new Mail());

        service.requestPasswordReset(pedido);

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void laPlataformaSinEmpresaRestableceSoloCuentasDePlataforma() {
        actuarComo(RolePurpose.PLATFORM_ADMIN, null);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(colaborador));
        when(credencialRepository.findByUsuarioIdAndCompanyIsNull(10)).thenReturn(Optional.of(new UsuarioEmpresaCredencial()));
        when(emailService.createMail(anyString(), anyString(), anyMap())).thenReturn(new Mail());

        service.requestPasswordReset(pedido(10, null));

        ArgumentCaptor<PasswordResetToken> guardado = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(guardado.capture());
        assertThat(guardado.getValue().getCompany()).isNull();
        verify(sharedRateLimitService).enforce("admin-reset-account", "global:ana@example.test", 3, Duration.ofHours(1));
        verify(administratorProtection, never()).assertCanManage(any(), anyInt());
    }

    @Test
    void laPlataformaNoPuedeRestablecerUnaCuentaQueNoTieneCredencialEnLaEmpresaIndicada() {
        actuarComo(RolePurpose.PLATFORM_ADMIN, null);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(colaborador));
        when(companyMembershipService.hasActiveMembership(10, 8)).thenReturn(true);
        when(companyRepository.findById(8)).thenReturn(Optional.of(new Company()));
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 8)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestPasswordReset(pedido(10, 8)))
                .isInstanceOf(IllegalStateException.class);

        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void unaPersonaSinAccesoActivoEnLaClinicaNoRecibeElEnlace() {
        actuarComo(RolePurpose.COMPANY_ADMIN, 7);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(colaborador));
        when(companyMembershipService.hasAnyMembership(10, 7)).thenReturn(true);
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(false);
        when(companyRepository.findById(7)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> service.requestPasswordReset(pedido(10, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no tiene acceso activo");

        verify(passwordResetTokenRepository, never()).save(any());
        verifyNoInteractions(emailService, auditLogService, sharedRateLimitService);
    }

    @Test
    void unaPersonaConMembresiaEnDosEmpresasSeAlcanzaPorSuMembresiaYNoPorElCampoHeredado() {
        actuarComo(RolePurpose.COMPANY_ADMIN, 7);
        colaborador.setCompany(null);
        colaboradorDeLaClinica7();

        service.requestPasswordReset(pedido(10, null));

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(auditLogService).log(eq(7), eq("SOLICITAR_RESET_ADMINISTRATIVO"), eq("Seguridad"), anyString());
    }
}

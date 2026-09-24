package veterinaria.vargasvet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.EmailChangeRequest;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.dto.request.RequestEmailChangeDTO;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmailChangeRequestRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.util.AppClock;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailChangeServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock EmailChangeRequestRepository emailChangeRequestRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;
    @Mock SessionSecurityService sessionSecurityService;
    @Mock AuditLogService auditLogService;
    @Mock SharedRateLimitService sharedRateLimitService;
    @Mock UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock CompanyRepository companyRepository;

    @InjectMocks EmailChangeService service;

    @BeforeEach
    void configure() {
        ReflectionTestUtils.setField(service, "validityMinutes", 30L);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://frontend.test");
        ReflectionTestUtils.setField(service, "defaultCompanyName", "Veterinaria Test");
    }

    @Test
    void solicitudNoCambiaElCorreoAntesDeConfirmarAmbasDirecciones() {
        Usuario usuario = activeUser();
        RequestEmailChangeDTO dto = new RequestEmailChangeDTO();
        dto.setCurrentPassword("CurrentPassword-123");
        dto.setNewEmail(" Nuevo@Example.com ");

        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setPassword("bcrypt-hash");

        Company company = new Company();
        company.setId(3);
        company.setSlug("vargas-vet");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, Object>> modelCaptor = ArgumentCaptor.forClass(java.util.Map.class);

        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(credencialRepository.findByUsuarioIdAndCompanyId(usuario.getId(), 3)).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches(dto.getCurrentPassword(), credencial.getPassword())).thenReturn(true);
        when(usuarioRepository.existsByEmailIgnoreCaseAndCompanyIsNull("nuevo@example.com")).thenReturn(false);
        when(companyRepository.findById(3)).thenReturn(Optional.of(company));
        when(emailService.createMail(anyString(), anyString(), modelCaptor.capture())).thenReturn(new Mail());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(3);
            service.requestChange(usuario.getId(), dto);
        }

        ArgumentCaptor<EmailChangeRequest> captor = ArgumentCaptor.forClass(EmailChangeRequest.class);
        verify(emailChangeRequestRepository).save(captor.capture());
        EmailChangeRequest saved = captor.getValue();
        assertEquals("actual@example.com", usuario.getEmail());
        assertEquals("nuevo@example.com", saved.getNewEmail());
        assertEquals(company, saved.getCompany());
        // El enlace de confirmacion debe llevar el slug de LA SESION ACTIVA, nunca sin
        // slug - de lo contrario la persona termina en el login sin marca de empresa.
        assertTrue(modelCaptor.getAllValues().stream()
                .allMatch(model -> String.valueOf(model.get("confirmationUrl")).contains("/vargas-vet/confirm-email-change")));
        assertEquals(64, saved.getOldEmailTokenHash().length());
        assertEquals(64, saved.getNewEmailTokenHash().length());
        assertNotEquals(saved.getOldEmailTokenHash(), saved.getNewEmailTokenHash());
        verify(sessionSecurityService, never()).invalidateAllSessions(any());
    }

    @Test
    void correoSoloSeActualizaCuandoAmbasConfirmacionesSonValidas() {
        Usuario usuario = activeUser();
        EmailChangeRequest request = new EmailChangeRequest();
        request.setUsuario(usuario);
        request.setNewEmail("nuevo@example.com");
        request.setOldEmailTokenHash(SecurityTokenUtils.hash("old-token"));
        request.setNewEmailTokenHash(SecurityTokenUtils.hash("new-token"));
        request.setCreatedAt(AppClock.now());
        request.setExpiresAt(AppClock.now().plusMinutes(30));

        when(emailChangeRequestRepository.findByOldTokenForUpdate(SecurityTokenUtils.hash("old-token")))
                .thenReturn(Optional.of(request));
        when(emailChangeRequestRepository.findByNewTokenForUpdate(SecurityTokenUtils.hash("new-token")))
                .thenReturn(Optional.of(request));
        when(usuarioRepository.existsByEmailIgnoreCaseAndCompanyIsNull("nuevo@example.com")).thenReturn(false);

        assertFalse(service.confirmCurrentEmail("old-token"));
        assertEquals("actual@example.com", usuario.getEmail());

        assertTrue(service.confirmNewEmail("new-token"));
        assertEquals("nuevo@example.com", usuario.getEmail());
        assertTrue(usuario.isEmailVerified());
        verify(sessionSecurityService).invalidateAllSessions(usuario);
        verify(emailChangeRequestRepository).delete(request);
    }

    @Test
    void sincronizaElUsernameCuandoCoincideConElCorreoViejo() {
        // El login acepta username O correo en el mismo campo. Si la persona escribio su
        // correo como username al registrarse, cambiar solo Usuario.email dejaba el
        // correo VIEJO funcionando para siempre como credencial de acceso via el campo
        // username - justo el riesgo que se quiere evitar al cambiar de correo.
        Usuario usuario = activeUser();
        usuario.setUsername("actual@example.com");
        EmailChangeRequest request = new EmailChangeRequest();
        request.setUsuario(usuario);
        request.setNewEmail("nuevo@example.com");
        request.setOldEmailTokenHash(SecurityTokenUtils.hash("old-token"));
        request.setNewEmailTokenHash(SecurityTokenUtils.hash("new-token"));
        request.setCreatedAt(AppClock.now());
        request.setExpiresAt(AppClock.now().plusMinutes(30));

        when(emailChangeRequestRepository.findByOldTokenForUpdate(SecurityTokenUtils.hash("old-token")))
                .thenReturn(Optional.of(request));
        when(emailChangeRequestRepository.findByNewTokenForUpdate(SecurityTokenUtils.hash("new-token")))
                .thenReturn(Optional.of(request));
        when(usuarioRepository.existsByEmailIgnoreCaseAndCompanyIsNull("nuevo@example.com")).thenReturn(false);
        when(usuarioRepository.existsByUsernameIgnoreCaseAndCompanyIsNull("nuevo@example.com")).thenReturn(false);

        service.confirmCurrentEmail("old-token");
        assertTrue(service.confirmNewEmail("new-token"));

        assertEquals("nuevo@example.com", usuario.getEmail());
        assertEquals("nuevo@example.com", usuario.getUsername());
    }

    @Test
    void noTocaElUsernameCuandoEraDistintoDelCorreoViejo() {
        Usuario usuario = activeUser();
        usuario.setUsername("usuario.fijo");
        EmailChangeRequest request = new EmailChangeRequest();
        request.setUsuario(usuario);
        request.setNewEmail("nuevo@example.com");
        request.setOldEmailTokenHash(SecurityTokenUtils.hash("old-token"));
        request.setNewEmailTokenHash(SecurityTokenUtils.hash("new-token"));
        request.setCreatedAt(AppClock.now());
        request.setExpiresAt(AppClock.now().plusMinutes(30));

        when(emailChangeRequestRepository.findByOldTokenForUpdate(SecurityTokenUtils.hash("old-token")))
                .thenReturn(Optional.of(request));
        when(emailChangeRequestRepository.findByNewTokenForUpdate(SecurityTokenUtils.hash("new-token")))
                .thenReturn(Optional.of(request));
        when(usuarioRepository.existsByEmailIgnoreCaseAndCompanyIsNull("nuevo@example.com")).thenReturn(false);

        service.confirmCurrentEmail("old-token");
        assertTrue(service.confirmNewEmail("new-token"));

        assertEquals("nuevo@example.com", usuario.getEmail());
        assertEquals("usuario.fijo", usuario.getUsername());
    }

    private Usuario activeUser() {
        Usuario usuario = new Usuario();
        usuario.setId(10);
        usuario.setEmail("actual@example.com");
        usuario.setUsername("usuario.prueba");
        usuario.setNombre("Usuario");
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        return usuario;
    }
}

package veterinaria.vargasvet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.EmailChangeRequest;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.request.RequestEmailChangeDTO;
import veterinaria.vargasvet.repository.EmailChangeRequestRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityTokenUtils;
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

        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(dto.getCurrentPassword(), usuario.getPassword())).thenReturn(true);
        when(usuarioRepository.existsByEmail("nuevo@example.com")).thenReturn(false);

        service.requestChange(usuario.getEmail(), dto);

        ArgumentCaptor<EmailChangeRequest> captor = ArgumentCaptor.forClass(EmailChangeRequest.class);
        verify(emailChangeRequestRepository).save(captor.capture());
        EmailChangeRequest saved = captor.getValue();
        assertEquals("actual@example.com", usuario.getEmail());
        assertEquals("nuevo@example.com", saved.getNewEmail());
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
        when(usuarioRepository.existsByEmail("nuevo@example.com")).thenReturn(false);

        assertFalse(service.confirmCurrentEmail("old-token"));
        assertEquals("actual@example.com", usuario.getEmail());

        assertTrue(service.confirmNewEmail("new-token"));
        assertEquals("nuevo@example.com", usuario.getEmail());
        assertTrue(usuario.isEmailVerified());
        verify(sessionSecurityService).invalidateAllSessions(usuario);
        verify(emailChangeRequestRepository).delete(request);
    }

    private Usuario activeUser() {
        Usuario usuario = new Usuario();
        usuario.setId(10);
        usuario.setEmail("actual@example.com");
        usuario.setPassword("bcrypt-hash");
        usuario.setNombre("Usuario");
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        return usuario;
    }
}

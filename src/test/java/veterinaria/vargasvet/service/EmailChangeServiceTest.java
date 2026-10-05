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
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.dto.request.RequestEmailChangeDTO;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmailChangeRequestRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.util.AppClock;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.mockito.InOrder;

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
    @Mock CompanyMembershipService companyMembershipService;
    @Mock UsuarioPorRolRepository usuarioPorRolRepository;
    @Mock veterinaria.vargasvet.repository.PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock veterinaria.vargasvet.repository.EmpleadoRepository empleadoRepository;
    @Mock veterinaria.vargasvet.repository.ApoderadoRepository apoderadoRepository;
    @Mock veterinaria.vargasvet.repository.MascotaRepository mascotaRepository;
    @Mock jakarta.persistence.EntityManager entityManager;

    @InjectMocks EmailChangeService service;

    @BeforeEach
    void configure() {
        ReflectionTestUtils.setField(service, "validityMinutes", 30L);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://frontend.test");
        ReflectionTestUtils.setField(service, "defaultCompanyName", "Veterinaria Test");
        ReflectionTestUtils.setField(service, "administrativeValidityHours", 24L);
    }

    @Test
    void solicitudNoCambiaElCorreoAntesDeConfirmarAmbasDirecciones() {
        Usuario usuario = activeUser();
        RequestEmailChangeDTO dto = new RequestEmailChangeDTO();
        dto.setCurrentPassword("CurrentPassword-123");
        dto.setNewEmail(" Nuevo@Example.com ");

        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setPassword("bcrypt-hash");
        credencial.setPasswordChanged(true);

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

    // ---------- cambio propio: cuentas sin contraseña propia y duplicados entre clínicas ----------

    private RequestEmailChangeDTO solicitudPropia(String nuevoCorreo) {
        RequestEmailChangeDTO dto = new RequestEmailChangeDTO();
        dto.setCurrentPassword("CurrentPassword-123");
        dto.setNewEmail(nuevoCorreo);
        return dto;
    }

    @Test
    void quienActivoConGoogleYNuncaCreoContrasenaRecibeUnaIndicacionClaraEnVezDeContrasenaIncorrecta() {
        Usuario usuario = activeUser();
        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setPassword("hash-aleatorio-que-nadie-conoce");
        credencial.setPasswordChanged(false);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(credencialRepository.findByUsuarioIdAndCompanyId(usuario.getId(), 3)).thenReturn(Optional.of(credencial));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(3);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> service.requestChange(usuario.getId(), solicitudPropia("nuevo@example.com")));
            assertTrue(error.getMessage().contains("Olvidé mi contraseña"));
        }

        verifyNoInteractions(passwordEncoder);
        verify(emailChangeRequestRepository, never()).save(any());
    }

    @Test
    void elCorreoNuevoSeRechazaSiOtroUsuarioLoTieneEnAlgunaClinicaDeLaPersona() {
        Usuario usuario = activeUser();
        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setPassword("bcrypt-hash");
        credencial.setPasswordChanged(true);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(credencialRepository.findByUsuarioIdAndCompanyId(usuario.getId(), 3)).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("CurrentPassword-123", "bcrypt-hash")).thenReturn(true);
        when(companyMembershipService.isEmailTakenInUserCompanies(usuario, "ocupado@example.com")).thenReturn(true);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(3);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> service.requestChange(usuario.getId(), solicitudPropia("ocupado@example.com")));
            assertEquals("El nuevo correo no está disponible", error.getMessage());
        }

        verify(emailChangeRequestRepository, never()).save(any());
    }

    @Test
    void unaNuevaSolicitudBorraYVaciaLaAnteriorAntesDeGuardar() {
        Usuario usuario = activeUser();
        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setPassword("bcrypt-hash");
        credencial.setPasswordChanged(true);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(credencialRepository.findByUsuarioIdAndCompanyId(usuario.getId(), 3)).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("CurrentPassword-123", "bcrypt-hash")).thenReturn(true);
        when(emailService.createMail(anyString(), anyString(), any())).thenReturn(new Mail());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(3);
            service.requestChange(usuario.getId(), solicitudPropia("nuevo@example.com"));
        }

        InOrder orden = inOrder(emailChangeRequestRepository);
        orden.verify(emailChangeRequestRepository).deleteByUsuario(usuario);
        orden.verify(emailChangeRequestRepository).flush();
        orden.verify(emailChangeRequestRepository).save(any(EmailChangeRequest.class));
    }

    // ---------- cambio administrativo (la persona perdió el acceso a su correo) ----------

    private Usuario cuentaDelPersonal() {
        Usuario usuario = activeUser();
        usuario.setId(20);
        usuario.setEmail("sin.acceso@example.com");
        return usuario;
    }

    private UsuarioPorRol asignacion(RolePurpose purpose, boolean activo) {
        Role role = new Role();
        role.setPurpose(purpose);
        role.setActivo(activo);
        UsuarioPorRol assignment = new UsuarioPorRol();
        assignment.setRol(role);
        return assignment;
    }

    private void prepararAdministradorDeClinica(MockedStatic<SecurityUtils> security) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(99);
        security.when(SecurityUtils::isSuperAdmin).thenReturn(false);
        security.when(SecurityUtils::getCurrentCompanyId).thenReturn(3);
    }

    @Test
    void elAdministradorNoPuedeCambiarSuPropioCorreoPorLaViaAdministrativa() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(99);

            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> service.requestAdministrativeChange(99, "otro@example.com", "Verificado con DNI en mostrador"));
            assertTrue(error.getMessage().contains("tu propio correo"));
        }

        verifyNoInteractions(usuarioRepository, emailChangeRequestRepository, emailService);
    }

    @Test
    void unAdministradorDeClinicaNoPuedeCambiarElCorreoDeOtraCuentaAdministradora() {
        Usuario otroAdmin = cuentaDelPersonal();
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(otroAdmin));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(true);
        when(usuarioPorRolRepository.findByUsuarioId(20)).thenReturn(List.of(asignacion(RolePurpose.COMPANY_ADMIN, true)));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> service.requestAdministrativeChange(20, "otro@example.com", "Verificado con DNI en mostrador"));
        }

        verify(emailChangeRequestRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void unAdministradorDeClinicaNoGestionaCuentasDeOtraEmpresa() {
        Usuario deOtraEmpresa = cuentaDelPersonal();
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(deOtraEmpresa));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(false);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            assertThrows(veterinaria.vargasvet.exception.ResourceNotFoundException.class,
                    () -> service.requestAdministrativeChange(20, "otro@example.com", "Verificado con DNI en mostrador"));
        }

        verify(emailChangeRequestRepository, never()).save(any());
    }

    @Test
    void elCambioAdministrativoNoSeAplicaEnElActoYEsperaLaConfirmacionDelCorreoNuevo() {
        Usuario usuario = cuentaDelPersonal();
        Company company = new Company();
        company.setId(3);
        company.setSlug("vargas-vet");
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(true);
        when(usuarioPorRolRepository.findByUsuarioId(20)).thenReturn(List.of(asignacion(RolePurpose.CUSTOM, true)));
        when(companyRepository.findById(3)).thenReturn(Optional.of(company));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, Object>> modelCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        ArgumentCaptor<String> destinoCaptor = ArgumentCaptor.forClass(String.class);
        when(emailService.createMail(destinoCaptor.capture(), anyString(), modelCaptor.capture())).thenReturn(new Mail());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            service.requestAdministrativeChange(20, " Nuevo@Example.com ", "  Verificado con DNI en mostrador  ");
        }

        ArgumentCaptor<EmailChangeRequest> captor = ArgumentCaptor.forClass(EmailChangeRequest.class);
        verify(emailChangeRequestRepository).save(captor.capture());
        EmailChangeRequest saved = captor.getValue();
        assertEquals("sin.acceso@example.com", usuario.getEmail());
        assertEquals("nuevo@example.com", saved.getNewEmail());
        assertNotNull(saved.getOldEmailConfirmedAt());
        assertNull(saved.getNewEmailConfirmedAt());
        assertTrue(saved.getExpiresAt().isAfter(AppClock.now().plusHours(23)));
        verify(sessionSecurityService, never()).invalidateAllSessions(any());

        // Al correo nuevo: enlace de confirmación del tipo "nuevo", con el slug de la clínica.
        // Al correo anterior: solo un aviso, sin ningún enlace de confirmación.
        int nuevo = destinoCaptor.getAllValues().indexOf("nuevo@example.com");
        int anterior = destinoCaptor.getAllValues().indexOf("sin.acceso@example.com");
        assertTrue(nuevo >= 0 && anterior >= 0);
        assertEquals(true, modelCaptor.getAllValues().get(nuevo).get("administrative"));
        assertTrue(String.valueOf(modelCaptor.getAllValues().get(nuevo).get("confirmationUrl"))
                .contains("/vargas-vet/confirm-email-change#type=nuevo&token="));
        assertNull(modelCaptor.getAllValues().get(anterior).get("confirmationUrl"));
        verify(emailService).sendEmailWithRetry(any(), eq("email/email-change-admin-notice-template"));
        String cancelUrl = String.valueOf(modelCaptor.getAllValues().get(anterior).get("cancelUrl"));
        assertTrue(cancelUrl.contains("/vargas-vet/confirm-email-change#type=cancelar&token="));
        String cancelToken = cancelUrl.substring(cancelUrl.indexOf("&token=") + "&token=".length());
        assertEquals(SecurityTokenUtils.hash(cancelToken), saved.getOldEmailTokenHash());

        // La auditoría nombra a la persona y los dos correos, con la empresa y la nota; nunca un id interno.
        ArgumentCaptor<String> detalle = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq(3), eq("SOLICITAR_CAMBIO_CORREO_ADMINISTRATIVO"), eq("Seguridad"), detalle.capture());
        assertTrue(detalle.getValue().contains("Usuario (sin.acceso@example.com) a nuevo@example.com"));
        assertTrue(detalle.getValue().contains("Nota: Verificado con DNI en mostrador"));
        assertFalse(detalle.getValue().contains("ID"));
    }

    @Test
    void laNotaDeAuditoriaEsOpcional() {
        Usuario usuario = cuentaDelPersonal();
        when(companyRepository.findById(3)).thenReturn(Optional.of(clinica()));
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(true);
        when(usuarioPorRolRepository.findByUsuarioId(20)).thenReturn(List.of());
        when(emailService.createMail(anyString(), anyString(), any())).thenReturn(new Mail());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            service.requestAdministrativeChange(20, "nuevo@example.com", null);
        }

        ArgumentCaptor<String> detalle = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq(3), eq("SOLICITAR_CAMBIO_CORREO_ADMINISTRATIVO"), eq("Seguridad"), detalle.capture());
        assertEquals("Un administrador solicitó cambiar el correo de Usuario (sin.acceso@example.com) a nuevo@example.com",
                detalle.getValue());
    }

    @Test
    void elMismoCorreoEnOtraEmpresaNoEsUnConflicto() {
        Usuario usuario = cuentaDelPersonal();
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(true);
        when(usuarioPorRolRepository.findByUsuarioId(20)).thenReturn(List.of());
        when(emailService.createMail(anyString(), anyString(), any())).thenReturn(new Mail());
        // Hay otra cuenta con ese correo en otra empresa: el sistema solo compara dentro de la empresa de la cuenta.
        when(companyMembershipService.isEmailTakenInUserCompanies(usuario, "nuevo@example.com")).thenReturn(false);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            service.requestAdministrativeChange(20, "nuevo@example.com", "Verificado con DNI en mostrador");
        }

        verify(emailChangeRequestRepository).save(any(EmailChangeRequest.class));
    }

    @Test
    void elCambioAdministrativoSeCompletaCuandoLaPersonaConfirmaSoloElCorreoNuevo() {
        Usuario usuario = cuentaDelPersonal();
        EmailChangeRequest request = new EmailChangeRequest();
        request.setUsuario(usuario);
        request.setNewEmail("nuevo@example.com");
        request.setOldEmailTokenHash(SecurityTokenUtils.hash("nunca-enviado"));
        request.setOldEmailConfirmedAt(AppClock.now());
        request.setNewEmailTokenHash(SecurityTokenUtils.hash("new-token"));
        request.setCreatedAt(AppClock.now());
        request.setExpiresAt(AppClock.now().plusHours(24));
        when(emailChangeRequestRepository.findByNewTokenForUpdate(SecurityTokenUtils.hash("new-token")))
                .thenReturn(Optional.of(request));

        assertTrue(service.confirmNewEmail("new-token"));

        assertEquals("nuevo@example.com", usuario.getEmail());
        assertTrue(usuario.isEmailVerified());
        verify(sessionSecurityService).invalidateAllSessions(usuario);
        verify(emailChangeRequestRepository).delete(request);
    }

    @Test
    void elCambioAdministrativoSeRechazaSiLaCuentaNoEstaActivadaOElCorreoYaEstaOcupado() {
        Usuario sinActivar = cuentaDelPersonal();
        sinActivar.setEmailVerified(false);
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(sinActivar));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(true);
        when(usuarioPorRolRepository.findByUsuarioId(20)).thenReturn(List.of());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            assertThrows(IllegalStateException.class,
                    () -> service.requestAdministrativeChange(20, "otro@example.com", "Verificado con DNI en mostrador"));

            sinActivar.setEmailVerified(true);
            when(companyMembershipService.isEmailTakenInUserCompanies(sinActivar, "ocupado@example.com")).thenReturn(true);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> service.requestAdministrativeChange(20, "ocupado@example.com", "Verificado con DNI en mostrador"));
            assertEquals("El nuevo correo no está disponible", error.getMessage());
        }

        verify(emailChangeRequestRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void elAdministradorDePlataformaSiPuedeGestionarCuentasAdministradorasYDeVariasClinicas() {
        Usuario adminDeClinica = cuentaDelPersonal();
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(adminDeClinica));
        when(companyMembershipService.getActiveCompanyIds(adminDeClinica)).thenReturn(Set.of(3));
        when(emailService.createMail(anyString(), anyString(), any())).thenReturn(new Mail());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(1);
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(null);
            service.requestAdministrativeChange(20, "nuevo@example.com", "Verificado en llamada con soporte");
        }

        verify(emailChangeRequestRepository).save(any(EmailChangeRequest.class));
        verifyNoInteractions(usuarioPorRolRepository);
    }

    // ---------- "No fui yo": cancelar la solicitud desde el correo actual ----------

    @Test
    void elCorreoActualDelCambioPropioIncluyeUnEnlaceParaCancelarConSuMismoToken() {
        Usuario usuario = activeUser();
        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setPassword("bcrypt-hash");
        credencial.setPasswordChanged(true);
        Company company = new Company();
        company.setId(3);
        company.setSlug("vargas-vet");
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(credencialRepository.findByUsuarioIdAndCompanyId(usuario.getId(), 3)).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("CurrentPassword-123", "bcrypt-hash")).thenReturn(true);
        when(companyRepository.findById(3)).thenReturn(Optional.of(company));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, Object>> modelCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        ArgumentCaptor<String> destinoCaptor = ArgumentCaptor.forClass(String.class);
        when(emailService.createMail(destinoCaptor.capture(), anyString(), modelCaptor.capture())).thenReturn(new Mail());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(3);
            service.requestChange(usuario.getId(), solicitudPropia("nuevo@example.com"));
        }

        int actual = destinoCaptor.getAllValues().indexOf("actual@example.com");
        int nuevo = destinoCaptor.getAllValues().indexOf("nuevo@example.com");
        String confirmUrl = String.valueOf(modelCaptor.getAllValues().get(actual).get("confirmationUrl"));
        String cancelUrl = String.valueOf(modelCaptor.getAllValues().get(actual).get("cancelUrl"));
        assertTrue(cancelUrl.contains("/vargas-vet/confirm-email-change#type=cancelar&token="));
        assertEquals(confirmUrl.substring(confirmUrl.indexOf("&token=")), cancelUrl.substring(cancelUrl.indexOf("&token=")));
        // El correo nuevo no ofrece cancelar: quien no lo reconoce simplemente no confirma.
        assertNull(modelCaptor.getAllValues().get(nuevo).get("cancelUrl"));
    }

    @Test
    void cancelarBorraLaSolicitudYDejaConstanciaEnLaAuditoria() {
        Usuario usuario = activeUser();
        Company company = new Company();
        company.setId(3);
        company.setName("Vargas Vet");
        EmailChangeRequest request = new EmailChangeRequest();
        request.setUsuario(usuario);
        request.setCompany(company);
        request.setNewEmail("intruso@example.com");
        request.setOldEmailTokenHash(SecurityTokenUtils.hash("old-token"));
        request.setNewEmailTokenHash(SecurityTokenUtils.hash("new-token"));
        request.setCreatedAt(AppClock.now());
        request.setExpiresAt(AppClock.now().plusMinutes(30));
        when(emailChangeRequestRepository.findByOldTokenForUpdate(SecurityTokenUtils.hash("old-token")))
                .thenReturn(Optional.of(request));

        service.cancelRequest("old-token");

        verify(emailChangeRequestRepository).delete(request);
        assertEquals("actual@example.com", usuario.getEmail());
        verify(auditLogService).log(eq("actual@example.com"), eq("USER"), eq(3), eq("Vargas Vet"),
                eq("CANCELAR_CAMBIO_CORREO"), eq("Seguridad"), anyString(), isNull());
        verify(sessionSecurityService, never()).invalidateAllSessions(any());
    }

    @Test
    void cancelarConUnEnlaceInvalidoOYaUsadoSeRechaza() {
        when(emailChangeRequestRepository.findByOldTokenForUpdate(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.cancelRequest("token-desconocido"));

        verify(emailChangeRequestRepository, never()).delete(any(EmailChangeRequest.class));
        verifyNoInteractions(auditLogService);
    }


    private EmailChangeRequest solicitudReciente(Usuario usuario, String nuevoCorreo, int hacerMinutos) {
        EmailChangeRequest request = new EmailChangeRequest();
        request.setUsuario(usuario);
        request.setNewEmail(nuevoCorreo);
        request.setCreatedAt(AppClock.now().minusMinutes(hacerMinutos));
        request.setExpiresAt(AppClock.now().plusHours(24).minusMinutes(hacerMinutos));
        return request;
    }

    @Test
    void elMismoPedidoRepetidoALosPocosMinutosNoGeneraOtroEnlaceNiOtroCorreo() {
        Usuario usuario = cuentaDelPersonal();
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(true);
        when(usuarioPorRolRepository.findByUsuarioId(20)).thenReturn(List.of());
        when(emailChangeRequestRepository.findByUsuario(usuario))
                .thenReturn(Optional.of(solicitudReciente(usuario, "nuevo@example.com", 1)));

        EmailChangeService.AdministrativeResult resultado;
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            resultado = service.requestAdministrativeChange(20, " Nuevo@Example.com ", null);
        }

        assertEquals(EmailChangeService.AdministrativeResult.CONFIRMATION_PENDING, resultado);
        verify(emailChangeRequestRepository, never()).save(any());
        verify(emailChangeRequestRepository, never()).deleteByUsuario(any());
        verifyNoInteractions(emailService);
        verify(auditLogService, never()).log(any(Integer.class), anyString(), anyString(), anyString());
    }

    @Test
    void elMismoPedidoDespuesDeVariosMinutosSiReemplazaAlAnterior() {
        Usuario usuario = cuentaDelPersonal();
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(true);
        when(usuarioPorRolRepository.findByUsuarioId(20)).thenReturn(List.of());
        when(companyRepository.findById(3)).thenReturn(Optional.of(clinica()));
        when(emailChangeRequestRepository.findByUsuario(usuario))
                .thenReturn(Optional.of(solicitudReciente(usuario, "nuevo@example.com", 10)));
        when(emailService.createMail(anyString(), anyString(), any())).thenReturn(new Mail());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            service.requestAdministrativeChange(20, "nuevo@example.com", null);
        }

        verify(emailChangeRequestRepository).save(any(EmailChangeRequest.class));
    }

    @Test
    void laCorreccionDeUnaCuentaPendienteRepetidaALosPocosMinutosNoVuelveAInvitar() {
        Usuario usuario = cuentaPendienteConCorreoEquivocado();
        usuario.setEmail("correcto@example.com");
        usuario.setVerificationTokenExpiresAt(AppClock.now().plusHours(24).minusMinutes(1));
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(true);
        when(usuarioPorRolRepository.findByUsuarioId(20)).thenReturn(List.of());

        EmailChangeService.AdministrativeResult resultado;
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            resultado = service.requestAdministrativeChange(20, "correcto@example.com", null);
        }

        assertEquals(EmailChangeService.AdministrativeResult.PENDING_ACCOUNT_CORRECTED_WITH_INVITATION, resultado);
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void elBloqueoDeLaPersonaSeTomaYSeVuelveALeerElEstadoActual() {
        Usuario usuario = cuentaDelPersonal();
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(true);
        when(usuarioPorRolRepository.findByUsuarioId(20)).thenReturn(List.of());
        when(companyRepository.findById(3)).thenReturn(Optional.of(clinica()));
        when(emailService.createMail(anyString(), anyString(), any())).thenReturn(new Mail());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            service.requestAdministrativeChange(20, "nuevo@example.com", null);
        }

        InOrder bloqueo = inOrder(entityManager);
        bloqueo.verify(entityManager).lock(usuario, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        bloqueo.verify(entityManager).refresh(usuario);
    }

    private Usuario cuentaPendienteConCorreoEquivocado() {
        Usuario usuario = cuentaDelPersonal();
        usuario.setEmail("equivocado@example.com");
        usuario.setActivo(false);
        usuario.setEmailVerified(false);
        usuario.setVerificationToken("hash-anterior");
        return usuario;
    }

    private Company clinica() {
        Company company = new Company();
        company.setId(3);
        company.setSlug("vargas-vet");
        company.setName("Vargas Vet");
        return company;
    }

    private void prepararCuentaPendiente(Usuario usuario) {
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(true);
        when(usuarioPorRolRepository.findByUsuarioId(20)).thenReturn(List.of());
        when(companyRepository.findById(3)).thenReturn(Optional.of(clinica()));
    }

    @Test
    void unaCuentaPendienteConElCorreoMalEscritoSeCorrigeEnElActoYSeInvitaAlCorreoCorregido() {
        Usuario usuario = cuentaPendienteConCorreoEquivocado();
        prepararCuentaPendiente(usuario);
        when(empleadoRepository.existsByUserIdAndCompanyId(20, 3)).thenReturn(true);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, Object>> model = ArgumentCaptor.forClass(java.util.Map.class);
        ArgumentCaptor<String> destino = ArgumentCaptor.forClass(String.class);
        when(emailService.createMail(destino.capture(), anyString(), model.capture())).thenReturn(new Mail());
        when(emailService.sendEmailWithRetry(any(), anyString()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(true));

        EmailChangeService.AdministrativeResult resultado;
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            resultado = service.requestAdministrativeChange(20, " Correcto@Example.com ", "Se equivocó al escribirlo");
        }

        assertEquals(EmailChangeService.AdministrativeResult.PENDING_ACCOUNT_CORRECTED_WITH_INVITATION, resultado);
        assertEquals("correcto@example.com", usuario.getEmail());
        assertNotEquals("hash-anterior", usuario.getVerificationToken());
        assertNotNull(usuario.getVerificationTokenExpiresAt());
        assertFalse(usuario.isEmailVerified());
        assertEquals("correcto@example.com", destino.getValue());
        assertTrue(String.valueOf(model.getValue().get("verificationLink")).contains("/vargas-vet/auth/verify#token="));
        verify(emailService).sendEmailWithRetry(any(), eq("email/welcome-template"));
        verify(emailChangeRequestRepository, never()).save(any());
        verify(sessionSecurityService, never()).invalidateAllSessions(any());
        ArgumentCaptor<String> detalle = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq(3), eq("CORREGIR_CORREO_CUENTA_PENDIENTE"), eq("Seguridad"), detalle.capture());
        assertTrue(detalle.getValue().contains("equivocado@example.com → correcto@example.com"));
        assertTrue(detalle.getValue().contains("Nota: Se equivocó al escribirlo"));
        assertFalse(detalle.getValue().contains("ID"));
    }

    @Test
    void alCorregirElCorreoDeUnaCuentaPendienteElUsuarioQueEraElCorreoViejoSeSincroniza() {
        Usuario usuario = cuentaPendienteConCorreoEquivocado();
        usuario.setUsername("equivocado@example.com");
        prepararCuentaPendiente(usuario);
        when(empleadoRepository.existsByUserIdAndCompanyId(20, 3)).thenReturn(true);
        when(emailService.createMail(anyString(), anyString(), any())).thenReturn(new Mail());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            service.requestAdministrativeChange(20, "correcto@example.com", null);
        }

        assertEquals("correcto@example.com", usuario.getUsername());
    }

    @Test
    void aUnClienteSinMascotasSeLeCorrigeElCorreoPeroTodaviaNoSeLeInvita() {
        Usuario usuario = cuentaPendienteConCorreoEquivocado();
        prepararCuentaPendiente(usuario);
        when(empleadoRepository.existsByUserIdAndCompanyId(20, 3)).thenReturn(false);
        veterinaria.vargasvet.domain.entity.Apoderado apoderado = new veterinaria.vargasvet.domain.entity.Apoderado();
        apoderado.setId(40L);
        when(apoderadoRepository.findByUserIdAndCompanyId(20, 3)).thenReturn(Optional.of(apoderado));
        when(mascotaRepository.existsByApoderadoIdAndActivoTrue(40L)).thenReturn(false);

        EmailChangeService.AdministrativeResult resultado;
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            resultado = service.requestAdministrativeChange(20, "correcto@example.com", null);
        }

        assertEquals(EmailChangeService.AdministrativeResult.PENDING_ACCOUNT_CORRECTED, resultado);
        assertEquals("correcto@example.com", usuario.getEmail());
        assertNull(usuario.getVerificationToken());
        verifyNoInteractions(emailService);
    }

    @Test
    void siLaInvitacionDeUnaCuentaPendienteNoSaleSePideReintentarYNoQuedaConstancia() {
        Usuario usuario = cuentaPendienteConCorreoEquivocado();
        prepararCuentaPendiente(usuario);
        when(empleadoRepository.existsByUserIdAndCompanyId(20, 3)).thenReturn(true);
        when(emailService.createMail(anyString(), anyString(), any())).thenReturn(new Mail());
        when(emailService.sendEmailWithRetry(any(), anyString()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(false));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            assertThrows(veterinaria.vargasvet.exception.MailDeliveryException.class,
                    () -> service.requestAdministrativeChange(20, "correcto@example.com", null));
        }

        verify(auditLogService, never()).log(any(Integer.class), anyString(), anyString(), anyString());
    }

    @Test
    void unaCuentaPendienteTampocoSeGestionaSiEsAdministradora() {
        Usuario usuario = cuentaPendienteConCorreoEquivocado();
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(true);
        when(usuarioPorRolRepository.findByUsuarioId(20)).thenReturn(List.of(asignacion(RolePurpose.COMPANY_ADMIN, true)));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> service.requestAdministrativeChange(20, "correcto@example.com", null));
        }

        assertEquals("equivocado@example.com", usuario.getEmail());
    }

    @Test
    void siElCorreoNuevoDelCambioAdministrativoNoSaleSePideReintentarYNoQuedaConstancia() {
        Usuario usuario = cuentaDelPersonal();
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(true);
        when(usuarioPorRolRepository.findByUsuarioId(20)).thenReturn(List.of());
        when(companyRepository.findById(3)).thenReturn(Optional.of(clinica()));
        when(emailService.createMail(anyString(), anyString(), any())).thenReturn(new Mail());
        when(emailService.sendEmailWithRetry(any(), anyString()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(false));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            assertThrows(veterinaria.vargasvet.exception.MailDeliveryException.class,
                    () -> service.requestAdministrativeChange(20, "nuevo@example.com", null));
        }

        verify(emailService, never()).sendEmailWithRetry(any(), eq("email/email-change-admin-notice-template"));
        verify(auditLogService, never()).log(any(Integer.class), anyString(), anyString(), anyString());
    }

    @Test
    void siUnCorreoDelCambioPropioNoSaleSePideReintentar() {
        Usuario usuario = activeUser();
        RequestEmailChangeDTO dto = new RequestEmailChangeDTO();
        dto.setCurrentPassword("CurrentPassword-123");
        dto.setNewEmail("nuevo@example.com");
        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setPassword("bcrypt-hash");
        credencial.setPasswordChanged(true);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(credencialRepository.findByUsuarioIdAndCompanyId(usuario.getId(), 3)).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches(dto.getCurrentPassword(), credencial.getPassword())).thenReturn(true);
        when(companyRepository.findById(3)).thenReturn(Optional.of(clinica()));
        when(emailService.createMail(anyString(), anyString(), any())).thenReturn(new Mail());
        when(emailService.sendEmailWithRetry(any(), anyString()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(true))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(false));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(3);
            assertThrows(veterinaria.vargasvet.exception.MailDeliveryException.class,
                    () -> service.requestChange(usuario.getId(), dto));
        }

        verify(auditLogService, never()).log(any(Integer.class), anyString(), anyString(), anyString());
    }

    @Test
    void elContadorDelCambioAdministrativoEsPorEmpresaYSoloSeConsumeSiLaCuentaEsDeEsaEmpresa() {
        Usuario deOtraEmpresa = cuentaDelPersonal();
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(deOtraEmpresa));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(false);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            assertThrows(veterinaria.vargasvet.exception.ResourceNotFoundException.class,
                    () -> service.requestAdministrativeChange(20, "otro@example.com", null));
        }
        verifyNoInteractions(sharedRateLimitService);

        Usuario propia = cuentaDelPersonal();
        when(usuarioRepository.findById(21)).thenReturn(Optional.of(propia));
        when(companyMembershipService.hasActiveMembership(21, 3)).thenReturn(true);
        when(usuarioPorRolRepository.findByUsuarioId(propia.getId())).thenReturn(List.of());
        when(companyRepository.findById(3)).thenReturn(Optional.of(clinica()));
        when(emailService.createMail(anyString(), anyString(), any())).thenReturn(new Mail());
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            prepararAdministradorDeClinica(security);
            service.requestAdministrativeChange(21, "otro@example.com", null);
        }
        verify(sharedRateLimitService).enforce(eq("admin-email-change-target"), eq("3:21"), anyInt(), any());
    }

    @Test
    void alCompletarseElCambioSeAnulanLosEnlacesDeRestablecimientoPendientes() {
        Usuario usuario = cuentaDelPersonal();
        EmailChangeRequest request = solicitudAdministrativa(usuario);
        when(emailChangeRequestRepository.findByNewTokenForUpdate(SecurityTokenUtils.hash("new-token")))
                .thenReturn(Optional.of(request));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(true);
        when(emailService.createMail(anyString(), anyString(), any())).thenReturn(new Mail());

        assertTrue(service.confirmNewEmail("new-token"));

        verify(passwordResetTokenRepository).deleteByUsuario(usuario);
        ArgumentCaptor<String> detalle = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq("nuevo@example.com"), eq("USER"), eq(3), any(), eq("CONFIRMAR_CAMBIO_CORREO"),
                eq("Seguridad"), detalle.capture(), isNull());
        assertTrue(detalle.getValue().contains("sin.acceso@example.com → nuevo@example.com"));
        assertFalse(detalle.getValue().contains("ID"));
    }

    @Test
    void elCambioNoSeCompletaSiLaPersonaYaNoTieneAccesoActivoALaEmpresa() {
        Usuario usuario = cuentaDelPersonal();
        EmailChangeRequest request = solicitudAdministrativa(usuario);
        when(emailChangeRequestRepository.findByNewTokenForUpdate(SecurityTokenUtils.hash("new-token")))
                .thenReturn(Optional.of(request));
        when(companyMembershipService.hasActiveMembership(20, 3)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.confirmNewEmail("new-token"));

        assertEquals("sin.acceso@example.com", usuario.getEmail());
        verify(sessionSecurityService, never()).invalidateAllSessions(any());
        verify(passwordResetTokenRepository, never()).deleteByUsuario(any());
    }

    private EmailChangeRequest solicitudAdministrativa(Usuario usuario) {
        EmailChangeRequest request = new EmailChangeRequest();
        request.setUsuario(usuario);
        request.setCompany(clinica());
        request.setNewEmail("nuevo@example.com");
        request.setOldEmailTokenHash(SecurityTokenUtils.hash("nunca-enviado"));
        request.setOldEmailConfirmedAt(AppClock.now());
        request.setNewEmailTokenHash(SecurityTokenUtils.hash("new-token"));
        request.setCreatedAt(AppClock.now());
        request.setExpiresAt(AppClock.now().plusHours(24));
        return request;
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

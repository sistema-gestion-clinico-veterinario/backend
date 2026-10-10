package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.exception.MailDeliveryException;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
 * Reenvío administrativo de la invitación de activación (empleados y clientes): solo para
 * cuentas pendientes con vínculo vigente, dentro de la empresa del administrador y con un
 * margen mínimo entre envíos.
 */
@ExtendWith(MockitoExtension.class)
class InvitacionActivacionReenvioTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock ApoderadoRepository apoderadoRepository;
    @Mock UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock EmailService emailService;
    @Mock MascotaRepository mascotaRepository;
    @Mock EntityManager entityManager;
    @Mock AuditLogService auditLogService;
    @Mock veterinaria.vargasvet.service.PetOwnershipService petOwnershipService;

    @InjectMocks EmpleadoServiceImpl empleadoService;
    @InjectMocks ApoderadoServiceImpl apoderadoService;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(3);
        company.setName("Vargas Vet");
        company.setSlug("vargas-vet");

        UsuarioPrincipal principal = new UsuarioPrincipal(
                1, "admin@empresa.test", "", List.of(), 3,
                2, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        for (Object service : new Object[]{empleadoService, apoderadoService}) {
            ReflectionTestUtils.setField(service, "appUrl", "https://frontend.test");
            ReflectionTestUtils.setField(service, "verificationTokenValidityHours", 24L);
            ReflectionTestUtils.setField(service, "entityManager", entityManager);
        }
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuarioPendiente() {
        Usuario usuario = new Usuario();
        usuario.setId(20);
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        usuario.setEmail("ana@empresa.test");
        usuario.setActivo(false);
        usuario.setEmailVerified(false);
        usuario.setCompany(company);
        usuario.setVerificationToken("hash-anterior");
        usuario.setVerificationTokenExpiresAt(AppClock.now().plusHours(24).minusHours(2));
        return usuario;
    }

    private Empleado empleado(Usuario usuario, boolean estado) {
        Empleado empleado = new Empleado();
        empleado.setId(30L);
        empleado.setUser(usuario);
        empleado.setCompany(company);
        empleado.setEstado(estado);
        return empleado;
    }

    private Apoderado apoderado(Usuario usuario, boolean estado) {
        Apoderado apoderado = new Apoderado();
        apoderado.setId(40L);
        apoderado.setUser(usuario);
        apoderado.setCompany(company);
        apoderado.setEstado(estado);
        return apoderado;
    }

    private ArgumentCaptor<Map<String, Object>> stubMail() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> model = ArgumentCaptor.forClass(Map.class);
        when(emailService.createMail(eq("ana@empresa.test"), anyString(), model.capture()))
                .thenAnswer(i -> new Mail(null, i.getArgument(0), i.getArgument(1), i.getArgument(2)));
        when(emailService.sendEmailWithRetry(any(), anyString())).thenReturn(CompletableFuture.completedFuture(true));
        return model;
    }

    @Test
    void empleadoPendienteRecibeUnEnlaceNuevoConLaMarcaDeSuClinica() {
        Usuario usuario = usuarioPendiente();
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado(usuario, true)));
        when(credencialRepository.findAllByUsuarioId(20)).thenReturn(List.of());
        ArgumentCaptor<Map<String, Object>> model = stubMail();

        empleadoService.reenviarInvitacion(30L);

        assertThat(usuario.getVerificationToken()).isNotEqualTo("hash-anterior").isNotNull();
        assertThat(usuario.getVerificationTokenExpiresAt()).isAfter(AppClock.now().plusHours(23));
        assertThat((String) model.getValue().get("verificationLink"))
                .startsWith("https://frontend.test").contains("vargas-vet")
                .contains("/auth/verify?audiencia=TRABAJADORES_Y_USUARIOS#token=");
        verify(usuarioRepository).save(usuario);
        verify(emailService).sendEmailWithRetry(any(), eq("email/welcome-template"));
        org.mockito.InOrder bloqueo = org.mockito.Mockito.inOrder(entityManager);
        bloqueo.verify(entityManager).lock(usuario, LockModeType.PESSIMISTIC_WRITE);
        bloqueo.verify(entityManager).refresh(usuario);
        verify(auditLogService).log(eq(3), eq("REENVIAR_INVITACION_EMPLEADO"), eq("Empleados"),
                eq("Se reenvió la invitación de activación a Ana Pérez (ana@empresa.test)"));
    }

    @Test
    void empleadoConInvitacionVencidaPuedeRecibirOtraAunqueSeHayaEnviadoHaceHoras() {
        Usuario usuario = usuarioPendiente();
        usuario.setVerificationTokenExpiresAt(AppClock.now().minusHours(3));
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado(usuario, true)));
        when(credencialRepository.findAllByUsuarioId(20)).thenReturn(List.of());
        stubMail();

        empleadoService.reenviarInvitacion(30L);

        assertThat(usuario.getVerificationTokenExpiresAt()).isAfter(AppClock.now());
        verify(emailService).sendEmailWithRetry(any(), eq("email/welcome-template"));
    }

    @Test
    void empleadoInactivoNoRecibeInvitacion() {
        Usuario usuario = usuarioPendiente();
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado(usuario, false)));

        assertThatThrownBy(() -> empleadoService.reenviarInvitacion(30L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactivo");

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void empleadoConCuentaYaActivadaNoRecibeInvitacion() {
        Usuario usuario = usuarioPendiente();
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado(usuario, true)));

        assertThatThrownBy(() -> empleadoService.reenviarInvitacion(30L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya fue activada");

        verifyNoInteractions(emailService);
    }

    @Test
    void empleadoConContrasenaYaCreadaNoRecibeInvitacion() {
        Usuario usuario = usuarioPendiente();
        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setPasswordChanged(true);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado(usuario, true)));
        when(credencialRepository.findAllByUsuarioId(20)).thenReturn(List.of(credencial));

        assertThatThrownBy(() -> empleadoService.reenviarInvitacion(30L))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(emailService);
    }

    @Test
    void empleadoNoRecibeOtraInvitacionSiLaAnteriorSeEnvioHacePocosMinutos() {
        Usuario usuario = usuarioPendiente();
        LocalDateTime venceActual = AppClock.now().plusHours(24).minusMinutes(1);
        usuario.setVerificationTokenExpiresAt(venceActual);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado(usuario, true)));
        when(credencialRepository.findAllByUsuarioId(20)).thenReturn(List.of());

        assertThatThrownBy(() -> empleadoService.reenviarInvitacion(30L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La invitación se envió hace poco. Podrás reenviarla en 4 minutos");

        assertThat(usuario.getVerificationToken()).isEqualTo("hash-anterior");
        verifyNoInteractions(emailService);
    }

    @Test
    void empleadoDeOtraEmpresaNoEsAccesible() {
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> empleadoService.reenviarInvitacion(30L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(emailService);
    }

    @Test
    void clientePendienteRecibeUnEnlaceNuevo() {
        Usuario usuario = usuarioPendiente();
        when(apoderadoRepository.findByIdAndCompanyId(40L, 3)).thenReturn(Optional.of(apoderado(usuario, true)));
        when(mascotaRepository.existsByApoderadoIdAndActivoTrue(40L)).thenReturn(true);
        when(credencialRepository.findAllByUsuarioId(20)).thenReturn(List.of());
        ArgumentCaptor<Map<String, Object>> model = stubMail();

        apoderadoService.reenviarInvitacion(40L);

        assertThat(usuario.getVerificationToken()).isNotEqualTo("hash-anterior");
        assertThat((String) model.getValue().get("verificationLink")).contains("vargas-vet")
                .contains("/auth/verify?audiencia=PROPIETARIOS_Y_AUTORIZADOS#token=");
        verify(emailService).sendEmailWithRetry(any(), eq("email/welcome-template"));
        verify(auditLogService).log(eq(3), eq("REENVIAR_INVITACION_CLIENTE"), eq("Clientes"),
                eq("Se reenvió la invitación de activación a Ana Pérez (ana@empresa.test)"));
    }

    @Test
    void clienteInactivoOConCuentaActivadaNoRecibeInvitacion() {
        Usuario inactivo = usuarioPendiente();
        when(apoderadoRepository.findByIdAndCompanyId(40L, 3)).thenReturn(Optional.of(apoderado(inactivo, false)));

        assertThatThrownBy(() -> apoderadoService.reenviarInvitacion(40L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactivo");

        Usuario activado = usuarioPendiente();
        activado.setActivo(true);
        activado.setEmailVerified(true);
        when(apoderadoRepository.findByIdAndCompanyId(41L, 3)).thenReturn(Optional.of(apoderado(activado, true)));

        assertThatThrownBy(() -> apoderadoService.reenviarInvitacion(41L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya fue activada");

        verifyNoInteractions(emailService);
    }

    @Test
    void unCopropietarioSinMascotasPropiasSiRecibeLaInvitacionPorSuVinculoVigente() {
        Usuario usuario = usuarioPendiente();
        Apoderado copropietario = apoderado(usuario, true);
        when(apoderadoRepository.findByIdAndCompanyId(40L, 3)).thenReturn(Optional.of(copropietario));
        when(credencialRepository.findAllByUsuarioId(20)).thenReturn(List.of());
        when(mascotaRepository.existsByApoderadoIdAndActivoTrue(40L)).thenReturn(false);
        when(petOwnershipService.tieneVinculoVigente(copropietario)).thenReturn(true);
        stubMail();

        apoderadoService.reenviarInvitacion(40L);

        verify(emailService).sendEmailWithRetry(any(), eq("email/welcome-template"));
    }

    @Test
    void clienteNoRecibeOtraInvitacionSiLaAnteriorSeEnvioHacePocosMinutos() {
        Usuario usuario = usuarioPendiente();
        usuario.setVerificationTokenExpiresAt(AppClock.now().plusHours(24).minusMinutes(2));
        when(apoderadoRepository.findByIdAndCompanyId(40L, 3)).thenReturn(Optional.of(apoderado(usuario, true)));
        when(mascotaRepository.existsByApoderadoIdAndActivoTrue(40L)).thenReturn(true);
        when(credencialRepository.findAllByUsuarioId(20)).thenReturn(List.of());

        assertThatThrownBy(() -> apoderadoService.reenviarInvitacion(40L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Podrás reenviarla en");

        verifyNoInteractions(emailService);
    }

    @Test
    void clienteSinMascotasNoRecibeInvitacionPorqueAunNoSeLeHabiaInvitado() {
        Usuario usuario = usuarioPendiente();
        when(apoderadoRepository.findByIdAndCompanyId(40L, 3)).thenReturn(Optional.of(apoderado(usuario, true)));
        when(credencialRepository.findAllByUsuarioId(20)).thenReturn(List.of());
        when(mascotaRepository.existsByApoderadoIdAndActivoTrue(40L)).thenReturn(false);

        assertThatThrownBy(() -> apoderadoService.reenviarInvitacion(40L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Este cliente aún no tiene mascotas registradas ni está vinculado a ninguna. "
                        + "Se le invita cuando se registre su primera mascota o se le vincule a una");

        assertThat(usuario.getVerificationToken()).isEqualTo("hash-anterior");
        verifyNoInteractions(emailService);
    }

    @Test
    void siElCorreoDeUnEmpleadoNoSalePideReintentarYNoDejaConstanciaDeEnvio() {
        Usuario usuario = usuarioPendiente();
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado(usuario, true)));
        when(credencialRepository.findAllByUsuarioId(20)).thenReturn(List.of());
        stubMail();
        when(emailService.sendEmailWithRetry(any(), anyString())).thenReturn(CompletableFuture.completedFuture(false));

        assertThatThrownBy(() -> empleadoService.reenviarInvitacion(30L))
                .isInstanceOf(MailDeliveryException.class)
                .hasMessageContaining("No pudimos enviar el correo");

        verify(auditLogService, never()).log(anyString(), anyString(), anyString());
        verify(auditLogService, never()).log(any(Integer.class), anyString(), anyString(), anyString());
    }

    @Test
    void siElCorreoDeUnClienteNoSalePideReintentarYNoDejaConstanciaDeEnvio() {
        Usuario usuario = usuarioPendiente();
        when(apoderadoRepository.findByIdAndCompanyId(40L, 3)).thenReturn(Optional.of(apoderado(usuario, true)));
        when(mascotaRepository.existsByApoderadoIdAndActivoTrue(40L)).thenReturn(true);
        when(credencialRepository.findAllByUsuarioId(20)).thenReturn(List.of());
        stubMail();
        when(emailService.sendEmailWithRetry(any(), anyString())).thenReturn(CompletableFuture.completedFuture(false));

        assertThatThrownBy(() -> apoderadoService.reenviarInvitacion(40L))
                .isInstanceOf(MailDeliveryException.class);

        verify(auditLogService, never()).log(any(Integer.class), anyString(), anyString(), anyString());
    }

    @Test
    void siElEnvioDelCorreoTerminaConErrorTambienSePideReintentar() {
        Usuario usuario = usuarioPendiente();
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado(usuario, true)));
        when(credencialRepository.findAllByUsuarioId(20)).thenReturn(List.of());
        stubMail();
        CompletableFuture<Boolean> roto = new CompletableFuture<>();
        roto.completeExceptionally(new RuntimeException("sin conexión"));
        when(emailService.sendEmailWithRetry(any(), anyString())).thenReturn(roto);

        assertThatThrownBy(() -> empleadoService.reenviarInvitacion(30L))
                .isInstanceOf(MailDeliveryException.class);
    }

    @Test
    void laListaYElReenvioUsanLaMismaDefinicionDeCuentaPendiente() {
        Usuario usuario = usuarioPendiente();

        assertThat(veterinaria.vargasvet.util.CuentaPendiente.es(usuario, false)).isTrue();
        assertThat(veterinaria.vargasvet.util.CuentaPendiente.es(usuario, true)).isFalse();
        usuario.setEmailVerified(true);
        assertThat(veterinaria.vargasvet.util.CuentaPendiente.es(usuario, false)).isFalse();
    }
}

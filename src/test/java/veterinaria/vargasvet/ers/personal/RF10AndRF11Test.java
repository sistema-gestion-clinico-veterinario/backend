package veterinaria.vargasvet.ers.personal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HorarioEmpleado;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.TipoInactividad;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.dto.request.EmpleadoRequest;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyExceptionRepository;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.EspecialidadRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.TipoEmpleadoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.SessionSecurityService;
import veterinaria.vargasvet.service.impl.EmpleadoServiceImpl;
import veterinaria.vargasvet.util.BusinessValidator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RF10AndRF11Test {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private veterinaria.vargasvet.service.AccountClosureGuard accountClosureGuard;
    @Mock private RoleRepository roleRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private EspecialidadRepository especialidadRepository;
    @Mock private TipoEmpleadoRepository tipoEmpleadoRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private HorarioEmpleadoRepository horarioEmpleadoRepository;
    @Mock private CompanyOperatingHourRepository companyOperatingHourRepository;
    @Mock private CompanyExceptionRepository companyExceptionRepository;
    @Mock private CitaRepository citaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;
    @Mock private EmailService emailService;
    @Mock private BusinessValidator businessValidator;
    @Mock private AuditLogService auditLogService;
    @Mock private veterinaria.vargasvet.service.RoleAssignmentService roleAssignmentService;
    @Mock private UsuarioPorRolRepository usuarioPorRolRepository;
    @Mock private SessionSecurityService sessionSecurityService;
    @Mock private veterinaria.vargasvet.service.CompanyMembershipService companyMembershipService;
    @Mock private veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock private veterinaria.vargasvet.service.impl.UsuarioContactoService contactoService;
    @Mock private veterinaria.vargasvet.service.AdministratorProtection administratorProtection;
    @Mock private veterinaria.vargasvet.service.CajasAbiertasDelPersonal cajasAbiertas;
    @Mock private veterinaria.vargasvet.service.AccessRestoredNotifier accessRestoredNotifier;
    @Mock private jakarta.persistence.EntityManager entityManager;
    @Mock private veterinaria.vargasvet.service.ConsentimientoDatosService consentimientoDatosService;

    @InjectMocks private EmpleadoServiceImpl service;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(3);
        company.setName("Empresa de prueba");
        company.setActivo(true);

        UsuarioPrincipal principal = new UsuarioPrincipal(
                1, "admin@empresa.test", "", List.of(), 3,
                2, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        ReflectionTestUtils.setField(service, "appUrl", "https://frontend.test");
        ReflectionTestUtils.setField(service, "defaultCompanyLogo", "https://frontend.test/logo.png");
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("[CP-RF10-01] Crea empleado inactivo y envía enlace de activación sin contraseña")
    void registraEmpleadoConActivacionSegura() {
        EmpleadoRequest request = requestValido();
        Role role = roleStaff(8);
        ArgumentCaptor<Usuario> userCaptor = ArgumentCaptor.forClass(Usuario.class);
        ArgumentCaptor<veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial> credencialCaptor =
                ArgumentCaptor.forClass(veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> modelCaptor = ArgumentCaptor.forClass(Map.class);

        when(companyRepository.findById(3)).thenReturn(Optional.of(company));
        when(passwordEncoder.encode(any())).thenReturn("HASH_NO_REVERSIBLE");
        when(usuarioRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
            Usuario user = invocation.getArgument(0);
            user.setId(20);
            return user;
        });
        when(empleadoRepository.saveAndFlush(any(Empleado.class))).thenAnswer(invocation -> {
            Empleado empleado = invocation.getArgument(0);
            empleado.setId(30L);
            return empleado;
        });
        when(userMapper.toProfileDTO(any())).thenReturn(new UserProfileDTO());
        when(emailService.createMail(eq("nuevo@empresa.test"), any(), modelCaptor.capture()))
                .thenAnswer(invocation -> new Mail(null, invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));

        UserProfileDTO response = service.registerEmpleado(request);

        Usuario saved = userCaptor.getValue();
        verify(credencialRepository).save(credencialCaptor.capture());
        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial = credencialCaptor.getValue();
        String verificationLink = String.valueOf(modelCaptor.getValue().get("verificationLink"));
        String rawToken = verificationLink.substring(verificationLink.indexOf("token=") + 6);
        assertThat(response).isNotNull();
        assertThat(saved.isActivo()).isFalse();
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(credencial.getPassword()).isEqualTo("HASH_NO_REVERSIBLE");
        assertThat(credencial.getCompany()).isEqualTo(company);
        assertThat(saved.getVerificationToken()).isEqualTo(SecurityTokenUtils.hash(rawToken));
        assertThat(modelCaptor.getValue()).doesNotContainKeys("password", "tempPassword", "contraseña");
        verify(emailService).sendEmailWithRetry(any(Mail.class), eq("email/welcome-template"));
    }

    @Test
    @DisplayName("[CP-RF10-02] Rechaza registrar con un correo ya usado EN ESTA MISMA empresa (aislamiento total entre empresas)")
    void rechazaCorreoYaUsadoEnEstaEmpresa() {
        EmpleadoRequest request = requestValido();
        when(companyRepository.findById(3)).thenReturn(Optional.of(company));
        when(usuarioRepository.existsByUsernameIgnoreCaseAndCompanyId(request.getUsername(), 3)).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCaseAndCompanyId("nuevo@empresa.test", 3)).thenReturn(true);

        assertThatThrownBy(() -> service.registerEmpleado(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correo ya está registrado en esta empresa");

        verify(usuarioRepository, never()).save(any());
        verify(empleadoRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("[CP-RF11-01] Actualiza datos sin reemplazar horarios cuando no se enviaron")
    void actualizaEmpleadoYConservaHorarios() {
        Empleado empleado = empleadoExistente();
        HorarioEmpleado horario = new HorarioEmpleado();
        empleado.getHorarios().add(horario);
        EmpleadoRequest request = requestValido();
        request.setEmail(empleado.getUser().getEmail());
        request.setTelefono("987654321");
        request.setRoleIds(null);

        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(userMapper.toProfileDTO(any())).thenReturn(new UserProfileDTO());

        service.updateEmpleado(30L, request);

        verify(contactoService).actualizar(empleado.getUser(), empleado.getCompany(),
                "987654321", request.getDireccion());
        assertThat(empleado.getHorarios()).containsExactly(horario);
        verify(horarioEmpleadoRepository, never()).deleteByEmpleadoId(any());
    }

    @Test
    @DisplayName("[CP-RF11-02] Desactivar empleado bloquea usuario e invalida sus sesiones")
    void desactivaEmpleadoEInvalidaSesiones() {
        Empleado empleado = empleadoExistente();
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(citaRepository.existsCitaVigenteByEmpleadoId(eq(30L), any())).thenReturn(false);

        service.cambiarEstado(30L, false, TipoInactividad.BAJA, null);

        assertThat(empleado.getEstado()).isFalse();
        assertThat(empleado.getUser().isActivo()).isTrue();
        assertThat(empleado.getEstadoModificadoPor()).isEqualTo("admin@empresa.test");
        verify(sessionSecurityService).invalidateSessionsForCompany(empleado.getUser(), empleado.getCompany());
        verify(administratorProtection).assertCanDeactivate(empleado.getUser(), 3);
    }

    @Test
    @DisplayName("[CP-RF11-08] Suspender deja al empleado sin acceso con tipo SUSPENSION y auditoría propia")
    void suspenderRegistraElTipoYLaAuditoriaDeSuEmpresa() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(citaRepository.existsCitaVigenteByEmpleadoId(eq(30L), any())).thenReturn(false);

        service.cambiarEstado(30L, false, TipoInactividad.SUSPENSION, "Licencia médica");

        assertThat(empleado.getEstado()).isFalse();
        assertThat(empleado.getTipoInactividad()).isEqualTo(TipoInactividad.SUSPENSION);
        assertThat(empleado.getUser().isActivo()).isTrue();
        verify(sessionSecurityService).invalidateSessionsForCompany(empleado.getUser(), company);
        verify(auditLogService).log(eq(3), eq("SUSPENDER_EMPLEADO"), eq("Empleados"),
                org.mockito.ArgumentMatchers.endsWith(". Motivo: Licencia médica"));
    }

    @Test
    @DisplayName("[CP-RF11-09] Dar de baja usa su propia acción de auditoría y, sin tipo, es lo que se asume")
    void darDeBajaYElTipoPorOmision() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(citaRepository.existsCitaVigenteByEmpleadoId(eq(30L), any())).thenReturn(false);

        service.cambiarEstado(30L, false, null, null);

        assertThat(empleado.getTipoInactividad()).isEqualTo(TipoInactividad.BAJA);
        verify(auditLogService).log(eq(3), eq("DAR_DE_BAJA_EMPLEADO"), eq("Empleados"), any());
    }

    @Test
    @DisplayName("[CP-RF11-09b] La baja de quien dejó una caja abierta se aplica y la auditoría y la respuesta lo informan")
    void laBajaConCajaAbiertaSeAplicaYInforma() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(citaRepository.existsCitaVigenteByEmpleadoId(eq(30L), any())).thenReturn(false);
        when(cajasAbiertas.nombres(empleado.getUser(), 3)).thenReturn(List.of("Mostrador 1"));

        List<String> cajas = service.cambiarEstado(30L, false, TipoInactividad.BAJA, "Renuncia");

        assertThat(cajas).containsExactly("Mostrador 1");
        assertThat(empleado.getEstado()).isFalse();
        ArgumentCaptor<String> detalle = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq(3), eq("DAR_DE_BAJA_EMPLEADO"), eq("Empleados"), detalle.capture());
        assertThat(detalle.getValue()).contains("Tenía abierta la caja Mostrador 1: un administrador debe cerrarla");
    }

    @Test
    @DisplayName("[CP-RF11-09c] Repetir la misma baja a los pocos minutos no repite la auditoría ni cierra otra vez las sesiones")
    void repetirLaMismaBajaEsIdempotente() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(citaRepository.existsCitaVigenteByEmpleadoId(eq(30L), any())).thenReturn(false);

        service.cambiarEstado(30L, false, TipoInactividad.BAJA, "Primera");
        List<String> segunda = service.cambiarEstado(30L, false, TipoInactividad.BAJA, "Segunda");

        assertThat(segunda).isEmpty();
        verify(auditLogService, times(1)).log(eq(3), eq("DAR_DE_BAJA_EMPLEADO"), eq("Empleados"), any());
        verify(sessionSecurityService, times(1)).invalidateSessionsForCompany(any(), any());
        verify(entityManager, times(2)).lock(empleado, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("[CP-RF11-09d] Reactivar a quien ya está activo no repite sus efectos")
    void reactivarAQuienYaEstaActivoNoHaceNada() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));

        List<String> resultado = service.cambiarEstado(30L, true, null, null);

        assertThat(resultado).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(auditLogService, sessionSecurityService);
    }

    @Test
    @DisplayName("[CP-RF11-09e] Solo un administrador puede eliminar a un empleado")
    void soloUnAdministradorElimina() {
        UsuarioPrincipal recepcion = new UsuarioPrincipal(
                5, "recepcion@empresa.test", "", List.of(), 3,
                9, RoleScope.STAFF, RolePurpose.CUSTOM, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(recepcion, null, recepcion.getAuthorities()));

        assertThatThrownBy(() -> service.eliminar(30L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Solo un administrador");
        org.mockito.Mockito.verifyNoInteractions(empleadoRepository, auditLogService);
    }

    @Test
    @DisplayName("[CP-RF11-10] Reactivar limpia el tipo; una baja solo se reactiva y una suspensión puede pasar a baja")
    void reactivarYTransicionesEntreTipos() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        empleado.setEstado(false);
        empleado.setTipoInactividad(TipoInactividad.BAJA);
        empleado.getUser().setActivo(false);
        empleado.getUser().setEmailVerified(true);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));

        assertThatThrownBy(() -> service.cambiarEstado(30L, false, TipoInactividad.SUSPENSION, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dado de baja");

        empleado.setTipoInactividad(TipoInactividad.SUSPENSION);
        assertThatThrownBy(() -> service.cambiarEstado(30L, false, TipoInactividad.SUSPENSION, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está suspendido");
        service.cambiarEstado(30L, false, TipoInactividad.BAJA, null);
        assertThat(empleado.getTipoInactividad()).isEqualTo(TipoInactividad.BAJA);

        service.cambiarEstado(30L, true, null, null);

        assertThat(empleado.getEstado()).isTrue();
        assertThat(empleado.getTipoInactividad()).isNull();
        assertThat(empleado.getUser().isActivo()).isTrue();
        verify(auditLogService).log(eq(3), eq("REACTIVAR_EMPLEADO"), eq("Empleados"), any());
    }

    @Test
    @DisplayName("[CP-RF11-11] La edición delega la asignación de roles al servicio autorizado")
    void cambiarRolesDelegaEnElServicioDeAsignaciones() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        EmpleadoRequest request = requestValido();
        request.setEmail(empleado.getUser().getEmail());
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(userMapper.toProfileDTO(any())).thenReturn(new UserProfileDTO());

        service.updateEmpleado(30L, request);

        verify(roleAssignmentService).replaceStaffRoles(empleado.getUser(), company, Set.of(8));
    }

    @Test
    @DisplayName("[CP-RF11-12] Si la protección rechaza, los roles del empleado quedan como estaban")
    void siLaProteccionRechazaLosRolesNoSeTocan() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        EmpleadoRequest request = requestValido();
        request.setEmail(empleado.getUser().getEmail());
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        org.mockito.Mockito.doThrow(new IllegalStateException("No se puede quitar el rol de administrador al único administrador activo"))
                .when(roleAssignmentService).replaceStaffRoles(empleado.getUser(), company, Set.of(8));

        assertThatThrownBy(() -> service.updateEmpleado(30L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("único administrador activo");

        verify(empleadoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("[CP-RF11-13] La plataforma también delega la asignación del administrador")
    void plataformaDelegaLaAsignacionDelAdministrador() {
        UsuarioPrincipal plataforma = new UsuarioPrincipal(
                1, "plataforma@test.local", "", List.of(), null,
                1, RoleScope.PLATFORM, RolePurpose.PLATFORM_ADMIN, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(plataforma, null, plataforma.getAuthorities()));
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        EmpleadoRequest request = requestValido();
        request.setEmail(empleado.getUser().getEmail());
        Role admin = roleStaff(9);
        admin.setCompany(null);
        admin.setPurpose(RolePurpose.COMPANY_ADMIN);
        request.setRoleIds(Set.of(9));
        when(empleadoRepository.findById(30L)).thenReturn(Optional.of(empleado));
        when(userMapper.toProfileDTO(any())).thenReturn(new UserProfileDTO());

        service.updateEmpleado(30L, request);

        verify(roleAssignmentService).replaceStaffRoles(empleado.getUser(), company, Set.of(9));
    }

    @Test
    @DisplayName("[CP-RF11-03] Desactivar con motivo deja el motivo en la auditoría")
    void desactivarConMotivoLoRegistraEnLaAuditoria() {
        Empleado empleado = empleadoExistente();
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(citaRepository.existsCitaVigenteByEmpleadoId(eq(30L), any())).thenReturn(false);

        service.cambiarEstado(30L, false, TipoInactividad.BAJA, "  Renuncia  ");

        verify(auditLogService).log(eq(3), eq("DAR_DE_BAJA_EMPLEADO"), eq("Empleados"),
                org.mockito.ArgumentMatchers.endsWith(". Motivo: Renuncia"));
    }

    @Test
    @DisplayName("[CP-RF11-04] Si la protección de administradores rechaza, el empleado queda como estaba")
    void siLaProteccionRechazaNoCambiaNada() {
        Empleado empleado = empleadoExistente();
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        org.mockito.Mockito.doThrow(new IllegalStateException("No se puede desactivar al único administrador activo"))
                .when(administratorProtection).assertCanDeactivate(empleado.getUser(), 3);

        assertThatThrownBy(() -> service.cambiarEstado(30L, false, TipoInactividad.BAJA, null))
                .isInstanceOf(IllegalStateException.class);

        assertThat(empleado.getEstado()).isTrue();
        assertThat(empleado.getUser().isActivo()).isTrue();
        verifyNoInteractions(sessionSecurityService);
    }

    @Test
    @DisplayName("[CP-RF11-05] Activar también pasa por la jerarquía de administradores")
    void activarPasaPorLaJerarquia() {
        Empleado empleado = empleadoExistente();
        empleado.setEstado(false);
        empleado.getUser().setActivo(false);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));

        service.cambiarEstado(30L, true, null, null);

        verify(administratorProtection).assertCanManage(empleado.getUser(), 3);
        verify(administratorProtection, never()).assertCanDeactivate(any(), any());
        assertThat(empleado.getEstado()).isTrue();
    }

    @Test
    @DisplayName("[CP-RF11-06] La edición no puede desactivar al empleado saltándose la acción de estado")
    void actualizarNoPermiteCambiarElEstado() {
        Empleado empleado = empleadoExistente();
        EmpleadoRequest request = requestValido();
        request.setEmail(empleado.getUser().getEmail());
        request.setRoleIds(null);
        request.setEstado(false);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));

        assertThatThrownBy(() -> service.updateEmpleado(30L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("activar o desactivar");

        assertThat(empleado.getEstado()).isTrue();
        verifyNoInteractions(sessionSecurityService);
    }

    @Test
    @DisplayName("[CP-RF11-07] Eliminar también protege a los administradores")
    void eliminarPasaPorLaProteccionDeAdministradores() {
        Empleado empleado = empleadoExistente();
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("Solo un administrador"))
                .when(administratorProtection).assertCanDeactivate(empleado.getUser(), 3);

        assertThatThrownBy(() -> service.eliminar(30L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        verify(empleadoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("[CP-RF18-01] Reactivar a quien perdió su colegiatura a manos de otra persona activa se rechaza con un mensaje claro y no cambia nada")
    void reactivarConLaColegiaturaTomadaSeRechazaYNoCambiaNada() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        empleado.setEstado(false);
        empleado.setTipoInactividad(TipoInactividad.BAJA);
        empleado.setNumeroColegiatura("CMVP-1234");
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(empleadoRepository.existsByNumeroColegiaturaAndCompanyIdAndEstadoTrueAndIdNot("CMVP-1234", 3, 30L)).thenReturn(true);

        assertThatThrownBy(() -> service.cambiarEstado(30L, true, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CMVP-1234")
                .hasMessageContaining("otra persona activa");

        assertThat(empleado.getEstado()).isFalse();
        assertThat(empleado.getTipoInactividad()).isEqualTo(TipoInactividad.BAJA);
        verify(empleadoRepository, never()).saveAndFlush(any());
        verifyNoInteractions(auditLogService, sessionSecurityService, accessRestoredNotifier);
    }

    @Test
    @DisplayName("[CP-RF18-02] Si dos reactivaciones o altas chocan en el índice de colegiatura, la persona recibe el mismo mensaje claro y no un error interno")
    void siLaBaseRechazaLaColegiaturaSeTraduceAUnMensajeClaro() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        empleado.setEstado(false);
        empleado.setTipoInactividad(TipoInactividad.BAJA);
        empleado.setNumeroColegiatura("CMVP-1234");
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(empleadoRepository.saveAndFlush(any(Empleado.class))).thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                "no se pudo ejecutar", new RuntimeException("llave duplicada viola restricción de unicidad «uq_empleado_colegiatura_activo»")));

        assertThatThrownBy(() -> service.cambiarEstado(30L, true, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CMVP-1234");
    }

    @Test
    @DisplayName("[CP-RF18-03] Otra violación de la base de datos no se disfraza de colegiatura repetida")
    void otraViolacionDeLaBaseSeDejaPasarTalCual() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        empleado.setEstado(false);
        empleado.setTipoInactividad(TipoInactividad.BAJA);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(empleadoRepository.saveAndFlush(any(Empleado.class))).thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                "no se pudo ejecutar", new RuntimeException("llave duplicada viola restricción de unicidad «otro_indice»")));

        assertThatThrownBy(() -> service.cambiarEstado(30L, true, null, null))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("[CP-RF10-03] Registrar un veterinario con una colegiatura que ya usa otra persona activa se rechaza")
    void registrarConLaColegiaturaDeOtraPersonaActivaSeRechaza() {
        EmpleadoRequest request = requestValido();
        request.setTiposEmpleado(Set.of("VETERINARIO"));
        request.setNumeroColegiatura("CMVP-1234");
        when(companyRepository.findById(3)).thenReturn(Optional.of(company));
        when(passwordEncoder.encode(any())).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario user = invocation.getArgument(0);
            user.setId(20);
            return user;
        });
        when(empleadoRepository.existsByNumeroColegiaturaAndCompanyIdAndEstadoTrue("CMVP-1234", 3)).thenReturn(true);

        assertThatThrownBy(() -> service.registerEmpleado(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El número de colegiatura ya está registrado en esta empresa");

        verify(empleadoRepository, never()).saveAndFlush(any());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("[CP-RF11-11] Cambiar la colegiatura por la de otra persona activa se rechaza; conservar la propia no consulta nada")
    void editarLaColegiaturaPorLaDeOtraPersonaSeRechaza() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        empleado.setNumeroColegiatura("CMVP-1111");
        EmpleadoRequest request = requestValido();
        request.setEmail(empleado.getUser().getEmail());
        request.setRoleIds(null);
        request.setTiposEmpleado(Set.of("VETERINARIO"));
        request.setNumeroColegiatura("CMVP-2222");
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(tipoEmpleadoRepository.findByNombreAndCompanyId("VETERINARIO", 3))
                .thenReturn(Optional.of(new veterinaria.vargasvet.domain.entity.TipoEmpleado()));
        when(empleadoRepository.existsByNumeroColegiaturaAndCompanyIdAndEstadoTrueAndIdNot("CMVP-2222", 3, 30L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateEmpleado(30L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El número de colegiatura ya está registrado en esta empresa");
        assertThat(empleado.getNumeroColegiatura()).isEqualTo("CMVP-1111");
    }

    @Test
    @DisplayName("[CP-RF18-04] Al reactivar a alguien con la cuenta activada se le avisa por correo, y no se avisa en un baja ni en una repetición")
    void alReactivarSeAvisaALaPersonaSoloUnaVez() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        empleado.setEstado(false);
        empleado.setTipoInactividad(TipoInactividad.BAJA);
        empleado.getUser().setEmailVerified(true);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));

        service.cambiarEstado(30L, true, null, null);
        service.cambiarEstado(30L, true, null, null);

        verify(accessRestoredNotifier, times(1)).send(empleado.getUser(), company);

        service.cambiarEstado(30L, false, TipoInactividad.BAJA, "Renuncia");
        verify(accessRestoredNotifier, times(1)).send(any(), any());
    }

    private EmpleadoRequest requestValido() {
        EmpleadoRequest request = new EmpleadoRequest();
        request.setNombre("María");
        request.setApellido("Pérez");
        request.setNumeroDocumento("12345678");
        request.setEmail("nuevo@empresa.test");
        request.setUsername("maria.perez");
        request.setTelefono("999888777");
        request.setDireccion("Av. Central 123");
        request.setGenero(Genero.FEMENINO);
        request.setTipoDocumento(TipoDocumentoIdentidad.DNI);
        request.setRoleIds(Set.of(8));
        return request;
    }

    private Role roleStaff(Integer id) {
        Role role = new Role();
        role.setId(id);
        role.setName("ROLE_ASISTENTE");
        role.setActivo(true);
        role.setScope(RoleScope.STAFF);
        role.setPurpose(RolePurpose.CUSTOM);
        role.setCompany(company);
        return role;
    }

    @Test
    @DisplayName("Un administrador no puede reactivar la cuenta que la propia persona cerró")
    void elAdministradorNoReactivaUnaCuentaCerradaPorLaPersona() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        empleado.setEstado(false);
        empleado.setTipoInactividad(TipoInactividad.BAJA);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        org.mockito.Mockito.doThrow(new IllegalArgumentException("La persona cerró su propia cuenta"))
                .when(accountClosureGuard).assertNotSelfClosed(20, 3);

        assertThatThrownBy(() -> service.cambiarEstado(30L, true, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cerró su propia cuenta");

        assertThat(empleado.getEstado()).isFalse();
        verify(sessionSecurityService, never()).invalidateSessionsForCompany(any(), any());
    }

    @Test
    @DisplayName("Eliminar es un borrado lógico: da de baja con las mismas reglas y no toca la cuenta de la persona")
    void eliminarEsUnaBajaYNoBorraNada() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(citaRepository.existsCitaVigenteByEmpleadoId(eq(30L), any())).thenReturn(false);

        service.eliminar(30L);

        assertThat(empleado.getEstado()).isFalse();
        assertThat(empleado.getTipoInactividad()).isEqualTo(TipoInactividad.BAJA);
        assertThat(empleado.getUser().isActivo()).isTrue();
        assertThat(empleado.getEstadoModificadoPor()).isEqualTo("admin@empresa.test");
        verify(usuarioRepository, never()).deleteById(any());
        verify(empleadoRepository, never()).deleteById(any());
        verify(usuarioPorRolRepository, never()).deleteByUsuarioIdAndCompanyId(any(), any());
        verify(sessionSecurityService).invalidateSessionsForCompany(empleado.getUser(), company);
        verify(auditLogService).log(eq(3), eq("DAR_DE_BAJA_EMPLEADO"), eq("Empleados"),
                org.mockito.ArgumentMatchers.endsWith(". Motivo: Eliminado por el administrador"));
    }

    @Test
    @DisplayName("Eliminar respeta que no se da de baja a quien tiene citas programadas")
    void eliminarConCitasVigentesSeRechaza() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(citaRepository.existsCitaVigenteByEmpleadoId(eq(30L), any())).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(30L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("citas programadas vigentes");

        assertThat(empleado.getEstado()).isTrue();
    }

    @Test
    @DisplayName("Eliminar respeta la protección del último administrador")
    void eliminarAlUnicoAdministradorSeRechaza() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        org.mockito.Mockito.doThrow(new IllegalStateException("único administrador activo"))
                .when(administratorProtection).assertCanDeactivate(empleado.getUser(), 3);

        assertThatThrownBy(() -> service.eliminar(30L)).isInstanceOf(IllegalStateException.class);

        assertThat(empleado.getEstado()).isTrue();
    }

    @Test
    @DisplayName("Reactivar a quien nunca activó su cuenta no la activa por él")
    void reactivarNoActivaUnaCuentaPendiente() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        empleado.setEstado(false);
        empleado.setTipoInactividad(TipoInactividad.SUSPENSION);
        empleado.getUser().setActivo(false);
        empleado.getUser().setEmailVerified(false);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));

        service.cambiarEstado(30L, true, null, null);

        assertThat(empleado.getEstado()).isTrue();
        assertThat(empleado.getUser().isActivo()).isFalse();
    }

    @Test
    @DisplayName("Una persona con membresía en dos empresas (sin empresa heredada) se gestiona desde la empresa del empleado")
    void laBajaNoDependeDelCampoEmpresaHeredadoDelUsuario() {
        Empleado empleado = empleadoExistente();
        empleado.setCompany(company);
        empleado.getUser().setCompany(null);
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(citaRepository.existsCitaVigenteByEmpleadoId(eq(30L), any())).thenReturn(false);

        service.cambiarEstado(30L, false, TipoInactividad.BAJA, null);

        assertThat(empleado.getEstado()).isFalse();
        assertThat(empleado.getTipoInactividad()).isEqualTo(TipoInactividad.BAJA);
        verify(sessionSecurityService).invalidateSessionsForCompany(empleado.getUser(), company);
        verify(auditLogService).log(eq(3), eq("DAR_DE_BAJA_EMPLEADO"), eq("Empleados"), any());
    }

    private Empleado empleadoExistente() {
        Usuario user = new Usuario();
        user.setId(20);
        user.setEmail("empleado@empresa.test");
        user.setNombre("Ana");
        user.setApellido("Torres");
        user.setDni("87654321");
        user.setActivo(true);
        user.setCompany(company);

        Empleado empleado = new Empleado();
        empleado.setId(30L);
        empleado.setUser(user);
        empleado.setEstado(true);
        empleado.setGenero(Genero.FEMENINO);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad("87654321");
        return empleado;
    }
}

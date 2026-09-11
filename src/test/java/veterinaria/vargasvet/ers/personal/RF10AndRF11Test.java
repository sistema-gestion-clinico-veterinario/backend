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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RF10AndRF11Test {

    @Mock private UsuarioRepository usuarioRepository;
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
    @Mock private UsuarioPorRolRepository usuarioPorRolRepository;
    @Mock private SessionSecurityService sessionSecurityService;

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

        ReflectionTestUtils.setField(service, "frontendVerifyUrl", "https://frontend.test/verify?token=");
        ReflectionTestUtils.setField(service, "defaultCompanyLogo", "https://frontend.test/logo.png");
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
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> modelCaptor = ArgumentCaptor.forClass(Map.class);

        when(usuarioRepository.existsByEmail("nuevo@empresa.test")).thenReturn(false);
        when(companyRepository.findById(3)).thenReturn(Optional.of(company));
        when(passwordEncoder.encode(any())).thenReturn("HASH_NO_REVERSIBLE");
        when(usuarioRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
            Usuario user = invocation.getArgument(0);
            user.setId(20);
            return user;
        });
        when(roleRepository.findById(8)).thenReturn(Optional.of(role));
        when(empleadoRepository.save(any(Empleado.class))).thenAnswer(invocation -> {
            Empleado empleado = invocation.getArgument(0);
            empleado.setId(30L);
            return empleado;
        });
        when(userMapper.toProfileDTO(any())).thenReturn(new UserProfileDTO());
        when(emailService.createMail(eq("nuevo@empresa.test"), any(), modelCaptor.capture()))
                .thenAnswer(invocation -> new Mail(null, invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));

        UserProfileDTO response = service.registerEmpleado(request);

        Usuario saved = userCaptor.getValue();
        String verificationLink = String.valueOf(modelCaptor.getValue().get("verificationLink"));
        String rawToken = verificationLink.substring(verificationLink.indexOf("token=") + 6);
        assertThat(response).isNotNull();
        assertThat(saved.isActivo()).isFalse();
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(saved.getPassword()).isEqualTo("HASH_NO_REVERSIBLE");
        assertThat(saved.getVerificationToken()).isEqualTo(SecurityTokenUtils.hash(rawToken));
        assertThat(modelCaptor.getValue()).doesNotContainKeys("password", "tempPassword", "contraseña");
        verify(emailService).sendEmailWithRetry(any(Mail.class), eq("email/welcome-template"));
    }

    @Test
    @DisplayName("[CP-RF10-02] Rechaza correo duplicado antes de persistir")
    void rechazaCorreoDuplicadoSinCambiosParciales() {
        EmpleadoRequest request = requestValido();
        when(usuarioRepository.existsByEmail("nuevo@empresa.test")).thenReturn(true);

        assertThatThrownBy(() -> service.registerEmpleado(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correo");

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

        assertThat(empleado.getUser().getTelefono()).isEqualTo("987654321");
        assertThat(empleado.getHorarios()).containsExactly(horario);
        verify(horarioEmpleadoRepository, never()).deleteByEmpleadoId(any());
    }

    @Test
    @DisplayName("[CP-RF11-02] Desactivar empleado bloquea usuario e invalida sus sesiones")
    void desactivaEmpleadoEInvalidaSesiones() {
        Empleado empleado = empleadoExistente();
        when(empleadoRepository.findByIdAndCompanyId(30L, 3)).thenReturn(Optional.of(empleado));
        when(citaRepository.existsCitaVigenteByEmpleadoId(eq(30L), any())).thenReturn(false);

        service.cambiarEstado(30L, false);

        assertThat(empleado.getEstado()).isFalse();
        assertThat(empleado.getUser().isActivo()).isFalse();
        assertThat(empleado.getEstadoModificadoPor()).isEqualTo("admin@empresa.test");
        verify(sessionSecurityService).invalidateAllSessions(empleado.getUser());
    }

    private EmpleadoRequest requestValido() {
        EmpleadoRequest request = new EmpleadoRequest();
        request.setNombre("María");
        request.setApellido("Pérez");
        request.setNumeroDocumento("12345678");
        request.setEmail("nuevo@empresa.test");
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

    private Empleado empleadoExistente() {
        Usuario user = new Usuario();
        user.setId(20);
        user.setEmail("empleado@empresa.test");
        user.setNombre("Ana");
        user.setApellido("Torres");
        user.setDni("87654321");
        user.setTelefono("999999999");
        user.setDireccion("Av. Antigua 100");
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

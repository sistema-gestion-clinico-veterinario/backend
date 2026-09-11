package veterinaria.vargasvet.ers.personal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestParam;
import veterinaria.vargasvet.controller.EmpleadoController;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HorarioEmpleado;
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
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.SessionSecurityService;
import veterinaria.vargasvet.service.impl.EmpleadoServiceImpl;
import veterinaria.vargasvet.util.BusinessValidator;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RF09AndRF15Test {

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

    @Test
    @DisplayName("[CP-RF09-01] La consulta de empleados expone documento, rol, tipo y estado")
    void listadoExponeFiltrosExigidosPorElErs() {
        Method endpoint = Arrays.stream(EmpleadoController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("listar"))
                .findFirst()
                .orElseThrow();

        Set<String> params = Arrays.stream(endpoint.getParameters())
                .map(this::requestParamName)
                .filter(name -> !name.isBlank())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        assertThat(params).anyMatch(name -> name.contains("document"));
        assertThat(params).anyMatch(name -> name.equals("roleid") || name.equals("rolid"));
        assertThat(params).contains("tipoempleadoid");
        assertThat(params).anyMatch(name -> name.equals("estado") || name.equals("active") || name.equals("activo"));
    }

    @Test
    @DisplayName("[CP-RF15-01] Un turno con citas activas no puede eliminarse")
    void eliminarHorarioConCitaRelacionadaEsRechazado() {
        Empleado empleado = new Empleado();
        empleado.setId(7L);
        empleado.setEstado(true);

        HorarioEmpleado horario = new HorarioEmpleado();
        horario.setId(50L);
        horario.setEmpleado(empleado);
        horario.setFecha(LocalDate.of(2026, 9, 10));
        horario.setHoraInicio(LocalTime.of(9, 0));
        horario.setHoraFin(LocalTime.of(12, 0));

        when(horarioEmpleadoRepository.findById(50L)).thenReturn(Optional.of(horario));
        lenient().when(citaRepository.findActiveByEmpleadoIdAndFecha(7L, horario.getFecha()))
                .thenReturn(List.of(new Cita()));

        assertThatThrownBy(() -> service.deleteHorario(50L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("citas");

        verify(horarioEmpleadoRepository, never()).deleteById(50L);
    }

    private String requestParamName(Parameter parameter) {
        RequestParam annotation = parameter.getAnnotation(RequestParam.class);
        if (annotation == null) return "";
        if (!annotation.name().isBlank()) return annotation.name();
        if (!annotation.value().isBlank()) return annotation.value();
        return parameter.getName();
    }
}

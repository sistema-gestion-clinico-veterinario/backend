package veterinaria.vargasvet.ers.personal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HorarioEmpleado;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.request.BulkScheduleRequest;
import veterinaria.vargasvet.dto.request.HorarioEmpleadoRequest;
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
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.SessionSecurityService;
import veterinaria.vargasvet.service.impl.EmpleadoServiceImpl;
import veterinaria.vargasvet.util.BusinessValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RF12To14Test {

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

    private Empleado empleado;
    private final LocalDate fecha = LocalDate.of(2026, 9, 10);

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setId(3);
        company.setActivo(true);
        Usuario user = new Usuario();
        user.setId(20);
        user.setEmail("empleado@empresa.test");
        user.setNombre("Ana");
        user.setApellido("Torres");
        user.setCompany(company);
        empleado = new Empleado();
        empleado.setId(7L);
        empleado.setUser(user);
        empleado.setEstado(true);

        UsuarioPrincipal principal = new UsuarioPrincipal(
                1, "admin@empresa.test", "", List.of(), 3,
                2, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("[CP-RF12-01] Devuelve turnos ordenados con fecha y horas")
    void consultaHorarioOrdenado() {
        HorarioEmpleado tarde = horario(2L, LocalTime.of(14, 0), LocalTime.of(18, 0));
        HorarioEmpleado manana = horario(1L, LocalTime.of(8, 0), LocalTime.of(12, 0));
        when(horarioEmpleadoRepository.findByEmpleadoId(7L)).thenReturn(List.of(tarde, manana));

        var response = service.getHorario(7L);

        assertThat(response).extracting("id").containsExactly(1L, 2L);
        assertThat(response.getFirst().getFecha()).isEqualTo(fecha);
        assertThat(response.getFirst().getHoraInicio()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.getFirst().getHoraFin()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("[CP-RF13-01] Crea un turno válido para un empleado activo y período libre")
    void creaHorarioValido() {
        when(empleadoRepository.findByIdAndCompanyId(7L, 3)).thenReturn(Optional.of(empleado));
        when(citaRepository.findByEmpleadoIdAndDateRange(7L, fecha, fecha)).thenReturn(List.of());
        BulkScheduleRequest request = bulk(turno(LocalTime.of(8, 0), LocalTime.of(12, 0)));

        service.assignBulkSchedule(7L, request);

        verify(horarioEmpleadoRepository).save(any(HorarioEmpleado.class));
        assertThat(empleado.getHorarios()).singleElement()
                .satisfies(turno -> {
                    assertThat(turno.getFecha()).isEqualTo(fecha);
                    assertThat(turno.getHoraInicio()).isEqualTo(LocalTime.of(8, 0));
                    assertThat(turno.getHoraFin()).isEqualTo(LocalTime.of(12, 0));
                });
    }

    @Test
    @DisplayName("[CP-RF13-02] Rechaza un turno cruzado sin persistirlo")
    void rechazaHorarioCruzado() {
        when(empleadoRepository.findByIdAndCompanyId(7L, 3)).thenReturn(Optional.of(empleado));
        when(citaRepository.findByEmpleadoIdAndDateRange(7L, fecha, fecha)).thenReturn(List.of());
        when(horarioEmpleadoRepository.existsOverlap(7L, fecha, LocalTime.of(9, 0), LocalTime.of(13, 0)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.assignBulkSchedule(
                7L, bulk(turno(LocalTime.of(9, 0), LocalTime.of(13, 0)))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conflicto de horario");

        verify(horarioEmpleadoRepository, never()).save(any());
    }

    @Test
    @DisplayName("[CP-RF14-01][PARCIAL] Actualiza un turno hacia un período disponible")
    void actualizaHorarioValido() {
        HorarioEmpleado horario = horario(50L, LocalTime.of(8, 0), LocalTime.of(12, 0));
        HorarioEmpleadoRequest request = turno(LocalTime.of(9, 0), LocalTime.of(13, 0));
        request.setFecha(fecha.plusDays(1));
        when(horarioEmpleadoRepository.findById(50L)).thenReturn(Optional.of(horario));
        when(horarioEmpleadoRepository.existsOverlapExcluding(
                7L, fecha.plusDays(1), LocalTime.of(9, 0), LocalTime.of(13, 0), 50L)).thenReturn(false);

        service.updateHorario(50L, request);

        assertThat(horario.getFecha()).isEqualTo(fecha.plusDays(1));
        assertThat(horario.getHoraInicio()).isEqualTo(LocalTime.of(9, 0));
        assertThat(horario.getHoraFin()).isEqualTo(LocalTime.of(13, 0));
        verify(horarioEmpleadoRepository).save(horario);
    }

    @Test
    @DisplayName("[CP-RF14-01] Modificar un turno con citas exige identificarlas para revisión")
    void modificarHorarioConCitasRelacionadasEsRechazado() {
        HorarioEmpleado horario = horario(50L, LocalTime.of(8, 0), LocalTime.of(12, 0));
        HorarioEmpleadoRequest request = turno(LocalTime.of(9, 0), LocalTime.of(13, 0));
        request.setFecha(fecha);
        Cita cita = new Cita();
        cita.setFechaHoraInicio(LocalDateTime.of(fecha, LocalTime.of(9, 30)));
        cita.setFechaHoraFin(LocalDateTime.of(fecha, LocalTime.of(10, 0)));

        when(horarioEmpleadoRepository.findById(50L)).thenReturn(Optional.of(horario));
        lenient().when(citaRepository.findActiveByEmpleadoIdAndFecha(7L, fecha)).thenReturn(List.of(cita));

        assertThatThrownBy(() -> service.updateHorario(50L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("citas");

        verify(horarioEmpleadoRepository, never()).save(any());
    }

    private HorarioEmpleado horario(Long id, LocalTime inicio, LocalTime fin) {
        HorarioEmpleado horario = new HorarioEmpleado();
        horario.setId(id);
        horario.setEmpleado(empleado);
        horario.setFecha(fecha);
        horario.setDiaSemana(DiaSemana.JUEVES);
        horario.setHoraInicio(inicio);
        horario.setHoraFin(fin);
        horario.setActivo(true);
        return horario;
    }

    private HorarioEmpleadoRequest turno(LocalTime inicio, LocalTime fin) {
        HorarioEmpleadoRequest request = new HorarioEmpleadoRequest();
        request.setHoraInicio(inicio);
        request.setHoraFin(fin);
        return request;
    }

    private BulkScheduleRequest bulk(HorarioEmpleadoRequest shift) {
        BulkScheduleRequest request = new BulkScheduleRequest();
        request.setStartDate(fecha);
        request.setEndDate(fecha);
        request.setShifts(List.of(shift));
        request.setOverwrite(false);
        return request;
    }
}

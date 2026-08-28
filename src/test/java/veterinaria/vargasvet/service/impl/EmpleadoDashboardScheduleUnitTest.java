package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HorarioEmpleado;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.util.BusinessValidator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpleadoDashboardScheduleUnitTest {

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

    @InjectMocks private EmpleadoServiceImpl service;

    @Test
    void dashboardLoadsEmployeesAndTodaySchedulesWithTwoBatchQueries() {
        LocalDate today = LocalDate.of(2026, 8, 28);
        var firstEmployee = employeeProjection(10L, "Ana Torres", "Veterinaria");
        var secondEmployee = employeeProjection(20L, "Luis Vega", "Recepcionista");
        when(empleadoRepository.findDashboardEmployeesByCompanyId(7))
                .thenReturn(List.of(firstEmployee, secondEmployee));
        when(horarioEmpleadoRepository.findDashboardSchedulesByCompanyAndDate(7, today))
                .thenReturn(List.of(schedule(10L, today)));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(false);
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(7);

            var result = service.getSchedulesReportForDate(7, today);

            assertThat(result).hasSize(2);
            assertThat(result.getFirst().getNombreCompleto()).isEqualTo("Ana Torres");
            assertThat(result.getFirst().getHorarios()).hasSize(1);
            assertThat(result.getFirst().getHorarios().getFirst().getFecha()).isEqualTo(today);
            assertThat(result.get(1).getHorarios()).isEmpty();
        }

        verify(empleadoRepository, times(1)).findDashboardEmployeesByCompanyId(7);
        verify(horarioEmpleadoRepository, times(1))
                .findDashboardSchedulesByCompanyAndDate(7, today);
        verify(horarioEmpleadoRepository, never()).findByEmpleadoId(anyLong());
    }

    private EmpleadoRepository.DashboardEmployeeProjection employeeProjection(
            Long id, String name, String role) {
        var projection = mock(EmpleadoRepository.DashboardEmployeeProjection.class);
        when(projection.getEmpleadoId()).thenReturn(id);
        when(projection.getNombreCompleto()).thenReturn(name);
        when(projection.getCargo()).thenReturn(role);
        return projection;
    }

    private HorarioEmpleado schedule(Long employeeId, LocalDate date) {
        Empleado employee = new Empleado();
        employee.setId(employeeId);
        HorarioEmpleado schedule = new HorarioEmpleado();
        schedule.setId(100L);
        schedule.setEmpleado(employee);
        schedule.setFecha(date);
        schedule.setDiaSemana(DiaSemana.VIERNES);
        schedule.setHoraInicio(LocalTime.of(8, 0));
        schedule.setHoraFin(LocalTime.of(17, 0));
        schedule.setActivo(true);
        return schedule;
    }
}

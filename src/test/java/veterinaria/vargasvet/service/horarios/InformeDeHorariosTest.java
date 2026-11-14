package veterinaria.vargasvet.service.horarios;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.dto.response.EmployeeScheduleReportResponse;
import veterinaria.vargasvet.dto.response.HorarioEmpleadoResponse;
import veterinaria.vargasvet.repository.EmpleadoDelInforme;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HorarioDeEmpleado;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InformeDeHorariosTest {

    private static final Integer EMPRESA = 7;
    private static final LocalDate LUNES = LocalDate.of(2026, 12, 7);

    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private HorarioEmpleadoRepository horarioEmpleadoRepository;

    private InformeDeHorarios informe;

    @BeforeEach
    void preparar() {
        informe = new InformeDeHorarios(empleadoRepository, horarioEmpleadoRepository);
    }

    @Test
    void armaUnaFilaPorEmpleadoConSuCargoYSusTurnosEnElOrdenRecibido() {
        when(empleadoRepository.findEmpleadosDelInformeDeHorarios(EMPRESA)).thenReturn(List.of(
                new EmpleadoDelInforme(10L, "Ana Torres", "Veterinaria"),
                new EmpleadoDelInforme(20L, "Luis Vega", "Personal")));
        when(horarioEmpleadoRepository.findHorariosDeLaEmpresa(EMPRESA)).thenReturn(List.of(
                turno(1L, 10L, LUNES, "08:00"),
                turno(2L, 10L, LUNES, "15:00"),
                turno(3L, 20L, LUNES, "09:00")));

        List<EmployeeScheduleReportResponse> resultado = informe.completo(EMPRESA);

        assertThat(resultado).extracting(EmployeeScheduleReportResponse::getEmpleadoId).containsExactly(10L, 20L);
        assertThat(resultado.get(0).getNombreCompleto()).isEqualTo("Ana Torres");
        assertThat(resultado.get(0).getCargo()).isEqualTo("Veterinaria");
        assertThat(resultado.get(0).getHorarios()).extracting(HorarioEmpleadoResponse::getId).containsExactly(1L, 2L);
        assertThat(resultado.get(1).getHorarios()).extracting(HorarioEmpleadoResponse::getId).containsExactly(3L);
    }

    @Test
    void unEmpleadoSinTurnosApareceConUnaListaVacia() {
        when(empleadoRepository.findEmpleadosDelInformeDeHorarios(EMPRESA)).thenReturn(List.of(
                new EmpleadoDelInforme(10L, "Ana Torres", "Veterinaria"),
                new EmpleadoDelInforme(20L, "Luis Vega", "Personal")));
        when(horarioEmpleadoRepository.findHorariosDeLaEmpresa(EMPRESA)).thenReturn(List.of(turno(1L, 10L, LUNES, "08:00")));

        List<EmployeeScheduleReportResponse> resultado = informe.completo(EMPRESA);

        assertThat(resultado.get(1).getHorarios()).isEmpty();
    }

    @Test
    void losTurnosDeUnEmpleadoQueNoEstaEnElInformeNoSeMuestran() {
        when(empleadoRepository.findEmpleadosDelInformeDeHorarios(EMPRESA)).thenReturn(List.of(
                new EmpleadoDelInforme(10L, "Ana Torres", "Veterinaria")));
        when(horarioEmpleadoRepository.findHorariosDeLaEmpresa(EMPRESA)).thenReturn(List.of(
                turno(1L, 10L, LUNES, "08:00"), turno(2L, 99L, LUNES, "08:00")));

        List<EmployeeScheduleReportResponse> resultado = informe.completo(EMPRESA);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getHorarios()).extracting(HorarioEmpleadoResponse::getId).containsExactly(1L);
    }

    @Test
    void conservaElOrdenDeLosEmpleadosQueEntregaLaConsulta() {
        when(empleadoRepository.findEmpleadosDelInformeDeHorarios(EMPRESA)).thenReturn(List.of(
                new EmpleadoDelInforme(30L, "Cesar", "Personal"),
                new EmpleadoDelInforme(10L, "Ana", "Personal")));
        when(horarioEmpleadoRepository.findHorariosDeLaEmpresa(EMPRESA)).thenReturn(List.of());

        assertThat(informe.completo(EMPRESA)).extracting(EmployeeScheduleReportResponse::getEmpleadoId)
                .containsExactly(30L, 10L);
    }

    @Test
    void consultaLosEmpleadosYLosTurnosUnaSolaVezSinImportarCuantosEmpleadosHaya() {
        List<EmpleadoDelInforme> empleados = new java.util.ArrayList<>();
        for (long id = 1; id <= 50; id++) {
            empleados.add(new EmpleadoDelInforme(id, "Empleado " + id, "Personal"));
        }
        when(empleadoRepository.findEmpleadosDelInformeDeHorarios(EMPRESA)).thenReturn(empleados);
        when(horarioEmpleadoRepository.findHorariosDeLaEmpresa(EMPRESA)).thenReturn(List.of());

        informe.completo(EMPRESA);

        verify(empleadoRepository, times(1)).findEmpleadosDelInformeDeHorarios(EMPRESA);
        verify(horarioEmpleadoRepository, times(1)).findHorariosDeLaEmpresa(EMPRESA);
        verify(horarioEmpleadoRepository, never()).findByEmpleadoId(any());
        verify(horarioEmpleadoRepository, never()).findHorarioDelEmpleado(any());
    }

    @Test
    void sinEmpleadosDevuelveUnaListaVaciaSinConsultarLosTurnos() {
        when(empleadoRepository.findEmpleadosDelInformeDeHorarios(EMPRESA)).thenReturn(List.of());

        assertThat(informe.completo(EMPRESA)).isEmpty();

        verify(horarioEmpleadoRepository, never()).findHorariosDeLaEmpresa(any());
    }

    private HorarioDeEmpleado turno(Long id, Long empleadoId, LocalDate fecha, String inicio) {
        return new HorarioDeEmpleado(id, empleadoId, fecha, DiaSemana.LUNES, LocalTime.parse(inicio),
                LocalTime.parse(inicio).plusHours(4), true);
    }
}

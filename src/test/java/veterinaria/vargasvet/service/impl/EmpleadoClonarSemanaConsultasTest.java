package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.CompanyOperatingHour;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HorarioEmpleado;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyExceptionRepository;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoLoteRepository.NuevoHorario;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpleadoClonarSemanaConsultasTest {

    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private HorarioEmpleadoRepository horarioEmpleadoRepository;
    @Mock private CompanyOperatingHourRepository companyOperatingHourRepository;
    @Mock private CompanyExceptionRepository companyExceptionRepository;
    @Mock private CitaRepository citaRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private EmpleadoServiceImpl service;

    @Test
    void clonarUnaSemanaConsultaElCalendarioUnaVezPorDiaYNoUnaVezPorTurno() {
        LocalDate origen = LocalDate.of(2026, 9, 7);
        LocalDate destino = LocalDate.of(2026, 9, 14);

        Company company = new Company();
        company.setId(7);
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setApellido("Torres");
        Empleado empleado = new Empleado();
        empleado.setEstado(true);
        empleado.setCompany(company);
        empleado.setUser(usuario);
        empleado.setHorarios(new ArrayList<>());

        List<HorarioEmpleado> turnos = new ArrayList<>();
        for (int dia = 0; dia < 7; dia++) {
            turnos.add(turno(origen.plusDays(dia), LocalTime.of(8, 0), LocalTime.of(12, 0)));
            turnos.add(turno(origen.plusDays(dia), LocalTime.of(14, 0), LocalTime.of(18, 0)));
        }

        when(empleadoRepository.findById(10L)).thenReturn(java.util.Optional.of(empleado));
        when(horarioEmpleadoRepository.findByEmpleadoIdAndFechaBetween(10L, origen, origen.plusDays(6)))
                .thenReturn(turnos);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);
            security.when(SecurityUtils::getCurrentUserEmail).thenReturn("admin@vargasvet.pe");

            service.cloneWeekSchedule(10L, origen, destino);
        }

        assertThat(loteInsertado()).hasSize(14);
        verify(companyExceptionRepository, times(7)).findByCompanyIdAndDate(eq(7), any());
        verify(companyOperatingHourRepository, times(7)).findByCompanyIdAndDiaSemana(eq(7), any());
    }

    @Test
    void clonarUnaSemanaBorraLaSemanaDestinoConUnaSolaSentenciaYInsertaTodoEnUnLote() {
        LocalDate origen = LocalDate.of(2026, 9, 7);
        LocalDate destino = LocalDate.of(2026, 9, 14);
        Empleado empleado = empleadoActivo();
        when(empleadoRepository.findById(10L)).thenReturn(java.util.Optional.of(empleado));
        when(horarioEmpleadoRepository.findByEmpleadoIdAndFechaBetween(10L, origen, origen.plusDays(6)))
                .thenReturn(List.of(turno(origen, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                        turno(origen.plusDays(2), LocalTime.of(9, 0), LocalTime.of(13, 0))));

        clonar(() -> service.cloneWeekSchedule(10L, origen, destino));

        verify(horarioEmpleadoRepository).eliminarTurnosEntre(10L, destino, destino.plusDays(6));
        List<NuevoHorario> lote = loteInsertado();
        assertThat(lote).extracting(NuevoHorario::fecha).containsExactly(destino, destino.plusDays(2));
        assertThat(lote).extracting(NuevoHorario::diaSemana).containsExactly(DiaSemana.LUNES, DiaSemana.MIERCOLES);
        assertThat(lote).extracting(NuevoHorario::creadoPor).containsOnly("admin@vargasvet.pe");
        assertThat(lote).extracting(NuevoHorario::activo).containsOnly(true);
        assertThat(empleado.getHorarios()).isEmpty();
    }

    @Test
    void clonarUnDiaBorraElDiaDestinoYInsertaTodosLosTurnosEnUnLote() {
        LocalDate origen = LocalDate.of(2026, 9, 7);
        LocalDate destino = LocalDate.of(2026, 9, 9);
        Empleado empleado = empleadoActivo();
        when(empleadoRepository.findById(10L)).thenReturn(java.util.Optional.of(empleado));
        when(horarioEmpleadoRepository.findByEmpleadoIdAndFecha(10L, origen))
                .thenReturn(List.of(turno(origen, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                        turno(origen, LocalTime.of(14, 0), LocalTime.of(18, 0))));

        clonar(() -> service.cloneDaySchedule(10L, origen, destino));

        verify(horarioEmpleadoRepository).eliminarTurnosEntre(10L, destino, destino);
        List<NuevoHorario> lote = loteInsertado();
        assertThat(lote).extracting(NuevoHorario::fecha).containsOnly(destino);
        assertThat(lote).extracting(NuevoHorario::horaInicio).containsExactly(LocalTime.of(8, 0), LocalTime.of(14, 0));
        assertThat(lote).extracting(NuevoHorario::diaSemana).containsOnly(DiaSemana.MIERCOLES);
    }

    @Test
    void siLaClinicaEstaCerradaElDiaDestinoNoSeBorraNiSeInsertaNada() {
        LocalDate origen = LocalDate.of(2026, 9, 7);
        LocalDate destino = LocalDate.of(2026, 9, 9);
        Empleado empleado = empleadoActivo();
        when(empleadoRepository.findById(10L)).thenReturn(java.util.Optional.of(empleado));
        when(horarioEmpleadoRepository.findByEmpleadoIdAndFecha(10L, origen))
                .thenReturn(List.of(turno(origen, LocalTime.of(8, 0), LocalTime.of(12, 0))));
        CompanyOperatingHour cerrado = new CompanyOperatingHour();
        cerrado.setIsOpen(false);
        when(companyOperatingHourRepository.findByCompanyIdAndDiaSemana(7, DiaSemana.MIERCOLES))
                .thenReturn(java.util.Optional.of(cerrado));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> clonar(() -> service.cloneDaySchedule(10L, origen, destino)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(horarioEmpleadoRepository, never()).eliminarTurnosEntre(any(), any(), any());
        verify(horarioEmpleadoRepository, never()).insertarEnLote(any());
    }

    private void clonar(Runnable accion) {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);
            security.when(SecurityUtils::getCurrentUserEmail).thenReturn("admin@vargasvet.pe");
            accion.run();
        }
    }

    private Empleado empleadoActivo() {
        Company company = new Company();
        company.setId(7);
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setApellido("Torres");
        Empleado empleado = new Empleado();
        empleado.setEstado(true);
        empleado.setCompany(company);
        empleado.setUser(usuario);
        empleado.setHorarios(new ArrayList<>());
        return empleado;
    }

    private List<NuevoHorario> loteInsertado() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NuevoHorario>> captor = ArgumentCaptor.forClass(List.class);
        verify(horarioEmpleadoRepository).insertarEnLote(captor.capture());
        return captor.getValue();
    }

    private HorarioEmpleado turno(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        HorarioEmpleado turno = new HorarioEmpleado();
        turno.setFecha(fecha);
        turno.setDiaSemana(DiaSemana.LUNES);
        turno.setHoraInicio(inicio);
        turno.setHoraFin(fin);
        return turno;
    }
}

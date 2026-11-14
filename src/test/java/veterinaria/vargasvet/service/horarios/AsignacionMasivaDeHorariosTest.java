package veterinaria.vargasvet.service.horarios;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.CompanyException;
import veterinaria.vargasvet.domain.entity.CompanyOperatingHour;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.dto.request.BulkScheduleRequest;
import veterinaria.vargasvet.dto.request.HorarioEmpleadoRequest;
import veterinaria.vargasvet.repository.CompanyExceptionRepository;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoLoteRepository.NuevoHorario;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository.TurnoOcupado;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignacionMasivaDeHorariosTest {

    private static final Long EMPLEADO = 7L;
    private static final Integer EMPRESA = 3;
    private static final String ADMIN = "admin@empresa.test";
    private static final LocalDate LUNES = LocalDate.of(2026, 12, 7);

    @Mock private HorarioEmpleadoRepository horarioEmpleadoRepository;
    @Mock private CompanyOperatingHourRepository companyOperatingHourRepository;
    @Mock private CompanyExceptionRepository companyExceptionRepository;

    private AsignacionMasivaDeHorarios asignacion;
    private List<CompanyOperatingHour> horariosDeLaClinica;
    private List<CompanyException> excepciones;
    private List<TurnoOcupado> ocupados;

    @BeforeEach
    void preparar() {
        asignacion = new AsignacionMasivaDeHorarios(
                horarioEmpleadoRepository, companyOperatingHourRepository, companyExceptionRepository);
        horariosDeLaClinica = new ArrayList<>();
        for (DiaSemana dia : DiaSemana.values()) {
            horariosDeLaClinica.add(horario(dia, true));
        }
        excepciones = new ArrayList<>();
        ocupados = new ArrayList<>();
        when(companyOperatingHourRepository.findByCompanyId(EMPRESA)).thenReturn(horariosDeLaClinica);
        when(companyExceptionRepository.findByCompanyIdAndDateBetween(any(), any(), any())).thenReturn(excepciones);
        when(horarioEmpleadoRepository.findTurnosOcupados(any(), any(), any())).thenReturn(ocupados);
    }

    @Test
    void creaTodosLosTurnosDelRangoEnUnSoloLote() {
        asignacion.asignar(EMPLEADO, EMPRESA, solicitud(LUNES, LUNES.plusDays(1), false, null,
                turno("09:00", "13:00", null), turno("15:00", "19:00", null)), ADMIN);

        List<NuevoHorario> lote = loteInsertado();
        assertThat(lote).hasSize(4);
        assertThat(lote).extracting(NuevoHorario::fecha)
                .containsExactly(LUNES, LUNES, LUNES.plusDays(1), LUNES.plusDays(1));
        assertThat(lote).extracting(NuevoHorario::horaInicio)
                .containsExactly(hora("09:00"), hora("15:00"), hora("09:00"), hora("15:00"));
        assertThat(lote).extracting(NuevoHorario::diaSemana)
                .containsExactly(DiaSemana.LUNES, DiaSemana.LUNES, DiaSemana.MARTES, DiaSemana.MARTES);
        assertThat(lote).allSatisfy(nuevo -> {
            assertThat(nuevo.empleadoId()).isEqualTo(EMPLEADO);
            assertThat(nuevo.creadoPor()).isEqualTo(ADMIN);
            assertThat(nuevo.creadoEn()).isNotNull();
        });
    }

    @Test
    void noConsultaLaBaseDeDatosUnaVezPorDia() {
        asignacion.asignar(EMPLEADO, EMPRESA, solicitud(LUNES, LUNES.plusDays(59), false, null,
                turno("09:00", "13:00", null), turno("15:00", "19:00", null)), ADMIN);

        verify(companyExceptionRepository, times(1)).findByCompanyIdAndDateBetween(EMPRESA, LUNES, LUNES.plusDays(59));
        verify(companyOperatingHourRepository, times(1)).findByCompanyId(EMPRESA);
        verify(horarioEmpleadoRepository, times(1)).findTurnosOcupados(EMPLEADO, LUNES, LUNES.plusDays(59));
        verify(horarioEmpleadoRepository, times(1)).insertarEnLote(any());
        assertThat(loteInsertado()).hasSize(120);
    }

    @Test
    void omiteLosDiasEnQueLaClinicaEstaCerrada() {
        horariosDeLaClinica.removeIf(h -> h.getDiaSemana() == DiaSemana.DOMINGO);
        horariosDeLaClinica.add(horario(DiaSemana.DOMINGO, false));

        asignacion.asignar(EMPLEADO, EMPRESA, solicitud(LUNES, LUNES.plusDays(6), false, null,
                turno("09:00", "13:00", null)), ADMIN);

        assertThat(loteInsertado()).extracting(NuevoHorario::diaSemana).doesNotContain(DiaSemana.DOMINGO).hasSize(6);
    }

    @Test
    void omiteLosFeriadosDeLaClinica() {
        CompanyException feriado = new CompanyException();
        feriado.setDate(LUNES.plusDays(2));
        feriado.setIsOpen(false);
        feriado.setDescription("Feriado de prueba");
        excepciones.add(feriado);

        asignacion.asignar(EMPLEADO, EMPRESA, solicitud(LUNES, LUNES.plusDays(3), false, null,
                turno("09:00", "13:00", null)), ADMIN);

        assertThat(loteInsertado()).extracting(NuevoHorario::fecha).containsExactly(LUNES, LUNES.plusDays(1), LUNES.plusDays(3));
    }

    @Test
    void unaExcepcionAbiertaNoImpideAsignarTurnos() {
        CompanyException apertura = new CompanyException();
        apertura.setDate(LUNES);
        apertura.setIsOpen(true);
        excepciones.add(apertura);

        asignacion.asignar(EMPLEADO, EMPRESA, solicitud(LUNES, LUNES, false, null, turno("09:00", "13:00", null)), ADMIN);

        assertThat(loteInsertado()).hasSize(1);
    }

    @Test
    void soloAplicaLosTurnosDelDiaDeLaSemanaIndicado() {
        asignacion.asignar(EMPLEADO, EMPRESA, solicitud(LUNES, LUNES.plusDays(6), false, null,
                turno("09:00", "13:00", DiaSemana.LUNES), turno("10:00", "14:00", DiaSemana.MIERCOLES)), ADMIN);

        List<NuevoHorario> lote = loteInsertado();
        assertThat(lote).extracting(NuevoHorario::diaSemana).containsExactly(DiaSemana.LUNES, DiaSemana.MIERCOLES);
        assertThat(lote).extracting(NuevoHorario::horaInicio).containsExactly(hora("09:00"), hora("10:00"));
    }

    @Test
    void rechazaUnTurnoQueSeTraslapaConUnoExistenteYNoInsertaNada() {
        ocupados.add(ocupado(LUNES, "08:00", "12:00"));

        assertThatThrownBy(() -> asignacion.asignar(EMPLEADO, EMPRESA,
                solicitud(LUNES, LUNES, false, null, turno("09:00", "13:00", null)), ADMIN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Conflicto de horario el día " + LUNES + ": el nuevo rango (09:00-13:00) se traslapa con otro turno existente.");

        verify(horarioEmpleadoRepository, never()).insertarEnLote(any());
    }

    @Test
    void unTurnoQueSoloTocaElBordeDeUnoExistenteNoEsConflicto() {
        ocupados.add(ocupado(LUNES, "08:00", "09:00"));

        asignacion.asignar(EMPLEADO, EMPRESA, solicitud(LUNES, LUNES, false, null, turno("09:00", "13:00", null)), ADMIN);

        assertThat(loteInsertado()).hasSize(1);
    }

    @Test
    void unTurnoExistenteDeOtroDiaNoEsConflicto() {
        ocupados.add(ocupado(LUNES.plusDays(1), "08:00", "12:00"));

        asignacion.asignar(EMPLEADO, EMPRESA, solicitud(LUNES, LUNES, false, null, turno("09:00", "13:00", null)), ADMIN);

        assertThat(loteInsertado()).hasSize(1);
    }

    @Test
    void rechazaTurnosDelMismoDiaQueSeTraslapanEntreSi() {
        assertThatThrownBy(() -> asignacion.asignar(EMPLEADO, EMPRESA,
                solicitud(LUNES, LUNES, false, null, turno("09:00", "13:00", null), turno("12:00", "16:00", null)), ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Los turnos no pueden traslaparse o cruzarse (09:00 - 13:00 con 12:00 - 16:00)");
    }

    @Test
    void exigeUnaHoraDeRefrigerioEntreTurnos() {
        assertThatThrownBy(() -> asignacion.asignar(EMPLEADO, EMPRESA,
                solicitud(LUNES, LUNES, false, null, turno("09:00", "13:00", null), turno("13:30", "17:00", null)), ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Debe haber al menos 1 hora de refrigerio entre turnos (13:00 - 13:30)");
    }

    @Test
    void aceptaExactamenteUnaHoraDeRefrigerio() {
        asignacion.asignar(EMPLEADO, EMPRESA,
                solicitud(LUNES, LUNES, false, null, turno("09:00", "13:00", null), turno("14:00", "18:00", null)), ADMIN);

        assertThat(loteInsertado()).hasSize(2);
    }

    @Test
    void rechazaUnTurnoFueraDelHorarioDeAtencionDeLaClinica() {
        horariosDeLaClinica.clear();
        horariosDeLaClinica.add(horario(DiaSemana.LUNES, true, "08:00", "20:00"));

        assertThatThrownBy(() -> asignacion.asignar(EMPLEADO, EMPRESA,
                solicitud(LUNES, LUNES, false, null, turno("07:00", "12:00", null)), ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El horario (07:00 - 12:00) está fuera del horario de atención de la clínica (08:00 - 20:00)");

        verify(horarioEmpleadoRepository, never()).insertarEnLote(any());
    }

    @Test
    void sinHorarioDeAtencionConfiguradoSePermiteCualquierHora() {
        horariosDeLaClinica.clear();

        asignacion.asignar(EMPLEADO, EMPRESA, solicitud(LUNES, LUNES, false, null, turno("05:00", "23:00", null)), ADMIN);

        assertThat(loteInsertado()).hasSize(1);
    }

    @Test
    void sobrescribirEliminaSoloElTurnoOriginalYPermiteElNuevo() {
        ocupados.add(ocupado(LUNES, "09:00", "13:00"));
        ocupados.add(ocupado(LUNES, "15:00", "19:00"));

        asignacion.asignar(EMPLEADO, EMPRESA,
                solicitud(LUNES, LUNES, true, "09:00", turno("10:00", "12:00", null)), ADMIN);

        verify(horarioEmpleadoRepository).eliminarTurno(EMPLEADO, LUNES, hora("09:00"));
        assertThat(loteInsertado()).singleElement().satisfies(nuevo -> {
            assertThat(nuevo.horaInicio()).isEqualTo(hora("10:00"));
            assertThat(nuevo.horaFin()).isEqualTo(hora("12:00"));
        });
    }

    @Test
    void sobrescribirSigueRechazandoUnTraslapeConOtroTurnoExistente() {
        ocupados.add(ocupado(LUNES, "09:00", "13:00"));
        ocupados.add(ocupado(LUNES, "15:00", "19:00"));

        assertThatThrownBy(() -> asignacion.asignar(EMPLEADO, EMPRESA,
                solicitud(LUNES, LUNES, true, "09:00", turno("14:00", "16:00", null)), ADMIN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conflicto de horario");
    }

    @Test
    void sinSobrescribirNoEliminaNingunTurno() {
        asignacion.asignar(EMPLEADO, EMPRESA, solicitud(LUNES, LUNES, false, "09:00", turno("09:00", "13:00", null)), ADMIN);

        verify(horarioEmpleadoRepository, never()).eliminarTurno(any(), any(), any());
    }

    @Test
    void siNingunTurnoAplicaNoInsertaFilas() {
        asignacion.asignar(EMPLEADO, EMPRESA,
                solicitud(LUNES, LUNES, false, null, turno("09:00", "13:00", DiaSemana.MARTES)), ADMIN);

        assertThat(loteInsertado()).isEmpty();
    }

    private List<NuevoHorario> loteInsertado() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NuevoHorario>> captor = ArgumentCaptor.forClass(List.class);
        verify(horarioEmpleadoRepository).insertarEnLote(captor.capture());
        return captor.getValue();
    }

    private BulkScheduleRequest solicitud(LocalDate inicio, LocalDate fin, boolean sobrescribir, String horaOriginal,
                                          HorarioEmpleadoRequest... turnos) {
        BulkScheduleRequest request = new BulkScheduleRequest();
        request.setStartDate(inicio);
        request.setEndDate(fin);
        request.setOverwrite(sobrescribir);
        request.setOriginalStartTime(horaOriginal == null ? null : hora(horaOriginal));
        request.setShifts(List.of(turnos));
        return request;
    }

    private HorarioEmpleadoRequest turno(String inicio, String fin, DiaSemana dia) {
        HorarioEmpleadoRequest turno = new HorarioEmpleadoRequest();
        turno.setHoraInicio(hora(inicio));
        turno.setHoraFin(hora(fin));
        turno.setDiaSemana(dia);
        return turno;
    }

    private CompanyOperatingHour horario(DiaSemana dia, boolean abierto) {
        return horario(dia, abierto, "00:00", "23:59");
    }

    private CompanyOperatingHour horario(DiaSemana dia, boolean abierto, String apertura, String cierre) {
        CompanyOperatingHour horario = new CompanyOperatingHour();
        horario.setDiaSemana(dia);
        horario.setIsOpen(abierto);
        horario.setOpeningTime(hora(apertura));
        horario.setClosingTime(hora(cierre));
        return horario;
    }

    private TurnoOcupado ocupado(LocalDate fecha, String inicio, String fin) {
        return new TurnoOcupado() {
            @Override
            public LocalDate getFecha() {
                return fecha;
            }

            @Override
            public LocalTime getHoraInicio() {
                return hora(inicio);
            }

            @Override
            public LocalTime getHoraFin() {
                return hora(fin);
            }
        };
    }

    private LocalTime hora(String valor) {
        return LocalTime.parse(valor);
    }
}

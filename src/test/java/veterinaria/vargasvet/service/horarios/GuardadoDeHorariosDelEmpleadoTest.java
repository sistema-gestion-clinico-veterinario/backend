package veterinaria.vargasvet.service.horarios;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.CompanyException;
import veterinaria.vargasvet.domain.entity.CompanyOperatingHour;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.dto.request.HorarioEmpleadoRequest;
import veterinaria.vargasvet.repository.CompanyExceptionRepository;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoLoteRepository.NuevoHorario;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardadoDeHorariosDelEmpleadoTest {

    private static final Long EMPLEADO = 7L;
    private static final Integer EMPRESA = 3;
    private static final String ADMIN = "admin@empresa.test";
    private static final LocalDate LUNES = LocalDate.of(2026, 12, 7);

    @Mock private HorarioEmpleadoRepository horarioEmpleadoRepository;
    @Mock private CompanyOperatingHourRepository companyOperatingHourRepository;
    @Mock private CompanyExceptionRepository companyExceptionRepository;

    private GuardadoDeHorariosDelEmpleado guardado;
    private List<CompanyOperatingHour> horariosDeLaClinica;
    private List<CompanyException> excepciones;

    @BeforeEach
    void preparar() {
        guardado = new GuardadoDeHorariosDelEmpleado(
                horarioEmpleadoRepository, companyOperatingHourRepository, companyExceptionRepository);
        horariosDeLaClinica = new ArrayList<>();
        for (DiaSemana dia : DiaSemana.values()) {
            horariosDeLaClinica.add(horario(dia, true));
        }
        excepciones = new ArrayList<>();
        lenient().when(companyOperatingHourRepository.findByCompanyId(EMPRESA)).thenReturn(horariosDeLaClinica);
        lenient().when(companyExceptionRepository.findByCompanyIdAndDateBetween(any(), any(), any())).thenReturn(excepciones);
    }

    @Test
    void agregarInsertaLaPlantillaSemanalEnUnSoloLoteSinFecha() {
        guardado.agregar(EMPLEADO, EMPRESA, List.of(
                semanal(DiaSemana.LUNES, "09:00", "13:00"),
                semanal(DiaSemana.MARTES, "09:00", "13:00")), ADMIN);

        List<NuevoHorario> lote = loteInsertado();
        assertThat(lote).hasSize(2);
        assertThat(lote).extracting(NuevoHorario::fecha).containsOnlyNulls();
        assertThat(lote).extracting(NuevoHorario::diaSemana).containsExactly(DiaSemana.LUNES, DiaSemana.MARTES);
        assertThat(lote).extracting(NuevoHorario::empleadoId).containsOnly(EMPLEADO);
        assertThat(lote).extracting(NuevoHorario::creadoPor).containsOnly(ADMIN);
        assertThat(lote.get(0).creadoEn()).isNotNull();
    }

    @Test
    void agregarNoBorraNada() {
        guardado.agregar(EMPLEADO, EMPRESA, List.of(semanal(DiaSemana.LUNES, "09:00", "13:00")), ADMIN);

        verify(horarioEmpleadoRepository, never()).eliminarPlantillaSemanal(any());
        verify(horarioEmpleadoRepository, never()).eliminarTurnosEntre(any(), any(), any());
    }

    @Test
    void validaTodosLosTurnosConUnaSolaLecturaDelHorarioDeLaClinicaSinImportarCuantosSean() {
        List<HorarioEmpleadoRequest> pedidos = new ArrayList<>();
        for (DiaSemana dia : DiaSemana.values()) {
            pedidos.add(semanal(dia, "09:00", "13:00"));
            pedidos.add(semanal(dia, "15:00", "19:00"));
        }

        guardado.agregar(EMPLEADO, EMPRESA, pedidos, ADMIN);

        verify(companyOperatingHourRepository, times(1)).findByCompanyId(EMPRESA);
        verify(companyOperatingHourRepository, never()).findByCompanyIdAndDiaSemana(any(), any());
        verify(companyExceptionRepository, never()).findByCompanyIdAndDateBetween(any(), any(), any());
        assertThat(loteInsertado()).hasSize(14);
    }

    @Test
    void losTurnosConFechaConsultanLasExcepcionesUnaSolaVezParaElRangoCompleto() {
        guardado.agregar(EMPLEADO, EMPRESA, List.of(
                conFecha(LUNES.plusDays(5), "09:00", "13:00"),
                conFecha(LUNES, "09:00", "13:00"),
                conFecha(LUNES.plusDays(2), "09:00", "13:00")), ADMIN);

        verify(companyExceptionRepository, times(1)).findByCompanyIdAndDateBetween(EMPRESA, LUNES, LUNES.plusDays(5));
        assertThat(loteInsertado()).extracting(NuevoHorario::diaSemana)
                .containsExactly(DiaSemana.SABADO, DiaSemana.LUNES, DiaSemana.MIERCOLES);
    }

    @Test
    void ignoraLosTurnosSinHorasOSinFechaNiDiaDeSemana() {
        HorarioEmpleadoRequest sinHoraFin = semanal(DiaSemana.LUNES, "09:00", "13:00");
        sinHoraFin.setHoraFin(null);
        HorarioEmpleadoRequest sinFechaNiDia = semanal(null, "09:00", "13:00");

        guardado.agregar(EMPLEADO, EMPRESA, List.of(sinHoraFin, sinFechaNiDia, semanal(DiaSemana.MARTES, "09:00", "13:00")), ADMIN);

        assertThat(loteInsertado()).extracting(NuevoHorario::diaSemana).containsExactly(DiaSemana.MARTES);
    }

    @Test
    void sinTurnosAplicablesNoConsultaNiInserta() {
        guardado.agregar(EMPLEADO, EMPRESA, List.of(), ADMIN);

        verifyNoInteractions(companyOperatingHourRepository, companyExceptionRepository);
        assertThat(loteInsertado()).isEmpty();
    }

    @Test
    void respetaElEstadoActivoPedidoYPorDefectoEsActivo() {
        HorarioEmpleadoRequest inactivo = semanal(DiaSemana.LUNES, "09:00", "13:00");
        inactivo.setActivo(false);
        HorarioEmpleadoRequest sinIndicar = semanal(DiaSemana.MARTES, "09:00", "13:00");
        sinIndicar.setActivo(null);

        guardado.agregar(EMPLEADO, EMPRESA, List.of(inactivo, sinIndicar, semanal(DiaSemana.MIERCOLES, "09:00", "13:00")), ADMIN);

        assertThat(loteInsertado()).extracting(NuevoHorario::activo).containsExactly(false, true, true);
    }

    @Test
    void rechazaUnTurnoSemanalEnUnDiaCerradoConElMismoMensajeDeSiempre() {
        horariosDeLaClinica.set(DiaSemana.DOMINGO.ordinal(), horario(DiaSemana.DOMINGO, false));

        assertThatThrownBy(() -> guardado.agregar(EMPLEADO, EMPRESA, List.of(semanal(DiaSemana.DOMINGO, "09:00", "13:00")), ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La clínica no abre los días DOMINGO");
    }

    @Test
    void rechazaUnTurnoConFechaEnUnDiaDeCierreEspecial() {
        excepciones.add(excepcion(LUNES, "Feriado"));

        assertThatThrownBy(() -> guardado.agregar(EMPLEADO, EMPRESA, List.of(conFecha(LUNES, "09:00", "13:00")), ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La clínica está cerrada el día " + LUNES + " (Feriado)");
    }

    @Test
    void reemplazarBorraLaPlantillaYLasFechasPedidasAntesDeInsertar() {
        guardado.reemplazar(EMPLEADO, EMPRESA, List.of(
                semanal(DiaSemana.LUNES, "09:00", "13:00"),
                conFecha(LUNES, "09:00", "13:00"),
                conFecha(LUNES, "15:00", "19:00"),
                conFecha(LUNES.plusDays(1), "09:00", "13:00")), ADMIN);

        InOrder orden = inOrder(horarioEmpleadoRepository);
        orden.verify(horarioEmpleadoRepository).eliminarPlantillaSemanal(EMPLEADO);
        orden.verify(horarioEmpleadoRepository).eliminarTurnosEntre(EMPLEADO, LUNES, LUNES);
        orden.verify(horarioEmpleadoRepository).eliminarTurnosEntre(EMPLEADO, LUNES.plusDays(1), LUNES.plusDays(1));
        orden.verify(horarioEmpleadoRepository).insertarEnLote(any());
        verify(horarioEmpleadoRepository, times(2)).eliminarTurnosEntre(any(), any(), any());
    }

    @Test
    void reemplazarConSoloPlantillaNoTocaLosTurnosPorFecha() {
        guardado.reemplazar(EMPLEADO, EMPRESA, List.of(semanal(DiaSemana.LUNES, "09:00", "13:00")), ADMIN);

        verify(horarioEmpleadoRepository).eliminarPlantillaSemanal(EMPLEADO);
        verify(horarioEmpleadoRepository, never()).eliminarTurnosEntre(any(), any(), any());
    }

    @Test
    void reemplazarConUnaListaVaciaSoloLimpiaLaPlantilla() {
        guardado.reemplazar(EMPLEADO, EMPRESA, List.of(), ADMIN);

        verify(horarioEmpleadoRepository).eliminarPlantillaSemanal(EMPLEADO);
        verify(horarioEmpleadoRepository, never()).eliminarTurnosEntre(any(), any(), any());
        assertThat(loteInsertado()).isEmpty();
    }

    @Test
    void siUnTurnoEsInvalidoNoSeBorraNiSeInsertaNada() {
        horariosDeLaClinica.set(DiaSemana.DOMINGO.ordinal(), horario(DiaSemana.DOMINGO, false));

        assertThatThrownBy(() -> guardado.reemplazar(EMPLEADO, EMPRESA, List.of(
                semanal(DiaSemana.LUNES, "09:00", "13:00"),
                semanal(DiaSemana.DOMINGO, "09:00", "13:00")), ADMIN))
                .isInstanceOf(IllegalArgumentException.class);

        verify(horarioEmpleadoRepository, never()).eliminarPlantillaSemanal(any());
        verify(horarioEmpleadoRepository, never()).eliminarTurnosEntre(any(), any(), any());
        verify(horarioEmpleadoRepository, never()).insertarEnLote(any());
    }

    private List<NuevoHorario> loteInsertado() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NuevoHorario>> captor = ArgumentCaptor.forClass(List.class);
        verify(horarioEmpleadoRepository).insertarEnLote(captor.capture());
        return captor.getValue();
    }

    private HorarioEmpleadoRequest semanal(DiaSemana dia, String inicio, String fin) {
        HorarioEmpleadoRequest pedido = new HorarioEmpleadoRequest();
        pedido.setDiaSemana(dia);
        pedido.setHoraInicio(LocalTime.parse(inicio));
        pedido.setHoraFin(LocalTime.parse(fin));
        return pedido;
    }

    private HorarioEmpleadoRequest conFecha(LocalDate fecha, String inicio, String fin) {
        HorarioEmpleadoRequest pedido = semanal(null, inicio, fin);
        pedido.setFecha(fecha);
        return pedido;
    }

    private CompanyOperatingHour horario(DiaSemana dia, boolean abierto) {
        CompanyOperatingHour horario = new CompanyOperatingHour();
        horario.setDiaSemana(dia);
        horario.setIsOpen(abierto);
        horario.setOpeningTime(LocalTime.of(0, 0));
        horario.setClosingTime(LocalTime.of(23, 59));
        return horario;
    }

    private CompanyException excepcion(LocalDate fecha, String descripcion) {
        CompanyException excepcion = new CompanyException();
        excepcion.setDate(fecha);
        excepcion.setIsOpen(false);
        excepcion.setDescription(descripcion);
        return excepcion;
    }
}

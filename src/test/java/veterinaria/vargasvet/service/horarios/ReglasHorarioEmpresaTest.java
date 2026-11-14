package veterinaria.vargasvet.service.horarios;

import org.junit.jupiter.api.Test;
import veterinaria.vargasvet.domain.entity.CompanyException;
import veterinaria.vargasvet.domain.entity.CompanyOperatingHour;
import veterinaria.vargasvet.domain.enums.DiaSemana;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasHorarioEmpresaTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 12, 7);
    private static final LocalDate DOMINGO = LocalDate.of(2026, 12, 13);

    @Test
    void sinConfiguracionLaClinicaSeConsideraAbiertaYAceptaCualquierTurno() {
        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(List.of(), List.of());

        assertThat(reglas.estaAbierta(LUNES)).isTrue();
        assertThatCode(() -> reglas.validarTurno(LUNES, hora("03:00"), hora("23:00"))).doesNotThrowAnyException();
    }

    @Test
    void unDiaMarcadoComoCerradoNoEstaAbierto() {
        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(List.of(), List.of(horario(DiaSemana.DOMINGO, false)));

        assertThat(reglas.estaAbierta(DOMINGO)).isFalse();
        assertThat(reglas.estaAbierta(LUNES)).isTrue();
    }

    @Test
    void unaExcepcionCerradaCierraEseDiaConSuDescripcion() {
        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(List.of(excepcion(LUNES, false, "Feriado")), List.of());

        assertThat(reglas.estaAbierta(LUNES)).isFalse();
        assertThatThrownBy(() -> reglas.validarTurno(LUNES, hora("09:00"), hora("10:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La clínica está cerrada el día " + LUNES + " (Feriado)");
    }

    @Test
    void unaExcepcionAbiertaNoCierraElDia() {
        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(List.of(excepcion(LUNES, true, "Apertura")), List.of());

        assertThat(reglas.estaAbierta(LUNES)).isTrue();
    }

    @Test
    void rechazaUnTurnoEnUnDiaCerrado() {
        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(List.of(), List.of(horario(DiaSemana.DOMINGO, false)));

        assertThatThrownBy(() -> reglas.validarTurno(DOMINGO, hora("09:00"), hora("10:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La clínica no abre los días DOMINGO");
    }

    @Test
    void rechazaUnTurnoDeDuracionCeroOQueCruzaLaMedianoche() {
        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(List.of(), List.of(horario(DiaSemana.LUNES, true)));

        assertThatThrownBy(() -> reglas.validarTurno(LUNES, hora("10:00"), hora("10:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("La hora de inicio debe ser anterior a la de fin");
        assertThatThrownBy(() -> reglas.validarTurno(LUNES, hora("22:00"), hora("06:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("La hora de inicio debe ser anterior a la de fin");
    }

    @Test
    void rechazaUnTurnoQueEmpiezaAntesDeLaAperturaOTerminaDespuesDelCierre() {
        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(List.of(), List.of(horario(DiaSemana.LUNES, true)));

        assertThatThrownBy(() -> reglas.validarTurno(LUNES, hora("07:59"), hora("12:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El horario (07:59 - 12:00) está fuera del horario de atención de la clínica (08:00 - 20:00)");
        assertThatThrownBy(() -> reglas.validarTurno(LUNES, hora("12:00"), hora("20:01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El horario (12:00 - 20:01) está fuera del horario de atención de la clínica (08:00 - 20:00)");
    }

    @Test
    void aceptaUnTurnoQueOcupaExactamenteElHorarioDeAtencion() {
        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(List.of(), List.of(horario(DiaSemana.LUNES, true)));

        assertThatCode(() -> reglas.validarTurno(LUNES, hora("08:00"), hora("20:00"))).doesNotThrowAnyException();
    }

    @Test
    void conVariosRegistrosParaElMismoDiaUsaElPrimero() {
        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(
                List.of(excepcion(LUNES, false, "Primero"), excepcion(LUNES, true, "Segundo")), List.of());

        assertThat(reglas.estaAbierta(LUNES)).isFalse();
    }

    @Test
    void unTurnoSemanalDentroDelHorarioDeAtencionEsValido() {
        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(List.of(), List.of(horario(DiaSemana.LUNES, true)));

        assertThatCode(() -> reglas.validarTurnoSemanal(DiaSemana.LUNES, hora("08:00"), hora("20:00"))).doesNotThrowAnyException();
    }

    @Test
    void unTurnoSemanalEnUnDiaSinConfiguracionSeAcepta() {
        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(List.of(), List.of());

        assertThatCode(() -> reglas.validarTurnoSemanal(DiaSemana.LUNES, hora("03:00"), hora("23:00"))).doesNotThrowAnyException();
    }

    @Test
    void rechazaUnTurnoSemanalEnUnDiaCerrado() {
        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(List.of(), List.of(horario(DiaSemana.DOMINGO, false)));

        assertThatThrownBy(() -> reglas.validarTurnoSemanal(DiaSemana.DOMINGO, hora("09:00"), hora("10:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La clínica no abre los días DOMINGO");
    }

    @Test
    void rechazaUnTurnoSemanalFueraDelHorarioDeAtencion() {
        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(List.of(), List.of(horario(DiaSemana.LUNES, true)));

        assertThatThrownBy(() -> reglas.validarTurnoSemanal(DiaSemana.LUNES, hora("07:00"), hora("10:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El horario (07:00 - 10:00) está fuera del horario de atención de la clínica (08:00 - 20:00)");
    }

    @Test
    void rechazaUnTurnoSemanalCuyoInicioNoEsAnteriorAlFinAunqueNoHayaHorarioConfigurado() {
        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(List.of(), List.of());

        assertThatThrownBy(() -> reglas.validarTurnoSemanal(DiaSemana.LUNES, hora("10:00"), hora("10:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La hora de inicio debe ser anterior a la de fin");
    }

    private CompanyOperatingHour horario(DiaSemana dia, boolean abierto) {
        CompanyOperatingHour horario = new CompanyOperatingHour();
        horario.setDiaSemana(dia);
        horario.setIsOpen(abierto);
        horario.setOpeningTime(hora("08:00"));
        horario.setClosingTime(hora("20:00"));
        return horario;
    }

    private CompanyException excepcion(LocalDate fecha, boolean abierta, String descripcion) {
        CompanyException excepcion = new CompanyException();
        excepcion.setDate(fecha);
        excepcion.setIsOpen(abierta);
        excepcion.setDescription(descripcion);
        return excepcion;
    }

    private LocalTime hora(String valor) {
        return LocalTime.parse(valor);
    }
}

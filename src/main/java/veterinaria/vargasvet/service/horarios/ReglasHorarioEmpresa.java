package veterinaria.vargasvet.service.horarios;

import veterinaria.vargasvet.domain.entity.CompanyException;
import veterinaria.vargasvet.domain.entity.CompanyOperatingHour;
import veterinaria.vargasvet.domain.enums.DiaSemana;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ReglasHorarioEmpresa {

    private final Map<LocalDate, CompanyException> excepciones;
    private final Map<DiaSemana, CompanyOperatingHour> horarios;

    public ReglasHorarioEmpresa(List<CompanyException> excepciones, List<CompanyOperatingHour> horarios) {
        this.excepciones = excepciones.stream()
                .collect(Collectors.toMap(CompanyException::getDate, Function.identity(), (primera, repetida) -> primera));
        this.horarios = horarios.stream()
                .collect(Collectors.toMap(CompanyOperatingHour::getDiaSemana, Function.identity(), (primero, repetido) -> primero));
    }

    public boolean estaAbierta(LocalDate fecha) {
        CompanyException excepcion = excepciones.get(fecha);
        if (excepcion != null && Boolean.FALSE.equals(excepcion.getIsOpen())) {
            return false;
        }
        CompanyOperatingHour horario = horarios.get(DiaSemana.desde(fecha.getDayOfWeek()));
        return horario == null || !Boolean.FALSE.equals(horario.getIsOpen());
    }

    public void validarTurnoSemanal(DiaSemana dia, LocalTime inicio, LocalTime fin) {
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la de fin");
        }
        CompanyOperatingHour horario = horarios.get(dia);
        if (horario == null) {
            return;
        }
        if (Boolean.FALSE.equals(horario.getIsOpen())) {
            throw new IllegalArgumentException("La clínica no abre los días " + dia);
        }
        validarDentroDelHorario(horario, inicio, fin);
    }

    public void validarTurno(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        DiaSemana dia = DiaSemana.desde(fecha.getDayOfWeek());

        CompanyException excepcion = excepciones.get(fecha);
        if (excepcion != null && Boolean.FALSE.equals(excepcion.getIsOpen())) {
            throw new IllegalArgumentException("La clínica está cerrada el día " + fecha + " (" + excepcion.getDescription() + ")");
        }

        CompanyOperatingHour horario = horarios.get(dia);
        if (horario == null) {
            return;
        }
        if (Boolean.FALSE.equals(horario.getIsOpen())) {
            throw new IllegalArgumentException("La clínica no abre los días " + dia);
        }

        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la de fin (no se permiten turnos de duración cero o que crucen la medianoche)");
        }

        validarDentroDelHorario(horario, inicio, fin);
    }

    private void validarDentroDelHorario(CompanyOperatingHour horario, LocalTime inicio, LocalTime fin) {
        LocalTime apertura = horario.getOpeningTime();
        LocalTime cierre = horario.getClosingTime();
        if (inicio.isBefore(apertura) || fin.isAfter(cierre)) {
            throw new IllegalArgumentException(String.format("El horario (%s - %s) está fuera del horario de atención de la clínica (%s - %s)",
                    inicio, fin, apertura, cierre));
        }
    }
}

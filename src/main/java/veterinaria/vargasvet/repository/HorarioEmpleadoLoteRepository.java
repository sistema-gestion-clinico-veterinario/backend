package veterinaria.vargasvet.repository;

import veterinaria.vargasvet.domain.enums.DiaSemana;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface HorarioEmpleadoLoteRepository {

    record NuevoHorario(Long empleadoId, LocalDate fecha, DiaSemana diaSemana, LocalTime horaInicio,
                        LocalTime horaFin, LocalDateTime creadoEn, String creadoPor) {
    }

    void insertarEnLote(List<NuevoHorario> horarios);

    void eliminarTurno(Long empleadoId, LocalDate fecha, LocalTime horaInicio);
}

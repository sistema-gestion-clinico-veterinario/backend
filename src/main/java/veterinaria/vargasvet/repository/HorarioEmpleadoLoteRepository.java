package veterinaria.vargasvet.repository;

import veterinaria.vargasvet.domain.enums.DiaSemana;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface HorarioEmpleadoLoteRepository {

    record NuevoHorario(Long empleadoId, LocalDate fecha, DiaSemana diaSemana, LocalTime horaInicio,
                        LocalTime horaFin, boolean activo, LocalDateTime creadoEn, String creadoPor) {

        public NuevoHorario(Long empleadoId, LocalDate fecha, DiaSemana diaSemana, LocalTime horaInicio,
                            LocalTime horaFin, LocalDateTime creadoEn, String creadoPor) {
            this(empleadoId, fecha, diaSemana, horaInicio, horaFin, true, creadoEn, creadoPor);
        }
    }

    void insertarEnLote(List<NuevoHorario> horarios);

    void eliminarTurno(Long empleadoId, LocalDate fecha, LocalTime horaInicio);

    void eliminarTurnosEntre(Long empleadoId, LocalDate desde, LocalDate hasta);

    void eliminarPlantillaSemanal(Long empleadoId);
}

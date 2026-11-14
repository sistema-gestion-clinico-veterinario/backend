package veterinaria.vargasvet.repository;

import veterinaria.vargasvet.domain.enums.DiaSemana;

import java.time.LocalDate;
import java.time.LocalTime;

public record HorarioDeEmpleado(Long id, Long empleadoId, LocalDate fecha, DiaSemana diaSemana,
                                LocalTime horaInicio, LocalTime horaFin, Boolean activo) {
}

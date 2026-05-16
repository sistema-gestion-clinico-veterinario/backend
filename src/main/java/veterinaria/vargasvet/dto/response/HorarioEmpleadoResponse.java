package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.time.LocalTime;

@Data
public class HorarioEmpleadoResponse {
    private Long id;
    private java.time.LocalDate fecha;
    private String diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Boolean activo;
}

package veterinaria.vargasvet.modules.clinical.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TratamientoResumenResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estado;
}

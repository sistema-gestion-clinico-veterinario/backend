package veterinaria.vargasvet.dto.response;

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
    // Campos adicionales para el listado de seguimiento por mascota (fuera de una consulta puntual)
    private Long consultaId;
    private LocalDate fechaConsulta;
    private String veterinarioNombre;
}

package veterinaria.vargasvet.dto.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DiagnosticoResumenResponse {
    private Long id;
    private String nombre;
    private String codigoCIE;
    private String descripcion;
    private String tipo;
    private String estado;
    private LocalDate fechaProximoControl;
    // Campos adicionales para el listado de seguimiento por mascota (fuera de una consulta puntual)
    private Long consultaId;
    private LocalDate fechaConsulta;
    private String veterinarioNombre;
}

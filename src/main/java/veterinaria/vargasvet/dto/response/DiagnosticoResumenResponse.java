package veterinaria.vargasvet.dto.response;

import lombok.Data;

@Data
public class DiagnosticoResumenResponse {
    private Long id;
    private String nombre;
    private String codigoCIE;
    private String descripcion;
    private String tipo;
    private String estado;
}

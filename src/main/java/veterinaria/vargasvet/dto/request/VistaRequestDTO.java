package veterinaria.vargasvet.dto.request;

import lombok.Data;

@Data
public class VistaRequestDTO {
    private String codigo;
    private String nombre;
    private String ruta;
    private String grupo;
    private boolean activo = true;
}

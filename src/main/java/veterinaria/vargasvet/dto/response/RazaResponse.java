package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RazaResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private String especie;
    private Boolean activo;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}

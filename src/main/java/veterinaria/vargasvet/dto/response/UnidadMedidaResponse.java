package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UnidadMedidaResponse {
    private Long id;
    private Integer companyId;
    private String companyName;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private Long productosAsociados;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}

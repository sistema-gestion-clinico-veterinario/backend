package veterinaria.vargasvet.modules.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServicioRequest {

    private Integer companyId;

    @NotBlank(message = "El nombre del servicio es obligatorio")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private BigDecimal precio;

    private Boolean disponible = true;

    @NotNull(message = "La duración estimada es obligatoria")
    private Integer duracionEstimada;
}

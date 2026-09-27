package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.MotivoAjusteStock;

@Data
public class AjusteStockRequest {

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    @NotNull(message = "El nuevo stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stockNuevo;

    @NotNull(message = "El motivo es obligatorio")
    private MotivoAjusteStock motivo;

    @Size(max = 300, message = "Las observaciones no deben superar 300 caracteres")
    private String observaciones;
}

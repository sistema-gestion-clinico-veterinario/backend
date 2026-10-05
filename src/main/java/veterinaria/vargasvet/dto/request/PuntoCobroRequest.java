package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PuntoCobroRequest {
    @NotNull(message = "Debe seleccionar una sede")
    private Integer companyId;

    @NotBlank(message = "Escribe un nombre para el punto de cobro")
    @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
    private String nombre;

    private Boolean activa;
}

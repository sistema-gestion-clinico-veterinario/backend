package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsentimientoFinalidadRequest {

    private boolean otorgar;

    @Size(max = 300, message = "El motivo no debe superar 300 caracteres")
    private String motivo;
}

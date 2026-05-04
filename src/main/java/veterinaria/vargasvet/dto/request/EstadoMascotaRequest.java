package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.MotivoBajaMascota;

@Data
public class EstadoMascotaRequest {
    
    @NotNull(message = "El estado (active) es obligatorio")
    private Boolean active;
    
    private MotivoBajaMascota motivoBaja;
    
    private String otroMotivoBaja;
}

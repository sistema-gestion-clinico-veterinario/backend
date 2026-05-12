package veterinaria.vargasvet.modules.mascotas.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import veterinaria.vargasvet.modules.mascotas.domain.enums.MotivoBajaMascota;

@Data
public class EstadoMascotaRequest {
    
    @NotNull(message = "El estado (active) es obligatorio")
    private Boolean active;
    
    private MotivoBajaMascota motivoBaja;
    
    private String otroMotivoBaja;
}

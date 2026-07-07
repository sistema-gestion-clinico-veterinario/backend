package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.MotivoBajaMascota;

@Data
public class EstadoMascotaRequest {
    
    @NotNull(message = "El estado (active) es obligatorio")
    private Boolean active;
    
    private MotivoBajaMascota motivoBaja;
    
    @Size(max = 300, message = "El motivo no debe superar 300 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "El motivo contiene caracteres no permitidos")
    private String otroMotivoBaja;
}

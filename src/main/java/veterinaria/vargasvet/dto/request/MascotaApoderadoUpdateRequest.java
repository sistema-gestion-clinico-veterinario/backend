package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MascotaApoderadoUpdateRequest {

    @Size(max = 500, message = "La URL de foto no debe superar 500 caracteres")
    @Pattern(regexp = "^$|^https?://[^\\s<>]+$", message = "La URL de foto debe iniciar con http:// o https:// y no contener espacios")
    private String fotoUrl;
}

package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GoogleReactivateRequest {
    @NotBlank(message = "La confirmación no es válida")
    @Size(max = 100, message = "La confirmación no es válida")
    private String ticket;
}

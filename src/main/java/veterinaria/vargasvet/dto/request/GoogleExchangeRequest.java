package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleExchangeRequest {
    @NotBlank(message = "El código es obligatorio")
    private String code;
}

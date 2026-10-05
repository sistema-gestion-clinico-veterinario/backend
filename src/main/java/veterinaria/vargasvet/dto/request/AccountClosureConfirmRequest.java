package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AccountClosureConfirmRequest {
    @NotBlank(message = "Ingresa el código que recibiste")
    @Pattern(regexp = "^\\s*\\d{6}\\s*$", message = "El código tiene 6 dígitos")
    private String code;
}

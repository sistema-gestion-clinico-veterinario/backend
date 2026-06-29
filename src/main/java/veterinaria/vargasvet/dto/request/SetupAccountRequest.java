package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetupAccountRequest {
    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 6, max = 72)
    private String password;
}

package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResendVerificationByTokenRequest {
    @NotBlank
    @Size(max = 512)
    private String token;

    @Size(max = 100)
    private String slug;
}

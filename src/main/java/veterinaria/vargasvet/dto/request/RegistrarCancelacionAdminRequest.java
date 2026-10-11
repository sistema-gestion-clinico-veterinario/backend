package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrarCancelacionAdminRequest {
    @NotNull(message = "Indique al cliente")
    private Long apoderadoId;

    @NotBlank(message = "Indique cómo verificó la identidad del titular")
    @Size(min = 5, max = 180, message = "Describa la verificación en 5 a 180 caracteres")
    private String metodoVerificacion;

    @Size(max = 500, message = "El motivo no debe superar 500 caracteres")
    private String motivo;
}

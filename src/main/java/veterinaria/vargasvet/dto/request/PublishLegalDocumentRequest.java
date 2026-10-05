package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.LegalDocumentType;

@Data
public class PublishLegalDocumentRequest {
    @NotNull(message = "Indica el tipo de documento")
    private LegalDocumentType tipo;

    @NotBlank(message = "Indica la versión")
    @Size(max = 20, message = "La versión no puede superar los 20 caracteres")
    private String version;

    @NotBlank(message = "El texto del documento es obligatorio")
    @Size(max = 300_000, message = "El texto es demasiado extenso")
    private String contenido;
}

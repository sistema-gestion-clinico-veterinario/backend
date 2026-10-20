package veterinaria.vargasvet.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AltaClienteRequest {
    @Valid
    @NotNull(message = "Los datos del cliente son obligatorios")
    private ApoderadoRequest cliente;

    @Valid
    @NotNull(message = "Los datos de la primera mascota son obligatorios")
    private PrimeraMascotaRequest mascota;
}

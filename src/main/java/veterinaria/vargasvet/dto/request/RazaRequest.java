package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

@Data
public class RazaRequest {

    @NotBlank(message = "El nombre de la raza es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Size(max = 300, message = "La descripcion no debe superar 300 caracteres")
    private String descripcion;

    @NotNull(message = "Debe seleccionar una especie")
    private EspecieMascota especie;
}

package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

@Data
public class RazaRequest {

    @NotBlank(message = "El nombre de la raza es obligatorio")
    @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "El nombre solo debe contener letras y espacios")
    private String nombre;

    @Size(max = 500, message = "La descripción no debe superar 500 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "La descripcion contiene caracteres no permitidos")
    private String descripcion;

    @NotNull(message = "La especie es obligatoria")
    private EspecieMascota especie;
}

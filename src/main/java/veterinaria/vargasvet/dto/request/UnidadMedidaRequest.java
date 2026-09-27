package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.validation.MeaningfulText;

@Data
public class UnidadMedidaRequest {

    private Integer companyId;

    @NotBlank(message = "El nombre de la unidad de medida es obligatorio")
    @Size(min = 1, max = 40, message = "El nombre debe tener entre 1 y 40 caracteres")
    @MeaningfulText(message = "El nombre de la unidad de medida debe contener texto real")
    private String nombre;

    @Size(max = 300, message = "La descripción no debe superar 300 caracteres")
    @Pattern(regexp = "^$|^(?=.*[\\p{L}\\p{N}])(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*$", message = "La descripción contiene caracteres no permitidos")
    private String descripcion;
}

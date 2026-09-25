package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.validation.MeaningfulText;

@Data
public class CategoriaProductoRequest {

    private Integer companyId;

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
    @MeaningfulText(message = "El nombre de la categoría debe contener texto real")
    private String nombre;
}

package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.validation.MeaningfulText;

@Data
public class RoleRequest {
    @Size(min = 2, max = 65, message = "El nombre del rol debe tener entre 2 y 65 caracteres")
    @Pattern(regexp = "^(ROLE_)?[A-Za-zÑñ_\\s]{2,60}$", message = "El rol solo puede contener letras, espacios y guion bajo")
    private String name;

    @Size(min = 2, max = 65, message = "El nombre del rol debe tener entre 2 y 65 caracteres")
    @Pattern(regexp = "^(ROLE_)?[A-Za-zÑñ_\\s]{2,60}$", message = "El rol solo puede contener letras, espacios y guion bajo")
    private String nombre;

    @Size(max = 250, message = "La descripcion no debe superar 250 caracteres")
    @MeaningfulText(message = "La descripcion debe contener texto real, no solo numeros o simbolos")
    private String descripcion;

    private Integer companyId;
}

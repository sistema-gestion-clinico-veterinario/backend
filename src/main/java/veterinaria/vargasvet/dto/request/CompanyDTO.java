package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CompanyDTO {
    
    private Integer id;

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    private String name;

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "\\d{11}", message = "El RUC debe tener exactamente 11 dígitos")
    private String ruc;

    @NotBlank(message = "La dirección es obligatoria")
    private String address;

    @NotBlank(message = "El teléfono es obligatorio")
    private String phone;

    @NotBlank(message = "El correo de contacto es obligatorio")
    @Email(message = "El formato del correo es inválido")
    private String email;

    private String logoUrl;
    private String website;
    private String description;
    private String businessHours;
}

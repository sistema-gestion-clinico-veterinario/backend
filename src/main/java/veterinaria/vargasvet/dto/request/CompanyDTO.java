package veterinaria.vargasvet.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.validation.MeaningfulText;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CompanyDTO {
    
    private Integer id;

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    @Pattern(regexp = "^(?=.*\\p{L})[\\p{L}\\p{N}\\s.&,()'\\-]+$", message = "El nombre debe contener letras y solo caracteres permitidos")
    private String name;

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "(10|20)\\d{9}", message = "El RUC debe tener 11 digitos y empezar con 10 o 20")
    private String ruc;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(min = 5, max = 200, message = "La direccion debe tener entre 5 y 200 caracteres")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s.,#\\-/°:]+$", message = "La direccion contiene caracteres no permitidos")
    @MeaningfulText(message = "La direccion debe contener texto real, no solo numeros o simbolos")
    private String address;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{9}$", message = "El telefono debe contener 9 digitos numericos")
    private String phone;

    @NotBlank(message = "El correo de contacto es obligatorio")
    @Email(message = "El formato del correo es inválido")
    @Size(max = 100, message = "El correo no debe superar 100 caracteres")
    @Pattern(regexp = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$", message = "El correo debe estar en minusculas y tener un formato valido")
    private String email;

    @Size(max = 500, message = "La URL del logo no debe superar 500 caracteres")
    @Pattern(regexp = "^$|^https?://[^\\s<>]+$", message = "La URL del logo debe iniciar con http:// o https:// y no contener espacios")
    private String logoUrl;

    @Size(max = 200, message = "El sitio web no debe superar 200 caracteres")
    @Pattern(regexp = "^$|^https?://[^\\s<>]+$", message = "El sitio web debe iniciar con http:// o https:// y no contener espacios")
    private String website;

    @Size(max = 500, message = "La descripcion no debe superar 500 caracteres")
    @MeaningfulText(message = "La descripcion debe contener texto real, no solo numeros o simbolos")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "La descripcion contiene caracteres no permitidos")
    private String description;

    @Size(max = 100, message = "El horario comercial no debe superar 100 caracteres")
    @MeaningfulText(requireLetter = false, message = "El horario comercial no puede estar compuesto solo por simbolos")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*$", message = "El horario comercial contiene caracteres no permitidos")
    private String businessHours;
    
    private List<@Valid CompanyOperatingHourDTO> operatingHours;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}

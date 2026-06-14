package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VistaRequestDTO {
    @NotBlank(message = "El codigo de la vista es obligatorio")
    @Size(min = 3, max = 80, message = "El codigo debe tener entre 3 y 80 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9_\\s-]+$", message = "El codigo solo puede contener letras, numeros, guiones y espacios")
    private String codigo;

    @NotBlank(message = "El nombre de la vista es obligatorio")
    @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
    private String nombre;

    @Size(max = 120, message = "La ruta no debe superar 120 caracteres")
    @Pattern(regexp = "^$|^/[A-Za-z0-9/_-]+$", message = "La ruta debe iniciar con / y usar caracteres validos")
    private String ruta;

    @NotBlank(message = "El grupo es obligatorio")
    @Size(max = 40, message = "El grupo no debe superar 40 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9_\\s-]+$", message = "El grupo solo puede contener letras, numeros, guiones y espacios")
    private String grupo;
    private Integer orden;
    private Integer ordenGrupo;
    private boolean activo = true;

    @Size(max = 60, message = "El ícono no debe superar 60 caracteres")
    private String icono;
}

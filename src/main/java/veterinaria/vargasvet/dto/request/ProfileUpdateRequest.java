package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
    private String nombre;

    @Size(min = 2, max = 80, message = "El apellido debe tener entre 2 y 80 caracteres")
    private String apellido;

    @Pattern(regexp = "^$|^[0-9]{9}$", message = "El telefono debe contener 9 digitos numericos")
    private String telefono;

    @Size(max = 200, message = "La direccion no debe superar 200 caracteres")
    private String direccion;

    @Size(max = 500, message = "Las observaciones no deben superar 500 caracteres")
    private String observaciones;

    @Size(max = 500, message = "La URL de foto no debe superar 500 caracteres")
    private String fotoUrl;
}

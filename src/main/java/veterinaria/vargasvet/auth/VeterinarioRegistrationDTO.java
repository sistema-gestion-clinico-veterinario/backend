package veterinaria.vargasvet.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import veterinaria.vargasvet.shared.Genero;
import veterinaria.vargasvet.shared.TipoDocumentoIdentidad;

import java.util.Set;

@Data
public class VeterinarioRegistrationDTO {
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El número de colegiatura es obligatorio")
    private String numeroColegiatura;

    @NotEmpty(message = "Debe especificar al menos una especialidad")
    private Set<String> especialidades;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo es inválido")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;

    // Campos opcionales
    private String fotoUrl;
    private String direccion;
    private String observaciones;

    // Campos adicionales necesarios para la entidad base
    private TipoDocumentoIdentidad tipoDocumento;
    private String numeroDocumento;
    private Genero genero;
}

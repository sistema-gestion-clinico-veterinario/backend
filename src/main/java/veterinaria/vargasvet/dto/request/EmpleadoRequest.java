package veterinaria.vargasvet.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;

import java.util.List;
import java.util.Set;

@Data
public class EmpleadoRequest {
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 80, message = "El apellido debe tener entre 2 y 80 caracteres")
    private String apellido;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El documento no debe superar 20 caracteres")
    private String numeroDocumento;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo es inválido")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{9}$", message = "El telefono debe contener 9 digitos numericos")
    private String telefono;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(max = 200, message = "La direccion no debe superar 200 caracteres")
    private String direccion;

    @NotNull(message = "El genero es obligatorio")
    private Genero genero;

    @NotNull(message = "El tipo de documento es obligatorio")
    private TipoDocumentoIdentidad tipoDocumento;

    @NotEmpty(message = "Debe asignar al menos un rol")
    private Set<String> roles;

    private Set<String> tiposEmpleado;
    @Size(max = 30, message = "El numero de colegiatura no debe superar 30 caracteres")
    private String numeroColegiatura;
    private Set<String> especialidades;
    private String fotoUrl;
    private String observaciones;
    private Boolean estado;
    private Integer companyId;
    private List<@Valid HorarioEmpleadoRequest> horarios;

    @AssertTrue(message = "El numero de documento no corresponde al tipo seleccionado")
    public boolean isNumeroDocumentoValido() {
        if (tipoDocumento == null || numeroDocumento == null) return true;
        return switch (tipoDocumento) {
            case DNI -> numeroDocumento.matches("^\\d{8}$");
            case CARNET_EXTRANJERIA -> numeroDocumento.matches("^\\d{9}$");
            case PASAPORTE -> numeroDocumento.matches("^[A-Za-z]\\d{8}$");
        };
    }

    @AssertTrue(message = "El numero de colegiatura es obligatorio para veterinarios")
    public boolean isColegiaturaValida() {
        if (roles == null) return true;
        boolean esVeterinario = roles.stream().anyMatch(rol -> "ROLE_VETERINARIO".equalsIgnoreCase(rol));
        return !esVeterinario || (numeroColegiatura != null && !numeroColegiatura.isBlank());
    }
}

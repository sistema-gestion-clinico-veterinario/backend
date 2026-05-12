package veterinaria.vargasvet.modules.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import veterinaria.vargasvet.modules.users.domain.enums.Genero;
import veterinaria.vargasvet.modules.users.domain.enums.TipoDocumentoIdentidad;

@Data
public class ApoderadoRequest {
    private Long id;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;
    
    @NotBlank(message = "El email es obligatorio")
    private String email;
    
    @NotNull(message = "El tipo de documento es obligatorio")
    private TipoDocumentoIdentidad tipoDocumento;
    
    @NotBlank(message = "El número de documento es obligatorio")
    private String numeroDocumento;
    
    @NotNull(message = "El género es obligatorio")
    private Genero genero;
    
    private String telefono;
    private String direccion;
    private String referencias;
    private String observaciones;
    private Integer companyId;
}

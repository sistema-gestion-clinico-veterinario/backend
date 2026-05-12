package veterinaria.vargasvet.perfil;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String nombre;
    private String apellido;
    private String telefono;
    private String direccion;
    private String observaciones;
    private String fotoUrl;
}

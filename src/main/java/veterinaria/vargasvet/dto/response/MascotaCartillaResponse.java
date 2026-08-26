package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MascotaCartillaResponse {
    private Long id;
    private String nombreCompleto;
    private String especie;
    private String razaNombre;
    private String apoderadoNombreCompleto;
    private Boolean activo;
    private LocalDate fechaUltimaAplicacion;
}

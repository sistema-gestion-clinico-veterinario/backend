package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class SugerenciaControlResponse {
    private String origen; // "TRATAMIENTO" o "DIAGNOSTICO"
    private Long origenId;
    private String nombre;
    private Long mascotaId;
    private String mascotaNombre;
    private Long apoderadoId;
    private String apoderadoNombre;
    private LocalDate fechaControl;
    private long diasRestantes;
}

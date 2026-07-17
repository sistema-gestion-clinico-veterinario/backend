package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;

import java.time.LocalDate;

@Data
@Builder
public class AplicacionPreventivaResponse {
    private Long id;
    private TipoControlPreventivo tipo;
    private String nombreControl;
    private LocalDate fechaAplicacion;
    private Integer periodicidadMeses;
    private LocalDate fechaProximaAplicacion;
    private String veterinarioNombre;
}

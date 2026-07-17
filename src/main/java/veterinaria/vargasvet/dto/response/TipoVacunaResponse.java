package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

@Data
@Builder
public class TipoVacunaResponse {
    private Long id;
    private String nombre;
    private EspecieMascota especie;
    private Integer periodicidadMesesSugerida;
}

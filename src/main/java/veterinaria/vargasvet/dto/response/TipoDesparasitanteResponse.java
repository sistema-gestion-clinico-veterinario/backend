package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

import java.math.BigDecimal;

@Data
@Builder
public class TipoDesparasitanteResponse {
    private Long id;
    private String nombre;
    private EspecieMascota especie;
    private Integer periodicidadMesesSugerida;
    private BigDecimal precio;
}
package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CartillaDetalleResponse {
    private List<TipoVacunaResponse> vacunas;
    private List<TipoDesparasitanteResponse> desparasitantes;
    private List<ControlPreventivoResponse> controles;
    private List<AplicacionPreventivaResponse> aplicaciones;
}
